package com.superstore.product.mapper;

import java.util.Collection;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.superstore.product.entity.ProductEntity;
import com.superstore.product.pojo.ProductPojo;

@SuppressWarnings("all")
@Mapper(componentModel = "spring") // tells MapStruct to make this a Spring Bean.
public interface ProductMapper {
    
    // to map different name. 
    //@Mapping(source = "emailAddress", target = "email")

    @Mapping(source = "company", target = "id.company")
    @Mapping(source = "sku", target = "id.sku")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity pojoToEntity(ProductPojo productPojo);

    @Mapping(source = "id.company", target = "company")
    @Mapping(source = "id.sku", target = "sku")
    ProductPojo entityToPojo(ProductEntity productEntity);
    
    // MapStruct will automatically look at the method above to process each item in this list.
    List<ProductPojo> entityListToPojoList(Collection<ProductEntity> listProductEntity);

    List<ProductEntity> pojoListToEntityList(Collection<ProductPojo> listProductPojo);
}