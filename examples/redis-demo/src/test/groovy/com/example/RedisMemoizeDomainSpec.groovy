package com.example

import grails.gorm.transactions.Rollback
import grails.plugins.redis.RedisService
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Specification

@Integration
@Rollback
@Ignore
class RedisMemoizeDomainSpec extends Specification {

    @Autowired RedisService redisService

    def setup() {
        redisService.flushDB()
    }

    def "get AST transformed domain object method"() {
        given:
        def title = 'all the things'
        def date1 = new Date()
        def date2 = new Date() + 1
        Book book = new Book(title: title).save(flush: true)

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