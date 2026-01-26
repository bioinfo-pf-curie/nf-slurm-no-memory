package cubiccurie.plugin

import nextflow.Session
import spock.lang.Specification

/**
 * Implements a basic factory test
 *
 */
class SlurmNoMemoryObserverTest extends Specification {

    def 'should create the observer instance' () {
        given:
        def factory = new SlurmNoMemoryFactory()
        when:
        def result = factory.create(Mock(Session))
        then:
        result.size() == 1
        result.first() instanceof SlurmNoMemoryObserver
    }

}
