package com.lumos.lumosspring.authentication.service;

import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.authentication.dto.LoginResponse;
import com.lumos.lumosspring.authentication.dto.LoginResponseMobile;
import com.lumos.lumosspring.authentication.entities.RefreshToken;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.team.entities.Stockist;
import com.lumos.lumosspring.team.entities.Team;
import com.lumos.lumosspring.team.repository.StockistRepository;
import com.lumos.lumosspring.user.Role;
import com.lumos.lumosspring.user.AppUser;
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

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final StockistRepository stockistRepository;

    public TokenService(UserService userService, JwtEncoder jwtEncoder, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, @Qualifier("jwtDecoder") JwtDecoder jwtDecoder, RefreshTokenRepository refreshTokenRepository, Util util, StockistRepository stockistRepository) {
        this.userService = userService;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtDecoder = jwtDecoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.util = util;
        this.stockistRepository = stockistRepository;
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

        return userService.resetPassword(user.get().getUserId().toString());
    }

    public ResponseEntity<LoginResponse> refreshToken(String refreshToken, boolean isMobile) {
        var now = util.getDateTime();
        var expiresIn = 1800L; // 30 minutos para access token expirar

        if (isMobile) {
            expiresIn = 2592000L;
        }

        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty() || tokenFromDb.get().isRevoked() || tokenFromDb.get().getExpiryDate().isBefore(now)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var user = tokenFromDb.get().getUser();
        var scope = getScope(user);

        // Cria novo token de acesso com as mesmas informações do refresh token
        var accessTokenClaims = getAccessClaims(
                user,
                now,
                expiresIn,
                scope
        );

        var newAccessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessTokenClaims)).getTokenValue();

        return ResponseEntity.ok(new LoginResponse(newAccessToken, expiresIn, getRoles(user), getTeams(user)));
    }

    public ResponseEntity<?> login(LoginRequest loginRequest, HttpServletResponse response, boolean isMobile) {
        var user = userRepository.findByUsernameOrCpfIgnoreCase(loginRequest.username(), loginRequest.username());
        if (user.isEmpty() || !user.get().isLoginCorrect(loginRequest, passwordEncoder)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Usuário/CPF ou senha incorretos"));
        }

        if (!user.get().getStatus()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("O Usuário informado não possuí permissão de acesso."));
        }

        if (isMobile) {
            var allowedRoles = new HashSet<>(Set.of(Role.Values.MOTORISTA.name(), Role.Values.ELETRICISTA.name(), Role.Values.ANALISTA.name(), Role.Values.ADMIN.name(), Role.Values.RESPONSAVEL_TECNICO.name()));
            var roles = user.get().getRoles();
            boolean hasAccess = roles.stream().anyMatch(roleName -> allowedRoles.contains(roleName.getRoleName()));

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Usuário sem acesso ao aplicativo"));
            }
        }

        var now = util.getDateTime();
        var expiresIn = 1800L; // 30 Minutos
        var refreshExpiresIn = 2592000L; // Expiração do refresh token (30 dias)

        if (isMobile) {
            expiresIn = 2592000L;
            refreshExpiresIn = 15552000L;
        }

        var scopes = getScope(user.get());

        var accessTokenClaims = getAccessClaims(
                user.get(),
                now,
                expiresIn,
                scopes
        );

        var refreshTokenClaims = JwtClaimsSet.builder()
                .issuer("LumosSoftware")
                .subject(user.get().getUserId().toString())
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
        if (!isMobile) {
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshTokenValue);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true); // Use apenas em HTTPS em produção
            refreshTokenCookie.setPath("/"); // Disponível em toda a aplicação
            refreshTokenCookie.setMaxAge((int) refreshExpiresIn); // Expiração em 1 dia
            response.addCookie(refreshTokenCookie);

            return ResponseEntity.ok(new LoginResponse(accessTokenValue, expiresIn, getRoles(user.get()), getTeams(user.get())));
        } else {
            var responseBody = new LoginResponseMobile(accessTokenValue, expiresIn, getRoles(user.get()), getTeams(user.get()), refreshTokenValue, user.get().getUserId().toString());

            return ResponseEntity.ok(responseBody);
        }

    }

    public ResponseEntity<Void> logout(String refreshToken, HttpServletResponse response, boolean isMobile) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isPresent()) {
            var token = tokenFromDb.get();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }

        if (!isMobile) {
            // Remove o cookie do refreshToken do navegador
            Cookie deleteCookie = new Cookie("refreshToken", null);
            deleteCookie.setPath("/");
            deleteCookie.setHttpOnly(true);
            deleteCookie.setMaxAge(0); // Remove o cookie imediatamente
            deleteCookie.setSecure(true); // Use apenas em HTTPS em produção
            response.addCookie(deleteCookie);
        }


        return ResponseEntity.noContent().build();
    }

    private String getScope(AppUser appUser) {
        var roles = getRoles(appUser);
        var teams = getTeams(appUser);

        if (roles.isBlank()) return teams;
        if (teams.isBlank()) return roles;
        return roles + " " + teams;
    }

    private String getRoles(AppUser appUser) {
        return appUser.getRoles()
                .stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(" "));
    }

    private String getTeams(AppUser appUser) {
        var eTeams = Optional.ofNullable(appUser.getElectricians())
                .orElse(Collections.emptyList())
                .stream()
                .map(Team::getTeamCode)
                .collect(Collectors.joining(" "));

        var dTeams = Optional.ofNullable(appUser.getDrivers())
                .orElse(Collections.emptyList())
                .stream()
                .map(Team::getTeamCode)
                .collect(Collectors.joining(" "));

        var stockists = stockistRepository.findAllByUserId(appUser.getUserId());
        var sTeams = Optional.ofNullable(stockists)
                .orElse(Collections.emptyList())
                .stream()
                .map(Stockist::getStockistCode)
                .collect(Collectors.joining(" "));

        // Junta todos com espaços, ignorando vazios
        return Stream.of(eTeams, dTeams, sTeams)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    private JwtClaimsSet getAccessClaims(AppUser appUser, Instant now, Long expiresIn, String scope) {
        return JwtClaimsSet.builder()
                .issuer("LumosSoftware")
                .subject(appUser.getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scope)
                .build();
    }


}