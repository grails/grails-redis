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

import redis.clients.jedis.Jedis
import redis.clients.jedis.Pipeline
import redis.clients.jedis.Transaction

class RedisService {

    public static final int NO_EXPIRATION_TTL = -1

    def redisPool

    boolean transactional = false

    def withPipeline(Closure closure) {
        withRedis { Jedis redis ->
            Pipeline pipeline = redis.pipelined()
            closure(pipeline)
            pipeline.sync()
        }
    }

    def withTransaction(Closure closure) {
        withRedis { Jedis redis ->
            Transaction transaction = redis.multi()
            closure(transaction)
            transaction.exec()
        }
    }

    def methodMissing(String name, args) {
        println "methodMissing $name"
        withRedis { Jedis redis ->
            redis.invokeMethod(name, args)
        }
    }

    void propertyMissing(String name, Object value) {
        withRedis { Jedis redis ->
            redis.set(name, value.toString())
        }
    }

    Object propertyMissing(String name) {
        withRedis { Jedis redis ->
            redis.get(name)
        }
    }

    def withRedis(Closure closure) {
        Jedis redis = redisPool.getResource()
        try {
            return closure(redis)
        } finally {
            redisPool.returnResource(redis)
        }
    }

    def memoize(String key, Integer expire, Closure closure) {
        memoize(key, [expire: expire], closure)
    }

    // SET/GET a value on a Redis key
    def memoize(String key, Map options = [:], Closure closure) {
        println "using key $key"
        def result = withRedis { Jedis redis ->
            redis.get(key)
        }

        if (!result) {
            log.debug "cache miss: $key"
            result = closure()
            if (result) withRedis { Jedis redis ->
                if (!options?.expire) {
                    redis.set(key, result as String)
                } else {
                    redis.setex(key, options.expire, result as String)
                }
            }
        } else {
            log.debug "cache hit : $key = $result"
        }
        return result
    }

    def memoizeHash(String key, Integer expire, Closure closure) {
        memoizeHash(key, [expire: expire], closure)
    }

    def memoizeHash(String key, Map options = [:], Closure closure) {
        def hash = withRedis { Jedis redis ->
            redis.hgetAll(key)
        }

        if (!hash) {
            log.debug "cache miss: $key"
            hash = closure()
            if (hash) withRedis { Jedis redis ->
                redis.hmset(key, hash)
                if (options?.expire) redis.expire(key, options.expire)
            }
        } else {
            log.debug "cache hit : $key = $hash"
        }
        return hash
    }

    def memoizeHashField(String key, String field, Integer expire, Closure closure) {
        memoizeHashField(key, field, [expire: expire], closure)
    }

    // HSET/HGET a value on a Redis hash at key.field
    // if expire is not null it will be the expire for the whole hash, not this value
    // and will only be set if there isn't already a TTL on the hash
    def memoizeHashField(String key, String field, Map options = [:], Closure closure) {
        def result = withRedis { Jedis redis ->
            redis.hget(key, field)
        }

        if (!result) {
            log.debug "cache miss: $key.$field"
            result = closure()
            if (result) withRedis { Jedis redis ->
                redis.hset(key, field, result as String)
                if (options?.expire && redis.ttl(key) == NO_EXPIRATION_TTL) redis.expire(key, options.expire)
            }
        } else {
            log.debug "cache hit : $key.$field = $result"
        }
        return result
    }

    def memoizeScore(String key, String member, Integer expire, Closure closure) {
        memoizeScore(key, member, [expire: expire], closure)
    }

    // set/get a 'double' score within a sorted set
    // if expire is not null it will be the expire for the whole zset, not this value
    // and will only be set if there isn't already a TTL on the zset
    def memoizeScore(String key, String member, Map options = [:], Closure closure) {
        def score = withRedis { Jedis redis ->
            redis.zscore(key, member)
        }

        if (!score) {
            log.debug "cache miss: $key.$member"
            score = closure()
            if (score) withRedis { Jedis redis ->
                redis.zadd(key, score, member)
                if (options?.expire && redis.ttl(key) == NO_EXPIRATION_TTL) redis.expire(key, options.expire)
            }
        } else {
            log.debug "cache hit : $key.$member = $score"
        }
        return score
    }

    List memoizeDomainList(Class domainClass, String key, Integer expire, Closure closure) {
        memoizeDomainList(domainClass, key, [expire: expire], closure)
    }

    List memoizeDomainList(Class domainClass, String key, Map options = [:], Closure closure) {
        List<Long> idList = getIdListFor(key)
        if (idList) return hydrateDomainObjectsFrom(domainClass, idList)

        def domainList = withRedis { Jedis redis ->
            closure(redis)
        }

        saveIdListTo(key, domainList, options.expire)

        return domainList
    }

    List<Long> memoizeDomainIdList(Class domainClass, String key, Integer expire, Closure closure) {
        memoizeDomainIdList(domainClass, key, [expire: expire], closure)
    }

    // used when we just want the list of Ids back rather than hydrated objects
    List<Long> memoizeDomainIdList(Class domainClass, String key, Map options = [:], Closure closure) {
        List<Long> idList = getIdListFor(key)
        if (idList) return idList

        def domainList = closure()

        saveIdListTo(key, domainList, options.expire)

        return getIdListFor(key)
    }

    protected List<Long> getIdListFor(String key) {
        List<String> idList = withRedis { Jedis redis ->
            redis.lrange(key, 0, -1)
        }

        if (idList) {
            log.debug "$key cache hit, returning ${idList.size()} ids"
            List<Long> idLongList = idList.collect { String id -> id.toLong() }
            return idLongList
        }
    }

    protected void saveIdListTo(String key, List domainList, Integer expire = null) {
        log.debug "$key cache miss, memoizing ${domainList?.size() ?: 0} ids"
        withPipeline { pipeline ->
            for (domain in domainList) {
                pipeline.rpush(key, domain.id as String)
            }
            if (expire) pipeline.expire(key, expire)
        }
    }

    protected List hydrateDomainObjectsFrom(Class domainClass, List<Long> idList) {
        if (domainClass && idList) {
            //return domainClass.findAllByIdInList(idList, [cache: true])
            return idList.collect { id -> domainClass.load(id) }
        }
        return []
    }

    // closure can return either a domain object or a Long id of a domain object
    // it will be persisted into redis as the Long
    def memoizeDomainObject(Class domainClass, String key, Closure closure) {
        Long domainId = withRedis { redis ->
            redis.get(key)?.toLong()
        }
        if (!domainId) domainId = persistDomainId(closure(), key)
        return domainClass.load(domainId)
    }

    Long persistDomainId(Object domainInstance, String key) {
        return persistDomainId(domainInstance?.id as Long, key)
    }

    Long persistDomainId(Long domainId, String key) {
        if (domainId) withRedis { Jedis redis -> redis.set(key, domainId.toString()) }
        return domainId
    }

    // deletes all keys matching a pattern (see redis "keys" documentation for more)
    // OK for low traffic methods, but expensive compared to other redis commands
    // perf test before relying on this rather than storing your own set of keys to 
    // delete
    void deleteKeysWithPattern(keyPattern) {
        log.info("Cleaning all redis keys with pattern  [${keyPattern}]")
        withRedis { Jedis redis ->
            String[] keys = redis.keys(keyPattern)
            if (keys) redis.del(keys)
        }
    }

    def memoizeList(String key, Integer expire, Closure closure) {
        memoizeList(key, [expire: expire], closure)
    }

    def memoizeList(String key, Map options = [:], Closure closure) {
        List list = withRedis { Jedis redis ->
            redis.lrange(key, 0, -1)
        }

        if (!list) {
            log.debug "cache miss: $key"
            list = closure()
            if (list) withPipeline { pipeline -> 
                for ( obj in list ) pipeline.rpush(key, obj)
                if (options?.expire) pipeline.expire(key, options.expire)
            }
        } else {
            log.debug "cach hit: $key"
        }
        return list
    }

    def memoizeSet(String key, Integer expire, Closure closure) {
        memoizeSet(key, [expire: expire], closure)
    }

    def memoizeSet(String key, Map options = [:], Closure closure) {
        def set = withRedis { Jedis redis ->
            redis.smembers(key)
        }

        if (!set) {
            log.debug "cache miss: $key"
            set = closure()
            if (set) withPipeline { pipeline ->
                for (obj in set) pipeline.sadd(key, obj)
                if (options?.expire) pipeline.expire(key, options.expire)
            }
        } else {
            log.debug "cach hit: $key"
        }
        return set
    }
    // should ONLY Be used from tests unless we have a really good reason to clear out the entire redis db
    def flushDB() {
        log.warn("flushDB called!")
        withRedis { Jedis redis ->
            redis.flushDB()
        }
    }
}
