package com.superstore.product.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.superstore.product.config.vector.ProductVectorSyncListener;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
// added indexes for lightning-fast POS barcode lookups.
@Table(name = "PRODUCT", indexes = {
 // REMOVED redundant PK index (DB handles this automatically via @EmbeddedId).
 // COMPANY is the leftmost (first) column in that automatic primary index, any query filtering by just WHERE company = ? will already use the primary index.
 // @Index(name = "idx_product_pk",             columnList = "COMPANY, SKU"),
 // @Index(name = "idx_product_company",        columnList = "COMPANY"),
    @Index(name = "idx_product_sku",            columnList = "SKU"),
    @Index(name = "idx_product_name",           columnList = "NAME"),
    @Index(name = "idx_product_department",     columnList = "DEPARTMENT"),
    @Index(name = "idx_product_category",       columnList = "CATEGORY"),    
    @Index(name = "idx_product_upc",            columnList = "UPC"),
    @Index(name = "idx_product_barcode",        columnList = "BARCODE"),
    @Index(name = "idx_product_hsncode",        columnList = "HSN_CODE")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// @EntityListeners(AuditingEntityListener.class) // MUST for audit.
@EntityListeners({ AuditingEntityListener.class, ProductVectorSyncListener.class })
public class ProductEntity {

    @EmbeddedId
    private ProductIdEntity id;

    @Column(name = "NAME", length = 200, nullable = false)
    private String name;

    @Column(name = "DEPARTMENT", length = 50, nullable = true)
    private String department;

    @Column(name = "CATEGORY", length = 50, nullable = true)
    private String category;

    @Column(name = "UPC", length = 50, nullable = true)
    private String upc;

    @Column(name = "BARCODE", length = 50, nullable = true)
    private String barcode;

    @Builder.Default
    @Column(name = "MRP", precision = 10, scale = 2, nullable = false)
    private BigDecimal mrp = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "COST_PRICE", precision = 10, scale = 2, nullable = false)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "SELL_PRICE", precision = 10, scale = 2, nullable = false)
    private BigDecimal sellPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "STOCK", nullable = false)
    private Long stock = 0L;

    @Builder.Default
    @Column(name = "THRESHOLD", nullable = false)
    private Long threshold = 0L;

    @Column(name = "HSN_CODE", length = 8, nullable = true)
    private String hsnCode;

    @Builder.Default
    @Column(name = "TAX_RATE", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxRate = BigDecimal.ZERO; // like 5.00, 12.00, 18.00, 28.00

    @Builder.Default
    @Column(name = "CESS_RATE", precision = 5, scale = 2, nullable = false)
    private BigDecimal cessRate = BigDecimal.ZERO; // like luxury/compensation cess.

    @Builder.Default
    @Column(name = "TAX_INCLUSIVE", nullable = false)
    private Boolean taxInclusive = true; // true if MRP/Selling Price already includes GST.

    @Builder.Default
    @Column(name = "TAX_EXEMPTED", nullable = false)
    private Boolean taxExempted = false; // true for 0% non-taxable essentials (like fresh milk/grains).

    @Column(name = "PKD_DATE", nullable = true)
    private LocalDate pkdDate;

    @Column(name = "EXP_DATE", nullable = true)
    private LocalDate expDate;

    @Column(name = "CREATED_BY", length = 50, nullable = false, updatable = false)
    @CreatedBy
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @CreatedDate // // set automatically on save.
    private LocalDateTime createdAt; 

    @Column(name = "UPDATED_BY", length = 50, nullable = false)
    @LastModifiedBy
    private String updatedBy;

    @Column(name = "UPDATED_AT", nullable = false)
    @LastModifiedDate // update automatically on save/update.
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "ACTIVE", nullable = false)
    // @Column(name = "ACTIVE_FLAG", nullable = false)
    // private String activeFlag = "Y";
    private Boolean active = true; // Boolean returns default null.

    // primitive boolean initialized to true.
    // the only time it will ever be false is if we explicitly built it that way : 
    // ProductEntity.builder().active(false).build();
    // @PrePersist method will now explicitly find those deactivated items and force them back to true right before they hit the database.
    // so, COMMENTED.
    // @PrePersist
    // protected void onCreate() {
    //     // if (this.activeFlag == null || this.activeFlag.isBlank()) {
    //     //     this.activeFlag = "Y";
    //     // }
    //     if (this.active == false)
    //         this.active = true;
    // }

    @PrePersist
    protected void onCreate() {
        if (this.mrp == null) {
            this.mrp = BigDecimal.ZERO;
        }
        if (this.costPrice == null) {
            this.costPrice = BigDecimal.ZERO;
        }
        if (this.sellPrice == null) {
            this.sellPrice = BigDecimal.ZERO;
        }
        if (this.stock == null) {
            this.stock = 0L;
        }
        if (this.threshold == null) {
            this.threshold = 0L;
        }
        if (this.taxRate == null) {
            this.taxRate = BigDecimal.ZERO;
        }
        if (this.cessRate == null) {
            this.cessRate = BigDecimal.ZERO;
        }
        if (this.active == null || Boolean.FALSE.equals(this.active)) {
            this.active = true;
        }
    }
}