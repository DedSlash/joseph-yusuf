package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.PageResponse;
import com.josephyusuf.auth.dto.UserDto;
import com.josephyusuf.auth.dto.UserMapper;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.exception.UserNotFoundException;
import com.josephyusuf.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listUsers(int page, int size, Plan plan, Boolean enabled, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> usersPage = userRepository.searchUsers(plan, enabled, search, pageable);

        List<UserDto> content = usersPage.getContent().stream()
                .map(userMapper::toDto)
                .toList();

        return PageResponse.<UserDto>builder()
                .content(content)
                .page(usersPage.getNumber())
                .size(usersPage.getSize())
                .totalElements(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getUser(UUID id) {
        return userMapper.toDto(findUser(id));
    }

    @Transactional
    public UserDto updatePlan(UUID id, Plan plan) {
        User user = findUser(id);
        user.setPlan(plan);
        log.info("Plan utilisateur {} modifié → {}", id, plan);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void updatePlanInternal(UUID userId, Plan plan) {
        User user = findUser(userId);
        user.setPlan(plan);
        userRepository.save(user);
        log.info("Plan utilisateur {} mis à jour (interne) → {}", userId, plan);
    }

    @Transactional
    public UserDto setEnabled(UUID id, boolean enabled) {
        User user = findUser(id);
        user.setEnabled(enabled);
        log.info("Utilisateur {} {}", id, enabled ? "activé" : "désactivé");
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateRole(UUID id, Role role) {
        User user = findUser(id);
        user.setRole(role);
        log.info("Rôle utilisateur {} modifié → {}", id, role);
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = findUser(id);
        userRepository.delete(user);
        log.info("Utilisateur {} supprimé (RGPD)", id);
    }

    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    @Transactional(readOnly = true)
    public long countByPlan(Plan plan) {
        return userRepository.countByPlan(plan);
    }

    @Transactional(readOnly = true)
    public long countByEnabled(boolean enabled) {
        return userRepository.countByEnabled(enabled);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return userRepository.count();
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé : " + id));
    }
}
