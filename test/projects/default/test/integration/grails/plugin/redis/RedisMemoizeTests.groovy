package grails.plugin.redis

import com.example.BookService

class RedisMemoizeTests extends GroovyTestCase {

    RedisService redisService
    BookService bookService

    protected void setUp() {
        super.setUp()
        redisService.flushDB()
    }

    def testCachedMemoizeOnService(){
        def text = 'hello'
        def date = new Date()
        def outputMemoize = bookService.getMemoizedTextDate(text, date)

        assert outputMemoize == "$text $date"

        Thread.sleep(1000)

        def date2 = new Date()
        def outputMemoize2 = bookService.getMemoizedTextDate(text, date2)

        assert outputMemoize2 != "$text $date2"
        assert outputMemoize2 == "$text $date"
    }

    def testMemoizeAstTransformationOnService(){
        def text = 'hello'
        def date = new Date()
        def outputAst = bookService.getAnnotatedText(text, date)

        assert outputAst == "$text $date"

        Thread.sleep(1000)

        def date2 = new Date()
        def outputAst2 = bookService.getAnnotatedText(text, date2)


        assert outputAst2 != "$text $date2"
        assert outputAst2 == "$text $date"
    }
}