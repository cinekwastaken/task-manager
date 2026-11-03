package pl.powtorka.task_manager.dto;


import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.powtorka.task_manager.enums.TaskStatus;
@Setter
@Getter
@NoArgsConstructor
public class ChangeTaskStatusRequest {
    @NotNull
    private TaskStatus status;
}
