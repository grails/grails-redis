grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6

grails.project.dependency.resolution = {
    inherits("global") {
    }
    log "info" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve

    useOrigin true
    repositories {
        /*
        inherits true // Whether to inherit repository definitions from plugins
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenCentral()
        */

        mavenLocal()
    }
    dependencies {
    }

    plugins {
//        compile(':redis-gorm:1.0.0.M8') {
//            excludes('redis')
//        }
    }
}

grails.plugin.location.redis = "../../.."

// need to have the grails-data-mapping installed locally for this to work
grails.plugin.location.'redis-gorm' = "../../../../grails-data-mapping/grails-plugins/redis/"




