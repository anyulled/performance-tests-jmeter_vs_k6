package com.example.perftest.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final TokenService tokenService;

    public AuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AuthRequest request) {
        // Accept any non-empty credentials for demo purposes
        if (request.getUsername() == null || request.getUsername().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_credentials"));
        }
        String token = tokenService.issueToken(request.getUsername());
        return ResponseEntity.ok(Map.of("access_token", token, "token_type", "bearer"));
    }
}
