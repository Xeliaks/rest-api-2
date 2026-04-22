package com.example.tasks.hypermedia;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import org.springframework.stereotype.Component;

import com.example.tasks.model.Student;
import com.example.tasks.web.StudentController;

@Component
public class StudentEntityModelAssembler implements RepresentationModelAssembler<Student, EntityModel<Student>> {

    @Override
    public EntityModel<Student> toModel(Student student) {
        return EntityModel.of(
                student,
                linkTo(methodOn(HypermediaStudentController.class).get(student.getId())).withSelfRel(),
                linkTo(methodOn(HypermediaStudentController.class).list(0, HypermediaStudentController.DEFAULT_PAGE_SIZE))
                        .withRel(IanaLinkRelations.COLLECTION),
                linkTo(methodOn(StudentController.class).get(student.getId())).withRel("alternate"));
    }
}