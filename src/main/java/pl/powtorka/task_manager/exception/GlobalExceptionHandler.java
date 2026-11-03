package pl.powtorka.task_manager.exception;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pl.powtorka.task_manager.dto.ErrorResponse;
import pl.powtorka.task_manager.dto.ValidationErrorResponse;
import org.hibernate.exception.ConstraintViolationException;



import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> wrongArgument(MethodArgumentNotValidException  exception){
        Map<String, String> errors = new HashMap<>();

        for(FieldError error : exception.getBindingResult().getFieldErrors()){
            errors.put(error.getField(),error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors,
                LocalDateTime.now()
        ));
    }
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> emailAlreadyExists(EmailAlreadyExistsException exception){


        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage(),
                LocalDateTime.now()
        ));
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity <ErrorResponse> userNotFound(UserNotFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                LocalDateTime.now()
        ));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity <ErrorResponse> notReadableMessage(HttpMessageNotReadableException exception){

        return ResponseEntity.badRequest().body(new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request body",
                LocalDateTime.now()
        ));
    }
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity <ErrorResponse> taskNotFoundException(TaskNotFoundException exception){

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                LocalDateTime.now()
        ));
    }
    @ExceptionHandler(UserHasTasksException.class)
    public ResponseEntity <ErrorResponse> userHasTasksException(UserHasTasksException exception){

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage(),
                LocalDateTime.now()
        ));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> mismatchException (MethodArgumentTypeMismatchException exception){
        Map<String, String> error = new HashMap<>();
        error.put("message", "Invalid parameter value");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidTaskStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> invalidStatusChange(InvalidTaskStatusTransitionException exception){

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOpitimisticLock(ObjectOptimisticLockingFailureException exception){

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Task was modified concurrently. Please retry",
                LocalDateTime.now()
        ));
    }
    @ExceptionHandler(ActiveTasksLimitExceededException.class)
    public ResponseEntity<ErrorResponse> tooManyTasks (ActiveTasksLimitExceededException exception){

        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage(),
                LocalDateTime.now()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintException) {
                String constraintName = constraintException.getConstraintName();

                if ("users_email_key".equals(constraintName)) {
                    return conflict("Email already exists");
                }
                if ("fk_tasks_user".equals(constraintName)) {
                    return conflict("Operation violates the relationship between task and user");
                }
            }
            cause = cause.getCause();
        }
        throw exception;
    }

    private ResponseEntity<ErrorResponse> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        message,
                        LocalDateTime.now()
                ));
    }

}
