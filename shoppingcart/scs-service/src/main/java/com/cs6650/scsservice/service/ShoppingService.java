package com.cs6650.scsservice.service;

import com.cs6650.scsservice.dto.CheckoutRequest;
import com.cs6650.scsservice.model.OrderMessage;
import com.cs6650.scsservice.model.PaymentDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ShoppingService {

    private final RestTemplate restTemplate; // HTTP Request
    private final RabbitMQService rabbitMQService; // Send RabbitMQ Message
    private final ObjectMapper objectMapper; // Convert object to JSON

    //Don't Encode url, AWS setting
    //All setting in application.properties
    @Value("${cca.base-url}")
    private String ccaBaseUrl;

    @Value("${cca.authorize-path:/credit-card/authorize}")
    private String authorizePath;

    public ShoppingService(RestTemplate restTemplate, RabbitMQService rabbitMQService, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.rabbitMQService = rabbitMQService;
        this.objectMapper = objectMapper;
    }

    public PaymentDecision processCheckout(CheckoutRequest request) {
        // Defensive checks
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return PaymentDecision.PAYMENT_SYSTEM_UNAVAILABLE;
        }

        String card = request.getCreditCard();
        if (card == null || card.isBlank()) {
            return PaymentDecision.INVALID_FORMAT;
        }

        // CCA expects JSON: { "cardNumber": "1234-..." }
        Map<String, String> ccaRequest = Map.of("cardNumber", card);
        String url = ccaBaseUrl + authorizePath;

        try {
            // Sync Call CCA, and sen message to RabbitMQ only if PaymentSuccess,
            restTemplate.postForEntity(url, ccaRequest, String.class);

            // 2) If CCA authorized, publish order message to RabbitMQ and WAIT confirms
            OrderMessage msg = new OrderMessage(
                    request.getCartId() + "-" + System.currentTimeMillis(),
                    request.getCartId(),
                    request.getItems()
            );

            String json = objectMapper.writeValueAsString(msg);

            // Confirm success before returning AUTHORIZED
            rabbitMQService.publishAndConfirm(json);

            return PaymentDecision.AUTHORIZED;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.PAYMENT_REQUIRED) return PaymentDecision.DECLINED;
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) return PaymentDecision.INVALID_FORMAT;
            return PaymentDecision.PAYMENT_SYSTEM_UNAVAILABLE;

        } catch (ResourceAccessException e) {
            // timeout / DNS / connection refused
            return PaymentDecision.PAYMENT_SYSTEM_UNAVAILABLE;

        } catch (Exception e) {
            // JSON serialization or RabbitMQ publish/confirm failure etc.
            return PaymentDecision.PAYMENT_SYSTEM_UNAVAILABLE;
        }
    }
}

