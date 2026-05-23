package com.snapfix.verification.service;

import com.snapfix.proof.service.ProofService;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.service.TaskService;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoVerificationTask {
    private final TaskService taskService;
    private final ProofService proofService;
    VerificationService verificationService;
    public AutoVerificationTask(VerificationService verificationService, ProofService proofService, TaskService taskService){
        this.verificationService = verificationService;
        this.proofService = proofService;
        this.taskService = taskService;
    }

    @Scheduled(cron = "0 0 1 * * *")  // Scheduled Daily At 01:00 AM 
    public void autoVerifyOldTasks(){
        List<Task> tasks = proofService.getOldProofSubmitted(); // Get the Tasks from the proof's uploaded 5 day's ago.
        for (Task task : tasks) {
            verificationService.autoVerifyTask(task.getId()); // Auto-Verify each task.
            task.setStatus(TaskStatus.VERIFIED_BY_CITIZEN);   // Update the task status to VERIFIED.
            taskService.saveTask(task);
        }
    }
}
