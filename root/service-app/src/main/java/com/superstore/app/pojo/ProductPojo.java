package com.superstore.app.pojo;

import java.math.BigDecimal;
import java.time.LocalDate;

// import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
// @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class ProductPojo {
    // PK
    private String company;
    private String sku;

    private String name;
    private String department;
    private String category;

    private String upc;
    private String barcode;

    private BigDecimal mrp;
    private BigDecimal costPrice;
    private BigDecimal sellPrice;

    private Long stock;
    private Long threshold;

    private String hsnCode;
    private BigDecimal taxRate;
    private BigDecimal cessRate;
    // public BigDecimal getCessRate() {
    //     // fallback to ZERO if incoming request leaves it blank or null.
    //     return cessRate == null ? BigDecimal.ZERO : cessRate;
    // }
    private boolean taxInclusive;
    private boolean taxExempted;

    private LocalDate pkdDate;
    private LocalDate expDate;

    private boolean active;
}