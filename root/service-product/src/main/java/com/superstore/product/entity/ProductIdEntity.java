package com.superstore.product.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductIdEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "COMPANY", length = 50, nullable = false)
    private String company;

    @Column(name = "SKU", length = 50, nullable = false)
    private String sku;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductIdEntity)) return false;
        ProductIdEntity that = (ProductIdEntity) o;
        return Objects.equals(company, that.company) &&
               Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(company, sku);
    }

    @Override
    public String toString() {
        return (company != null ? company : "") + ":" + (sku != null ? sku : "");
    }
}