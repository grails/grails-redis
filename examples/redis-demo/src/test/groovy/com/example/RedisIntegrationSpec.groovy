package com.example

import grails.core.support.proxy.ProxyHandler
import grails.plugins.redis.RedisService
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Rollback
import spock.lang.Specification

import static grails.plugins.redis.RedisService.KEY_DOES_NOT_EXIST
import static grails.plugins.redis.RedisService.NO_EXPIRATION_TTL

@Integration
@Rollback
class RedisIntegrationSpec extends Specification implements ProxyAwareSpec {

    @Autowired RedisService redisService
    @Autowired ProxyHandler proxyHandler

    void setup() {
        redisService.flushDB()
    }

    void testMemoizeDomainList() {
        given:
        def book1 = createBook('book1')
        def book2 = createBook('book2')
        def book3 = createBook('book3')

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            Book.withNewTransaction {
                return Book.executeQuery("from Book b where b.title = 'book1' or b.title = 'book3'")
            }
        }

        when:
        def cacheMissList = redisService.memoizeDomainList(Book, "domainkey", cacheMissClosure)

        then:
        1 == calledCount
        [book1, book3]*.id== cacheMissList*.id
        NO_EXPIRATION_TTL == redisService.ttl("domainkey")

        when:
        def cacheHitList = redisService.memoizeDomainList(Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        1 == calledCount
        [book1, book3]*.id == cacheHitList.collect { getEntityId(it) }
        cacheMissList*.id == cacheHitList*.id
    }


    public void testMemoizeDomainListWithExpire() {
        given:
        def book1 = createBook('book1')
        KEY_DOES_NOT_EXIST == redisService.ttl("domainkey")

        when:
        def result = redisService.memoizeDomainList(Book, "domainkey", 60) { [book1] }

        then:
        [book1] == result
        NO_EXPIRATION_TTL < redisService.ttl("domainkey")
    }


    public void testMemoizeDomainIdList() {
        given:
        def book1 = createBook('book1')
        def book2 = createBook('book2')
        def book3 = createBook('book3')

        def books = [book1, book3]

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return books
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
        Book book1 = createBook('book1')

        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            Book.withNewTransaction {
                return Book.get(book1.id)
            }
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

    private static Book createBook(String title) {
        Book.withNewTransaction {
            return new Book(title: title).save(flush: true)
        }
    }
}
