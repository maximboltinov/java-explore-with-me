package ru.practicum.ewm.mainservice.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.mainservice.dto.user.NewUserRequest;
import ru.practicum.ewm.mainservice.dto.user.UserDto;
import ru.practicum.ewm.mainservice.exception.custom.ObjectNotFoundExceptionCust;
import ru.practicum.ewm.mainservice.service.UserService;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserControllerAdmin.class)
class UserControllerAdminTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @SneakyThrows
    @Test
    void create_correct() {
        final NewUserRequest newUserRequest = new NewUserRequest("email@email.com", "name name");

        when(userService.create(newUserRequest))
                .thenAnswer(invocationOnMock -> {
                    NewUserRequest request = invocationOnMock.getArgument(0);
                    return new UserDto(1L, request.getName(), request.getEmail());
                });

        mockMvc.perform(post("/admin/users")
                        .content(objectMapper.writeValueAsString(newUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1L), Long.class))
                .andExpect(jsonPath("$.name", is("name name"), String.class))
                .andExpect(jsonPath("$.email", is("email@email.com"), String.class));

        verify(userService).create((newUserRequest));
    }

    @SneakyThrows
    @Test
    void create_incorrectBody_badRequest() {
        final NewUserRequest newUserRequest = NewUserRequest.builder()
                .name("name name")
                .email("emailemail")
                .build();

        mockMvc.perform(post("/admin/users")
                        .content(objectMapper.writeValueAsString(newUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(userService, never()).create(any(NewUserRequest.class));
    }

    @SneakyThrows
    @Test
    void create_userWithBusyEmailAddress_conflict() {
        when(userService.create(any(NewUserRequest.class)))
                .thenThrow(new DataIntegrityViolationException(""));

        final NewUserRequest newUserRequest = NewUserRequest.builder()
                .name("name name")
                .email("email@email.com")
                .build();

        mockMvc.perform(post("/admin/users")
                        .content(objectMapper.writeValueAsString(newUserRequest))
                        .characterEncoding(StandardCharsets.UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @SneakyThrows
    @Test
    void getUsers_incorrectParams_badRequest() {
        mockMvc.perform(get("/admin/users")
                        .param("from", "-1")
                        .param("size", "1")
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void deleteUser_incorrectPathVariableUserId_badRequest() {
        mockMvc.perform(delete("/admin/users/{userId}", 0)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void deleteUser_userNotFoundById_notFound() {
        doThrow(new ObjectNotFoundExceptionCust("")).when(userService).deleteUser(anyLong());

        mockMvc.perform(delete("/admin/users/{userId}", 1)
                        .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().isNotFound());
    }
}