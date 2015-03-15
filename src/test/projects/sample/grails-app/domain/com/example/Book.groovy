package com.example

import groovy.transform.ToString

@ToString(includes = "id,title,createDate")
class Book {

    transient redisService

    String title = ''
    Date createDate = new Date()

    //todo: FIX THESE ASAP!
//    @Memoize(key = '#{title}')
    def getMemoizedTitle(Date date) {
        "$title $date"
    }


    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", createDate=" + createDate +
                '}';
    }
}

//todo: get the ast to do this because this seems to work.
//import grails.plugins.redis.RedisService
//import grails.util.Holders
//
//class Book {
//
//    RedisService redisService
//
//    static transients = ['redisService', 'redisTitle']
//
//    String title = ''
//    Date createDate = new Date()
//    String redisTitle = getMemoizedTitle(createDate)
//
//    def getMemoizedTitle(Date date) {
//        getRedisService()?.memoize(title) {
//            println 'cache miss'
//            "$title $date"
//        }
//    }
//
//    def getRedisService() {
//        return Holders?.findApplicationContext()?.getBean('redisService')
//    }
//
//
//    @Override
//    public String toString() {
//        return "Book{" +
//                "redisTitle='" + redisTitle + '\'' +
//                ", createDate=" + createDate +
//                ", title='" + title + '\'' +
//                ", id=" + id +
//                '}';
//    }
//}
