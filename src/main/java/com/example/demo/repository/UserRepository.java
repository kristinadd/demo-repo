package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends CassandraRepository<User, UUID> {
    @Query("SELECT * FROM users WHERE username = ?0 ALLOW FILTERING")
    List<User> findByUsername(String username);

    @Query("SELECT * FROM users WHERE email = ?0 ALLOW FILTERING")
    List<User> findByEmail(String email);
} 