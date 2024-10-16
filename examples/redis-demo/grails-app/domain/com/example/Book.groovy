package com.example

import grails.plugins.redis.RedisService
import groovy.transform.ToString
import java.time.LocalDate

@ToString(includes = "id,createDate")
class Book {

    RedisService redisService

    String title = ''
    LocalDate createDate = LocalDate.now()
    static transients = ['redisService']

    static mapping = {
        autowire true
    }

    //todo: FIX THESE ASAP!
//    @Memoize(key = '#{title}')
    def getMemoizedTitle(LocalDate date) {
        redisService?.memoize(title) {
            println 'cache miss'
            "$title $date"
       }
    }
}