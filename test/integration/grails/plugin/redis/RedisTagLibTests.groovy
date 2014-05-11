package grails.plugin.redis

import org.junit.Before
import org.junit.Test

class RedisTagLibTests {
    protected static KEY = "RedisTagLibTests:memoize"
    protected static CONTENTS = "expected contents"
    protected static FAIL_BODY = "unexpected contents, should not have this"
    def redisService
    def grailsApplication
    def tagLib

    @Before
    public void setUp() {
        redisService.flushDB()
        tagLib = grailsApplication.mainContext.getBean(RedisTagLib.class.name)
    }

    @Test
    public void testMemoize() {
        String result = tagLib.memoize([key: KEY], { -> CONTENTS }).toString()
        assert CONTENTS == result

        result = tagLib.memoize([key: KEY], { -> FAIL_BODY }).toString()
        assert CONTENTS == result // won't find $FAIL_BODY
    }

    @Test
    public void testMemoizeTTL() {
        String result = tagLib.memoize([key:'no-ttl-test'], { -> CONTENTS }).toString()
        assert CONTENTS == result
        assert redisService.NO_EXPIRATION_TTL == redisService.ttl("no-ttl-test")

        result = tagLib.memoize([key:'ttl-test', expire:60], { -> CONTENTS }).toString()
        assert CONTENTS == result
        assert redisService.ttl("ttl-test") > 0
    }
}
