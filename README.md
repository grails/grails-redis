Grails Redis Plugin
===================

For integration between [Redis][redis] and Grails GORM layer, see the [Redis GORM plugin][redisgorm]. 

This plugin was originally called just "redis" (the name of this plugin), but it has since been refactored to "redis-gorm" and now relies on this plugin for connectivity.

What is Redis?
--------------

The best definition of Redis that I've heard is that it is a "collection of data structures exposed over the network".   

Redis is an [insanely fast][redisfast] key/value store, in some ways similar to [memcached][memcached], but the values it stores aren't just dumb blobs of data.  Redis values are data structures like [strings][redisstring], [lists][redislist], [hash maps][redishash], [sets][redisset], and [sorted sets][redissortedset].  Redis also can act as a lightweight pub/sub or message queueing system.


Redis is used in production today by a [number of very popular][redisusing] websites including Craigslist, StackOverflow, GitHub, The Guardian, and Digg.

It's commonly lumped in with other NoSQL technologies and is commonly used as a caching layerhas some similarities to Memcached or Tokyo Tyrant.  Because Redis provides network-available data structures, it's very flexible and it's able to solve all kinds of problems.  The creator of Redis, Salvatore Sanfilippo, has a nice post on his blog showing [how to take advantage of Redis by just adding it to your stack][addredisstack].  With the Grails Redis plugin, adding Redis to your grails app is very easy.

I've created an [introduction to Redis using groovy][redisgroovy] that shows you how to install redis and use some basic groovy commands.  There is also a [presentation that I gave at gr8conf 2011][slideshareggr].

The official [Redis documentation][redis] is fantastic and includes a comprensive [list of Redis commands][rediscommands], each command web page also has an embedded REPL that lets you test out the command against a live Redis server.

What is Jedis?
==============

[Jedis][jedis] is the Java Redis connection library that the Grails Redis plugin uses.  It's actively maintained, very fast, and doesn't try to do anything too clever.  One of the nice things about it is that it doesn't try to munge around with the Redis command names, but follows them as closely as possible.  This means that for almost all commands, the [Redis command][rediscommands] documentation can also be used to understand how to use the Jedis connection objects.  You don't need to worry about translating the Redis documentation into Jedis commands.

Installation
------------

    grails install-plugin redis


Out of the box, the plugin expects that Redis is running on `localhost:6379`.  You can modify this (as well as any other pool config options) by adding a stanza like this to your `grails-app/conf/Config.groovy` file:

    grails {
        redis {
            poolConfig {
                // jedis pool specific tweaks here, see jedis docs & src
                // ex: testWhileIdle = true
            }
            port = 6379
            host = "localhost"
        }
    }

The poolConfig section will let you tweak any of the [setter values made available by the JedisPoolConfig][jedispoolconfig].  It implements the Apache Commons [GenericObjectPool][genericobjectpool].

Plugin Usage
------------


### RedisService Bean ###

    def redisService

The `redisService` bean wraps the pool connection.   It has a number of caching/memoization helper functions, template methods, and basic Redis commands, it will be your primary interface to Redis.

The service overrides `propertyMissing` and `methodMissing` to delegate any missing requests to a Redis connection object.  This means that any method that you'd normally call on a Redis connection object can be called directly on `redisService`.

    // overrides propertyMissing and methodMissing to delegate to redis
    def redisService

    redisService.foo = "bar"   
    assert "bar" == redisService.foo   

    redisService.sadd("months", "february")
    assert true == redisService.sismember("months", "february")
        
It also provides a template method called `withRedis` that takes a closure as a parameter.  It passes a Jedis connection object to Redis into the closure.  The template method automatically gets an object out of the pool and ensures that it gets returned to the pool after the closure finishes (even if there's an error).

    redisService.withRedis { Jedis redis ->
        redis.set("foo", "bar")
    }

The advantage to calling `withRedis` rather than just calling methods directly on `redisService` is that multiple commands will only use a single connection instance, rather than one per command.

Redis also allows you to pipeline commands.  Pipelining allows you to quickly send commands to Redis without waiting for a response.  When the pipeline is executed, it returns a Result object, which works like a `Future` to give you the results of the pipeline.  See the [Jedis][jedis] documentation on pipelining for more details.  It works pretty much like the `withRedis` template does:

    redisService.withPipeline { Pipeline pipeline ->
        pipeline.set("foo", "bar")
    }

Redis has the notion of transactions, but it's not exactly the same as a database transaction.  Redis transactions guarantee that all of the commands in the transaction will be executed as an atomic unit.  Because Redis is single threaded, you're guaranteed to execute atomically and have a known state throughout the transaction.  Redis does not support rolling back modifications that happen during a transaction.

The `withTransaction` template method automatically opens and closes the transaction for you.  If the closure doesn't throw and exception, it will tell Redis to execute the transaction

    redisService.withTransaction { Transaction transaction ->
        transaction.set("foo", "bar")
    }

### Memoization ###

Memoization is a write-through caching technique.  The plugin gives a number of methods that take a key name, and a closure as parameters.  These methods first check Redis to see if the key exists.  If it does, it returns the value of the key and does not execute the closure.  If it does not exist in Redis, it executes the closure and saves the result in Redis under the key.  Subsequent calls will then be served the cached value from Redis rather than recalculating.

This technique is very useful for caching values that are frequently requested but expensive to calculate.   There are methods for the basic Redis data types:

### String Memoization ###

    redisService.memoize("user:$userId:helloMessage") { Jedis redis ->
        // expensive to calculate method that returns a String
        "Hello ${security.currentLoggedInUser().firstName}"
    }

By default, the key/value will be cached forever in Redis, you can ensure that the key is refreshed either by deleting the key from Redis, making the key include a date or timestamp, or by using the optional `expire` parameter, the value is the number of seconds before Redis should expire the key:

    def ONE_HOUR = 3600
    redisService.memoize("user:$userId:helloMessage", [expire: ONE_HOUR]) { Jedis redis ->
        """
        Hello ${security.currentLoggedInUser().firstName. 
        The temperature this hour is ${currentTemperature()}
        """
    }

### Domain List Memoization ###

You can also memoize a list of domain object identifiers.  It doesn't cache the entire domain object, just the database IDs of the domain objects in a returned list.

This allows you to still grab the freshest objects from the database, but not repeatedly create an expensive list.  This could be a big database query that joins a bunch of tables.  Or some other process that does additional filtering based on selections the user has made in the UI.  Something ephemeral for that session or user, that you don't want to persist, but need to be able to react to.

    def key = "user:$id:friends-books-user-does-not-own"

    redisService.memoizeDomainList(Book, key, ONE_HOUR) { redis ->
        // expensive process to calculate all friend’s books and filter out books
        // that the user already owns, this stores the list of determined Book IDs
        // in Redis, but hydrates the Book objects from the DB
    }
        
### Other Memoization Methods ###

There are other memoization methods that the plugin provides, check out the [RedisService.groovy][redisservicecode] and the plugin tests for the exhaustive list.

    // Redis Hash memoize methods
    
    redisService.memoizeHash("saved-hash") { Jedis redis -> return [foo: "bar"] }

    redisService.memoizeHashField("saved-hash", "foo") { Jedis redis -> return "bar" }

    // Redis Sorted Set memoize methods
    redisService.memoizeScore("saved-sorted-set", "set-item") { Jedis redis -> return score }

### Redis Pool Bean ###

You can have direct access to the pool of Redis connection objects by injecting `redisPool` into your code.   Normally, you won't want to directly work with the pool, and instead interact with the `redisService` bean, but you have the option to manually work with the pool if desired.

    def redisPool


### Redis Taglib ###


The `redis:memoize` TagLib lets you leverage memoization within your GSP files.  Wrap it around any expensive to generate text and it will cache it and quickly serve it from Redis.

    <redis:memoize key="mykey" expire="3600">
        <!-- 
            insert expensive to generate GSP content here 

            taglib body will be executed once, subsequent calls 
            will pull from redis till the key expires
        -->
        <div id='header'>
            ... expensive header stuff here that can be cached ...
        </div>
    </redis:memoize>




[redisgorm]: http://grails.github.com/inconsequential/redis/
[redis]: http://redis.io
[redisgroovy]: http://naleid.com/blog/2010/12/28/intro-to-using-redis-with-groovy/
[slideshareggr]: http://naleid.com/blog/2011/06/27/redis-groovy-and-grails-presentation-at-gr8conf-2011-and-gum/
[rediscommands]: http://redis.io/commands
[redisstring]:http://redis.io/commands#string
[redislist]:http://redis.io/commands#list
[redishash]:http://redis.io/commands#hash
[redisset]:http://redis.io/commands#set
[redissortedset]:http://redis.io/commands#sorted_set
[redisfast]:http://redis.io/topics/benchmarks
[memcached]:http://memcached.org/
[redisusing]:http://redis.io/topics/whos-using-redis
[addredisstack]:http://antirez.com/post/take-advantage-of-redis-adding-it-to-your-stack.html
[jedis]:https://github.com/xetorthio/jedis/wiki
[jedispoolconfig]:https://github.com/xetorthio/jedis/blob/master/src/main/java/redis/clients/jedis/JedisPoolConfig.java
[genericobjectpool]:http://commons.apache.org/pool/apidocs/org/apache/commons/pool/impl/GenericObjectPool.html
[redisservicecode]:https://github.com/grails-plugins/grails-redis/blob/master/grails-app/services/grails/plugin/redis/RedisService.groovy
