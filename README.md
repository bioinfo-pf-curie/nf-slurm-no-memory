# nf-slurm-no-memory plugin

## Building

To build the plugin:
```bash
make assemble
```

## Testing with Nextflow

The plugin can be tested without a local Nextflow installation:

1. Build and install the plugin to your local Nextflow installation: `make install`
2. Run a pipeline with the plugin: `nextflow run hello -plugins nf-slurm-no-memory@0.1.0`

## Publishing

Plugins can be published to a central plugin registry to make them accessible to the Nextflow community. 


Follow these steps to publish the plugin to the Nextflow Plugin Registry:

1. Create a file named `$HOME/.gradle/gradle.properties`, where $HOME is your home directory. Add the following properties:

    * `npr.apiKey`: Your Nextflow Plugin Registry access token.

2. Use the following command to package and create a release for your plugin on GitHub: `make release`.


> [!NOTE]
> The Nextflow Plugin registry is currently available as preview technology. Contact info@nextflow.io to learn how to get access to it.
> 


# Donwload plugin

## Install plugin one by one

nextflow plugin install <PLUGIN_NAME>@<VERSION>

## Install all plugin
Run the pipeline once, which will automatically download all plugins required by the pipeline.

## Plugin Store

The plugins are store in $HOME/.nextflow/plugin

[text](https://nextflow.io/docs/latest/plugins/using-plugins.html)