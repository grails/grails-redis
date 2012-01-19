grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {

    inherits "global"
    log "warn"

    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()
//        mavenRepo "http://m2repo.spockframework.org/snapshots"
    }

    dependencies {
        compile 'redis.clients:jedis:2.0.0'
    }

    plugins {
//         test(':spock:0.6-SNAPSHOT') {
        //            export = false
        //        }
        test(':code-coverage:1.2.5') {
            export = false
        }
        test(':codenarc:0.16.1') {
            export = false
        }
    }
}

codenarc {
    processTestUnit = false
    processTestIntegration = false
    propertiesFile = 'codenarc.properties'
    ruleSetFiles = "file:grails-app/conf/redis-codenarc.groovy"
    reports = {
        CxfClientReport('xml') {                    // The report name "MyXmlReport" is user-defined; Report type is 'xml'
            outputFile = 'target/codenarc.xml'  // Set the 'outputFile' property of the (XML) Report
            title = 'Grails Redis Plugin'             // Set the 'title' property of the (XML) Report
        }
    }
}
