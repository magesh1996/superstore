package com.superstore.product.config.audit;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware") //activates auditing annotations like @CreatedDate and @LastModifiedDate.
public class JpaConfig {

}