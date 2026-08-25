package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.UserDto;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.User;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.UserRepository;
import com.infineonbit.sustainablefarm.modules.watersupply.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPasswordHash(hash(request.password()));
        try {
            return UserDto.from(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }
    }

    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(found -> verify(request.password(), found.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", jwtService.getExpirationSeconds());
        response.put("user", UserDto.from(user));
        return response;
    }

    @PostMapping("/api/auth/refresh")
    public Map<String, String> refresh() {
        return Map.of("message", "Refresh endpoint requires a valid JWT; provide the access token in the Authorization header");
    }

    @PostMapping("/api/auth/logout")
    public Map<String, String> logout() {
        return Map.of("message", "Logged out successfully");
    }

    @PostMapping("/api/auth/forgot-password")
    public Map<String, String> forgotPassword() {
        return Map.of("message", "If the email exists, a reset link has been sent");
    }

    @PostMapping("/api/auth/reset-password")
    public Map<String, String> resetPassword() {
        return Map.of("message", "Password reset endpoint is ready for token implementation");
    }

    @GetMapping("/api/users")
    public List<UserDto> users() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    @GetMapping("/api/users/{userId}")
    public UserDto user(@PathVariable UUID userId) {
        return UserDto.from(userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User")));
    }

    @PutMapping("/api/users/{userId}")
    public UserDto updateUser(@PathVariable UUID userId, @RequestBody User payload) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User"));
        user.setFirstName(payload.getFirstName() == null ? user.getFirstName() : payload.getFirstName());
        user.setLastName(payload.getLastName() == null ? user.getLastName() : payload.getLastName());
        user.setEmail(payload.getEmail() == null ? user.getEmail() : payload.getEmail());
        user.setStatus(payload.getStatus() == null ? user.getStatus() : payload.getStatus());
        return UserDto.from(userRepository.save(user));
    }

    @DeleteMapping("/api/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable UUID userId) {
        userRepository.delete(userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User")));
    }

    private String hash(String value) {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt) + ":" + sha256(salt, value);
    }

    private boolean verify(String rawPassword, String storedHash) {
        String[] parts = storedHash.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = HexFormat.of().parseHex(parts[0]);
        return parts[1].equals(sha256(salt, rawPassword));
    }

    private String sha256(byte[] salt, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public record RegisterRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @Email @NotBlank String email,
            @NotBlank String password) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    static class EmailAlreadyExistsException extends RuntimeException {
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class InvalidCredentialsException extends RuntimeException {
    }
}
