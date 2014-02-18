package grails.plugin.redis

import grails.test.GrailsMock

class MemoizeAnnotationTests extends GroovyTestCase {
	
	def redisService
	
	protected void setUp() {
		super.setUp()
		redisService.flushDB()
	}
	
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
		
		// inject redis service
		testInstance.redisService = redisService
		testInstance.key = testKey
		testInstance.expire = testExpire
		
		assert redisService."$testKey" == null
		
		// test method
		testInstance.testAnnotatedMethod()
		
		//verify
		assert redisService."$testKey" == 'testValue'

	}		

}