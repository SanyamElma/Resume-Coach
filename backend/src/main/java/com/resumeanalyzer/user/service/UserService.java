package com.resumeanalyzer.user.service;

import com.resumeanalyzer.common.exception.ResourceNotFoundException;
import com.resumeanalyzer.user.domain.User;
import com.resumeanalyzer.user.dto.UpdateProfileRequest;
import com.resumeanalyzer.user.dto.UserDto;
import com.resumeanalyzer.user.mapper.UserMapper;
import com.resumeanalyzer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Read/update operations on the authenticated user's own profile. */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        return userMapper.toDto(getUser(userId));
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.setName(request.name().trim());
        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
