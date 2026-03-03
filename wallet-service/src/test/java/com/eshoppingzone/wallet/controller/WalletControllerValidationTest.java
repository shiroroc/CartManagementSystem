package com.eshoppingzone.wallet.controller;

import com.eshoppingzone.wallet.dto.WalletRequest;
import com.eshoppingzone.wallet.dto.WalletResponse;
import com.eshoppingzone.wallet.exception.GlobalExceptionHandler;
import com.eshoppingzone.wallet.exception.InsufficientBalanceException;
import com.eshoppingzone.wallet.exception.ResourceNotFoundException;
import com.eshoppingzone.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller validation tests for WalletController.
 * 
 * Tests @Valid annotation + GlobalExceptionHandler behavior:
 * - MethodArgumentNotValidException handling (400 Bad Request)
 * - InsufficientBalanceException handling (400 Bad Request)
 * - ResourceNotFoundException handling (404 Not Found)
 * - Input validation edge cases (null, negative values)
 */
@ExtendWith(MockitoExtension.class)
class WalletControllerValidationTest {

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_WALLET_ID = 100L;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("500.00");

    @Mock
    private WalletService walletService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        WalletController walletController = new WalletController(walletService);
        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================
    
    private WalletResponse createWalletResponse(Long walletId, Long userId, BigDecimal balance) {
        WalletResponse response = new WalletResponse();
        response.setId(walletId);
        response.setUserId(userId);
        response.setBalance(balance);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());
        return response;
    }

    // =========================================================================
    // Create Wallet - Validation Tests
    // =========================================================================
    @Nested
    @DisplayName("Create Wallet - Validation")
    class CreateWalletValidationTests {

        @Test
        @DisplayName("createWallet_NullUserId_Returns400WithFieldError")
        void createWallet_NullUserId_Returns400WithFieldError() throws Exception {
            // Arrange - userId is null
            String requestBody = "{\"amount\":100.00}";

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("userId")))
                    .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("User ID is required")));

            // Verify service was never called
            verify(walletService, never()).createWallet(any());
        }

        @Test
        @DisplayName("createWallet_NullAmount_Returns400WithFieldError")
        void createWallet_NullAmount_Returns400WithFieldError() throws Exception {
            // Arrange - amount is null
            String requestBody = "{\"userId\":1}";

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("amount")))
                    .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Amount is required")));

            verify(walletService, never()).createWallet(any());
        }

        @Test
        @DisplayName("createWallet_NegativeAmount_Returns400WithFieldError")
        void createWallet_NegativeAmount_Returns400WithFieldError() throws Exception {
            // Arrange - negative amount violates @DecimalMin("0.00")
            WalletRequest request = new WalletRequest();
            request.setUserId(TEST_USER_ID);
            request.setAmount(new BigDecimal("-50.00"));

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("amount")))
                    .andExpect(jsonPath("$.fieldErrors[*].message", hasItem("Amount cannot be negative")));

            verify(walletService, never()).createWallet(any());
        }

        @Test
        @DisplayName("createWallet_BothFieldsNull_Returns400WithMultipleErrors")
        void createWallet_BothFieldsNull_Returns400WithMultipleErrors() throws Exception {
            // Arrange - Empty JSON body
            String requestBody = "{}";

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("userId")))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("amount")));

            verify(walletService, never()).createWallet(any());
        }

        @Test
        @DisplayName("createWallet_ValidRequest_Returns201Created")
        void createWallet_ValidRequest_Returns201Created() throws Exception {
            // Arrange
            WalletRequest request = new WalletRequest();
            request.setUserId(TEST_USER_ID);
            request.setAmount(INITIAL_BALANCE);

            WalletResponse expectedResponse = createWalletResponse(TEST_WALLET_ID, TEST_USER_ID, INITIAL_BALANCE);
            when(walletService.createWallet(any(WalletRequest.class))).thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(TEST_WALLET_ID.intValue())))
                    .andExpect(jsonPath("$.userId", is(TEST_USER_ID.intValue())))
                    .andExpect(jsonPath("$.balance", is(500.0)));
        }

        @Test
        @DisplayName("createWallet_ZeroAmount_IsValid")
        void createWallet_ZeroAmount_IsValid() throws Exception {
            // Arrange - Zero is valid (@DecimalMin("0.00") allows it)
            WalletRequest request = new WalletRequest();
            request.setUserId(TEST_USER_ID);
            request.setAmount(BigDecimal.ZERO);

            WalletResponse expectedResponse = createWalletResponse(TEST_WALLET_ID, TEST_USER_ID, BigDecimal.ZERO);
            when(walletService.createWallet(any(WalletRequest.class))).thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    // =========================================================================
    // Business Logic Exception Tests
    // =========================================================================
    @Nested
    @DisplayName("Business Logic Exceptions")
    class BusinessExceptionTests {

        @Test
        @DisplayName("deductFunds_InsufficientBalance_Returns400BadRequest")
        void deductFunds_InsufficientBalance_Returns400BadRequest() throws Exception {
            // Arrange
            BigDecimal deductAmount = new BigDecimal("1000.00");
            when(walletService.deductFunds(TEST_USER_ID, deductAmount))
                    .thenThrow(new InsufficientBalanceException(
                            "Insufficient balance. Current: 500.00, Required: 1000.00"));

            // Act & Assert
            mockMvc.perform(post("/wallets/user/{userId}/deduct", TEST_USER_ID)
                            .param("amount", deductAmount.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Insufficient Balance")))
                    .andExpect(jsonPath("$.message", is("Insufficient balance. Current: 500.00, Required: 1000.00")));
        }

        @Test
        @DisplayName("addFunds_NegativeAmount_ThrowsException")
        void addFunds_NegativeAmount_ThrowsException() throws Exception {
            // Arrange
            BigDecimal negativeAmount = new BigDecimal("-100.00");
            when(walletService.addFunds(TEST_USER_ID, negativeAmount))
                    .thenThrow(new IllegalArgumentException("Amount must be positive"));

            // Act & Assert
            mockMvc.perform(post("/wallets/user/{userId}/add", TEST_USER_ID)
                            .param("amount", negativeAmount.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Bad Request")));
        }

        @Test
        @DisplayName("deductFunds_NegativeAmount_ThrowsException")
        void deductFunds_NegativeAmount_ThrowsException() throws Exception {
            // Arrange
            BigDecimal negativeAmount = new BigDecimal("-50.00");
            when(walletService.deductFunds(TEST_USER_ID, negativeAmount))
                    .thenThrow(new IllegalArgumentException("Amount must be positive"));

            // Act & Assert
            mockMvc.perform(post("/wallets/user/{userId}/deduct", TEST_USER_ID)
                            .param("amount", negativeAmount.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Bad Request")));
        }

        @Test
        @DisplayName("getWalletByUserId_NonExistentUser_Returns404")
        void getWalletByUserId_NonExistentUser_Returns404() throws Exception {
            // Arrange
            Long nonExistentUserId = 999L;
            when(walletService.getWalletByUserId(nonExistentUserId))
                    .thenThrow(new ResourceNotFoundException("Wallet not found for user: 999"));

            // Act & Assert
            mockMvc.perform(get("/wallets/user/{userId}", nonExistentUserId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", is("Wallet not found for user: 999")));
        }

        @Test
        @DisplayName("getWalletById_NonExistentWallet_Returns404")
        void getWalletById_NonExistentWallet_Returns404() throws Exception {
            // Arrange
            Long nonExistentWalletId = 999L;
            when(walletService.getWalletById(nonExistentWalletId))
                    .thenThrow(new ResourceNotFoundException("Wallet not found with id: 999"));

            // Act & Assert
            mockMvc.perform(get("/wallets/{id}", nonExistentWalletId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", is("Wallet not found with id: 999")));
        }
    }

    // =========================================================================
    // Edge Case Tests
    // =========================================================================
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("createWallet_VeryLargeAmount_IsValid")
        void createWallet_VeryLargeAmount_IsValid() throws Exception {
            // Arrange - Very large amount (testing BigDecimal handling)
            BigDecimal largeAmount = new BigDecimal("999999999999.99");
            WalletRequest request = new WalletRequest();
            request.setUserId(TEST_USER_ID);
            request.setAmount(largeAmount);

            WalletResponse expectedResponse = createWalletResponse(TEST_WALLET_ID, TEST_USER_ID, largeAmount);
            when(walletService.createWallet(any(WalletRequest.class))).thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("addFunds_ZeroAmount_ThrowsException")
        void addFunds_ZeroAmount_ThrowsException() throws Exception {
            // Arrange - Zero amount might be invalid for add operation
            BigDecimal zeroAmount = BigDecimal.ZERO;
            when(walletService.addFunds(TEST_USER_ID, zeroAmount))
                    .thenThrow(new IllegalArgumentException("Amount must be greater than zero"));

            // Act & Assert
            mockMvc.perform(post("/wallets/user/{userId}/add", TEST_USER_ID)
                            .param("amount", zeroAmount.toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("createWallet_MalformedJson_Returns400")
        void createWallet_MalformedJson_Returns400() throws Exception {
            // Arrange
            String malformedJson = "{\"userId\": 1, \"amount\": }";

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("createWallet_InvalidAmountFormat_Returns400")
        void createWallet_InvalidAmountFormat_Returns400() throws Exception {
            // Arrange - Amount is not a valid number
            String invalidJson = "{\"userId\": 1, \"amount\": \"not-a-number\"}";

            // Act & Assert
            mockMvc.perform(post("/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("addFunds_ValidOperation_Returns200")
        void addFunds_ValidOperation_Returns200() throws Exception {
            // Arrange
            BigDecimal addAmount = new BigDecimal("150.00");
            BigDecimal newBalance = INITIAL_BALANCE.add(addAmount);
            WalletResponse expectedResponse = createWalletResponse(TEST_WALLET_ID, TEST_USER_ID, newBalance);
            when(walletService.addFunds(TEST_USER_ID, addAmount)).thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/wallets/user/{userId}/add", TEST_USER_ID)
                            .param("amount", addAmount.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance", is(650.0)));
        }

        @Test
        @DisplayName("deductFunds_ValidOperation_Returns200")
        void deductFunds_ValidOperation_Returns200() throws Exception {
            // Arrange
            BigDecimal deductAmount = new BigDecimal("100.00");
            BigDecimal newBalance = INITIAL_BALANCE.subtract(deductAmount);
            WalletResponse expectedResponse = createWalletResponse(TEST_WALLET_ID, TEST_USER_ID, newBalance);
            when(walletService.deductFunds(TEST_USER_ID, deductAmount)).thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/wallets/user/{userId}/deduct", TEST_USER_ID)
                            .param("amount", deductAmount.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance", is(400.0)));
        }
    }

    // =========================================================================
    // Exact Balance Deduction Tests
    // =========================================================================
    @Nested
    @DisplayName("Exact Balance Scenarios")
    class ExactBalanceTests {

        @Test
        @DisplayName("deductFunds_ExactBalance_Returns200")
        void deductFunds_ExactBalance_Returns200() throws Exception {
            // Arrange - Deduct exactly the wallet balance
            BigDecimal deductAmount = INITIAL_BALANCE;
            WalletResponse expectedResponse = createWalletResponse(TEST_WALLET_ID, TEST_USER_ID, BigDecimal.ZERO);
            when(walletService.deductFunds(TEST_USER_ID, deductAmount)).thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/wallets/user/{userId}/deduct", TEST_USER_ID)
                            .param("amount", deductAmount.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance", is(0.0)));
        }

        @Test
        @DisplayName("deductFunds_OneMoreThanBalance_Returns400")
        void deductFunds_OneMoreThanBalance_Returns400() throws Exception {
            // Arrange - Try to deduct $0.01 more than balance
            BigDecimal deductAmount = INITIAL_BALANCE.add(new BigDecimal("0.01"));
            when(walletService.deductFunds(TEST_USER_ID, deductAmount))
                    .thenThrow(new InsufficientBalanceException(
                            "Insufficient balance. Current: 500.00, Required: 500.01"));

            // Act & Assert
            mockMvc.perform(post("/wallets/user/{userId}/deduct", TEST_USER_ID)
                            .param("amount", deductAmount.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error", is("Insufficient Balance")));
        }
    }
}
