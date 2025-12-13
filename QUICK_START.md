# Quick Start Guide - JWT Authentication Web App

## Overview

This Spring Boot application implements a complete authentication system with JWT tokens, user registration, and secure endpoints. All code follows security best practices including password hashing, SQL injection prevention, and CSRF protection.

## Files Created

### Core Application
- **WebappApplication.java** - Spring Boot entry point

### Controllers (API Endpoints)
- **AuthController.java** - POST /api/auth/register, POST /api/auth/login
- **UserController.java** - GET /api/users/me (protected)
- **HomeController.java** - GET /, GET /api/hello (public)

### Security Implementation
- **SecurityConfig.java** - Spring Security configuration with JWT filter chain
- **JwtUtil.java** - JWT token generation and validation (HS512 algorithm)
- **JwtAuthenticationFilter.java** - Extracts and validates JWT from Authorization header
- **CustomUserDetailsService.java** - Loads user details from database
- **PasswordEncoderConfig.java** - BCrypt password encoder bean

### Data Layer
- **User.java** - JPA entity with username, email, passwordHash, roles, createdAt
- **UserRepository.java** - JpaRepository with findByUsername, existsByUsername, existsByEmail

### DTOs (Data Transfer Objects)
- **RegisterRequest.java** - { username, email, password }
- **LoginRequest.java** - { username, password }
- **AuthResponse.java** - { jwt, tokenType }
- **UserResponse.java** - { id, username, email }
- **ErrorResponse.java** - { message, timestamp, status }

### Error Handling
- **GlobalExceptionHandler.java** - Centralized @ControllerAdvice for exception mapping

### Configuration
- **application.properties** - JWT secret, expiration, database, logging

## Authentication Flow

### Registration Flow
1. POST `/api/auth/register` with RegisterRequest
2. Validate username and email uniqueness
3. Hash password with BCrypt
4. Save user to database with ROLE_USER
5. Return 201 with UserResponse

### Login Flow (JWT)
1. POST `/api/auth/login` with LoginRequest
2. Authenticate via AuthenticationManager
3. If valid: generate JWT token and return AuthResponse
4. Client stores JWT token

### Protected Endpoint Access
1. Client sends request with `Authorization: Bearer <jwt_token>`
2. JwtAuthenticationFilter intercepts request
3. Validates JWT signature and expiration
4. Sets UsernamePasswordAuthenticationToken in SecurityContextHolder
5. Request proceeds if valid

## Running the Application

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Test with cURL
curl http://localhost:8080/

# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com","password":"Test123"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123"}'

# Access protected endpoint (use token from login response)
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

## Security Features Implemented

✅ **Password Security**
- BCryptPasswordEncoder with 10 rounds
- Passwords stored as hashes, never in plain text

✅ **JWT Token Security**
- HS512 signature algorithm
- 24-hour expiration
- Token validation on every request

✅ **SQL Injection Prevention**
- JPA parameterized queries throughout
- No string concatenation in queries
- @Query with parameter binding when needed

✅ **CSRF Protection**
- Enabled globally for session-based forms
- Disabled for stateless JWT endpoints

✅ **XSS Protection**
- Content-Security-Policy header
- Restricts script execution

✅ **Input Validation**
- @Valid and @NotBlank annotations
- Detailed error messages
- Email format validation

✅ **Access Control**
- Public endpoints: /api/auth/**, /, /api/hello
- Protected endpoints: /api/users/**, other /api/** routes
- Role-based authorization support

## Key Configuration Values

```properties
# Change these for production:
jwt.secret=mySecretKeyForJWTThatShouldBeAtLeast32CharactersLongForHS512Algorithm
jwt.expirationMs=86400000  # 24 hours

# Database (in-memory for development):
spring.datasource.url=jdbc:h2:mem:testdb

# Logging:
logging.level.com.example.webapp=DEBUG
logging.level.org.springframework.security=DEBUG
```

## Database Access

**H2 Console**: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

## Error Responses

All errors return JSON:
```json
{
  "message": "Username already exists",
  "timestamp": "2024-12-06T08:30:00",
  "status": 409
}
```

## What's Next?

1. Change `jwt.secret` to a strong random value
2. Implement refresh token mechanism for better security
3. Add email verification on registration
4. Add password reset functionality
5. Implement rate limiting on auth endpoints
6. Configure CORS for frontend integration
7. Add more user endpoints (profile updates, etc.)
8. Set up production database (PostgreSQL, MySQL)
9. Deploy with HTTPS
10. Set up monitoring and alerting

## Troubleshooting

**Build fails**: Ensure Java 21+ is installed
```bash
java -version
```

**Port 8080 in use**: Change port in application.properties
```properties
server.port=8081
```

**JWT validation fails**: Verify token format is `Bearer <token>`

**User registration fails**: Check for duplicate username/email in H2 console

## Project Structure Summary

```
Spring Boot Web App (Java 21)
├── REST Controllers
│   ├── Public Auth APIs (register, login)
│   └── Protected User APIs
├── Security Layer
│   ├── JWT Token Generation & Validation
│   ├── Password Hashing (BCrypt)
│   └── Request Filtering & Authorization
├── Data Layer
│   ├── User Entity (JPA)
│   └── User Repository (JPA)
├── DTOs (Request/Response Objects)
├── Exception Handling (Global @ControllerAdvice)
└── Configuration
    ├── Security Settings
    ├── JWT Properties
    └── Database Settings
```

## Files You May Need to Modify

1. **application.properties** - JWT secret, database URL, server port
2. **SecurityConfig.java** - Add/modify endpoints or security rules
3. **User.java** - Add additional user properties
4. **AuthController.java** - Add additional auth logic
5. **.gitignore** - Already configured for Maven/Java

Enjoy building your secure web application!
