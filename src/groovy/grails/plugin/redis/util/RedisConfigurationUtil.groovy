package grails.plugin.redis.util

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisSentinelPool
import redis.clients.jedis.Protocol

/**
* This class provides a closure that can (and must) be used within the context of a BeanBuilder. 
* To wire all redisServices using a custom class do the following
*
* def configureService = RedisConfigurationUtil.configureService
* def redisConfigMap = application.config.grails.redis ?: [:]
*
* configureService.delegate = delegate
* configureService(redisConfigMap, "", MyRedisService)
* redisConfigMap?.connections?.each { connection ->
*   configureService(connection.value, connection?.key?.capitalize(), MyRedisService)
* }
*
*/
class RedisConfigurationUtil {
	
    /**
     * delegate to wire up the required beans.
     */
    static def configureService = {redisConfigMap, key, serviceClass ->
        def poolBean = "redisPoolConfig${key}"
        def validPoolProperties = findValidPoolProperties(log, redisConfigMap.poolConfig)
        "${poolBean}"(JedisPoolConfig) {
            // used to set arbitrary config values without calling all of them out here or requiring any of them
            // any property that can be set on RedisPoolConfig can be set here
            validPoolProperties.each { configKey, value ->
                delegate.setProperty(configKey, value)
            }
        }

        def host = redisConfigMap?.host ?: 'localhost'
        def port = redisConfigMap.containsKey("port") ? "${redisConfigMap.port}" as Integer : Protocol.DEFAULT_PORT
        def timeout = redisConfigMap.containsKey("timeout") ? "${redisConfigMap?.timeout}" as Integer : Protocol.DEFAULT_TIMEOUT
        def password = redisConfigMap?.password ?: null
        def database = redisConfigMap?.database ?: Protocol.DEFAULT_DATABASE
        def sentinels = redisConfigMap?.sentinels ?: null
        def masterName = redisConfigMap?.masterName ?: null

        // If sentinels and a masterName is present, using different pool implementation
        if (sentinels && masterName) {
            if (sentinels instanceof String) {
                sentinels = Eval.me(sentinels.toString())
            }

            if (sentinels instanceof Collection) {
                "redisPool${key}"(JedisSentinelPool, masterName, sentinels as Set, ref(poolBean), timeout, password, database) { bean ->
                    bean.destroyMethod = 'destroy'
                }
            } else {
                throw new RuntimeException('Redis configuraiton property [sentinels] does not appear to be a valid collection.')
            }
        } else {
            "redisPool${key}"(JedisPool, ref(poolBean), host, port, timeout, password, database) { bean ->
                bean.destroyMethod = 'destroy'
            }
        }

        "redisService${key}"(serviceClass) {
            redisPool = ref("redisPool${key}")
        }
    }
    static def findValidPoolProperties(log, ConfigObject properties) {
        def fakeJedisPoolConfig = new JedisPoolConfig()
        properties.findAll{ configKey, value ->
            try {
                fakeJedisPoolConfig[configKey] = value
                return true
            } catch(Exception exception) {
                log.warn "Redis pool configuration parameter (${configKey}) does not exist on JedisPoolConfig or value is the wrong type"
                return false
            }
        }
    }
}
