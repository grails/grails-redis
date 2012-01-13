package com.example

import grails.plugin.redis.RedisService
import org.codehaus.groovy.grails.compiler.Memoize
import com.example.Book

class BookService {

    RedisService redisService

    @Memoize({"#{text}"})
    def getAnnotatedText(String text, Date date) {
        println 'called getTextDate'
        return "$text $date"
    }

    @Memoize({"#{book.title}:#{book.id}"})
    def getAnnotatedBook(Book book, Date date) {
        println 'called getBookTitle'
        return "$book $date"
    }

    def getMemoizedTextDate(String text, Date date) {
        return redisService.memoize(text) {
            println "cache miss"
            return "$text $date"
        }
    }
}