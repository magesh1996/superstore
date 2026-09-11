package com.superstore.product.mcp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.superstore.product.pojo.ProductPojo;
import com.superstore.product.service.ProductService;

@Component
public class ProductMcpTools {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductService productService;

    public ProductMcpTools(ProductService productService) {
        this.productService = productService;
    }

    @McpTool(description = "fetch detailed information about a specific product by its company and sku")
    public ProductPojo getProductDetail(
        @McpToolParam(description = "company name", required = false) String company,
        @McpToolParam(description = "product SKU", required = true) String sku) {
        logger.info("fetching detail for product : [{}]", sku);
        return productService.getProductDetail(company, sku);
    }

    @McpTool(description = "search for products in the store by search text (company, name, category or barcode)")
    public List<ProductPojo> searchProduct(
        @McpToolParam(description = "search text", required = true) String searchText) {
        logger.info("searching for products with text : [{}]", searchText);
        return productService.searchProduct(searchText);
    }

    // name is optional, if not provided, the method name will be used as the tool name
    @McpTool(name = "all_product", description = "list all available products in the store")
    public List<ProductPojo> getAllProduct() {
        return productService.getAllProduct();
    }
}
