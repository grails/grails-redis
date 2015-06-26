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

import grails.plugin.redis.RedisService
import grails.plugin.redis.util.RedisConfigurationUtil
import grails.util.Environment

class RedisGrailsPlugin {

    def version = "1.6.6"
    def grailsVersion = "2.0.0 > *"
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
            [name: "Christian Oestreich"],
            [name: "John Engelman"],
            [name: "David Seiler"],
            [name: "Jordon Saardchit"],
            [name: "Florian Langenhahn"],
            [name: "German Sancho"],
            [name: "John Mulhern"],
            [name: "Shaun Jurgemeyer"],
            [name: "R.A. Porter"]
    ]

    def pluginExcludes = [
            "codenarc.properties",
            "grails-app/conf/DataSource.groovy",
            "grails-app/conf/redis-codenarc.groovy",
            "grails-app/views/**",
            "grails-app/domain/**",
            "grails-app/services/test/**",
            "test/**",
            "web-app/**"
    ]

    def scm = [url: "https://github.com/grails-plugins/grails-redis"]

    def documentation = "http://grails.org/plugin/redis"

    def doWithSpring = {
        def configureService = RedisConfigurationUtil.configureService
        def redisConfigMap = application.config.grails.redis ?: [:]

        configureService.delegate = delegate
        configureService(redisConfigMap, "", RedisService)
        redisConfigMap?.connections?.each { connection ->
            configureService(connection.value, connection?.key?.capitalize(), RedisService)
        }
    }
}
