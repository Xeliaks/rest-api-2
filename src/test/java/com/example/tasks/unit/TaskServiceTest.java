package com.example.tasks.unit;

import com.example.tasks.dto.TaskCreateRequest;
import com.example.tasks.dto.TaskPatchRequest;
import com.example.tasks.dto.TaskUpdateRequest;
import com.example.tasks.model.Task;
import com.example.tasks.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TaskServiceTest {

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService();
    }

    @Test
    void create_persistsAndReturnsTaskWithNewId() {
        TaskCreateRequest req = new TaskCreateRequest();
        req.setTitle("Alpha");
        req.setDescription("details");
        req.setCompleted(true);

        Task created = service.create(req);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("Alpha");
        assertThat(created.getDescription()).isEqualTo("details");
        assertThat(created.isCompleted()).isTrue();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isEqualTo(created.getCreatedAt());

        assertThat(service.findById(created.getId())).contains(created);
    }

    @Test
    void findById_returnsEmptyWhenMissing() {
        assertThat(service.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findAll_isEmptyInitially() {
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findAll_pageable_returnsStableSlicesOrderedByCreatedAt() {
        service.create(createRequest("a", null, false));
        service.create(createRequest("b", null, false));
        service.create(createRequest("c", null, false));

        Page<Task> first = service.findAll(PageRequest.of(0, 2));
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getContent().get(0).getTitle()).isEqualTo("a");
        assertThat(first.getContent().get(1).getTitle()).isEqualTo("b");
        assertThat(first.hasNext()).isTrue();

        Page<Task> second = service.findAll(PageRequest.of(1, 2));
        assertThat(second.getContent()).hasSize(1);
        assertThat(second.getContent().get(0).getTitle()).isEqualTo("c");
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void replace_updatesFieldsAndPreservesCreatedAt() {
        Task t = service.create(createRequest("Old", "d", false));
        var createdAt = t.getCreatedAt();

        TaskUpdateRequest upd = new TaskUpdateRequest();
        upd.setTitle("New");
        upd.setDescription("nd");
        upd.setCompleted(true);

        Task updated = service.replace(t.getId(), upd).orElseThrow();

        assertThat(updated.getTitle()).isEqualTo("New");
        assertThat(updated.getDescription()).isEqualTo("nd");
        assertThat(updated.isCompleted()).isTrue();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }

    @Test
    void replace_returnsEmptyWhenIdUnknown() {
        TaskUpdateRequest upd = new TaskUpdateRequest();
        upd.setTitle("x");
        upd.setCompleted(false);
        assertThat(service.replace(UUID.randomUUID(), upd)).isEmpty();
    }

    @Test
    void patch_appliesOnlyProvidedFields() {
        Task t = service.create(createRequest("Title", "desc", false));

        TaskPatchRequest p = new TaskPatchRequest();
        p.setCompleted(true);
        Task patched = service.patch(t.getId(), p).orElseThrow();

        assertThat(patched.getTitle()).isEqualTo("Title");
        assertThat(patched.getDescription()).isEqualTo("desc");
        assertThat(patched.isCompleted()).isTrue();
    }

    @Test
    void patch_canClearDescriptionWithEmptyString() {
        Task t = service.create(createRequest("T", "gone", false));
        TaskPatchRequest p = new TaskPatchRequest();
        p.setDescription("");
        Task patched = service.patch(t.getId(), p).orElseThrow();
        assertThat(patched.getDescription()).isEmpty();
    }

    @Test
    void delete_removesTask() {
        Task t = service.create(createRequest("x", null, false));
        assertThat(service.delete(t.getId())).isTrue();
        assertThat(service.findById(t.getId())).isEmpty();
    }

    @Test
    void delete_returnsFalseWhenMissing() {
        assertThat(service.delete(UUID.randomUUID())).isFalse();
    }

    private static TaskCreateRequest createRequest(String title, String description, boolean completed) {
        TaskCreateRequest r = new TaskCreateRequest();
        r.setTitle(title);
        r.setDescription(description);
        r.setCompleted(completed);
        return r;
    }
}
