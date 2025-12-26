package com.salonplatform.controller;


import com.salonplatform.dto.ApiResponse;
import com.salonplatform.dto.AuthResponse;
import com.salonplatform.dto.LoginRequest;
import com.salonplatform.dto.SignUpRequest;
import com.salonplatform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register new user")
    public ResponseEntity<ApiResponse<AuthResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        AuthResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.loginUser(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token.replace("Bearer ", ""));
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestHeader("Authorization") String token) {
        AuthResponse response = authService.refreshToken(token.replace("Bearer ", ""));
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify account")
    public ResponseEntity<ApiResponse<Void>> verifyAccount(@RequestParam UUID userId, @RequestParam String code) {
        authService.verifyAccount(userId, code);
        return ResponseEntity.ok(ApiResponse.success("Account verified successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String phone) {
        authService.requestPasswordReset(phone);
        return ResponseEntity.ok(ApiResponse.success("Password reset instructions sent"));
    }

    @GetMapping("/check-phone")
    @Operation(summary = "Check phone availability")
    public ResponseEntity<ApiResponse<Boolean>> checkPhoneAvailability(@RequestParam String phone) {
        boolean available = authService.isPhoneAvailable(phone);
        return ResponseEntity.ok(ApiResponse.success("Phone availability checked", available));
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check email availability")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(ApiResponse.success("Email availability checked", available));
    }
}