package pl.powtorka.task_manager.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.powtorka.task_manager.enums.TaskPriority;
import pl.powtorka.task_manager.enums.TaskStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse implements Serializable {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private Long userId;
}
