package com.ebirth.controller;

import com.ebirth.model.User;
import com.ebirth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(
        originPatterns = {"http://localhost:5173", "http://localhost:3000", "https://*.onrender.com"},
        allowCredentials = "true"
)
public class AdminController {

    @Autowired
    private UserRepository userRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // ✅ Get all users (excluding admins if you want)
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // ✅ Create officer account
    @PostMapping("/create-officer")
    public String createOfficer(@RequestBody User user) {
        if (userRepo.existsByEmail(user.getEmail())) {
            return "Email already exists!";
        }

        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("OFFICER");
        userRepo.save(user);
        return "Officer account created!";
    }

    // ✅ Delete user by ID
    @DeleteMapping("/delete-user/{id}")
    public String deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) {
            return "User not found!";
        }

        userRepo.deleteById(id);
        return "User deleted!";
    }
}
