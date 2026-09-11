package com.superstore.chatbot.config.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsernameOrMobile(String username, String mobile);
}