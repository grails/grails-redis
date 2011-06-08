import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisGrailsPlugin {
    def version = "0.1"
    def grailsVersion = "1.3.6 > *"
    def dependsOn = [:]
    def pluginExcludes = [
            "grails-app/domain/*.groovy",
            "grails-app/controllers/*.groovy"
    ]

    def author = "Ted Naleid"
    def authorEmail = "contact@naleid.com"
    def title = "Redis Plugin"
    def description = '''\\
    The Redis plugin provides integration with a Redis datastore. Redis is a lightning fast 'data structure server'.  The plugin enables a number of memoization techniques to cache results from complex operations in Redis.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/redis"

    def doWithSpring = {
        def redisPoolConfigMap = application.config?.grails?.redis?.poolConfig ?: [:]
        def redisConfigMap = application.config?.grails?.redis ?: [:]

        redisPoolConfig(JedisPoolConfig) {
            // used to set arbitrary config values without calling all of them out here or requiring any of them
            // any property that can be set on RedisPoolConfig can be set here
            redisPoolConfigMap?.each { key, value ->
                delegate.setProperty(key, value)
            }
        }

        redisPool(JedisPool, ref('redisPoolConfig'), redisConfigMap.host ?: 'localhost', redisConfigMap.port ?: 6379) { bean ->
            bean.destroyMethod = 'destroy'
        }
    }

}
