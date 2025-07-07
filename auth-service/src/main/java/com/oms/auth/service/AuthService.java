package com.oms.auth.service;

import com.oms.auth.dto.AuthResponseDto;
import com.oms.auth.dto.LoginRequestDto;
import com.oms.auth.dto.RegisterRequestDto;
import com.oms.auth.dto.UserDetailsDto;
import com.oms.auth.entity.User;
import com.oms.auth.exception.UserAlreadyExistsException;
import com.oms.auth.exception.UserNotFoundException;
import com.oms.auth.repository.UserRepository;
import com.oms.auth.util.JwtUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    
    public AuthService(UserRepository userRepository, 
                      @Lazy PasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil,
                      @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }
    
    public AuthResponseDto register(RegisterRequestDto request) {
        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Save user to database
        User savedUser = userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(savedUser);
        
        return new AuthResponseDto(
            token,
            savedUser.getId(),
            savedUser.getUsername(),
            savedUser.getEmail()
        );
    }
    
    public AuthResponseDto login(LoginRequestDto request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrEmail(),
                request.getPassword()
            )
        );
        
        // Get user details
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsernameOrEmail(
            request.getUsernameOrEmail(),
            request.getUsernameOrEmail()
        ).orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Update last login time
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate JWT token
        String token = jwtUtil.generateToken(userDetails);
        
        return new AuthResponseDto(
            token,
            user.getId(),
            user.getUsername(),
            user.getEmail()
        );
    }
    
    /**
     * Get current user details using JWT token
     * @param token JWT token
     * @return UserDetailsDto with user information
     */
    public UserDetailsDto getCurrentUserDetails(String token) {
        try {
            // Validate token first
            if (!jwtUtil.isTokenValid(token)) {
                throw new RuntimeException("Invalid or expired token");
            }
            
            // Extract username from token
            String username = jwtUtil.extractUsername(token);
            
            // Get user from database
            User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            return convertToUserDetailsDto(user);
            
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving user details: " + e.getMessage());
        }
    }
    
    /**
     * Get user details by username
     * @param username Username
     * @return UserDetailsDto with user information
     */
    public UserDetailsDto getUserDetailsByUsername(String username) {
        User user = userRepository.findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
        
        return convertToUserDetailsDto(user);
    }
    
    /**
     * Validate JWT token
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        return jwtUtil.isTokenValid(token);
    }
    
    /**
     * Refresh JWT token
     * @param token Current JWT token
     * @return AuthResponseDto with new token
     */
    public AuthResponseDto refreshToken(String token) {
        try {
            // Validate current token
            if (!jwtUtil.isTokenValid(token)) {
                throw new RuntimeException("Invalid or expired token");
            }
            
            // Extract username from token
            String username = jwtUtil.extractUsername(token);
            
            // Get user from database
            User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            // Generate new token
            String newToken = jwtUtil.generateToken(user);
            
            return new AuthResponseDto(
                newToken,
                user.getId(),
                user.getUsername(),
                user.getEmail()
            );
            
        } catch (Exception e) {
            throw new RuntimeException("Error refreshing token: " + e.getMessage());
        }
    }
    
    /**
     * Convert User entity to UserDetailsDto
     * @param user User entity
     * @return UserDetailsDto
     */
    private UserDetailsDto convertToUserDetailsDto(User user) {
        return new UserDetailsDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.isActive(),
            user.getLastLoginAt()
        );
    }
    
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail));
    }
}