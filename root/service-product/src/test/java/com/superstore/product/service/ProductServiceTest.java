package com.superstore.product.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.superstore.product.entity.ProductEntity;
import com.superstore.product.entity.ProductIdEntity;
import com.superstore.product.mapper.ProductMapper;
import com.superstore.product.pojo.ProductPojo;
import com.superstore.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ProductService selfProxy;

    @InjectMocks
    private ProductService productService;

    @Captor
    ArgumentCaptor<List<Document>> captor;

    @Test
    void testDeleteMultiProduct() {
        ProductPojo productPojo = buildProductPojo();
        ProductEntity productEntity = buildProductEntity();

        when(productMapper.pojoListToEntityList(any())).thenReturn(List.of(productEntity));

        productService.deleteMultiProduct(Set.of(productPojo));

        verify(productRepository).deleteAllInBatch(List.of(productEntity));
    }

    @Test
    void testDeleteProduct() {
        productService.deleteProduct("COMPANY-A", "SKU-1");

        verify(productRepository).deleteByIdCompanyAndIdSku("COMPANY-A", "SKU-1");
    }

    @Test
    void testGetAllProduct() {
        ProductEntity productEntity = buildProductEntity();
        ProductPojo productPojo = buildProductPojo();

        assertTimeoutPreemptively(Duration.ofSeconds(6), () -> {
            when(productRepository.findAllByOrderByIdCompanyAscIdSkuAsc()).thenReturn(List.of(productEntity));
            when(productMapper.entityListToPojoList(List.of(productEntity))).thenReturn(List.of(productPojo));

            List<ProductPojo> result = productService.getAllProduct();

            assertEquals(1, result.size());
            assertEquals("COMPANY-A", result.get(0).getCompany());
            assertEquals("SKU-1", result.get(0).getSku());
        });
    }

    @Test
    void testLoadAllProductsToVectorStore() {
        ProductEntity productEntity = buildProductEntity();

        when(productRepository.findAll(PageRequest.of(0, 100)))
            .thenReturn(new PageImpl<>(List.of(productEntity)));

        productService.loadAllProductsToVectorStore();

        // ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.captor();
        verify(vectorStore).add(captor.capture());

        assertEquals(1, captor.getValue().size());
    }

    @Test
    void testSaveProduct() {
        ProductPojo productPojo = buildProductPojo();
        ProductEntity productEntity = buildProductEntity();

        when(productMapper.pojoToEntity(productPojo)).thenReturn(productEntity);

        productService.saveProduct(productPojo);

        verify(productRepository).save(productEntity);
    }

    @Test
    void testSearchProduct() {
        ProductPojo first = buildProductPojo();
        ProductPojo second = buildProductPojo();
        second.setName("Bread");

        when(selfProxy.getAllProduct()).thenReturn(List.of(first, second));

        List<ProductPojo> result = productService.searchProduct("milk");

        assertEquals(1, result.size());
        assertEquals("Milk", result.get(0).getName());
    }

    @Test
    void testUpdateStock() {
        String orderJson = """
            {
              "ordHdr": {
                "ordNo": 1001,
                "mobile": 9999999999
              },
              "ordDtl": [
                {
                  "lineNo": 1,
                  "company": "COMPANY-A",
                  "sku": "SKU-1",
                  "qty": 2
                }
              ]
            }
            """;

        assertDoesNotThrow(() -> productService.updateStock(orderJson));
    }

    private ProductPojo buildProductPojo() {
        ProductPojo pojo = new ProductPojo();
        pojo.setCompany("COMPANY-A");
        pojo.setSku("SKU-1");
        pojo.setName("Milk");
        pojo.setCategory("Dairy");
        pojo.setBarcode("123456");
        return pojo;
    }

    private ProductEntity buildProductEntity() {
        ProductEntity entity = new ProductEntity();
        entity.setId(new ProductIdEntity("COMPANY-A", "SKU-1"));
        entity.setName("Milk");
        entity.setCategory("Dairy");
        entity.setBarcode("123456");
        return entity;
    }
}