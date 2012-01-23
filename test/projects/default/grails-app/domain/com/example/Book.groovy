package com.example

import groovy.transform.ToString
import grails.plugin.redis.Memoize

@ToString(includes="id,title,createDate")
class Book {
    //def redisService
    //def bookService

    String title
    Date createDate

    @Memoize(key="#{title}")
    def getMemoizedTitle(Date date){
        "$title $date"
    }
}
