package grails.plugin.redis

import static grails.plugin.redis.RedisService.NO_EXPIRATION_TTL

import com.example.Book

class RedisIntegrationTests extends GroovyTestCase {

    def redisService

    protected void setUp() {
        super.setUp()
        redisService.flushDB()
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
    
    def testMemoizeSet() {
        def book1 = "book1"
        def book2 = "book2"
        def book3 = "book3"
        def bookSet = [book1, book2, book3] as Set

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return bookSet
        }

        def cacheMissList = redisService.memoizeSet("Books", cacheMissClosure)

        assertEquals 1, calledCount
        assertEquals([book1, book2, book3] as Set, cacheMissList)
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("Books")

        def cacheHitList = redisService.memoizeSet("Books", cacheMissClosure)

        // cache hit, don't call closure again
        assertEquals 1, calledCount
        assertEquals([book1, book2, book3] as Set, cacheHitList)
        assertEquals cacheMissList, cacheHitList
    }

    def testMemoizeSetWithExpire() {
        def book1 = "book1"
        assertEquals NO_EXPIRATION_TTL, redisService.ttl("Books")
        def result = redisService.memoizeSet("Books", 60) { [book1] as Set }
        assertEquals([book1] as Set, result)
        assertTrue NO_EXPIRATION_TTL < redisService.ttl("Books")
    }    
}
