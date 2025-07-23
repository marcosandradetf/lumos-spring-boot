package com.lumos.lumosspring.authentication.controller;

import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.authentication.service.TokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/auth")
public class TokenControllerMobile {

    private final TokenService tokenService;

    public TokenControllerMobile(TokenService tokenService) {
        this.tokenService = tokenService;
    }


    @PostMapping("/login")
    public ResponseEntity<?> loginMobile(@RequestBody LoginRequest loginRequest) {
        return tokenService.login(loginRequest, null, true);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String refreshToken) {
        return tokenService.logout(refreshToken, null, true);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        return tokenService.refreshToken(refreshToken, true);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody LoginRequest loginRequest) {
        return tokenService.forgotPassword(loginRequest);
    }
}
