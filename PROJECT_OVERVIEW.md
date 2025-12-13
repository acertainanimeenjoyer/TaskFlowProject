# Project Overview - Task Management System

## Project Description
A full-featured task management web application built with Spring Boot and MongoDB. Features include team collaboration, project management, task tracking with comments and tags, and user profile management with avatar uploads using GridFS.

## Technology Stack

### Backend Framework
- **Spring Boot 3.2.1** - Core application framework
- **Java 21** - Programming language
- **Maven** - Build tool and dependency management

### Database
- **MongoDB Atlas** - Cloud-hosted 3-node replica set cluster
- **Spring Data MongoDB 4.2.1** - Data access layer
- **GridFS** - Binary file storage (avatars, profile pictures)

### Security
- **Spring Security 6.2.1** - Authentication and authorization
- **JWT (JJWT 0.12.3)** - Token-based authentication
- **BCrypt** - Password hashing

### Development Tools
- **Lombok** - Code generation (@Data, @Builder, @Slf4j)
- **Spring Boot DevTools** - Hot reload during development
- **Tomcat 10.1.17** - Embedded servlet container

### Real-time Communication
- **Spring WebSocket** - WebSocket support with STOMP protocol
- **SockJS** - WebSocket fallback for older browsers
- **STOMP** - Simple Text-Oriented Messaging Protocol

## Core Features

### 1. Authentication & User Management
- User registration with email validation
- JWT-based login system
- Email-based authentication (unique email per user)
- Secure password storage with BCrypt
- User profile management with avatar/profile picture uploads

### 2. Team Management
- Create teams with designated managers
- Invite users to teams via email
- Join teams through invitations
- Maximum 10 members per team
- Manager-only permissions for invitations
- List all teams for authenticated user

### 3. Project Management
- Create projects within teams
- Projects linked to specific teams (teamId)
- Owner and member access control
- Add/remove project members
- Permission-based access (403 Forbidden for unauthorized users)
- List projects by team

### 4. Task Management
- Full CRUD operations for tasks
- Task assignment to users
- Status tracking (TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED)
- Due dates and priority levels (LOW, MEDIUM, HIGH, URGENT)
- **Advanced Filtering** by:
  - Status
  - Assignee ID
  - Tag ID
  - Priority
  - Due Date Range (start/end)
  - Multiple criteria simultaneously
- **Full-Text Search** in task titles and descriptions
- **Overdue Task Detection** (due date < now AND status != DONE)
- **Task Statistics** (real-time counts by status + overdue count)
- Pagination support (page, size) with sorting
- Permission-based access control
- **6 compound MongoDB indexes** for optimal query performance

### 5. Comment System
- Add comments to tasks
- Nested replies (parentId support)
- Cursor-based pagination (limit, before timestamp)
- Automatic timestamp tracking
- Permission checks (only project members can comment)

### 6. Tag System
- Create project-scoped tags
- Color-coded tags (hex color codes)
- Unique constraint per project (projectId, name)
- Duplicate prevention
- Default gray color (#808080)

### 7. File Storage (GridFS)
- Avatar upload/download/delete
- Profile picture upload/download/delete
- Automatic old file deletion on replacement
- File validation (image types only, 5MB max)
- Public download URLs for images
- Authenticated upload/delete operations

### 8. Real-time Chat (WebSocket)
- Team channels for team-wide communication
- Task channels for task-specific discussions
- WebSocket-based real-time messaging (STOMP protocol)
- REST API fallback for message history
- JWT authentication at WebSocket handshake
- Automatic broadcasting to channel members only
- Message history retrieval (paginated)
- Join/leave notifications
- Up to 50 recent messages on channel join

### 9. Automated Testing & CI/CD
- **Unit Tests** - Service layer testing with Mockito
- **Integration Tests** - End-to-end workflow testing with Testcontainers
- **GitHub Actions CI** - Automated build and test pipeline
- **Test Coverage** - Critical flow validation (53 test methods)
- **Test Reports** - Maven Surefire reporting
- **Docker Support** - Testcontainers for MongoDB integration tests

## Database Collections

### MongoDB Collections (8 total)
1. **users** - User authentication data
2. **user_profiles** - User profile data with file references
3. **teams** - Team information with manager and members
4. **projects** - Project data with team linking
5. **tasks** - Task details with status and assignments
6. **comments** - Task comments with nested replies
7. **tags** - Project-scoped tags with colors
8. **messages** - Chat messages for team and task channels

### GridFS Collections (auto-managed)
- **fs.files** - File metadata
- **fs.chunks** - File chunks (16MB+ files supported)

## API Endpoints Summary

### Authentication (`/api/auth`)
- POST `/api/auth/register` - Register new user
- POST `/api/auth/login` - Login and get JWT token

### User Management (`/api/users`)
- GET `/api/users/me` - Get current user profile
- GET `/api/users` - Get all users
- GET `/api/users/{id}` - Get user by ID
- POST `/api/users/me/avatar` - Upload avatar (authenticated)
- POST `/api/users/me/profile-pic` - Upload profile picture (authenticated)
- GET `/api/users/{email}/avatar` - Download avatar (public)
- GET `/api/users/{email}/profile-pic` - Download profile picture (public)
- DELETE `/api/users/me/avatar` - Delete avatar (authenticated)
- DELETE `/api/users/me/profile-pic` - Delete profile picture (authenticated)

### Team Management (`/api/teams`)
- POST `/api/teams` - Create team
- GET `/api/teams` - List user's teams
- GET `/api/teams/{id}` - Get team by ID
- POST `/api/teams/{id}/invite` - Invite user (manager only)
- POST `/api/teams/join` - Join team via invitation
- GET `/api/teams/{id}/projects` - Get team's projects

### Project Management (`/api/projects`)
- POST `/api/projects` - Create project
- GET `/api/projects/{id}` - Get project by ID
- PUT `/api/projects/{id}` - Update project
- DELETE `/api/projects/{id}` - Delete project
- POST `/api/projects/{id}/members` - Add member
- DELETE `/api/projects/{id}/members/{userId}` - Remove member

### Task Management (`/api/projects/{projectId}/tasks`)
- POST `/api/projects/{projectId}/tasks` - Create task
- GET `/api/projects/{projectId}/tasks` - List tasks (with advanced filters)
  - Filters: status, assigneeId, tagId, priority, dueDateStart, dueDateEnd
  - Sorting & pagination support
- GET `/api/projects/{projectId}/tasks/search` - **Full-text search** in titles/descriptions
- GET `/api/projects/{projectId}/tasks/overdue` - **Get overdue tasks**
- GET `/api/projects/{projectId}/tasks/statistics` - **Get task counts by status**
- GET `/api/tasks/{id}` - Get task by ID
- PUT `/api/tasks/{id}` - Update task
- DELETE `/api/tasks/{id}` - Delete task

### Comment Management (`/api/tasks/{taskId}/comments`)
- POST `/api/tasks/{taskId}/comments` - Add comment
- GET `/api/tasks/{taskId}/comments` - List comments (paginated)

### Tag Management (`/api/projects/{projectId}/tags`)
- POST `/api/projects/{projectId}/tags` - Create tag
- GET `/api/projects/{projectId}/tags` - List project tags

### Real-time Chat (`/api/chat`, `/ws/chat`)
- GET `/api/chat/{channelType}/{channelId}` - Get message history (REST)
- POST `/api/chat/{channelType}/{channelId}` - Send message (REST fallback)
- WebSocket `/ws/chat` - Real-time messaging endpoint
  - `/app/chat.join` - Join channel
  - `/app/chat.leave` - Leave channel
  - `/app/chat.message` - Send message
  - `/topic/chat/{channelType}/{channelId}` - Subscribe to channel messages

## Security Features

### Authentication
- JWT tokens with 24-hour expiration
- Email claim in JWT for user identification
- Stateless authentication (no server-side sessions)

### Authorization
- Role-based access control
- Permission checks via PermissionService:
  - `isProjectMember(projectId, userId)`
  - `isTeamMember(teamId, userId)`
  - `isManager(teamId, userId, email)`
  - `isProjectOwner(projectId, userId)`
- HTTP 403 Forbidden responses for unauthorized access

### Input Validation
- Email format validation
- File type validation (images only)
- File size limits (5MB max)
- Hex color validation for tags
- Required field validation with @Valid annotations

### Data Protection
- BCrypt password hashing (strength 10)
- CSRF protection disabled for API endpoints
- CORS configuration for cross-origin requests
- SQL injection prevention (MongoDB parameterized queries)

## Application Architecture

### Layer Structure
```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
MongoDB Database
```

### Key Components
- **Controllers** - Handle HTTP requests, validate input, return responses
- **Services** - Business logic, data transformation, permission checks
- **Repositories** - MongoDB data access with Spring Data
- **Entities** - Domain models with MongoDB annotations
- **DTOs** - Data transfer objects for API requests/responses
- **Security Filters** - JWT authentication filter for token validation
- **Exception Handlers** - Global exception handling with proper HTTP status codes

## Development Workflow

### Running the Application
```bash
mvn spring-boot:run
```
Application starts on `http://localhost:8080`

### Building the Project
```bash
mvn clean compile -DskipTests
```

### Running Tests
```bash
mvn test
```

### Environment Configuration
Configure MongoDB connection in `src/main/resources/application.properties`:
```properties
spring.data.mongodb.uri=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>
jwt.secret=your-secret-key-here
jwt.expiration=86400000
```

## Performance Considerations

### Indexes
- Email fields: Unique indexes for fast user lookups
- Compound indexes:
  - (projectId, status) on tasks
  - (projectId, dueDate) on tasks
  - (taskId, createdAt) on comments
  - (projectId, name) on tags (unique)
  - (teamId) on projects

### Pagination
- Task listing: Page-based pagination
- Comment listing: Cursor-based pagination with timestamps

### File Storage
- GridFS: Efficient storage for large files (>16MB)
- Streaming downloads: No full file loading in memory
- Automatic cleanup: Old files deleted on replacement

## Error Handling

### Global Exception Handler
- Converts exceptions to proper HTTP responses
- Provides consistent error message format
- Returns appropriate status codes (400, 401, 403, 404, 500)

### Custom Error Responses
- Validation errors: 400 Bad Request
- Authentication failures: 401 Unauthorized
- Permission denials: 403 Forbidden
- Resource not found: 404 Not Found
- Server errors: 500 Internal Server Error

## Future Enhancements

### Potential Features
- Real-time notifications for task updates
- Task dependencies and sub-tasks
- Activity logs and audit trails
- Email notifications for invitations
- Advanced search and filtering
- Dashboard with statistics and charts
- File attachments for tasks
- Recurring tasks
- Task templates
- Time tracking

### Technical Improvements
- Redis caching for frequently accessed data
- WebSocket support for real-time updates
- Elasticsearch integration for advanced search
- Rate limiting for API endpoints
- API versioning strategy
- Comprehensive test coverage
- API documentation with Swagger/OpenAPI
- Docker containerization
- CI/CD pipeline setup

## Project Statistics

- **Total MongoDB Collections**: 7
- **Total API Endpoints**: ~30+
- **Programming Language**: Java 21
- **Lines of Code**: ~3,000+ (excluding tests)
- **Dependencies**: 15+ Maven dependencies
- **Database**: MongoDB Atlas (Cloud)
- **Authentication**: JWT (Stateless)

## Contact & Support

For questions or issues, refer to:
- **README.md** - Complete setup and API documentation
- **QUICK_START.md** - Quick setup guide
- **ARCHITECTURE.md** - Technical architecture details
- **API_REFERENCE.md** - Detailed API documentation
- **TESTING_GUIDE.md** - Testing examples and scenarios
