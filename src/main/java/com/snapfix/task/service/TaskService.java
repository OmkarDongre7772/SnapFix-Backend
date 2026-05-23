package com.snapfix.task.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.repository.TaskRepository;

import jakarta.transaction.Transactional;

@Service
public class TaskService {

/*
        BEAN INJECTION------------------
*/
    TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

/*
        SERVICE FUNCTIONS---------------
*/

    @Transactional
    public void startTask(UUID id) {
        Task task = getTask(id);
        if (task.getStatus() == TaskStatus.ASSIGNED) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        } else {
            throw new IllegalStateException("Cannot change state to IN_PROGRESS from " + task.getStatus().toString());
        }
        taskRepository.save(task);
    }

/*
        UTILITY FUNCTIONS---------------
*/

    private void ensureAssignedWorker(Task task) {
        if (!task.getWorker().getId().equals(getCurrentUserId())) {
            throw new AccessDeniedException("Task is not assigned to current worker");
        }
    }
    
    public Task getTaskById(UUID id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    }

    public Task getTask(UUID id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        ensureAssignedWorker(task);
        return task;
    }

    public void saveTask(Task task){
        taskRepository.save(task);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }

}
