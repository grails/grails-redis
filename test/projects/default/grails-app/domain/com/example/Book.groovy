package com.example

import grails.plugin.redis.Memoize
import groovy.transform.ToString

@ToString(includes = "id,title,createDate")
class Book {

    String title
    Date createDate

    @Memoize(key = "#{title}")
    def getMemoizedTitle(Date date) {
        "$title $date"
    }
}
