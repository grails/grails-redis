package com.example

import grails.plugin.redis.Memoize
import grails.plugin.redis.MemoizeDomainList
import grails.plugin.redis.RedisService

class BookService {

    RedisService redisService

    @MemoizeDomainList(key = "getDomainListWithKeyClass:#title", clazz = Book.class)
    def getDomainListWithKeyClass(String title, Date date) {
        redisService.getDomainListWithKeyClass = "$title $date"
        println 'cache miss getDomainListWithKeyClass'
        Book.findAllByTitle(title)
    }

    @Memoize(key="#{text}")
    def getAnnotatedTextUsingClosure(String text, Date date) {
        println 'cache miss getAnnotatedTextUsingClosure'
        return "$text $date"
    }

    @Memoize(key = 'text')
    def getAnnotatedTextUsingKey(String text, Date date) {
        println 'cache miss getAnnotatedTextUsingKey'
        return "$text $date"
    }

    //exire this extremely fast to test that it works
    @Memoize(key = 'text', expire = '1')
    def getAnnotatedTextUsingKeyAndExpire(String text, Date date) {
        println 'cache miss getAnnotatedTextUsingKeyAndExpire'
        return "$text $date"
    }

    @Memoize(key = "#{book.title}:#{book.id}")
    def getAnnotatedBook(Book book, Date date) {
        println 'cache miss getAnnotatedBook'
        return "$book $date"
    }

    def getMemoizedBook(Book book, Date date) {
        return redisService.memoize("${book.title}:${book.id}") {
            println "cache miss getMemoizedTextDate"
            return "$text $date"
        }
    }

    def getMemoizedTextDate(String text, Date date) {
        return redisService.memoize(text) {
            println "cache miss getMemoizedTextDate"
            return "$text $date"
        }
    }
}