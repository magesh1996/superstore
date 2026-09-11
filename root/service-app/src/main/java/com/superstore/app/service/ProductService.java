package com.superstore.app.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.superstore.app.client.ProductClient;
import com.superstore.app.facade.ProductFacade;
import com.superstore.app.pojo.ProductPojo;

@Service
public class ProductService implements ProductFacade {

    private final ProductClient productClient;

    public ProductService(ProductClient productClient) {
        this.productClient = productClient;
    }

    @Override
    public List<ProductPojo> getAllProduct() {
        return productClient.getAllProduct();
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