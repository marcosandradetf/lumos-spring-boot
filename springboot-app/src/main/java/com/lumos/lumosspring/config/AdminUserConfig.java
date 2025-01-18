package com.lumos.lumosspring.config;

import com.lumos.lumosspring.user.Role;
import com.lumos.lumosspring.user.User;
import com.lumos.lumosspring.user.RoleRepository;
import com.lumos.lumosspring.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

@Configuration
public class AdminUserConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminUserConfig(RoleRepository roleRepository, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        var roleAdmin = roleRepository.findByRoleName(Role.Values.ADMIN.name());
        var roleManager = roleRepository.findByRoleName(Role.Values.ANALISTA.name());
        var userAdmin = userRepository.findByUsername("admin");
        var date = LocalDate.now();
        userAdmin.ifPresentOrElse(
                _ -> {
                    System.out.println("Admin já existe!");
                },
                () -> {
                    var user = new User();
                    user.setUsername("admin");
                    user.setName("Usuário");
                    user.setLastName("Administrador");
                    user.setEmail("admin@admin.com");
                    user.setDateOfBirth(date);
                    user.setPassword(passwordEncoder.encode("4dejulho_"));
                    user.setRoles(Set.of(roleAdmin, roleManager));
                    user.setStatus(true);
                    userRepository.save(user);
                }
        );

    }
}
