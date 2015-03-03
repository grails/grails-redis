package grails.plugins.redis

import com.example.Book
import grails.test.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Specification

import static grails.plugins.redis.RedisService.KEY_DOES_NOT_EXIST
import static grails.plugins.redis.RedisService.NO_EXPIRATION_TTL

@Integration
@org.springframework.transaction.annotation.Transactional
class RedisIntegrationSpec extends Specification {

    @Autowired
    RedisService redisService

    public void setup() {
        redisService.flushDB()
    }

    public void testMemoizeDomainList() {
        given:
        def book1 = new Book(title: "book1").save(flush: true)
        def book2 = new Book(title: "book2").save(flush: true)
        def book3 = new Book(title: "book3").save(flush: true)

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.findAllByTitleInList(["book1", "book3"])
        }

        when:
        def cacheMissList = redisService.memoizeDomainList(Book, "domainkey", cacheMissClosure)

        then:
        1 == calledCount
        [book1, book3] == cacheMissList
        NO_EXPIRATION_TTL == redisService.ttl("domainkey")

        when:
        def cacheHitList = redisService.memoizeDomainList(Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        1 == calledCount
        [book1, book3] == cacheHitList
        cacheMissList == cacheHitList
    }


    public void testMemoizeDomainListWithExpire() {
        given:
        def book1 = new Book(title: "book1").save(flush: true)
        KEY_DOES_NOT_EXIST == redisService.ttl("domainkey")

        when:
        def result = redisService.memoizeDomainList(Book, "domainkey", 60) { [book1] }

        then:
        [book1] == result
        NO_EXPIRATION_TTL < redisService.ttl("domainkey")
    }


    public void testMemoizeDomainIdList() {
        given:
        def book1 = new Book(title: "book1").save(flush: true)
        def book2 = new Book(title: "book2").save(flush: true)
        def book3 = new Book(title: "book3").save(flush: true)

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.findAllByTitleInList(["book1", "book3"])
        }

        when:
        def cacheMissList = redisService.memoizeDomainIdList(Book, "domainkey", cacheMissClosure)

        then:
        1 == calledCount
        [book1.id, book3.id] == cacheMissList

        when:
        def cacheHitList = redisService.memoizeDomainIdList(Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        1 == calledCount
        [book1.id, book3.id] == cacheHitList
        cacheMissList == cacheHitList
    }


    public void testMemoizeDomainObject() {
        given:
        Book book1 = new Book(title: "book1").save(flush: true)

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.get(book1.id)
        }

        when:
        def cacheMissBook = redisService.memoizeDomainObject(Book, "domainkey", cacheMissClosure)

        then:
        1 == calledCount
        book1.id == cacheMissBook.id

        when:
        def cacheHitBook = redisService.memoizeDomainObject(Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        1 == calledCount
        book1.id == cacheHitBook.id
        cacheHitBook.id == cacheMissBook.id
    }
}
