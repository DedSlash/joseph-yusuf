package com.josephyusuf.auth.service;

import com.josephyusuf.auth.dto.*;
import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.RefreshToken;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.exception.EmailAlreadyExistsException;
import com.josephyusuf.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Cet email est déjà utilisé");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .plan(Plan.FREE)
                .role(Role.USER)
                .enabled(true)
                .country(request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry() : "SN")
                .currency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency() : "XOF")
                .build();

        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getPlan(), user.getRole(), user.getCountry(), user.getCurrency());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(userMapper.toDto(user))
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Compte désactivé");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Identifiants invalides");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getPlan(), user.getRole(), user.getCountry(), user.getCurrency());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(userMapper.toDto(user))
                .build();
    }

    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getPlan(), user.getRole(), user.getCountry(), user.getCurrency());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenService.deleteByToken(request.getRefreshToken());
    }

    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Utilisateur non trouvé"));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Utilisateur non trouvé"));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName());
        }
        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            user.setCountry(request.getCountry().toUpperCase());
        }
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            user.setCurrency(request.getCurrency().toUpperCase());
        }

        user = userRepository.save(user);
        return userMapper.toDto(user);
    }
}
