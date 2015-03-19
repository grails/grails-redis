package grails.plugins.redis

import grails.core.GrailsApplication
import grails.test.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Specification

@Integration
class RedisTagLibSpec extends Specification {

    @Autowired
    GrailsApplication grailsApplication
    @Autowired
    RedisService redisService
    RedisTagLib tagLib

    protected static KEY = "RedisTagLibTests:memoize"
    protected static CONTENTS = "expected contents"
    protected static FAIL_BODY = "unexpected contents, should not have this"

    def setup() {
        redisService.flushDB()
        tagLib = grailsApplication.mainContext.getBean(RedisTagLib)
    }

    @Ignore
    def testMemoize() {
        when:
        String result = tagLib.memoize([key: KEY], { -> CONTENTS })

        then:
        CONTENTS == result

        when:
        result = tagLib.memoize([key: KEY], { -> FAIL_BODY })
        then:
        CONTENTS == result // won't find $FAIL_BODY
    }

    @Ignore
    def testMemoizeTTL() {
        when:
        String result = tagLib.memoize([key: 'no-ttl-test'], { -> CONTENTS }).toString()

        then:
        CONTENTS == result
        redisService.NO_EXPIRATION_TTL == redisService.ttl("no-ttl-test")

        when:
        result = tagLib.memoize([key: 'ttl-test', expire: 60], { -> CONTENTS }).toString()

        then:
        CONTENTS == result
        redisService.ttl("ttl-test") > 0
    }
}
