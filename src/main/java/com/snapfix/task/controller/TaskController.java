package com.snapfix.task.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.task.dto.TaskResponse;
import com.snapfix.task.service.TaskService;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/tasks")
public class TaskController {
    
    TaskService taskService;

    public TaskController(TaskService taskService){
        this.taskService = taskService;
    }

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable UUID id) {
        return TaskResponse.mapTask(taskService.getTask(id));
    }

    @PreAuthorize("hasRole('WORKER')")
    @PatchMapping("/{id}/start")
    public ResponseEntity<Void> startTask(@PathVariable UUID id){
        taskService.startTask(id);
        return ResponseEntity.ok().build();
    }
    
}
