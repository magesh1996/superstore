package com.superstore.order.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.superstore.order.entity.OrdDtlEntity;
import com.superstore.order.entity.OrdHdrEntity;
import com.superstore.order.mapper.OrderMapper;
import com.superstore.order.pojo.OrderPojo;
import com.superstore.order.repository.OrdDtlRepository;
import com.superstore.order.repository.OrdHdrRepository;

@Service
public class OrderFileImportService {

    private final OrderMapper orderMapper;
    private final OrdHdrRepository ordHdrRepository;
    private final OrdDtlRepository ordDtlRepository;

    public OrderFileImportService(
        OrderMapper orderMapper,
        OrdHdrRepository ordHdrRepository,
        OrdDtlRepository ordDtlRepository) {
        this.orderMapper = orderMapper;
        this.ordHdrRepository = ordHdrRepository;
        this.ordDtlRepository = ordDtlRepository;
    }

    @Transactional
    public void importOrders(List<OrderPojo> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("the file contains no orders");
        }

        for (OrderPojo order : orders) {
            validate(order);
        }

        for (OrderPojo order : orders) {
            Long ordNo = order.getOrdHdr().getOrdNo();

            if (ordHdrRepository.existsById(ordNo)) {
                throw new IllegalArgumentException("order already exists: " + ordNo);
            }

            OrdHdrEntity header = orderMapper.ordHdrPojoToOrdHdrEntity(order.getOrdHdr());
            ordHdrRepository.save(header);

            List<OrdDtlEntity> details = orderMapper.listOrdDtlPojoToListOrdDtlEntity(ordNo, order.getOrdDtl());
            ordDtlRepository.saveAll(details);
        }
    }

    private void validate(OrderPojo order) {
        if (order == null || order.getOrdHdr() == null) {
            throw new IllegalArgumentException("order header is required");
        }
        if (order.getOrdHdr().getOrdNo() == null || order.getOrdHdr().getMobile() == null) {
            throw new IllegalArgumentException("order number and mobile are required");
        }
        if (order.getOrdDtl() == null || order.getOrdDtl().isEmpty()) {
            throw new IllegalArgumentException("an order must contain details");
        }

        Set<Long> lineNumbers = new HashSet<>();
        for (OrderPojo.OrdDtl detail : order.getOrdDtl()) {
            if (detail == null 
                || detail.getLineNo() == null
                || detail.getCompany() == null 
                || detail.getSku() == null
                || detail.getName() == null 
                || detail.getQty() == null) {
                throw new IllegalArgumentException("each detail requires line number, company, sku, name, and qty");
            }
            if (!lineNumbers.add(detail.getLineNo())) {
                throw new IllegalArgumentException("duplicate line number: " + detail.getLineNo());
            }
        }
    }
}