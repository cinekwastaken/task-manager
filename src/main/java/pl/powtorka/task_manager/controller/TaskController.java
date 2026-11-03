package pl.powtorka.task_manager.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.powtorka.task_manager.dto.ChangeTaskStatusRequest;
import pl.powtorka.task_manager.dto.CreateTaskRequest;
import pl.powtorka.task_manager.dto.TaskResponse;
import pl.powtorka.task_manager.dto.UpdateTaskRequest;
import pl.powtorka.task_manager.enums.TaskStatus;
import pl.powtorka.task_manager.service.TaskService;



@RestController
@RequiredArgsConstructor
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(@RequestBody @Valid CreateTaskRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity <TaskResponse> getById(@PathVariable Long id){
        return ResponseEntity.ok(taskService.findTaskById(id));
    }

    @GetMapping
    public ResponseEntity <Page<TaskResponse>> getAll(@PageableDefault(page=0,size=20,sort="id",direction = Sort.Direction.ASC) Pageable pageable){
        return ResponseEntity.ok(taskService.findAll(pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<TaskResponse>> getTasksByStatus(@PathVariable TaskStatus status, @PageableDefault(page=0,size=20,sort="id",direction = Sort.Direction.ASC) Pageable pageable){
        return ResponseEntity.ok(taskService.findTasksByStatus(status, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id, @RequestBody @Valid UpdateTaskRequest request){
        return ResponseEntity.ok(taskService.updateTask(id,request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> statusChange(@PathVariable Long id, @RequestBody @Valid ChangeTaskStatusRequest request){
        return ResponseEntity.ok(taskService.changeStatus(id,request.getStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
