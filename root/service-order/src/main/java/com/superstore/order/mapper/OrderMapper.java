package com.superstore.order.mapper;

import java.util.List;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.superstore.order.entity.OrdDtlEntity;
import com.superstore.order.entity.OrdDtlIdEntity;
import com.superstore.order.entity.OrdHdrEntity;
import com.superstore.order.pojo.OrderPojo;

@SuppressWarnings("all")
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "createUser", ignore = true)
    @Mapping(target = "createTstamp", ignore = true)
    @Mapping(target = "modifyUser", ignore = true)
    @Mapping(target = "modifyTstamp", ignore = true)
    @Mapping(target = "activeFlag", ignore = true)
    OrdHdrEntity ordHdrPojoToOrdHdrEntity(OrderPojo.OrdHdr ordHdrPojo);

    OrderPojo.OrdHdr ordHdrEntityToOrdHdrPojo(OrdHdrEntity ordHdrEntity);

    default OrdDtlIdEntity mapId(Long ordNo, Long lineNo) {
        if (ordNo == null && lineNo == null) {
            return null;
        }
        return OrdDtlIdEntity.builder()
            .ordNo(ordNo)
            .lineNo(lineNo)
            .build();
    }

    // @Mapping(target = "id", expression = "java(mapId(ordNo, ordDtlPojo.getLineNo()))")
    @Mapping(target = "id", expression = "java(ordDtlPojo != null ? mapId(ordNo, ordDtlPojo.getLineNo()) : null)")
    @Mapping(target = "createUser", ignore = true)
    @Mapping(target = "createTstamp", ignore = true)
    @Mapping(target = "modifyUser", ignore = true)
    @Mapping(target = "modifyTstamp", ignore = true)
    @Mapping(target = "activeFlag", ignore = true)
    OrdDtlEntity ordDtlPojoToOrdDtlEntity(Long ordNo, OrderPojo.OrdDtl ordDtlPojo);

    @Mapping(source = "id.lineNo", target = "lineNo")
    OrderPojo.OrdDtl ordDtlEntityToOrdDtlPojo(OrdDtlEntity ordDtlEntity);

    // custom method to map the List of ordDtl.
    default List<OrdDtlEntity> listOrdDtlPojoToListOrdDtlEntity(Long ordNo, List<OrderPojo.OrdDtl> listOrdDtlPojo) {
        if (listOrdDtlPojo == null) {
            return null;
        }
        return listOrdDtlPojo.stream()
            .map(ordDtlPojo -> ordDtlPojoToOrdDtlEntity(ordNo, ordDtlPojo))
            .toList();
    }

    List<OrderPojo.OrdDtl> listOrdDtlEntityToListOrdDtlPojo(List<OrdDtlEntity> listOrdDtlEntity);
}