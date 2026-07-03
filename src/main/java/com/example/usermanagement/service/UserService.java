package com.example.usermanagement.service;

import com.example.usermanagement.dto.UserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserDTO createUser(UserDTO userDTO);

    UserDTO getUserById(Long id);

    Page<UserDTO> getAllUsers(Pageable pageable);

    UserDTO updateUser(Long id, UserDTO userDTO);

    void deleteUser(Long id);
}