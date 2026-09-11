package com.superstore.app.config.session;

import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import com.superstore.app.config.redis.RedisCacheConfig;

@SuppressWarnings({ "unchecked", "rawtypes" })
class DynamicSessionRepositoryProxy implements SessionRepository<Session> {

    private final SessionRepository redisDelegate;
    private final SessionRepository memoryDelegate;
    private final RedisCacheConfig config; // to re-check Redis.

    public DynamicSessionRepositoryProxy(
            SessionRepository redisDelegate,
            SessionRepository memoryDelegate,
            RedisCacheConfig config) {
        this.redisDelegate = redisDelegate;
        this.memoryDelegate = memoryDelegate;
        this.config = config;
    }

    private SessionRepository activeDelegate() {
        return config.isRedisAvailable() ? redisDelegate : memoryDelegate;
    }

    @Override
    public Session createSession() {
        return activeDelegate().createSession();
    }

    @Override
    public void save(Session session) {
        activeDelegate().save(session);
    }

    @Override
    public Session findById(String id) {
        return (Session) activeDelegate().findById(id);
    }

    @Override
    public void deleteById(String id) {
        activeDelegate().deleteById(id);
    }
}