package redis

import grails.plugins.Plugin
import grails.plugins.redis.RedisService
import grails.plugins.redis.util.RedisConfigurationUtil
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisSentinelPool
import redis.clients.jedis.Protocol

class RedisGrailsPlugin extends Plugin {

    def grailsVersion = "3.0.0.BUILD-SNAPSHOT > *"
    def pluginExcludes = [
            "codenarc.properties",
            "grails-app/conf/DataSource.groovy",
            "grails-app/conf/redis-codenarc.groovy",
            "grails-app/views/**",
            "grails-app/domain/**",
            "grails-app/services/test/**",
            "test/**"
    ]

    def title = "Redis Plugin" // Headline display name of the plugin
    def author = "Ted Naleid"
    def authorEmail = "contact@naleid.com"

    def description = '''The Redis plugin provides integration with a Redis datastore. Redis is a lightning fast 'data structure server'.  The plugin enables a number of memoization techniques to cache results from complex operations in Redis.'''
    def issueManagement = [system: 'github', url: 'https://github.com/grails-plugins/grails-redis/issues']
    def scm = [url: "https://github.com/grails-plugins/grails-redis"]
    def documentation = "http://grails.org/plugin/grails-redis"
    def license = "APACHE"

    def developers = [
            [name: "Burt Beckwith"],
            [name: "Brian Coles"],
            [name: "Michael Cameron"],
            [name: "Christian Oestreich"],
            [name: "John Engelman"],
            [name: "David Seiler"],
            [name: "Jordon Saardchit"],
            [name: "Florian Langenhahn"],
            [name: "German Sancho"],
            [name: "John Mulhern"],
            [name: "Shaun Jurgemeyer"]]

    Closure doWithSpring() {
        { ->
            def redisConfigMap = grailsApplication.config.grails.redis ?: [:]

            RedisConfigurationUtil.configureService(delegate, redisConfigMap, "", RedisService)
            redisConfigMap?.connections?.each { connection ->
                RedisConfigurationUtil.configureService(delegate, connection.value, connection?.key?.capitalize(), RedisService)
            }
        }
    }

//    def configureServiceInline(builder, redisConfigMap, key, serviceClass){
//        builder.with {
//            def poolBean = "redisPoolConfig${key}"
//            def validPoolProperties = findValidPoolPropertiesInline(null, redisConfigMap.poolConfig)
//            "${poolBean}"(JedisPoolConfig) {
//                // used to set arbitrary config values without calling all of them out here or requiring any of them
//                // any property that can be set on RedisPoolConfig can be set here
////                validPoolProperties.each { configKey, value ->
////                    delegate.setProperty(configKey, value)
////                }
//            }
//
//            def host = redisConfigMap?.host ?: 'localhost'
//            def port = redisConfigMap.containsKey("port") ? "${redisConfigMap.port}" as Integer : Protocol.DEFAULT_PORT
//            def timeout = redisConfigMap.containsKey("timeout") ? "${redisConfigMap?.timeout}" as Integer : Protocol.DEFAULT_TIMEOUT
//            def password = redisConfigMap?.password ?: null
//            def database = redisConfigMap?.database ?: Protocol.DEFAULT_DATABASE
//            def sentinels = redisConfigMap?.sentinels ?: null
//            def masterName = redisConfigMap?.masterName ?: null
//
//            // If sentinels and a masterName is present, using different pool implementation
//            if (sentinels && masterName) {
//                "redisPool${key}"(JedisSentinelPool, masterName, sentinels as Set, ref(poolBean), timeout, password, database) { bean ->
//                    bean.destroyMethod = 'destroy'
//                }
//            } else {
//                "redisPool${key}"(JedisPool, ref(poolBean), host, port, timeout, password, database) { bean ->
//                    bean.destroyMethod = 'destroy'
//                }
//            }
//
//            "redisService${key}"(serviceClass) { bean ->
//                redisPool = ref("redisPool${key}")
//            }
//        }
//    }
//
//    def findValidPoolPropertiesInline(log, ConfigObject properties) {
//        def fakeJedisPoolConfig = new JedisPoolConfig()
//        properties.findAll{ configKey, value ->
//            try {
//                fakeJedisPoolConfig[configKey] = value
//                return true
//            } catch(Exception exception) {
//                //log.warn "Redis pool configuration parameter (${configKey}) does not exist on JedisPoolConfig or value is the wrong type"
//                return false
//            }
//        }
//    }
}
