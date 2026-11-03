package pl.powtorka.task_manager.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import pl.powtorka.task_manager.dto.ChangeTaskStatusRequest;
import pl.powtorka.task_manager.dto.UpdateTaskRequest;
import pl.powtorka.task_manager.model.Task;
import tools.jackson.databind.ObjectMapper;
import pl.powtorka.task_manager.config.IntegrationTestConfig;
import pl.powtorka.task_manager.dto.CreateTaskRequest;
import pl.powtorka.task_manager.dto.TaskResponse;
import pl.powtorka.task_manager.enums.TaskPriority;
import pl.powtorka.task_manager.enums.TaskStatus;
import pl.powtorka.task_manager.model.User;
import pl.powtorka.task_manager.repository.TaskRepository;
import pl.powtorka.task_manager.repository.UserRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        cacheManager.getCache("users").clear();
        cacheManager.getCache("tasks").clear();

        User user = new User();
        user.setFirstName("Cinek");
        user.setLastName("Luki");
        user.setEmail("Cinek@example.com");
        user.setCreatedAt(LocalDateTime.now());

        userId = userRepository.save(user).getId();
    }


    @Test
    void shouldCreateTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("test");
        request.setDescription("test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value(request.getTitle()));
    }

    @Test
    void shouldGetTasksById() throws Exception{
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("test");
        request.setDescription("test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        mockMvc.perform(get("/tasks/{id}", createdTask.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTask.getId()))
                .andExpect(jsonPath("$.title").value(request.getTitle()))
                .andExpect(jsonPath("$.status").value("TODO"));


    }

    @Test
    void shouldReturnNotFoundWhenCreatingTaskForMissingUser() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(999999L);
        request.setTitle("Test");
        request.setDescription("Test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
        assertEquals(0, taskRepository.count());
    }

    @Test
    void shouldUpdateTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("test");
        request.setDescription("test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        Long taskId = createdTask.getId();

        UpdateTaskRequest updateRequest = new UpdateTaskRequest();
        updateRequest.setTitle("Update test");
        updateRequest.setDescription("Update test");
        updateRequest.setPriority(TaskPriority.LOW);
        updateRequest.setDueDate(LocalDate.now().plusDays(14));

        mockMvc.perform(put("/tasks/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Update test"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("LOW"));

        Task updatedTask = taskRepository.findById(taskId)
                .orElseThrow();

        assertEquals("Update test", updatedTask.getTitle());
        assertEquals(TaskStatus.TODO, updatedTask.getStatus());
        assertEquals(TaskPriority.LOW, updatedTask.getPriority());

    }

    @Test
    void shouldDeleteTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("test");
        request.setDescription("test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        Long taskId = createdTask.getId();

        mockMvc.perform(delete("/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.existsById(taskId));
    }

    @Test
    void shouldReturnNotFoundForDeletedTask() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("test");
        request.setDescription("test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        Long taskId = createdTask.getId();

        mockMvc.perform(delete("/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found"));
    }

    @Test
    void shouldInvalidateCachceAfterUpdate() throws Exception{
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("test");
        request.setDescription("test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        Long taskId = createdTask.getId();

        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("test"));

        Cache tasksCache = cacheManager.getCache("tasks");

        assertNotNull(tasksCache);
        assertNotNull(tasksCache.get(taskId));

        UpdateTaskRequest updateRequest = new UpdateTaskRequest();
        updateRequest.setTitle("Update test");
        updateRequest.setDescription("test");
        updateRequest.setPriority(TaskPriority.HIGH);
        updateRequest.setDueDate(LocalDate.now().plusDays(7));

        mockMvc.perform(put("/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        assertNull(tasksCache.get(taskId));

        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(jsonPath("$.title").value("Update test"));
    }

    @Test
    void shouldChangeTaskStatusFromTodoToInProgress() throws Exception{
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("test");
        request.setDescription("test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        Long taskId = createdTask.getId();

        ChangeTaskStatusRequest changeRequest = new ChangeTaskStatusRequest();
        changeRequest.setStatus(TaskStatus.IN_PROGRESS);

        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        Task updatedTask = taskRepository.findById(taskId)
                .orElseThrow();

        assertEquals(TaskStatus.IN_PROGRESS, updatedTask.getStatus());
    }

    @Test
    void shouldRejectTransitionFromTodoToDone() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("test");
        request.setDescription("test");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        Long taskId = createdTask.getId();

        ChangeTaskStatusRequest changeRequest = new ChangeTaskStatusRequest();
        changeRequest.setStatus(TaskStatus.DONE);

        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeRequest)))
                .andExpect(status().isConflict());

        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        assertEquals(TaskStatus.TODO, task.getStatus());
    }

    @Test
    void shouldReturnNotFoundWhenChangingStatusOfMissingTask() throws Exception {

        ChangeTaskStatusRequest request = new ChangeTaskStatusRequest();
        request.setStatus(TaskStatus.IN_PROGRESS);

        mockMvc.perform(patch("/tasks/{id}/status", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnBadRequestWhenStatusIsMissing() throws Exception {

        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setUserId(userId);
        createRequest.setTitle("Test");
        createRequest.setDescription("Test");
        createRequest.setPriority(TaskPriority.HIGH);
        createRequest.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        Long taskId = createdTask.getId();

        mockMvc.perform(patch("/tasks/{id}/status", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.status").exists());

        Task task = taskRepository.findById(taskId)
                .orElseThrow();

        assertEquals(TaskStatus.TODO, task.getStatus());
    }

    @Test
    void shouldPreventConcurrentTaskUpdates() throws Exception {

        CreateTaskRequest request = new CreateTaskRequest();
        request.setUserId(userId);
        request.setTitle("Original task");
        request.setDescription("Test description");
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        MvcResult result = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TaskResponse createdTask = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TaskResponse.class
        );

        Long taskId = createdTask.getId();

        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        CountDownLatch ready = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Boolean> updateTask = () -> {
            try {
                return transactionTemplate.execute(status ->{
                    Task task = taskRepository.findById(taskId)
                            .orElseThrow();

                    task.setTitle(Thread.currentThread().getName());
                    ready.countDown();
                    try {
                        if(!ready.await(10, TimeUnit.SECONDS)){
                            throw new IllegalStateException(
                                    "Threads did not reach the synchronization point"
                            );
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                    taskRepository.flush();
                    return true;
                });
            } catch (OptimisticLockingFailureException
                     | OptimisticLockException e) {
                return false;
            }
        };
        try {
            Future<Boolean> first = executor.submit(updateTask);
            Future<Boolean> second = executor.submit(updateTask);

            boolean firstResult = first.get(20, TimeUnit.SECONDS);
            boolean secondResult = second.get(20, TimeUnit.SECONDS);

            assertNotEquals(firstResult, secondResult);

            Task updatedTask = taskRepository.findById(taskId)
                    .orElseThrow();

            assertEquals(1L, updatedTask.getVersion());

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldNotExceedActiveTaskLimitWhenCreatingConcurrently() throws Exception {
        for (int i = 0; i < 4; i++) {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setUserId(userId);
            request.setTitle("Task " + i);
            request.setDescription("Test");
            request.setPriority(TaskPriority.HIGH);
            request.setDueDate(LocalDate.now().plusDays(7));

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
        List<TaskStatus> activeStatuses = List.of(
                TaskStatus.TODO,
                TaskStatus.IN_PROGRESS
        );
        assertEquals(4, taskRepository.countByUserIdAndStatusIn(
                userId, activeStatuses
        ));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> createTask = () -> {
            CreateTaskRequest request = new CreateTaskRequest();
            request.setUserId(userId);
            request.setTitle("Concurrent task");
            request.setDescription("Test");
            request.setPriority(TaskPriority.HIGH);
            request.setDueDate(LocalDate.now().plusDays(7));

            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start timeout");
            }
            return mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };
        try {
            Future<Integer> first = executor.submit(createTask);
            Future<Integer> second = executor.submit(createTask);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int firstStatus = first.get(20, TimeUnit.SECONDS);
            int secondStatus = second.get(20, TimeUnit.SECONDS);

            assertEquals(
                    List.of(201, 409),
                    Stream.of(firstStatus, secondStatus).sorted().toList()
            );

            assertEquals(5, taskRepository.countByUserIdAndStatusIn(
                    userId, activeStatuses
            ));

        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
