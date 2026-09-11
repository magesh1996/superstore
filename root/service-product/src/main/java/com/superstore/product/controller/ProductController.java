package com.superstore.product.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.superstore.product.pojo.ProductPojo;
import com.superstore.product.service.ProductService;

@RestController
@RequestMapping("/product")
public class ProductController
{
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/all")
	public List<ProductPojo> getAllProduct() 
	{
		return productService.getAllProduct();
	}

    @PostMapping("/save")
	public ResponseEntity<String> saveProduct(@RequestBody ProductPojo product) 
	{
		productService.saveProduct(product);
		return new ResponseEntity<>("product created successfully!", HttpStatus.CREATED);
	}

	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteProduct(@RequestParam String company, @RequestParam String sku) 
	{
		productService.deleteProduct(company, sku);
		return new ResponseEntity<>("product deleted successfully!", HttpStatus.OK);
	}

	@DeleteMapping("/deleteMulti")
	public ResponseEntity<String> deleteMultiProduct(@RequestBody Set<ProductPojo> multiProduct) 
	{
		productService.deleteMultiProduct(multiProduct);
		return new ResponseEntity<>("product(s) deleted successfully!", HttpStatus.OK);
	}

	@GetMapping("/search")
	public List<ProductPojo> searchProduct(@RequestParam String searchText) 
	{
		return productService.searchProduct(searchText);
	}
}