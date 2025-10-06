package com.example.perftest.business;

import com.example.perftest.auth.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final TokenService tokenService;

    public ProductController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(
            @PathVariable("id") String id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String auth
    ) {
        String token = null;
        if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
            token = auth.substring(7);
        }
        if (!tokenService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_token"));
        }
        // return dummy product payload
        return ResponseEntity.ok(Map.of(
                "id", id,
                "name", "Product-" + id,
                "price", 9.99,
                "currency", "EUR"
        ));
    }
}
