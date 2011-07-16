package grails.plugin.redis

import grails.test.*
import com.example.Book
import static grails.plugin.redis.RedisService.NO_EXPIRATION_TTL

class RedisIntegrationTests extends GroovyTestCase {
    
    def redisService

    protected void setUp() {
        super.setUp()
        redisService.flushDB()
    }

    protected void tearDown() {
        super.tearDown()
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
}
