package com.eshoppingzone.order.service;

import com.eshoppingzone.order.config.RabbitMQConfig;
import com.eshoppingzone.order.dto.OrderPlacedEvent;
import com.eshoppingzone.order.dto.OrderRequest;
import com.eshoppingzone.order.dto.OrderResponse;
import com.eshoppingzone.order.entity.Order;
import com.eshoppingzone.order.exception.PaymentFailedException;
import com.eshoppingzone.order.exception.ResourceNotFoundException;
import com.eshoppingzone.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_FAILED = "FAILED";

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;

    @Value("${stripe.api.url:https://api.stripe.com/v1/charges}")
    private String stripeApiUrl;

    @Value("${stripe.api.key:sk_test_placeholder}")
    private String stripeApiKey;

    public OrderService(OrderRepository orderRepository, RabbitTemplate rabbitTemplate, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // Create order with PENDING status
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus(STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        logger.info("Order created with PENDING status: orderId={}", savedOrder.getId());

        // Process payment
        boolean paymentSuccess = processPayment(savedOrder.getId(), request.getTotalAmount());

        if (paymentSuccess) {
            savedOrder.setStatus(STATUS_CONFIRMED);
            savedOrder.setUpdatedAt(LocalDateTime.now());
            savedOrder = orderRepository.save(savedOrder);
            logger.info("Order confirmed: orderId={}", savedOrder.getId());

            // Publish event to RabbitMQ
            publishOrderPlacedEvent(savedOrder);
        } else {
            savedOrder.setStatus(STATUS_FAILED);
            savedOrder.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(savedOrder);
            logger.error("Order payment failed: orderId={}", savedOrder.getId());
            throw new PaymentFailedException("Payment failed for order: " + savedOrder.getId());
        }

        return mapToResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Simulates a Stripe payment.
     * In production, this would make an actual API call to Stripe.
     */
    /**
     * Executes a REAL API call to the Stripe Sandbox.
     */
    private boolean processPayment(Long orderId, BigDecimal amount) {
        logger.info("Initiating REAL Stripe payment for orderId={}, amount={}", orderId, amount);

        try {
            // 1. Set up headers with your secret key
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(stripeApiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 2. Build the Stripe charge payload (Form URL Encoded)
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            
            // Stripe requires the amount in cents (e.g., $150.00 = 15000)
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
            
            map.add("amount", String.valueOf(amountInCents));
            map.add("currency", "usd");
            map.add("source", "tok_visa"); // Stripe's universal test token for a successful card
            map.add("description", "CartManagementSystem Order #" + orderId);

            // 3. Execute the POST request
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(stripeApiUrl, request, String.class);
            
            logger.info("Stripe Payment Successful. Response Status: {}", response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            // Log the exact error from Stripe for debugging
            logger.error("Stripe Payment Failed for orderId={}: {}", orderId, e.getMessage());
            return false;
        }
    }

    /**
     * Publishes an OrderPlacedEvent to RabbitMQ.
     */
    private void publishOrderPlacedEvent(Order order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY,
                    event
            );
            logger.info("OrderPlacedEvent published to RabbitMQ: {}", event);
        } catch (Exception e) {
            logger.error("Failed to publish OrderPlacedEvent: {}", e.getMessage());
            // Don't fail the order if event publishing fails
            // In production, consider retry logic or dead letter queue
        }
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }
}
