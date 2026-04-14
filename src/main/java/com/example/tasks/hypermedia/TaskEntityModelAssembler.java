package com.example.tasks.hypermedia;

import com.example.tasks.model.Task;
import com.example.tasks.web.TaskController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class TaskEntityModelAssembler implements RepresentationModelAssembler<Task, EntityModel<Task>> {

    @Override
    public EntityModel<Task> toModel(Task task) {
        return EntityModel.of(
                task,
                linkTo(methodOn(HypermediaTaskController.class).get(task.getId())).withSelfRel(),
                linkTo(methodOn(HypermediaTaskController.class).list(0, HypermediaTaskController.DEFAULT_PAGE_SIZE))
                        .withRel(IanaLinkRelations.COLLECTION),
                linkTo(methodOn(TaskController.class).get(task.getId())).withRel("alternate"));
    }
}
