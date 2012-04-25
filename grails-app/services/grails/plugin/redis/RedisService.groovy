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
import redis.clients.jedis.JedisPool
import redis.clients.jedis.Pipeline
import redis.clients.jedis.Transaction

class RedisService {

    public static final int NO_EXPIRATION_TTL = -1

    def redisPool

    boolean transactional = false

    def withPipeline(JedisPool pool, Closure closure) {
        withRedis pool, pipelineClosure(closure)
    }

    def withPipeline(Closure closure) {
        withRedis pipelineClosure(closure)
    }

    def withTransaction(JedisPool pool, Closure closure) {
        withRedis pool, transactionClosure(closure)
    }

    def withTransaction(Closure closure) {
        withRedis transactionClosure(closure)
    }

    def methodMissing(String name, args) {
        log.debug "methodMissing $name"
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
        withRedis(redisPool, closure)
    }

    def withRedis(JedisPool pool, Closure closure) {
        Jedis redis = pool.resource
        try {
            return closure(redis)
        } finally {
            pool.returnResource(redis)
        }
    }

    /**
     * Gets the common pipeline closure for both single and multiple
     * connection setups
     * @param closure Closure to execute against redis
     * @return the surrounding closure for pipeline
     */
    private pipelineClosure(Closure closure) {
        return { Jedis redis ->
            Pipeline pipeline = redis.pipelined()
            closure(pipeline)
            pipeline.sync()
        }
    }

    /**
     * Gets the common transaction closure for both single and multiple
     * connection setups
     * @param closure Closure to execute against redis
     * @return the surrounding closure for transaction
     */
    private transactionClosure(Closure closure) {
        return { Jedis redis ->
            Transaction transaction = redis.multi()
            closure(transaction)
            transaction.exec()
        }
    }

    def memoize(JedisPool pool = redisPool, String key, Integer expire, Closure closure) {
        memoize(pool, key, [expire: expire], closure)
    }

    // SET/GET a value on a Redis key
    def memoize(JedisPool pool = redisPool, String key, Map options = [:], Closure closure) {
        log.debug "using key $key"
        def result = withRedis(pool) { Jedis redis ->
            redis.get(key)
        }

        if(!result) {
            log.debug "cache miss: $key"
            result = closure()
            if(result) withRedis(pool) { Jedis redis ->
                if(options?.expire) {
                    redis.setex(key, options.expire, result as String)
                } else {
                    redis.set(key, result as String)
                }
            }
        } else {
            log.debug "cache hit : $key = $result"
        }
        result
    }

    def memoizeHash(JedisPool pool = redisPool, String key, Integer expire, Closure closure) {
        memoizeHash(pool, key, [expire: expire], closure)
    }

    def memoizeHash(JedisPool pool = redisPool,String key, Map options = [:],  Closure closure) {
        def hash = withRedis(pool) { Jedis redis ->
            redis.hgetAll(key)
        }

        if(!hash) {
            log.debug "cache miss: $key"
            hash = closure()
            if(hash) withRedis(pool) { Jedis redis ->
                redis.hmset(key, hash)
                if(options?.expire) redis.expire(key, options.expire)
            }
        } else {
            log.debug "cache hit : $key = $hash"
        }
        hash
    }

    def memoizeHashField(JedisPool pool = redisPool,String key, String field, Integer expire,Closure closure) {
        memoizeHashField(pool, key, field, [expire: expire], closure)
    }

    // HSET/HGET a value on a Redis hash at key.field
    // if expire is not null it will be the expire for the whole hash, not this value
    // and will only be set if there isn't already a TTL on the hash
    def memoizeHashField(JedisPool pool = redisPool,String key, String field, Map options = [:], Closure closure) {
        def result = withRedis(pool) { Jedis redis ->
            redis.hget(key, field)
        }

        if(!result) {
            log.debug "cache miss: $key.$field"
            result = closure()
            if(result) withRedis(pool) { Jedis redis ->
                redis.hset(key, field, result as String)
                if(options?.expire && redis.ttl(key) == NO_EXPIRATION_TTL) redis.expire(key, options.expire)
            }
        } else {
            log.debug "cache hit : $key.$field = $result"
        }
        result
    }

    def memoizeScore(JedisPool pool = redisPool, String key, String member, Integer expire, Closure closure) {
        memoizeScore(pool, key, member, [expire: expire], closure)
    }

    // set/get a 'double' score within a sorted set
    // if expire is not null it will be the expire for the whole zset, not this value
    // and will only be set if there isn't already a TTL on the zset
    def memoizeScore(JedisPool pool = redisPool,String key, String member, Map options = [:], Closure closure) {
        def score = withRedis(pool) { Jedis redis ->
            redis.zscore(key, member)
        }

        if(!score) {
            log.debug "cache miss: $key.$member"
            score = closure()
            if(score) withRedis(pool) { Jedis redis ->
                redis.zadd(key, score, member)
                if(options?.expire && redis.ttl(key) == NO_EXPIRATION_TTL) redis.expire(key, options.expire)
            }
        } else {
            log.debug "cache hit : $key.$member = $score"
        }
        score
    }

    List memoizeDomainList(JedisPool pool = redisPool, Class domainClass, String key, Integer expire, Closure closure) {
        memoizeDomainList(pool, domainClass, key, [expire: expire], closure)
    }

    List memoizeDomainList(JedisPool pool = redisPool, Class domainClass, String key, Map options = [:], Closure closure) {
        List<Long> idList = getIdListFor(key, pool)
        if(idList) return hydrateDomainObjectsFrom(domainClass, idList)

        def domainList = withRedis(pool) { Jedis redis ->
            closure(redis)
        }

        saveIdListTo(key, domainList, options.expire, pool)

        domainList
    }

    List<Long> memoizeDomainIdList(JedisPool pool = redisPool, Class domainClass, String key, Integer expire, Closure closure) {
        memoizeDomainIdList(pool, domainClass, key, [expire: expire], closure)
    }

    // used when we just want the list of Ids back rather than hydrated objects
    List<Long> memoizeDomainIdList(JedisPool pool = redisPool, Class domainClass, String key, Map options = [:], Closure closure) {
        List<Long> idList = getIdListFor(key, pool)
        if(idList) return idList

        def domainList = closure()

        saveIdListTo(key, domainList, options.expire, pool)

        getIdListFor(key, pool)
    }

    protected List<Long> getIdListFor(String key, JedisPool pool = redisPool) {
        List<String> idList = withRedis(pool) { Jedis redis ->
            redis.lrange(key, 0, -1)
        }

        if(idList) {
            log.debug "$key cache hit, returning ${idList.size()} ids"
            List<Long> idLongList = idList*.toLong()
            return idLongList
        }
    }

    protected void saveIdListTo(String key, List domainList, Integer expire = null, JedisPool pool) {
        log.debug "$key cache miss, memoizing ${domainList?.size() ?: 0} ids"
        withPipeline(pool) { pipeline ->
            for(domain in domainList) {
                pipeline.rpush(key, domain.id as String)
            }
            if(expire) pipeline.expire(key, expire)
        }
    }

    protected List hydrateDomainObjectsFrom(Class domainClass, List<Long> idList) {
        if(domainClass && idList) {
            //return domainClass.findAllByIdInList(idList, [cache: true])
            return idList.collect { id -> domainClass.load(id) }
        }
        []
    }

    def memoizeDomainObject(JedisPool pool = redisPool, Class domainClass, String key, Integer expire, Closure closure) {
        memoizeDomainList(pool, domainClass, key, [expire: expire], closure)
    }

    // closure can return either a domain object or a Long id of a domain object
    // it will be persisted into redis as the Long
    def memoizeDomainObject(JedisPool pool = redisPool, Class domainClass, String key, Map options = [:], Closure closure) {
        Long domainId = withRedis(pool) { redis ->
            redis.get(key)?.toLong()
        }
        if(!domainId) domainId = persistDomainId(pool, closure()?.id as Long, key, options.expire)
        domainClass.load(domainId)
    }

//    Long persistDomainId(Object domainInstance, String key, Map options) {
    //        return persistDomainId(domainInstance?.id as Long, key, options.expire)
    //    }

    Long persistDomainId(JedisPool pool = redisPool, Long domainId, String key, Integer expire) {
        if(domainId) {
            withPipeline(pool) { pipeline ->
                pipeline.set(key, domainId.toString())
                if(expire) pipeline.expire(key, expire)
            }
        }
//        if (domainId) withRedis { Jedis redis -> redis.set(key, domainId.toString()) }
        domainId
    }

    // deletes all keys matching a pattern (see redis "keys" documentation for more)
    // OK for low traffic methods, but expensive compared to other redis commands
    // perf test before relying on this rather than storing your own set of keys to 
    // delete
    void deleteKeysWithPattern(JedisPool pool = redisPool, keyPattern) {
        log.info("Cleaning all redis keys with pattern  [${keyPattern}]")
        withRedis(pool) { Jedis redis ->
            String[] keys = redis.keys(keyPattern)
            if(keys) redis.del(keys)
        }
    }

    def memoizeList(JedisPool pool = redisPool, String key, Integer expire, Closure closure) {
        memoizeList(pool, key, [expire: expire], closure)
    }

    def memoizeList(JedisPool pool = redisPool, String key, Map options = [:], Closure closure) {
        List list = withRedis(pool) { Jedis redis ->
            redis.lrange(key, 0, -1)
        }

        if(!list) {
            log.debug "cache miss: $key"
            list = closure()
            if(list) withPipeline(pool) { pipeline ->
                for(obj in list) { pipeline.rpush(key, obj) }
                if(options?.expire) pipeline.expire(key, options.expire)
            }
        } else {
            log.debug "cach hit: $key"
        }
        list
    }

    def memoizeSet(JedisPool pool = redisPool, String key, Integer expire, Closure closure) {
        memoizeSet(key, [expire: expire], closure)
    }

    def memoizeSet(JedisPool pool = redisPool, String key, Map options = [:], Closure closure) {
        def set = withRedis(pool) { Jedis redis ->
            redis.smembers(key)
        }

        if(!set) {
            log.debug "cache miss: $key"
            set = closure()
            if(set) withPipeline(pool) { pipeline ->
                for(obj in set) { pipeline.sadd(key, obj) }
                if(options?.expire) pipeline.expire(key, options.expire)
            }
        } else {
            log.debug "cach hit: $key"
        }
        set
    }
    // should ONLY Be used from tests unless we have a really good reason to clear out the entire redis db
    def flushDB(JedisPool pool = redisPool) {
        log.warn('flushDB called!')
        withRedis(pool) { Jedis redis ->
            redis.flushDB()
        }
    }
}
