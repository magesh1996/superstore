package com.superstore.app.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.superstore.app.client.ProductClient;
import com.superstore.app.pojo.ProductPojo;

@ExtendWith(MockitoExtension.class)
class ProductServiceResilienceTest {

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldUseFallbackWhenProductServiceFails() {
        when(productClient.getAllProduct())
            .thenThrow(new RuntimeException("service-product down"));

        List<ProductPojo> result = productService.getAllProduct();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}