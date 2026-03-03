package com.eshoppingzone.product.controller;

import com.eshoppingzone.product.dto.ProductRequest;
import com.eshoppingzone.product.dto.ProductResponse;
import com.eshoppingzone.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@Tag(name = "Product Catalog", description = "Product Catalog Management APIs")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
        logger.info("ProductController initialized");
    }

    @GetMapping
    @Operation(summary = "Get all products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        logger.info("GET /products - Fetching all products");
        List<ProductResponse> products = productService.getAllProducts();
        logger.info("Returning {} products", products.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        logger.info("GET /products/{} - Fetching product by ID", id);
        ProductResponse product = productService.getProductById(id);
        logger.info("Found product: {}", product.getName());
        return ResponseEntity.ok(product);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products by name")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String name) {
        logger.info("GET /products/search?name={} - Searching products", name);
        List<ProductResponse> products = productService.searchProducts(name);
        logger.info("Found {} products matching '{}'", products.size(), name);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/available")
    @Operation(summary = "Get all available products (stock > 0)")
    public ResponseEntity<List<ProductResponse>> getAvailableProducts() {
        logger.info("GET /products/available - Fetching available products");
        List<ProductResponse> products = productService.getAvailableProducts();
        logger.info("Returning {} available products", products.size());
        return ResponseEntity.ok(products);
    }

    @PostMapping
    @Operation(summary = "Create a new product")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        logger.info("POST /products - Creating new product: {}", request.getName());
        ProductResponse createdProduct = productService.createProduct(request);
        logger.info("Product created with id: {}", createdProduct.getId());
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, 
                                                          @Valid @RequestBody ProductRequest request) {
        logger.info("PUT /products/{} - Updating product", id);
        ProductResponse updatedProduct = productService.updateProduct(id, request);
        logger.info("Product updated: {}", updatedProduct.getName());
        return ResponseEntity.ok(updatedProduct);
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update product stock (positive to add, negative to reduce)")
    public ResponseEntity<ProductResponse> updateStock(@PathVariable Long id, @RequestParam Integer quantity) {
        logger.info("PATCH /products/{}/stock?quantity={} - Updating stock", id, quantity);
        ProductResponse product = productService.updateStock(id, quantity);
        logger.info("Stock updated for product {}: new stock = {}", product.getName(), product.getStock());
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        logger.info("DELETE /products/{} - Deleting product", id);
        productService.deleteProduct(id);
        logger.info("Product deleted with id: {}", id);
        return ResponseEntity.noContent().build();
    }

}
