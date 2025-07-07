package com.oms.auth.controller;

import com.oms.auth.dto.AuthResponseDto;
import com.oms.auth.dto.LoginRequestDto;
import com.oms.auth.dto.RegisterRequestDto;
import com.oms.auth.dto.UserDetailsDto;
import com.oms.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles user authentication, registration, and user profile operations
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    /**
     * Register a new user
     * @param request Registration request containing username, email, and password
     * @return AuthResponseDto with JWT token and user details
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Login user
     * @param request Login request containing username/email and password
     * @return AuthResponseDto with JWT token and user details
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current user details
     * @param request HTTP request containing JWT token in Authorization header
     * @return UserDetailsDto with current user information
     */
    @GetMapping("/me")
    public ResponseEntity<UserDetailsDto> getCurrentUser(HttpServletRequest request) {
        // Get the JWT token from the Authorization header
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Get user details using the token
        UserDetailsDto userDetails = authService.getCurrentUserDetails(token);
        return ResponseEntity.ok(userDetails);
    }
    
    /**
     * Alternative approach using Spring Security Context
     * Get current authenticated user details
     * @return UserDetailsDto with current user information
     */
    @GetMapping("/me/profile")
    public ResponseEntity<UserDetailsDto> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Get username from authentication principal
        String username = authentication.getName();
        UserDetailsDto userDetails = authService.getUserDetailsByUsername(username);
        
        return ResponseEntity.ok(userDetails);
    }
    
    /**
     * Validate JWT token
     * @param request HTTP request containing JWT token
     * @return Success message if token is valid
     */
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No token provided");
        }
        
        boolean isValid = authService.validateToken(token);
        
        if (isValid) {
            return ResponseEntity.ok("Token is valid");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }
    }
    
    /**
     * Refresh JWT token
     * @param request HTTP request containing current JWT token
     * @return AuthResponseDto with new JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refreshToken(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        AuthResponseDto response = authService.refreshToken(token);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint
     * @return Success message
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running!");
    }
    
    /**
     * Extract JWT token from Authorization header
     * @param request HTTP request
     * @return JWT token without "Bearer " prefix, or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }
}