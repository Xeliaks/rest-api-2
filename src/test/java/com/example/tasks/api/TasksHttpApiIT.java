package com.example.tasks.api;

import com.example.tasks.TasksApplication;
import com.example.tasks.model.Task;
import com.example.tasks.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TasksApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TasksHttpApiIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TaskService taskService;

    @BeforeEach
    void resetStore() {
        taskService.clearAll();
    }

    @Test
    void crudFlow_overRealHttp() {
        ResponseEntity<List<Task>> empty = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(empty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(empty.getBody()).isNotNull().isEmpty();

        ResponseEntity<Task> created = restTemplate.postForEntity(
                "/api/tasks",
                Map.of("title", "HTTP task", "description", "via TestRestTemplate", "completed", false),
                Task.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        UUID id = created.getBody().getId();
        assertThat(created.getBody().getTitle()).isEqualTo("HTTP task");

        ResponseEntity<Task> fetched = restTemplate.getForEntity("/api/tasks/{id}", Task.class, id);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().getId()).isEqualTo(id);

        ResponseEntity<Task> put = restTemplate.exchange(
                "/api/tasks/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("title", "Updated", "description", "d2", "completed", true)),
                Task.class,
                id
        );
        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(put.getBody()).isNotNull();
        assertThat(put.getBody().isCompleted()).isTrue();

        ResponseEntity<Task> patched = restTemplate.exchange(
                "/api/tasks/{id}",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("completed", false)),
                Task.class,
                id
        );
        assertThat(patched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patched.getBody()).isNotNull();
        assertThat(patched.getBody().isCompleted()).isFalse();

        ResponseEntity<Void> deleted = restTemplate.exchange(
                "/api/tasks/{id}",
                HttpMethod.DELETE,
                null,
                Void.class,
                id
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> gone = restTemplate.getForEntity("/api/tasks/{id}", String.class, id);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void openApiJson_isServed() {
        ResponseEntity<String> docs = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody()).isNotNull();
        assertThat(docs.getBody()).contains("Tasks");
        assertThat(docs.getBody()).contains("/api/tasks");
        assertThat(docs.getBody()).contains("/api/hypermedia/tasks");
    }
}
