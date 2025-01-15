package com.lumos.lumosspring.user;

import com.lumos.lumosspring.notification.EmailService;
import com.lumos.lumosspring.user.dto.UserResponse;
import com.lumos.lumosspring.util.DefaultResponse;
import com.lumos.lumosspring.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public ResponseEntity<List<UserResponse>> findAll() {
        List<User> users = userRepository.findAll();
        List<UserResponse> userResponses = new ArrayList<>();
        for (User user : users) {
            LocalDate dateOfBirth = user.getDateOfBirth();
            userResponses.add(new UserResponse(
                    user.getIdUser().toString(),
                    user.getUsername(),
                    user.getName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRoles().stream()
                            .map(Role::getNomeRole) // Pega apenas o nome de cada Role
                            .collect(Collectors.toList()), // Coleta como uma lista
                    dateOfBirth != null ? dateOfBirth.toString() : null,
                    user.getStatus()
            ));
        }

        return ResponseEntity.status(HttpStatus.OK).body(userResponses);
    }

    public ResponseEntity<?> resetPassword(String userId) {
        var user = userRepository.findByIdUser(UUID.fromString(userId));
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Usuário não encontrado"));
        }
        String newPassword = UUID.randomUUID().toString();
        user.get().setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user.get());

        emailService.sendEmail(user.get().getEmail(),
                "Sistema Lumos - Reset de Senha",
                STR."Olá, \{user.get().getName()}<br>Sua senha foi resetada! A nova senha é: \{newPassword}<br>Use-a para acessar sua conta e definir uma nova senha.");

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

    private ResponseEntity<?> updatePassword(String userId, String code, String newPassword) {
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
}
