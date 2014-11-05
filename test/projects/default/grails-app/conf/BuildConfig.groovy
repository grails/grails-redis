grails.servlet.version = "3.0"

grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"

grails.project.target.level = 1.6
grails.project.source.level = 1.6

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
    }

    plugins {
        runtime ':hibernate4:4.3.5.2' // or ':hibernate:3.6.10.14'
        test ":build-test-data:2.2.2"
    }
}


grails.plugin.location.redis = "../../.."
