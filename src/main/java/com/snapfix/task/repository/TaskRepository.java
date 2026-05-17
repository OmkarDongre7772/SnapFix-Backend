package com.snapfix.task.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snapfix.task.entity.Task;

public interface TaskRepository extends JpaRepository<Task, UUID>{
    List<Task> findByWorker_Id(UUID workerId);
}
