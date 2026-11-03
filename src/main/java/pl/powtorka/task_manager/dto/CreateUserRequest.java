package pl.powtorka.task_manager.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;
    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a properly formatted email address")
    @Size(max = 50)
    private String email;
}
