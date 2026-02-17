package nextflow.executor.slurm_no_memory

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import nextflow.plugin.BasePlugin
import org.pf4j.PluginWrapper

/**
 * Nextflow plugin for slurm executor without memory
 *
 * @author CUBIC
 */
@Slf4j
@CompileStatic
class SlurmNoMemoryPlugin extends BasePlugin {

    SlurmNoMemoryPlugin(PluginWrapper wrapper) {
        super(wrapper)
    }

    @Override
    void start() {
        super.start()
        log.info "SlurmNoMemory executor plugin started"
    }
}