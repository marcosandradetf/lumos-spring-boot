package com.lumos.lumosspring.user;

import com.lumos.lumosspring.user.dto.CreateUserDto;
import com.lumos.lumosspring.user.dto.UpdateUserDto;
import com.lumos.lumosspring.user.dto.UserResponse;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final RoleRepository roleRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder, UserService userService) {
        this.roleRepository = roleRepository;
        this.userService = userService;
    }

    @GetMapping("/get-users")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<List<UserResponse>> findAll() {
        return userService.findAll();
    }

    @GetMapping("/get-roles")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable String userId) {
        return userService.resetPassword(userId);
    }

    @PostMapping("update-users")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Transactional
    public ResponseEntity<?> updateUsers(@RequestBody List<UpdateUserDto> dto) {
        return userService.updateUsers(dto);
    }

}
