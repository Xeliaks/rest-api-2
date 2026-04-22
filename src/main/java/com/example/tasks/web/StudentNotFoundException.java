package com.example.tasks.web;

import java.util.UUID;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(UUID id) {
        super("Student not found: " + id);
    }
}