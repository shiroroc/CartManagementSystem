# CartManagementSystem - Phase 1.5 Security Testing Suite

## Postman Test Collection for API Gateway Security Verification

This document provides a comprehensive Postman test suite for validating the security architecture implemented in Phase 1.5. All requests are routed through the API Gateway at `http://localhost:8080`.

---

## Table of Contents

1. [Environment Setup](#environment-setup)
2. [Authentication Tests](#1-authentication-tests)
3. [Security Bypass Attempt Tests](#2-security-bypass-attempt-tests)
4. [Profile Service Tests](#3-profile-service-tests)
5. [Product Service Tests](#4-product-service-tests)
6. [Cart Service Tests](#5-cart-service-tests)
7. [Order Service Tests](#6-order-service-tests)
8. [Wallet Service Tests](#7-wallet-service-tests)
9. [Security Attack Tests](#8-security-attack-tests)
10. [End-to-End Workflow Tests](#9-end-to-end-workflow-tests)

---

## Environment Setup

### Postman Environment Variables

Create a Postman environment with these variables:

| Variable | Initial Value | Description |
|----------|---------------|-------------|
| `baseUrl` | `http://localhost:8080` | API Gateway URL |
| `jwt` | (empty) | JWT token from login |
| `userId` | (empty) | Authenticated user ID |
| `productId` | (empty) | Product ID for testing |
| `walletId` | (empty) | Wallet ID for testing |
| `orderId` | (empty) | Order ID for testing |

### Pre-request Script for Authenticated Requests

Add this script to automatically include JWT token:

```javascript
if (pm.environment.get("jwt")) {
    pm.request.headers.add({
        key: "Authorization",
        value: "Bearer " + pm.environment.get("jwt")
    });
}
```

---

## 1. Authentication Tests

### 1.1 Register New User (SUCCESS)

| Property | Value |
|----------|-------|
| **Test Name** | Register Admin User |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "fullName": "Admin User",
    "email": "admin@eshoppingzone.com",
    "password": "SecurePassword123!"
}
```

**Expected Response:**
- **Status:** `201 Created`
- **Body:**
```json
{
    "id": 1,
    "fullName": "Admin User",
    "email": "admin@eshoppingzone.com"
}
```

**Post-response Script:**
```javascript
if (pm.response.code === 201) {
    var jsonData = pm.response.json();
    pm.environment.set("userId", jsonData.id);
    console.log("User registered with ID: " + jsonData.id);
}
pm.test("User registration successful", function() {
    pm.response.to.have.status(201);
    pm.expect(pm.response.json()).to.have.property("id");
    pm.expect(pm.response.json()).to.not.have.property("password");
});
```

---

### 1.2 Register Duplicate User (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Attempt Duplicate Registration |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "fullName": "Admin User Again",
    "email": "admin@eshoppingzone.com",
    "password": "DifferentPassword456!"
}
```

**Expected Response:**
- **Status:** `409 Conflict`
- **Body:**
```json
{
    "timestamp": "...",
    "status": 409,
    "error": "Conflict",
    "message": "User already exists with email: admin@eshoppingzone.com"
}
```

**Test Script:**
```javascript
pm.test("Duplicate registration rejected", function() {
    pm.response.to.have.status(409);
    pm.expect(pm.response.json().message).to.include("already exists");
});
```

---

### 1.3 Login Successful (SUCCESS)

| Property | Value |
|----------|-------|
| **Test Name** | Login with Valid Credentials |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/login` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "email": "admin@eshoppingzone.com",
    "password": "SecurePassword123!"
}
```

**Expected Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 1,
    "email": "admin@eshoppingzone.com",
    "fullName": "Admin User",
    "expiresIn": 86400
}
```

**Post-response Script:**
```javascript
pm.test("Login successful with JWT token", function() {
    pm.response.to.have.status(200);
    var jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property("token");
    pm.expect(jsonData).to.have.property("userId");
    pm.expect(jsonData).to.have.property("email");
    pm.expect(jsonData).to.have.property("expiresIn");
    
    // Store JWT for subsequent requests
    pm.environment.set("jwt", jsonData.token);
    pm.environment.set("userId", jsonData.userId);
    console.log("JWT Token stored. Expires in " + jsonData.expiresIn + " seconds");
});
```

---

### 1.4 Login Wrong Password (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Login with Wrong Password |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/login` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "email": "admin@eshoppingzone.com",
    "password": "WrongPassword999!"
}
```

**Expected Response:**
- **Status:** `401 Unauthorized`
- **Body:**
```json
{
    "timestamp": "...",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid email or password"
}
```

**Test Script:**
```javascript
pm.test("Wrong password returns 401", function() {
    pm.response.to.have.status(401);
    pm.expect(pm.response.json().message).to.equal("Invalid email or password");
});
```

---

### 1.5 Login Non-existent Email (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Login with Non-existent Email |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/login` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "email": "nonexistent@example.com",
    "password": "AnyPassword123!"
}
```

**Expected Response:**
- **Status:** `401 Unauthorized`
- **Body:**
```json
{
    "timestamp": "...",
    "status": 401,
    "error": "Unauthorized",
    "message": "Invalid email or password"
}
```

**Test Script:**
```javascript
pm.test("Non-existent email returns same error (no user enumeration)", function() {
    pm.response.to.have.status(401);
    // Same message for both wrong password and non-existent email (security)
    pm.expect(pm.response.json().message).to.equal("Invalid email or password");
});
```

---

### 1.6 Login Missing Email (VALIDATION FAILURE)

| Property | Value |
|----------|-------|
| **Test Name** | Login with Missing Email |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/login` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "password": "SecurePassword123!"
}
```

**Expected Response:**
- **Status:** `400 Bad Request`
- **Body:**
```json
{
    "timestamp": "...",
    "status": 400,
    "error": "Validation Failed",
    "message": "Input validation failed",
    "fieldErrors": [
        {
            "field": "email",
            "message": "Email is required",
            "rejectedValue": "null"
        }
    ]
}
```

**Test Script:**
```javascript
pm.test("Missing email validation error", function() {
    pm.response.to.have.status(400);
    pm.expect(pm.response.json().error).to.equal("Validation Failed");
    pm.expect(pm.response.json().fieldErrors[0].field).to.equal("email");
});
```

---

### 1.7 Login Invalid Email Format (VALIDATION FAILURE)

| Property | Value |
|----------|-------|
| **Test Name** | Login with Invalid Email Format |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/login` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "email": "not-an-email",
    "password": "SecurePassword123!"
}
```

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Invalid email format rejected", function() {
    pm.response.to.have.status(400);
    var fieldErrors = pm.response.json().fieldErrors;
    var emailError = fieldErrors.find(e => e.field === "email");
    pm.expect(emailError.message).to.include("valid email");
});
```

---

### 1.8 Verify Password Success

| Property | Value |
|----------|-------|
| **Test Name** | Verify Correct Password |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/verify-password` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "userId": {{userId}},
    "password": "SecurePassword123!"
}
```

**Expected Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
    "valid": true
}
```

**Test Script:**
```javascript
pm.test("Password verification successful", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json().valid).to.be.true;
});
```

---

### 1.9 Verify Password Failure

| Property | Value |
|----------|-------|
| **Test Name** | Verify Wrong Password |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/verify-password` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "userId": {{userId}},
    "password": "WrongPassword999!"
}
```

**Expected Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
    "valid": false
}
```

**Test Script:**
```javascript
pm.test("Wrong password verification returns false", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json().valid).to.be.false;
});
```

---

## 2. Security Bypass Attempt Tests

### 2.1 Access Wallets Without JWT (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Wallets Bypass - No Token |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/wallets` |
| **Headers** | None |
| **Auth Required** | ✅ Yes (MISSING) |

**Expected Response:**
- **Status:** `401 Unauthorized`

**Test Script:**
```javascript
pm.test("Wallets endpoint blocks unauthorized access", function() {
    pm.response.to.have.status(401);
});
```

---

### 2.2 Access Orders Without JWT (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Orders Bypass - No Token |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/orders` |
| **Headers** | None |
| **Auth Required** | ✅ Yes (MISSING) |

**Expected Response:**
- **Status:** `401 Unauthorized`

**Test Script:**
```javascript
pm.test("Orders endpoint blocks unauthorized access", function() {
    pm.response.to.have.status(401);
});
```

---

### 2.3 Access Products Without JWT (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Products Bypass - No Token |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | None |
| **Auth Required** | ✅ Yes (MISSING) |

**Expected Response:**
- **Status:** `401 Unauthorized`

**Test Script:**
```javascript
pm.test("Products endpoint blocks unauthorized access", function() {
    pm.response.to.have.status(401);
});
```

---

### 2.4 Access Cart Without JWT (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Cart Bypass - No Token |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/cart/1` |
| **Headers** | None |
| **Auth Required** | ✅ Yes (MISSING) |

**Expected Response:**
- **Status:** `401 Unauthorized`

**Test Script:**
```javascript
pm.test("Cart endpoint blocks unauthorized access", function() {
    pm.response.to.have.status(401);
});
```

---

### 2.5 Access with Expired JWT (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Expired JWT Token |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | `Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBlc2hvcHBpbmd6b25lLmNvbSIsInVzZXJJZCI6MSwiaWF0IjoxNjA5NDU5MjAwLCJleHAiOjE2MDk0NTkyMDF9.expired` |
| **Auth Required** | ✅ Yes (EXPIRED) |

**Expected Response:**
- **Status:** `401 Unauthorized`

**Test Script:**
```javascript
pm.test("Expired JWT rejected", function() {
    pm.response.to.have.status(401);
});
```

---

### 2.6 Access with Tampered JWT (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Tampered JWT Token |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/wallets` |
| **Headers** | `Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoYWNrZXJAZXZpbC5jb20iLCJ1c2VySWQiOjk5OTk5LCJpYXQiOjE2MDk0NTkyMDAsImV4cCI6MTkwOTU0NTYwMH0.INVALID_SIGNATURE` |
| **Auth Required** | ✅ Yes (TAMPERED) |

**Expected Response:**
- **Status:** `401 Unauthorized`

**Test Script:**
```javascript
pm.test("Tampered JWT rejected", function() {
    pm.response.to.have.status(401);
});
```

---

### 2.7 Access with Malformed JWT (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Malformed JWT Token |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/orders` |
| **Headers** | `Authorization: Bearer not.a.valid.jwt.token.at.all` |
| **Auth Required** | ✅ Yes (MALFORMED) |

**Expected Response:**
- **Status:** `401 Unauthorized`

**Test Script:**
```javascript
pm.test("Malformed JWT rejected", function() {
    pm.response.to.have.status(401);
});
```

---

### 2.8 Access with Missing Bearer Prefix (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Missing Bearer Prefix |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | `Authorization: {{jwt}}` (no "Bearer " prefix) |
| **Auth Required** | ✅ Yes (INVALID FORMAT) |

**Expected Response:**
- **Status:** `401 Unauthorized`

**Test Script:**
```javascript
pm.test("Missing Bearer prefix rejected", function() {
    pm.response.to.have.status(401);
});
```

---

## 3. Profile Service Tests

### 3.1 Get All Users (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Get All Users |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/profiles` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`
- **Body:** Array of user objects

**Test Script:**
```javascript
pm.test("Get all users successful", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json()).to.be.an("array");
});
```

---

### 3.2 Get User by ID (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Get User by ID |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/profiles/{{userId}}` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`
- **Body:**
```json
{
    "id": 1,
    "fullName": "Admin User",
    "email": "admin@eshoppingzone.com"
}
```

**Test Script:**
```javascript
pm.test("Get user by ID successful", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json().id).to.equal(pm.environment.get("userId"));
});
```

---

### 3.3 Get Non-existent User (404 EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Get Non-existent User |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/profiles/9999` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `404 Not Found`

**Test Script:**
```javascript
pm.test("Non-existent user returns 404", function() {
    pm.response.to.have.status(404);
});
```

---

### 3.4 Update User (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Update User Profile |
| **Method** | `PUT` |
| **URL** | `{{baseUrl}}/profiles/{{userId}}` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "fullName": "Admin User Updated",
    "email": "admin@eshoppingzone.com",
    "password": "NewSecurePassword456!"
}
```

**Expected Response:**
- **Status:** `200 OK`

**Test Script:**
```javascript
pm.test("Update user successful", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json().fullName).to.equal("Admin User Updated");
});
```

---

## 4. Product Service Tests

### 4.1 Create Product (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Create New Product |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "name": "Gaming Laptop",
    "price": 1999.99,
    "stock": 50,
    "category": "Electronics"
}
```

**Expected Response:**
- **Status:** `201 Created`

**Post-response Script:**
```javascript
pm.test("Product created successfully", function() {
    pm.response.to.have.status(201);
    pm.environment.set("productId", pm.response.json().id);
});
```

---

### 4.2 Create Product Invalid Price (VALIDATION FAILURE)

| Property | Value |
|----------|-------|
| **Test Name** | Create Product - Negative Price |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "name": "Invalid Product",
    "price": -100.00,
    "stock": 50,
    "category": "Electronics"
}
```

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Negative price validation error", function() {
    pm.response.to.have.status(400);
    pm.expect(pm.response.json().error).to.equal("Validation Failed");
});
```

---

### 4.3 Create Product Missing Name (VALIDATION FAILURE)

| Property | Value |
|----------|-------|
| **Test Name** | Create Product - Missing Name |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "price": 299.99,
    "stock": 10,
    "category": "Electronics"
}
```

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Missing name validation error", function() {
    pm.response.to.have.status(400);
    var fieldErrors = pm.response.json().fieldErrors;
    var nameError = fieldErrors.find(e => e.field === "name");
    pm.expect(nameError).to.not.be.undefined;
});
```

---

### 4.4 Get All Products (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Get All Products |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

**Test Script:**
```javascript
pm.test("Get all products successful", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json()).to.be.an("array");
});
```

---

### 4.5 Get Product by ID (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Get Product by ID |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/products/{{productId}}` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

**Test Script:**
```javascript
pm.test("Get product by ID successful", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json().id).to.equal(pm.environment.get("productId"));
});
```

---

### 4.6 Update Stock (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Update Product Stock |
| **Method** | `PATCH` |
| **URL** | `{{baseUrl}}/products/{{productId}}/stock?quantity=10` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

**Test Script:**
```javascript
pm.test("Stock updated successfully", function() {
    pm.response.to.have.status(200);
});
```

---

## 5. Cart Service Tests

### 5.1 Create Cart (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Create Shopping Cart |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/cart` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "userId": {{userId}},
    "items": [
        {
            "productId": {{productId}},
            "quantity": 2,
            "category": "Electronics"
        }
    ]
}
```

**Expected Response:**
- **Status:** `200 OK` or `201 Created`

**Test Script:**
```javascript
pm.test("Cart created successfully", function() {
    pm.expect([200, 201]).to.include(pm.response.code);
});
```

---

### 5.2 Create Cart Invalid Quantity (VALIDATION FAILURE)

| Property | Value |
|----------|-------|
| **Test Name** | Create Cart - Zero Quantity |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/cart` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "userId": {{userId}},
    "items": [
        {
            "productId": {{productId}},
            "quantity": 0,
            "category": "Electronics"
        }
    ]
}
```

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Zero quantity validation error", function() {
    pm.response.to.have.status(400);
});
```

---

### 5.3 Get Cart by User (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Get User's Cart |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/cart/{{userId}}` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

**Test Script:**
```javascript
pm.test("Get cart successful", function() {
    pm.response.to.have.status(200);
});
```

---

### 5.4 Add Item to Cart (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Add Item to Cart |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/cart/{{userId}}/items` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "productId": {{productId}},
    "quantity": 1,
    "category": "Electronics"
}
```

**Expected Response:**
- **Status:** `200 OK`

---

### 5.5 Clear Cart (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Clear User's Cart |
| **Method** | `DELETE` |
| **URL** | `{{baseUrl}}/cart/{{userId}}` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `204 No Content` or `200 OK`

---

## 6. Order Service Tests

### 6.1 Create Order (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Create New Order |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/orders` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "userId": {{userId}},
    "totalAmount": 1999.99
}
```

**Expected Response:**
- **Status:** `201 Created`

**Post-response Script:**
```javascript
pm.test("Order created successfully", function() {
    pm.response.to.have.status(201);
    pm.environment.set("orderId", pm.response.json().id);
});
```

---

### 6.2 Create Order Negative Amount (VALIDATION FAILURE)

| Property | Value |
|----------|-------|
| **Test Name** | Create Order - Negative Amount |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/orders` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "userId": {{userId}},
    "totalAmount": -100.00
}
```

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Negative order amount rejected", function() {
    pm.response.to.have.status(400);
    pm.expect(pm.response.json().error).to.equal("Validation Failed");
});
```

---

### 6.3 Get Order by ID (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Get Order by ID |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/orders/{{orderId}}` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

---

### 6.4 Get User's Orders (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Get User's Orders |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/orders/user/{{userId}}` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

---

## 7. Wallet Service Tests

### 7.1 Create Wallet (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Create User Wallet |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/wallets` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "userId": {{userId}},
    "amount": 5000.00
}
```

**Expected Response:**
- **Status:** `201 Created`

**Post-response Script:**
```javascript
pm.test("Wallet created successfully", function() {
    pm.response.to.have.status(201);
    pm.environment.set("walletId", pm.response.json().id);
    pm.expect(pm.response.json().balance).to.equal(5000.0);
});
```

---

### 7.2 Create Wallet Negative Amount (VALIDATION FAILURE)

| Property | Value |
|----------|-------|
| **Test Name** | Create Wallet - Negative Amount |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/wallets` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "userId": 999,
    "amount": -500.00
}
```

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Negative wallet amount rejected", function() {
    pm.response.to.have.status(400);
    pm.expect(pm.response.json().error).to.equal("Validation Failed");
    var amountError = pm.response.json().fieldErrors.find(e => e.field === "amount");
    pm.expect(amountError.message).to.equal("Amount cannot be negative");
});
```

---

### 7.3 Get Wallet by User (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Get User's Wallet |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/wallets/user/{{userId}}` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

**Test Script:**
```javascript
pm.test("Get wallet successful", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json().userId).to.equal(pm.environment.get("userId"));
});
```

---

### 7.4 Add Funds to Wallet (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Add Funds to Wallet |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/wallets/user/{{userId}}/add?amount=500.00` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

**Test Script:**
```javascript
pm.test("Funds added successfully", function() {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json().balance).to.be.above(5000);
});
```

---

### 7.5 Deduct Funds Insufficient Balance (FAILURE EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Deduct More Than Balance |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/wallets/user/{{userId}}/deduct?amount=999999.00` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Insufficient balance error", function() {
    pm.response.to.have.status(400);
    pm.expect(pm.response.json().error).to.equal("Insufficient Balance");
});
```

---

### 7.6 Deduct Funds Success (AUTHENTICATED)

| Property | Value |
|----------|-------|
| **Test Name** | Deduct Funds - Valid Amount |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/wallets/user/{{userId}}/deduct?amount=100.00` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `200 OK`

**Test Script:**
```javascript
pm.test("Funds deducted successfully", function() {
    pm.response.to.have.status(200);
});
```

---

### 7.7 Get Non-existent Wallet (404 EXPECTED)

| Property | Value |
|----------|-------|
| **Test Name** | Get Non-existent Wallet |
| **Method** | `GET` |
| **URL** | `{{baseUrl}}/wallets/user/9999` |
| **Headers** | `Authorization: Bearer {{jwt}}` |
| **Auth Required** | ✅ Yes |

**Expected Response:**
- **Status:** `404 Not Found`

**Test Script:**
```javascript
pm.test("Non-existent wallet returns 404", function() {
    pm.response.to.have.status(404);
});
```

---

## 8. Security Attack Tests

### 8.1 SQL Injection in Login Email

| Property | Value |
|----------|-------|
| **Test Name** | SQL Injection - Login Email |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/login` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "email": "' OR '1'='1' --",
    "password": "anything"
}
```

**Expected Response:**
- **Status:** `400 Bad Request` (invalid email format) or `401 Unauthorized`
- Should NOT return all users or bypass authentication

**Test Script:**
```javascript
pm.test("SQL injection attempt blocked", function() {
    pm.expect([400, 401]).to.include(pm.response.code);
    // Should not contain user data
    pm.expect(pm.response.json()).to.not.have.property("token");
});
```

---

### 8.2 SQL Injection in Login Password

| Property | Value |
|----------|-------|
| **Test Name** | SQL Injection - Login Password |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/login` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "email": "admin@eshoppingzone.com",
    "password": "'; DROP TABLE users; --"
}
```

**Expected Response:**
- **Status:** `401 Unauthorized`
- Database should remain intact

**Test Script:**
```javascript
pm.test("SQL injection in password blocked", function() {
    pm.response.to.have.status(401);
});
```

---

### 8.3 XSS Attack in User Registration

| Property | Value |
|----------|-------|
| **Test Name** | XSS in User Full Name |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "fullName": "<script>alert('XSS')</script>",
    "email": "xss-test@example.com",
    "password": "SecurePassword123!"
}
```

**Expected Response:**
- **Status:** `201 Created`
- XSS payload is stored as literal string (escaped at render time)

**Test Script:**
```javascript
pm.test("XSS payload stored safely", function() {
    if (pm.response.code === 201) {
        // Payload stored as literal (will be escaped on frontend)
        pm.expect(pm.response.json().fullName).to.include("<script>");
    }
});
```

---

### 8.4 Oversized Payload Attack

| Property | Value |
|----------|-------|
| **Test Name** | Oversized Password Payload |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/profiles/login` |
| **Headers** | `Content-Type: application/json` |
| **Auth Required** | ❌ No |

**Request Body:**
```json
{
    "email": "admin@eshoppingzone.com",
    "password": "AAAA...AAAA" 
}
```
(Password field contains 100,000 'A' characters)

**Expected Response:**
- **Status:** `400 Bad Request` or `401 Unauthorized` or `413 Payload Too Large`
- Server should NOT crash

**Test Script:**
```javascript
pm.test("Oversized payload handled gracefully", function() {
    pm.expect([400, 401, 413]).to.include(pm.response.code);
});
```

---

### 8.5 Negative Integer Overflow Test

| Property | Value |
|----------|-------|
| **Test Name** | Integer Overflow in Wallet |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/wallets` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "userId": -9999999999999999999,
    "amount": 100.00
}
```

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Integer overflow handled", function() {
    pm.response.to.have.status(400);
});
```

---

### 8.6 Boundary Test - Zero Stock

| Property | Value |
|----------|-------|
| **Test Name** | Product with Zero Stock |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "name": "Out of Stock Item",
    "price": 99.99,
    "stock": 0,
    "category": "Electronics"
}
```

**Expected Response:**
- **Status:** `201 Created`

**Test Script:**
```javascript
pm.test("Zero stock allowed", function() {
    pm.response.to.have.status(201);
    pm.expect(pm.response.json().stock).to.equal(0);
});
```

---

### 8.7 Boundary Test - Negative Stock

| Property | Value |
|----------|-------|
| **Test Name** | Product with Negative Stock |
| **Method** | `POST` |
| **URL** | `{{baseUrl}}/products` |
| **Headers** | `Authorization: Bearer {{jwt}}`, `Content-Type: application/json` |
| **Auth Required** | ✅ Yes |

**Request Body:**
```json
{
    "name": "Invalid Stock Item",
    "price": 99.99,
    "stock": -10,
    "category": "Electronics"
}
```

**Expected Response:**
- **Status:** `400 Bad Request`

**Test Script:**
```javascript
pm.test("Negative stock rejected", function() {
    pm.response.to.have.status(400);
});
```

---

## 9. End-to-End Workflow Tests

### 9.1 Complete Shopping Workflow

Execute these tests in sequence to simulate a real user journey:

| Step | Test Name | Expected Status |
|------|-----------|-----------------|
| 1 | Register New User | `201` |
| 2 | Login | `200` + JWT |
| 3 | Create Wallet with $1000 | `201` |
| 4 | Create Product | `201` |
| 5 | Add to Cart | `200` |
| 6 | Create Order ($500) | `201` |
| 7 | Check Wallet (Balance reduced via RabbitMQ) | `200` |
| 8 | Get Order Details | `200` |

---

## Test Summary Matrix

| Category | Tests | Expected Pass | Expected Fail |
|----------|-------|---------------|---------------|
| Authentication | 9 | 5 | 4 |
| Security Bypass | 8 | 0 | 8 |
| Profile Service | 4 | 3 | 1 |
| Product Service | 6 | 4 | 2 |
| Cart Service | 5 | 4 | 1 |
| Order Service | 4 | 3 | 1 |
| Wallet Service | 7 | 5 | 2 |
| Security Attacks | 7 | 2 | 5 |
| **Total** | **50** | **26** | **24** |

---

## Prerequisites for Running Tests

1. **All 8 microservices running:**
   - discovery-service (:8761)
   - gateway-service (:8080)
   - admin-service (:9090)
   - profile-service (:8081)
   - product-service (:8082)
   - cart-service (:8083)
   - order-service (:8084)
   - wallet-service (:8085)

2. **MySQL running** on localhost:3306

3. **RabbitMQ running** on localhost:5672

4. **Start order:** discovery → admin → gateway → remaining services

---

## Postman Collection Import

To use this test suite:

1. Create a new Postman Collection named "CartManagementSystem Security Tests"
2. Import each test as a new request
3. Set up the environment variables as described
4. Run collections in the order: Authentication → Security Bypass → Services → Attacks
5. Monitor the Test Results tab for pass/fail status

---

*Document Version: Phase 1.5 Security Testing Suite*
*Last Updated: March 2026*
