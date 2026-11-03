package pl.powtorka.task_manager.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.powtorka.task_manager.dto.CreateUserRequest;
import pl.powtorka.task_manager.dto.UpdateUserRequest;
import pl.powtorka.task_manager.dto.UserResponse;
import pl.powtorka.task_manager.exception.EmailAlreadyExistsException;
import pl.powtorka.task_manager.exception.UserHasTasksException;
import pl.powtorka.task_manager.exception.UserNotFoundException;
import pl.powtorka.task_manager.model.User;
import pl.powtorka.task_manager.repository.TaskRepository;
import pl.powtorka.task_manager.repository.UserRepository;

import java.time.LocalDateTime;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public UserService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }
    @Transactional
    public UserResponse createUser(CreateUserRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new EmailAlreadyExistsException("Email already exists");
        }
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        User savedUser =userRepository.save(user);
        return toResponse(savedUser);
    }
    @Cacheable(value ="users", key="#id")
    @Transactional(readOnly = true)
    public UserResponse findUserById(Long id){
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found")));
    }
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable){
        return userRepository.findAll(pageable)
                .map(this::toResponse);

    }
    @CacheEvict(value ="users", key="#id")
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request){
        User userToUpdate = userRepository.findById(id).
                orElseThrow(() -> new UserNotFoundException("User not found"));
        if(userRepository.existsByEmailAndIdNot(request.getEmail(),id)){
            throw new EmailAlreadyExistsException("Email already exists");
        }
        userToUpdate.setFirstName(request.getFirstName());
        userToUpdate.setLastName(request.getLastName());
        userToUpdate.setEmail(request.getEmail());
       User savedUser = userRepository.save(userToUpdate);
        return toResponse(savedUser);
    }
    @CacheEvict(value ="users", key="#id")
    @Transactional
    public void deleteUser(Long id){
        User userToDelete = userRepository.findById(id).
                orElseThrow(() -> new UserNotFoundException("User not found"));
        if(taskRepository.existsByUser_Id(id)){
            throw new UserHasTasksException("Cannot delete user with assigned tasks");
        }
        userRepository.delete(userToDelete);
    }

    private UserResponse toResponse (User user){
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
