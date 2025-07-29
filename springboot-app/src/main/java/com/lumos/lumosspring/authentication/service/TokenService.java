package com.lumos.lumosspring.authentication.service;

import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.authentication.dto.LoginResponse;
import com.lumos.lumosspring.authentication.dto.LoginResponseMobile;
import com.lumos.lumosspring.authentication.entities.RefreshToken;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.team.repository.TeamQueryRepository;
import com.lumos.lumosspring.team.entities.Stockist;
import com.lumos.lumosspring.team.repository.StockistRepository;
import com.lumos.lumosspring.user.*;
import com.lumos.lumosspring.util.ErrorResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TokenService {
    private final UserService userService;
    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StockistRepository stockistRepository;
    private final RoleRepository roleRepository;
    private final TeamQueryRepository teamQueryRepository;

    public TokenService(UserService userService,
                        JwtEncoder jwtEncoder,
                        UserRepository userRepository,
                        BCryptPasswordEncoder passwordEncoder,
                        RefreshTokenRepository refreshTokenRepository,
                        StockistRepository stockistRepository,
                        RoleRepository roleRepository,
                        TeamQueryRepository teamQueryRepository
    ) {
        this.userService = userService;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.stockistRepository = stockistRepository;
        this.roleRepository = roleRepository;
        this.teamQueryRepository = teamQueryRepository;
    }

//    @Scheduled(cron = "0 0 3 * * *") // Roda todo dia às 3 da manhã
//    @Transactional
//    public void cleanUpExpiredTokens() {
//        refreshTokenRepository.deleteExpiredOrRevokedTokens(util.getDateTime());
//    }

    public ResponseEntity<?> forgotPassword(LoginRequest loginRequest) {
        var user = userService.findUserByUsernameOrCpf(loginRequest.username());

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return userService.resetPassword(user.get().getUserId().toString());
    }

    public ResponseEntity<?> refreshToken(String refreshToken,  HttpServletResponse response, boolean isMobile) {
        var now = Instant.now();
        var expiresIn = 1800L; // 30 minutos para access token expirar
        var refreshExpiresIn = 2592000L; // Expiração do refresh token (30 dias)

        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty() || tokenFromDb.get().isRevoked() || tokenFromDb.get().getExpiryDate().isBefore(now)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var existingToken = tokenFromDb.get();

        var user = userRepository.findByUserId(existingToken.getUser()).orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));
        var scope = getScope(user);

        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        // Cria novo token de acesso com as mesmas informações do refresh token
        var accessTokenClaims = getAccessClaims(
                user,
                now,
                expiresIn,
                scope
        );

        var refreshTokenClaims = getAccessClaims(
                user,
                now,
                refreshExpiresIn,
                scope
        );

        var newAccessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessTokenClaims)).getTokenValue();
        var newRefreshTokenValue = jwtEncoder.encode(JwtEncoderParameters.from(refreshTokenClaims)).getTokenValue();

        var newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(newRefreshTokenValue);
        newRefreshToken.setExpiryDate(now.plusSeconds(refreshExpiresIn));
        newRefreshToken.setUser(user.getUserId());
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);

        // Configura o refreshToken como um cookie HTTP-Only
        if (!isMobile) {
            // Spring não suporta SameSite diretamente via Cookie API — definimos manualmente o header:
            String cookieValue = "refreshToken=" + newRefreshTokenValue +
                    "; Max-Age=" + refreshExpiresIn +
                    "; Path=/" +
                    "; HttpOnly; Secure; SameSite=Strict";
            response.setHeader(HttpHeaders.SET_COOKIE, cookieValue);

            // Não enviar refresh token no body para web
            return ResponseEntity.ok(new LoginResponse(newAccessToken, expiresIn, getRoles(user), getTeams(user)));
        } else {
            return ResponseEntity.ok(new LoginResponseMobile(newAccessToken, expiresIn, getRoles(user), getTeams(user), newRefreshTokenValue, null));
        }
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
            var allowedRoles = new HashSet<>(Set.of(
                    Role.Values.MOTORISTA.name(),
                    Role.Values.ELETRICISTA.name(),
                    Role.Values.ANALISTA.name(),
                    Role.Values.ADMIN.name(),
                    Role.Values.RESPONSAVEL_TECNICO.name()
            ));

            // Busca direto as roles do usuário sem precisar buscar UserRole e Role separadamente
            var rolesNames = roleRepository.findRolesByUserId(user.get().getUserId());

            // Verificar se o usuário tem acesso
            boolean hasAccess = rolesNames.stream().anyMatch(allowedRoles::contains);

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Usuário sem acesso ao aplicativo"));
            }
        }


        var now = Instant.now();
        var expiresIn = 1800L; // 30 Minutos
        var refreshExpiresIn = 2592000L; // Expiração do refresh token (30 dias)

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
        refreshToken.setUser(user.get().getUserId());
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        // Configura o refreshToken como um cookie HTTP-Only
        if (!isMobile) {
            String cookieValue = "refreshToken=" + refreshTokenValue +
                    "; Max-Age=" + refreshExpiresIn +
                    "; Path=/" +
                    "; HttpOnly; Secure; SameSite=Strict";
            response.setHeader(HttpHeaders.SET_COOKIE, cookieValue);

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
        var rolesNames = roleRepository.findRolesByUserId(appUser.getUserId());
        return String.join(" ", rolesNames);
    }

    private String getTeams(AppUser appUser) {
        Long numberTeamId = teamQueryRepository.getTeamIdByUserId(appUser.getUserId());
        String teamId = "";
        if (numberTeamId != null) {
            teamId =  numberTeamId.toString();
        }

        // Obter códigos dos stockists diretamente e juntar com espaço
        var sTeams = stockistRepository.findAllByUserId(appUser.getUserId()).stream()
                .map(Stockist::getStockistCode)
                .collect(Collectors.joining(" "));

        // Junta teamId e sTeams, ignorando valores nulos ou vazios
        return Stream.of(teamId, sTeams)
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