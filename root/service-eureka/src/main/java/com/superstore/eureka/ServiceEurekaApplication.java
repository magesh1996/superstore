package com.superstore.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication(
	exclude = {
		// Database exclusions.
		DataSourceAutoConfiguration.class, 
		DataSourceTransactionManagerAutoConfiguration.class, 
		HibernateJpaAutoConfiguration.class,
		// Security exclusions to bypass the login page.
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
	}
)
@EnableEurekaServer // this activates the lookup dashboard and registry engine.
public class ServiceEurekaApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceEurekaApplication.class, args);
	}

}
