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

class RedisGrailsPlugin {
    def version = "1.0.0.M7"
    def grailsVersion = "1.3.4 > *"
    def author = "Ted Naleid"
    def authorEmail = "contact@naleid.com"
    def title = "Redis Plugin"
    def description = '''\\
    The Redis plugin provides integration with a Redis datastore. Redis is a lightning fast 'data structure server'.  The plugin enables a number of memoization techniques to cache results from complex operations in Redis.
'''

    def documentation = "http://grails.org/plugin/redis"

    def doWithSpring = {
        def redisConfigMap = application.config.grails.redis

        redisPoolConfig(JedisPoolConfig) {
            // used to set arbitrary config values without calling all of them out here or requiring any of them
            // any property that can be set on RedisPoolConfig can be set here
            redisConfigMap.poolConfig.each { key, value ->
                delegate.setProperty(key, value)
            }
        }

        redisPool(JedisPool, ref('redisPoolConfig'), redisConfigMap.host ?: 'localhost', redisConfigMap.port ?: 6379) { bean ->
            bean.destroyMethod = 'destroy'
        }
    }
}
