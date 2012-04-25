package grails.plugin.redis

import com.example.Book
import grails.plugin.spock.IntegrationSpec
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import spock.lang.Ignore

import static grails.plugin.redis.RedisService.NO_EXPIRATION_TTL
import com.example.BookService

/**
 * This test will only work if you fire up 3 redis instances on different hosts or ports
 * Todo: Start multiple instances of redis and comment out the ignore for these to pass
 */
@Ignore
class RedisMultipleConnectionsSpec extends IntegrationSpec {

    RedisService redisService
    JedisPool redisConn1
    JedisPool redisConn2
    JedisPool redisConn3
    BookService bookService

    def setup() {
        redisService.flushDB()
        redisService.flushDB(redisConn1)
        redisService.flushDB(redisConn2)
        redisService.flushDB(redisConn3)
    }

    def "test multiple redis pools"() {
        given:
        def key = "key"
        def data = "data"

        when:
        redisService.withRedis(redisConn1) {Jedis redis ->
            redis.set(key, data)
        }

        then:
        redisService.withRedis(redisConn1) {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withRedis(redisConn2) {Jedis redis ->
            !redis.get(key)
        }
        redisService.withRedis(redisConn3) {Jedis redis ->
            !redis.get(key)
        }

        when:
        redisService.withRedis(redisConn2) {Jedis redis ->
            redis.set(key, data)
        }

        then:
        redisService.withRedis(redisConn1) {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withRedis(redisConn2) {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withRedis(redisConn3) {Jedis redis ->
            !redis.get(key)
        }

        when:
        redisService.withRedis(redisConn3) {Jedis redis ->
            redis.set(key, data)
        }

        then:
        redisService.withRedis(redisConn1) {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withRedis(redisConn2) {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withRedis(redisConn3) {Jedis redis ->
            redis.get(key) == data
        }
    }

    def "ensure memoize works across connections"() {
        given:
        def key = "key"
        def data = "data"
        def calledCount = 0

        when:
        def result = redisService.memoize(redisConn2, key) { Jedis redis ->
            calledCount += 1
            data
        }

        then:
        calledCount == 1
        redisService.withRedis(redisConn2) { Jedis redis ->
            redis.get(key) == data
        }

        when:
        def result2 = redisService.memoize(redisConn2, key) {Jedis redis ->
            calledCount += 1
            data
        }

        then:
        calledCount == 1
        result2 == result

        when:
        def result3 = redisService.memoize(key) { Jedis redis ->
            calledCount += 1
            data
        }

        then:
        calledCount == 2
        result3 == result
    }

    def "ensure memoizeDomainList works across connections"() {
        given:
        def book1 = Book.build(title: "book1")
        def book2 = Book.build(title: "book2")
        def book3 = Book.build(title: "book3")
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.findAllByTitleInList(["book1", "book3"])
        }

        when:
        def cacheMissList = redisService.memoizeDomainList(redisConn1, Book, "domainkey", cacheMissClosure)

        then:
        1 == calledCount
        [book1, book3] == cacheMissList
        NO_EXPIRATION_TTL == redisService.ttl("domainkey")

        when:
        def cacheHitList = redisService.memoizeDomainList(redisConn1, Book, "domainkey", cacheMissClosure)

        // cache hit, don't call closure again
        then:
        1 == calledCount
        [book1, book3] == cacheHitList
        cacheMissList == cacheHitList

        when:
        def cacheMissList2 = redisService.memoizeDomainList(redisConn2, Book, "domainkey", cacheMissClosure)

        then:
        2 == calledCount
        [book1, book3] == cacheMissList2
        NO_EXPIRATION_TTL == redisService.ttl("domainkey")

        when:
        def cacheHitList2 = redisService.memoizeDomainList(redisConn2, Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        2 == calledCount
        [book1, book3] == cacheHitList2
        cacheMissList2 == cacheHitList2
    }

    def "ensure memoizeDomainList with expire works across connections"() {
        given:
        def book1 = Book.build(title: "book1")

        when:
        def result = redisService.withRedis(redisConn1) {Jedis redis ->
            return redis.ttl("domainkey")
        }

        then:
        NO_EXPIRATION_TTL == result

        when:
        result = redisService.memoizeDomainList(redisConn1, Book, "domainkey", 60) { [book1] }

        then:
        [book1] == result
        NO_EXPIRATION_TTL < redisService.withRedis(redisConn1) {Jedis redis ->
            return redis.ttl("domainkey")
        }
    }

    def "ensure memoizeDomainIdList works across connections"() {
        given:
        def book1 = Book.build(title: "book1")
        def book2 = Book.build(title: "book2")
        def book3 = Book.build(title: "book3")
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.findAllByTitleInList(["book1", "book3"])
        }

        when:
        def cacheMissList = redisService.memoizeDomainIdList(redisConn1, Book, "domainkey", cacheMissClosure)

        then:
        1 == calledCount
        [book1.id, book3.id] == cacheMissList

        when:
        def cacheHitList = redisService.memoizeDomainIdList(redisConn1, Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        1 == calledCount
        [book1.id, book3.id] == cacheHitList
        cacheMissList == cacheHitList

        when:
        def cacheMissList1 = redisService.memoizeDomainIdList(redisConn2, Book, "domainkey", cacheMissClosure)

        then:
        2 == calledCount
        [book1.id, book3.id] == cacheMissList1

        when:
        def cacheHitList1 = redisService.memoizeDomainIdList(redisConn2, Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        2 == calledCount
        [book1.id, book3.id] == cacheHitList1
        cacheMissList1 == cacheHitList1
    }

    def "ensure memoizeDomainObject works across connections"() {
        given:
        Book book1 = Book.build(title: "book1")
        def calledCount = 0
        def cacheMissClosure = {
            calledCount += 1
            return Book.get(book1.id)
        }

        when:
        def cacheMissBook = redisService.memoizeDomainObject(redisConn1, Book, "domainkey", cacheMissClosure)

        then:
        1 == calledCount
        book1.id == cacheMissBook.id

        when:
        def cacheHitBook = redisService.memoizeDomainObject(redisConn1, Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        1 == calledCount
        book1.id == cacheHitBook.id
        cacheHitBook.id == cacheMissBook.id

        when:
        def cacheMissBook1 = redisService.memoizeDomainObject(redisConn2, Book, "domainkey", cacheMissClosure)

        then:
        2 == calledCount
        book1.id == cacheMissBook1.id

        when:
        def cacheHitBook1 = redisService.memoizeDomainObject(redisConn2, Book, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        2 == calledCount
        book1.id == cacheHitBook1.id
        cacheHitBook1.id == cacheMissBook1.id
    }

    def "ensure memoizeHash works across connections"() {
        given:
        def calledCount = 0
        def hash = [foo: "bar"]
        def cacheMissClosure = {
            calledCount += 1
            return [foo: "bar"]
        }

        when:
        def cacheMissHash = redisService.memoizeHash(redisConn1, "domainkey", cacheMissClosure)

        then:
        1 == calledCount
        hash == cacheMissHash

        when:
        def cacheHitHash = redisService.memoizeHash(redisConn1, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        1 == calledCount
        hash == cacheHitHash
        cacheHitHash == cacheMissHash

        when:
        def cacheMissHash1 = redisService.memoizeHash(redisConn2, "domainkey", cacheMissClosure)

        then:
        2 == calledCount
        hash == cacheMissHash1

        when:
        def cacheHitHash1 = redisService.memoizeHash(redisConn2, "domainkey", cacheMissClosure)

        then:
        // cache hit, don't call closure again
        2 == calledCount
        hash == cacheHitHash1
        cacheHitHash1 == cacheMissHash1
    }
}
