package grails.plugin.redis

import static grails.plugin.redis.RedisService.NO_EXPIRATION_TTL

import com.example.Book
import com.example.BookService

class RedisIntegrationTests extends GroovyTestCase {

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

    def testMemoizeDomainList() {
        def book1 = Book.build(title: "book1")
        def book2 = Book.build(title: "book2")
        def book3 = Book.build(title: "book3")

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.findAllByTitleInList(["book1", "book3"])
        }

        def cacheMissList = redisService.memoizeDomainList(Book, "domainkey", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals([book1, book3], cacheMissList)
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("domainkey")

        def cacheHitList = redisService.memoizeDomainList(Book, "domainkey", cacheMissClosure)

        // cache hit, don't call closure again
        assertEquals 1, calledCount
        assertEquals([book1, book3], cacheHitList)
        assertEquals cacheMissList, cacheHitList
    }

    def testMemoizeDomainListWithExpire() {
        def book1 = Book.build(title: "book1")
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("domainkey")
        def result = redisService.memoizeDomainList(Book, "domainkey", 60) { [book1] }
        assertEquals([book1], result)
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("domainkey")
    }

    def testMemoizeDomainIdList() {
        def book1 = Book.build(title: "book1")
        def book2 = Book.build(title: "book2")
        def book3 = Book.build(title: "book3")

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.findAllByTitleInList(["book1", "book3"])
        }

        def cacheMissList = redisService.memoizeDomainIdList(Book, "domainkey", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals([book1.id, book3.id], cacheMissList)

        def cacheHitList = redisService.memoizeDomainIdList(Book, "domainkey", cacheMissClosure)

        // cache hit, don't call closure again
        assertEquals 1, calledCount
        assertEquals([book1.id, book3.id], cacheHitList)
        assertEquals cacheMissList, cacheHitList
    }

    def testMemoizeDomainObject() {
        Book book1 = Book.build(title: "book1")

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.get(book1.id)
        }

        def cacheMissBook = redisService.memoizeDomainObject(Book, "domainkey",  cacheMissClosure)

        assert 1 == calledCount
        assert book1.id == cacheMissBook.id

        def cacheHitBook = redisService.memoizeDomainObject(Book, "domainkey", cacheMissClosure)

        // cache hit, don't call closure again
        assert 1 == calledCount
        assert book1.id == cacheHitBook.id
        assert cacheHitBook.id == cacheMissBook.id
    }
}
