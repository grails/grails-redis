package com.example

import grails.plugin.redis.RedisService
import org.codehaus.groovy.grails.compiler.Memoize

class BookService {

    RedisService redisService

    @Memoize({"#{text}"})
    def doWork(String text, Date date) {
        println 'in doWork'
        return "$text $date"
    }

    def doWork2(String text, Date date) {
        def closure = {
            println "cache miss"
            return "$text $date"
        }
        return redisService.memoize(text, closure)
    }
}