package com.superstore.app.config.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

// import com.superstore.app.pojo.ProductPojo;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching // MUST ENABLE.
// @EnableSpringHttpSession // provides manual control over HTTP session.
// NOTE: @EnableRedisHttpSession or @EnableSpringHttpSession are NOT needed here anymore. 
public class RedisCacheConfig implements CachingConfigurer {

    // *** IMPORTANT ***
    // Cluster/Sentinel Environments :
    // if our production environment uses Redis Sentinel or a Redis Cluster layout rather than a standalone endpoint, 
    // we need to replace RedisStandaloneConfiguration inside redisConnectionFactory() with RedisSentinelConfiguration or RedisClusterConfiguration.

    private static final Logger log = LoggerFactory.getLogger("RedisCacheConfig");
    
    private static Boolean isRedisUp = null;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        // stops Lettuce from EAGER validation and lock up if the Redis server is missing at startup.
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    // reusable, thread-safe connection check to verify if physical Redis is online.
    private synchronized boolean checkRedisConnection() {
        if (isRedisUp != null) {
            return isRedisUp;
        }
        LettuceConnectionFactory factory = null;
        try {
            log.info("is Redis UP?");
            log.info("connecting to Redis at {}:{}", redisHost, redisPort);
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
            factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();
            factory.getConnection().close(); // attempts connection handshake.
            log.info("Redis is UP, building RedisCacheManager.");
            isRedisUp = true;
        } catch (Exception e) {
            log.error("Redis is DOWN at startup: {}, so activating local in-memory caching (ConcurrentMapCacheManager).", e.getMessage());
            isRedisUp = false;
        } finally {
            if (factory != null) {
                factory.destroy(); // prevents socket leak during the connection probe.
            }
        }
        return isRedisUp;
    }

    public boolean isRedisAvailable() {
        try (var conn = redisConnectionFactory().getConnection()) {
            conn.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Bean
    @Primary
    public CacheManager cacheManager() {
        if (checkRedisConnection()) {
            return buildRedisCacheManager(redisConnectionFactory());
        } else {
            return new ConcurrentMapCacheManager();
        }
    }

    public RedisCacheManager buildRedisCacheManager(LettuceConnectionFactory factory) {

        // GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder()
        // .enableDefaultTyping(
        //     BasicPolymorphicTypeValidator.builder()
        //         // .allowIfBaseType(Object.class)
        //         .allowIfSubType("com.superstore")
        //         .allowIfSubType("java.util")
        //         .build()
        // )
        // // since we have .disableCachingNullValues() we can skip this.
        // // .enableSpringCacheNullValueSupport()
        // .build();
        
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder().build();
        
        // defines DEFAULT configuration (fallback).
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        // defines CUSTOM configurations for specific caches.
        Map<String, RedisCacheConfiguration> customCacheConfigurations = new HashMap<>();
        // customCacheConfigurations.put("key", defaultCacheConfig.entryTtl(Duration.ofHours(24)));
        // customCacheConfigurations.put("key", defaultCacheConfig.entryTtl(Duration.ofMinutes(1)));

        // if we want zero @JsonTypeInfo annotations on our POJOs, we can register a typed serializer per cache name in customCacheConfigurations.
        // customCacheConfigurations.put("product", cacheConfigFor(ProductPojo.class, Duration.ofMinutes(30)));
        
        // build the manager and hand it over to Spring.
        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultCacheConfig) // DEFAULT fallback.
            .withInitialCacheConfigurations(customCacheConfigurations) // CUSTOM TTL map.
            .build();
    }

    // private <T> RedisCacheConfiguration cacheConfigFor(Class<T> type, Duration ttl) {
    //     GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder().build();
    //     return RedisCacheConfiguration.defaultCacheConfig()
    //         .entryTtl(ttl)
    //         .disableCachingNullValues()
    //         .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    // }

    @Override
    public CacheErrorHandler errorHandler() {

        return new SimpleCacheErrorHandler() {
              
            @Override
            public void handleCacheGetError(RuntimeException ex, org.springframework.cache.Cache cache, Object key) {
                log.error("Redis down while fetching key '{}' from cache '{}'.", key, cache.getName());
            }
            @Override
            public void handleCachePutError(RuntimeException ex, org.springframework.cache.Cache cache, Object key, Object value) {
                log.error("Redis down while saving key '{}' to cache '{}'.", key, cache.getName());
            }
            @Override
            public void handleCacheEvictError(RuntimeException ex, org.springframework.cache.Cache cache, Object key) {
                log.error("Redis down while evicting key '{}' from cache '{}'.", key, cache.getName());
            }
            @Override
            public void handleCacheClearError(RuntimeException ex, org.springframework.cache.Cache cache) {
                log.error("Redis down while clearing cache '{}'.", cache.getName());
            }
        };
    }
}