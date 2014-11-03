package grails.plugin.redis

import grails.spring.BeanBuilder
import org.junit.Before
import org.junit.Test
import redis.clients.jedis.exceptions.JedisConnectionException

import static grails.plugin.redis.RedisService.NO_EXPIRATION_TTL
import redis.clients.jedis.Jedis
import redis.clients.jedis.Transaction

class RedisServiceTests {
    def redisService
    def redisServiceMock
    def grailsApplication

    boolean transactional = false

    @Before
    public void setUp() {
        redisServiceMock = mockRedisServiceForFailureTest(getNewInstanceOfBean(RedisService))

        try {
            redisService.flushDB()
        }
        catch (JedisConnectionException jce) {
            // swallow connect exception so failure tests can proceed
        }

        assert redisService != redisServiceMock
    }

    @Test
    public void testFlushDB() {
        // actually called as part of setup too, but we can test it here
        redisService.withRedis { Jedis redis ->
            assert 0 == redis.dbSize()
            redis.set("foo", "bar")
            assert 1 == redis.dbSize()
        }

        redisService.flushDB()

        redisService.withRedis { Jedis redis ->
            assert 0 == redis.dbSize()
        }
    }

    /**
     * This test method ensures that memoization method succeeds in the event of an unreachable redis store
     */
    @Test
    public void testMemoizeKeyWithoutRedis() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisServiceMock.memoize("mykey", cacheMissClosure)

        assert 1 == calledCount
        assert "foo" == cacheMissResult

        cacheMissResult = redisServiceMock.memoize("mykey", cacheMissClosure)

        assert 2 == calledCount
        assert "foo" == cacheMissResult
    }

    @Test
    public void testMemoizeKey() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisService.memoize("mykey", cacheMissClosure)

        assert 1 == calledCount
        assert "foo" == cacheMissResult
        assert NO_EXPIRATION_TTL == redisService.ttl("mykey")

        def cacheHitResult = redisService.memoize("mykey", cacheMissClosure)

        // should have hit the cache, not called our method again
        assert 1 == calledCount
        assert "foo" == cacheHitResult
    }

    @Test
    public void testMemoizeKeyWithExpire() {
        assert 0 > redisService.ttl("mykey")
        def result = redisService.memoize("mykey", 60) { "foo" }
        assert "foo" == result
        assert NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    @Test
    public void testMemoizeKeyNullValue() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return null
        }
        def cacheMissResult = redisService.memoize("mykey", cacheMissClosure)

        assert 1 == calledCount
        assert null == cacheMissResult

        def cacheMissAgainResult = redisService.memoize("mykey", cacheMissClosure)

        // should have called the method again if we got a null
        assert 2 == calledCount
        assert null == cacheMissAgainResult
    }

    /**
     * Test hashfield memoization with an unreachable redis store
     */
    @Test
    public void testMemoizeHashFieldWithoutRedis() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisServiceMock.memoizeHashField("mykey", "first", cacheMissClosure)

        assert 1 == calledCount
        assert "foo" == cacheMissResult

        cacheMissResult = redisServiceMock.memoizeHashField("mykey", "first", cacheMissClosure)

        // should have hit the cache, not called our method again
        assert 2 == calledCount
        assert "foo" == cacheMissResult
    }

    @Test
    public void testMemoizeHashField() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foo"
        }
        def cacheMissResult = redisService.memoizeHashField("mykey", "first", cacheMissClosure)

        assert 1 == calledCount
        assert "foo" == cacheMissResult
        assert NO_EXPIRATION_TTL == redisService.ttl("mykey")

        def cacheHitResult = redisService.memoizeHashField("mykey", "first", cacheMissClosure)

        // should have hit the cache, not called our method again
        assert 1 == calledCount
        assert "foo" == cacheHitResult

        def cacheMissSecondResult = redisService.memoizeHashField("mykey", "second", cacheMissClosure)

        // cache miss because we're using a different field in the same key
        assert 2 == calledCount
        assert "foo" == cacheMissSecondResult
    }

    @Test
    public void testMemoizeHashFieldWithExpire() {
        assert 0 > redisService.ttl("mykey")
        def result = redisService.memoizeHashField("mykey", "first", 60) { "foo" }
        assert "foo" == result
        assert NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    /**
     * Tests hash memoization with an unreachable redis store
     */
    @Test
    public void testMemoizeHashWithoutRedis() {
        def calledCount = 0
        def expectedHash = [foo: 'bar', baz: 'qux']
        def cacheMissClosure = {
            calledCount += 1
            return expectedHash
        }
        def cacheMissResult = redisServiceMock.memoizeHash("mykey", cacheMissClosure)

        assert 1 == calledCount
        assert expectedHash == cacheMissResult

        def cacheHitResult = redisServiceMock.memoizeHash("mykey", cacheMissClosure)

        // should have hit the cache, not called our method again
        assert 2 == calledCount
        assert expectedHash == cacheHitResult
    }

    @Test
    public void testMemoizeHash() {
        def calledCount = 0
        def expectedHash = [foo: 'bar', baz: 'qux']
        def cacheMissClosure = {
            calledCount += 1
            return expectedHash
        }
        def cacheMissResult = redisService.memoizeHash("mykey", cacheMissClosure)

        assert 1 == calledCount
        assert expectedHash == cacheMissResult
        assert NO_EXPIRATION_TTL == redisService.ttl("mykey")

        def cacheHitResult = redisService.memoizeHash("mykey", cacheMissClosure)

        // should have hit the cache, not called our method again
        assert 1 == calledCount
        assert expectedHash == cacheHitResult
    }

    @Test
    public void testMemoizeHashWithExpire() {
        def expectedHash = [foo: 'bar', baz: 'qux']
        assert 0 > redisService.ttl("mykey")
        def result = redisService.memoizeHash("mykey", 60) { expectedHash }
        assert expectedHash == result
        assert NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    /**
     * testing list memoization with an unreachable redis store
     */
    @Test
    public void testMemoizeListWithoutRedis() {
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

        assert 1 == calledCount
        assert [book1, book2, book3] == cacheMissList

        List cacheHitList = redisServiceMock.memoizeList("mykey", cacheMissClosure)

        // cache hit, don't call closure again
        assert 2 == calledCount
        assert [book1, book2, book3] == cacheHitList
        assert cacheMissList == cacheHitList
    }

    @Test
    public void testMemoizeList() {
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

        assert 1 == calledCount
        assert [book1, book2, book3] == cacheMissList
        assert NO_EXPIRATION_TTL == redisService.ttl("mykey")
        
        List cacheHitList = redisService.memoizeList("mykey", cacheMissClosure)
        
        // cache hit, don't call closure again
        assert 1 == calledCount
        assert [book1, book2, book3] == cacheHitList
        assert cacheMissList == cacheHitList
    }

    @Test
    public void testMemoizeListWithExpire() {
        def book1 = "book1"
        assert 0 > redisService.ttl("mykey")
        def result = redisService.memoizeList("mykey", 60) { [book1] }
        assert [book1] == result
        assert NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    /**
     * tests set memoization with an unreachable redis store
     */
    @Test
    public void testMemoizeSetWithoutRedis() {
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

        assert 1 == calledCount
        assert [book1, book2, book3] as Set == cacheMissSet

        def cacheHitSet = redisServiceMock.memoizeSet("mykey", cacheMissClosure)

        // cache hit, don't call closure again
        assert 2 == calledCount
        assert [book1, book2, book3] as Set == cacheHitSet
        assert cacheMissSet == cacheHitSet
    }

    @Test
    public void testMemoizeSet() {
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

        assert 1 == calledCount
        assert [book1, book2, book3] as Set == cacheMissSet
        assert NO_EXPIRATION_TTL == redisService.ttl("mykey")
        
        def cacheHitSet = redisService.memoizeSet("mykey", cacheMissClosure)
        
        // cache hit, don't call closure again
        assert 1 == calledCount
        assert [book1, book2, book3] as Set == cacheHitSet
        assert cacheMissSet == cacheHitSet
    }

    @Test
    public void testMemoizeSetWithExpire() {
        def book1 = "book1"
        assert 0 > redisService.ttl("mykey")
        def result = redisService.memoizeSet("mykey", 60) { [book1] as Set }
        assert [book1] as Set == result
        assert NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    @Test
    public void testMemoizeObject_simpleMapOfStrings() {
        Map<String, String> map = [foo: "bar", baz: "qux"]

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            map
        }

        def cacheMissValue = redisService.memoizeObject(Map.class, "mykey", cacheMissClosure)

        assert 1 == calledCount
        assert "bar" == cacheMissValue.foo
        assert "qux" == cacheMissValue.baz
        assert NO_EXPIRATION_TTL == redisService.ttl("mykey")

        def cacheHitValue = redisService.memoizeObject(Map.class, "mykey", cacheMissClosure)

        assert 1 == calledCount
        assert "bar" == cacheHitValue.foo
        assert "qux" == cacheHitValue.baz
    }


    @Test
    public void testMemoizeObject_withTTL() {
        Map<String, String> map = [foo: "bar", baz: "qux"]
        assert 0 > redisService.ttl("mykey")

        def cacheMissValue = redisService.memoizeObject(Map.class, "mykey", 60) { -> map }

        assert "bar" == cacheMissValue.foo
        assert "qux" == cacheMissValue.baz
        assert NO_EXPIRATION_TTL < redisService.ttl("mykey")
    }

    @Test
    public void testMemoizeObject_nullValue() {
        Map<String, String> map = null

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            map
        }

        def cacheMissValue = redisService.memoizeObject(Map.class, "mykey", cacheMissClosure)

        assert 1 == calledCount
        assert null == cacheMissValue

        def cacheHitValue = redisService.memoizeObject(Map.class, "mykey", cacheMissClosure)

        assert 1 == calledCount
        assert null == cacheHitValue
    }

    @Test
    public void testMemoizeObject_nullValue_cacheNullFalse() {
        Map<String, String> map = null

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            map
        }

        def cacheMissValue = redisService.memoizeObject(Map.class, "mykey", [cacheNull: false], cacheMissClosure)

        assert 1 == calledCount
        assert null == cacheMissValue

        def cacheMissAgainValue = redisService.memoizeObject(Map.class, "mykey", [cacheNull: false], cacheMissClosure)

        assert 2 == calledCount
        assert null == cacheMissAgainValue
    }


    @Test
    public void testDeleteKeysWithPattern() {
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return "foobar"
        }
        redisService.memoize("mykey:1", cacheMissClosure)
        redisService.memoize("mykey:2", cacheMissClosure)

        assert 2 == calledCount

        redisService.memoize("mykey:1", cacheMissClosure)
        redisService.memoize("mykey:2", cacheMissClosure)

        // call count shouldn't increase
        assert 2 == calledCount

        redisService.deleteKeysWithPattern("mykey:*")

        redisService.memoize("mykey:1", cacheMissClosure)
        redisService.memoize("mykey:2", cacheMissClosure)

        // Because we deleted those keys before and there is a cache miss
        assert 4 == calledCount
    }

    @Test
    public void testWithTransaction() {
        redisService.withRedis { Jedis redis ->
            assert redis.get("foo") == null
            redisService.withTransaction { Transaction transaction ->
                transaction.set("foo", "bar")
                assert redis.get("foo") == null
            }
            assert "bar" == redis.get("foo")
        }
    }

    @Test
    public void testWithTransactionClosureException() {
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

    @Test
    public void testPropertyMissingGetterRetrievesStringValue() {
        assert redisService.foo == null

        redisService.withRedis { Jedis redis ->
            redis.set("foo", "bar")
        }

        assert "bar" == redisService.foo
    }

    @Test
    public void testPropertyMissingSetterSetsStringValue() {
        redisService.withRedis { Jedis redis ->
            assert redis.foo == null
        }

        redisService.foo = "bar"

        redisService.withRedis { Jedis redis ->
            assert "bar" == redis.foo
        }
    }

    @Test
    public void testMethodMissingDelegatesToJedis() {
        assert redisService.foo == null

        redisService.set("foo", "bar")

        assert "bar" == redisService.foo
    }

    @Test
    public void testClearingRecordedKeys() {

        redisService.recordKeys = false
        redisService.set("foo", "bar")
        redisService.recordKeys = true
        redisService.set("bar", "bar")

        assert "bar" == redisService.foo
        assert "bar" == redisService.bar

        redisService.rollbackRecordedKeys()

        assert "bar" == redisService.foo
        assert null == redisService.bar
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
