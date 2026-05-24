package com.snapfix.task.service;

import com.snapfix.report.service.ReportService;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.report.entity.ReportStatus;
import com.snapfix.task.dto.TaskResponse;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.repository.TaskRepository;

import jakarta.transaction.Transactional;

@Service
public class TaskService {

    private final ReportService reportService;
    /*
     * BEAN INJECTION------------------
     */
    TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository, ReportService reportService) {
        this.taskRepository = taskRepository;
        this.reportService = reportService;
    }

    /*
     * SERVICE FUNCTIONS---------------
     */

    @Transactional
    public void startTask(UUID id) {
        Task task = getTaskOfWorkerByTask_Id(id);
        if (task.getStatus() == TaskStatus.ASSIGNED) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        } else {
            throw new IllegalStateException("Cannot change state to IN_PROGRESS from " + task.getStatus().toString());
        }
        taskRepository.save(task);
    }

    @Transactional
    public TaskResponse retryTask(UUID taskId) {
        Task task = getTaskOfWorkerByTask_Id(taskId);
        if (task.getStatus() != TaskStatus.REJECTED) throw new IllegalStateException("Cannot retry non-rejected tasks."); 
        if (task.getRetryCount() >= 3) throw new IllegalStateException("Max Retry Count Reached."); 
        task.setRetryCount(task.getRetryCount() + 1); 
        task.setStatus(TaskStatus.IN_PROGRESS); 
        taskRepository.save(task);
        //TODO: admin intervention logic pending
        return TaskResponse.mapTask(task);
    }

    @Transactional
    public List<Task> getTasksByStatus(TaskStatus status) {
        if (status == null) {
            return taskRepository.findAll();
        }
        return taskRepository.findAllByStatus(status);
    }

    @Transactional
    public TaskResponse approveTask(UUID taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if(task == null){
            throw new IllegalStateException("Task not Found");
        }
        if(task.getStatus() == TaskStatus.COMPLETED){
            throw new IllegalStateException("Cannot approve an pre-approved task.");
        }
        if(task.getStatus() == TaskStatus.REJECTED){
            throw new IllegalStateException("Cannot approve a rejected task.");
        }
        if(task.getStatus() != TaskStatus.VERIFIED_BY_CITIZEN){
            throw new IllegalStateException("Can only verify task after Citizen Verification.");
        }
        if(task.getReport().getStatus() != ReportStatus.IN_PROGRESS){
            throw new IllegalStateException("The Report must be In_Progress before Approving");
        }
        task.getReport().setStatus(ReportStatus.COMPLETED);
        task.setStatus(TaskStatus.COMPLETED);
        reportService.saveReport(task.getReport());
        taskRepository.save(task);
        //TODO: Payment Pipeline
        return TaskResponse.mapTask(task);
    }


    @Transactional
    public TaskResponse rejectTask(UUID taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if(task == null){
            throw new IllegalStateException("Task not Found");
        }
        if(task.getStatus() == TaskStatus.REJECTED){
            throw new IllegalStateException("Cannot reject an pre-rejected task.");
        }
        if(task.getStatus() == TaskStatus.COMPLETED){
            throw new IllegalStateException("Cannot reject an pre-approved task.");
        }
        if(task.getStatus() != TaskStatus.VERIFIED_BY_CITIZEN ){
            throw new IllegalStateException("Can only reject task after Citizen Verification.");
        }
        if(task.getReport().getStatus() != ReportStatus.IN_PROGRESS){
            throw new IllegalStateException("The Report must be In_Progress before Rejecting");
        }
        task.setStatus(TaskStatus.REJECTED);
        task.getReport().setStatus(ReportStatus.IN_PROGRESS);
        taskRepository.save(task);
        return TaskResponse.mapTask(task);
    }

    /*
     * UTILITY FUNCTIONS---------------
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

    public Task getTaskOfWorkerByTask_Id(UUID id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        ensureAssignedWorker(task);
        return task;
    }

    public void saveTask(Task task) {
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
