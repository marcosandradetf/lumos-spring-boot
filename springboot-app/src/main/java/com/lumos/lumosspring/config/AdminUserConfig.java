package com.lumos.lumosspring.config;

import com.lumos.lumosspring.user.Role;
import com.lumos.lumosspring.user.User;
import com.lumos.lumosspring.user.repository.RoleRepository;
import com.lumos.lumosspring.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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
        var roleAdmin = roleRepository.findByNomeRole(Role.Values.ADMIN.name());
        var userAdmin = userRepository.findByUsername("admin");
        userAdmin.ifPresentOrElse(
                _ -> {
                    System.out.println("Admin já existe!");
                },
                () -> {
                    var user = new User();
                    user.setUsername("admin");
                    user.setName("Conta");
                    user.setLastName("Administrador");
                    user.setPassword(passwordEncoder.encode("4dejulho_"));
                    user.setRoles(Set.of(roleAdmin));
                    userRepository.save(user);
                }
        );

    }
}
