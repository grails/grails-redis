package grails.plugin.redis

import grails.plugin.spock.IntegrationSpec
import redis.clients.jedis.JedisPool
import com.example.BookService
import redis.clients.jedis.Jedis

/**
 */
class RedisMultipleConnectionsConfigSpec extends IntegrationSpec {

    RedisService redisService

    def setup() {
        redisService.flushDB()
        redisService.withConnection('one').flushDB()
        redisService.withConnection('two').flushDB()
    }

    def "test multiple redis pools"() {
        given:
        def key = "key"
        def data = "data"

        when:
        redisService.withConnection('one').withRedis {Jedis redis ->
            redis.set(key, data)
        }

        then:
        redisService.withConnection('one').withRedis {Jedis redis ->
            redis.get(key) == data
        }
        redisService.withConnection('two').withRedis {Jedis redis ->
            !redis.get(key)
        }
        redisService.withRedis {Jedis redis ->
            !redis.get(key)
        }

        when:
        redisService.withConnection('two').withRedis{Jedis redis ->
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
    }
}
