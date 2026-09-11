package com.superstore.product.config.vector;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.superstore.product.service.ProductService;

@Configuration
public class ProductVectorLoadInitializer {
    @Bean
    public ApplicationRunner initializeVectorStore(ProductService productService) {
        return args -> {
            // call this on application startup
            productService.loadAllProductsToVectorStore();
        };
    }
}
