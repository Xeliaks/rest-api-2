package com.example.tasks.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    /**
     * Retrieves a paginated list of students, sorted by creation time.
     */
    public Page<Student> findAll(Pageable pageable) {
        List<Student> allStudents = new ArrayList<>(store.values());
        allStudents.sort(Comparator.comparing(Student::getCreatedAt).reversed());
        int total = allStudents.size();
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, pageable.getPageSize());
        int start = (int) Math.min((long) page * size, total);
        int end = Math.min(start + size, total);
        List<Student> slice = start >= end ? List.of() : allStudents.subList(start, end);
        return new PageImpl<>(slice, pageable.withPage(page), total);
    }
}