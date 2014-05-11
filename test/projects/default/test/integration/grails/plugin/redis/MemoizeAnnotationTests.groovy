package grails.plugin.redis

import org.junit.Before
import org.junit.Test

class MemoizeAnnotationTests {
	
	def redisService

    @Before
	public void setUp() {
		redisService.flushDB()
	}

    @Test
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
		
		assert redisService."$testKey" == null
		
		// test method
		testInstance.testAnnotatedMethod()
		
		//verify
		assert redisService."$testKey" == 'testValue'

	}		

}
