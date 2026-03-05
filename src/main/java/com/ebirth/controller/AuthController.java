package com.ebirth.controller;

import com.ebirth.dto.LoginRequest;
import com.ebirth.dto.RegisterRequest;
import com.ebirth.model.User;
import com.ebirth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
        origins = "https://ebirthsystem.netlify.app",
        allowCredentials = "true"
)
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ✅ Register a new client user
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request == null || isBlank(request.email) || isBlank(request.password) || isBlank(request.fullName)) {
            return ResponseEntity.badRequest().body(Map.of("message", "fullName, email and password are required."));
        }

        String normalizedEmail = request.email.trim().toLowerCase();
        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already registered!"));
        }

        User user = new User();
        user.setFullName(request.fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(encoder.encode(request.password.trim()));
        user.setRole("CLIENT");

        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Registration successful!"));
    }

    // ✅ Login for any user (Client, Officer, Admin)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || isBlank(request.email) || isBlank(request.password)) {
            return ResponseEntity.badRequest().body(Map.of("message", "email and password are required."));
        }

        String normalizedEmail = request.email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Email not found!"));
        }

        User user = userOpt.get();
        String storedPassword = user.getPassword();

        // Support legacy users created with plain text passwords,
        // then migrate them to BCrypt on successful login.
        String rawPassword = request.password.trim();
        boolean bcryptMatch = storedPassword != null && storedPassword.startsWith("$2") && encoder.matches(rawPassword, storedPassword);
        boolean plainMatch = storedPassword != null && storedPassword.equals(rawPassword);

        if (!bcryptMatch && !plainMatch) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid password!"));
        }

        if (plainMatch) {
            user.setPassword(encoder.encode(rawPassword));
            userRepository.save(user);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        return ResponseEntity.ok(response);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
