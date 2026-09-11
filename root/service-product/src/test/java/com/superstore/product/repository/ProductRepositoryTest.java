package com.superstore.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.superstore.product.config.audit.AuditConfig;
import com.superstore.product.config.audit.JpaConfig;
import com.superstore.product.entity.ProductEntity;
import com.superstore.product.entity.ProductIdEntity;

@DataJpaTest
@Import({JpaConfig.class, AuditConfig.class})
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void testDeleteByIdCompanyAndIdSku() {
        ProductEntity first = saveProduct("COMPANY-A", "SKU-1", "Apple");
        ProductEntity second = saveProduct("COMPANY-A", "SKU-2", "Banana");
        ProductEntity third = saveProduct("COMPANY-B", "SKU-1", "Cherry");

        entityManager.flush();
        entityManager.clear();

        long deletedCount = repository.deleteByIdCompanyAndIdSku("COMPANY-A", "SKU-1");

        assertThat(deletedCount).isEqualTo(1L);
        assertThat(repository.findById(first.getId())).isEmpty();
        assertThat(repository.findById(second.getId())).isPresent();
        assertThat(repository.findById(third.getId())).isPresent();
    }

    @Test
    void testFindAllByOrderByIdCompanyAscIdSkuAsc() {
        saveProduct("ZETA", "SKU-2", "Zeta");
        saveProduct("ALPHA", "SKU-10", "Alpha-10");
        saveProduct("ALPHA", "SKU-2", "Alpha-2");
        saveProduct("ALPHA", "SKU-1", "Alpha-1");

        entityManager.flush();
        entityManager.clear();

        List<ProductEntity> products = repository.findAllByOrderByIdCompanyAscIdSkuAsc();

        assertThat(products)
            .extracting(product -> product.getId().getCompany() + ":" + product.getId().getSku())
            // .containsExactly("ALPHA:SKU-1", "ALPHA:SKU-2", "ALPHA:SKU-10", "ZETA:SKU-2"); // lexicographic order
            .containsExactly("ALPHA:SKU-1", "ALPHA:SKU-10", "ALPHA:SKU-2", "ZETA:SKU-2");
    }

    private ProductEntity saveProduct(String company, String sku, String name) {
        ProductEntity product = ProductEntity.builder()
                .id(ProductIdEntity.builder()
                        .company(company)
                        .sku(sku)
                        .build())
                .name(name)
                .build();

        return repository.saveAndFlush(product);
    }
}