package com.example.tasks.service;

import com.example.tasks.dto.TaskCreateRequest;
import com.example.tasks.dto.TaskPatchRequest;
import com.example.tasks.dto.TaskUpdateRequest;
import com.example.tasks.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskService {

    private final Map<UUID, Task> store = new ConcurrentHashMap<>();

    public List<Task> findAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * Tasks ordered by creation time (stable paging for the in-memory store).
     */
    public Page<Task> findAll(Pageable pageable) {
        List<Task> all = new ArrayList<>(store.values());
        all.sort(Comparator
                .comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(t -> t.getTitle() != null ? t.getTitle() : ""));
        int total = all.size();
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.max(1, pageable.getPageSize());
        int start = (int) Math.min((long) page * size, total);
        int end = Math.min(start + size, total);
        List<Task> slice = start >= end ? List.of() : all.subList(start, end);
        return new PageImpl<>(slice, pageable.withPage(page), total);
    }

    public Optional<Task> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    public Task create(TaskCreateRequest request) {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        Task task = new Task(
                id,
                request.getTitle(),
                request.getDescription(),
                request.isCompleted(),
                now,
                now
        );
        store.put(id, task);
        return task;
    }

    public Optional<Task> replace(UUID id, TaskUpdateRequest request) {
        return findById(id).map(existing -> {
            Instant now = Instant.now();
            Task updated = new Task(
                    id,
                    request.getTitle(),
                    request.getDescription(),
                    request.isCompleted(),
                    existing.getCreatedAt(),
                    now
            );
            store.put(id, updated);
            return updated;
        });
    }

    public Optional<Task> patch(UUID id, TaskPatchRequest request) {
        return findById(id).map(existing -> {
            Instant now = Instant.now();
            String title = request.getTitle() != null ? request.getTitle() : existing.getTitle();
            String description = existing.getDescription();
            if (request.getDescription() != null) {
                description = request.getDescription();
            }
            boolean completed = request.getCompleted() != null ? request.getCompleted() : existing.isCompleted();
            Task updated = new Task(
                    id,
                    title,
                    description,
                    completed,
                    existing.getCreatedAt(),
                    now
            );
            store.put(id, updated);
            return updated;
        });
    }

    public boolean delete(UUID id) {
        return store.remove(id) != null;
    }

    /** Clears all tasks; used by tests to isolate scenarios. */
    public void clearAll() {
        store.clear();
    }
}
