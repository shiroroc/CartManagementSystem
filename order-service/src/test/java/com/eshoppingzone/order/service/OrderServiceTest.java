package com.eshoppingzone.order.service;

import com.eshoppingzone.order.config.RabbitMQConfig;
import com.eshoppingzone.order.dto.OrderPlacedEvent;
import com.eshoppingzone.order.dto.OrderRequest;
import com.eshoppingzone.order.dto.OrderResponse;
import com.eshoppingzone.order.entity.Order;
import com.eshoppingzone.order.exception.PaymentFailedException;
import com.eshoppingzone.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RestTemplate restTemplate;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, rabbitTemplate, restTemplate);
        // Set the @Value fields via reflection for testing
        ReflectionTestUtils.setField(orderService, "stripeApiUrl", "https://api.stripe.com/v1/charges");
        ReflectionTestUtils.setField(orderService, "stripeApiKey", "sk_test_placeholder");
    }

    @Test
    @DisplayName("createOrder_Success - Payment succeeds, order confirmed, RabbitMQ event published once")
    void createOrder_Success() {
        // Arrange
        Long userId = 1L;
        BigDecimal amount = new BigDecimal("150.00");
        OrderRequest request = new OrderRequest(userId, amount);

        Order pendingOrder = new Order();
        pendingOrder.setId(1L);
        pendingOrder.setUserId(userId);
        pendingOrder.setTotalAmount(amount);
        pendingOrder.setStatus("PENDING");

        Order confirmedOrder = new Order();
        confirmedOrder.setId(1L);
        confirmedOrder.setUserId(userId);
        confirmedOrder.setTotalAmount(amount);
        confirmedOrder.setStatus("CONFIRMED");

        // Mock repository saves - return orders with IDs set
        when(orderRepository.save(any(Order.class)))
                .thenReturn(pendingOrder)      // First save (PENDING)
                .thenReturn(confirmedOrder);    // Second save (CONFIRMED)

        // Mock Stripe API call to return 200 OK
        ResponseEntity<String> stripeResponse = new ResponseEntity<>("{\"id\":\"ch_test\"}", HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenReturn(stripeResponse);

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(userId, response.getUserId());
        assertEquals(amount, response.getTotalAmount());
        assertEquals("CONFIRMED", response.getStatus());

        // Verify order was saved twice (PENDING then CONFIRMED)
        verify(orderRepository, times(2)).save(any(Order.class));

        // Verify RabbitMQ event was published exactly once
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq(RabbitMQConfig.ROUTING_KEY),
                eventCaptor.capture()
        );

        OrderPlacedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(1L, publishedEvent.getOrderId());
        assertEquals(userId, publishedEvent.getUserId());
        assertEquals(amount, publishedEvent.getTotalAmount());
    }

    @Test
    @DisplayName("createOrder_PaymentFailed - Stripe fails, order marked FAILED, exception thrown")
    void createOrder_PaymentFailed() {
        // Arrange
        Long userId = 2L;
        BigDecimal amount = new BigDecimal("200.00");
        OrderRequest request = new OrderRequest(userId, amount);

        Order pendingOrder = new Order();
        pendingOrder.setId(2L);
        pendingOrder.setUserId(userId);
        pendingOrder.setTotalAmount(amount);
        pendingOrder.setStatus("PENDING");

        Order failedOrder = new Order();
        failedOrder.setId(2L);
        failedOrder.setUserId(userId);
        failedOrder.setTotalAmount(amount);
        failedOrder.setStatus("FAILED");

        // Mock repository saves
        when(orderRepository.save(any(Order.class)))
                .thenReturn(pendingOrder)   // First save (PENDING)
                .thenReturn(failedOrder);   // Second save (FAILED)

        // Mock Stripe API call to throw exception (payment failure)
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Card declined"));

        // Act & Assert
        PaymentFailedException exception = assertThrows(
                PaymentFailedException.class,
                () -> orderService.createOrder(request)
        );

        // Verify exception message contains order ID
        assertNotNull(exception.getMessage());
        assertEquals("Payment failed for order: 2", exception.getMessage());

        // Verify order was saved twice (PENDING then FAILED)
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());

        // Second save should have FAILED status
        Order savedFailedOrder = orderCaptor.getAllValues().get(1);
        assertEquals("FAILED", savedFailedOrder.getStatus());

        // Verify RabbitMQ event was NOT published on failure
        verify(rabbitTemplate, never()).convertAndSend(
                anyString(),
                anyString(),
                any(OrderPlacedEvent.class)
        );
    }
}
