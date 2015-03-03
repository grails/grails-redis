package grails.plugins.redis

import com.google.gson.GsonBuilder
import grails.test.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Specification

@Integration
class MemoizeObjectAnnotationSpec extends Specification {

    @Autowired
    RedisService redisService
    GsonBuilder gsonBuilder = new GsonBuilder()

    public void setup() {
        redisService.flushDB()
    }

    void testMemoizeAnnotationExpire() {
        given:
        // set up test class
        def testClass = new GroovyClassLoader().parseClass('''
import grails.plugins.redis.*

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

        def testInstance = testClass.newInstance()

        // inject redis service
        testInstance.redisService = redisService
        // inject gsonBuilder service
        testInstance.gsonBuilder = gsonBuilder
        testInstance.key = testKey
        testInstance.expire = testExpire

        when: "create instance of testClass"
        def testResult = testInstance.testAnnotatedMethod('Groovy', 'Author',
                ['Groovy': 'This is the content', 'Testing': 'Testing is important'])

        then:
        //verify stored JSON
        redisService."$testKey" == '''{"author":"Author","title":"Book of Groovy","chapters":[{"title":"Groovy","content":"This is the content","length":19},{"title":"Testing","content":"Testing is important","length":20}]}'''

        //verify returned Book
        testResult.author == "Author"
        testResult.title == "Book of Groovy"
        testResult.chapters.size() == 2
        testResult.chapters[0].title == "Groovy"
        testResult.chapters[1].title == "Testing"
    }

    void testMemoizeSimpleObject() {
        given:
        TestSimpleObject testInstance = new TestSimpleObject(redisService: redisService)
        testInstance.callCount == 0

        when:
        Long testResult = testInstance.testAnnotatedMethod()

        then:
        redisService."${TestSimpleObject.key}" == '''10'''
        testResult == TestSimpleObject.value
        testInstance.callCount == 1

        when:
        testResult = testInstance.testAnnotatedMethod()

        then:
        redisService."${TestSimpleObject.key}" == '''10'''
        testResult == TestSimpleObject.value
        testInstance.callCount == 1
    }
}


class TestSimpleObject {
    def redisService
    def callCount = 0
    public static final key = "TheKey"
    public static final Long value = 10

    @MemoizeObject(key = "#{key}", clazz = Long.class)
    def testAnnotatedMethod() {
        callCount += 1
        return new Long(value)
    }
}