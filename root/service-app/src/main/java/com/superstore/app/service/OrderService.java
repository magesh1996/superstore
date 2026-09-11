package com.superstore.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.superstore.app.client.OrderClient;
import com.superstore.app.facade.OrderFacade;
import com.superstore.app.pojo.OrderPojo;

@Service
public class OrderService implements OrderFacade {

    private final OrderClient orderClient;

    public OrderService(OrderClient orderClient) {
        this.orderClient = orderClient;
    }

    @Override
    public List<OrderPojo> getAllOrder() {
        return orderClient.getAllOrder();
    }

    @Override
    public void saveOrder(OrderPojo order) {
        orderClient.saveOrder(order);
    }
}