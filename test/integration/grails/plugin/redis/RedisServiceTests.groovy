package grails.plugin.redis

import grails.spring.BeanBuilder
import redis.clients.jedis.exceptions.JedisConnectionException

import static grails.plugin.redis.RedisService.NO_EXPIRATION_TTL
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction

class RedisServiceTests extends GroovyTestCase {
    def redisService
    def redisServiceMock
    def grailsApplication

    boolean transactional = false

    protected void setUp() {
        super.setUp()
        redisServiceMock = mockRedisServiceForFailureTest(getNewInstanceOfBean(RedisService))

        try {
            redisService.flushDB()
        }
        catch (JedisConnectionException jce) {
            // swallow connect exception so failure tests can proceed
        }

        assert redisService != redisServiceMock
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

    /**
     * This test method ensures that memoization method succeeds in the event of an unreachable redis store
     */
    void testMemoizeKeyWithoutRedis() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisServiceMock.memoize("mykey", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals "foo", cacheMissResult

        cacheMissResult = redisServiceMock.memoize("mykey", cacheMissClosure)

        assertEquals 2, calledCount
        assertEquals "foo", cacheMissResult
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
        assertTrue 0 > redisService.ttl("mykey")
        def result = redisService.memoize("mykey", 60) { "foo" }
        assertEquals "foo", result
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    /**
     * Test hashfield memoization with an unreachable redis store
     */
    void testMemoizeHashFieldWithoutRedis() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisServiceMock.memoizeHashField("mykey", "first", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals "foo", cacheMissResult

        cacheMissResult = redisServiceMock.memoizeHashField("mykey", "first", cacheMissClosure)

        // should have hit the cache, not called our method again
        assertEquals 2, calledCount
        assertEquals "foo", cacheMissResult
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
        assertTrue 0 > redisService.ttl("mykey")
        def result = redisService.memoizeHashField("mykey", "first", 60) { "foo" }
        assertEquals "foo", result
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    /**
     * Tests hash memoization with an unreachable redis store
     */
    def testMemoizeHashWithoutRedis() {
        def calledCount = 0
        def expectedHash = [foo: 'bar', baz: 'qux']
        def cacheMissClosure = {
            calledCount += 1
            return expectedHash
        }
        def cacheMissResult = redisServiceMock.memoizeHash("mykey", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals expectedHash, cacheMissResult

        def cacheHitResult = redisServiceMock.memoizeHash("mykey", cacheMissClosure)

        // should have hit the cache, not called our method again
        assertEquals 2, calledCount
        assertEquals expectedHash, cacheHitResult
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
        assertTrue 0 > redisService.ttl("mykey")
        def result = redisService.memoizeHash("mykey", 60) { expectedHash }
        assertEquals expectedHash, result
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    /**
     * testing list memoization with an unreachable redis store
     */
    def testMemoizeListWithoutRedis() {
        def book1 = "book1"
        def book2 = "book2"
        def book3 = "book3"
        List books = [book1, book2, book3]

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return books
        }

        def cacheMissList = redisServiceMock.memoizeList("mykey", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals([book1, book2, book3], cacheMissList)

        List cacheHitList = redisServiceMock.memoizeList("mykey", cacheMissClosure)

        // cache hit, don't call closure again
        assertEquals 2, calledCount
        assertEquals([book1, book2, book3], cacheHitList)
        assertEquals cacheMissList, cacheHitList
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
        assertTrue 0 > redisService.ttl("mykey")
        def result = redisService.memoizeList("mykey", 60) { [book1] }
        assertEquals([book1], result)
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    /**
     * tests set memoization with an unreachable redis store
     */
    def testMemoizeSetWithoutRedis() {
        def book1 = "book1"
        def book2 = "book2"
        def book3 = "book3"
        def bookSet = [book1, book2, book3]as Set

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return bookSet
        }

        Set cacheMissSet = redisServiceMock.memoizeSet("mykey", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals([book1, book2, book3] as Set, cacheMissSet)

        def cacheHitSet = redisServiceMock.memoizeSet("mykey", cacheMissClosure)

        // cache hit, don't call closure again
        assertEquals 2, calledCount
        assertEquals([book1, book2, book3] as Set, cacheHitSet)
        assertEquals cacheMissSet, cacheHitSet
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
        assertTrue 0 > redisService.ttl("mykey")
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

    def testWithTransactionClosureException() {
        redisService.withRedis { Jedis redis ->
            assert redis.get("foo") == null
        }

        shouldFail{
            redisService.withTransaction { Transaction transaction ->
                transaction.set("foo", "bar")
                throw new Exception("Something bad happened")
            }
        }

        redisService.withRedis { Jedis redis ->
            assert redis.get("foo") == null
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

    // utility method for assisting in test setup
    def getNewInstanceOfBean(Class clazz) {
        String beanName = "prototype${clazz.name}"
        BeanBuilder beanBuilder = new BeanBuilder(grailsApplication.mainContext)

        beanBuilder.beans {
            "$beanName"(clazz) { bean ->
                bean.autowire = 'byName'
            }
        }

        beanBuilder.createApplicationContext().getBean(beanName)
    }

    def mockRedisServiceForFailureTest(RedisService svc) {
        def redisPoolMock = new Object()
        redisPoolMock.metaClass.getResource =  { ->
            throw new JedisConnectionException('Generated by a mocked redisPool')
        }
        svc.redisPool = redisPoolMock
        return svc
    }
}
