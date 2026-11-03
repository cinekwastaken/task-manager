package pl.powtorka.task_manager.exception;

public class ActiveTasksLimitExceededException extends RuntimeException {
    public ActiveTasksLimitExceededException(String message) {
        super(message);
    }
}
