package com.lumos.lumosspring.user.service;

import com.lumos.lumosspring.authentication.repository.RefreshTokenRepository;
import com.lumos.lumosspring.notifications.service.EmailService;
import com.lumos.lumosspring.user.dto.PasswordDTO;
import com.lumos.lumosspring.user.dto.UpdateUserDto;
import com.lumos.lumosspring.user.dto.UserResponse;
import com.lumos.lumosspring.team.repository.TeamRepository;
import com.lumos.lumosspring.user.model.AppUser;
import com.lumos.lumosspring.user.model.Role;
import com.lumos.lumosspring.user.repository.RoleRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import com.lumos.lumosspring.util.DefaultResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import com.lumos.lumosspring.util.Utils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.lumos.lumosspring.user.dto.OperationalAndTeamsResponse;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final TeamRepository teamRepository;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       EmailService emailService,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       NamedParameterJdbcTemplate namedParameterJdbcTemplate, TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.teamRepository = teamRepository;
    }

    @Cacheable("getAllUsers")
    public ResponseEntity<List<UserResponse>> findAll() {
        List<AppUser> appUsers = userRepository.findByTenantIdAndStatusTrueAndSupportFalseOrderByNameAsc(
                Utils.INSTANCE.getCurrentTenantId(),
                true,
                false
        );
        List<UserResponse> userResponses = new ArrayList<>();
        for (AppUser appUser : appUsers) {
            if (appUser.getStatus()) {
                LocalDate dateOfBirth = appUser.getDateOfBirth();
                var roles = roleRepository.findRolesByUserId(appUser.getUserId());

                userResponses.add(new UserResponse(
                        appUser.getUserId().toString(),
                        appUser.getUsername(),
                        appUser.getName(),
                        appUser.getLastName(),
                        appUser.getEmail(),
                        maskCpf(appUser.getCpf()),
                        roles,
                        dateOfBirth != null ? dateOfBirth.getYear() : null,
                        dateOfBirth != null ? dateOfBirth.getMonth().getValue() : null,
                        dateOfBirth != null ? dateOfBirth.getDayOfMonth() : null,
                        appUser.getStatus()
                ));
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(userResponses);
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return cpf; // retorna como está se for inválido
        }
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }


    @Cacheable("getUserByUUID")
    public ResponseEntity<UserResponse> find(String uuid) {
        var user = userRepository.findByUserId(UUID.fromString(uuid));
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LocalDate dateOfBirth = user.get().getDateOfBirth();
        var roles = roleRepository.findRolesByUserId(user.get().getUserId());
        UserResponse userResponse = new UserResponse(
                user.get().getUserId().toString(),
                user.get().getUsername(),
                user.get().getName(),
                user.get().getLastName(),
                user.get().getEmail(),
                user.get().getCpf(),
                roles,
                dateOfBirth != null ? dateOfBirth.getYear() : null,
                dateOfBirth != null ? dateOfBirth.getMonth().getValue() : null,
                dateOfBirth != null ? dateOfBirth.getDayOfMonth() : null,
                user.get().getStatus()
        );


        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    public Optional<AppUser> findUserByUsernameOrCpf(String username) {
        return userRepository.findByUsernameOrCpfIgnoreCase(username, username);
    }

    public ResponseEntity<?> resetPassword(String userId) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) { // tamanho da senha
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String newPassword = sb.toString();

        user.get().setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user.get());


        return ResponseEntity.ok(new DefaultResponse(newPassword));
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

    private ResponseEntity<?> confirmCode(String userId, String code) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        if (!code.equals(user.get().getCodeResetPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Código informado é inválido"));
        }

        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> recoveryPassword(String userId, String code, String newPassword) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        if (!code.equals(user.get().getCodeResetPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Erro ao tentar atualizar a senha, tente novamente."));
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.get().setPassword(newPasswordHash);
        userRepository.save(user.get());

        return ResponseEntity.ok().body(new DefaultResponse("Senha atualizada com sucesso"));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "getAllUsers", allEntries = true),
            @CacheEvict(cacheNames = "getUserByUUID", allEntries = true)
    })
    @Transactional
    public ResponseEntity<?> updateUsers(List<UpdateUserDto> dto) {
        boolean hasInvalidUser = dto.stream().noneMatch(UpdateUserDto::sel);
        String regex = "^(?!.*\\.\\.)(?!.*\\.@)(?!.*@\\.)(?!.*@example)(?!.*@teste)(?!.*@email)\\b[A-Za-z0-9][A-Za-z0-9._%+-]{0,63}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}\\b$";
        Pattern pattern = Pattern.compile(regex);

        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Nenhum usuário selecionado foi enviado."));
        }


        for (UpdateUserDto u : dto) {
            if (!u.sel()) {
                continue;
            }

            if (u.userId() == null || u.userId().isEmpty() || u.userId().isBlank()) {
                this.insertUser(u);
                continue;
            }

            var user = userRepository.findByUserId(UUID.fromString(u.userId()))
                    .orElseThrow(() -> new Utils.BusinessException(
                            "O usuário %s não foi encontrado no sistema.".formatted(u.name()))
                    );

            if (!pattern.matcher(u.email()).matches()) {
                throw new Utils.BusinessException(STR."ERRO: Email \{u.email()} é inválido'.");
            }

            if (!isValidCPF(u.cpf())) {
                throw new Utils.BusinessException(STR."ERRO: CPF \{u.cpf()} é inválido.");
            }

            // Verifica se o username já existe no sistema
            Optional<AppUser> userOptional = userRepository.findByUsernameIgnoreCase(u.username());
            if (userOptional.isPresent() && !userOptional.get().getUserId().equals(UUID.fromString(u.userId()))) {
                throw new Utils.BusinessException(String.format("Username %s já existente no sistema.", u.username()));
            }

            // Verifica se o e-mail já existe no banco de dados
            userOptional = userRepository.findByCpfIgnoreCase(u.cpf());
            if (userOptional.isPresent() && !userOptional.get().getUserId().equals(UUID.fromString(u.userId()))) {
                throw new Utils.BusinessException(String.format("CPF %s já existente no sistema.", u.cpf()));
            }


            Set<Role> userRoles = new HashSet<>();
            var date = LocalDate.of(u.year(), u.month(), u.day());

            for (String r : u.role()) {
                if (r.isEmpty()) {
                    continue;
                }

                var role = roleRepository.findByRoleName(r);
                userRoles.add(role);
            }

            var oldRoleNames = roleRepository.findRolesByUserId(user.getUserId());

            List<String> newRoleNames = new ArrayList<>(u.role());

            if (!u.status() || !oldRoleNames.equals(newRoleNames)) {
                refreshTokenRepository.findByAppUser(user.getUserId())
                        .ifPresent(tokens -> tokens.forEach(token -> {
                            if (token.getExpiryDate().isBefore(Instant.now())) {
                                var params = new MapSqlParameterSource()
                                        .addValue("id_token", token.getIdToken());

                                namedParameterJdbcTemplate.update("""
                                            delete from refresh_token where id_token = :id_token
                                        """, params);
                            }
                        }));
            }

            user.setUsername(u.username());
            user.setName(u.name());
            user.setLastName(u.lastname());
            user.setEmail(u.email());
            if (!u.cpf().startsWith("***") && !u.cpf().endsWith("**")) {
                user.setCpf(u.cpf());
            }
            user.setDateOfBirth(date);
            user.setStatus(u.status());

            for (Role role : userRoles) {
                var params = new MapSqlParameterSource()
                        .addValue("userId", UUID.fromString(u.userId()))
                        .addValue("roleId", role.getRoleId());

                namedParameterJdbcTemplate.update("""
                            delete from user_role where id_user = :userId
                        """, params);

                namedParameterJdbcTemplate.update("""
                            INSERT INTO user_role (id_user, id_role)
                            VALUES (:userId, :roleId)
                        """, params);
            }


            userRepository.save(user);

        }

        return this.findAll();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "getAllUsers", allEntries = true),
            @CacheEvict(cacheNames = "getUserByUUID", allEntries = true)
    })
    public void insertUser(UpdateUserDto u) {
        String regex = "^(?!.*\\.\\.)(?!.*\\.@)(?!.*@\\.)(?!.*@example)(?!.*@teste)(?!.*@email)\\b[A-Za-z0-9][A-Za-z0-9._%+-]{0,63}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}\\b$";
        Pattern pattern = Pattern.compile(regex);


        if (!u.userId().isEmpty()) {
            return;
        }

        if (u.username().contains(" ")) {
            throw new Utils.BusinessException(STR."ERRO: Username \{u.username()} foi enviado com espaços.");
        }

        if (!pattern.matcher(u.email()).matches()) {
            throw new Utils.BusinessException(STR."ERRO: Email \{u.email()} é inválido'.");
        }

        if (!isValidCPF(u.cpf())) {
            throw new Utils.BusinessException(STR."ERRO: CPF \{u.cpf()} é inválido.");
        }


        if (userRepository.findByUsernameIgnoreCase(u.username()).isPresent()) {
            throw new Utils.BusinessException(STR."Username \{u.username()} já existente no sistema, recupere a senha ou utilize outro username.");
        }

        if (userRepository.findByCpfIgnoreCase(u.cpf()).isPresent()) {
            throw new Utils.BusinessException(STR."CPF \{u.email()} já existente no sistema, recupere a senha ou utilize outro CPF.");
        }

        Set<Role> userRoles = new HashSet<>();
        var user = new AppUser();
        var date = LocalDate.of(u.year(), u.month(), u.day());
        var password = UUID.randomUUID().toString();

        for (String r : u.role()) {
            if (r.isEmpty()) {
                continue;
            }

            var role = roleRepository.findByRoleName(r);
            if (role == null) {
                continue;
            }
            userRoles.add(role);
        }

        user.setUserId(UUID.randomUUID());
        user.setNewEntry(true);
        user.setUsername(u.username());
        user.setPassword(passwordEncoder.encode(password));
        user.setName(u.name());
        user.setLastName(u.lastname());
        user.setEmail(u.email());
        user.setCpf(u.cpf());
        user.setDateOfBirth(date);
        user.setStatus(u.status());
        user.setSupport(false);
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

        try {
            emailService.sendPasswordForEmail(u.name(), u.email(), password);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public ResponseEntity<?> setPassword(String userId, PasswordDTO dto) {
        var user = userRepository.findByUserId(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado."));
        }

        if (!dto.password().equals(dto.passwordConfirm())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("As senhas não conferem."));
        }

        String newPasswordHash = passwordEncoder.encode(dto.password());
        user.get().setPassword(newPasswordHash);
        userRepository.save(user.get());

        return ResponseEntity.ok().body(new DefaultResponse("Senha atualizada com sucesso"));
    }

    public static boolean isValidCPF(String cpf) {
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
        var operationalUsers = userRepository.getOperationalUsers();
        var teams = teamRepository.getTeams(Utils.INSTANCE.getCurrentTenantId());

        return ResponseEntity.ok().body(
                new OperationalAndTeamsResponse(
                        operationalUsers,
                        teams
                )
        );
    }
}
