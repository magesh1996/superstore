package com.superstore.app.facade;

import java.util.List;
import java.util.Set;

import com.superstore.app.pojo.ProductPojo;

public interface ProductFacade {
    List<ProductPojo> getAllProduct();    
    void saveProduct(ProductPojo product);
    void deleteProduct(String company, String sku);
    void deleteMultiProduct(Set<ProductPojo> multiProduct);
    List<ProductPojo> searchProduct(String filter);
}