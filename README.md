# Task Management System - Spring Boot Application

A full-featured collaborative task management system built with Spring Boot 3.2.1, MongoDB, and JWT authentication. Features team collaboration, project management, task tracking with comments and tags, and user profile management with GridFS file storage.

## 🚀 Quick Start

```bash
# Navigate to project
cd "e:\Desktop\112 Project"

# Build the project
mvn clean compile -DskipTests

# Run the application
mvn spring-boot:run

# Run tests
mvn test

# Run with test reports
mvn test surefire-report:report
```

Application starts on `http://localhost:8080`

## 📋 Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Security](#security)
- [Testing](#testing)
- [Project Structure](#project-structure)

## ✨ Features

### Core Functionality
- **User Authentication** - JWT-based login and registration with email
- **Team Management** - Create teams, invite members (max 10), manager-only permissions
- **Project Management** - Team-scoped projects with owner/member access control
- **Task Management** - Full CRUD with status tracking, assignments, filtering, pagination
- **Comment System** - Nested replies on tasks with cursor-based pagination
- **Tag System** - Color-coded project tags with duplicate prevention
- **File Storage** - Avatar and profile picture uploads using GridFS
- **Real-time Chat** - WebSocket-based team & task channels with message history

### Advanced Features
- Permission-based access control (403 Forbidden responses)
- Automatic old file deletion on replacement
- Compound indexes for performance (6 indexes on tasks)
- Cursor and page-based pagination
- Email-based user identification
- Real-time validation with proper error messages
- **Advanced Task Filtering** - Multi-criteria search (status, assignee, tag, priority, due date ranges)
- **Text Search** - Full-text search in task titles and descriptions
- **Task Statistics** - Real-time project task metrics

## 🛠 Technology Stack

### Backend
- **Spring Boot 3.2.1** - Application framework
- **Java 21** - Programming language
- **Spring Security 6.2.1** - Authentication & authorization
- **JWT (JJWT 0.12.3)** - Token-based authentication
- **Spring Data MongoDB 4.2.1** - Data access layer
- **MongoDB Atlas** - Cloud database (3-node replica set)
- **GridFS** - Binary file storage
- **WebSocket (STOMP)** - Real-time chat messaging
- **Lombok** - Code generation
- **Maven** - Build tool

### Testing
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Spring Test** - Integration testing
- **Spring Security Test** - Security testing
- **Testcontainers** - MongoDB integration tests
- **Maven Surefire** - Test execution and reporting

### Security
- BCrypt password hashing
- JWT stateless authentication
- CSRF protection (disabled for API)
- Input validation with @Valid
- Permission-based authorization

## 📦 Prerequisites

- **Java 21** or higher
- **Maven 3.8+** 
- **MongoDB Atlas** account (or local MongoDB)
- **Internet connection** for MongoDB Atlas
- **Docker** (optional, for integration tests with Testcontainers)

## 💾 Installation

### 1. Clone or Download Project
```bash
cd "e:\Desktop\112 Project"
```

### 2. Configure MongoDB
Edit `src/main/resources/application.properties`:

```properties
# MongoDB Atlas Connection
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/webapp

# JWT Configuration
jwt.secret=your-secret-key-here-make-it-long-and-random
jwt.expiration=86400000

# Server Configuration
server.port=8080
```

### 3. Build the Project
```bash
mvn clean install
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

## 🔧 Configuration

### MongoDB Setup
1. Create MongoDB Atlas account at https://www.mongodb.com/cloud/atlas
2. Create a cluster (free tier available)
3. Create database user with password
4. Whitelist your IP address (or use 0.0.0.0/0 for development)
5. Get connection string and update `application.properties`

### JWT Secret
Generate a secure secret key:
```bash
# Use a random string generator or command
openssl rand -base64 64
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080
```

### Authentication

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}

Response: 201 Created
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}

Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com"
}
```

### User Management

#### Get Current User
```http
GET /api/users/me
Authorization: Bearer <token>

Response: 200 OK
{
  "id": "675328a1d5e8f23b4c1a2b3c",
  "name": "John Doe",
  "email": "john@example.com"
}
```

#### Upload Avatar
```http
POST /api/users/me/avatar
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <image file>

Response: 200 OK
{
  "fileId": "675328a1d5e8f23b4c1a2b3d",
  "filename": "avatar.jpg",
  "contentType": "image/jpeg",
  "fileSize": 1024,
  "message": "Avatar uploaded successfully"
}
```

#### Download Avatar
```http
GET /api/users/{email}/avatar

Response: 200 OK
Content-Type: image/jpeg
<binary image data>
```

### Team Management

#### Create Team
```http
POST /api/teams
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Development Team",
  "description": "Main dev team"
}

Response: 201 Created
{
  "id": "675328a1d5e8f23b4c1a2b3e",
  "name": "Development Team",
  "description": "Main dev team",
  "managerEmail": "john@example.com",
  "members": ["john@example.com"]
}
```

#### Invite User to Team
```http
POST /api/teams/{teamId}/invite
Authorization: Bearer <token>
Content-Type: application/json

{
  "email": "jane@example.com"
}

Response: 200 OK
{
  "id": "675328a1d5e8f23b4c1a2b3e",
  "members": ["john@example.com", "jane@example.com"]
}
```

### Project Management

#### Create Project
```http
POST /api/projects
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Mobile App",
  "description": "iOS and Android app",
  "teamId": "675328a1d5e8f23b4c1a2b3e"
}

Response: 201 Created
{
  "id": "675328a1d5e8f23b4c1a2b3f",
  "name": "Mobile App",
  "description": "iOS and Android app",
  "teamId": "675328a1d5e8f23b4c1a2b3e",
  "ownerEmail": "john@example.com",
  "members": ["john@example.com"]
}
```

### Task Management

#### Create Task
```http
POST /api/projects/{projectId}/tasks
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Implement login screen",
  "description": "Create UI for user login",
  "status": "TODO",
  "assignedTo": "jane@example.com",
  "priority": "HIGH",
  "dueDate": "2025-12-31T23:59:59"
}

Response: 201 Created
{
  "id": "675328a1d5e8f23b4c1a2b40",
  "title": "Implement login screen",
  "status": "TODO",
  "assignedTo": "jane@example.com",
  "priority": "HIGH"
}
```

#### List Tasks with Filters
```http
GET /api/projects/{projectId}/tasks?status=TODO&assigneeId={userId}&tagId={tagId}&priority=HIGH&dueDateStart=2025-12-01T00:00:00&dueDateEnd=2025-12-31T23:59:59&page=0&size=20
Authorization: Bearer <token>

Response: 200 OK
{
  "tasks": [
    {
      "id": "675328a1d5e8f23b4c1a2b42",
      "title": "Implement login screen",
      "status": "TODO",
      "priority": "HIGH",
      "dueDate": "2025-12-15T23:59:59"
    }
  ],
  "currentPage": 0,
  "totalItems": 15,
  "totalPages": 1
}
```

#### Search Tasks
```http
GET /api/projects/{projectId}/tasks/search?q=login&page=0&size=20
Authorization: Bearer <token>

Response: 200 OK
{
  "tasks": [ /* matching tasks */ ],
  "currentPage": 0,
  "totalItems": 5,
  "totalPages": 1
}
```

#### Get Overdue Tasks
```http
GET /api/projects/{projectId}/tasks/overdue?page=0&size=20
Authorization: Bearer <token>

Response: 200 OK
{
  "tasks": [ /* overdue tasks */ ],
  "currentPage": 0,
  "totalItems": 3,
  "totalPages": 1
}
```

#### Get Task Statistics
```http
GET /api/projects/{projectId}/tasks/statistics
Authorization: Bearer <token>

Response: 200 OK
{
  "total": 50,
  "todo": 15,
  "inProgress": 10,
  "inReview": 5,
  "done": 18,
  "blocked": 2,
  "overdue": 3
}
```

### Comment Management

#### Add Comment
```http
POST /api/tasks/{taskId}/comments
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "Working on this now",
  "parentId": null
}

Response: 201 Created
{
  "id": "675328a1d5e8f23b4c1a2b41",
  "content": "Working on this now",
  "userId": "675328a1d5e8f23b4c1a2b3c",
  "taskId": "675328a1d5e8f23b4c1a2b40",
  "createdAt": "2025-12-06T12:00:00"
}
```

### Tag Management

#### Create Tag
```http
POST /api/projects/{projectId}/tags
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "bug",
  "color": "#FF0000"
}

Response: 201 Created
{
  "id": "675328a1d5e8f23b4c1a2b42",
  "name": "bug",
  "color": "#FF0000",
  "projectId": "675328a1d5e8f23b4c1a2b3f"
}
```

### Real-time Chat

#### Get Message History (REST)
```http
GET /api/chat/{channelType}/{channelId}?page=0&size=50
Authorization: Bearer <token>

channelType: "team" or "task"
channelId: Team ID or Task ID

Response: 200 OK
{
  "content": [
    {
      "id": "675328a1d5e8f23b4c1a2b43",
      "channelType": "team",
      "channelId": "675328a1d5e8f23b4c1a2b40",
      "senderId": "john@example.com",
      "senderName": "John Doe",
      "text": "Hello team!",
      "createdAt": "2025-12-06T12:00:00"
    }
  ],
  "totalElements": 25,
  "totalPages": 1,
  "number": 0,
  "size": 50
}
```

#### Send Message (REST Fallback)
```http
POST /api/chat/{channelType}/{channelId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "text": "Hello everyone!"
}

Response: 201 Created
{
  "id": "675328a1d5e8f23b4c1a2b44",
  "channelType": "team",
  "channelId": "675328a1d5e8f23b4c1a2b40",
  "senderId": "john@example.com",
  "senderName": "John Doe",
  "text": "Hello everyone!",
  "createdAt": "2025-12-06T12:05:00"
}
```

#### WebSocket Connection
```javascript
// Connect to WebSocket with JWT
const socket = new SockJS('http://localhost:8080/ws/chat');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { Authorization: 'Bearer ' + token },
  frame => {
    // Join a channel
    stompClient.send('/app/chat.join', {}, JSON.stringify({
      channelType: 'team',
      channelId: '675328a1d5e8f23b4c1a2b40'
    }));
    
    // Subscribe to channel messages
    stompClient.subscribe('/topic/chat/team/675328a1d5e8f23b4c1a2b40', message => {
      const msg = JSON.parse(message.body);
      console.log(msg.senderName + ': ' + msg.text);
    });
    
    // Send a message
    stompClient.send('/app/chat.message', {}, JSON.stringify({
      channelType: 'team',
      channelId: '675328a1d5e8f23b4c1a2b40',
      text: 'Hello from WebSocket!'
    }));
    
    // Leave channel
    stompClient.send('/app/chat.leave', {}, JSON.stringify({
      channelType: 'team',
      channelId: '675328a1d5e8f23b4c1a2b40'
    }));
  }
);
```

## 🗄 Database Schema

### Collections

#### users
```javascript
{
  _id: ObjectId,
  email: String (unique, indexed),
  name: String,
  passwordHash: String,
  createdAt: DateTime
}
```

#### user_profiles
```javascript
{
  _id: ObjectId,
  email: String (unique, indexed),
  avatarId: String,        // GridFS file ID
  profilePicId: String,    // GridFS file ID
  hiScore: Number,
  updatedAt: DateTime
}
```

#### teams
```javascript
{
  _id: ObjectId,
  name: String,
  description: String,
  managerEmail: String (indexed),
  members: [String],       // Array of emails (max 10)
  createdAt: DateTime
}
```

#### projects
```javascript
{
  _id: ObjectId,
  name: String,
  description: String,
  teamId: String (indexed),
  ownerEmail: String,
  members: [String],       // Array of emails
  createdAt: DateTime
}
```

#### tasks
```javascript
{
  _id: ObjectId,
  projectId: String,
  title: String,
  description: String,
  status: String,          // TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED
  assignedTo: String,
  priority: String,        // LOW, MEDIUM, HIGH, URGENT
  dueDate: DateTime,
  tags: [String],
  createdAt: DateTime,
  updatedAt: DateTime,
  
  // Compound indexes:
  // (projectId, status)
  // (projectId, dueDate)
}
```

#### comments
```javascript
{
  _id: ObjectId,
  taskId: String,
  userId: String,
  content: String,
  parentId: String,        // For nested replies
  createdAt: DateTime,
  
  // Compound index: (taskId, createdAt)
}
```

#### tags
```javascript
{
  _id: ObjectId,
  projectId: String,
  name: String,
  color: String,           // Hex color code
  
  // Unique compound index: (projectId, name)
}
```

#### messages
```javascript
{
  _id: ObjectId,
  channelType: String,     // "team" or "task"
  channelId: String,       // Team ID or Task ID
  senderId: String,        // User email
  senderName: String,      // User name (denormalized)
  text: String,
  createdAt: DateTime,
  
  // Compound index: (channelType, channelId, createdAt DESC)
}
```

### GridFS Collections (auto-managed)
- **fs.files** - File metadata
- **fs.chunks** - File data chunks

## 🔒 Security

### Authentication Flow
1. User registers with email/password
2. Server hashes password with BCrypt
3. User logs in with credentials
4. Server validates and returns JWT token
5. Client includes token in Authorization header: `Bearer <token>`
6. Server validates token on each request

### Authorization Rules
- Public endpoints: `/api/auth/**`, `/api/users/*/avatar`, `/api/users/*/profile-pic`
- Protected endpoints: All other `/api/**` routes require JWT authentication
- Permission checks:
  - Team manager required for invitations
  - Project member required for task/comment operations
  - Project owner required for member management
  - Returns 403 Forbidden for unauthorized access

### Input Validation
- Email format validation
- Password strength requirements
- File type validation (images only)
- File size limits (5MB max)
- Hex color validation
- Required field validation

## 🧪 Testing

### Using cURL

#### Register and Login
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","password":"password123"}'

# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"password123"}' | jq -r '.token')

echo $TOKEN
```

#### Upload Avatar
```bash
curl -X POST http://localhost:8080/api/users/me/avatar \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test_image.jpg"
```

#### Create Team
```bash
curl -X POST http://localhost:8080/api/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Dev Team","description":"Development team"}'
```

See **TESTING_GUIDE.md** for comprehensive testing scenarios.

## 📁 Project Structure

```
src/main/java/com/example/webapp/
├── WebappApplication.java           # Main Spring Boot application
├── controller/
│   ├── MongoAuthController.java     # Authentication endpoints
│   ├── MongoUserController.java     # User and file management
│   ├── TeamController.java          # Team management
│   ├── ProjectController.java       # Project management
│   ├── TaskController.java          # Task management
│   ├── CommentController.java       # Comment management
│   └── TagController.java           # Tag management
├── service/
│   ├── GridFsStorageService.java    # GridFS file operations
│   ├── UserAvatarService.java       # Avatar upload logic
│   ├── TeamService.java             # Team business logic
│   ├── ProjectService.java          # Project business logic
│   ├── TaskService.java             # Task business logic
│   ├── CommentService.java          # Comment business logic
│   ├── TagService.java              # Tag business logic
│   └── PermissionService.java       # Authorization checks
├── repository/
│   ├── UserRepository.java
│   ├── UserProfileRepository.java
│   ├── TeamRepository.java
│   ├── ProjectRepository.java
│   ├── TaskRepository.java
│   ├── CommentRepository.java
│   └── TagRepository.java
├── entity/
│   ├── User.java
│   ├── UserProfile.java
│   ├── Team.java
│   ├── Project.java
│   ├── Task.java
│   ├── Comment.java
│   └── Tag.java
├── dto/                             # Data Transfer Objects
├── security/
│   ├── SecurityConfig.java          # Security configuration
│   ├── JwtUtil.java                 # JWT utility
│   ├── JwtAuthenticationFilter.java # JWT filter
│   └── CustomUserDetailsService.java
├── exception/
│   └── GlobalExceptionHandler.java  # Error handling
└── util/
    └── FileValidationUtil.java      # File validation
```

## 📖 Additional Documentation

- **PROJECT_OVERVIEW.md** - Comprehensive project overview
- **ARCHITECTURE.md** - Technical architecture details
- **API_REFERENCE.md** - Complete API endpoint reference
- **TESTING_GUIDE.md** - Testing examples and scenarios
- **QUICK_START.md** - Quick setup guide
- **MONGODB_SETUP.md** - MongoDB configuration guide

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### MongoDB Connection Issues
- Verify MongoDB Atlas connection string
- Check IP whitelist in MongoDB Atlas
- Ensure username and password are correct
- Test connection with MongoDB Compass

### JWT Token Issues
- Verify JWT secret is set in application.properties
- Check token expiration (default 24 hours)
- Ensure token is included in Authorization header

## 📝 License

This project is for educational purposes.

## 👥 Contributors

Development Team

## 📧 Support

For issues or questions, refer to the documentation files or create an issue in the project repository.
