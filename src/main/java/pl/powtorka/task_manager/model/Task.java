package pl.powtorka.task_manager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.powtorka.task_manager.enums.TaskPriority;
import pl.powtorka.task_manager.enums.TaskStatus;
import pl.powtorka.task_manager.exception.InvalidTaskStatusTransitionException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Setter
    private String title;
    @Setter
    private String description;
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.TODO;
    @Setter
    @Enumerated(EnumType.STRING)
    private TaskPriority priority;
    @Setter
    private LocalDate dueDate;
    @Setter
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Setter
    private User user;
    @Version
    @Column(nullable = false)
    private Long version;

    public void changeStatus(TaskStatus newStatus){
    if(this.status == TaskStatus.TODO && newStatus == TaskStatus.IN_PROGRESS){
        this.status = newStatus;
        return;
    }
    if(this.status == TaskStatus.IN_PROGRESS && newStatus == TaskStatus.DONE){
        this.status = newStatus;
        return;
    }
        throw new InvalidTaskStatusTransitionException("Invalid task status transition");
    }

}
