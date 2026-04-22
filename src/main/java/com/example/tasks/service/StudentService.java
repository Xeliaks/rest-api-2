package com.example.tasks.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.tasks.dto.StudentCreateRequest;
import com.example.tasks.model.Student;

@Service
public class StudentService {

    private final Map<UUID, Student> store = new ConcurrentHashMap<>();

    public Student create(StudentCreateRequest request) {
        UUID newId = UUID.randomUUID();
        Instant now = Instant.now();

        Student student = new Student(
                newId,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                now
        );

        store.put(newId, student);
        return student;
    }
    
    public Student getById(UUID id) {
        return store.get(id);
    }
}