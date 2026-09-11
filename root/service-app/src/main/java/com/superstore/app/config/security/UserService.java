package com.superstore.app.config.security;

public interface UserService {
    User login(
        String username,
        String password);

    User register(
        String username,
        String mobile,
        String password);
}