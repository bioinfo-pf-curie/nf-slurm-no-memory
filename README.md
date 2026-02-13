# nf-slurm-no-memory plugin

**Optimize CPU allocation for Slurm clusters with fixed memory-per-CPU policies.**

## Introductions

Some HPC clusters enforce a **fixed memory-per-CPU ratio** and do not allow users to explicitly set memory limits in Slurm job submissions (no --mem or --mem-per-cpu). This plugin dynamically calculates the **optimal CPU count** for each Nextflow process, ensuring efficient resource allocation while respecting cluster constraints.

- **Automatic CPU Calculation**: For each process, the plugin computes the required CPUs based on:
  - The process’s requested CPU count.
  - The memory requirement divided by the cluster’s **memory-per-CPU ratio** (default: **4 GB/CPU**).
- **Resource Efficiency**: Avoids under-allocation (job failures) or over-allocation (wasted resources).


## Installation

From Nextflow Plugin Registry

```bash
nextflow plugin install nf-slurm-no-memory
```

alternatively, you can reference the plugin in the pipeline config:

```
plugins {
    id 'nf-slurm-no-memory'
}
```

<details>
<summary>From Source</summary>


1. Clone this repository:
```bash
git clone https://github.com/bioinfo-pf-curie/nf-slurm-no-memory.git
cd nf-slurm-no-memory

```

2. Build the plugin:
```bash
make assemble
```

3. Install the plugin:
```bash
make install
```

</details>

## Usage

Declare the plugin in your Nextflow pipeline configuration file:

```groovy title="nextflow.config"
plugins {
    id "nf-slurm-no-memory@1.0.0"
}
```

Modify your `executor` to : 
```groovy title="nextflow.config"
process {
    executor = 'slurm-no-memory'
}
```

More details are available in the Nextflow [plugin documentation](https://www.nextflow.io/docs/latest/plugins.html#plugins) and the [configuration guide](https://www.nextflow.io/docs/latest/config.html).

## Params

The memory-per-CPU ratio can be adjusted via the memoryPerCpu variable in your configuration:
```groovy title="nextflow.config"

executor {
    "slurm-no-memory" {
        memoryPerCore = "8"
    }
}
```


# Requirements

    Nextflow 25.10.0 or later
    Java 17 or later

# License

See [COPYING](COPYING) file for license information.

