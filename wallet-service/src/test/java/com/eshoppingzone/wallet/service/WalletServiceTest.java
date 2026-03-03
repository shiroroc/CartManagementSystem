package com.eshoppingzone.wallet.service;

import com.eshoppingzone.wallet.dto.OrderPlacedEvent;
import com.eshoppingzone.wallet.entity.Wallet;
import com.eshoppingzone.wallet.exception.InsufficientBalanceException;
import com.eshoppingzone.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository);
    }

    @Test
    @DisplayName("handleOrderPlacedEvent_Success - Deducts totalAmount and saves wallet")
    void handleOrderPlacedEvent_Success() {
        // Arrange
        Long userId = 1L;
        Long orderId = 100L;
        BigDecimal initialBalance = new BigDecimal("500.00");
        BigDecimal orderAmount = new BigDecimal("150.00");
        BigDecimal expectedBalance = new BigDecimal("350.00");

        Wallet wallet = new Wallet();
        wallet.setId(1L);
        wallet.setUserId(userId);
        wallet.setBalance(initialBalance);
        wallet.setCreatedAt(LocalDateTime.now());

        OrderPlacedEvent event = new OrderPlacedEvent(orderId, userId, orderAmount);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        walletService.handleOrderPlacedEvent(event);

        // Assert - Verify wallet balance was correctly deducted
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(walletCaptor.capture());

        Wallet savedWallet = walletCaptor.getValue();
        assertNotNull(savedWallet);
        assertEquals(userId, savedWallet.getUserId());
        assertEquals(0, expectedBalance.compareTo(savedWallet.getBalance()),
                "Expected balance: " + expectedBalance + ", Actual: " + savedWallet.getBalance());
        assertNotNull(savedWallet.getUpdatedAt());
    }

    @Test
    @DisplayName("handleOrderPlacedEvent_InsufficientFunds - Throws InsufficientBalanceException")
    void handleOrderPlacedEvent_InsufficientFunds() {
        // Arrange
        Long userId = 2L;
        Long orderId = 200L;
        BigDecimal initialBalance = new BigDecimal("50.00");
        BigDecimal orderAmount = new BigDecimal("150.00"); // More than balance

        Wallet wallet = new Wallet();
        wallet.setId(2L);
        wallet.setUserId(userId);
        wallet.setBalance(initialBalance);
        wallet.setCreatedAt(LocalDateTime.now());

        OrderPlacedEvent event = new OrderPlacedEvent(orderId, userId, orderAmount);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // Act & Assert
        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> walletService.handleOrderPlacedEvent(event)
        );

        // Verify exception message contains balance info
        assertNotNull(exception.getMessage());
        assertEquals(
                "Insufficient balance for order. Current: " + initialBalance + ", Required: " + orderAmount,
                exception.getMessage()
        );

        // Verify wallet was NOT saved when insufficient funds
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("handleOrderPlacedEvent_NewWalletCreated - Creates wallet when not found")
    void handleOrderPlacedEvent_NewWalletCreated() {
        // Arrange
        Long userId = 3L;
        Long orderId = 300L;
        BigDecimal orderAmount = new BigDecimal("100.00");

        OrderPlacedEvent event = new OrderPlacedEvent(orderId, userId, orderAmount);

        // No existing wallet - returns empty
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        // Mock save to return wallet with ID
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            w.setId(3L);
            return w;
        });

        // Act & Assert - Should throw exception since new wallet has zero balance
        InsufficientBalanceException exception = assertThrows(
                InsufficientBalanceException.class,
                () -> walletService.handleOrderPlacedEvent(event)
        );

        // Verify new wallet was created first
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(walletCaptor.capture());

        Wallet createdWallet = walletCaptor.getValue();
        assertEquals(userId, createdWallet.getUserId());
        assertEquals(BigDecimal.ZERO, createdWallet.getBalance());
    }
}
