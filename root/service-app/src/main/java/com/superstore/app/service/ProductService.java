package com.superstore.app.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.superstore.app.client.ProductClient;
import com.superstore.app.facade.ProductFacade;
import com.superstore.app.pojo.ProductPojo;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class ProductService implements ProductFacade {

    private final ProductClient productClient;

    public ProductService(ProductClient productClient) {
        this.productClient = productClient;
    }

    @CircuitBreaker(
        name = "productService",
        fallbackMethod = "getAllProductFallback"
    )
    @Override
    public List<ProductPojo> getAllProduct() {
        return productClient.getAllProduct();
    }
    
    // getAllProductFallback is declared to public from private because it is not called directly in Java code, 
    // rather it is invoked reflectively by Resilience4j's AOP framework at runtime.
    // the fallback method must:
        // be in the same class
        // have the same original parameters
        // add a final Throwable parameter
        // return the same type
    public List<ProductPojo> getAllProductFallback(Throwable throwable) {
        return List.of();
        // do not return an empty list for operations where that could be confused with valid data.
    }

    @Override
    public void saveProduct(ProductPojo product) {
        productClient.saveProduct(product);
    }

    @Override
    public void deleteProduct(String company, String sku) {
        productClient.deleteProduct(company, sku);
    }

    public void deleteMultiProduct(Set<ProductPojo> multiProduct) {
        productClient.deleteMultiProduct(multiProduct);
    }

    @Override
    public List<ProductPojo> searchProduct(String searchText) {
        return productClient.searchProduct(searchText);
    }
}