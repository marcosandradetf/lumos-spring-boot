package com.lumos.lumosspring.authentication.controller;

import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.authentication.dto.LoginResponse;
import com.lumos.lumosspring.authentication.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class TokenController {
    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        return tokenService.newLogin(loginRequest, response, false);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {
        return tokenService.logout(refreshToken, response, false);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {
        return tokenService.refreshToken(refreshToken, response, false);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody LoginRequest loginRequest) {
        return tokenService.forgotPassword(loginRequest);
    }

    @GetMapping("/get-qrcode-token")
    public ResponseEntity<?> getQrcodeToken() {
        return tokenService.getQrcodeToken();
    }

    @PostMapping("/login-with-qrcode-token")
    public ResponseEntity<?> loginWithQrCodeToken(
            @RequestParam(value = "token") UUID token,
            HttpServletResponse response
    ) {
        return tokenService.loginWithQrCodeToken(token, response, false);
    }
}
