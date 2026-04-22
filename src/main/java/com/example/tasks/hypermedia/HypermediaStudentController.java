package com.example.tasks.hypermedia;

import com.example.tasks.model.Student;
import com.example.tasks.service.StudentService;
import com.example.tasks.web.StudentNotFoundException;
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

@RestController
@RequestMapping("/api/hypermedia/students")
@Tag(name = "Students (hypermedia)", description = "Paginated HAL collection and item links")
public class HypermediaStudentController {

    static final int DEFAULT_PAGE_SIZE = 10;
    static final int MAX_PAGE_SIZE = 100;

    private final StudentService studentService;
    private final StudentEntityModelAssembler studentAssembler;

    public HypermediaStudentController(StudentService studentService, StudentEntityModelAssembler studentAssembler) {
        this.studentService = studentService;
        this.studentAssembler = studentAssembler;
    }

    @GetMapping
    @Operation(summary = "List students (paginated HAL)")
    @ApiResponse(
            responseCode = "200",
            description = "Paged students with navigation links",
            content = @Content(mediaType = "application/hal+json")
    )
    public PagedModel<EntityModel<Student>> list(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (capped)") @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        
        int pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize);
        Page<Student> studentPage = studentService.findAll(pageable);
        
        List<EntityModel<Student>> models = studentPage.map(studentAssembler::toModel).getContent();

        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                studentPage.getSize(), studentPage.getNumber(), studentPage.getTotalElements());

        PagedModel<EntityModel<Student>> paged = PagedModel.of(models, metadata);

        int currentPage = studentPage.getNumber();
        paged.add(linkTo(methodOn(HypermediaStudentController.class).list(currentPage, pageSize)).withSelfRel());

        if (studentPage.getTotalPages() > 0) {
            paged.add(linkTo(methodOn(HypermediaStudentController.class).list(0, pageSize)).withRel(IanaLinkRelations.FIRST));
            paged.add(linkTo(methodOn(HypermediaStudentController.class).list(studentPage.getTotalPages() - 1, pageSize))
                    .withRel(IanaLinkRelations.LAST));
        }
        if (studentPage.hasNext()) {
            paged.add(linkTo(methodOn(HypermediaStudentController.class).list(currentPage + 1, pageSize))
                    .withRel(IanaLinkRelations.NEXT));
        }
        if (studentPage.hasPrevious()) {
            paged.add(linkTo(methodOn(HypermediaStudentController.class).list(currentPage - 1, pageSize))
                    .withRel(IanaLinkRelations.PREV));
        }

        return paged;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a student by id (HAL item)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Student with hypermedia links",
                    content = @Content(schema = @Schema(implementation = Student.class))
            ),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public EntityModel<Student> get(@Parameter(description = "Student id", required = true) @PathVariable UUID id) {
        Student student = studentService.getById(id);
        if (student == null) {
            throw new StudentNotFoundException(id);
        }
        return studentAssembler.toModel(student);
    }
}