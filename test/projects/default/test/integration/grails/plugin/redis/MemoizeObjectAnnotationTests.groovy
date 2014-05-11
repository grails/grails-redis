package grails.plugin.redis

import org.junit.Before
import org.junit.Test

class MemoizeObjectAnnotationTests {
	
	def redisService
	def gsonBuilder

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
	def gsonBuilder
	def key
	def expire

	@MemoizeObject(key="#{key}", expire="#{expire}", clazz=Book.class)
	def testAnnotatedMethod(String bookTitle, String bookAuthor, Map chapterMap){
		Book book = new Book(author:bookAuthor, title:"Book of $bookTitle")
		List<Chapter> chapters = []
		chapterMap.each { chapterTitle, chapterContent ->
			chapters << new Chapter(title:chapterTitle, content:chapterContent, length:chapterContent.size())
		}
		book.chapters = chapters
		return book
	}

	private class Book {
		String author
		String title
		List<Chapter> chapters
	}

	private class Chapter {
		String title
		String content
		Integer length
	}

}
''')	
		String testKey = "key123"
		String testExpire = "1000"
		
		// create instance of testClass
		def testInstance = testClass.newInstance()
		
		// inject redis service
		testInstance.redisService = redisService
		// inject gsonBuilder service
		testInstance.gsonBuilder = gsonBuilder
		testInstance.key = testKey
		testInstance.expire = testExpire
		
		// test method
		def testResult = testInstance.testAnnotatedMethod('Groovy', 'Author', 
			['Groovy':'This is the content', 'Testing':'Testing is important'])
		
		//verify stored JSON
		assert redisService."$testKey" == '''{"author":"Author","title":"Book of Groovy","chapters":[{"title":"Groovy","content":"This is the content","length":19},{"title":"Testing","content":"Testing is important","length":20}]}'''
		
		//verify returned Book
		assert testResult.author == "Author"
		assert testResult.title == "Book of Groovy"
		assert testResult.chapters.size() == 2
		assert testResult.chapters[0].title == "Groovy"
		assert testResult.chapters[1].title == "Testing"
	}		

    @Test
	void testMemoizeSimpleObject() {
		TestSimpleObject testInstance = new TestSimpleObject(redisService: redisService)
		assert testInstance.callCount == 0
		
		Long testResult = testInstance.testAnnotatedMethod()
        assert redisService."${TestSimpleObject.key}" == '''10'''
		assert testResult == TestSimpleObject.value
        assert testInstance.callCount == 1

        testResult = testInstance.testAnnotatedMethod()
        assert redisService."${TestSimpleObject.key}" == '''10'''
        assert testResult == TestSimpleObject.value
        assert testInstance.callCount == 1
	}		
}


class TestSimpleObject {
    def redisService
    def callCount = 0
    public static final key = "TheKey"
    public static final Long value = 10

    @MemoizeObject(key="#{key}", clazz=Long.class)
    def testAnnotatedMethod() {
        callCount += 1
        return new Long(value)
    }
}