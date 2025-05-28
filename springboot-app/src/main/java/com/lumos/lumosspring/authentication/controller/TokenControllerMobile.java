package com.lumos.lumosspring.authentication.controller;

import com.lumos.lumosspring.authentication.entities.RefreshToken;
import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.authentication.dto.LoginResponse;
import com.lumos.lumosspring.authentication.dto.LoginResponseMobile;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.authentication.service.TokenService;
import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.team.repository.TeamRepository;
import com.lumos.lumosspring.user.Role;
import com.lumos.lumosspring.user.RoleRepository;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Util;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
