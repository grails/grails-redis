grails.doc.authors = 'Ted Naleid, Graeme Rocher, Burt Beckwith'
grails.doc.license = 'Apache License 2.0'
grails.doc.title = 'Redis Plugin'

grails.release.scm.enabled=false
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"

grails {
    redis {
        //intentionally leaving both a good and a bad config parameter for the plugin config to exercise the
        //pool configuration code
        poolConfig {
            maxIdle = 10
            doesnotexist = true
        }
    }
}