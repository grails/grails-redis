package org.grails.redis

import grails.test.TagLibUnitTestCase

class RedisTagLibTests extends TagLibUnitTestCase {
    protected static KEY = "JedisTagLibUnitTests:memoize"
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
        shouldFail {
            tagLib.memoize([:] as Binding) {-> CONTENTS }
        }
    }

    void testMemoize() {
        tagLib.memoize([key: KEY] as Binding) {-> CONTENTS }
        assertTagLibContentsEquals CONTENTS
    }

    void testMemoizedAlreadyInPreviousTest() {
        tagLib.memoize([key: KEY] as Binding) {-> FAIL_BODY }

        assertTagLibContentsEquals CONTENTS
    }

    def assertTagLibContentsEquals(String expectedContents) {
        def result = tagLib.out.toString()
        assertEquals expectedContents, result
        return result
    }
}

