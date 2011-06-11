package org.grails.redis

import redis.clients.jedis.Jedis
import redis.clients.jedis.Pipeline
import redis.clients.jedis.Transaction


class RedisService {

    def redisPool

    boolean transactional = true

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

    // SET/GET a value on a Redis key
    def memoize(String key, Closure closure) {
        withRedis { Jedis redis ->
            def result = redis.get(key)
            if (!result) {
                log.debug "cache miss: $key"
                result = closure(redis)
                if (result) redis.set(key, result as String)
            } else {
                log.debug "cache hit : $key = $result"
            }
            return result
        }
    }

    def memoizeHash(String key, Closure closure) {
        withRedis { Jedis redis ->
            def hash = redis.hgetAll(key)
            if (!hash) {
                log.debug "cache miss: $key"
                hash = closure(redis)
                if (hash) redis.hmset(key, hash)
            } else {
                log.debug "cache hit : $key = $hash"
            }
            return hash
        }
    }

    // set/get a 'double' score within a sorted set
    def memoizeScore(String key, String member, Closure closure) {
        withRedis { Jedis redis ->
            def score = redis.zscore(key, member)
            if (!score) {
                log.debug "cache miss: $key.$member"
                score = closure(redis)
                if (score) redis.zadd(key, score, member)
            } else {
                log.debug "cache hit : $key.$member = $score"
            }
            return score
        }
    }

    // HSET/HGET a value on a Redis hash at key.field
    def memoize(String key, String field, Closure closure) {
        withRedis { Jedis redis ->
            def result = redis.hget(key, field)
            if (!result) {
                log.debug "cache miss: $key.$field"
                result = closure(redis)
                if (result) redis.hset(key, field, result as String)
            } else {
                log.debug "cache hit : $key.$field = $result"
            }
            return result
        }
    }

    List memoizeDomainList(Class domainClass, String key, Closure closure) {
        List<Long> idList = getIdListFor(key)
        if (idList) return hydrateDomainObjectsFrom(domainClass, idList)

        def domainList = withRedis { Jedis redis ->
            closure(redis)
        }

        saveIdListTo(key, domainList)
        return domainList
    }

    // used when we just want the list of Ids back rather than hydrated objects
    List<Long> memoizeDomainIdList(Class domainClass, String key, Closure closure) {
        List<Long> idList = getIdListFor(key)
        if (idList) return idList

        def domainList = withRedis { Jedis redis ->
            closure(redis)
        }
        saveIdListTo(key, domainList)
        return getIdListFor(key)
    }

    List<Long> getIdListFor(String key) {
        List<String> idList = withRedis { Jedis redis ->
            redis.lrange(key, 0, -1)
        }

        if (idList) {
            log.debug "$key cache hit, returning ${idList.size()} ids"
            List<Long> idLongList = idList.collect { String id -> id.toLong() }
            return idLongList
        }
    }

    void saveIdListTo(String key, List domainList) {
        log.debug "$key cache miss, memoizing ${domainList?.size() ?: 0} ids"
        withPipeline { pipeline ->
            for (domain in domainList) {
                pipeline.rpush(key, domain.id as String)
            }
        }
    }

    List hydrateDomainObjectsFrom(Class domainClass, List<Long> idList) {
        if (domainClass && idList) {
            //return domainClass.findAllByIdInList(idList, [cache: true])
            return idList.collect { id -> domainClass.load(id) }
        }
        return []
    }

    // should ONLY Be used from tests unless we have a really good reason to clear out the entire redis db
    def flushDB() {
        log.warn("flushDB called!")
        withRedis { Jedis redis ->
            redis.flushDB()
        }
    }
}
