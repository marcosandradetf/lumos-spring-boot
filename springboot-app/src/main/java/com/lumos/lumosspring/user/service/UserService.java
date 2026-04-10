package com.lumos.lumosspring.user.service;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.notifications.service.EmailService;
import com.lumos.lumosspring.scheduler.AsyncTenantContext;
import com.lumos.lumosspring.team.repository.TeamRepository;
import com.lumos.lumosspring.user.dto.ActivationCodeResponse;
import com.lumos.lumosspring.user.dto.OperationalAndTeamsResponse;
import com.lumos.lumosspring.user.dto.PasswordDTO;
import com.lumos.lumosspring.user.dto.UpdateUserDto;
import com.lumos.lumosspring.user.dto.UserResponse;
import com.lumos.lumosspring.user.model.AppUser;
import com.lumos.lumosspring.user.model.Role;
import com.lumos.lumosspring.user.model.UserStatus;
import com.lumos.lumosspring.user.repository.RoleRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.util.DefaultResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class UserService {
    private static final String EMAIL_REGEX = "^(?!.*\\.\\.)(?!.*\\.@)(?!.*@\\.)(?!.*@example)(?!.*@teste)(?!.*@email)\\b[A-Za-z0-9][A-Za-z0-9._%+-]{0,63}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}\\b$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final String ACTIVATION_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final TeamRepository teamRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int activationCodeLength;
    private final int activationExpirationMinutes;
    private final int activationMaxAttempts;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       EmailService emailService,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                       TeamRepository teamRepository,
                       @Value("${lumos.auth.activation.code-length:8}") int activationCodeLength,
                       @Value("${lumos.auth.activation.expiration-minutes:15}") int activationExpirationMinutes,
                       @Value("${lumos.auth.activation.max-attempts:5}") int activationMaxAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.teamRepository = teamRepository;
        this.activationCodeLength = activationCodeLength;
        this.activationExpirationMinutes = activationExpirationMinutes;
        this.activationMaxAttempts = activationMaxAttempts;
    }

    @Cacheable(
            value = "getAllUsers",
            key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"
    )
    public ResponseEntity<List<UserResponse>> findAll() {
        List<AppUser> appUsers = userRepository.findByTenantIdAndSupportFalseOrderByNameAsc(
                Utils.getCurrentTenantId(),
                false
        );

        List<UserResponse> userResponses = appUsers.stream()
                .map(this::toUserResponse)
                .toList();

        return ResponseEntity.status(HttpStatus.OK).body(userResponses);
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return cpf;
        }
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }

    @Cacheable(
            value = "getUserByUUID",
            key = "T(com.lumos.lumosspring.util.Utils).getCurrentTenantId()"
    )
    public ResponseEntity<UserResponse> find(String uuid) {
        var user = userRepository.findByUserId(UUID.fromString(uuid));
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(toUserResponse(user.get(), false));
    }

    public Optional<AppUser> findUserByUsernameOrCpf(String username) {
        return userRepository.findByUsernameOrCpfIgnoreCase(username, username);
    }

    public ResponseEntity<?> generateActivationCode(String userId) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        var response = refreshActivationCode(user.get(), "Código de ativação gerado com sucesso.");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> resetActivation(String userId) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        user.get().setStatus(UserStatus.PENDING_ACTIVATION);
        revokeUserSessions(user.get().getUserId());
        var response = refreshActivationCode(user.get(), "Ativação redefinida com sucesso.");
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<?> changePassword(String userId, String oldPassword, String newPassword) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        if (!passwordEncoder.matches(oldPassword, user.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Senha incorreta"));
        }

        user.get().setPassword(passwordEncoder.encode(newPassword));
        user.get().setMustChangePassword(false);
        userRepository.save(user.get());

        return ResponseEntity.ok(new DefaultResponse("Senha alterada com sucesso"));
    }

    public ResponseEntity<?> forgotPassword(String userId, String email) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        if (!email.equals(user.get().getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Email incorreto ou não cadastrado, comunique ao administrador do sistema!"));
        }

        String code = UUID.randomUUID().toString();
        user.get().setCodeResetPassword(passwordEncoder.encode(code));
        userRepository.save(user.get());

        emailService.sendEmail(user.get().getEmail(),
                "Sistema Lumos - Código de verificaçao",
                STR."Olá \{user.get().getName()}<br>Seu código de verificação é: \{code}<br>Use-o para definir uma nova senha.");

        return ResponseEntity.ok(new DefaultResponse("Código de recuperação enviado com sucesso! Verifique seu e-mail."));
    }

    public ResponseEntity<?> forgotPasswordByUsername(String usernameOrCpf) {
        var user = findUserByUsernameOrCpf(usernameOrCpf);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        String code = UUID.randomUUID().toString();
        user.get().setCodeResetPassword(passwordEncoder.encode(code));
        userRepository.save(user.get());

        emailService.sendEmail(user.get().getEmail(),
                "Sistema Lumos - Código de recuperação",
                STR."Olá \{user.get().getName()}<br>Seu código de recuperação é: \{code}<br>Use-o para redefinir sua senha.");

        return ResponseEntity.ok(new DefaultResponse("Código de recuperação enviado com sucesso! Verifique seu e-mail."));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "getAllUsers", allEntries = true),
            @CacheEvict(cacheNames = "getUserByUUID", allEntries = true)
    })
    @Transactional
    public ResponseEntity<?> updateUsers(List<UpdateUserDto> dto) {
        boolean hasInvalidUser = dto.stream().noneMatch(UpdateUserDto::sel);
        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Nenhum usuário selecionado foi enviado."));
        }

        for (UpdateUserDto userDto : dto) {
            if (!userDto.sel()) {
                continue;
            }

            if (userDto.userId() == null || userDto.userId().isBlank()) {
                insertUser(userDto);
                continue;
            }

            updateExistingUser(userDto);
        }

        return this.findAll();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "getAllUsers", allEntries = true),
            @CacheEvict(cacheNames = "getUserByUUID", allEntries = true)
    })
    public void insertUser(UpdateUserDto userDto) {
        validateUserPayload(userDto, false);

        if (userRepository.findByUsernameIgnoreCase(userDto.username()).isPresent()) {
            throw new Utils.BusinessException(STR."Username \{userDto.username()} já existente no sistema, utilize outro username.");
        }

        if (userRepository.findByCpfIgnoreCase(userDto.cpf()).isPresent()) {
            throw new Utils.BusinessException(STR."CPF \{userDto.cpf()} já existente no sistema, utilize outro CPF.");
        }

        Set<Role> userRoles = new HashSet<>(userDto.role());
        var user = new AppUser();
        var date = LocalDate.of(userDto.year(), userDto.month(), userDto.day());

        user.setUserId(UUID.randomUUID());
        user.setNewEntry(true);
        user.setUsername(userDto.username());
        user.setPassword(null);
        user.setName(userDto.name());
        user.setLastName(userDto.lastname());
        user.setEmail(userDto.email());
        user.setCpf(userDto.cpf().replaceAll("\\D", ""));
        user.setDateOfBirth(date);
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        user.setMustChangePassword(false);
        user.setSupport(false);
        user.setActivationAttemptCount(0);
        user = userRepository.save(user);

        for (Role role : userRoles) {
            var params = new MapSqlParameterSource()
                    .addValue("userId", user.getId())
                    .addValue("roleId", role.getRoleId());
            namedParameterJdbcTemplate.update("""
                        INSERT INTO user_role (id_user, id_role)
                        VALUES (:userId, :roleId)
                    """, params);
        }

        refreshActivationCode(user, "Usuário criado com ativação pendente.");
    }

    public ResponseEntity<?> setPassword(String userId, PasswordDTO dto) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado."));
        }

        if (!dto.password().equals(dto.passwordConfirm())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("As senhas não conferem."));
        }

        String newPasswordHash = passwordEncoder.encode(dto.password());
        user.get().setPassword(newPasswordHash);
        user.get().setMustChangePassword(false);
        userRepository.save(user.get());

        return ResponseEntity.ok().body(new DefaultResponse("Senha atualizada com sucesso"));
    }

    @Transactional
    public ResponseEntity<?> activateUser(String cpf, String activationCode, String newPassword) {
        var normalizedCpf = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (!isValidCPF(normalizedCpf)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("CPF inválido."));
        }

        if (activationCode == null || activationCode.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Código de ativação obrigatório."));
        }

        if (newPassword == null || newPassword.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("A nova senha deve ter pelo menos 8 caracteres."));
        }

        var userOptional = userRepository.findByCpf(normalizedCpf);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado para o CPF informado."));
        }

        var user = userOptional.get();
        if (user.getStatus() == UserStatus.BLOCKED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Usuário bloqueado. Solicite um novo código ao administrador."));
        }

        if (user.getStatus() == UserStatus.ACTIVE && Boolean.FALSE.equals(user.getMustChangePassword())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Este usuário já foi ativado."));
        }

        if (user.getActivationAttemptCount() != null && user.getActivationAttemptCount() >= activationMaxAttempts) {
            user.setStatus(UserStatus.BLOCKED);
            userRepository.save(user);
            revokeUserSessions(user.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Limite de tentativas excedido. Solicite um novo código ao administrador."));
        }

        if (user.getActivationCodeHash() == null || user.getActivationCodeExpiresAt() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Usuário sem código de ativação ativo."));
        }

        if (user.getActivationCodeExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Código de ativação expirado."));
        }

        if (!passwordEncoder.matches(activationCode.trim(), user.getActivationCodeHash())) {
            user.setActivationAttemptCount((user.getActivationAttemptCount() == null ? 0 : user.getActivationAttemptCount()) + 1);
            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Código de ativação inválido."));
        }


        AsyncTenantContext.INSTANCE.setTenant(user.tenantId);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(false);
        user.setActivationCodeHash(null);
        user.setActivationCodeExpiresAt(null);
        user.setActivationAttemptCount(0);
        userRepository.save(user);

        AsyncTenantContext.INSTANCE.clear();

        return ResponseEntity.ok(new DefaultResponse("Usuário ativado com sucesso."));
    }

    public static boolean isValidCPF(String cpf) {
        if (cpf == null) {
            return false;
        }

        if (cpf.startsWith("***") && cpf.endsWith("**")) {
            return true;
        }

        cpf = cpf.replaceAll("\\D", "");

        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) return false;

        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) soma += (cpf.charAt(i) - '0') * (10 - i);
            int dig1 = soma % 11 < 2 ? 0 : 11 - (soma % 11);

            soma = 0;
            for (int i = 0; i < 10; i++) soma += (cpf.charAt(i) - '0') * (11 - i);
            int dig2 = soma % 11 < 2 ? 0 : 11 - (soma % 11);

            return dig1 == (cpf.charAt(9) - '0') && dig2 == (cpf.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    public ResponseEntity<?> getOperationalUsers() {
        var operationalUsers = userRepository.getOperationalUsers(Utils.getCurrentTenantId());
        var teams = teamRepository.getTeams(Utils.getCurrentTenantId());

        return ResponseEntity.ok().body(
                new OperationalAndTeamsResponse(
                        operationalUsers,
                        teams
                )
        );
    }

    private void updateExistingUser(UpdateUserDto userDto) {
        validateUserPayload(userDto, true);

        var user = userRepository.findByUserId(UUID.fromString(userDto.userId()))
                .orElseThrow(() -> new Utils.BusinessException(
                        "O usuário %s não foi encontrado no sistema.".formatted(userDto.name()))
                );

        Optional<AppUser> userOptional = userRepository.findByUsernameIgnoreCase(userDto.username());
        if (userOptional.isPresent() && !userOptional.get().getUserId().equals(UUID.fromString(userDto.userId()))) {
            throw new Utils.BusinessException(String.format("Username %s já existente no sistema.", userDto.username()));
        }

        var normalizedCpf = userDto.cpf().startsWith("***") ? user.getCpf() : userDto.cpf().replaceAll("\\D", "");
        userOptional = userRepository.findByCpfIgnoreCase(normalizedCpf);
        if (userOptional.isPresent() && !userOptional.get().getUserId().equals(UUID.fromString(userDto.userId()))) {
            throw new Utils.BusinessException(String.format("CPF %s já existente no sistema.", userDto.cpf()));
        }

        var date = LocalDate.of(userDto.year(), userDto.month(), userDto.day());
        Set<Role> currentUserRoles = new HashSet<>(roleRepository.findRolesByUserId(user.getUserId()));
        Set<Role> newRoles = new HashSet<>(userDto.role());

        if (userDto.status() != UserStatus.ACTIVE || !currentUserRoles.equals(newRoles)) {
            revokeUserSessions(user.getUserId());
        }

        user.setUsername(userDto.username());
        user.setName(userDto.name());
        user.setLastName(userDto.lastname());
        user.setEmail(userDto.email());
        if (!userDto.cpf().startsWith("***") && !userDto.cpf().endsWith("**")) {
            user.setCpf(normalizedCpf);
        }
        user.setDateOfBirth(date);
        user.setStatus(userDto.status());
        if (userDto.status() == UserStatus.BLOCKED) {
            user.setActivationCodeHash(null);
            user.setActivationCodeExpiresAt(null);
        }

        namedParameterJdbcTemplate.update("""
                    delete from user_role where id_user = :userId
                """, Map.of("userId", UUID.fromString(userDto.userId())));
        for (Role role : newRoles) {
            var params = new MapSqlParameterSource()
                    .addValue("userId", UUID.fromString(userDto.userId()))
                    .addValue("roleId", role.getRoleId());

            namedParameterJdbcTemplate.update("""
                        INSERT INTO user_role (id_user, id_role)
                        VALUES (:userId, :roleId)
                    """, params);
        }

        userRepository.save(user);
    }

    private void validateUserPayload(UpdateUserDto userDto, boolean existingUser) {
        if (!existingUser && (userDto.userId() != null && !userDto.userId().isEmpty())) {
            return;
        }

        if (userDto.username().contains(" ")) {
            throw new Utils.BusinessException(STR."ERRO: Username \{userDto.username()} foi enviado com espaços.");
        }

        if (!EMAIL_PATTERN.matcher(userDto.email()).matches()) {
            throw new Utils.BusinessException(STR."ERRO: Email \{userDto.email()} é inválido.");
        }

        if (!isValidCPF(userDto.cpf())) {
            throw new Utils.BusinessException(STR."ERRO: CPF \{userDto.cpf()} é inválido.");
        }
    }

    private UserResponse toUserResponse(AppUser appUser) {
        return toUserResponse(appUser, true);
    }

    private UserResponse toUserResponse(AppUser appUser, boolean maskCpf) {
        LocalDate dateOfBirth = appUser.getDateOfBirth();
        var roles = roleRepository.findRolesByUserId(appUser.getUserId());
        return new UserResponse(
                appUser.getUserId().toString(),
                appUser.getUsername(),
                appUser.getName(),
                appUser.getLastName(),
                appUser.getEmail(),
                maskCpf ? maskCpf(appUser.getCpf()) : appUser.getCpf(),
                roles,
                dateOfBirth != null ? dateOfBirth.getYear() : 0,
                dateOfBirth != null ? dateOfBirth.getMonth().getValue() : 0,
                dateOfBirth != null ? dateOfBirth.getDayOfMonth() : 0,
                appUser.getStatus(),
                Boolean.TRUE.equals(appUser.getMustChangePassword()),
                appUser.getActivationCodeExpiresAt()
        );
    }

    private ActivationCodeResponse refreshActivationCode(AppUser user, String message) {
        var code = generateActivationCodeValue();
        var expiresAt = Instant.now().plusSeconds(activationExpirationMinutes * 60L);

        user.setStatus(UserStatus.PENDING_ACTIVATION);
        user.setMustChangePassword(true);
        user.setActivationCodeHash(passwordEncoder.encode(code));
        user.setActivationCodeExpiresAt(expiresAt);
        user.setActivationAttemptCount(0);
        userRepository.save(user);

        return new ActivationCodeResponse(code, expiresAt, message);
    }

    private String generateActivationCodeValue() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < activationCodeLength; index++) {
            builder.append(ACTIVATION_CODE_CHARS.charAt(secureRandom.nextInt(ACTIVATION_CODE_CHARS.length())));
        }
        return builder.toString();
    }

    private void revokeUserSessions(UUID userId) {
        refreshTokenRepository.findByAppUser(userId)
                .ifPresent(tokens -> tokens.forEach(token -> {
                    var params = new MapSqlParameterSource()
                            .addValue("id_token", token.getIdToken());

                    namedParameterJdbcTemplate.update("""
                                delete from refresh_token where id_token = :id_token
                            """, params);
                }));
    }
}
