package com.eshoppingzone.wallet.controller;

import com.eshoppingzone.wallet.dto.WalletRequest;
import com.eshoppingzone.wallet.dto.WalletResponse;
import com.eshoppingzone.wallet.service.WalletService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/wallets")
@Tag(name = "Digital Wallet", description = "Wallet Management APIs with RabbitMQ Event Consumer")
public class WalletController {

    private static final Logger logger = LoggerFactory.getLogger(WalletController.class);

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
        logger.info("WalletController initialized");
    }

    @PostMapping
    @Operation(summary = "Create wallet", description = "Creates a new wallet for a user with optional initial balance")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody WalletRequest request) {
        logger.info("POST /wallets - Creating wallet for user: {} with initial amount: {}", 
                request.getUserId(), request.getAmount());
        WalletResponse response = walletService.createWallet(request);
        logger.info("Wallet created with id: {} for user: {}", response.getId(), response.getUserId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get wallet by user ID", description = "Retrieves wallet for a specific user")
    public ResponseEntity<WalletResponse> getWalletByUserId(@PathVariable Long userId) {
        logger.info("GET /wallets/user/{} - Fetching wallet for user", userId);
        WalletResponse response = walletService.getWalletByUserId(userId);
        logger.info("Wallet found for user {}: balance = {}", userId, response.getBalance());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wallet by ID", description = "Retrieves wallet by its unique identifier")
    public ResponseEntity<WalletResponse> getWalletById(@PathVariable Long id) {
        logger.info("GET /wallets/{} - Fetching wallet by ID", id);
        WalletResponse response = walletService.getWalletById(id);
        logger.info("Wallet retrieved: id={}, balance={}", response.getId(), response.getBalance());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all wallets", description = "Retrieves all wallets in the system")
    public ResponseEntity<List<WalletResponse>> getAllWallets() {
        logger.info("GET /wallets - Fetching all wallets");
        List<WalletResponse> wallets = walletService.getAllWallets();
        logger.info("Retrieved {} wallets", wallets.size());
        return ResponseEntity.ok(wallets);
    }

    @PostMapping("/user/{userId}/add")
    @Operation(summary = "Add funds", description = "Adds funds to user's wallet")
    public ResponseEntity<WalletResponse> addFunds(
            @PathVariable Long userId,
            @RequestParam BigDecimal amount) {
        logger.info("POST /wallets/user/{}/add?amount={} - Adding funds to wallet", userId, amount);
        WalletResponse response = walletService.addFunds(userId, amount);
        logger.info("Funds added to wallet for user {}: new balance = {}", userId, response.getBalance());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/user/{userId}/deduct")
    @Operation(summary = "Deduct funds", description = "Deducts funds from user's wallet")
    public ResponseEntity<WalletResponse> deductFunds(
            @PathVariable Long userId,
            @RequestParam BigDecimal amount) {
        logger.info("POST /wallets/user/{}/deduct?amount={} - Deducting funds from wallet", userId, amount);
        WalletResponse response = walletService.deductFunds(userId, amount);
        logger.info("Funds deducted from wallet for user {}: new balance = {}", userId, response.getBalance());
        return ResponseEntity.ok(response);
    }
}
