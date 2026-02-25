package com.ebirth.repository;

import com.ebirth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // ✅ Add this method to fix the error
    boolean existsByEmail(String email);
}
