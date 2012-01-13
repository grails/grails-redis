package grails.plugin.redis

import com.example.Book
import com.example.BookService
import grails.plugin.spock.IntegrationSpec

/**
 */
class RedisMemoizeSpec extends IntegrationSpec {

    RedisService redisService
    BookService bookService

    def "get AST transformed method using object property key"() {
        given:
        def title = 'narwhals'
        redisService.flushDB()
        def date = new Date()
        def book = Book.build(title: title, createDate: date)

        when: 'get the initial value and cache it'
        def value1 = bookService.getAnnotatedBook(book, date)

        then:
        value1 == "$book $date"

        when: 'get value again using book.title key and new date'
        def date2 = new Date() + 1
        def book2 = Book.build(title: title, createDate: date2)
        def value2 = bookService.getAnnotatedBook(book2, date2)

        then: 'value should be the same as first call due to overlapping titles'
        value2 == "$book $date"
        value2 != "$book2 $date2"
    }


    def "get AST transformed method using simple string key"() {
        given:
        redisService.flushDB()
        def text = 'hello'
        def date = new Date()

        when: 'get the initial value and cache it'
        def value1 = bookService.getAnnotatedText(text, date)

        then:
        value1 == "$text $date"

        when: 'get value again using text key and new date'
        def date2 = new Date() + 1
        def value2 = bookService.getAnnotatedText(text, date2)

        then: 'value should be the same as first call not new date'
        value2 == "$text $date"
        value2 != "$text $date2"
    }

    def "make sure redis is bahving correctly on non-annotated methods"() {
        given:
        redisService.flushDB()
        def text = 'hello'
        def date = new Date()

        when: 'get the initial value and cache it'
        def value1 = bookService.getMemoizedTextDate(text, date)

        then:
        value1 == "$text $date"

        when: 'get value again using text key and new date'
        def date2 = new Date() + 1
        def value2 = bookService.getMemoizedTextDate(text, date2)

        then: 'value should be the same as first call not new date'
        value2 == value1
        value2 == "$text $date"
        value2 != "$text $date2"
    }
}
