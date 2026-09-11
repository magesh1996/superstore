package com.superstore.order.config.redis;

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
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableCaching // mandatory to activate cache.
@EnableSpringHttpSession // provides manual control over HTTP session.
public class RedisCacheConfig implements CachingConfigurer {

    // *** IMPORTANT ***
    // Cluster/Sentinel Environments : 
    // if our production environment uses Redis Sentinel or a Redis Cluster layout rather than a standalone endpoint, 
    // we need to replace RedisStandaloneConfiguration inside redisConnectionFactory() with RedisSentinelConfiguration or RedisClusterConfiguration.

    private static final Logger log = LoggerFactory.getLogger("RedisCacheConfig");

    private final MapSessionRepository fallbackMemorySessionRepository = new MapSessionRepository(new ConcurrentHashMap<>());
    
    private static Boolean isRedisUp = null;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        
        SocketOptions socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofSeconds(2)) // set a strict 2-second timeout for connection attempts.
            .build();

        ClientOptions clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(clientOptions)
            .commandTimeout(Duration.ofSeconds(2)) // don't hang threads indefinitely.
            .build();

        // stops Lettuce from EAGER validating and locking up if the Redis server is missing at startup.
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.setValidateConnection(false);
        factory.setShareNativeConnection(false);
        
        return factory;
    }

    // reusable, thread-safe connection check to verify if physical Redis is online.
    // safe, non-static connection validation that honors injected Spring properties.
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
            factory.getConnection().close(); // attempt connection handshake.
            log.info("Redis is UP, building RedisCacheManager.");
            isRedisUp = true;
        } catch (Exception e) {
            log.error("Redis is DOWN at startup: {}, so activating local in-memory caching (ConcurrentMapCacheManager).", e.getMessage());
            isRedisUp = false;
        } finally {
            if (factory != null) {
                factory.destroy(); // prevent socket leaks during the connection probe.
            }
        }
        return isRedisUp;
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

    // define our own SessionRepository bean and force the auto-configuration to back off.
    @Bean
    @Primary 
    public SessionRepository<?> sessionRepository() {
        
        SessionRepository<?> activeDelegate;

        if (checkRedisConnection()) {
            log.info("Redis is UP, using Redis for HTTP Session storage.");
            
            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(redisConnectionFactory());
            template.afterPropertiesSet();
            
            RedisSessionRepository redisRepo = new RedisSessionRepository(template);
            // CRITICAL : force IMMEDIATE flushes so headers contain the cookie before Vaadin writes response data.
            redisRepo.setFlushMode(org.springframework.session.FlushMode.IMMEDIATE);
            activeDelegate = redisRepo;
        } else {
            log.warn("Redis is DOWN, swapping HTTP Session storage to standard local application memory.");
            // returns the exact same persistent reference every time.
            activeDelegate = this.fallbackMemorySessionRepository;
        }

        return new DynamicSessionRepositoryProxy(activeDelegate);
    }

    // CRITICAL : tells Spring Session to pass your custom JSESSIONID rules to the Servlet Filter chain.
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        CookieHttpSessionIdResolver idResolver = new CookieHttpSessionIdResolver();
        idResolver.setCookieSerializer(cookieSerializer());
        return idResolver;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setCookiePath("/");
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(false);
        return serializer;
    }

    public RedisCacheManager buildRedisCacheManager(LettuceConnectionFactory factory) {

        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder().build();
        
        // defines DEFAULT configuration (fallback).
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        // defines CUSTOM configurations for specific caches.
        Map<String, RedisCacheConfiguration> customCacheConfigurations = new HashMap<>();
        customCacheConfigurations.put("order-all", defaultCacheConfig.entryTtl(Duration.ofHours(24)));
        //customCacheConfigurations.put("order-", defaultCacheConfig.entryTtl(Duration.ofMinutes(1)));
        
        // build the manager and hand it over to Spring.
        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultCacheConfig) // DEFAULT fallback.
            .withInitialCacheConfigurations(customCacheConfigurations) // CUSTOM TTL map.
            .build();
    }

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

// clean, explicitly typed wrapper preventing type-capture errors in the runtime engine.
@SuppressWarnings({ "unchecked", "rawtypes" })
class DynamicSessionRepositoryProxy implements SessionRepository<Session> {
    
    private final SessionRepository delegate;

    public DynamicSessionRepositoryProxy(SessionRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Session createSession() {
        return this.delegate.createSession();
    }

    @Override
    public void save(Session session) {
        this.delegate.save(session);
    }

    @Override
    public Session findById(String id) {
        return (Session) this.delegate.findById(id);
    }

    @Override
    public void deleteById(String id) {
        this.delegate.deleteById(id);
    }
}