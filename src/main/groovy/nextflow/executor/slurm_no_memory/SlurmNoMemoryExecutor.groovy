package nextflow.executor.slurm_no_memory

import java.nio.file.Path
import java.util.regex.Pattern

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.util.ServiceName
import nextflow.fusion.FusionHelper
import nextflow.executor.AbstractGridExecutor
import nextflow.executor.ExecutorConfig
import nextflow.executor.TaskArrayExecutor
import nextflow.processor.TaskArrayRun
import nextflow.processor.TaskConfig
import nextflow.processor.TaskRun
import nextflow.util.MemoryUnit

/**
 * Processor for SLURM resource manager without memory based on nextflow/executor/slurm
 *
 * See http://computing.llnl.gov/linux/slurm/
 * See https://github.com/nextflow-io/nextflow/blob/master/modules/nextflow/src/main/groovy/nextflow/executor/SlurmExecutor.groovy
 *
 *
 * @author CUBIC
 */
@Slf4j
@ServiceName("slurm-no-memory")
@CompileStatic
class SlurmNoMemoryExecutor extends AbstractGridExecutor implements TaskArrayExecutor {

    static private Pattern SUBMIT_REGEX = ~/Submitted batch job (\d+)/

    private boolean perCpuMemAllocation
    private boolean onlyJobState

    private boolean hasSignalOpt(TaskConfig config) {
        final opts = config.getClusterOptionsAsString()
        return opts ? opts.contains('--signal ') || opts.contains('--signal=') : false
    }

    /**
     * Gets the directives to submit the specified task to the cluster for execution
     *
     * @param task A {@link TaskRun} to be submitted
     * @param result The {@link List} instance to which add the job directives
     * @return A {@link List} containing all directive tokens and values.
     */
    protected List<String> getDirectives(TaskRun task, List<String> result) {

        if( task instanceof TaskArrayRun ) {
            final arraySize = task.getArraySize()
            result << '--array' << "0-${arraySize - 1}".toString()
        }

        result << '-J' << getJobNameFor(task)

        // -o OUTFILE and no -e option => stdout and stderr merged to stdout/OUTFILE
        result << '-o' << (task.isArray() ? '/dev/null' : quote(task.workDir.resolve(TaskRun.CMD_LOG)))

        result << '--no-requeue' << '' // note: directive need to be returned as pairs

        if( !hasSignalOpt(task.config) ) {
            // see https://github.com/nextflow-io/nextflow/issues/2163
            // and https://slurm.schedmd.com/sbatch.html#OPT_signal
            result << '--signal' << 'B:USR2@30'
        }

        String memPerCoreConfig =  config.getExecConfigProp(name, 'memoryPerCore', null) as String
        int nbCpus = task.config.getCpus()

        if( memPerCoreConfig == null ) {
            memPerCoreConfig = "4"
            log.debug "By default the number of Memory per core is set to 4 !"
        }


        def memPerCore = MemoryUnit.of(memPerCoreConfig + ' GB').mega

        if( task.config.getMemory() ) {
            nbCpus = Math.max(task.config.getCpus(), (int) Math.ceil((task.config.getMemory().toMega() / memPerCore)))
        }
        

        if( nbCpus > 1 ) {
            result << '-c' << nbCpus.toString()
        }

        if( task.config.getTime() ) {
            result << '-t' << task.config.getTime().format('HH:mm:ss')
        }

        // the requested partition (a.k.a queue) name
        if( task.config.queue ) {
            result << '-p' << (task.config.queue.toString())
        }

        // -- at the end append the command script wrapped file name
        addClusterOptionsDirective(task.config, result)

        // add slurm account from config
        final account = config.getExecConfigProp(name, 'account', null) as String
        if( account ) {
            result << '-A' << account
        }

        return result
    }

    String getHeaderToken() { '#SBATCH' }

    /**
     * The command line to submit this job
     *
     * @param task The {@link TaskRun} instance to submit for execution to the cluster
     * @param scriptFile The file containing the job launcher script
     * @return A list representing the submit command line
     */
    @Override
    List<String> getSubmitCommandLine(TaskRun task, Path scriptFile ) {
        return pipeLauncherScript()
                ? List.of('sbatch')
                : List.of('sbatch', scriptFile.getName())
    }

    /**
     * Parse the string returned by the {@code sbatch} command and extract the job ID string
     *
     * @param text The string returned when submitting the job
     * @return The actual job ID string
     */
    @Override
    def parseJobId(String text) {

        for( String line : text.readLines() ) {
            def m = SUBMIT_REGEX.matcher(line)
            if( m.find() ) {
                return m.group(1).toString()
            }
        }

        // customised `sbatch` command can return only the jobid
        def id = text.trim()
        if( id.isLong() )
            return id

        throw new IllegalStateException("Invalid SLURM submit response:\n$text\n\n")
    }

    @Override
    protected List<String> getKillCommand() { ['scancel'] }

    @Override
    protected List<String> queueStatusCommand(Object queue) {

        final result = ['squeue','--noheader', '-o','%i %t', '-t', 'all']

        if( onlyJobState ) {
            result << '--only-job-state'
            // -p and -u cannot be used with --only-job-state
            return result
        }

        if( queue )
            result << '-p' << queue.toString()

        final user = System.getProperty('user.name')
        if( user )
            result << '-u' << user
        else
            log.debug "Cannot retrieve current user"

        return result
    }

    /*
     *  Maps SLURM job status to nextflow status
     *  see http://slurm.schedmd.com/squeue.html#SECTION_JOB-STATE-CODES
     */
    static private Map<String,QueueStatus> STATUS_MAP = [
            'PD': QueueStatus.PENDING,  // (pending)
            'R': QueueStatus.RUNNING,   // (running)
            'CA': QueueStatus.ERROR,    // (cancelled)
            'CF': QueueStatus.PENDING,  // (configuring)
            'CG': QueueStatus.RUNNING,  // (completing)
            'CD': QueueStatus.DONE,     // (completed)
            'F': QueueStatus.ERROR,     // (failed),
            'TO': QueueStatus.ERROR,    // (timeout),
            'NF': QueueStatus.ERROR,    // (node failure)
            'S': QueueStatus.HOLD,      // (job suspended)
            'ST': QueueStatus.HOLD,     // (stopped)
            'PR': QueueStatus.ERROR,    // (Job terminated due to preemption)
            'BF': QueueStatus.ERROR,    // (boot fail, Job terminated due to launch failure)
    ]

    @Override
    protected Map<String, QueueStatus> parseQueueStatus(String text) {

        final result = new LinkedHashMap<String, QueueStatus>()

        text.eachLine { String line ->
            def cols = line.split(/\s+/)
            if( cols.size() == 2 ) {
                result.put( cols[0], STATUS_MAP.get(cols[1]) )
            }
            else {
                log.debug "[SLURM] invalid status line: `$line`"
            }
        }

        return result
    }

    @Override
    void register() {
        super.register()
        perCpuMemAllocation = config.getExecConfigProp(name, 'perCpuMemAllocation', false)
        onlyJobState = config.getExecConfigProp(name, 'onlyJobState', false)
    }

    @Override
    protected boolean pipeLauncherScript() {
        return isFusionEnabled()
    }

    @Override
    boolean isFusionEnabled() {
        return FusionHelper.isFusionEnabled(session)
    }

    @Override
    String getArrayIndexName() {
        return 'SLURM_ARRAY_TASK_ID'
    }

    @Override
    int getArrayIndexStart() {
        return 0
    }

    @Override
    String getArrayTaskId(String jobId, int index) {
        assert jobId, "Missing 'jobId' argument"
        return "${jobId}_${index}"
    }

}