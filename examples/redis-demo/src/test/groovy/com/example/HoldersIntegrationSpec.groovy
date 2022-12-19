package com.example

import grails.testing.mixin.integration.Integration
import grails.util.Holders
import spock.lang.Specification


@Integration
class HoldersIntegrationSpec extends Specification {

    def "ensure holders wires up getGrailsApplication getMainContext in integration tests"(){
    	when:
        def grailsApplicationMainContext = Holders?.getGrailsApplication()?.getMainContext()

    	then:
        grailsApplicationMainContext?.getBean('redisService')
    }

    def "ensure holders wires up getGrailsApplication getParentContext in integration tests"(){
        when:
        def context = Holders?.getGrailsApplication()?.getParentContext()

        then:
        context?.getBean('redisService')
    }

    def "ensure holders wires up findApplicationContext in integration tests"(){
        when:
        def context = Holders?.findApplicationContext()

        then:
        context?.getBean('redisService')
    }
}