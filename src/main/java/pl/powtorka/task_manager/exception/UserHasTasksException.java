package pl.powtorka.task_manager.exception;

public class UserHasTasksException extends RuntimeException {
    public UserHasTasksException(String message) {
        super(message);
    }
}
