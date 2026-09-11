package com.superstore.app.config.security;

// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserService implements UserService {

    private final UserRepository userRepository;
    // private final PasswordEncoder passwordEncoder;

    public DatabaseUserService(UserRepository userRepository
        // , PasswordEncoder passwordEncoder
        ) {
        this.userRepository = userRepository;
        // this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String username, String mobile, String password) {

        User user = new User();
        user.setUsername(username);
        user.setMobile(mobile);
        // user.setPassword(passwordEncoder.encode(password));
        user.setPassword(password);
        
        return userRepository.save(user);
    }
    @Override
    public User login(String username, String password) {

        User user = userRepository.findByUsernameOrMobile(username, username)
                    .orElseThrow(() -> new RuntimeException("invalid username"));

        // if (!passwordEncoder.matches(password, user.getPassword())) {
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("invalid password");
        }

        return user;
    }
}