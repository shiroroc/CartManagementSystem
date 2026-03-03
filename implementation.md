# CartManagementSystem - Infrastructure Implementation

## Overview

This document describes the infrastructure layer of the CartManagementSystem microservices architecture. The infrastructure consists of three foundational services that provide service discovery, API routing, and monitoring capabilities.

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         CARTMANAGEMENTSYSTEM INFRASTRUCTURE                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                              EXTERNAL CLIENTS                                    │
│                                    │                                             │
│                                    ▼                                             │
│                         ┌────────────────────┐                                   │
│                         │    API GATEWAY     │                                   │
│                         │  gateway-service   │                                   │
│                         │     PORT: 8080     │                                   │
│                         └─────────┬──────────┘                                   │
│                                   │                                              │
│              ┌────────────────────┼────────────────────┐                         │
│              │                    │                    │                         │
│              ▼                    ▼                    ▼                         │
│   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                  │
│   │  EUREKA SERVER  │  │  SPRING BOOT    │  │  CORE SERVICES  │                  │
│   │discovery-service│  │  ADMIN SERVER   │  │    (Future)     │                  │
│   │   PORT: 8761    │  │  admin-service  │  │                 │                  │
│   │                 │  │   PORT: 9090    │  │ • Profile Svc   │                  │
│   │  ┌───────────┐  │  │                 │  │ • Product Svc   │                  │
│   │  │ Service   │  │  │  ┌───────────┐  │  │ • Cart Svc      │                  │
│   │  │ Registry  │◄─┼──┼──│ Health    │  │  │ • Order Svc     │                  │
│   │  └───────────┘  │  │  │ Metrics   │  │  │ • Wallet Svc    │                  │
│   │                 │  │  └───────────┘  │  │                 │                  │
│   └─────────────────┘  └─────────────────┘  └─────────────────┘                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Service Communication Flow

```
                                    ┌─────────────────┐
                                    │   HTTP Client   │
                                    │  (Browser/App)  │
                                    └────────┬────────┘
                                             │
                                             │ HTTP Request
                                             │ GET /products/123
                                             ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              GATEWAY-SERVICE (:8080)                             │
│                                                                                  │
│   ┌────────────┐    ┌─────────────────┐    ┌────────────────────────────────┐   │
│   │ Receive    │───►│ Match Route     │───►│ Load Balance via Eureka        │   │
│   │ Request    │    │ /products/**    │    │ lb://PRODUCT-SERVICE           │   │
│   └────────────┘    └─────────────────┘    └───────────────┬────────────────┘   │
│                                                            │                     │
└────────────────────────────────────────────────────────────┼─────────────────────┘
                                                             │
                          ┌──────────────────────────────────┘
                          │  Query Service Registry
                          ▼
              ┌───────────────────────┐
              │ DISCOVERY-SERVICE     │
              │      (:8761)          │
              │                       │
              │  Service Instances:   │
              │  ┌─────────────────┐  │
              │  │ PRODUCT-SERVICE │  │
              │  │ 192.168.1.10    │  │
              │  │ :8082           │  │
              │  └─────────────────┘  │
              └───────────────────────┘
                          │
                          │ Returns healthy instance
                          ▼
              ┌───────────────────────┐
              │   PRODUCT-SERVICE     │
              │  (Core - Future)      │
              │      :8082            │
              └───────────────────────┘
```

---

## Gateway Route Mapping Table

| Path Pattern    | Target Service         | Description                    |
|-----------------|------------------------|--------------------------------|
| `/profiles/**`  | `lb://PROFILE-SERVICE` | User profile management        |
| `/products/**`  | `lb://PRODUCT-SERVICE` | Product catalog operations     |
| `/cart/**`      | `lb://CART-SERVICE`    | Shopping cart management       |
| `/orders/**`    | `lb://ORDER-SERVICE`   | Order processing               |
| `/wallets/**`   | `lb://WALLET-SERVICE`  | Digital wallet operations      |

---

## Startup Sequence Diagram

```
TIME ──────────────────────────────────────────────────────────────────────────►

     ┌─────────────────┐
 1.  │ discovery-svc   │ START
     │    :8761        │━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━►
     └─────────────────┘
              │
              │ Eureka Ready
              ▼
     ┌─────────────────┐
 2.  │  admin-svc      │ START
     │    :9090        │━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━►
     └─────────────────┘
              │ Registers with Eureka
              │
              ▼
     ┌─────────────────┐
 3.  │ gateway-svc     │ START
     │    :8080        │━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━►
     └─────────────────┘
              │ Registers with Eureka
              │
              ▼
     ┌─────────────────┐
 4.  │ core-services   │ START (Future)
     │ profile/product │━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━►
     │ cart/order/     │
     │ wallet          │
     └─────────────────┘

INFRASTRUCTURE READY ────────────────────────────────────► SYSTEM OPERATIONAL
```

---

## Project Structure

```
CartManagementSystem/
│
├── discovery-service/                    # Eureka Server
│   ├── pom.xml                          # Maven config with eureka-server dependency
│   └── src/main/
│       ├── java/com/eshoppingzone/discovery/
│       │   └── DiscoveryServiceApplication.java    # @EnableEurekaServer
│       └── resources/
│           └── application.yml          # Port 8761, standalone mode
│
├── gateway-service/                      # API Gateway
│   ├── pom.xml                          # Maven config with gateway + eureka-client
│   └── src/main/
│       ├── java/com/eshoppingzone/gateway/
│       │   └── GatewayServiceApplication.java      # @SpringBootApplication
│       └── resources/
│           └── application.yml          # Port 8080, route definitions
│
├── admin-service/                        # Spring Boot Admin
│   ├── pom.xml                          # Maven config with admin-server + eureka-client
│   └── src/main/
│       ├── java/com/eshoppingzone/admin/
│       │   └── AdminServiceApplication.java        # @EnableAdminServer
│       └── resources/
│           └── application.yml          # Port 9090, registers with Eureka
│
└── [Future Core Services]
    ├── profile-service/
    ├── product-service/
    ├── cart-service/
    ├── order-service/
    └── wallet-service/
```

---

## Technology Stack

| Component            | Technology                    | Version   |
|----------------------|-------------------------------|-----------|
| Framework            | Spring Boot                   | 3.2.3     |
| Cloud Dependencies   | Spring Cloud                  | 2023.0.0  |
| Service Discovery    | Netflix Eureka                | (managed) |
| API Gateway          | Spring Cloud Gateway          | (managed) |
| Admin Dashboard      | Spring Boot Admin             | 3.2.2     |
| Build Tool           | Apache Maven                  | 3.8+      |
| Java Version         | OpenJDK                       | 17        |

---

## Service Port Allocation

| Service           | Port | Purpose                              |
|-------------------|------|--------------------------------------|
| discovery-service | 8761 | Eureka Server Dashboard & Registry   |
| gateway-service   | 8080 | Public API Entry Point               |
| admin-service     | 9090 | Spring Boot Admin Dashboard          |
| profile-service   | 8081 | User profile management (Future)     |
| product-service   | 8082 | Product catalog (Future)             |
| cart-service      | 8083 | Shopping cart (Future)               |
| order-service     | 8084 | Order processing (Future)            |
| wallet-service    | 8085 | Digital wallet (Future)              |

---

## Running the Infrastructure

### Prerequisites
- Java 17 installed
- Maven 3.8+ installed

### Start Services (in order)

```bash
# 1. Start Eureka Server
cd discovery-service
mvn spring-boot:run

# 2. Start Admin Server (new terminal)
cd admin-service
mvn spring-boot:run

# 3. Start API Gateway (new terminal)
cd gateway-service
mvn spring-boot:run
```

### Verification URLs

| Service           | URL                           | Expected Result                     |
|-------------------|-------------------------------|-------------------------------------|
| Eureka Dashboard  | http://localhost:8761         | Eureka web UI with registered apps  |
| Admin Dashboard   | http://localhost:9090         | Spring Boot Admin UI                |
| Gateway Health    | http://localhost:8080/actuator/health | `{"status":"UP"}`           |

---

## Current State

These three infrastructure services are **static bootstrap services**. They exist solely to:

1. **discovery-service**: Provide service registration and discovery for all microservices
2. **gateway-service**: Route external requests to internal services via load balancing
3. **admin-service**: Provide centralized monitoring and management dashboard

They contain **no business logic** and require **no controllers, services, or repositories**.

---

## Core Services Implementation

### Profile Service (Port 8081)

Manages user profiles with CRUD operations.

**Database**: `profile_db` on MySQL (localhost:3306)

**Entity**: `User` (id, fullName, email, password)

**Endpoints**:
| Method | Path                    | Description           |
|--------|-------------------------|-----------------------|
| GET    | /profiles               | Get all users         |
| GET    | /profiles/{id}          | Get user by ID        |
| GET    | /profiles/email?email=  | Get user by email     |
| POST   | /profiles               | Create new user       |
| PUT    | /profiles/{id}          | Update user           |
| DELETE | /profiles/{id}          | Delete user           |

**Swagger UI**: http://localhost:8081/swagger-ui.html

---

### Product Service (Port 8082)

Manages product catalog with inventory tracking.

**Database**: `product_db` on MySQL (localhost:3306)

**Entity**: `Product` (id, name, price, stock, category)

**Endpoints**:
| Method | Path                           | Description                |
|--------|--------------------------------|----------------------------|
| GET    | /products                      | Get all products           |
| GET    | /products/{id}                 | Get product by ID          |
| GET    | /products/search?name=         | Search products by name    |
| GET    | /products/available            | Get in-stock products      |
| POST   | /products                      | Create new product         |
| PUT    | /products/{id}                 | Update product             |
| PATCH  | /products/{id}/stock?quantity= | Adjust stock (+/-)         |
| DELETE | /products/{id}                 | Delete product             |

**Swagger UI**: http://localhost:8082/swagger-ui.html

---

### Core Service Architecture

```
profile-service/                              product-service/
├── pom.xml                                   ├── pom.xml
└── src/main/                                 └── src/main/
    ├── java/com/eshoppingzone/profile/          ├── java/com/eshoppingzone/product/
    │   ├── ProfileServiceApplication.java       │   ├── ProductServiceApplication.java
    │   ├── controller/                          │   ├── controller/
    │   │   └── UserController.java              │   │   └── ProductController.java
    │   ├── dto/                                 │   ├── dto/
    │   │   ├── UserRequest.java                 │   │   ├── ProductRequest.java
    │   │   └── UserResponse.java                │   │   └── ProductResponse.java
    │   ├── entity/                              │   ├── entity/
    │   │   └── User.java                        │   │   └── Product.java
    │   ├── exception/                           │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java      │   │   ├── GlobalExceptionHandler.java
    │   │   ├── ResourceNotFoundException.java   │   │   ├── ResourceNotFoundException.java
    │   │   └── DuplicateResourceException.java  │   │   └── InsufficientStockException.java
    │   ├── repository/                          │   ├── repository/
    │   │   └── UserRepository.java              │   │   └── ProductRepository.java
    │   └── service/                             │   └── service/
    │       └── UserService.java                 │       └── ProductService.java
    └── resources/                               └── resources/
        └── application.yml                          └── application.yml
```

---

### Updated System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              CARTMANAGEMENTSYSTEM                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                              EXTERNAL CLIENTS                                    │
│                                    │                                             │
│                                    ▼                                             │
│                         ┌────────────────────┐                                   │
│                         │    API GATEWAY     │                                   │
│                         │     :8080          │                                   │
│                         └─────────┬──────────┘                                   │
│                                   │                                              │
│       ┌───────────────────────────┼───────────────────────────┐                  │
│       │                           │                           │                  │
│       ▼                           ▼                           ▼                  │
│ ┌───────────────┐     ┌───────────────────┐     ┌───────────────────┐            │
│ │PROFILE-SERVICE│     │  PRODUCT-SERVICE  │     │  FUTURE SERVICES  │            │
│ │    :8081      │     │      :8082        │     │                   │            │
│ │               │     │                   │     │  • Cart :8083     │            │
│ │  ┌─────────┐  │     │  ┌─────────────┐  │     │  • Order :8084    │            │
│ │  │ users   │  │     │  │  products   │  │     │  • Wallet :8085   │            │
│ │  └────┬────┘  │     │  └──────┬──────┘  │     │                   │            │
│ └───────┼───────┘     └─────────┼─────────┘     └───────────────────┘            │
│         │                       │                                                │
│         ▼                       ▼                                                │
│    ┌─────────┐            ┌───────────┐                                          │
│    │profile_db│           │product_db │                                          │
│    └─────────┘            └───────────┘                                          │
│                    MySQL localhost:3306                                          │
│                                                                                  │
│   ┌─────────────────┐         ┌─────────────────┐                                │
│   │  EUREKA SERVER  │◄────────│  ADMIN SERVER   │                                │
│   │     :8761       │         │     :9090       │                                │
│   └─────────────────┘         └─────────────────┘                                │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

### Running Core Services

**Prerequisites**:
- MySQL running on localhost:3306
- Eureka discovery-service running

```bash
# 4. Start Profile Service (new terminal)
cd profile-service
mvn spring-boot:run

# 5. Start Product Service (new terminal)
cd product-service
mvn spring-boot:run
```

**Verification via Gateway**:
```bash
# Create a user
curl -X POST http://localhost:8080/profiles \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Doe","email":"john@example.com","password":"securePass123"}'

# Create a product
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":999.99,"stock":50,"category":"Electronics"}'
```

---

## Next Steps (Remaining Core Services)

The following core business services will be implemented next:

1. **cart-service** - Shopping cart operations (Port 8083)
2. **order-service** - Order placement and tracking (Port 8084)
3. **wallet-service** - Payment and wallet management (Port 8085)

Each will follow the same lean layered architecture as profile-service and product-service.

---

## Phase 2.5: Entity Field Enhancements

### Changes from Phase 2

This phase enhances the core services created in Phase 2 with additional fields while maintaining the lean layered architecture.

---

### Profile Service - Password Field Addition

**Phase 2 Entity**: `User` (id, fullName, email)  
**Phase 2.5 Entity**: `User` (id, fullName, email, **password**)

| Layer        | File           | password Field | Reason                                      |
|--------------|----------------|----------------|---------------------------------------------|
| Entity       | User.java      | ✅ Added       | Stored in database for authentication       |
| DTO Request  | UserRequest    | ✅ Added       | Accepted from client during create/update   |
| DTO Response | UserResponse   | ❌ NOT Added   | **SECURITY**: Never expose password in API  |
| Service      | UserService    | ✅ Mapped      | Request→Entity only; never Entity→Response  |

**Security Flow Diagram**:
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  UserRequest    │────►│     User        │────►│  UserResponse   │
│                 │     │    (Entity)     │     │                 │
│ • fullName      │     │ • id            │     │ • id            │
│ • email         │     │ • fullName      │     │ • fullName      │
│ • password ─────┼────►│ • email         │────►│ • email         │
│                 │     │ • password ─────┼──X──│                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                              │
                              ▼
                        ┌───────────┐
                        │  MySQL    │
                        │ users     │
                        │ table     │
                        └───────────┘
```

---

### Product Service - Category Field Addition

**Phase 2 Entity**: `Product` (id, name, price, stock)  
**Phase 2.5 Entity**: `Product` (id, name, price, stock, **category**)

| Layer        | File            | category Field | Reason                                    |
|--------------|-----------------|----------------|-------------------------------------------|
| Entity       | Product.java    | ✅ Added       | Stored in database for categorization     |
| DTO Request  | ProductRequest  | ✅ Added       | Accepted from client during create/update |
| DTO Response | ProductResponse | ✅ Added       | Returned to client (public information)   |
| Service      | ProductService  | ✅ Mapped      | Full bidirectional mapping                |

**Data Flow Diagram**:
```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ ProductRequest  │────►│    Product      │────►│ ProductResponse │
│                 │     │    (Entity)     │     │                 │
│ • name          │────►│ • id            │────►│ • id            │
│ • price         │────►│ • name          │────►│ • name          │
│ • stock         │────►│ • price         │────►│ • price         │
│ • category ─────┼────►│ • stock         │────►│ • stock         │
│                 │     │ • category ─────┼────►│ • category      │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

### Key Differences: Phase 2 vs Phase 2.5

| Aspect                    | Phase 2                          | Phase 2.5                                      |
|---------------------------|----------------------------------|------------------------------------------------|
| User fields               | id, fullName, email              | id, fullName, email, **password**              |
| Product fields            | id, name, price, stock           | id, name, price, stock, **category**           |
| Security consideration    | None                             | Password excluded from UserResponse            |
| DTO mapping strategy      | Symmetric (Request↔Response)     | Asymmetric for User (security), Symmetric for Product |

---

### Updated API Payloads

**Create User (POST /profiles)**:
```json
{
  "fullName": "John Doe",
  "email": "john@example.com",
  "password": "securePassword123"
}
```

**User Response** (password NOT included):
```json
{
  "id": 1,
  "fullName": "John Doe",
  "email": "john@example.com"
}
```

**Create Product (POST /products)**:
```json
{
  "name": "Laptop",
  "price": 999.99,
  "stock": 50,
  "category": "Electronics"
}
```

**Product Response** (category included):
```json
{
  "id": 1,
  "name": "Laptop",
  "price": 999.99,
  "stock": 50,
  "category": "Electronics"
}
```

---

### Files Modified in Phase 2.5

| Service         | File                | Change                          |
|-----------------|---------------------|---------------------------------|
| profile-service | User.java           | +password field                 |
| profile-service | UserRequest.java    | +password field                 |
| profile-service | UserService.java    | +password mapping (Request→Entity only) |
| product-service | Product.java        | +category field                 |
| product-service | ProductRequest.java | +category field                 |
| product-service | ProductResponse.java| +category field                 |
| product-service | ProductService.java | +category mapping (bidirectional) |

---

## Phase 3: Cart & Order Services

### Phase 3.1: Cart Service (Port 8083)

Shopping cart management service.

**Database**: `cart_db` on MySQL (localhost:3306)

#### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              CART-SERVICE (:8083)                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌────────────────────┐                                                         │
│   │   CartController   │◄──────────── REST API (/cart/**)                        │
│   └─────────┬──────────┘                                                         │
│             │                                                                    │
│             ▼                                                                    │
│   ┌────────────────────┐                                                         │
│   │    CartService     │                                                         │
│   │                    │                                                         │
│   │  - CRUD operations │                                                         │
│   └─────────┬──────────┘                                                         │
│             │                                                                    │
│             ▼                                                                    │
│   ┌────────────────────┐                                                         │
│   │   CartRepository   │                                                         │
│   └─────────┬──────────┘                                                         │
│             │                                                                    │
│             ▼                                                                    │
│   ┌────────────────────┐                                                         │
│   │      MySQL         │                                                         │
│   │     (cart_db)      │                                                         │
│   └────────────────────┘                                                         │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Entities

**Cart**:
| Field      | Type               | Description                    |
|------------|--------------------|--------------------------------|
| id         | Long               | Primary key (auto-generated)   |
| userId     | Long               | User reference (unique)        |
| cartItems  | List\<CartItem\>   | OneToMany relationship         |

**CartItem**:
| Field      | Type   | Description                    |
|------------|--------|--------------------------------|
| id         | Long   | Primary key (auto-generated)   |
| productId  | Long   | Product reference              |
| quantity   | Integer| Item quantity                  |
| category   | String | Product category               |
| cart       | Cart   | ManyToOne back-reference       |

#### DTOs

| DTO         | Fields                            | Purpose                      |
|-------------|-----------------------------------|------------------------------|
| CartItemDto | productId, quantity, category     | Item representation          |
| CartRequest | userId, List\<CartItemDto\> items | Create/update cart           |
| CartResponse| id, userId, List\<CartItemDto\> items | Output with cart ID      |

#### API Endpoints

| Method | Path                           | Description                              |
|--------|--------------------------------|------------------------------------------|
| GET    | /cart/{userId}                 | Get user's cart                          |
| POST   | /cart                          | Create or update cart                    |
| POST   | /cart/{userId}/items           | Add item to cart                         |
| DELETE | /cart/{userId}/items/{productId}| Remove item from cart                   |
| DELETE | /cart/{userId}                 | Clear cart                               |

### Phase 3.2: Order Service (Port 8084)

Order processing service with **simulated Stripe payments** and **RabbitMQ event publishing**.

**Database**: `order_db` on MySQL (localhost:3306)

#### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              ORDER-SERVICE (:8084)                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌─────────────────┐                    ┌────────────────────┐                  │
│   │ OrderController │◄── REST API ───────│   External Client  │                  │
│   └────────┬────────┘    (/orders/**)    └────────────────────┘                  │
│            │                                                                     │
│            ▼                                                                     │
│   ┌─────────────────┐                                                            │
│   │  OrderService   │                                                            │
│   │                 │                                                            │
│   │ ┌─────────────┐ │    ┌────────────────┐    ┌──────────────────┐              │
│   │ │ 1. Create   │─┼───►│ OrderRepository │───►│      MySQL       │              │
│   │ │    Order    │ │    │                 │    │    (order_db)    │              │
│   │ └──────┬──────┘ │    └─────────────────┘    └──────────────────┘              │
│   │        │        │                                                            │
│   │        ▼        │                                                            │
│   │ ┌─────────────┐ │    ┌────────────────┐                                       │
│   │ │ 2. Process  │─┼───►│ Stripe API     │  (Simulated)                          │
│   │ │    Payment  │ │    │ (RestTemplate) │                                       │
│   │ └──────┬──────┘ │    └────────────────┘                                       │
│   │        │        │                                                            │
│   │        ▼        │                                                            │
│   │ ┌─────────────┐ │    ┌────────────────┐    ┌──────────────────┐              │
│   │ │ 3. Publish  │─┼───►│ RabbitTemplate │───►│    RabbitMQ      │              │
│   │ │    Event    │ │    │                 │    │ order.exchange   │              │
│   │ └─────────────┘ │    └─────────────────┘    │ order.placed     │              │
│   │                 │                           └──────────────────┘              │
│   └─────────────────┘                                                            │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Entity: Order

| Field       | Type          | Description                    |
|-------------|---------------|--------------------------------|
| id          | Long          | Primary key (auto-generated)   |
| userId      | Long          | Customer reference             |
| totalAmount | BigDecimal    | Order total                    |
| status      | String        | PENDING / CONFIRMED / FAILED   |
| createdAt   | LocalDateTime | Creation timestamp             |
| updatedAt   | LocalDateTime | Last update timestamp          |

#### DTOs

| DTO              | Fields                              | Purpose                        |
|------------------|-------------------------------------|--------------------------------|
| OrderRequest     | userId, totalAmount                 | Create order input             |
| OrderResponse    | id, userId, totalAmount, status, createdAt | Order output           |
| OrderPlacedEvent | orderId, userId, totalAmount        | RabbitMQ event payload         |

#### API Endpoints

| Method | Path              | Description                              |
|--------|-------------------|------------------------------------------|
| POST   | /orders           | Create order (payment + event)           |
| GET    | /orders/{id}      | Get order by ID                          |
| GET    | /orders/user/{userId} | Get orders by user                   |
| GET    | /orders           | Get all orders                           |

#### Order Processing Flow

```
┌──────────┐    ┌───────────────┐    ┌────────────────┐    ┌─────────────┐
│  Client  │───►│ POST /orders  │───►│ OrderService   │───►│   MySQL     │
│          │    │               │    │                │    │ (PENDING)   │
└──────────┘    └───────────────┘    └───────┬────────┘    └─────────────┘
                                              │
                                              ▼
                                    ┌─────────────────┐
                                    │ processPayment()│
                                    │                 │
                                    │ Simulated       │
                                    │ Stripe API Call │
                                    └────────┬────────┘
                                              │
                         ┌────────────────────┴────────────────────┐
                         │                                         │
                         ▼                                         ▼
              ┌─────────────────┐                       ┌─────────────────┐
              │   SUCCESS       │                       │   FAILURE       │
              │                 │                       │                 │
              │ Update status:  │                       │ Update status:  │
              │ CONFIRMED       │                       │ FAILED          │
              └────────┬────────┘                       └────────┬────────┘
                       │                                         │
                       ▼                                         ▼
              ┌─────────────────┐                       ┌─────────────────┐
              │ Publish Event   │                       │ Throw           │
              │ to RabbitMQ     │                       │ PaymentFailed   │
              │                 │                       │ Exception       │
              │ Exchange:       │                       └─────────────────┘
              │ order.exchange  │
              │ Key: order.placed│
              └─────────────────┘
```

#### RabbitMQ Configuration

| Component    | Value                |
|--------------|----------------------|
| Queue        | order.placed.queue   |
| Exchange     | order.exchange (Topic)|
| Routing Key  | order.placed         |

```java
// RabbitMQConfig.java
@Bean
public Queue orderPlacedQueue() {
    return new Queue("order.placed.queue", true);
}

@Bean
public TopicExchange orderExchange() {
    return new TopicExchange("order.exchange");
}

@Bean
public Binding orderBinding(Queue orderPlacedQueue, TopicExchange orderExchange) {
    return BindingBuilder.bind(orderPlacedQueue).to(orderExchange).with("order.placed");
}
```

#### Configuration (application.yml)

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

# Stripe API (simulated)
stripe:
  api:
    url: https://api.stripe.com/v1/charges
    key: ${STRIPE_API_KEY:sk_test_placeholder}
```

#### Dependencies (pom.xml)

```xml
<!-- RabbitMQ AMQP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**Swagger UI**: http://localhost:8084/swagger-ui.html

---

### Phase 3 Verification

#### Start Services

```bash
# Prerequisites: MySQL running, RabbitMQ running (for order-service)

# 1. Start Cart Service
cd cart-service
mvn spring-boot:run

# 2. Start Order Service (new terminal)
cd order-service
mvn spring-boot:run
```

#### Test Cart Service

```bash
# Create cart with items
curl -X POST http://localhost:8083/cart \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"items":[{"productId":100,"quantity":2,"category":"Electronics"},{"productId":200,"quantity":1,"category":"Accessories"}]}'

# Get cart
curl http://localhost:8083/cart/1

# Add item to cart
curl -X POST http://localhost:8083/cart/1/items \
  -H "Content-Type: application/json" \
  -d '{"productId":300,"quantity":3,"category":"Books"}'

# Remove item
curl -X DELETE http://localhost:8083/cart/1/items/100

# Clear cart
curl -X DELETE http://localhost:8083/cart/1
```

#### Test Order Service

```bash
# Create order (triggers payment + RabbitMQ event)
curl -X POST http://localhost:8084/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"totalAmount":149.99}'

# Get order
curl http://localhost:8084/orders/1

# Get user's orders
curl http://localhost:8084/orders/user/1

# Get all orders
curl http://localhost:8084/orders
```

---

### Phase 3.3: Wallet Service (Port 8085)

Digital wallet service with **RabbitMQ event consumer** for automatic balance deduction when orders are placed.

**Database**: `wallet_db` on MySQL (localhost:3306)

#### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                             WALLET-SERVICE (:8085)                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│   ┌──────────────────┐                                                           │
│   │ WalletController │◄── REST API (/wallets/**)                                 │
│   └────────┬─────────┘                                                           │
│            │                                                                     │
│            ▼                                                                     │
│   ┌──────────────────┐         ┌────────────────────────────────────────────┐    │
│   │  WalletService   │◄────────│          RabbitMQ Consumer                 │    │
│   │                  │         │  @RabbitListener(order.placed.queue)       │    │
│   │ - createWallet() │         └────────────────────────────────────────────┘    │
│   │ - addFunds()     │                           ▲                               │
│   │ - deductFunds()  │                           │                               │
│   │ - handleOrder    │                           │ OrderPlacedEvent              │
│   │   PlacedEvent()  │                           │ (userId, totalAmount)         │
│   └────────┬─────────┘                           │                               │
│            │                           ┌─────────┴──────────┐                    │
│            ▼                           │     RabbitMQ       │                    │
│   ┌──────────────────┐                 │ order.placed.queue │                    │
│   │ WalletRepository │                 └────────────────────┘                    │
│   └────────┬─────────┘                           ▲                               │
│            │                                     │                               │
│            ▼                                     │ Published by                  │
│   ┌──────────────────┐                           │ order-service                 │
│   │      MySQL       │                           │                               │
│   │   (wallet_db)    │                 ┌─────────┴──────────┐                    │
│   └──────────────────┘                 │   ORDER-SERVICE    │                    │
│                                        │      (:8084)       │                    │
└────────────────────────────────────────┴────────────────────┴────────────────────┘
```

#### Entity: Wallet

| Field     | Type          | Description                      |
|-----------|---------------|----------------------------------|
| id        | Long          | Primary key (auto-generated)     |
| userId    | Long          | User reference (unique)          |
| balance   | BigDecimal    | Current wallet balance           |
| createdAt | LocalDateTime | Creation timestamp               |
| updatedAt | LocalDateTime | Last update timestamp            |

#### DTOs

| DTO              | Fields                                    | Purpose                        |
|------------------|-------------------------------------------|--------------------------------|
| WalletRequest    | userId, amount                            | Create wallet / add funds      |
| WalletResponse   | id, userId, balance, createdAt, updatedAt | Wallet output                  |
| OrderPlacedEvent | orderId, userId, totalAmount              | RabbitMQ event (from order-service) |

#### API Endpoints

| Method | Path                        | Description                    |
|--------|-----------------------------|--------------------------------|
| POST   | /wallets                    | Create wallet for user         |
| GET    | /wallets/{id}               | Get wallet by ID               |
| GET    | /wallets/user/{userId}      | Get wallet by user ID          |
| GET    | /wallets                    | Get all wallets                |
| POST   | /wallets/user/{userId}/add?amount= | Add funds to wallet      |
| POST   | /wallets/user/{userId}/deduct?amount= | Deduct funds from wallet |

#### RabbitMQ Consumer Flow

```
┌──────────────────┐     ┌─────────────────┐     ┌──────────────────┐
│  ORDER-SERVICE   │     │    RabbitMQ     │     │  WALLET-SERVICE  │
│    (:8084)       │     │                 │     │     (:8085)      │
└────────┬─────────┘     └────────┬────────┘     └────────┬─────────┘
         │                        │                       │
         │ 1. Order confirmed     │                       │
         │    Publish event       │                       │
         │ ──────────────────────►│                       │
         │                        │                       │
         │                        │ 2. Deliver to         │
         │                        │    @RabbitListener    │
         │                        │ ─────────────────────►│
         │                        │                       │
         │                        │                   ┌───┴───┐
         │                        │                   │ 3. Query│
         │                        │                   │ Wallet │
         │                        │                   │ by     │
         │                        │                   │ userId │
         │                        │                   └───┬───┘
         │                        │                       │
         │                        │                   ┌───┴───┐
         │                        │                   │ 4.    │
         │                        │                   │ Deduct│
         │                        │                   │ amount│
         │                        │                   └───┬───┘
         │                        │                       │
         │                        │                   ┌───┴───┐
         │                        │                   │ 5.    │
         │                        │                   │ Save  │
         │                        │                   │ to DB │
         │                        │                   └───┬───┘
         │                        │                       │
         │                        │                   ┌───┴───┐
         │                        │                   │ 6.    │
         │                        │                   │ Log   │
         │                        │                   │ SLF4J │
         │                        │                   └───────┘
```

#### RabbitMQ Configuration

| Component | Value                     |
|-----------|---------------------------|
| Queue     | order.placed.queue        |
| Consumer  | @RabbitListener annotation|

```java
// WalletService.java - RabbitMQ Consumer
@RabbitListener(queues = RabbitMQConfig.ORDER_PLACED_QUEUE)
@Transactional
public void handleOrderPlacedEvent(OrderPlacedEvent event) {
    logger.info("========== RABBITMQ EVENT RECEIVED ==========");
    logger.info("Received OrderPlacedEvent: {}", event);
    
    Wallet wallet = walletRepository.findByUserId(event.getUserId())
            .orElse(/* create new wallet if not exists */);
    
    wallet.setBalance(wallet.getBalance().subtract(event.getTotalAmount()));
    walletRepository.save(wallet);
    
    logger.info("WALLET DEDUCTION COMPLETE:");
    logger.info("  Order ID: {}", event.getOrderId());
    logger.info("  User ID: {}", event.getUserId());
    logger.info("  Deducted Amount: {}", event.getTotalAmount());
    logger.info("==============================================");
}
```

#### Configuration (application.yml)

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

#### Dependencies (pom.xml)

```xml
<!-- RabbitMQ AMQP -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**Swagger UI**: http://localhost:8085/swagger-ui.html

---

### Test Wallet Service

```bash
# Create wallet with initial balance
curl -X POST http://localhost:8085/wallets \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"amount":500.00}'

# Get wallet by user ID
curl http://localhost:8085/wallets/user/1

# Add funds
curl -X POST "http://localhost:8085/wallets/user/1/add?amount=100.00"

# Deduct funds manually
curl -X POST "http://localhost:8085/wallets/user/1/deduct?amount=50.00"

# Get all wallets
curl http://localhost:8085/wallets
```

#### Test RabbitMQ Integration (Order → Wallet)

```bash
# 1. Create wallet for user
curl -X POST http://localhost:8085/wallets \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"amount":500.00}'

# 2. Check initial balance
curl http://localhost:8085/wallets/user/1
# Expected: balance = 500.00

# 3. Create order (this publishes OrderPlacedEvent to RabbitMQ)
curl -X POST http://localhost:8084/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"totalAmount":149.99}'

# 4. Check wallet balance after async deduction
curl http://localhost:8085/wallets/user/1
# Expected: balance = 350.01 (500.00 - 149.99)

# 5. Check wallet-service logs for SLF4J output:
# ========== RABBITMQ EVENT RECEIVED ==========
# Received OrderPlacedEvent: OrderPlacedEvent{orderId=1, userId=1, totalAmount=149.99}
# WALLET DEDUCTION COMPLETE:
#   Order ID: 1
#   User ID: 1
#   Previous Balance: 500.00
#   Deducted Amount: 149.99
#   New Balance: 350.01
# ==============================================
```

---

### Final Port Allocation

| Service           | Port | Status      | Description                          |
|-------------------|------|-------------|--------------------------------------|
| gateway-service   | 8080 | ✅ Complete | Public API Entry Point               |
| profile-service   | 8081 | ✅ Complete | User profile management              |
| product-service   | 8082 | ✅ Complete | Product catalog                      |
| cart-service      | 8083 | ✅ Complete | Shopping cart                          |
| order-service     | 8084 | ✅ Complete | Orders + payment + RabbitMQ events   |
| **wallet-service**| 8085 | ✅ Complete | Wallet + RabbitMQ consumer           |
| discovery-service | 8761 | ✅ Complete | Eureka Server                        |
| admin-service     | 9090 | ✅ Complete | Spring Boot Admin Dashboard          |

---

### Complete System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        CARTMANAGEMENTSYSTEM - COMPLETE ARCHITECTURE                  │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│                              EXTERNAL CLIENTS                                        │
│                                    │                                                 │
│                                    ▼                                                 │
│                         ┌────────────────────┐                                       │
│                         │    API GATEWAY     │                                       │
│                         │  gateway-service   │                                       │
│                         │     PORT: 8080     │                                       │
│                         └─────────┬──────────┘                                       │
│                                   │                                                  │
│     ┌─────────────┬───────────────┼───────────────┬─────────────┬─────────────┐      │
│     │             │               │               │             │             │      │
│     ▼             ▼               ▼               ▼             ▼             ▼      │
│ ┌─────────┐ ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐  │
│ │PROFILE  │ │PRODUCT  │    │  CART   │    │  ORDER  │    │ WALLET  │    │ EUREKA  │  │
│ │SERVICE  │ │SERVICE  │    │ SERVICE │    │ SERVICE │    │ SERVICE │    │ SERVER  │  │
│ │  :8081  │ │  :8082  │    │  :8083  │    │  :8084  │    │  :8085  │    │  :8761  │  │
│ │         │ │         │    │         │    │         │    │         │    │         │  │
│ │ User    │ │ Product │    │  Cart   │    │ Orders  │    │ Wallet  │◄───│ Service │  │
│ │ CRUD    │ │ Catalog │    │ Service │    │ Payment │────│ Balance │    │Registry │  │
│ └────┬────┘ └────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘    └─────────┘  │
│      │           │              │              │              │                      │
│      │           │              │              │ RabbitMQ     │                      │
│      │           │              │              │ Event        │                      │
│      │           │              │              └──────────────┘                      │
│      │           │              │                                                    │
│      │           │              │                                                    │
│      │           │              │                                                    │
│      │           │              │                                                    │
│      │           │              │                                                    │
│      │           │              │                                                    │
│      │           │                                                                   │
│      ▼           ▼              ▼              ▼              ▼                      │
│ ┌─────────┐ ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐                 │
│ │ MySQL   │ │ MySQL   │    │ MySQL   │    │ MySQL   │    │ MySQL   │                 │
│ │profile_ │ │product_ │    │ cart_db │    │order_db │    │wallet_db│                 │
│ │   db    │ │   db    │    │         │    │         │    │         │                 │
│ └─────────┘ └─────────┘    └─────────┘    └─────────┘    └─────────┘                 │
│                                                                                      │
│                         ┌────────────────────┐                                       │
│                         │   SPRING BOOT      │                                       │
│                         │   ADMIN SERVER     │                                       │
│                         │     PORT: 9090     │                                       │
│                         └────────────────────┘                                       │
│                                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

### Files Created in Phase 3

#### Cart Service (Phase 3.1)

| Directory                                           | File                        |
|-----------------------------------------------------|-----------------------------|
| cart-service/                                       | pom.xml                     |
| cart-service/src/main/resources/                    | application.yml             |
| cart-service/src/main/java/.../cart/                | CartServiceApplication.java |
| cart-service/src/main/java/.../cart/controller/     | CartController.java         |
| cart-service/src/main/java/.../cart/dto/            | CartItemDto.java            |
| cart-service/src/main/java/.../cart/dto/            | CartRequest.java            |
| cart-service/src/main/java/.../cart/dto/            | CartResponse.java           |
| cart-service/src/main/java/.../cart/entity/         | Cart.java                   |
| cart-service/src/main/java/.../cart/entity/         | CartItem.java               |
| cart-service/src/main/java/.../cart/exception/      | GlobalExceptionHandler.java |
| cart-service/src/main/java/.../cart/exception/      | ResourceNotFoundException.java |
| cart-service/src/main/java/.../cart/repository/     | CartRepository.java         |
| cart-service/src/main/java/.../cart/service/        | CartService.java            |

#### Order Service (Phase 3.2)

| Directory                                           | File                        |
|-----------------------------------------------------|-----------------------------|
| order-service/                                      | pom.xml                     |
| order-service/src/main/resources/                   | application.yml             |
| order-service/src/main/java/.../order/              | OrderServiceApplication.java|
| order-service/src/main/java/.../order/controller/   | OrderController.java        |
| order-service/src/main/java/.../order/config/       | RabbitMQConfig.java         |
| order-service/src/main/java/.../order/dto/          | OrderRequest.java           |
| order-service/src/main/java/.../order/dto/          | OrderResponse.java          |
| order-service/src/main/java/.../order/dto/          | OrderPlacedEvent.java       |
| order-service/src/main/java/.../order/entity/       | Order.java                  |
| order-service/src/main/java/.../order/exception/    | GlobalExceptionHandler.java |
| order-service/src/main/java/.../order/exception/    | PaymentFailedException.java |
| order-service/src/main/java/.../order/exception/    | ResourceNotFoundException.java |
| order-service/src/main/java/.../order/repository/   | OrderRepository.java        |
| order-service/src/main/java/.../order/service/      | OrderService.java           |

#### Wallet Service (Phase 3.3)

| Directory                                           | File                        |
|-----------------------------------------------------|-----------------------------|
| wallet-service/                                     | pom.xml                     |
| wallet-service/src/main/resources/                  | application.yml             |
| wallet-service/src/main/java/.../wallet/            | WalletServiceApplication.java|
| wallet-service/src/main/java/.../wallet/controller/ | WalletController.java       |
| wallet-service/src/main/java/.../wallet/config/     | RabbitMQConfig.java         |
| wallet-service/src/main/java/.../wallet/dto/        | WalletRequest.java          |
| wallet-service/src/main/java/.../wallet/dto/        | WalletResponse.java         |
| wallet-service/src/main/java/.../wallet/dto/        | OrderPlacedEvent.java       |
| wallet-service/src/main/java/.../wallet/entity/     | Wallet.java                 |
| wallet-service/src/main/java/.../wallet/exception/  | GlobalExceptionHandler.java |
| wallet-service/src/main/java/.../wallet/exception/  | InsufficientBalanceException.java |
| wallet-service/src/main/java/.../wallet/exception/  | ResourceNotFoundException.java |
| wallet-service/src/main/java/.../wallet/repository/ | WalletRepository.java       |
| wallet-service/src/main/java/.../wallet/service/    | WalletService.java          |

---

## Implementation Complete

All 8 microservices have been implemented:

| Phase   | Service           | Key Features                                    |
|---------|-------------------|-------------------------------------------------|
| Phase 1 | discovery-service | Eureka Server for service registration          |
| Phase 1 | gateway-service   | API Gateway with route mapping                  |
| Phase 1 | admin-service     | Spring Boot Admin monitoring                    |
| Phase 2 | profile-service   | User CRUD (password secured)                    |
| Phase 2 | product-service   | Product catalog with category                   |
| Phase 3.1| cart-service     | Shopping cart management                        |
| Phase 3.2| order-service    | Orders + Simulated Stripe + RabbitMQ publisher  |
| Phase 3.3| wallet-service   | Wallet balance + RabbitMQ consumer              |

### Prerequisites to Run

1. **Java 17** installed
2. **Maven 3.8+** installed
3. **MySQL** running on localhost:3306 (root/root)
4. **RabbitMQ** running on localhost:5672 (guest/guest)

### Startup Order

```bash
# 1. Infrastructure
cd discovery-service && mvn spring-boot:run
cd admin-service && mvn spring-boot:run
cd gateway-service && mvn spring-boot:run

# 2. Core Services
cd profile-service && mvn spring-boot:run
cd product-service && mvn spring-boot:run
cd cart-service && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd wallet-service && mvn spring-boot:run
```

### Verification URLs

| Service           | URL                                    |
|-------------------|----------------------------------------|
| Eureka Dashboard  | http://localhost:8761                  |
| Admin Dashboard   | http://localhost:9090                  |
| Profile Swagger   | http://localhost:8081/swagger-ui.html  |
| Product Swagger   | http://localhost:8082/swagger-ui.html  |
| Cart Swagger      | http://localhost:8083/swagger-ui.html  |
| Order Swagger     | http://localhost:8084/swagger-ui.html  |
| Wallet Swagger    | http://localhost:8085/swagger-ui.html  |

---

## Phase 1.5: Enterprise Security & Hardening

This phase implements JWT authentication, BCrypt password hashing, reactive gateway security, input validation, and comprehensive SLF4J logging across all services.

---

### Security Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           SECURITY FLOW ARCHITECTURE                                 │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│   ┌──────────────┐                                                                   │
│   │   CLIENT     │                                                                   │
│   │  (Browser/   │                                                                   │
│   │   Mobile)    │                                                                   │
│   └──────┬───────┘                                                                   │
│          │                                                                           │
│          │ 1. POST /profiles (Register)                                              │
│          │    POST /profiles/login (Login)                                           │
│          ▼                                                                           │
│   ┌────────────────────────────────────────┐                                         │
│   │         GATEWAY-SERVICE (:8080)        │                                         │
│   │                                        │                                         │
│   │  ┌──────────────────────────────────┐  │                                         │
│   │  │    JwtAuthenticationFilter       │  │                                         │
│   │  │                                  │  │                                         │
│   │  │  • Whitelist: /profiles (POST),  │  │                                         │
│   │  │    /profiles/login, /actuator/** │  │                                         │
│   │  │  • Protected: All other routes   │  │                                         │
│   │  │  • Validates JWT Bearer token    │  │                                         │
│   │  │  • Adds X-User-Id, X-User-Email  │  │                                         │
│   │  │    headers to downstream         │  │                                         │
│   │  └──────────────────────────────────┘  │                                         │
│   └────────────────┬───────────────────────┘                                         │
│                    │                                                                 │
│     ┌──────────────┴──────────────┐                                                  │
│     │                             │                                                  │
│     ▼                             ▼                                                  │
│ ┌─────────────────┐      ┌─────────────────────────────────────┐                     │
│ │ PROFILE-SERVICE │      │    DOWNSTREAM SERVICES              │                     │
│ │    (:8081)      │      │                                     │                     │
│ │                 │      │  product-service (:8082)            │                     │
│ │ Identity        │      │  cart-service (:8083)               │                     │
│ │ Provider:       │      │  order-service (:8084)              │                     │
│ │                 │      │  wallet-service (:8085)             │                     │
│ │ • BCrypt Hash   │      │                                     │                     │
│ │ • JWT Generate  │      │  NO Spring Security dependencies    │                     │
│ │ • /login        │      │  Trust gateway-validated requests   │                     │
│ │ • /verify-pwd   │      │  Access user via X-User-Id header   │                     │
│ └─────────────────┘      └─────────────────────────────────────┘                     │
│                                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

### Authentication Flow

```
┌────────────┐     ┌──────────────┐     ┌─────────────────┐     ┌─────────────┐
│   Client   │     │   Gateway    │     │ Profile-Service │     │   MySQL     │
└─────┬──────┘     └──────┬───────┘     └────────┬────────┘     └──────┬──────┘
      │                   │                      │                     │
      │ 1. POST /profiles/login                  │                     │
      │   {"email":"...","password":"..."}       │                     │
      │──────────────────►│                      │                     │
      │                   │                      │                     │
      │                   │ 2. Route to          │                     │
      │                   │    PROFILE-SERVICE   │                     │
      │                   │─────────────────────►│                     │
      │                   │                      │                     │
      │                   │                      │ 3. SELECT user      │
      │                   │                      │    WHERE email=...  │
      │                   │                      │────────────────────►│
      │                   │                      │◄────────────────────│
      │                   │                      │                     │
      │                   │                      │ 4. BCrypt.matches() │
      │                   │                      │    Verify password  │
      │                   │                      │                     │
      │                   │                      │ 5. JwtUtil.generate │
      │                   │                      │    HS256 signed     │
      │                   │                      │                     │
      │                   │◄─────────────────────│                     │
      │                   │ 6. Return JWT token  │                     │
      │◄──────────────────│                      │                     │
      │  {"token":"eyJ..","userId":1,            │                     │
      │   "email":"...","expiresIn":86400000}    │                     │
      │                   │                      │                     │
```

---

### Protected Request Flow

```
┌────────────┐     ┌──────────────┐     ┌─────────────────┐
│   Client   │     │   Gateway    │     │ Product-Service │
└─────┬──────┘     └──────┬───────┘     └────────┬────────┘
      │                   │                      │
      │ 1. GET /products  │                      │
      │    Authorization: Bearer eyJ...          │
      │──────────────────►│                      │
      │                   │                      │
      │                   │ 2. JwtAuthFilter     │
      │                   │    validates token   │
      │                   │    extracts claims   │
      │                   │                      │
      │                   │ 3. Add headers:      │
      │                   │    X-User-Id: 123    │
      │                   │    X-User-Email: ... │
      │                   │                      │
      │                   │ 4. Route to          │
      │                   │    PRODUCT-SERVICE   │
      │                   │─────────────────────►│
      │                   │                      │
      │                   │◄─────────────────────│
      │◄──────────────────│ 5. Return products  │
      │                   │                      │
```

---

### JWT Configuration

| Property         | Value                                               |
|------------------|-----------------------------------------------------|
| Algorithm        | HS256 (HMAC-SHA256)                                 |
| Expiration       | 86400000ms (24 hours)                               |
| Token Type       | Bearer                                              |
| Claims           | sub (userId), email, iat, exp                       |

**JWT Secret Key** (application.yml):
```yaml
jwt:
  secret: CartManagementSystemSecretKeyForJWTTokenGeneration2024SecureKey
  expiration: 86400000
```

---

### Gateway Security Configuration

**Whitelisted Paths** (no authentication required):
| Method | Path                      | Description              |
|--------|---------------------------|--------------------------|
| POST   | /profiles                 | User registration        |
| POST   | /profiles/login           | User login               |
| POST   | /profiles/verify-password | Password re-verification |
| GET    | /actuator/**              | Health/metrics endpoints |
| GET    | /eureka/**                | Eureka client endpoints  |
| GET    | /swagger-ui/**            | API documentation        |

**Protected Paths** (require valid JWT):
- All other routes (`/**`)

---

### Input Validation Rules

#### Profile Service - UserRequest

| Field    | Validations                          |
|----------|--------------------------------------|
| fullName | @NotBlank                            |
| email    | @NotBlank, @Email                    |
| password | @NotBlank, @Size(min=6, max=100)     |

#### Profile Service - LoginRequest

| Field    | Validations                          |
|----------|--------------------------------------|
| email    | @NotBlank, @Email                    |
| password | @NotBlank                            |

#### Product Service - ProductRequest

| Field    | Validations                                  |
|----------|----------------------------------------------|
| name     | @NotBlank                                    |
| price    | @NotNull, @DecimalMin("0.01")                |
| stock    | @NotNull, @Min(0)                            |
| category | @NotBlank                                    |

#### Cart Service - CartRequest

| Field  | Validations                          |
|--------|--------------------------------------|
| userId | @NotNull                             |
| items  | @NotEmpty, @Valid (nested)           |

#### Cart Service - CartItemDto

| Field     | Validations                          |
|-----------|--------------------------------------|
| productId | @NotNull                             |
| quantity  | @NotNull, @Min(1)                    |

#### Order Service - OrderRequest

| Field       | Validations                          |
|-------------|--------------------------------------|
| userId      | @NotNull                             |
| totalAmount | @NotNull, @DecimalMin("0.01")        |

#### Wallet Service - WalletRequest

| Field  | Validations                          |
|--------|--------------------------------------|
| userId | @NotNull                             |
| amount | @NotNull, @DecimalMin("0.00")        |

---

### Validation Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input validation failed",
  "fieldErrors": [
    {
      "field": "email",
      "message": "must be a well-formed email address",
      "rejectedValue": "invalid-email"
    },
    {
      "field": "password",
      "message": "size must be between 6 and 100",
      "rejectedValue": "12345"
    }
  ]
}
```

---

### New API Endpoints

#### Authentication Endpoints (Profile Service)

**POST /profiles/login** - User Login
```json
// Request
{
  "email": "john@example.com",
  "password": "securePassword123"
}

// Response (200 OK)
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "john@example.com",
  "fullName": "John Doe",
  "expiresIn": 86400000
}

// Response (401 Unauthorized)
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

**POST /profiles/verify-password** - Password Re-verification
```json
// Request
{
  "userId": 1,
  "password": "securePassword123"
}

// Response
{
  "valid": true
}
```

---

### SLF4J Logging Examples

All controllers and services now include explicit SLF4J loggers (no Lombok):

```java
// Controller Example
@RestController
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        logger.info("POST /products - Creating product: {}", request.getName());
        ProductResponse response = productService.createProduct(request);
        logger.info("Product created with id: {}", response.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
```

**Log Output Format**:
```
2024-01-15 10:30:00  INFO  c.e.product.controller.ProductController : POST /products - Creating product: Laptop
2024-01-15 10:30:00  INFO  c.e.product.service.ProductService : Saving new product: Laptop
2024-01-15 10:30:00  INFO  c.e.product.controller.ProductController : Product created with id: 1
```

---

### Files Modified in Phase 1.5

#### Profile Service (Identity Provider)

| File                          | Changes                                          |
|-------------------------------|--------------------------------------------------|
| pom.xml                       | +spring-security-crypto, +jjwt-*, +validation    |
| application.yml               | +jwt.secret, +jwt.expiration                     |
| config/SecurityConfig.java    | NEW: BCryptPasswordEncoder bean, OAuth2 stubs   |
| security/JwtUtil.java         | NEW: Token generation/validation, 2FA stubs     |
| controller/AuthController.java| NEW: /login, /verify-password endpoints         |
| dto/LoginRequest.java         | NEW: Email/password with validation             |
| dto/LoginResponse.java        | NEW: Token response DTO                         |
| dto/PasswordVerifyRequest.java| NEW: Re-auth request DTO                        |
| dto/PasswordVerifyResponse.java| NEW: Boolean response DTO                      |
| exception/InvalidCredentialsException.java | NEW: 401 exception             |
| service/UserService.java      | +BCrypt hashing, +authenticate(), +verifyPassword(), +logging |
| controller/UserController.java| +@Valid annotations, +SLF4J logging             |
| dto/UserRequest.java          | +@NotBlank, +@Email, +@Size validations         |
| exception/GlobalExceptionHandler.java | +MethodArgumentNotValidException handler |

#### Gateway Service (Security Bouncer)

| File                              | Changes                                      |
|-----------------------------------|----------------------------------------------|
| pom.xml                           | +spring-security, +oauth2-resource-server, +jjwt |
| application.yml                   | +jwt.secret, +cors configuration             |
| config/SecurityConfig.java        | NEW: @EnableWebFluxSecurity, SecurityWebFilterChain |
| security/JwtAuthenticationFilter.java | NEW: WebFilter for JWT validation        |

#### Product Service

| File                          | Changes                                          |
|-------------------------------|--------------------------------------------------|
| pom.xml                       | +spring-boot-starter-validation                  |
| dto/ProductRequest.java       | +@NotBlank, +@NotNull, +@DecimalMin, +@Min       |
| controller/ProductController.java | +@Valid annotations, +SLF4J Logger           |
| exception/GlobalExceptionHandler.java | +MethodArgumentNotValidException handler |

#### Cart Service

| File                          | Changes                                          |
|-------------------------------|--------------------------------------------------|
| dto/CartRequest.java          | +@NotNull userId, +@NotEmpty @Valid items        |
| dto/CartItemDto.java          | +@NotNull productId, +@NotNull @Min quantity     |
| controller/CartController.java| +@Valid annotations, +SLF4J Logger               |
| exception/GlobalExceptionHandler.java | +MethodArgumentNotValidException handler |

#### Order Service

| File                          | Changes                                          |
|-------------------------------|--------------------------------------------------|
| dto/OrderRequest.java         | +@NotNull userId, +@DecimalMin totalAmount       |
| controller/OrderController.java | +@Valid annotations, +SLF4J Logger             |
| exception/GlobalExceptionHandler.java | +MethodArgumentNotValidException handler |

#### Wallet Service

| File                          | Changes                                          |
|-------------------------------|--------------------------------------------------|
| dto/WalletRequest.java        | +@NotNull userId, +@DecimalMin amount            |
| controller/WalletController.java | +@Valid annotations, +SLF4J Logger            |
| exception/GlobalExceptionHandler.java | +MethodArgumentNotValidException handler |

---

### Dependencies Added

#### Profile Service (pom.xml)
```xml
<!-- BCrypt password hashing (no full Spring Security) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
</dependency>

<!-- JWT Token handling -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>

<!-- Input validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

#### Gateway Service (pom.xml)
```xml
<!-- Reactive Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 Resource Server (future-proofing) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- JWT libraries (same as profile-service) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<!-- ... impl and jackson -->
```

---

### Testing Phase 1.5

#### Register New User
```bash
curl -X POST http://localhost:8080/profiles \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Doe","email":"john@example.com","password":"securePass123"}'
```

#### Login and Get JWT Token
```bash
curl -X POST http://localhost:8080/profiles/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"securePass123"}'

# Response:
# {"token":"eyJhbG...","userId":1,"email":"john@example.com","fullName":"John Doe","expiresIn":86400000}
```

#### Access Protected Endpoint
```bash
# Without token (401 Unauthorized)
curl http://localhost:8080/products

# With valid token (200 OK)
curl http://localhost:8080/products \
  -H "Authorization: Bearer eyJhbG..."
```

#### Test Validation
```bash
# Missing required field (400 Bad Request)
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbG..." \
  -d '{"name":"","price":-10,"stock":-5}'

# Response:
# {"timestamp":"...","status":400,"error":"Validation Failed","fieldErrors":[...]}
```

---

### Future-Proofing Stubs

#### OAuth2 Integration (SecurityConfig.java)
```java
// TODO: Phase 2 - OAuth2 Social Login
// @Bean
// public ClientRegistrationRepository clientRegistrationRepository() {
//     return new InMemoryClientRegistrationRepository(
//         CommonOAuth2Provider.GOOGLE.getBuilder("google")
//             .clientId("your-google-client-id")
//             .clientSecret("your-google-client-secret")
//             .build()
//     );
// }
```

#### Two-Factor Authentication (JwtUtil.java)
```java
// TODO: Phase 2 - 2FA/MFA Support
// public String generateTotpSecret() {
//     return new GoogleAuthenticator().createCredentials().getKey();
// }
// public boolean verifyTotp(String secret, int code) {
//     return new GoogleAuthenticator().authorize(secret, code);
// }
```

---

### Security Notes

1. **Password Storage**: User passwords are hashed with BCrypt (default 10 rounds) before storage. Plain text passwords are never stored or logged.

2. **Token Security**: JWT tokens are signed with HS256 algorithm. The secret key must be kept secure and should be externalized to environment variables in production.

3. **Decoupled Security**: Only gateway-service and profile-service have Spring Security dependencies. Downstream services (product, cart, order, wallet) remain security-free and trust the gateway's validation.

4. **CORS Configuration**: Gateway allows requests from localhost:3000 (React), localhost:4200 (Angular), and localhost:5173 (Vite).

5. **User Context Propagation**: Gateway adds `X-User-Id` and `X-User-Email` headers to authenticated requests, allowing downstream services to identify the user without JWT parsing.
