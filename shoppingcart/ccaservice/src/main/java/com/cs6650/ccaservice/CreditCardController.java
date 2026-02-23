package com.cs6650.ccaservice;

import com.cs6650.ccaservice.dto.CardRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// Random Number
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/credit-card")
public class CreditCardController {

    // Strict format: dddd-dddd-dddd-dddd
    private static final Pattern CARD_PATTERN =
            Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$");

    /**
     * Health check endpoint for ALB.
     * Always returns 200.
     */
    @GetMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK for healthy check :)");
    }

    /**
     * Authorize a credit card number.
     * 200: Authorized (90%)
     * 402: Declined  (10%)
     * 400: Bad Request (invalid format)
     */
    @PostMapping(
            value = "/authorize",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> authorize(@RequestBody CardRequest request) {
        // Request or field may be null
        if (request == null || request.getCardNumber() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Can't get request or cardnumber!");
        }

        //if number is valid

        String cardNumber = request.getCardNumber().trim();

        //  Validate format
        if (!CARD_PATTERN.matcher(cardNumber).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Do not match 4 number in a group!");
        }

        //  Mock authorization: 90% pass, 10% decline
        boolean authorized = ThreadLocalRandom.current().nextDouble() < 0.90;

        if (authorized) {
            return ResponseEntity.ok("Authorized");
        } else {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body("Declined,didn't pass 90% threshhold.");
        }
    }
}
