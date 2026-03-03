package com.eshoppingzone.order.controller;

import com.eshoppingzone.order.dto.OrderRequest;
import com.eshoppingzone.order.dto.OrderResponse;
import com.eshoppingzone.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Tag(name = "Order Management", description = "Order Processing APIs with Payment Simulation and RabbitMQ Events")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
        logger.info("OrderController initialized");
    }

    @PostMapping
    @Operation(summary = "Create a new order", description = "Creates an order, processes simulated Stripe payment, and publishes event to RabbitMQ")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        logger.info("POST /orders - Creating order for user: {} with amount: {}", 
                request.getUserId(), request.getTotalAmount());
        OrderResponse response = orderService.createOrder(request);
        logger.info("Order created successfully with id: {} and status: {}", 
                response.getId(), response.getStatus());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieves an order by its unique identifier")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        logger.info("GET /orders/{} - Fetching order by ID", id);
        OrderResponse response = orderService.getOrderById(id);
        logger.info("Order retrieved: id={}, status={}", response.getId(), response.getStatus());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user ID", description = "Retrieves all orders for a specific user")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId) {
        logger.info("GET /orders/user/{} - Fetching orders for user", userId);
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        logger.info("Retrieved {} orders for user {}", orders.size(), userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieves all orders in the system")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        logger.info("GET /orders - Fetching all orders");
        List<OrderResponse> orders = orderService.getAllOrders();
        logger.info("Retrieved {} total orders", orders.size());
        return ResponseEntity.ok(orders);
    }
}
