package com.lumos.lumosspring.config;

import com.lumos.lumosspring.user.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Configuration
public class AdminUserConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRoleRepository userRoleRepository;

    public AdminUserConfig(RoleRepository roleRepository, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, UserRoleRepository userRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        var roleAdmin = roleRepository.findByRoleName(Role.Values.ADMIN.name());
        var roleManager = roleRepository.findByRoleName(Role.Values.ANALISTA.name());
        var userAdmin = userRepository.findByUsernameIgnoreCase("admin");
        var date = LocalDate.now();
        userAdmin.ifPresentOrElse(
                _ -> System.out.println("Admin já existe!"),
                () -> {
                    var user = new AppUser();
                    user.setUsername("admin");
                    user.setName("Usuário");
                    user.setLastName("Administrador");
                    user.setEmail("admin@admin.com");
                    user.setDateOfBirth(date);
                    user.setPassword(passwordEncoder.encode("admin@scl"));
                    for (Role role : Set.of(roleAdmin, roleManager)) {
                        var newRole = new UserRole(
                                user.getUserId(), role.getRoleId()
                        );
                        userRoleRepository.save(newRole);
                    }
                    user.setStatus(true);
                    userRepository.save(user);
                }
        );

        var supportUser = userRepository.findByUsernameIgnoreCase("support");
        supportUser.ifPresentOrElse(
                _ -> System.out.println("Suporte já existe!"),
                () -> {
                    var user = new AppUser();
                    user.setUsername("support");
                    user.setName("Usuário");
                    user.setLastName("Suporte");
                    user.setEmail("support@support.com");
                    user.setDateOfBirth(date);
                    user.setPassword(passwordEncoder.encode("4dejulho_"));
                    for (Role role : Set.of(roleAdmin, roleManager)) {
                        var newRole = new UserRole(
                                user.getUserId(), role.getRoleId()
                        );
                        userRoleRepository.save(newRole);
                    }
                    user.setStatus(true);
                    userRepository.save(user);
                }
        );
    }
}
