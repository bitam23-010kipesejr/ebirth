package com.ebirth.controller;

import com.ebirth.model.User;
import com.ebirth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(
        origins = {"http://localhost:5173", "http://localhost:3000"},
        allowCredentials = "true"
)
public class UserController {

    @Autowired
    private UserRepository userRepo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // 🔹 Get all users
    @GetMapping
    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    // 🔹 Create a new officer
    @PostMapping("/officer")
    public String createOfficer(@RequestBody User user) {
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("OFFICER");
        userRepo.save(user);
        return "Officer account created successfully.";
    }

    // 🔹 Delete a user (client/officer)
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userRepo.deleteById(id);
        return "User deleted successfully.";
    }
}
