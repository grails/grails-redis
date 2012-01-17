package grails.plugin.redis


import grails.plugin.spock.IntegrationSpec
import test.BookService
import test.Book

/**
 */
class RedisMemoizeSpec extends IntegrationSpec {

    RedisService redisService
    BookService bookService

    def setup(){
        redisService.flushDB()
    }

    def "get AST transformed method using object property key"() {
        given:
        def title = 'narwhals'
        def date = new Date()
        def date2 = new Date() + 1
        def book = Book.build(title: title, createDate: date)
        def book2 = Book.build(title: title, createDate: date2)

        when: 'get the initial value and cache it'
        def value1 = bookService.getAnnotatedBook(book, date)
        def value2 = bookService.getAnnotatedBook(book2, date2)

        then: 'value should be the same as first call due to overlapping titles'
        value1 == "$book $date"
        value2 == "$book $date"
        value2 != "$book2 $date2"
    }


    def "get AST transformed method using simple string key property and expire"() {
        given:
        redisService.flushDB()
        def text = 'hello'
        def date = new Date()
        def date2 = new Date() + 1

        when: 'get the initial value and cache it'
        def value1 = bookService.getAnnotatedTextUsingKeyAndExpire(text, date)
        Thread.sleep(2000) //give redis sometime to expire 1ms ttl
        def value2 = bookService.getAnnotatedTextUsingKeyAndExpire(text, date2)

        then: 'value should be the same as first call not new date'
        value1 == "$text $date"
        value2 == "$text $date2"
    }

 def "get AST transformed method using simple string key property"() {
        given:
        redisService.flushDB()
        def text = 'hello'
        def date = new Date()
        def date2 = new Date() + 1

        when: 'get the initial value and cache it'
        def value1 = bookService.getAnnotatedTextUsingKey(text, date)
        def value2 = bookService.getAnnotatedTextUsingKey(text, date2)

        then: 'value should be the same as first call not new date'
        value1 == "$text $date"
        value2 == "$text $date"
        value2 != "$text $date2"
    }

    def "get AST transformed method using simple key closure"() {
        given:
        redisService.flushDB()
        def text = 'hello'
        def date = new Date()
        def date2 = new Date() + 1

        when: 'get the value and cache it'
        def value1 = bookService.getAnnotatedTextUsingClosure(text, date)
        def value2 = bookService.getAnnotatedTextUsingClosure(text, date2)

        then: 'value should be the same as first call not new date'
        value1 == "$text $date"
        value2 == "$text $date"
        value2 != "$text $date2"
    }

    def "make sure redis is bahving correctly on non-annotated methods"() {
        given:
        redisService.flushDB()
        def text = 'hello'
        def date = new Date()
        def date2 = new Date() + 1

        when: 'get the value and cache it'
        def value1 = bookService.getMemoizedTextDate(text, date)
        def value2 = bookService.getMemoizedTextDate(text, date2)

        then: 'value should be the same as first call not new date'
        value1 == "$text $date"
        value2 == value1
        value2 == "$text $date"
        value2 != "$text $date2"
    }
}
