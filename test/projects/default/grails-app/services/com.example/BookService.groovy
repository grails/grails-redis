package com.example

import grails.plugin.redis.RedisService
import grails.plugin.redis.Memoize

class BookService {

    RedisService redisService

    @Memoize({"#{text}"})
    def getAnnotatedTextUsingClosure(String text, Date date) {
        println 'called getAnnotatedTextUsingClosure'
        return "$text $date"
    }

    @Memoize(key='#text')
    def getAnnotatedTextUsingKey(String text, Date date) {
        println 'called getAnnotatedTextUsingKey'
        return "$text $date"
    }

    //exire this extremely fast to test that it works
    @Memoize(key='#text',expire='1')
    def getAnnotatedTextUsingKeyAndExpire(String text, Date date) {
        println 'called getAnnotatedTextUsingKeyAndExpire'
        return "$text $date"
    }

    @Memoize({"#{book.title}:#{book.id}"})
    def getAnnotatedBook(Book book, Date date) {
        println 'called getAnnotatedBook'
        return "$book $date"
    }

    def getMemoizedTextDate(String text, Date date) {
        return redisService.memoize(text) {
            println "called getMemoizedTextDate"
            return "$text $date"
        }
    }
}