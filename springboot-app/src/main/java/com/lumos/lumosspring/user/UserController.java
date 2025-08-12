package com.lumos.lumosspring.user;

import com.lumos.lumosspring.dto.user.CreateUserDto;
import com.lumos.lumosspring.dto.user.PasswordDTO;
import com.lumos.lumosspring.dto.user.UpdateUserDto;
import com.lumos.lumosspring.dto.user.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final RoleRepository roleRepository;
    private final UserService userService;

    public UserController(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder, UserService userService) {
        this.roleRepository = roleRepository;
        this.userService = userService;
    }

    @GetMapping("/user/get-users")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<List<UserResponse>> findAll() {
        return userService.findAll();
    }

    @GetMapping("/user/get-user/{uuid}")
    public ResponseEntity<UserResponse> find(@PathVariable String uuid) {
        return userService.find(uuid);
    }

    @GetMapping("/user/get-roles")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public Iterable<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @PostMapping("/user/{userId}/reset-password")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable String userId) {
        return userService.resetPassword(userId);
    }

    @PostMapping("/user/{userId}/set-password")
    public ResponseEntity<?> setPassword(@PathVariable String userId, @RequestBody PasswordDTO dto) {
        return userService.setPassword(userId, dto);
    }

    @PostMapping("/user/update-users")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Transactional
    public ResponseEntity<?> updateUsers(@RequestBody List<UpdateUserDto> dto) {
        return userService.updateUsers(dto);
    }

    @PostMapping("/user/insert-users")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    @Transactional
    public ResponseEntity<?> insertUsers(@RequestBody List<CreateUserDto> dto) {
        return userService.insertUsers(dto);
    }

    @GetMapping("/mobile/user/get-operational-users")
    public ResponseEntity<?> getOperationalUsers() {
        return userService.getOperationalUsers();
    }

}
