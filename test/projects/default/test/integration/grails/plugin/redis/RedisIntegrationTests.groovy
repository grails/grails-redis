package grails.plugin.redis

import com.example.Book
import org.junit.Before
import org.junit.Test

import static grails.plugin.redis.RedisService.KEY_DOES_NOT_EXIST
import static grails.plugin.redis.RedisService.NO_EXPIRATION_TTL

class RedisIntegrationTests {

    RedisService redisService

    @Before
    public void setUp() {
        redisService.flushDB()
    }

    @Test
    public void testMemoizeDomainList() {
        def book1 = Book.build(title: "book1")
        def book2 = Book.build(title: "book2")
        def book3 = Book.build(title: "book3")

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.findAllByTitleInList(["book1", "book3"])
        }

        def cacheMissList = redisService.memoizeDomainList(Book, "domainkey", cacheMissClosure)

        assert 1 == calledCount
        assert [book1, book3] == cacheMissList
        assert NO_EXPIRATION_TTL == redisService.ttl("domainkey")

        def cacheHitList = redisService.memoizeDomainList(Book, "domainkey", cacheMissClosure)

        // cache hit, don't call closure again
        assert 1 == calledCount
        assert [book1, book3] == cacheHitList
        assert cacheMissList == cacheHitList
    }

    @Test
    public void testMemoizeDomainListWithExpire() {
        def book1 = Book.build(title: "book1")
        assert KEY_DOES_NOT_EXIST == redisService.ttl("domainkey")
        def result = redisService.memoizeDomainList(Book, "domainkey", 60) { [book1] }
        assert [book1] == result
        assert NO_EXPIRATION_TTL < redisService.ttl("domainkey")
    }

    @Test
    public void testMemoizeDomainIdList() {
        def book1 = Book.build(title: "book1")
        def book2 = Book.build(title: "book2")
        def book3 = Book.build(title: "book3")

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.findAllByTitleInList(["book1", "book3"])
        }

        def cacheMissList = redisService.memoizeDomainIdList(Book, "domainkey", cacheMissClosure)

        assert 1 == calledCount
        assert [book1.id, book3.id] == cacheMissList

        def cacheHitList = redisService.memoizeDomainIdList(Book, "domainkey", cacheMissClosure)

        // cache hit, don't call closure again
        assert 1 == calledCount
        assert [book1.id, book3.id] == cacheHitList
        assert cacheMissList == cacheHitList
    }

    @Test
    public void testMemoizeDomainObject() {
        Book book1 = Book.build(title: "book1")

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.get(book1.id)
        }

        def cacheMissBook = redisService.memoizeDomainObject(Book, "domainkey", cacheMissClosure)

        assert 1 == calledCount
        assert book1.id == cacheMissBook.id

        def cacheHitBook = redisService.memoizeDomainObject(Book, "domainkey", cacheMissClosure)

        // cache hit, don't call closure again
        assert 1 == calledCount
        assert book1.id == cacheHitBook.id
        assert cacheHitBook.id == cacheMissBook.id
    }
}
