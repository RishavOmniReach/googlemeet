package com.example.googlemeet.repository;

import com.example.googlemeet.dto.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
   User findByEmail(String email);
}
