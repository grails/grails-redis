package grails.redis

import grails.test.TagLibUnitTestCase

class RedisTagLibTests extends TagLibUnitTestCase {
    protected static KEY = "RedisTagLibTests:memoize"
    protected static CONTENTS = "expected contents"
    protected static FAIL_BODY = "unexpected contents, should not have this"
    def redisService

    protected void setUp() {
        super.setUp()
        tagLib.redisService = redisService
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testMemoizeMissingRequiredKey() {
        def result = shouldFail {
            tagLib.memoize(createParams([:])) {-> CONTENTS }
        }

        assertEquals "[key] attribute must be specified for memoize!", result
    }

    void testMemoize() {
        tagLib.memoize(createParams([key: KEY])) {-> CONTENTS }
        assertTagLibContentsEquals CONTENTS
    }

    void testMemoizedAlreadyInPreviousTest() {
        tagLib.memoize(createParams([key: KEY])) {-> FAIL_BODY }

        assertTagLibContentsEquals CONTENTS
    }

    void testMemoizeWithoutExpiresHasNoTTL() {
        tagLib.memoize(createParams([key: "no-ttl-test"])) {-> CONTENTS }
        assertTagLibContentsEquals CONTENTS
        assertEquals redisService.NO_EXPIRATION_TTL, redisService.ttl("no-ttl-test") 
    }

    void testMemoizeWithExpireHasTTL() {
        tagLib.memoize(createParams([key: "ttl-test", expire: "60"])) {-> CONTENTS }
        assertTagLibContentsEquals CONTENTS
        assertTrue redisService.ttl("ttl-test") > 0
    }

    def assertTagLibContentsEquals(String expectedContents) {
        def result = tagLib.out.toString()
        assertEquals expectedContents, result
        return result
    }

    Binding createParams(params) {
        return ([key: null, expire: null] + params) as Binding
    }
}

