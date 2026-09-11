package com.superstore.order.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.superstore.order.pojo.OrderPojo;
import com.superstore.order.service.OrderService;

@RestController
@RequestMapping("/order")
public class OrderController
{
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/all")
	public List<OrderPojo> getAllProduct() 
	{
		return orderService.getAllOrder();
	}

    @PostMapping("/save")
	public String saveOrder(@RequestBody OrderPojo order) 
	{
		orderService.saveOrder(order);
		return "order created successfully!";
	}
}
