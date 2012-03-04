package com.example

import static org.junit.Assert.*
import org.junit.*

class WorkingWithRedisGormTests {

    def redisService

    @Before
    void setUp() {
        redisService.flushDB()
    }

    @Test
    void testPersistRedisBook() {
        Book book = new Book(title: "The Shining")
        book.save(failOnError: true)

        assert Book.list().size() == 1

        println redisService.keys("*")
    }


    @Test
    void testRedisServiceMethods() {
        redisService.foo = "foo"
        assert "foo" == redisService.foo
    }
}
