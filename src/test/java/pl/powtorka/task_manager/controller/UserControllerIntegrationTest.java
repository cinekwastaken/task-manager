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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.powtorka.task_manager.config.IntegrationTestConfig;
import pl.powtorka.task_manager.dto.CreateTaskRequest;
import pl.powtorka.task_manager.dto.CreateUserRequest;
import pl.powtorka.task_manager.dto.UserResponse;
import pl.powtorka.task_manager.enums.TaskPriority;
import pl.powtorka.task_manager.enums.TaskStatus;
import pl.powtorka.task_manager.repository.TaskRepository;
import pl.powtorka.task_manager.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Stream;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;




@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
public class UserControllerIntegrationTest {



    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void cleanUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        Cache usersCache = cacheManager.getCache("users");
        Cache tasksCache = cacheManager.getCache("tasks");

        if (usersCache != null) {
            usersCache.clear();
        }

        if (tasksCache != null) {
            tasksCache.clear();
        }
    }


    @Test
    void shouldCreateUser() throws Exception {


        CreateUserRequest user = new CreateUserRequest();
        user.setFirstName("Cinek");
        user.setLastName("Luki");
        user.setEmail("Cinek@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void shouldReturnBadRequestWhenCreateUserRequestIsInvalid() throws Exception{

        CreateUserRequest user = new CreateUserRequest();
        user.setFirstName("");
        user.setLastName("");
        user.setEmail("siema");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists());

    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception{
        CreateUserRequest user = new CreateUserRequest();
        user.setFirstName("Cinek");
        user.setLastName("Luki");
        user.setEmail("Cinek@example.com");

        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        CreateUserRequest user1 = new CreateUserRequest();
        user1.setFirstName("Cinek");
        user1.setLastName("Luki");
        user1.setEmail("Cinek@example.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));

    }

    @Test
    void shouldGetUserById() throws Exception{
        CreateUserRequest user = new CreateUserRequest();
        user.setFirstName("Cinek");
        user.setLastName("Luki");
        user.setEmail("Cinek@example.com");

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        UserResponse createdUser = objectMapper.readValue(responseBody, UserResponse.class);

        mockMvc.perform(get("/users/" + createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdUser.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExists() throws Exception{
        mockMvc.perform(get("/users/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void shouldReturnConflictWhenDeletingUserWithTasks() throws Exception{
        CreateUserRequest user = new CreateUserRequest();
        user.setFirstName("Cinek");
        user.setLastName("Luki");
        user.setEmail("Cinek@example.com");
      MvcResult result=  mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();

        UserResponse createdUser = objectMapper.readValue(responseBody,UserResponse.class);

        Long userId = createdUser.getId();



        CreateTaskRequest task = new CreateTaskRequest();
        task.setUserId(userId);
        task.setTitle("test");
        task.setDescription("test");
        task.setPriority(TaskPriority.HIGH);
        task.setDueDate(LocalDate.now());
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task)))
                        .andExpect(status().isCreated());

        mockMvc.perform(delete("/users/" + userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cannot delete user with assigned tasks"));
    }

    @Test
    void shouldAllowOnlyOneUserWIthSameEmailWhenRequestsAreConcurrent() throws Exception{
        String email = UUID.randomUUID() + "@example.com";

        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("Cinek");
        request.setLastName("Luki");
        request.setEmail(email);

        String requestBody = objectMapper.writeValueAsString(request);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Callable<Integer> createUser = () -> {
                start.await();

                return mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };

            Future<Integer> first = executor.submit(createUser);
            Future<Integer> second = executor.submit(createUser);

            start.countDown();

            int firstStatus = first.get(10, TimeUnit.SECONDS);
            int secondStatus = second.get(10, TimeUnit.SECONDS);

            assertEquals(1,
                    Stream.of(firstStatus, secondStatus)
                            .filter(status -> status == 201)
                            .count());

            assertEquals(1,
                    Stream.of(firstStatus, secondStatus)
                            .filter(status -> status == 409)
                            .count());

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE email = ?",
                    Integer.class,
                    email
            );

            assertEquals(1, count);

        } finally {
            executor.shutdownNow();
        }

    }

}
