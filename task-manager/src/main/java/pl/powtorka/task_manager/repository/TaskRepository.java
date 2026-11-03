package pl.powtorka.task_manager.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.powtorka.task_manager.enums.TaskStatus;
import pl.powtorka.task_manager.model.Task;

import java.util.Collection;


public interface TaskRepository extends JpaRepository<Task, Long> {
    boolean existsByUser_Id(Long userId);

    Page<Task> findByUser_Id(Long userId, Pageable pageable);

    Page<Task> findByUser_IdAndStatus(Long userId, TaskStatus status, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    long countByUserIdAndStatusIn(Long userId, Collection<TaskStatus> statuses);
}
