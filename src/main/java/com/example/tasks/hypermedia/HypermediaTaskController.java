package com.example.tasks.hypermedia;

import com.example.tasks.model.Task;
import com.example.tasks.service.TaskService;
import com.example.tasks.web.TaskNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Hypermedia-driven view of tasks: paginated collections and {@code _links} on each resource
 * (HATEOAS). Plain JSON CRUD remains under {@code /api/tasks}.
 */
@RestController
@RequestMapping("/api/hypermedia/tasks")
@Tag(name = "Tasks (hypermedia)", description = "Paginated HAL collection and item links")
public class HypermediaTaskController {

    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 100;

    private final TaskService taskService;
    private final TaskEntityModelAssembler taskAssembler;

    public HypermediaTaskController(TaskService taskService, TaskEntityModelAssembler taskAssembler) {
        this.taskService = taskService;
        this.taskAssembler = taskAssembler;
    }

    @GetMapping
    @Operation(summary = "List tasks (paginated HAL)")
    @ApiResponse(
            responseCode = "200",
            description = "Paged tasks with navigation links",
            content = @Content(mediaType = "application/hal+json")
    )
    public PagedModel<EntityModel<Task>> list(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (capped)") @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        int pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Task> taskPage = taskService.findAll(pageable);
        List<EntityModel<Task>> models = taskPage.map(taskAssembler::toModel).getContent();

        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                taskPage.getSize(), taskPage.getNumber(), taskPage.getTotalElements());

        PagedModel<EntityModel<Task>> paged = PagedModel.of(models, metadata);

        int currentPage = taskPage.getNumber();
        paged.add(linkTo(methodOn(HypermediaTaskController.class).list(currentPage, pageSize)).withSelfRel());

        if (taskPage.getTotalPages() > 0) {
            paged.add(linkTo(methodOn(HypermediaTaskController.class).list(0, pageSize)).withRel(IanaLinkRelations.FIRST));
            paged.add(linkTo(methodOn(HypermediaTaskController.class).list(taskPage.getTotalPages() - 1, pageSize))
                    .withRel(IanaLinkRelations.LAST));
        }
        if (taskPage.hasNext()) {
            paged.add(linkTo(methodOn(HypermediaTaskController.class).list(currentPage + 1, pageSize))
                    .withRel(IanaLinkRelations.NEXT));
        }
        if (taskPage.hasPrevious()) {
            paged.add(linkTo(methodOn(HypermediaTaskController.class).list(currentPage - 1, pageSize))
                    .withRel(IanaLinkRelations.PREV));
        }

        return paged;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by id (HAL item)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Task with hypermedia links",
                    content = @Content(schema = @Schema(implementation = Task.class))
            ),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public EntityModel<Task> get(@Parameter(description = "Task id", required = true) @PathVariable UUID id) {
        Task task = taskService.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        return taskAssembler.toModel(task);
    }
}
