package com.lumos.lumosspring.authentication.controller;

import com.lumos.lumosspring.authentication.entities.RefreshToken;
import com.lumos.lumosspring.authentication.dto.LoginRequest;
import com.lumos.lumosspring.authentication.dto.LoginResponse;
import com.lumos.lumosspring.authentication.dto.LoginResponseMobile;
import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
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

    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TeamRepository teamRepository;
    private final Util util;
    private final RoleRepository roleRepository;

    public TokenControllerMobile(JwtEncoder jwtEncoder, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, @Qualifier("jwtDecoder") JwtDecoder jwtDecoder, RefreshTokenRepository refreshTokenRepository, TeamRepository teamRepository, Util util, RoleRepository roleRepository) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.teamRepository = teamRepository;
        this.util = util;
        this.roleRepository = roleRepository;
    }


    @PostMapping("/login")
    public ResponseEntity<?> loginMobile(@RequestBody LoginRequest loginRequest) {
        var user = userRepository.findByUsernameOrCpfIgnoreCase(loginRequest.username(), loginRequest.email());
        if (user.isEmpty() || !user.get().isLoginCorrect(loginRequest, passwordEncoder)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Usuário ou senha incorretos"));
        }

        if (!user.get().getStatus()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("O Usuário informado está inativo no sistema."));
        }

        var allowedRoles = new HashSet<>(Set.of(Role.Values.OPERADOR.name(), Role.Values.ADMIN.name(), Role.Values.RESPONSAVEL_TECNICO.name()));

        var roles = user.get().getRoles();
        boolean hasAccess = roles.stream().anyMatch(roleName -> allowedRoles.contains(roleName.getRoleName()));

        if (!hasAccess) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Usuário sem acesso ao aplicativo"));
        }

        var now = util.getDateTime();
        var expiresIn = 1800L; // 30 minutos
        var refreshExpiresIn = 15552000L; // 6 meses

        var scopes = user.get().getRoles()
                .stream()
                .map(Role::getRoleName)
                .collect(Collectors.joining(" "));

        var eTeams = Optional.ofNullable(user.get().getElectricians())
                .orElse(Collections.emptyList())
                .stream()
                .map(Team::getTeamCode)
                .collect(Collectors.joining(" "));

        var dTeams = Optional.ofNullable(user.get().getDrivers())
                .orElse(Collections.emptyList())
                .stream()
                .map(Team::getTeamCode)
                .collect(Collectors.joining(" "));

        scopes = scopes + (eTeams.isEmpty() ? "" : " " + eTeams)
                + (dTeams.isEmpty() ? "" : " " + dTeams);


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

        // Ajuste: Retorna o refreshToken no corpo em vez de cookie
        var responseBody = new LoginResponseMobile(accessTokenValue, expiresIn, scopes, refreshTokenValue, user.get().getIdUser().toString());

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String refreshToken) {
        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isPresent()) {
            var token = tokenFromDb.get();
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        var now = util.getDateTime();
        var expiresIn = 1800L; // 30 minutos para access token expirar

        var tokenFromDb = refreshTokenRepository.findByToken(refreshToken);
        if (tokenFromDb.isEmpty() || tokenFromDb.get().isRevoked() || tokenFromDb.get().getExpiryDate().isBefore(now)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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

}
