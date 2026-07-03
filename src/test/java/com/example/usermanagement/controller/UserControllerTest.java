package com.example.usermanagement.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.usermanagement.dto.UserDTO;
import com.example.usermanagement.exception.GlobalExceptionHandler;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void createUserShouldReturnCreatedUser() throws Exception {
        UserDTO request = UserDTO.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();
        UserDTO response = UserDTO.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();

        when(userService.createUser(any(UserDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void createUserShouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        UserDTO request = UserDTO.builder()
                .name("")
                .email("not-an-email")
                .age(16)
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.validationErrors.email").value("Email must be a valid email address"))
                .andExpect(jsonPath("$.validationErrors.age").value("Age must be at least 18"));
    }

    @Test
    void getUserByIdShouldReturnUser() throws Exception {
        UserDTO response = UserDTO.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();

        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    void getUserByIdShouldReturnNotFoundWhenMissing() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found with id: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99"));
    }

    @Test
    void getAllUsersShouldReturnPagedResponse() throws Exception {
        UserDTO response = UserDTO.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();

        when(userService.getAllUsers(PageRequest.of(0, 5, Sort.by("name").descending())))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 5, Sort.by("name").descending()), 1));

        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "name")
                        .param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void updateUserShouldReturnUpdatedUser() throws Exception {
        UserDTO request = UserDTO.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(29)
                .build();
        UserDTO response = UserDTO.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .age(29)
                .build();

        when(userService.updateUser(eq(1L), any(UserDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void deleteUserShouldReturnNoContent() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }
}