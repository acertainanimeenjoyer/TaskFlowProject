# API Reference Card

## Quick Reference Guide

### Base URL
```
http://localhost:8080
```

---

## Authentication Endpoints

### Register User
**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePassword123!"
}
```

**Success Response (201 Created):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

**Error Responses:**
- `400 Bad Request` - Validation failed
- `409 Conflict` - Username or email already exists

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","email":"john@example.com","password":"SecurePass123!"}'
```

---

### Login User
**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "SecurePassword123!"
}
```

**Success Response (200 OK):**
```json
{
  "jwt": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTczMzQ5Njg5MCwiZXhwIjoxNzMzNTgzMjkwfQ.xyz...",
  "tokenType": "Bearer"
}
```

**Error Responses:**
- `400 Bad Request` - Validation failed
- `401 Unauthorized` - Invalid credentials

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"SecurePass123!"}'
```

---

## Protected Endpoints

### Get Current User
**Endpoint:** `GET /api/users/me`

**Headers Required:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

**Error Responses:**
- `401 Unauthorized` - Missing or invalid JWT token
- `404 Not Found` - User not found

**cURL Example:**
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

---

## Public Endpoints

### Home
**Endpoint:** `GET /`

**Response:**
```
Welcome to Spring Boot Web Application!
```

---

### Hello
**Endpoint:** `GET /api/hello`

**Response:**
```
Hello from Spring Boot!
```

---

## HTTP Status Codes

| Code | Meaning | Use Cases |
|------|---------|-----------|
| 200 | OK | Successful request |
| 201 | Created | User successfully registered |
| 400 | Bad Request | Invalid request body or validation error |
| 401 | Unauthorized | Invalid credentials or missing JWT |
| 403 | Forbidden | Access denied to resource |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Duplicate username or email |
| 500 | Server Error | Unexpected server error |

---

## Error Response Format

All error responses follow this format:

```json
{
  "message": "Error description",
  "timestamp": "2024-12-06T09:05:00",
  "status": 400
}
```

---

## Common Error Messages

| Message | Cause | Solution |
|---------|-------|----------|
| Username is required | Missing username field | Include username in request |
| Email is required | Missing email field | Include email in request |
| Password is required | Missing password field | Include password in request |
| Email should be valid | Invalid email format | Use valid email format |
| Username already exists | Username taken | Choose different username |
| Email already exists | Email already registered | Use different email |
| Invalid username or password | Wrong credentials | Check username and password |
| Unauthorized | Missing JWT token | Include Authorization header |

---

## JWT Token Structure

### How to Use Token

1. **Receive token from login:**
   ```json
   { "jwt": "eyJhbGciOiJIUzUxMiJ9...", "tokenType": "Bearer" }
   ```

2. **Include in protected requests:**
   ```
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   ```

### Token Expiration
- Default: 24 hours (86400000 ms)
- Configurable in `application.properties`: `jwt.expirationMs`

### Decode Token
Visit https://jwt.io and paste your token to see:
- **Header:** Algorithm (HS512) and type (JWT)
- **Payload:** Username and expiration time
- **Signature:** Cryptographic signature

---

## Request Examples

### JavaScript/Fetch

```javascript
// Register
const registerRes = await fetch('/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'john_doe',
    email: 'john@example.com',
    password: 'SecurePass123!'
  })
});
const user = await registerRes.json();

// Login
const loginRes = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'john_doe',
    password: 'SecurePass123!'
  })
});
const { jwt } = await loginRes.json();

// Protected request
const userRes = await fetch('/api/users/me', {
  method: 'GET',
  headers: { 'Authorization': `Bearer ${jwt}` }
});
const userData = await userRes.json();
```

### Python/Requests

```python
import requests

BASE = 'http://localhost:8080'

# Register
reg = requests.post(f'{BASE}/api/auth/register', json={
    'username': 'john_doe',
    'email': 'john@example.com',
    'password': 'SecurePass123!'
})

# Login
login = requests.post(f'{BASE}/api/auth/login', json={
    'username': 'john_doe',
    'password': 'SecurePass123!'
})
token = login.json()['jwt']

# Protected
user = requests.get(f'{BASE}/api/users/me',
    headers={'Authorization': f'Bearer {token}'})
```

### cURL

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","email":"john@example.com","password":"SecurePass123!"}'

# Login
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"SecurePass123!"}' \
  | jq -r '.jwt')

# Protected request
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## Validation Rules

### Username
- Required: Yes
- Min Length: 1 character
- Max Length: 50 characters
- Must be unique

### Email
- Required: Yes
- Format: Valid email (user@example.com)
- Max Length: 100 characters
- Must be unique

### Password
- Required: Yes
- Min Length: 1 character
- No max length restriction
- Will be hashed with BCrypt

---

## Authentication Methods

The API supports two authentication methods:

### JWT (Recommended for APIs)
```
Authorization: Bearer <jwt_token>
```
- Stateless (no server session needed)
- Good for single-page applications (SPAs)
- Good for mobile apps

### Session-based (Form Login)
```
Cookie: JSESSIONID=<session_id>
```
- Traditional form-based login
- Server maintains session
- Good for traditional web apps

---

## Security Headers

The API includes these security headers:

```
Content-Security-Policy: default-src 'self'; 
                        script-src 'self' 'unsafe-inline'; 
                        style-src 'self' 'unsafe-inline'
```

---

## Configuration Values

Edit `application.properties` to customize:

```properties
# JWT
jwt.secret=<your-secret-key>           # Min 32 chars
jwt.expirationMs=86400000              # Token expiration in ms

# Server
server.port=8080                       # Server port

# Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
```

---

## Testing Checklist

- [ ] Register user successfully
- [ ] Register duplicate username (should fail)
- [ ] Register invalid email (should fail)
- [ ] Login with correct credentials
- [ ] Login with wrong password (should fail)
- [ ] Access protected endpoint with JWT
- [ ] Access protected endpoint without JWT (should fail)
- [ ] Access protected endpoint with invalid JWT (should fail)
- [ ] Token includes correct username in payload
- [ ] Token expires after configured time

---

## Troubleshooting

### "Username already exists" when registering
- The username is already taken
- Choose a different username

### "Invalid username or password" when logging in
- Username or password is incorrect
- Verify credentials match registered account

### "Unauthorized" when accessing protected endpoint
- JWT token is missing or invalid
- Include valid JWT in `Authorization: Bearer <token>` header

### Token not working after long time
- Token has expired (default 24 hours)
- Login again to get a new token

### Port 8080 already in use
- Change port in `application.properties`: `server.port=8081`

---

## Database Access (Development Only)

**H2 Console:** http://localhost:8080/h2-console

**Credentials:**
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)

**Useful Queries:**
```sql
-- View all users
SELECT * FROM users;

-- View user roles
SELECT * FROM user_roles;

-- Count users
SELECT COUNT(*) FROM users;

-- Delete all users (caution!)
DELETE FROM user_roles;
DELETE FROM users;
```

---

## Rate Limiting

Currently no rate limiting is implemented. For production, consider:

```java
// Add to pom.xml
// org.springframework.cloud:spring-cloud-starter-gateway
// or use:
// io.github.bucket4j:bucket4j-core
```

---

## CORS Configuration (For Frontend Integration)

Currently CORS is not configured. To enable for your frontend:

```java
// Add to SecurityConfig.java
@Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true)
                .maxAge(3600);
        }
    };
}
```

---

## Performance Metrics

- **JWT Validation**: ~1ms per request
- **Password Hashing**: ~100-200ms per login
- **Database Query**: < 1ms for in-memory H2
- **Token Expiration**: 24 hours (configurable)

---

## Version Information

- **Spring Boot**: 3.2.1
- **Java**: 21 (LTS)
- **JWT Library**: JJWT 0.12.3
- **Database**: H2 (development), compatible with PostgreSQL/MySQL

---

**Last Updated**: December 6, 2024
**API Version**: 1.0.0
