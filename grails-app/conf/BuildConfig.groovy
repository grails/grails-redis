grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"

grails.project.fork = [
        test: false
]

grails.project.dependency.resolver = "maven"

grails.project.dependency.resolution = {
    inherits "global"
    log "warn"
    checksums true
    legacyResolve false


    repositories {
        inherits true

        grailsPlugins()
        grailsHome()
        mavenLocal()
        grailsCentral()
        jcenter()
        mavenCentral()
        mavenRepo "http://repo.spring.io/milestone/"
        mavenRepo "http://oss.sonatype.org/content/repositories/snapshots"
    }

    dependencies {
	    compile 'redis.clients:jedis:2.6.0'
  		compile 'com.google.code.gson:gson:2.2.4'
    }

    plugins {
        build(":release:3.0.1"){
            export = false
        }
        test(':code-coverage:1.2.5') {
            export = false
        }
        test(':codenarc:0.17') {
            export = false
        }
    }
}

if(System.getenv('TRAVIS_BRANCH')) {
    grails.project.repos.grailsCentral.username = System.getenv("GRAILS_CENTRAL_USERNAME")
    grails.project.repos.grailsCentral.password = System.getenv("GRAILS_CENTRAL_PASSWORD")
}

codenarc {
    processTestUnit = false
    processTestIntegration = false
    propertiesFile = 'codenarc.properties'
    ruleSetFiles = "file:grails-app/conf/redis-codenarc.groovy"
    reports = {
        RedisReport('xml') {                    // The report name "MyXmlReport" is user-defined; Report type is 'xml'
            outputFile = 'target/codenarc.xml'  // Set the 'outputFile' property of the (XML) Report
            title = 'Grails Redis Plugin'             // Set the 'title' property of the (XML) Report
        }
    }
}
