package com.superstore.app.client;

import java.util.List;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import com.superstore.app.pojo.OrderPojo;

@HttpExchange("/order")
public interface OrderClient {

    @GetExchange("/all")
    List<OrderPojo> getAllOrder();

    @PostExchange("/save")
    String saveOrder(@RequestBody OrderPojo order);
}