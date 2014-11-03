package grails.plugin.redis.test
import grails.plugin.redis.RedisService
/**
* A simple wrapper around RedisService to enable recording new keys that are set so that they can be removed later.
* The primary purpose of this is to avoid test pollution when running integration tests without flushing redis every time.
* This class is auto-injected during the test phase instead of the base RedisService.  Not all methods that 
* manipulate keys are wrapped as it's less clear what a reasonable default would be for things that maniplate rather
* than just set data.
* 
* in test setup use:
* redisService.recordKeys = true
* to record all keys that are being populated during the test
*
* and in cleanup use
* redisService.rollbackRecordedKeys()
* to delete all of the recorded keys
*
**/
class TestRedisService extends RedisService {

    Set<String> recordedKeys = []
    boolean recordKeys = false

    def memoize(String key, Integer expire, Closure closure) {
        recordKey(key)
        super.memoize(key, expire, closure)
    }

    def memoize(String key, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoize(key, options, closure)
    }

    def memoizeHash(String key, Integer expire, Closure closure) {
        recordKey(key)
        super.memoizeHash(key, expire, closure)
    }

    def memoizeHash(String key, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoizeHash(key, options, closure)
    }

    def memoizeHashField(String key, String field, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoizeHashField(key, field, options, closure)
    }

    def memoizeScore(String key, String member, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoizeScore(key, member, options, closure)
    }

    List memoizeDomainList(Class domainClass, String key, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoizeDomainList(domainClass, key, options, closure)
    }

    List<Long> memoizeDomainIdList(Class domainClass, String key, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoizeDomainIdList(domainClass, key, options, closure)
    }

    def memoizeObject(Class clazz, String key, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoizeObject(clazz, key, options, closure)
    }

    def memoizeList(String key, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoizeList(key, options, closure)
    }

    def memoizeSet(String key, Map options = [:], Closure closure) {
        recordKey(key)
        super.memoizeSet(key, options, closure)
    }

    Long persistDomainId(Long domainId, String key, Integer expire) {
        recordKey(key)
        super.persistDomainId(domainId, key, expire)
    }

    public String set(final String key, String value) {
        recordKey(key)
        super.set(key, value)
    }

    public String set(final String key, final String value, final String nxxx, final String expx, final long time) {
        recordKey(key)
        super.set(key, value, nxxx, expx, time)
    }

    public Long setnx(final String key, final String value) {
        recordKey(key)
        super.setnx(key, value)
    }

    public String setex(final String key, final int seconds, final String value) {
        recordKey(key)
        super.setex(key, seconds, value)
    }

    void recordKey(key) {
        if (recordKeys) { 
            recordedKeys << key
        }
    }

    def rollbackRecordedKeys() {
        withRedis { jedis ->
            jedis.del(recordedKeys as String[])
        }
        recordedKeys = []
        recordKeys = false
    }
}
