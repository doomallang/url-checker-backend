package com.doomole.uptime.service;

import com.doomole.uptime.entity.User;
import com.doomole.uptime.exception.ClientErrorException;
import com.doomole.uptime.repo.UserRepo;
import com.doomole.uptime.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String signup(String email, String rawPassword, String displayName) {
        if (userRepo.existsByEmail(email)) {
            throw new ClientErrorException("이미 존재하는 이메일입니다.");
        }
        log.info(email, rawPassword, displayName);
        String hash = passwordEncoder.encode(rawPassword);
        User user = userRepo.save(User.builder()
                .email(email)
                .password(hash)
                .displayName(displayName)
                .createdAt(LocalDateTime.now())
                .build());
        return jwtUtil.generate(user.getId(), user.getEmail());
    }

    public String login(String email, String rawPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ClientErrorException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ClientErrorException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return jwtUtil.generate(user.getId(), user.getEmail());
    }
}
