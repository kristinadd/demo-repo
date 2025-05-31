package com.example.demo.repository;

import com.example.demo.User;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends CassandraRepository<User, UUID> {
    // Basic CRUD operations are automatically implemented
} 