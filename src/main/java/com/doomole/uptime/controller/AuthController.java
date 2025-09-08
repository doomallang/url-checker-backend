package com.doomole.uptime.controller;

import com.doomole.uptime.dto.TokenResponse;
import com.doomole.uptime.dto.UserRequest;
import com.doomole.uptime.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@RequestBody UserRequest userRequest) {
        String token = userService.signup(userRequest.email(), userRequest.password(), userRequest.displayName());

        return ResponseEntity.ok(new TokenResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody UserRequest userRequest) {
        String token = userService.login(userRequest.email(), userRequest.password());

        return ResponseEntity.ok(new TokenResponse(token));
    }
}
