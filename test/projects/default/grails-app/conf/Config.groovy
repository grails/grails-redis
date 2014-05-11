grails {
    redis {
        poolConfig {
            // pool specific tweaks here
            // for parms see https://github.com/xetorthio/jedis/blob/master/src/main/java/redis/clients/jedis/JedisPoolConfig.java
            // numTestsPerEvictionRun = 4
        }
        port = 6379
        host = "localhost"

        connections {
            one {
                poolConfig {
                    // pool specific tweaks here
                    // for parms see https://github.com/xetorthio/jedis/blob/master/src/main/java/redis/clients/jedis/JedisPoolConfig.java
                    // numTestsPerEvictionRun = 4
                }
                port = 6380
                host = "localhost"
            }
            two {
                poolConfig {
                    // pool specific tweaks here
                    // for parms see https://github.com/xetorthio/jedis/blob/master/src/main/java/redis/clients/jedis/JedisPoolConfig.java
                    // numTestsPerEvictionRun = 4
                }
                port = 6381
                host = "localhost"
            }
        }
    }
}

// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',        // controllers
           'org.codehaus.groovy.grails.web.pages',          // GSP
           'org.codehaus.groovy.grails.web.sitemesh',       // layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping',        // URL mapping
           'org.codehaus.groovy.grails.commons',            // core / classloading
           'org.codehaus.groovy.grails.plugins',            // plugins
           'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
}
