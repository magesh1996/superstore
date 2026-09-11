package com.superstore.app.client;

import java.util.List;
import java.util.Set;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.superstore.app.pojo.ProductPojo;

@HttpExchange("/product")
public interface ProductClient {

    @GetExchange("/all")
    List<ProductPojo> getAllProduct();

    @PostExchange("/save")
    String saveProduct(@RequestBody ProductPojo product);

    @DeleteExchange("/delete")
    String deleteProduct(@RequestParam String company, @RequestParam String sku);

    @DeleteExchange("/deleteMulti")
    String deleteMultiProduct(@RequestBody Set<ProductPojo> multiProduct);

    @GetExchange("/search")
    List<ProductPojo> searchProduct(@RequestParam String searchText);
}