package grails.plugin.redis

class RedisMemoizeTests extends GroovyTestCase {

    def redisService
    def bookService

    protected void setUp() {
        super.setUp()
        redisService.flushDB()
    }

    def testCachedMemoizeOnService(){
        def text = 'hello'
        def date = new Date()
        def outputMemoize = bookService.doWork2(text, date)

        assert outputMemoize == "$text $date"

        Thread.sleep(1000)

        def date2 = new Date()
        def outputMemoize2 = bookService.doWork2(text, date2)

        assert outputMemoize2 != "$text $date2"
        assert outputMemoize2 == "$text $date"
    }

    def testMemoizeAstTransformationOnService(){
        def text = 'hello'
        def date = new Date()
        def outputAst = bookService.doWork(text, date)

        assert outputAst == "$text $date"

        Thread.sleep(1000)

        def date2 = new Date()
        def outputAst2 = bookService.doWork(text, date2)


        assert outputAst2 != "$text $date2"
        assert outputAst2 == "$text $date"
    }
}