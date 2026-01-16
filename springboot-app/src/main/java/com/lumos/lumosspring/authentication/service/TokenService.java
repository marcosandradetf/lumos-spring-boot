package com.lumos.lumosspring.authentication.service;

import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.authentication.dto.LoginResponse;
import com.lumos.lumosspring.authentication.dto.LoginResponseMobile;
import com.lumos.lumosspring.authentication.dto.NewLoginResponseMobile;
import com.lumos.lumosspring.authentication.model.QrcodeToken;
import com.lumos.lumosspring.authentication.model.RefreshToken;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.authentication.repository.TenantRepository;
import com.lumos.lumosspring.authentication.repository.QrcodeTokenRepository;
import com.lumos.lumosspring.team.repository.TeamQueryRepository;
import com.lumos.lumosspring.team.repository.StockistRepository;
import com.lumos.lumosspring.user.model.AppUser;
import com.lumos.lumosspring.user.model.Role;
import com.lumos.lumosspring.user.repository.RoleRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.user.service.UserService;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Utils;
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
    private final TenantRepository tenantRepository;
    private final QrcodeTokenRepository qrcodeTokenRepository;

    private final Long expiresIn = 1800L; // 30 minutos para access token expirar
    private final Long refreshExpiresIn = 2592000L; // Expiração do refresh token (30 dias)

    private String accessToken;
    private String refreshToken;

    public TokenService(UserService userService,
                        JwtEncoder jwtEncoder,
                        UserRepository userRepository,
                        BCryptPasswordEncoder passwordEncoder,
                        RefreshTokenRepository refreshTokenRepository,
                        StockistRepository stockistRepository,
                        RoleRepository roleRepository,
                        TeamQueryRepository teamQueryRepository,
                        TenantRepository tenantRepository, QrcodeTokenRepository qrcodeTokenRepository) {
        this.userService = userService;
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.stockistRepository = stockistRepository;
        this.roleRepository = roleRepository;
        this.teamQueryRepository = teamQueryRepository;
        this.tenantRepository = tenantRepository;
        this.qrcodeTokenRepository = qrcodeTokenRepository;
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

    private void generateToken(UUID userId) {
        var now = Instant.now();
        var user = userRepository.findByUserId(userId).orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));
        var scope = getRoles(user.getUserId());

        var bucket = tenantRepository.findById(user.tenantId).orElseThrow().getBucket();

        // Cria novo token de acesso com as mesmas informações do refresh token
        var accessTokenClaims = getTokenClaims(
                user,
                now,
                expiresIn,
                scope,
                bucket
        );

        var refreshTokenClaims = getTokenClaims(
                user,
                now,
                refreshExpiresIn,
                scope,
                bucket
        );

        accessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessTokenClaims)).getTokenValue();
        refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(refreshTokenClaims)).getTokenValue();

        var newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(refreshToken);
        newRefreshToken.setExpiryDate(now.plusSeconds(refreshExpiresIn));
        newRefreshToken.setUser(user.getUserId());
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);
    }

    // -> depreciated
    public ResponseEntity<?> refreshToken(String refreshToken, HttpServletResponse response, boolean isMobile) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty() ||
                tokenFromDb.get().isRevoked() ||
                tokenFromDb.get().getExpiryDate()
                        .isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshToken existingToken = tokenFromDb.get();
        UUID userId = existingToken.getUser();
        refreshTokenRepository.deleteById(existingToken.getIdToken());

        this.generateToken(userId);

        // Configura o refreshToken como um cookie HTTP-Only
        if (!isMobile) {
            // Spring não suporta SameSite diretamente via Cookie API — definimos manualmente o header:
            String cookieValue = "refreshToken=" + this.refreshToken +
                    "; Max-Age=" + refreshExpiresIn +
                    "; Path=/" +
                    "; HttpOnly; Secure; SameSite=Strict";
            response.setHeader(HttpHeaders.SET_COOKIE, cookieValue);

            // Não enviar refresh token no body para web
            return ResponseEntity.ok(new LoginResponse(accessToken));
        } else {
            return ResponseEntity.ok(new LoginResponseMobile(accessToken, 1800L, getRoles(userId), "", refreshToken, ""));
        }
    }

    public ResponseEntity<?> newRefreshToken(String refreshToken, HttpServletResponse response, boolean isMobile) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty() ||
                tokenFromDb.get().isRevoked() ||
                tokenFromDb.get().getExpiryDate()
                        .isBefore(Instant.now())
        ) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RefreshToken existingToken = tokenFromDb.get();
        UUID userId = existingToken.getUser();
        refreshTokenRepository.deleteById(existingToken.getIdToken());

        this.generateToken(userId);

        // Configura o refreshToken como um cookie HTTP-Only
        if (!isMobile) {
            // Spring não suporta SameSite diretamente via Cookie API — definimos manualmente o header:
            String cookieValue = "refreshToken=" + this.refreshToken +
                    "; Max-Age=" + this.refreshExpiresIn +
                    "; Path=/" +
                    "; HttpOnly; Secure; SameSite=Strict";
            response.setHeader(HttpHeaders.SET_COOKIE, cookieValue);

            // Não enviar refresh token no body para web
            return ResponseEntity.ok(new LoginResponse(accessToken));
        } else {
            return ResponseEntity.ok(new NewLoginResponseMobile(accessToken, this.refreshToken));
        }
    }

    // -> depreciated
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


        this.generateToken(user.get().getUserId());

        // Configura o refreshToken como um cookie HTTP-Only
        if (!isMobile) {
            String cookieValue = "refreshToken=" + refreshToken +
                    "; Max-Age=" + refreshExpiresIn +
                    "; Path=/" +
                    "; HttpOnly; Secure; SameSite=Strict";
            response.setHeader(HttpHeaders.SET_COOKIE, cookieValue);

            return ResponseEntity.ok(new LoginResponse(accessToken));
        } else {
            var responseBody = new LoginResponseMobile(accessToken, expiresIn, getRoles(user.get().getUserId()), getTeams(user.get()), refreshToken, user.get().getUserId().toString());

            return ResponseEntity.ok(responseBody);
        }

    }

    public ResponseEntity<?> newLogin(LoginRequest loginRequest, HttpServletResponse response, boolean isMobile) {
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

        this.generateToken(user.get().getUserId());

        // Configura o refreshToken como um cookie HTTP-Only
        if (!isMobile) {
            String cookieValue = "refreshToken=" + refreshToken +
                    "; Max-Age=" + refreshExpiresIn +
                    "; Path=/" +
                    "; HttpOnly; Secure; SameSite=Strict";
            response.setHeader(HttpHeaders.SET_COOKIE, cookieValue);

            return ResponseEntity.ok(new LoginResponse(accessToken));
        } else {
            var responseBody = new NewLoginResponseMobile(accessToken, refreshToken);
            return ResponseEntity.ok(responseBody);
        }
    }

    public ResponseEntity<Void> logout(String refreshToken, HttpServletResponse response, boolean isMobile) {
        refreshTokenRepository.deleteByToken(refreshToken);

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

    private String getRoles(UUID userId) {
        var rolesNames = roleRepository.findRolesByUserId(userId);
        return String.join(" ", rolesNames);
    }

    private String getTeams(AppUser appUser) {
        Long numberTeamId = teamQueryRepository.getTeamIdByUserId(appUser.getUserId());
        String teamId = "";
        if (numberTeamId != null) {
            teamId = numberTeamId.toString();
        }

        // Obter códigos dos stockists diretamente e juntar com espaço
        var sTeams = stockistRepository.findAllByUserId(appUser.getUserId()).stream()
                .map(s -> s.getNotificationCode().toString())
                .collect(Collectors.joining(" "));

        // Junta teamId e sTeams, ignorando valores nulos ou vazios
        return Stream.of(teamId, sTeams)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));
    }

    private JwtClaimsSet getTokenClaims(AppUser appUser, Instant now, Long expiresIn, String scope, String bucket) {
        return JwtClaimsSet.builder()
                .issuer("LumosSoftware")
                .subject(appUser.getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scope)
                .claim("jti", UUID.randomUUID().toString())
                .claim("username", appUser.getUsername())
                .claim("email", appUser.getEmail())
                .claim("fullname", appUser.getCompletedName())
                .claim("tenant", appUser.getTenantId())
                .claim("bucket", bucket)
                .build();

    }


    public ResponseEntity<?> getQrcodeToken() {
        record qrcodeResponse(UUID token, Long expiresIn) { }
        var now = Instant.now();
        var expiresIn = 90L;
        var token = UUID.randomUUID();
        var userId = Utils.INSTANCE.getCurrentUserId();

        qrcodeTokenRepository.deleteAllByUserId(userId);

        var qrcodeToken = new QrcodeToken();
        qrcodeToken.setToken(token);
        qrcodeToken.setUserId(userId);
        qrcodeToken.setExpiresAt(now.plusSeconds(expiresIn));
        qrcodeToken.setNewEntry(true);
        qrcodeTokenRepository.save(qrcodeToken);

        return ResponseEntity.ok(new qrcodeResponse(token, expiresIn));
    }

    public ResponseEntity<?> loginWithQrCodeToken(UUID token, HttpServletResponse response, boolean isMobile) {
        var qrcodeToken = qrcodeTokenRepository.findById(token)
                .orElseThrow(() -> new Utils.BusinessException("Token não encontrado"));

        if (qrcodeToken.getExpiresAt().isBefore(Instant.now())) {
            throw new Utils.BusinessException("Token expirado, gere outro token.");
        }

        this.generateToken(qrcodeToken.getUserId());

        // Configura o refreshToken como um cookie HTTP-Only
        if (!isMobile) {
            String cookieValue = "refreshToken=" + refreshToken +
                    "; Max-Age=" + refreshExpiresIn +
                    "; Path=/" +
                    "; HttpOnly; Secure; SameSite=Strict";
            response.setHeader(HttpHeaders.SET_COOKIE, cookieValue);

            return ResponseEntity.ok(new LoginResponse(accessToken));
        } else {
            var responseBody = new NewLoginResponseMobile(accessToken, refreshToken);

            return ResponseEntity.ok(responseBody);
        }
    }
}