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
package grails.plugin.redis

class RedisTagLib {

    def redisService

    static namespace = 'redis'

    def memoize = { attrs, body ->
        String key = attrs.key
        if (!key) throw new IllegalArgumentException('[key] attribute must be specified for memoize!')

        Integer expire = attrs.expire?.toInteger()

        out << redisService.memoize(key, expire) { body() ?: '' }
    }
}
