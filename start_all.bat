@echo off
echo ===================================================
echo   INJECTING ENVIRONMENT VARIABLES...
echo ===================================================

:: 1. Set the exact paths to your portable JDK and Maven
set JAVA_HOME=C:\Users\bnileshn\Downloads\OpenJDK17U-jdk_x64_windows_hotspot_17.0.18_8\jdk-17.0.18+8
set MAVEN_HOME=C:\Users\bnileshn\Downloads\apache-maven-3.9.12

:: 2. Inject them into the PATH for this session
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

echo Java and Maven are set for this session!
echo.
echo ===================================================
echo   INITIATING V3 CLOUD-NATIVE CLUSTER
echo ===================================================

:: Note: We use 'cmd /k' so if a service crashes, the window STAYS OPEN so you can read the error!

echo [1/8] Starting Eureka Discovery Server...
start "Eureka Server" cmd /k "cd discovery-service && mvn spring-boot:run"
timeout /t 10 /nobreak >nul

echo [2/8] Starting API Gateway...
start "API Gateway" cmd /k "cd gateway-service && mvn spring-boot:run"

echo [3/8] Starting Spring Boot Admin...
start "Boot Admin" cmd /k "cd admin-service && mvn spring-boot:run"

echo [4/8] Starting Profile Service...
start "Profile Service" cmd /k "cd profile-service && mvn spring-boot:run"

echo [5/8] Starting Product Service...
start "Product Service" cmd /k "cd product-service && mvn spring-boot:run"

echo [6/8] Starting Cart Service...
start "Cart Service" cmd /k "cd cart-service && mvn spring-boot:run"

echo [7/8] Starting Wallet Service...
start "Wallet Service" cmd /k "cd wallet-service && mvn spring-boot:run"

echo [8/8] Starting Order Service (Stripe/RabbitMQ)...
start "Order Service" cmd /k "cd order-service && mvn spring-boot:run"

echo ===================================================
echo   All 8 services are booting up!
echo   Eureka UI: http://localhost:8761
echo   Admin UI:  http://localhost:9090
echo ===================================================
pause