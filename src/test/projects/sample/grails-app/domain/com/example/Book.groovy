package com.example

import grails.plugins.redis.Memoize
import groovy.transform.ToString

@ToString(includes = "id,title,createDate")
class Book {

    String title = ''
    Date createDate = new Date()

    @Memoize(key = '#{title}')
    def getMemoizedTitle(Date date) {
        "$title $date"
    }
}
