package com.example.tasks.integration;

import com.example.tasks.TasksApplication;
import com.example.tasks.dto.TaskCreateRequest;
import com.example.tasks.service.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TasksApplication.class)
@AutoConfigureMockMvc
class TasksHypermediaMockMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskService taskService;

    @BeforeEach
    void resetStore() {
        taskService.clearAll();
    }

    @Test
    void pagedCollection_halShape_andNavigationLinks() throws Exception {
        for (int i = 0; i < 3; i++) {
            TaskCreateRequest body = new TaskCreateRequest();
            body.setTitle("t" + i);
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        String json = mockMvc.perform(get("/api/hypermedia/tasks").queryParam("page", "0").queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.tasks").isArray())
                .andExpect(jsonPath("$._embedded.tasks.length()").value(2))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.first.href").exists())
                .andExpect(jsonPath("$._links.last.href").exists())
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$._embedded.tasks[0]._links.self.href").exists())
                .andExpect(jsonPath("$._embedded.tasks[0]._links.collection.href").exists())
                .andExpect(jsonPath("$._embedded.tasks[0]._links.alternate.href").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(json);
        assertThat(root.path("_links").path("next").path("href").asText()).contains("page=1");
    }

    @Test
    void item_hal_includesSelfCollectionAndAlternate() throws Exception {
        String id = objectMapper.readTree(
                mockMvc.perform(post("/api/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createReq("one", null, false))))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("id").asText();

        mockMvc.perform(get("/api/hypermedia/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("one"))
                .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.endsWith("/api/hypermedia/tasks/" + id)))
                .andExpect(jsonPath("$._links.collection.href").exists())
                .andExpect(jsonPath("$._links.alternate.href").value(org.hamcrest.Matchers.endsWith("/api/tasks/" + id)));
    }

    @Test
    void item_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/hypermedia/tasks/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private static TaskCreateRequest createReq(String title, String description, boolean completed) {
        TaskCreateRequest r = new TaskCreateRequest();
        r.setTitle(title);
        r.setDescription(description);
        r.setCompleted(completed);
        return r;
    }
}
