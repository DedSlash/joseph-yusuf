package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.PageResponse;
import com.josephyusuf.auth.dto.UserDto;
import com.josephyusuf.auth.dto.UserMapper;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.exception.UserNotFoundException;
import com.josephyusuf.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserManagementService service;

    private UUID userId;
    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("user@test.com")
                .password("hashed")
                .firstName("Test")
                .lastName("User")
                .plan(Plan.FREE)
                .role(Role.USER)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
        userDto = UserDto.builder()
                .id(userId)
                .email("user@test.com")
                .plan(Plan.FREE)
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("listUsers - returns paginated response")
    void listUsers_paginated() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(userMapper.toDto(user)).thenReturn(userDto);

        PageResponse<UserDto> response = service.listUsers(0, 20, null, null, null);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getUser - throws UserNotFoundException if missing")
    void getUser_notFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("updatePlan - updates plan and returns dto")
    void updatePlan_nominal() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        service.updatePlan(userId, Plan.PREMIUM);

        assertThat(user.getPlan()).isEqualTo(Plan.PREMIUM);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("setEnabled - toggles enabled")
    void setEnabled_disable() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        service.setEnabled(userId, false);

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("updateRole - promotes user to ADMIN")
    void updateRole_promote() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        service.updateRole(userId, Role.ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("deleteUser - removes user")
    void deleteUser_nominal() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.deleteUser(userId);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("updatePlanInternal - updates plan without returning dto")
    void updatePlanInternal_nominal() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        service.updatePlanInternal(userId, Plan.PREMIUM_PLUS);

        assertThat(user.getPlan()).isEqualTo(Plan.PREMIUM_PLUS);
        verify(userRepository).save(user);
    }
}
