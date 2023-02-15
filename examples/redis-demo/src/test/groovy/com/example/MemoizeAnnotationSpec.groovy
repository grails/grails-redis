package com.example

import grails.plugins.redis.RedisService
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
class MemoizeAnnotationSpec extends Specification {

    @Autowired RedisService redisService

    void setup() {
        redisService.flushDB()
    }

    void testMemoizeAnnotationExpire() {
        given:
        // set up test class
        def testClass = new GroovyClassLoader().parseClass('''
import grails.plugins.redis.*

class TestClass{
    RedisService redisService

	def key
	def expire

	@Memoize(key="#{key}", expire="#{expire}")
	def testAnnotatedMethod(){
		return "testValue"
	}
}
''')
        String testKey = "key123"
        String testExpire = "1000"

        when:
        def testInstance = testClass.newInstance()

        // inject redis service
//        testInstance.redisService = redisService
        testInstance.key = testKey
        testInstance.expire = testExpire

        then:
        redisService.get("$testKey") == null

        when:
        def output = testInstance.testAnnotatedMethod()

        then:
        output == 'testValue'
        redisService.get("$testKey") == 'testValue'

    }

}
