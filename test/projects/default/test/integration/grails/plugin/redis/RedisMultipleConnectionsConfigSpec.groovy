package grails.plugin.redis

import grails.plugin.spock.IntegrationSpec
import redis.clients.jedis.Jedis

/**
 */
//@Ignore
class RedisMultipleConnectionsConfigSpec extends IntegrationSpec {

    RedisService redisService
    RedisService redisServiceOne
    RedisService redisServiceTwo

    def setup() {
        redisService.flushDB()
        redisService.withConnection('one').flushDB()
        redisService.withConnection('two').flushDB()
        redisServiceOne.flushDB() //redundant, but just illustrates another way to do this
        redisServiceTwo.flushDB() //redundant, but just illustrates another way to do this
    }

    def "test multiple redis pools"() {
        given:
        def key = "key"
        def data = "data"

        when:
        redisService.withConnection('one').withRedis {Jedis redis ->
            redis.set(key, data)
        }

        then: 'These will both use the same redis connection'
        redisService.withConnection('one').withRedis {Jedis redis ->
            redis.get(key) == data
        }
        redisServiceOne.withRedis {Jedis redis ->
            redis.get(key) == data
        }

        and:
        redisService.withConnection('two').withRedis {Jedis redis ->
            !redis.get(key)
        }
        redisServiceTwo.withRedis {Jedis redis ->
            !redis.get(key)
        }
        redisService.withRedis {Jedis redis ->
            !redis.get(key)
        }
        redisService.withConnection('blahblahblah').withRedis {Jedis redis ->
            !redis.get(key)
        }

        when:
        redisService.withConnection('two').withRedis {Jedis redis ->
            redis.set(key, data)
        }

        then:
        redisService.withConnection('one').withRedis {Jedis redis ->
            redis.get(key) == data
        }
        redisServiceOne.withRedis {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withConnection('two').withRedis {Jedis redis ->
            redis.get(key) == data
        }
        redisServiceTwo.withRedis {Jedis redis ->
            redis.get(key) == data
        }

        and:
        redisService.withRedis {Jedis redis ->
            !redis.get(key)
        }
        redisService.withConnection('blahblahblah').withRedis {Jedis redis ->
            !redis.get(key)
        }


        when:
        redisService.withRedis {Jedis redis ->
            redis.set(key, data)
        }

        then:
        redisService.withConnection('one').withRedis {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withConnection('two').withRedis {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withRedis {Jedis redis ->
            redis.get(key) == data
        }

        //will use default connection just lke normal redisService.withRedis since blahblahblah isn't valid
        redisService.withConnection('blahblahblah').withRedis {Jedis redis ->
            redis.get(key) == data
        }
    }
}
