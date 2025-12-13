# Testing the Application - Complete Examples

## Starting the Application

```bash
cd "e:\Desktop\112 Project"
mvn spring-boot:run
```

**Expected Output:**
```
...
2024-12-06 09:00:00 INFO Tomcat started on port(s): 8080 (http) with context path ''
2024-12-06 09:00:00 INFO Started WebappApplication in X.XXX seconds
```

## Testing Public Endpoints

### 1. Test Home Endpoint

```bash
curl -v http://localhost:8080/
```

**Expected Response (200 OK):**
```
Welcome to Spring Boot Web Application!
```

### 2. Test Hello Endpoint

```bash
curl -v http://localhost:8080/api/hello
```

**Expected Response (200 OK):**
```
Hello from Spring Boot!
```

## Testing User Registration

### 3. Register User (Valid Request)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

### 4. Register User (Validation Error - Missing Username)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "",
    "email": "test@example.com",
    "password": "TestPass123!"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "message": "username: Username is required",
  "timestamp": "2024-12-06T09:05:00",
  "status": 400
}
```

### 5. Register User (Duplicate Username)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "another@example.com",
    "password": "TestPass123!"
  }'
```

**Expected Response (409 Conflict):**
```json
{
  "message": "Username already exists",
  "timestamp": "2024-12-06T09:05:00",
  "status": 409
}
```

### 6. Register User (Invalid Email Format)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jane_doe",
    "email": "not-an-email",
    "password": "TestPass123!"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "message": "email: Email should be valid",
  "timestamp": "2024-12-06T09:05:00",
  "status": 400
}
```

## Testing User Login (JWT)

### 7. Login (Valid Credentials)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePassword123!"
  }'
```

**Expected Response (200 OK):**
```json
{
  "jwt": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTczMzQ5Njg5MCwiZXhwIjoxNzMzNTgzMjkwfQ.xyz...",
  "tokenType": "Bearer"
}
```

### 8. Login (Invalid Credentials)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "WrongPassword"
  }'
```

**Expected Response (401 Unauthorized):**
```json
{
  "message": "Invalid username or password",
  "timestamp": "2024-12-06T09:05:00",
  "status": 401
}
```

### 9. Login (Non-existent User)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "nonexistent_user",
    "password": "AnyPassword123!"
  }'
```

**Expected Response (401 Unauthorized):**
```json
{
  "message": "Invalid username or password",
  "timestamp": "2024-12-06T09:05:00",
  "status": 401
}
```

## Testing Protected Endpoints

### 10. Get Current User (With Valid JWT)

First, copy the JWT token from login response (step 7), then:

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTczMzQ5Njg5MCwiZXhwIjoxNzMzNTgzMjkwfQ.xyz..."
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com"
}
```

### 11. Get Current User (Without JWT Token)

```bash
curl -X GET http://localhost:8080/api/users/me
```

**Expected Response (401 Unauthorized):**
```
Unauthorized
```

### 12. Get Current User (With Invalid JWT)

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer invalid.token.here"
```

**Expected Response (401 Unauthorized):**
```
Unauthorized
```

### 13. Get Current User (With Expired JWT)

(If token has expired - would return 401 Unauthorized)

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTczMzQ5Njg5MCwiZXhwIjoxNTAwMDAwMDAwfQ.xyz..."
```

**Expected Response (401 Unauthorized):**
```
Unauthorized
```

### 14. Get Current User (Wrong Bearer Format)

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: eyJhbGciOiJIUzUxMiJ9.xyz"
```

**Expected Response (401 Unauthorized):**
```
Unauthorized
```

## Testing with Different HTTP Clients

### Using Postman

1. **Register Request:**
   - Method: POST
   - URL: http://localhost:8080/api/auth/register
   - Body: JSON
   ```json
   {
     "username": "postman_user",
     "email": "postman@example.com",
     "password": "PostmanPass123!"
   }
   ```

2. **Login Request:**
   - Method: POST
   - URL: http://localhost:8080/api/auth/login
   - Body: JSON
   ```json
   {
     "username": "postman_user",
     "password": "PostmanPass123!"
   }
   ```

3. **Get User Request:**
   - Method: GET
   - URL: http://localhost:8080/api/users/me
   - Headers:
     - Authorization: Bearer <token_from_login>

### Using REST Client in VS Code

Create file `test.http`:

```http
### Register
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "vscode_user",
  "email": "vscode@example.com",
  "password": "VscodePass123!"
}

### Login
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "vscode_user",
  "password": "VscodePass123!"
}

### Get User (replace TOKEN with actual JWT)
GET http://localhost:8080/api/users/me
Authorization: Bearer TOKEN_HERE

### Public Endpoint
GET http://localhost:8080/
```

### Using JavaScript Fetch API

```javascript
// Register
const registerRes = await fetch('http://localhost:8080/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'js_user',
    email: 'js@example.com',
    password: 'JsPass123!'
  })
});
const registerData = await registerRes.json();
console.log('Register:', registerData);

// Login
const loginRes = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'js_user',
    password: 'JsPass123!'
  })
});
const loginData = await loginRes.json();
const token = loginData.jwt;
console.log('Login Token:', token);

// Get User
const userRes = await fetch('http://localhost:8080/api/users/me', {
  method: 'GET',
  headers: { 'Authorization': `Bearer ${token}` }
});
const userData = await userRes.json();
console.log('User:', userData);
```

### Using Python Requests

```python
import requests
import json

BASE_URL = 'http://localhost:8080'

# Register
register_data = {
    'username': 'python_user',
    'email': 'python@example.com',
    'password': 'PythonPass123!'
}
register_res = requests.post(f'{BASE_URL}/api/auth/register', json=register_data)
print('Register:', register_res.status_code, register_res.json())

# Login
login_data = {
    'username': 'python_user',
    'password': 'PythonPass123!'
}
login_res = requests.post(f'{BASE_URL}/api/auth/login', json=login_data)
login_json = login_res.json()
token = login_json['jwt']
print('Token:', token)

# Get User
headers = {'Authorization': f'Bearer {token}'}
user_res = requests.get(f'{BASE_URL}/api/users/me', headers=headers)
print('User:', user_res.status_code, user_res.json())
```

## Testing Database Access

### Check User in Database

Open H2 Console: http://localhost:8080/h2-console

**Query to verify user registration:**
```sql
SELECT * FROM users;
```

**Expected Result:**
```
| ID | USERNAME   | EMAIL            | PASSWORD_HASH                                    | CREATED_AT
|----+------------+------------------+---------------------------------------------------+---------------------|
| 1  | john_doe   | john@example.com | $2a$10$xyz... (BCrypt hash)                     | 2024-12-06 09:05:00 |
```

**Query to check roles:**
```sql
SELECT * FROM user_roles;
```

**Expected Result:**
```
| USER_ID | ROLE
|---------+-----------|
| 1       | ROLE_USER |
```

## Testing Error Scenarios

### Test Missing Authorization Header
```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Content-Type: application/json"
```
**Expected**: 401 Unauthorized

### Test Malformed JSON
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "test", invalid json'
```
**Expected**: 400 Bad Request

### Test GET on POST-only Endpoint
```bash
curl -X GET http://localhost:8080/api/auth/login
```
**Expected**: 405 Method Not Allowed

### Test Non-existent Endpoint
```bash
curl -X GET http://localhost:8080/api/nonexistent
```
**Expected**: 404 Not Found

## Performance Testing

### Simple Load Test with Apache Bench

```bash
# Test home endpoint
ab -n 1000 -c 10 http://localhost:8080/

# Test registration endpoint (note: will fail on duplicates after first)
ab -n 10 -c 1 -p register.json http://localhost:8080/api/auth/register
```

### JWT Token Inspection

Decode JWT at https://jwt.io (copy the token and paste there):

```
Header: {"alg":"HS512","typ":"JWT"}
Payload: {"sub":"john_doe","iat":1733496890,"exp":1733583290}
Signature: <cryptographic signature>
```

## Security Testing

### SQL Injection Test
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe\" OR \"1\"=\"1",
    "password": "anything"
  }'
```
**Expected**: Login fails (SQL injection prevented by JPA)

### XSS Test
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "<script>alert(1)</script>",
    "email": "xss@example.com",
    "password": "TestPass123!"
  }'
```
**Expected**: Username stored safely, no script execution

### CSRF Token Test
```bash
curl -X POST http://localhost:8080/api/users/me \
  -H "Content-Type: application/json" \
  -d '{}'
```
**Expected**: May fail due to CSRF protection (expected behavior)

## Cleanup & Reset

To reset the database and start fresh:

1. Stop the application (Ctrl+C)
2. Start again with `mvn spring-boot:run`
3. Database automatically recreates (create-drop mode)

Or manually clear in H2 Console:
```sql
DELETE FROM user_roles;
DELETE FROM users;
```

## Summary of Test Cases

| Test Case | Method | Endpoint | Expected Status | Notes |
|-----------|--------|----------|-----------------|-------|
| Home | GET | / | 200 | Public endpoint |
| Hello | GET | /api/hello | 200 | Public endpoint |
| Register Valid | POST | /api/auth/register | 201 | Creates user |
| Register Invalid | POST | /api/auth/register | 400 | Validation error |
| Register Duplicate | POST | /api/auth/register | 409 | Username exists |
| Login Valid | POST | /api/auth/login | 200 | Returns JWT |
| Login Invalid | POST | /api/auth/login | 401 | Bad credentials |
| Get User Auth | GET | /api/users/me | 200 | With JWT token |
| Get User NoAuth | GET | /api/users/me | 401 | No token |
| Get User BadJWT | GET | /api/users/me | 401 | Invalid token |

---

**All tests should pass!** If any fail, check application logs for debugging information.
