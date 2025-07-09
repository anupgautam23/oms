package com.oms.notification.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client for communicating with Auth Service to get user details
 */
@Component
public class UserServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceClient.class);
    
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    private final RestTemplate restTemplate;
    
    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Get user details by user ID
     * @param userId User ID
     * @return UserDetails or null if not found
     */
    public UserDetails getUserById(Long userId) {
        try {
            String url = authServiceUrl + "/api/auth/users/" + userId;
            ResponseEntity<UserDetails> response = restTemplate.getForEntity(url, UserDetails.class);
            return response.getBody();
        } catch (Exception e) {
            logger.error("Failed to get user details for user ID {}: {}", userId, e.getMessage());
            return null;
        }
    }
    
    /**
     * User details DTO
     */
    public static class UserDetails {
        @JsonProperty("id")
        private Long id;
        
        @JsonProperty("username")
        private String username;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("isActive")
        private boolean isActive;
        
        // Constructors
        public UserDetails() {}
        
        public UserDetails(Long id, String username, String email, boolean isActive) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.isActive = isActive;
        }
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
    }
}