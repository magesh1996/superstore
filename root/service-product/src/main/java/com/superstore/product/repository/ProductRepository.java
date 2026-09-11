package com.superstore.product.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.superstore.product.entity.ProductEntity;
import com.superstore.product.entity.ProductIdEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, ProductIdEntity> {
    // basic CRUD is automatically available.

    public List<ProductEntity> findAllByOrderByIdCompanyAscIdSkuAsc();
    
    // returns the number of deleted records (1 if deleted, 0 if nothing was found).
    public long deleteByIdCompanyAndIdSku(String company, String sku);

    @Query("SELECT p FROM ProductEntity p WHERE (:company IS NULL OR p.id.company = :company) AND p.id.sku = :sku")
    // @Param not mandatory but explicitly defined is the safest approach.
    public ProductEntity findByIdCompanyAndIdSku(@Param("company") String company, @Param("sku") String sku);

    // deleteAllInBatch(Iterable) is a method from JpaRepository, not a custom method.
    // public long deleteAllInBatch(Set<ProductPojo> multiProduct);
}