package grails.plugin.redis

import static grails.plugin.redis.RedisService.NO_EXPIRATION_TTL
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction

class RedisServiceTests extends GroovyTestCase {
    def redisService

    boolean transactional = false

    protected void setUp() {
        super.setUp()
        redisService.flushDB()
    }

    void testFlushDB() {
        // actually called as part of setup too, but we can test it here
        redisService.withRedis { Jedis redis ->
            assertEquals 0, redis.dbSize()
            redis.set("foo", "bar")
            assertEquals 1, redis.dbSize()
        }

        redisService.flushDB()

        redisService.withRedis { Jedis redis ->
            assertEquals 0, redis.dbSize()
        }
    }

    void testMemoizeKey() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisService.memoize("mykey", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals "foo", cacheMissResult
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")

        def cacheHitResult = redisService.memoize("mykey", cacheMissClosure)

        // should have hit the cache, not called our method again
        assertEquals 1, calledCount
        assertEquals "foo", cacheHitResult
    }

    void testMemoizeKeyWithExpire() {
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")
        def result = redisService.memoize("mykey", 60) { "foo" }
        assertEquals "foo", result
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    void testMemoizeHashField() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisService.memoizeHashField("mykey", "first", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals "foo", cacheMissResult
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")

        def cacheHitResult = redisService.memoizeHashField("mykey", "first", cacheMissClosure)

        // should have hit the cache, not called our method again
        assertEquals 1, calledCount
        assertEquals "foo", cacheHitResult

        def cacheMissSecondResult = redisService.memoizeHashField("mykey", "second", cacheMissClosure)

        // cache miss because we're using a different field in the same key
        assertEquals 2, calledCount
        assertEquals "foo", cacheMissSecondResult
    }

    void testMemoizeHashFieldWithExpire() {
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")
        def result = redisService.memoizeHashField("mykey", "first", 60) { "foo" }
        assertEquals "foo", result
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    def testMemoizeHash() {
        def calledCount = 0
        def expectedHash = [foo: 'bar', baz: 'qux']
        def cacheMissClosure = {
            calledCount += 1
            return expectedHash
        }
        def cacheMissResult = redisService.memoizeHash("mykey", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals expectedHash, cacheMissResult
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")

        def cacheHitResult = redisService.memoizeHash("mykey", cacheMissClosure)

        // should have hit the cache, not called our method again
        assertEquals 1, calledCount
        assertEquals expectedHash, cacheHitResult
    }

    void testMemoizeHashWithExpire() {
        def expectedHash = [foo: 'bar', baz: 'qux']
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")
        def result = redisService.memoizeHash("mykey", 60) { expectedHash }
        assertEquals expectedHash, result
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    def testMemoizeList() {
        def book1 = "book1"
        def book2 = "book2"
        def book3 = "book3"
        List books = [book1, book2, book3] 
        
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return books
        }
        
        def cacheMissList = redisService.memoizeList("mykey", cacheMissClosure)
        
        assertEquals 1, calledCount
        assertEquals([book1, book2, book3], cacheMissList)
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")
        
        List cacheHitList = redisService.memoizeList("mykey", cacheMissClosure)
        
        // cache hit, don't call closure again
        assertEquals 1, calledCount
        assertEquals([book1, book2, book3], cacheHitList)
        assertEquals cacheMissList, cacheHitList
    }

    def testMemoizeListWithExpire() {
        def book1 = "book1"
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")
        def result = redisService.memoizeList("mykey", 60) { [book1] }
        assertEquals([book1], result)
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }
    

    def testMemoizeSet() {
        def book1 = "book1"
        def book2 = "book2"
        def book3 = "book3"
        def bookSet = [book1, book2, book3]as Set
        
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return bookSet
        }
        
        Set cacheMissSet = redisService.memoizeSet("mykey", cacheMissClosure)
        
        assertEquals 1, calledCount
        assertEquals([book1, book2, book3] as Set, cacheMissSet)
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")
        
        def cacheHitSet = redisService.memoizeSet("mykey", cacheMissClosure)
        
        // cache hit, don't call closure again
        assertEquals 1, calledCount
        assertEquals([book1, book2, book3] as Set, cacheHitSet)
        assertEquals cacheMissSet, cacheHitSet
    }

    def testMemoizeSetWithExpire() {
        def book1 = "book1"
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("mykey")
        def result = redisService.memoizeSet("mykey", 60) { [book1] as Set }
        assertEquals([book1] as Set, result)
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }


    void testDeleteKeysWithPattern() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foobar"
        }
        redisService.memoize("mykey:1", cacheMissClosure)
        redisService.memoize("mykey:2", cacheMissClosure)

        assertEquals 2, calledCount

        redisService.memoize("mykey:1", cacheMissClosure)
        redisService.memoize("mykey:2", cacheMissClosure)

        // call count shouldn't increase
        assertEquals 2, calledCount

        redisService.deleteKeysWithPattern("mykey:*")

        redisService.memoize("mykey:1", cacheMissClosure)
        redisService.memoize("mykey:2", cacheMissClosure)

        // Because we deleted those keys before and there is a cache miss
        assertEquals 4, calledCount
    }

    def testWithTransaction() {
        redisService.withRedis { Jedis redis ->
            assertNull redis.get("foo")
            redisService.withTransaction { Transaction transaction ->
                transaction.set("foo", "bar")
                assertNull redis.get("foo")
            }
            assertEquals "bar", redis.get("foo")
        }
    }

    def testPropertyMissingGetterRetrievesStringValue() {
        assertNull redisService.foo

        redisService.withRedis { Jedis redis ->
            redis.set("foo", "bar")
        }

        assertEquals "bar", redisService.foo
    }

    def testPropertyMissingSetterSetsStringValue() {
        redisService.withRedis { Jedis redis ->
            assertNull redis.foo
        }

        redisService.foo = "bar"

        redisService.withRedis { Jedis redis ->
            assertEquals "bar", redis.foo
        }
    }

    def testMethodMissingDelegatesToJedis() {
        assertNull redisService.foo

        redisService.set("foo", "bar")

        assertEquals "bar", redisService.foo
    }

    def testMethodNotOnJedisThrowsMethodMissingException() {
        def result = shouldFail { redisService.methodThatDoesNotExistAndNeverWill() }

        assert result?.startsWith("No signature of method: redis.clients.jedis.Jedis.methodThatDoesNotExistAndNeverWill")
    }
}
