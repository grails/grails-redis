package org.grails.redis

import grails.test.*
import redis.clients.jedis.Transaction
import redis.clients.jedis.Jedis

class RedisServiceTests extends GroovyTestCase {
    def redisService

    protected void setUp() {
        super.setUp()
        redisService.flushDB()
    }

    protected void tearDown() {
        super.tearDown()
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

        def cacheHitResult = redisService.memoize("mykey", cacheMissClosure)

        // should have hit the cache, not called our method again
        assertEquals 1, calledCount
        assertEquals "foo", cacheHitResult
    }

    void testMemoizeKeyField() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisService.memoize("mykey", "first", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals "foo", cacheMissResult

        def cacheHitResult = redisService.memoize("mykey", "first", cacheMissClosure)

        // should have hit the cache, not called our method again
        assertEquals 1, calledCount
        assertEquals "foo", cacheHitResult

        def cacheMissSecondResult = redisService.memoize("mykey", "second", cacheMissClosure)

        // cache miss because we're using a different field in the same key
        assertEquals 2, calledCount
        assertEquals "foo", cacheMissSecondResult
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

        def cacheHitResult = redisService.memoizeHash("mykey", cacheMissClosure)

        // should have hit the cache, not called our method again
        assertEquals 1, calledCount
        assertEquals expectedHash, cacheHitResult
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
        def result = shouldFail {
            redisService.methodThatDoesNotExistAndNeverWill()
        }

        assert result?.startsWith("No signature of method: redis.clients.jedis.Jedis.methodThatDoesNotExistAndNeverWill")
    }
}
