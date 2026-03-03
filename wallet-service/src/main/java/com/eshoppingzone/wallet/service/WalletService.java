package com.eshoppingzone.wallet.service;

import com.eshoppingzone.wallet.config.RabbitMQConfig;

import com.eshoppingzone.wallet.dto.OrderPlacedEvent;
import com.eshoppingzone.wallet.dto.WalletRequest;
import com.eshoppingzone.wallet.dto.WalletResponse;
import com.eshoppingzone.wallet.entity.Wallet;
import com.eshoppingzone.wallet.exception.InsufficientBalanceException;
import com.eshoppingzone.wallet.exception.ResourceNotFoundException;
import com.eshoppingzone.wallet.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public WalletResponse createWallet(WalletRequest request) {
        if (walletRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("Wallet already exists for user: " + request.getUserId());
        }

        Wallet wallet = new Wallet();
        wallet.setUserId(request.getUserId());
        wallet.setBalance(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());

        Wallet savedWallet = walletRepository.save(wallet);
        logger.info("Wallet created for userId={}, initialBalance={}", savedWallet.getUserId(), savedWallet.getBalance());
        return mapToResponse(savedWallet);
    }

    public WalletResponse getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
        return mapToResponse(wallet);
    }

    public WalletResponse getWalletById(Long id) {
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + id));
        return mapToResponse(wallet);
    }

    public List<WalletResponse> getAllWallets() {
        return walletRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WalletResponse addFunds(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(LocalDateTime.now());

        Wallet savedWallet = walletRepository.save(wallet);
        logger.info("Funds added: userId={}, amount={}, newBalance={}", userId, amount, savedWallet.getBalance());
        return mapToResponse(savedWallet);
    }

    @Transactional
    public WalletResponse deductFunds(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Current: " + wallet.getBalance() + ", Required: " + amount);
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setUpdatedAt(LocalDateTime.now());

        Wallet savedWallet = walletRepository.save(wallet);
        logger.info("Funds deducted: userId={}, amount={}, newBalance={}", userId, amount, savedWallet.getBalance());
        return mapToResponse(savedWallet);
    }

    /**
     * RabbitMQ Consumer: Listens for OrderPlacedEvent from order-service.
     * Deducts the order totalAmount from the user's wallet balance.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_PLACED_QUEUE)
    @Transactional
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        logger.info("========== RABBITMQ EVENT RECEIVED ==========");
        logger.info("Received OrderPlacedEvent: {}", event);

        try {
            Wallet wallet = walletRepository.findByUserId(event.getUserId())
                    .orElse(null);

            if (wallet == null) {
                logger.warn("No wallet found for userId={}. Creating new wallet with zero balance.", event.getUserId());
                wallet = new Wallet();
                wallet.setUserId(event.getUserId());
                wallet.setBalance(BigDecimal.ZERO);
                wallet.setCreatedAt(LocalDateTime.now());
                wallet = walletRepository.save(wallet);
            }

            BigDecimal previousBalance = wallet.getBalance();
            BigDecimal deductAmount = event.getTotalAmount();

            if (wallet.getBalance().compareTo(deductAmount) < 0) {
                logger.warn("INSUFFICIENT BALANCE for userId={}: balance={}, orderAmount={}",
                        event.getUserId(), wallet.getBalance(), deductAmount);
                throw new InsufficientBalanceException(
                        "Insufficient balance for order. Current: " + wallet.getBalance() + ", Required: " + deductAmount);
            }

            wallet.setBalance(wallet.getBalance().subtract(deductAmount));
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);

            logger.info("WALLET DEDUCTION COMPLETE:");
            logger.info("  Order ID: {}", event.getOrderId());
            logger.info("  User ID: {}", event.getUserId());
            logger.info("  Previous Balance: {}", previousBalance);
            logger.info("  Deducted Amount: {}", deductAmount);
            logger.info("  New Balance: {}", wallet.getBalance());
            logger.info("==============================================");

        } catch (Exception e) {
            logger.error("Error processing OrderPlacedEvent: {}", e.getMessage(), e);
            throw e; // Re-throw to trigger RabbitMQ retry/dead-letter handling
        }
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        WalletResponse response = new WalletResponse();
        response.setId(wallet.getId());
        response.setUserId(wallet.getUserId());
        response.setBalance(wallet.getBalance());
        response.setCreatedAt(wallet.getCreatedAt());
        response.setUpdatedAt(wallet.getUpdatedAt());
        return response;
    }
}
