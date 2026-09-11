package com.superstore.product.config.vector;

import com.superstore.product.entity.ProductEntity;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ProductVectorSyncListener {

    private static VectorStore vectorStore;

    public ProductVectorSyncListener(VectorStore vectorStore) {
        ProductVectorSyncListener.vectorStore = vectorStore;
    }

    // JPA requires a default constructor when JPA events (@PostPersist, @PostUpdate, @PostRemove) trigger, JPA creates its own instance of ProductVectorSyncListener.
    public ProductVectorSyncListener() {}

    @PostPersist
    @PostUpdate
    public void onProductSaveOrUpdate(ProductEntity product) {
        if (vectorStore == null) return;

        // optionally ignore inactive products from being indexed
        if (!Boolean.TRUE.equals(product.getActive())) {
            onProductDelete(product);
            return;
        }

        Document doc = toVectorDocument(product);
        vectorStore.add(List.of(doc)); // upserts document into VectorStore automatically
    }

    @PostRemove
    public void onProductDelete(ProductEntity product) {
        if (vectorStore == null) return;

        String documentId = product.getId() != null ? product.getId().toString() : null;
        if (documentId != null) {
            vectorStore.delete(List.of(documentId));
        }
    }

    public Document toVectorDocument(ProductEntity product) {
        String company = (product.getId() != null && product.getId().getCompany() != null) ? product.getId().getCompany() : "";
        String sku = (product.getId() != null && product.getId().getSku() != null) ? product.getId().getSku() : "";
        String documentId = product.getId() != null ? product.getId().toString() : UUID.randomUUID().toString();

        // 1. unstructured text representation for semantic searching.
        String textContent = String.format(
            "Company: %s. SKU: %s. Product Name: %s. " +
            "Department: %s. Category: %s. " +
            "UPC: %s. Barcode: %s. " +
            "MRP: %s. Cost Price: %s. Sell Price: %s. " +
            "Stock: %s. Threshold: %s. " +
            "HSN Code: %s. Tax Rate: %s. Cess Rate: %s. " +
            "Tax Inclusive: %s. Tax Exempted: %s. " +
            "PKD Date: %s. EXP Date: %s. Active: %s. ",
            company, sku, product.getName() != null ? product.getName() : "",
            product.getDepartment() != null ? product.getDepartment() : "N/A", product.getCategory() != null ? product.getCategory() : "N/A",
            product.getUpc() != null ? product.getUpc() : "N/A", product.getBarcode() != null ? product.getBarcode() : "N/A",
            product.getMrp() != null ? product.getMrp() : BigDecimal.ZERO, product.getCostPrice() != null ? product.getCostPrice() : BigDecimal.ZERO, product.getSellPrice() != null ? product.getSellPrice() : BigDecimal.ZERO,
            product.getStock() != null ? product.getStock() : 0L, product.getThreshold() != null ? product.getThreshold() : 0L,
            product.getHsnCode() != null ? product.getHsnCode() : "N/A", product.getTaxRate() != null ? product.getTaxRate() : BigDecimal.ZERO, product.getCessRate() != null ? product.getCessRate() : BigDecimal.ZERO,
            Boolean.TRUE.equals(product.getTaxInclusive()), Boolean.TRUE.equals(product.getTaxExempted()),
            product.getPkdDate(), product.getExpDate(), Boolean.TRUE.equals(product.getActive())
        );

        // 2. metadata fields for filtered searches.
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("company", company);
        metadata.put("sku", sku);
        metadata.put("name", product.getName() != null ? product.getName() : "N/A");
        metadata.put("department", product.getDepartment() != null ? product.getDepartment() : "N/A");
        metadata.put("category", product.getCategory() != null ? product.getCategory() : "N/A");
        metadata.put("upc", product.getUpc() != null ? product.getUpc() : "N/A");
        metadata.put("barcode", product.getBarcode() != null ? product.getBarcode() : "N/A");
        metadata.put("mrp", product.getMrp() != null ? product.getMrp().doubleValue() : 0.0);        
        metadata.put("cost_price", product.getCostPrice() != null ? product.getCostPrice().doubleValue() : 0.0);
        metadata.put("sell_price", product.getSellPrice() != null ? product.getSellPrice().doubleValue() : 0.0);
        metadata.put("stock", product.getStock() != null ? product.getStock() : 0L);
        metadata.put("threshold", product.getThreshold() != null ? product.getThreshold() : 0L);
        metadata.put("hsn_code", product.getHsnCode() != null ? product.getHsnCode() : "N/A");
        metadata.put("tax_rate", product.getTaxRate() != null ? product.getTaxRate().doubleValue() : 0.0);
        metadata.put("cess_rate", product.getCessRate() != null ? product.getCessRate().doubleValue() : 0.0);
        metadata.put("tax_inclusive", Boolean.TRUE.equals(product.getTaxInclusive()));
        metadata.put("tax_exempted", Boolean.TRUE.equals(product.getTaxExempted()));
        metadata.put("pkd_date", product.getPkdDate() != null ? product.getPkdDate() : "N/A");
        metadata.put("exp_date", product.getExpDate() != null ? product.getExpDate() : "N/A");
        metadata.put("active", Boolean.TRUE.equals(product.getActive()));
        metadata.put("in_stock", product.getStock() != null && product.getStock() > 0);

        return new Document(documentId, textContent, metadata);
    }
}