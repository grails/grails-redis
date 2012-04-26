/* Copyright (C) 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.Protocol
import grails.plugin.redis.RedisService

class RedisGrailsPlugin {

    def version = "1.3"
    def grailsVersion = "1.3.4 > *"
    def author = "Ted Naleid"
    def authorEmail = "contact@naleid.com"
    def title = "Redis Plugin"

    def description = '''The Redis plugin provides integration with a Redis datastore. Redis is a lightning fast 'data structure server'.  The plugin enables a number of memoization techniques to cache results from complex operations in Redis.
'''
    def issueManagement = [system: 'github', url: 'https://github.com/grails-plugins/grails-redis/issues']

    def license = "APACHE"
    def developers = [
            [name: "Burt Beckwith"],
            [name: "Brian Coles"],
            [name: "Michael Cameron"],
            [name: "Christian Oestreich"]
    ]

    def pluginExcludes = [
            "codenarc.properties",
            "grails-app/conf/DataSource.groovy",
            "grails-app/conf/redis-codenarc.groovy",
            "grails-app/views/**",
            "grails-app/domain/**",
            "grails-app/services/test/**",
            "test/**"

    ]

    def scm = [url: "https://github.com/grails-plugins/grails-redis"]

    def documentation = "http://grails.org/plugin/redis"

    def doWithSpring = {
        def redisConfigMap = application.config.grails.redis ?: [:]

        redisPoolConfig(JedisPoolConfig) {
            // used to set arbitrary config values without calling all of them out here or requiring any of them
            // any property that can be set on RedisPoolConfig can be set here
            redisConfigMap.poolConfig.each { key, value ->
                delegate.setProperty(key, value)
            }
        }

        def host = redisConfigMap.host ?: 'localhost'
        def port = redisConfigMap.port ?: Protocol.DEFAULT_PORT
        def timeout = redisConfigMap.timeout ?: Protocol.DEFAULT_TIMEOUT
        def password = redisConfigMap.password ?: null

        redisPool(JedisPool, ref('redisPoolConfig'), host, port, timeout, password) { bean ->
            bean.destroyMethod = 'destroy'
        }


        redisConfigMap?.connections?.each { connection ->
            String poolBean = 'redisPoolConfig'
            if(connection?.value?.poolConfig) {
                poolBean = "redisPoolConfig${connection.key}"
                "redisPoolConfig${connection.key}"(JedisPoolConfig) {
                    // used to set arbitrary config values without calling all of them out here or requiring any of them
                    // any property that can be set on RedisPoolConfig can be set here
                    connection.value.poolConfig.each { key, value ->
                        delegate.setProperty(key, value)
                    }
                }
            }

            host = connection?.value?.host ?: 'localhost'
            port = connection?.value?.port ?: Protocol.DEFAULT_PORT
            timeout = connection?.value?.timeout ?: Protocol.DEFAULT_TIMEOUT
            password = connection?.value?.password ?: null

            "redisPool${connection.key}"(JedisPool, ref(poolBean), host, port, timeout, password) { bean ->
                bean.destroyMethod = 'destroy'
            }

            "redisService${connection.key}"(RedisService){
                redisPool = ref("redisPool${connection.key}")
            }
        }
    }
}
