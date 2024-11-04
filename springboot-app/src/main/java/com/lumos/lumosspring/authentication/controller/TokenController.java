package com.lumos.lumosspring.authentication.controller;

import com.lumos.lumosspring.authentication.controller.dto.LoginRequest;
import com.lumos.lumosspring.authentication.controller.dto.LoginResponse;
import com.lumos.lumosspring.authentication.controller.dto.RefreshTokenRequest;
import com.lumos.lumosspring.authentication.entities.RefreshToken;
import com.lumos.lumosspring.authentication.entities.Role;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.authentication.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class TokenController {

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenController(JwtEncoder jwtEncoder, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, @Qualifier("jwtDecoder") JwtDecoder jwtDecoder, RefreshTokenRepository refreshTokenRepository) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtDecoder = jwtDecoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        var user = userRepository.findByUsername(loginRequest.username());
        if (user.isEmpty() || !user.get().isLoginCorrect(loginRequest, passwordEncoder)) {
            throw new BadCredentialsException("Usuário ou senha incorretos");
        }

        var now = Instant.now();
        var expiresIn = 300L;
        var refreshExpiresIn = 86400L; // Expiração do refresh token (24 horas)

        var scopes = user.get().getRoles()
                .stream()
                .map(Role::getNomeRole)
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
                .build();

        var accessTokenValue = jwtEncoder.encode(JwtEncoderParameters.from(accessTokenClaims)).getTokenValue();
        var refreshTokenValue = jwtEncoder.encode(JwtEncoderParameters.from(refreshTokenClaims)).getTokenValue();

        var refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiryDate(now.plusSeconds(refreshExpiresIn));
        refreshToken.setUser(user.get());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        // Configura o `refreshToken` como um cookie HTTP-Only
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenValue);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // Use apenas em HTTPS em produção
        refreshTokenCookie.setPath("/"); // Disponível em toda a aplicação
        refreshTokenCookie.setMaxAge((int) refreshExpiresIn); // Expiração em 1 dia
        response.addCookie(refreshTokenCookie);

        // Retorna o `accessToken` no corpo da resposta
        return ResponseEntity.ok(new LoginResponse(accessTokenValue, expiresIn, scopes));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isPresent()) {
            var token = tokenFromDb.get();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }

        // Remove o cookie do `refreshToken` do navegador
        Cookie deleteCookie = new Cookie("refreshToken", null);
        deleteCookie.setPath("/");
        deleteCookie.setHttpOnly(true);
        deleteCookie.setMaxAge(0); // Remove o cookie imediatamente
        deleteCookie.setSecure(true); // Use apenas em HTTPS em produção
        response.addCookie(deleteCookie);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@CookieValue("refreshToken") String refreshToken) {
        var now = Instant.now();
        var expiresIn = 300L;

        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty() || tokenFromDb.get().isRevoked() || tokenFromDb.get().getExpiryDate().isBefore(now)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Jwt jwt = jwtDecoder.decode(refreshToken);
        var user = tokenFromDb.get().getUser();
        var scope = user.getRoles()
                .stream()
                .map(Role::getNomeRole)
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

}
