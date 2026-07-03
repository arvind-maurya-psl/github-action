package com.example.usermanagement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.usermanagement.dto.UserDTO;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.DuplicateResourceException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();

        userDTO = UserDTO.builder()
                .name("John Doe")
                .email("john@example.com")
                .age(30)
                .build();
    }

    @Test
    void createUserShouldPersistUserWhenEmailIsUnique() {
        when(userRepository.findByEmail(userDTO.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDTO createdUser = userService.createUser(userDTO);

        assertThat(createdUser.getId()).isEqualTo(1L);
        assertThat(createdUser.getEmail()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserShouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.findByEmail(userDTO.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.createUser(userDTO))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@example.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserByIdShouldReturnUserWhenPresent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDTO foundUser = userService.getUserById(1L);

        assertThat(foundUser.getName()).isEqualTo("John Doe");
    }

    @Test
    void getUserByIdShouldThrowExceptionWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 99");
    }

    @Test
    void getAllUsersShouldReturnPagedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserDTO> users = userService.getAllUsers(pageable);

        assertThat(users.getTotalElements()).isEqualTo(1);
        assertThat(users.getContent()).hasSize(1);
        assertThat(users.getContent().get(0).getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void updateUserShouldPersistChangesWhenUserExists() {
        UserDTO updateRequest = UserDTO.builder()
                .name("Jane Doe")
                .email("jane@example.com")
                .age(28)
                .build();
        User updatedUser = User.builder()
                .id(1L)
                .name("Jane Doe")
                .email("jane@example.com")
                .age(28)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(updateRequest.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(updatedUser);

        UserDTO result = userService.updateUser(1L, updateRequest);

        assertThat(result.getName()).isEqualTo("Jane Doe");
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void updateUserShouldThrowExceptionWhenUserMissing() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(42L, userDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 42");
    }

    @Test
    void deleteUserShouldRemoveUserWhenPresent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserShouldThrowExceptionWhenUserMissing() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: 7");
    }
}