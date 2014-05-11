package grails.plugin.redis

import com.example.Book
import com.example.BookService
import spock.lang.Specification

class RedisMemoizeServiceSpec extends Specification {

    RedisService redisService
    BookService bookService

    def setup() {
        redisService.flushDB()
    }

    def "get AST transformed score using map key"() {
        given:
        Map map = [key: 'key', foo: 100]

        when:
        def score = bookService.getAnnotatedScore(map)

        then:
        score == 100

        when:
        map.foo = 200
        def score2 = bookService.getAnnotatedScore(map)

        then:
        score2 == 100
    }

    def "get AST transformed list using item 0 as key"() {
        given:
        def list = ['one', 'two', 'three']
        def list2 = ['one', 'three', 'four']

        when:
        def aList = bookService.getAnnotatedList(list)

        then:
        redisService.lrange('one', 0, -1) == list
        aList == list
        aList[0] == 'one'
        aList[1] == 'two'
        aList[2] == 'three'

        when:
        def aList2 = bookService.getAnnotatedList(list2)

        then:
        redisService.lrange('one', 0, -1) == list
        redisService.lrange('one', 0, -1) != list2
        aList2 == list
        aList2[0] == 'one'
        aList2[1] == 'two'
        aList2[2] == 'three'
    }

    def "get AST transformed hash using maps foo as key"() {
        given:
        def map = [foo: 'foo', bar: 'bar']
        def map2 = [foo: 'foo', bar: 'bar2']

        when:
        def hash = bookService.getAnnotatedHash(map)

        then:
        redisService.hgetAll('foo') == map
        hash == map
        hash.foo == 'foo'
        hash.bar == 'bar'

        when:
        def hash2 = bookService.getAnnotatedHash(map2)

        then:
        redisService.hgetAll('foo') == map
        redisService.hgetAll('foo') != map2
        hash2 == map
        hash2.foo == 'foo'
        hash2.bar == 'bar'
    }


    def "get AST transformed hash field using maps foo as key"() {
        given:
        def map = [foo: 'foo', bar: 'bar']
        def map2 = [foo: 'foo', bar: 'bar2']

        when:
        def hash = bookService.getAnnotatedHash(map)
        def fieldValue = bookService.getAnnotatedHashField(map)

        then:
        redisService.hget('foo', 'foo') == 'foo'
        redisService.hget('foo', 'bar') == 'bar'
        redisService.hgetAll('foo') == map
        fieldValue == 'foo'
        hash == map
        hash.foo == 'foo'
        hash.bar == 'bar'

        when:
        def fieldValue2 = bookService.getAnnotatedHashField(map2)
        def hash2 = bookService.getAnnotatedHash(map2)

        then:
        redisService.hget('foo', 'foo') == 'foo'
        redisService.hget('foo','bar') == 'bar'
        fieldValue2 == 'foo'
        redisService.hgetAll('foo') == map
        redisService.hgetAll('foo') != map2
        hash2 == map
        hash2.foo == 'foo'
        hash2.bar == 'bar'
    }

    def "get AST transformed domain object using title"() {
        given:
        def title = 'ted'
        def date = new Date()

        when:
        Book book = bookService.createDomainObject(title, date)

        then:
        redisService.ted == book.id.toString()
        book.title == title
        book.createDate == date

        when:
        Book book2 = bookService.createDomainObject(title, date + 1)

        then:
        book2.title == title
        book2.createDate == date
        book2.createDate != date + 1

        when: 'change the title and it should get a new book'
        Book book3 = bookService.createDomainObject(title + '2', date)

        then:
        redisService.ted2 == book3.id.toString()
        book3.title == title + '2'
        book3.createDate == date

    }

    def "get AST transformed domain list using key and class"() {
        given:
        def title = 'narwhals'
        def books = []
        def date1 = new Date(), date2 = new Date() + 1
        10.times {
            books << Book.build(title: title)
        }

        when:
        def list1 = bookService.getDomainListWithKeyClass(title, date1)

        then:
        redisService.domainListWithKeyClassKey == "$title $date1"
        list1.size() == 10
        list1.containsAll(books)

        when: 'calling again should not invoke cache miss and key should remain unchanged'
        def list2 = bookService.getDomainListWithKeyClass(title, date2)

        then:
        redisService.domainListWithKeyClassKey == "$title $date1"
        list2.size() == 10
        list2.containsAll(books)
    }

    def "get AST transformed method using object property key"() {
        given:
        def title = 'narwhals'
        def date = new Date()
        Book book = Book.build(title: title, createDate: date)
        def bookString1 = book.toString()

        when: 'get the initial value and cache it'
        def value1 = bookService.getAnnotatedBook(book)

        then:
        value1 == bookString1

        when: 'change some non-key prop on book and get again'
        book.createDate += 1
        def bookString2 = book.toString()
        def value2 = bookService.getAnnotatedBook(book)

        then: 'value should be the same as first call due to overlapping keys'
        value2 == bookString1
        value2 != bookString2
    }


    def "get AST transformed method using simple string key property and expire"() {
        given:
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
