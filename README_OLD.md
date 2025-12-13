# Spring Boot Web Application with JWT Authentication

A secure web application built with Spring Boot 3.2.1, Java 21, Spring Security, and JWT (JSON Web Tokens).

## Features

- **User Authentication**: Register and login with username/password
- **JWT-based Security**: Stateless API authentication using JWT tokens
- **Session-based Authentication**: Session management for traditional form login
- **Password Encryption**: BCrypt password hashing
- **Role-based Access Control**: Support for user roles
- **Input Validation**: Request validation with detailed error messages
- **Global Exception Handling**: Centralized error handling with JSON responses
- **Security Headers**: Content Security Policy (CSP) for XSS protection
- **SQL Injection Prevention**: JPA parameterized queries throughout

## Project Structure

```
src/
├── main/
│   ├── java/com/example/webapp/
│   │   ├── WebappApplication.java              # Main Spring Boot application
│   │   ├── controller/
│   │   │   ├── AuthController.java             # Register & Login endpoints
│   │   │   ├── HomeController.java             # Public endpoints
│   │   │   └── UserController.java             # Protected user endpoints
│   │   ├── dto/
│   │   │   ├── RegisterRequest.java            # Registration request DTO
│   │   │   ├── LoginRequest.java               # Login request DTO
│   │   │   ├── AuthResponse.java               # JWT response DTO
│   │   │   ├── UserResponse.java               # User data response DTO
│   │   │   └── ErrorResponse.java              # Error response DTO
│   │   ├── entity/
│   │   │   └── User.java                       # User JPA entity
│   │   ├── repository/
│   │   │   └── UserRepository.java             # User data access layer
│   │   ├── security/
│   │   │   ├── SecurityConfig.java             # Spring Security configuration
│   │   │   ├── JwtUtil.java                    # JWT token utility
│   │   │   ├── JwtAuthenticationFilter.java    # JWT validation filter
│   │   │   ├── CustomUserDetailsService.java   # User details service
│   │   │   └── PasswordEncoderConfig.java      # Password encoder bean
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java     # Global exception handling
│   └── resources/
│       └── application.properties               # Application configuration
└── test/
    └── java/com/example/webapp/
        └── WebappApplicationTests.java         # Test cases
```

## Prerequisites

- Java 21 or higher
- Maven 3.8.0 or higher

## Getting Started

### 1. Navigate to Project

```bash
cd "e:\Desktop\112 Project"
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`

## API Endpoints

### Authentication Endpoints

#### Register User
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePassword123!"
}

Response: 201 Created
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

#### Login (JWT)
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePassword123!"
}

Response: 200 OK
{
  "jwt": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer"
}
```

### Protected Endpoints

#### Get Current User
```
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

Response: 200 OK
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

### Public Endpoints

#### Home
```
GET /
Response: 200 OK - Welcome to Spring Boot Web Application!
```

#### API Hello
```
GET /api/hello
Response: 200 OK - Hello from Spring Boot!
```

## Security Configuration

### JWT Authentication
- **Header**: `Authorization: Bearer <token>`
- **Secret Key**: Configured in `application.properties` (jwt.secret)
- **Expiration**: 24 hours (configurable via jwt.expirationMs)
- **Algorithm**: HS512

### Form-based Authentication
- Traditional session-based login using Spring Security's formLogin()
- CSRF protection enabled for form submissions
- Session creation policy: IF_REQUIRED

### Password Security
- **Algorithm**: BCrypt
- **Strength**: Default (10 rounds)

### CSRF Protection
- Enabled globally
- Disabled for stateless JWT endpoints (`/api/auth/**`)

### Security Headers
- **Content-Security-Policy**: Prevents XSS attacks
  ```
  default-src 'self'; 
  script-src 'self' 'unsafe-inline'; 
  style-src 'self' 'unsafe-inline'
  ```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# JWT Configuration
jwt.secret=mySecretKeyForJWTThatShouldBeAtLeast32CharactersLongForHS512Algorithm
jwt.expirationMs=86400000  # 24 hours in milliseconds

# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=

# Logging
logging.level.com.example.webapp=DEBUG
```

## Database

### H2 Console
Access the in-memory H2 database console at:
```
http://localhost:8080/h2-console
```

**Credentials:**
- URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

### User Table Schema
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT,
    role VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Error Handling

All endpoints return consistent JSON error responses:

```json
{
  "message": "Error description",
  "timestamp": "2024-12-06T08:30:00",
  "status": 400
}
```

### Common HTTP Status Codes
- `200 OK`: Request successful
- `201 Created`: Resource created (registration)
- `400 Bad Request`: Invalid request body or validation error
- `401 Unauthorized`: Invalid credentials or missing JWT token
- `403 Forbidden`: Access denied to resource
- `404 Not Found`: Resource not found
- `409 Conflict`: Duplicate username or email
- `500 Internal Server Error`: Server error

## Testing

### Run Tests
```bash
mvn test
```

### Example cURL Commands

**Register:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"Pass123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"Pass123"}'
```

**Access Protected Endpoint:**
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

## Security Best Practices

1. **Change JWT Secret**: Update `jwt.secret` to a strong, random value in production
2. **HTTPS**: Always use HTTPS in production
3. **Token Expiration**: Configure shorter expiration times for sensitive operations
4. **Password Policy**: Enforce strong password requirements
5. **Rate Limiting**: Implement rate limiting on authentication endpoints
6. **Logging**: Monitor authentication attempts and security-related events
7. **CORS**: Configure CORS appropriately for your frontend
8. **Dependency Updates**: Keep Spring Boot and security dependencies updated

## Technologies

- **Java 21**: Latest LTS Java version with virtual threads support
- **Spring Boot 3.2.1**: Modern Spring framework for rapid development
- **Spring Security 6**: Comprehensive security framework
- **Spring Data JPA**: Object-relational mapping with Hibernate
- **JJWT 0.12.3**: JSON Web Token creation and validation
- **BCrypt**: Secure password hashing
- **Lombok**: Reduce boilerplate code
- **H2 Database**: Lightweight in-memory database
- **Maven**: Build automation

## Development Tips

1. **Hot Reload**: Spring DevTools enables automatic restart when files change
2. **Logging**: Check logs in `DEBUG` level for detailed security events
3. **H2 Console**: Use H2 console to inspect database state
4. **Validation**: Use `@Valid` annotation on controller methods
5. **DTOs**: Always use DTOs for request/response objects

## Future Enhancements

- [ ] OAuth2 integration (Google, GitHub)
- [ ] Two-factor authentication (2FA)
- [ ] Email verification on registration
- [ ] Password reset functionality
- [ ] User profile updates
- [ ] Refresh token mechanism
- [ ] Rate limiting and throttling
- [ ] Audit logging
- [ ] API documentation with Swagger/OpenAPI

## Troubleshooting

### Port Already in Use
```bash
# Find and kill process on port 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### JWT Token Invalid
- Ensure token is correctly extracted from Authorization header
- Verify `jwt.secret` matches the one used to create the token
- Check token expiration time

### User Not Found
- Verify username/email exists in database
- Check H2 console for data integrity
- Ensure user registration completed successfully

## License

This project is provided as-is for educational purposes.

## Contact & Support

For issues or questions, check the logs and error responses for detailed information.
