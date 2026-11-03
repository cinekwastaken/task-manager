package pl.powtorka.task_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.powtorka.task_manager.enums.TaskPriority;
import pl.powtorka.task_manager.enums.TaskStatus;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class UpdateTaskRequest {
    @NotBlank(message = "Setting title is mandatory")
    @Size(max = 100, message = "Title size max is 100")
    private String title;
    @Size(max=500, message = "Description size max is 500")
    private String description;
    @NotNull
    private TaskPriority priority;
    private LocalDate dueDate;
}
