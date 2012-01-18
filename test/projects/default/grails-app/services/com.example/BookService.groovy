package com.example

import grails.plugin.redis.*

class BookService {

    RedisService redisService

    @MemoizeList(key = "#{list[0]}")
    def getAnnotatedList(List list) {
        return list
    }

    @MemoizeHash(key = "#{map.foo}")
    def getAnnotatedHash(Map map) {
        return map
    }

    @MemoizeDomainObject(key = "#{title}", clazz = Book.class)
    def createDomainObject(String title, Date date) {
        println 'cache miss createDomainObject'
        Book.build(title: title, createDate: date)
    }

    @MemoizeDomainList(key = "getDomainListWithKeyClass:#{title}", clazz = Book.class)
    def getDomainListWithKeyClass(String title, Date date) {
        redisService.getDomainListWithKeyClass = "$title $date"
        println 'cache miss getDomainListWithKeyClass'
        Book.findAllByTitle(title)
    }

    @Memoize(key = "#{text}")
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
    def getAnnotatedBook(Book book) {
        println 'cache miss getAnnotatedBook'
        return book.toString()
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