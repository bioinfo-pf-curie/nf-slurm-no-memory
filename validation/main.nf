
process test_output {
    input:
    val protein_name

    output:
    tuple val(protein_name), path("*.sam"), emit: sam
    tuple val(protein_name), path("*.tab"), emit: tab

    script:
    """
    touch ${protein_name}.sam
    touch ${protein_name}.tab
    """
}


workflow {
    main:
    (sam, tab) = test_output(channel.of("protA","protB"))
}