package com.colorgame.backend.service;

import com.colorgame.backend.dto.UserDTO;
import com.colorgame.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(u -> new UserDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getCreatedAt()))
                .toList();
    }
}