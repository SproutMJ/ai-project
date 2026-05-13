package org.mj.trip.auth.controller;

import jakarta.validation.Valid;
import org.mj.trip.auth.dto.LoginRequest;
import org.mj.trip.auth.dto.LoginResponse;
import org.mj.trip.auth.service.AuthService;
import org.mj.trip.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
