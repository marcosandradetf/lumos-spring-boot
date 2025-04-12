package com.lumos.lumosspring.user;

import com.lumos.lumosspring.authentication.RefreshTokenRepository;
import com.lumos.lumosspring.notification.EmailService;
import com.lumos.lumosspring.user.dto.CreateUserDto;
import com.lumos.lumosspring.user.dto.PasswordDTO;
import com.lumos.lumosspring.user.dto.UpdateUserDto;
import com.lumos.lumosspring.user.dto.UserResponse;
import com.lumos.lumosspring.util.DefaultResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, EmailService emailService, RoleRepository roleRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Cacheable("getAllUsers")
    public ResponseEntity<List<UserResponse>> findAll() {
        List<User> users = userRepository.findAll();
        List<UserResponse> userResponses = new ArrayList<>();
        for (User user : users) {
            if (user.getStatus()) {
                LocalDate dateOfBirth = user.getDateOfBirth();
                userResponses.add(new UserResponse(
                        user.getIdUser().toString(),
                        user.getUsername(),
                        user.getName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getRoles().stream()
                                .map(Role::getRoleName) // Pega apenas o nome de cada Role
                                .collect(Collectors.toList()), // Coleta como uma lista
                        dateOfBirth != null ? dateOfBirth.getYear() : null,
                        dateOfBirth != null ? dateOfBirth.getMonth().getValue() : null,
                        dateOfBirth != null ? dateOfBirth.getDayOfMonth() : null,
                        user.getStatus()
                ));
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(userResponses);
    }

    @Cacheable("getUserByUUID")
    public ResponseEntity<UserResponse> find(String uuid) {
        var user = userRepository.findByIdUser(UUID.fromString(uuid));
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LocalDate dateOfBirth = user.get().getDateOfBirth();
        UserResponse userResponse = new UserResponse(
                user.get().getIdUser().toString(),
                user.get().getUsername(),
                user.get().getName(),
                user.get().getLastName(),
                user.get().getEmail(),
                user.get().getRoles().stream()
                        .map(Role::getRoleName) // Pega apenas o nome de cada Role
                        .collect(Collectors.toList()), // Coleta como uma lista
                dateOfBirth != null ? dateOfBirth.getYear() : null,
                dateOfBirth != null ? dateOfBirth.getMonth().getValue() : null,
                dateOfBirth != null ? dateOfBirth.getDayOfMonth() : null,
                user.get().getStatus()
        );


        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    public ResponseEntity<?> resetPassword(String userId) {
        var user = userRepository.findByIdUser(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }
        String newPassword = UUID.randomUUID().toString();
        user.get().setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user.get());

        emailService.sendNewPasswordForEmail(user.get().getName(), user.get().getEmail(), newPassword);

        return ResponseEntity.ok(new DefaultResponse(STR."A nova senha foi enviada para o email \{user.get().getEmail()}"));
    }

    public ResponseEntity<?> changePassword(String userId, String oldPassword, String newPassword) {
        var user = userRepository.findByIdUser(UUID.fromString(userId));
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
        var user = userRepository.findByIdUser(UUID.fromString(userId));
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
        var user = userRepository.findByIdUser(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }

        if (!code.equals(user.get().getCodeResetPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Código informado é inválido"));
        }

        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> recoveryPassword(String userId, String code, String newPassword) {
        var user = userRepository.findByIdUser(UUID.fromString(userId));
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

    public ResponseEntity<?> updateUsers(List<UpdateUserDto> dto) {
        boolean hasInvalidUser = dto.stream().noneMatch(UpdateUserDto::sel);
        String regex = "^(?!.*\\.\\.)(?!.*\\.@)(?!.*@\\.)(?!.*@example)(?!.*@teste)(?!.*@email)\\b[A-Za-z0-9][A-Za-z0-9._%+-]{0,63}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}\\b$";
        Pattern pattern = Pattern.compile(regex);

        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Nenhum usuário selecionado foi enviado."));
        }

        hasInvalidUser = dto.stream().anyMatch(u -> u.userId() == null || u.userId().isEmpty());
        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Foi enviado um ou mais usuário sem identificação."));
        }

        for (UpdateUserDto u : dto) {
            if (!u.sel()) {
                continue;
            }
            var user = userRepository.findByIdUser(UUID.fromString(u.userId()));
            if (user.isEmpty()) {
                continue;
            }

            if (!pattern.matcher(u.email()).matches()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(STR."ERRO: Email \{u.email()} é inválido'."));
            }

            // Verifica se o username já existe no sistema
            Optional<User> userOptional = userRepository.findByUsernameIgnoreCase(u.username());
            if (userOptional.isPresent() && !userOptional.get().getIdUser().equals(UUID.fromString(u.userId()))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ErrorResponse(String.format("Username %s já existente no sistema.", u.username()))
                );
            }

            // Verifica se o e-mail já existe no banco de dados
            userOptional = userRepository.findByEmailIgnoreCase(u.email());
            if (userOptional.isPresent() && !userOptional.get().getIdUser().equals(UUID.fromString(u.userId()))) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        new ErrorResponse(String.format("Email %s já existente no sistema.", u.email()))
                );
            }


            Set<Role> userRoles = new HashSet<>();
            var date = LocalDate.of(u.year(), u.month(), u.day());

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

            Set<String> oldRoleNames = user.get().getRoles().stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toSet());

            Set<String> newRoleNames = new HashSet<>(u.role());

            if (!u.status() || !oldRoleNames.equals(newRoleNames)) {
                refreshTokenRepository.findByUser(user.get())
                        .ifPresent(tokens -> tokens.forEach(token -> {
                            token.setRevoked(true);
                            refreshTokenRepository.save(token);
                        }));
            }

            user.get().setUsername(u.username());
            user.get().setName(u.name());
            user.get().setLastName(u.lastname());
            user.get().setEmail(u.email());
            user.get().setDateOfBirth(date);
            user.get().setRoles(userRoles);
            user.get().setStatus(u.status());
            userRepository.save(user.get());

        }

        return this.findAll();
    }

    public ResponseEntity<?> insertUsers(List<CreateUserDto> dto) {
        var hasInvalidUser = dto.stream().noneMatch(u -> u.userId().isEmpty());
        String regex = "^(?!.*\\.\\.)(?!.*\\.@)(?!.*@\\.)(?!.*@example)(?!.*@teste)(?!.*@email)\\b[A-Za-z0-9][A-Za-z0-9._%+-]{0,63}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}\\b$";
        Pattern pattern = Pattern.compile(regex);

        if (hasInvalidUser) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Erro: Foi enviado apenas usuários já existentes no sistema!"));
        }

        for (CreateUserDto u : dto) {
            if (!u.userId().isEmpty()) {
                continue;
            }

            if (u.username().contains(" ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(STR."ERRO: Username \{u.username()} foi enviado com espaços."));
            }

            if (!pattern.matcher(u.email()).matches()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(STR."ERRO: Email \{u.email()} é inválido'."));
            }

            if (userRepository.findByUsernameIgnoreCase(u.username()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(STR."Username \{u.username()} já existente no sistema, recupere a senha ou utilize outro username."));
            }

            if (userRepository.findByEmailIgnoreCase(u.email()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(STR."Email \{u.email()} já existente no sistema, recupere a senha ou utilize outro email."));
            }

            Set<Role> userRoles = new HashSet<>();
            var user = new User();
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

            user.setUsername(u.username());
            user.setPassword(passwordEncoder.encode(password));
            user.setName(u.name());
            user.setLastName(u.lastname());
            user.setEmail(u.email());
            user.setDateOfBirth(date);
            user.setRoles(userRoles);
            user.setStatus(u.status());
            userRepository.save(user);

            emailService.sendPasswordForEmail(u.name(), u.email(), password);
        }


        return this.findAll();
    }


    public ResponseEntity<?> setPassword(String userId, PasswordDTO dto) {
        var user = userRepository.findByIdUser(UUID.fromString(userId));
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
}
