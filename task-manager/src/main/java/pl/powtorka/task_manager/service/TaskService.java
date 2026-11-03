package pl.powtorka.task_manager.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.powtorka.task_manager.dto.CreateTaskRequest;
import pl.powtorka.task_manager.dto.TaskResponse;
import pl.powtorka.task_manager.dto.UpdateTaskRequest;
import pl.powtorka.task_manager.enums.TaskStatus;
import pl.powtorka.task_manager.exception.ActiveTasksLimitExceededException;
import pl.powtorka.task_manager.exception.TaskNotFoundException;
import pl.powtorka.task_manager.exception.UserNotFoundException;
import pl.powtorka.task_manager.model.Task;
import pl.powtorka.task_manager.model.User;
import pl.powtorka.task_manager.repository.TaskRepository;
import pl.powtorka.task_manager.repository.UserRepository;


import java.time.LocalDateTime;
import java.util.List;


@Service
public class TaskService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public TaskService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request){
       User user = userRepository.findByIdForUpdate(request.getUserId())
               .orElseThrow(() -> new UserNotFoundException("User not found"));

       List<TaskStatus> activeStatuses = List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS);

       long activeTasks = taskRepository.countByUserIdAndStatusIn(user.getId(),activeStatuses);
       if(activeTasks>=5) {
           throw new ActiveTasksLimitExceededException("Task limit is 5");
       }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setCreatedAt(LocalDateTime.now());
        task.setUser(user);
        Task savedTask = taskRepository.save(task);


        return toResponse(savedTask);
    }
    @Cacheable(value ="tasks", key="#id")
    @Transactional(readOnly = true)
    public TaskResponse findTaskById(Long id){
        return toResponse(taskRepository.findById(id).
                orElseThrow(() -> new TaskNotFoundException("Task not found")));
    }
    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(Pageable pageable){
        return taskRepository.findAll(pageable)
                .map(this::toResponse);

    }
    @Transactional(readOnly = true)
    public Page<TaskResponse> findTasksByUser(Long userId, TaskStatus status, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found");
        }
        if (status == null) {
           return taskRepository.findByUser_Id(userId,pageable)
                    .map(this::toResponse);


        }
            return taskRepository.findByUser_IdAndStatus(userId,status, pageable)
                    .map(this::toResponse);


    }
    @Transactional(readOnly = true)
    public Page<TaskResponse> findTasksByStatus(TaskStatus status, Pageable pageable){
        return taskRepository.findByStatus(status, pageable)
                .map(this::toResponse);

    }
    @CacheEvict(value ="tasks", key="#id")
    @Transactional
    public TaskResponse updateTask(Long id,UpdateTaskRequest request){
        Task toUpdate = taskRepository.findById(id).
                orElseThrow(() -> new TaskNotFoundException("Task not found"));
        toUpdate.setTitle(request.getTitle());
        toUpdate.setDescription(request.getDescription());
        toUpdate.setPriority(request.getPriority());
        toUpdate.setDueDate(request.getDueDate());

        return toResponse(toUpdate);
    }
    @CacheEvict(value ="tasks", key="#id")
    @Transactional
    public void deleteTask(Long id){
        Task toDelete = taskRepository.findById(id).
                orElseThrow(() -> new TaskNotFoundException("Task not found"));
        taskRepository.delete(toDelete);
    }

    @Transactional
    @CacheEvict(value = "tasks", key = "#taskId")
    public TaskResponse changeStatus(Long taskId, TaskStatus newStatus) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task "+ taskId + " not found" ));
        task.changeStatus(newStatus);
        return toResponse(task);
    }

    private TaskResponse toResponse(Task task){
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUser().getId()
        );
    }
}
