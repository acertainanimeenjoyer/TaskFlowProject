# Architecture & Implementation Details

## Security Architecture

### Authentication Flow Diagram

```
User Request
    ↓
JwtAuthenticationFilter
    ├─ Check Authorization Header
    ├─ Extract Bearer Token
    └─ Validate JWT Signature & Expiration
        ├─ Valid → Load UserDetails
        │   ├─ Query UserRepository by username
        │   ├─ Create Spring Security GrantedAuthorities from roles
        │   └─ Set UsernamePasswordAuthenticationToken in SecurityContext
        │
        └─ Invalid → Continue without authentication
    ↓
SecurityConfig Authorization Rules
    ├─ /api/auth/** → Permit all (public)
    ├─ /api/users/** → Require authentication
    ├─ /api/** → Require authentication
    └─ Public resources → Permit all
    ↓
Controller Method Execution
```

### JWT Token Structure

The application uses JJWT library (version 0.12.3) with HS512 (HMAC-SHA512):

```
Header: {
  "alg": "HS512",
  "typ": "JWT"
}

Payload: {
  "sub": "username",        // Subject (username)
  "iat": 1701936600,        // Issued at
  "exp": 1702023000         // Expiration
}

Signature: HMAC-SHA512(header.payload, secret)
```

Encoded format: `eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ...`

## Component Responsibilities

### Controllers
- **AuthController** - Handles /api/auth/** requests
  - Validates RegisterRequest/LoginRequest with @Valid
  - Checks for username/email duplicates in UserRepository
  - Encodes passwords using PasswordEncoder
  - Generates JWT tokens via JwtUtil
  
- **UserController** - Handles /api/users/** requests
  - Returns authenticated user information
  - Works with both JWT and session authentication
  
- **HomeController** - Handles public endpoints
  - No authentication required

### Security Components
- **SecurityConfig** - Defines:
  - AuthenticationManager bean
  - SecurityFilterChain with:
    - Endpoint authorization rules
    - Form login configuration
    - CSRF settings
    - JWT filter insertion point
    - Security headers (CSP)
    
- **JwtUtil** - Responsible for:
  - Token generation from Authentication object
  - Token generation from username string
  - Token validation (signature, expiration)
  - Username extraction from token claims
  
- **JwtAuthenticationFilter** - Implements:
  - OncePerRequestFilter (one execution per request)
  - Authorization header parsing ("Bearer <token>")
  - Token validation delegation to JwtUtil
  - UserDetails loading via CustomUserDetailsService
  - SecurityContext population
  
- **CustomUserDetailsService** - Implements UserDetailsService:
  - Loads User entity from UserRepository
  - Converts roles to Spring GrantedAuthority
  - Returns org.springframework.security.core.userdetails.User

### Data Access Layer
- **UserRepository** - Extends JpaRepository<User, Long>
  - findByUsername() - Optional<User> for login/JWT validation
  - existsByUsername() - Check duplicate on registration
  - existsByEmail() - Check duplicate on registration

### DTOs & Entities
- **RegisterRequest** - Input validation for registration
  - @NotBlank on all fields
  - @Email on email field
  
- **LoginRequest** - Input validation for login
  - @NotBlank on all fields
  
- **AuthResponse** - JWT token response
  - jwt: Bearer token
  - tokenType: "Bearer"
  
- **UserResponse** - Safe user data response
  - id, username, email (NO password hash)
  
- **User Entity** - JPA mapping to database
  - Unique constraints on username and email
  - @ElementCollection for roles (normalized storage)
  - @PrePersist for automatic timestamps
  - @Builder.Default for role initialization

### Exception Handling
- **GlobalExceptionHandler** - @ControllerAdvice handles:
  - MethodArgumentNotValidException → 400 with field errors
  - UsernameNotFoundException → 404
  - AccessDeniedException → 403
  - Generic Exception → 500

## Database Schema

```sql
-- Auto-created by JPA (create-drop DDL mode)

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT uk_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    PRIMARY KEY (user_id, role)
);
```

## Security Implementations

### SQL Injection Prevention
✅ All queries use JPA parameterized methods:
```java
// GOOD - Parameterized query
Optional<User> findByUsername(String username);

// GOOD - JPQL with parameter binding
@Query("SELECT u FROM User u WHERE u.username = :username")
Optional<User> findByUsername(@Param("username") String username);

// BAD - Never do this
Query query = em.createQuery("SELECT * FROM User WHERE username = '" + username + "'");
```

### Password Hashing
✅ BCryptPasswordEncoder with 10 rounds (default strength):
```java
// Registration - Encode before saving
user.setPasswordHash(passwordEncoder.encode(password));
userRepository.save(user);

// Login - AuthenticationManager handles matching
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(username, password)
);
```

### CSRF Protection
✅ Configured in SecurityConfig:
```
- Enabled globally (default)
- Disabled for stateless JWT endpoints: /api/auth/**, /api/users/**
- Form-based login includes CSRF token automatically
```

### JWT Token Security
✅ Best practices implemented:
- HS512 signature algorithm (symmetric)
- Secret key at least 32 characters
- Token stored in Authorization header (not cookies)
- 24-hour expiration
- Token validation on every request

### XSS Protection
✅ Content-Security-Policy header:
```
default-src 'self'          // Only same-origin
script-src 'self' 'unsafe-inline'  // Scripts from self
style-src 'self' 'unsafe-inline'   // Styles from self
```

### Input Validation
✅ Multiple layers:
```java
// Layer 1: Bean Validation (JSR-303)
@NotBlank(message = "Username is required")
private String username;

@Email(message = "Email should be valid")
private String email;

// Layer 2: Controller method
@PostMapping("/register")
public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
    
    // Layer 3: Business logic validation
    if (userRepository.existsByUsername(request.getUsername())) {
        return ResponseEntity.status(HttpStatus.CONFLICT)...
    }
}
```

## Configuration Properties

```properties
# JWT Settings
jwt.secret=<32+ character random string>
jwt.expirationMs=86400000              # 24 hours

# Server
server.port=8080

# Database (H2 in-memory)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# Logging
logging.level.root=INFO
logging.level.com.example.webapp=DEBUG
logging.level.org.springframework.security=DEBUG
```

## Request/Response Examples

### Register
```
Request:
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}

Response (201):
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}

Error Response (409):
{
  "message": "Username already exists",
  "timestamp": "2024-12-06T08:30:00",
  "status": 409
}
```

### Login
```
Request:
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123!"
}

Response (200):
{
  "jwt": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6...",
  "tokenType": "Bearer"
}

Error Response (401):
{
  "message": "Invalid username or password",
  "timestamp": "2024-12-06T08:30:00",
  "status": 401
}
```

### Get Current User
```
Request:
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

Response (200):
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}

Error Response (401):
{
  "message": "Unauthorized",
  "timestamp": "2024-12-06T08:30:00",
  "status": 401
}
```

## Request Processing Flow

```
1. HTTP Request arrives
   ↓
2. JwtAuthenticationFilter.doFilterInternal()
   ├─ Extract JWT from Authorization header
   ├─ Validate token with JwtUtil
   ├─ Load user details
   └─ Set authentication in SecurityContext
   ↓
3. SecurityConfig filters & rules
   ├─ Check CSRF token (if applicable)
   └─ Authorize based on URL pattern
   ↓
4. Controller method execution
   ├─ @Valid validation occurs
   ├─ Business logic processes
   └─ Response DTO created
   ↓
5. GlobalExceptionHandler (if exception)
   └─ Map to appropriate HTTP response
   ↓
6. HTTP Response sent with headers
   ├─ Content-Security-Policy header
   └─ CSRF token (if applicable)
```

## Performance Considerations

- **JWT Stateless**: No database queries for token validation
- **Caching**: Spring Security caches user details in session
- **MongoDB Indexes**: Compound indexes on messages (channelType, channelId, createdAt)
- **WebSocket**: Persistent connections reduce HTTP overhead
- **Single-threaded Filter**: JwtAuthenticationFilter runs once per request
- **Connection Pooling**: Spring manages database connections

## WebSocket Architecture

### Real-time Chat Implementation

#### Connection Flow

```
Client
  ↓
SockJS Connection → /ws/chat
  ↓
STOMP Protocol Handshake
  ├─ Extract Authorization Header: "Bearer <token>"
  ├─ Validate JWT via JwtUtil
  └─ Set UsernamePasswordAuthenticationToken in WebSocket session
      ↓
WebSocketConfig ChannelInterceptor
  ├─ preSend() intercepts CONNECT command
  ├─ Validates token at handshake
  └─ Rejects connection if invalid token
      ↓
Client Connected & Authenticated
  ├─ Can send messages to /app/** destinations
  └─ Can subscribe to /topic/** channels
```

#### Message Flow

```
1. Join Channel:
   Client → /app/chat.join → WebSocketChatController
     ├─ Verify channel access via ChatService
     ├─ Store channel info in session attributes
     ├─ Send recent messages to joining user
     └─ Broadcast join notification to /topic/chat/{type}/{id}

2. Send Message:
   Client → /app/chat.message → WebSocketChatController
     ├─ Validate user access to channel
     ├─ Save message to MongoDB (messages collection)
     └─ Broadcast to /topic/chat/{type}/{id} (all subscribers)

3. Leave Channel:
   Client → /app/chat.leave → WebSocketChatController
     └─ Broadcast leave notification to /topic/chat/{type}/{id}
```

### WebSocket Components

- **WebSocketConfig** - Configures WebSocket endpoints
  - Registers `/ws/chat` STOMP endpoint with SockJS fallback
  - Sets `/app` as application destination prefix
  - Enables simple broker with `/topic` prefix
  - Implements ChannelInterceptor for JWT authentication at handshake
  
- **WebSocketChatController** - Handles STOMP messages
  - `@MessageMapping("/chat.join")` - Join channel, get history
  - `@MessageMapping("/chat.leave")` - Leave channel notification
  - `@MessageMapping("/chat.message")` - Send real-time message
  - Uses SimpMessagingTemplate for broadcasting
  
- **ChatService** - Business logic for chat
  - `getMessageHistory()` - Paginated REST fallback
  - `getRecentMessages()` - Up to 50 recent messages for WebSocket join
  - `saveMessage()` - Persist message to MongoDB
  - `verifyChannelAccess()` - Permission checks (team/task membership)

### Channel Types

**Team Channels** (`channelType: "team"`)
- Broadcast to all team members
- Access: User must be team member
- Use case: Team-wide discussions

**Task Channels** (`channelType: "task"`)
- Broadcast to all project members (task belongs to project)
- Access: User must be project member
- Use case: Task-specific discussions

### Security

**WebSocket Authentication:**
1. Client connects with JWT in Authorization header
2. ChannelInterceptor validates token at STOMP CONNECT
3. Token validation uses existing JwtUtil
4. UserDetails loaded via UserDetailsService
5. Authentication stored in WebSocket session (Principal)
6. All subsequent messages authenticated via Principal

**Message Authorization:**
- Every message send triggers permission check
- ChatService validates user access to channel
- Team channels: `isTeamMember(teamId, userEmail)`
- Task channels: `canAccessTask(taskId, userEmail)`
- Returns error to sender if access denied

### REST Fallback

HTTP endpoints for clients without WebSocket support:
- `GET /api/chat/{channelType}/{channelId}` - Message history (paginated)
- `POST /api/chat/{channelType}/{channelId}` - Send message via REST
- Both require JWT Bearer token
- Same permission checks as WebSocket

## Dependencies & Versions

- **Spring Boot**: 3.2.1
- **Spring Security**: 6.x (included with Spring Boot)
- **Spring WebSocket**: 6.x (included with Spring Boot)
- **JJWT**: 0.12.3 (JWT library)
- **BCrypt**: Included in spring-security-core
- **MongoDB**: 4.11.1 (Spring Data MongoDB)
- **Lombok**: Latest (included)
- **Validation**: Spring Boot Validation Starter

## Deployment Checklist

- [ ] Change jwt.secret to strong random value
- [ ] Set jwt.expirationMs appropriately (shorter for production)
- [ ] Configure production MongoDB cluster
- [ ] Enable HTTPS (WebSocket requires wss:// in production)
- [ ] Set spring.data.mongodb.uri to production cluster
- [ ] Change server.port as needed
- [ ] Configure logging for production
- [ ] Implement rate limiting
- [ ] Add request logging/auditing
- [ ] Set up monitoring and alerting
- [ ] Configure CORS for your frontend (WebSocket origins)
- [ ] Configure WebSocket message size limits
- [ ] Set up load balancer with WebSocket support (sticky sessions)
- [ ] Implement WebSocket reconnection logic in client

