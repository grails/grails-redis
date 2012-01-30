package test

import grails.plugin.redis.Memoize
import groovy.transform.ToString
import groovy.transform.EqualsAndHashCode

@ToString(includes = 'id,title,createDate')
@EqualsAndHashCode
class Book {

    String title
    Date createDate

    @Memoize(key = '#{title}')
    def getMemoizedTitle(Date date) {
        "$title $date"
    }
}
