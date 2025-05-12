package com.lumos.lumosspring.authentication.service;

import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.authentication.dto.LoginResponse;
import com.lumos.lumosspring.authentication.entities.RefreshToken;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.user.Role;
import com.lumos.lumosspring.user.UserRepository;
import com.lumos.lumosspring.user.UserService;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;

import jakarta.transaction.Transactional;

@Service
public class TokenService {
    private final UserService userService;
    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Util util;

    public TokenService(UserService userService, JwtEncoder jwtEncoder, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, @Qualifier("jwtDecoder") JwtDecoder jwtDecoder, RefreshTokenRepository refreshTokenRepository, Util util) {
        this.userService = userService;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtDecoder = jwtDecoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.util = util;
    }

    @Scheduled(cron = "0 0 3 * * *") // Roda todo dia às 3 da manhã
    @Transactional
    public void cleanUpExpiredTokens() {
        refreshTokenRepository.deleteExpiredOrRevokedTokens(util.getDateTime());
    }

    public ResponseEntity<?> forgotPassword(LoginRequest loginRequest) {
        var user = userService.findUserByUsernameOrCpf(loginRequest.username());

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return userService.resetPassword(user.get().getIdUser().toString());
    }

    public ResponseEntity<LoginResponse> refreshToken(String refreshToken) {
        var now = util.getDateTime();
        var expiresIn = 1800L; // 30 minutos para access token expirar

        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty() || tokenFromDb.get().isRevoked() || tokenFromDb.get().getExpiryDate().isBefore(now)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Jwt jwt = jwtDecoder.decode(refreshToken);
        var user = tokenFromDb.get().getUser();
        var scope = user.getRoles()
                .stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(" "));

        // Cria novo token de acesso com as mesmas informações do refresh token
        var accessTokenClaims = JwtClaimsSet.builder()
                .issuer("LumosSoftware")
                .subject(user.getIdUser().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scope)
                .build();

        var newAccessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessTokenClaims)).getTokenValue();

        return ResponseEntity.ok(new LoginResponse(newAccessToken, expiresIn, scope));
    }

    public ResponseEntity<?> login(LoginRequest loginRequest, HttpServletResponse response) {
        var user = userRepository.findByUsernameOrCpfIgnoreCase(loginRequest.username(), loginRequest.username());
        if (user.isEmpty() || !user.get().isLoginCorrect(loginRequest, passwordEncoder)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Usuário/CPF ou senha incorretos"));
        }

        if (!user.get().getStatus()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("O Usuário informado não possuí permissão de acesso."));
        }

        var now = util.getDateTime();
        var expiresIn = 2592000L; // 30 Minutos
        var refreshExpiresIn = 2592000L; // Expiração do refresh token (30 dias)

        var scopes = user.get().getRoles()
                .stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(" "));

        var accessTokenClaims = JwtClaimsSet.builder()
                .issuer("LumosSoftware")
                .subject(user.get().getIdUser().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scopes)
                .build();

        var refreshTokenClaims = JwtClaimsSet.builder()
                .issuer("LumosSoftware")
                .subject(user.get().getIdUser().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(refreshExpiresIn))
                .claim("jti", UUID.randomUUID().toString())
                .build();

        var accessTokenValue = jwtEncoder.encode(JwtEncoderParameters.from(accessTokenClaims)).getTokenValue();
        var refreshTokenValue = jwtEncoder.encode(JwtEncoderParameters.from(refreshTokenClaims)).getTokenValue();

        var refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiryDate(now.plusSeconds(refreshExpiresIn));
        refreshToken.setUser(user.get());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        // Configura o refreshToken como um cookie HTTP-Only
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenValue);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // Use apenas em HTTPS em produção
        refreshTokenCookie.setPath("/"); // Disponível em toda a aplicação
        refreshTokenCookie.setMaxAge((int) refreshExpiresIn); // Expiração em 1 dia
        response.addCookie(refreshTokenCookie);

        // Retorna o accessToken no corpo da resposta
        return ResponseEntity.ok(new LoginResponse(accessTokenValue, expiresIn, scopes));
    }

    public ResponseEntity<Void> logout(String refreshToken, HttpServletResponse response) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isPresent()) {
            var token = tokenFromDb.get();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }

        // Remove o cookie do refreshToken do navegador
        Cookie deleteCookie = new Cookie("refreshToken", null);
        deleteCookie.setPath("/");
        deleteCookie.setHttpOnly(true);
        deleteCookie.setMaxAge(0); // Remove o cookie imediatamente
        deleteCookie.setSecure(true); // Use apenas em HTTPS em produção
        response.addCookie(deleteCookie);

        return ResponseEntity.noContent().build();
    }
}