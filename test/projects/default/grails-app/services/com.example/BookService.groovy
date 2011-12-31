package com.example

import grails.plugin.redis.RedisService
import org.codehaus.groovy.grails.compiler.Memoize

class BookService {

    RedisService redisService

    @Memoize
    def doWork(String text, Date date) {
        println 'in doWork'
        "$text $date"
    }
}