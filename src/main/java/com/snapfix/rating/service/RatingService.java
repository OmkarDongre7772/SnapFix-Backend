package com.snapfix.rating.service;

import com.snapfix.task.service.TaskService;
import com.snapfix.user.entity.WorkerProfile;
import com.snapfix.worker.service.WorkerService;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.rating.dto.RatingRequest;
import com.snapfix.rating.dto.RatingResponse;
import com.snapfix.rating.dto.RatingSummary;
import com.snapfix.rating.entity.Rating;
import com.snapfix.rating.repository.RatingRepository;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;

import jakarta.transaction.Transactional;

@Service
public class RatingService {

    private final WorkerService workerService;
    private final TaskService taskService;
    private final RatingRepository ratingRepository;

    public RatingService(RatingRepository ratingRepository, TaskService taskService, WorkerService workerService) {
        this.ratingRepository = ratingRepository;
        this.taskService = taskService;
        this.workerService = workerService;
    }

    @Transactional
    public RatingResponse rateWorker(UUID workerId, RatingRequest request) {
        if (request.getTaskId() == null) {
            throw new IllegalArgumentException("taskId is required");
        }
        if (request.getScore() < 1 || request.getScore() > 5) {
            throw new IllegalArgumentException("score must be between 1 and 5");
        }

        Task task = taskService.getTaskById(request.getTaskId());

        if (!task.getWorker().getId().equals(workerId)) {
            throw new IllegalArgumentException("workerId does not match the task worker");
        }
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new IllegalStateException("The task must be complete before rating worker.");
        }
        if (!task.getReport().getCitizenId().equals(getCurrentUserId())) {
            throw new AccessDeniedException("Only the report owner can rate the worker.");
        }
        if (ratingRepository.existsByTask_Id(task.getId())) {
            throw new IllegalStateException("Task has already been rated.");
        }

        Rating rating = new Rating();
        rating.setCitizenId(task.getReport().getCitizenId());
        rating.setComment(request.getComment() == null ? "" : request.getComment());
        rating.setScore(request.getScore());
        rating.setTask(task);
        rating.setTimestamp(Instant.now());
        rating.setWorkerId(task.getWorker().getId());

        ratingRepository.save(rating);

        // Update the Profile Data
        WorkerProfile worker = workerService.getWorkerProfileByWorker_Id(task.getWorker().getId()).orElse(null);
        if (worker == null) {
            throw new IllegalStateException("Cannot find Worker Profile");
        }
        worker.setRating(ratingRepository.findAverageScoreByWorkerId(task.getWorker().getId()));
        worker.setCompletedTasks(worker.getCompletedTasks() + 1);
        workerService.saveProfile(worker);

        return RatingResponse.mapTOResponse(rating);
    }

    public RatingSummary getWorkeRatingSummary(UUID workerId) {
        WorkerProfile p = workerService.getWorkerProfileByWorker_Id(workerId).orElse(null);
        if (p == null) {
            throw new IllegalStateException("Cannot find Worker Profile");
        }
        return RatingSummary.mapToResponse(p.getRating(), p.getCompletedTasks());
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }
}
