package com.cs6650.scsservice.controller;

import com.cs6650.scsservice.dto.CheckoutRequest;
//enum when calling CCA
import com.cs6650.scsservice.model.PaymentDecision;
import com.cs6650.scsservice.service.ShoppingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shopping-cart")
public class ShoppingCartController {

    private final ShoppingService shoppingService;

    public ShoppingCartController(ShoppingService shoppingService) {
        this.shoppingService = shoppingService;
    }

    // For ALB health check
    @GetMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ALB part is OK, start to check shopping cart");
    }

    // For /shopping-cart/checkout request
    @PostMapping(
            value = "/checkout",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> checkout(@RequestBody CheckoutRequest request) {

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Cart is empty or request is null or items is empty");
        }

        // ShoppingCart Service
        PaymentDecision decision = shoppingService.processCheckout(request);

        //enum four status, use switch
        return switch (decision) {
            case AUTHORIZED ->
                    ResponseEntity.ok("Order Placed Successfully");
            case DECLINED ->
                    ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Payment Declined");
            case INVALID_FORMAT ->
                    ResponseEntity.badRequest().body("Invalid Credit Card");
            case PAYMENT_SYSTEM_UNAVAILABLE ->
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Payment System Unavailable");
        };
    }
}

