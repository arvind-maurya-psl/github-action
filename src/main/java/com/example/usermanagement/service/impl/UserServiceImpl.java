package com.example.usermanagement.service.impl;

import com.example.usermanagement.dto.UserDTO;
import com.example.usermanagement.entity.User;
import com.example.usermanagement.exception.DuplicateResourceException;
import com.example.usermanagement.exception.ResourceNotFoundException;
import com.example.usermanagement.repository.UserRepository;
import com.example.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        log.info("Creating user with email {}", userDTO.getEmail());
        userRepository.findByEmail(userDTO.getEmail())
                .ifPresent(existingUser -> {
                    throw new DuplicateResourceException("User with email already exists: " + userDTO.getEmail());
                });

        User savedUser = userRepository.save(toEntity(userDTO));
        return toDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        log.debug("Fetching user by id {}", id);
        User user = findUserById(id);
        return toDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.debug("Fetching users page {} with size {}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable).map(this::toDTO);
    }

    @Override
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        log.info("Updating user {}", id);
        User existingUser = findUserById(id);

        userRepository.findByEmail(userDTO.getEmail())
                .filter(user -> !user.getId().equals(id))
                .ifPresent(user -> {
                    throw new DuplicateResourceException("User with email already exists: " + userDTO.getEmail());
                });

        existingUser.setName(userDTO.getName());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setAge(userDTO.getAge());

        User updatedUser = userRepository.save(existingUser);
        return toDTO(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user {}", id);
        User user = findUserById(id);
        userRepository.delete(user);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .age(user.getAge())
                .build();
    }

    private User toEntity(UserDTO userDTO) {
        return User.builder()
                .id(userDTO.getId())
                .name(userDTO.getName())
                .email(userDTO.getEmail())
                .age(userDTO.getAge())
                .build();
    }
}