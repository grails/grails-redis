package grails.plugin.redis

import groovy.util.GroovyTestCase
import grails.test.GrailsMock

class MemoizeAnnotationTests extends GroovyTestCase {
	
	void testMemoizeAnnotationExpire() {
		 
		// set up test class
		def testClass = new GroovyClassLoader().parseClass(''' 
import grails.plugin.redis.*

class TestClass{
	def redisService
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
		
		// create instance of testClass
		def testInstance = testClass.newInstance()
		
		// create redisService mock
		GrailsMock mockRedisService = new GrailsMock(RedisService)
		mockRedisService.demand.memoize(1) { key, expire, closure ->
			assert key == testKey
			assert expire == Integer.parseInt(testExpire)
			assert closure
			return closure()
		}
		
		// inject redis service
		testInstance.redisService = redisService
		testInstance.key = testKey
		testInstance.expire = testExpire
		
		// test method
		testInstance.testAnnotatedMethod()
		
		//verify mock
		mockRedisService.verify()

	}		

}