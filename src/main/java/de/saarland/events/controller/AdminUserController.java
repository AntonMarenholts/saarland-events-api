package de.saarland.events.controller;

import de.saarland.events.dto.UserDto;
import de.saarland.events.mapper.UserMapper;
import de.saarland.events.model.User; // ИМПОРТ
import de.saarland.events.service.UserService;
import org.springframework.data.domain.Page; // ИМПОРТ
import org.springframework.data.domain.Pageable; // ИМПОРТ
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public AdminUserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }


    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        Page<User> userPage = userService.findAllUsers(pageable);
        Page<UserDto> dtoPage = userPage.map(userMapper::toDto);
        return ResponseEntity.ok(dtoPage);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}