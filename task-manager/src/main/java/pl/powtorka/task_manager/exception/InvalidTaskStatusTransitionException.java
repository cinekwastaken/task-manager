package pl.powtorka.task_manager.exception;

public class InvalidTaskStatusTransitionException extends RuntimeException {
    public InvalidTaskStatusTransitionException(String message) {
        super(message);
    }
}
