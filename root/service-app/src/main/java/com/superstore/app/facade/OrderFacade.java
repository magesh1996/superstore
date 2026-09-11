package com.superstore.app.facade;

import java.util.List;

import com.superstore.app.pojo.OrderPojo;

public interface OrderFacade {
    List<OrderPojo> getAllOrder();
    void saveOrder(OrderPojo order);
}