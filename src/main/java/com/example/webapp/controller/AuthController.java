package com.example.webapp.controller;

import org.springframework.web.bind.annotation.RestController;
import com.example.webapp.entity.User;
import com.example.webapp.repository.UserRepository;
import com.example.webapp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	// Register a new user
	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
		String email = payload.get("email");
		String password = payload.get("password");
		String username = payload.get("username");
		String name = payload.get("name");
		
		if (userRepository.findByEmail(email).isPresent()) {
			Map<String, String> error = new HashMap<>();
			error.put("error", "Email already registered");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
		}
		
		User user = User.builder()
			.email(email)
			.username(username != null ? username : email)
			.name(name != null ? name : "User")
			.passwordHash(passwordEncoder.encode(password))
			.createdAt(LocalDateTime.now())
			.build();
		
		userRepository.save(user);
		
		Map<String, String> response = new HashMap<>();
		response.put("message", "User registered successfully");
		return ResponseEntity.ok(response);
	}

	// Login endpoint
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
		String email = payload.get("email");
		String password = payload.get("password");
		
		Optional<User> userOpt = userRepository.findByEmail(email);
		
		if (userOpt.isEmpty()) {
			Map<String, String> error = new HashMap<>();
			error.put("error", "Invalid email or password");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
		}
		
		User user = userOpt.get();
		
		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			Map<String, String> error = new HashMap<>();
			error.put("error", "Invalid email or password");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
		}
		
		// Generate JWT token
		String token = jwtUtil.generateTokenFromUsername(user.getEmail());
		
		Map<String, Object> response = new HashMap<>();
		response.put("token", token);
		response.put("user", Map.of(
			"id", user.getId(),
			"email", user.getEmail(),
			"username", user.getUsername(),
			"name", user.getName()
		));
		
		return ResponseEntity.ok(response);
	}
}
