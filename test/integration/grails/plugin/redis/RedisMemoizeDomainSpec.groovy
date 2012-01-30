package grails.plugin.redis

import grails.plugin.spock.IntegrationSpec
import test.Book
import spock.lang.Shared

class RedisMemoizeDomainSpec extends IntegrationSpec {

    @Shared
    RedisService redisService

    def setup() {
        redisService.flushDB()
    }

    def "get AST transformed domain object method"() {
        given:
        def title = 'all the things'
        def date1 = new Date()
        def date2 = new Date() + 1
        Book book = Book.build(title: title)

        when:
        def string1 = book.getMemoizedTitle(date1)

        then:
        string1 == "$title $date1"

        when:
        def string2 = book.getMemoizedTitle(date2)

        then:
        string2 != "$title $date2"
        string2 == "$title $date1"
    }
}