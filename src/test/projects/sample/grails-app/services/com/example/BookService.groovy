package com.example

import grails.plugins.redis.*

class BookService {

    RedisService redisService

    @MemoizeScore(key = '#{map.key}', member = 'foo')
    def getAnnotatedScore(Map map) {
        println 'cache miss getAnnotatedScore'
        return map.foo
    }

    @MemoizeList(key = '#{list[0]}')
    def getAnnotatedList(List list) {
        println 'cache miss getAnnotatedList'
        return list
    }

    @MemoizeHash(key = '#{map.foo}')
    def getAnnotatedHash(Map map) {
        println 'cache miss getAnnotatedHash'
        return map
    }

    @MemoizeHashField(key = '#{map.foo}', member = 'foo')
    def getAnnotatedHashField(Map map) {
        println 'cache miss getAnnotatedHashField'
        return map.foo
    }


    @MemoizeDomainObject(key = '#{title}', clazz = Book.class)
    def createDomainObject(String title, Date date) {
        println 'cache miss createDomainObject'
        def book = new Book(title: title, createDate: date).save(flush: true)
        book
    }

    @MemoizeDomainList(key = 'getDomainListWithKeyClass:#{title}', clazz = Book.class)
    def getDomainListWithKeyClass(String title, Date date) {
        redisService.domainListWithKeyClassKey = "$title $date"
        println 'cache miss getDomainListWithKeyClass'
        Book.findAllByTitle(title)
    }

    @Memoize({ '#{text}' })
    def getAnnotatedTextUsingClosure(String text, Date date) {
        println 'cache miss getAnnotatedTextUsingClosure'
        return "$text $date"
    }

    @Memoize(key = '#{text}')
    def getAnnotatedTextUsingKey(String text, Date date) {
        println 'cache miss getAnnotatedTextUsingKey'
        return "$text $date"
    }

    //expire this extremely fast to test that it works
    @Memoize(key = '#{text}', expire = '1')
    def getAnnotatedTextUsingKeyAndExpire(String text, Date date) {
        println 'cache miss getAnnotatedTextUsingKeyAndExpire'
        return "$text $date"
    }

    @Memoize(key = '#{book.title}:#{book.id}')
    def getAnnotatedBook(Book book) {
        println 'cache miss getAnnotatedBook'
        return book.toString()
    }

    def getMemoizedTextDate(String text, Date date) {
        return redisService.memoize(text) {
            println 'cache miss getMemoizedTextDate'
            return "$text $date"
        }
    }
}