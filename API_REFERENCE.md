# API Reference Guide

Complete API documentation for the Task Management System.

## Base URL
```
http://localhost:8080
```

## Authentication

All protected endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

---

## 📝 Authentication Endpoints

### Register User
Creates a new user account.

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huQGV4YW1wbGUuY29tIiwiaWF0IjoxNzMzNDg0MDAwLCJleHAiOjE3MzM1NzA0MDB9.signature",
  "email": "john@example.com"
}
```

**Validation:**
- Email must be valid format
- Email must be unique
- Password is required (minimum length recommended)

---

### Login
Authenticates user and returns JWT token.

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "john@example.com"
}
```

**Errors:**
- `401 Unauthorized` - Invalid credentials

---

## 👤 User Management Endpoints

### Get Current User
Returns authenticated user's profile.

**Endpoint:** `GET /api/users/me`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b3c",
  "name": "John Doe",
  "email": "john@example.com"
}
```

---

### Get All Users
Returns list of all registered users.

**Endpoint:** `GET /api/users`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
[
  {
    "id": "675328a1d5e8f23b4c1a2b3c",
    "name": "John Doe",
    "email": "john@example.com"
  },
  {
    "id": "675328a1d5e8f23b4c1a2b3d",
    "name": "Jane Smith",
    "email": "jane@example.com"
  }
]
```

---

### Get User by ID
Returns specific user details.

**Endpoint:** `GET /api/users/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b3c",
  "name": "John Doe",
  "email": "john@example.com"
}
```

---

### Upload Avatar
Uploads or replaces user avatar image.

**Endpoint:** `POST /api/users/me/avatar`

**Headers:** 
- `Authorization: Bearer <token>`
- `Content-Type: multipart/form-data`

**Form Data:**
- `file`: Image file (JPEG, PNG, GIF)

**Response:** `200 OK`
```json
{
  "fileId": "675328a1d5e8f23b4c1a2b3e",
  "filename": "avatar.jpg",
  "contentType": "image/jpeg",
  "fileSize": 1024,
  "message": "Avatar uploaded successfully"
}
```

**Validation:**
- File must be an image (image/jpeg, image/png, image/gif)
- Maximum file size: 5MB
- Old avatar is automatically deleted

---

### Upload Profile Picture
Uploads or replaces profile picture.

**Endpoint:** `POST /api/users/me/profile-pic`

**Headers:** 
- `Authorization: Bearer <token>`
- `Content-Type: multipart/form-data`

**Form Data:**
- `file`: Image file

**Response:** `200 OK`
```json
{
  "fileId": "675328a1d5e8f23b4c1a2b3f",
  "filename": "profile.jpg",
  "contentType": "image/jpeg",
  "fileSize": 2048,
  "message": "Profile picture uploaded successfully"
}
```

---

### Download Avatar
Downloads user's avatar image (public endpoint).

**Endpoint:** `GET /api/users/{email}/avatar`

**Response:** `200 OK`
- Content-Type: image/jpeg
- Binary image data

**Errors:**
- `404 Not Found` - User has no avatar

---

### Download Profile Picture
Downloads user's profile picture (public endpoint).

**Endpoint:** `GET /api/users/{email}/profile-pic`

**Response:** `200 OK`
- Content-Type: image/jpeg
- Binary image data

**Errors:**
- `404 Not Found` - User has no profile picture

---

### Delete Avatar
Deletes user's avatar image.

**Endpoint:** `DELETE /api/users/me/avatar`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "fileId": null,
  "filename": null,
  "contentType": null,
  "fileSize": 0,
  "message": "Avatar deleted successfully"
}
```

---

### Delete Profile Picture
Deletes user's profile picture.

**Endpoint:** `DELETE /api/users/me/profile-pic`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "fileId": null,
  "filename": null,
  "contentType": null,
  "fileSize": 0,
  "message": "Profile picture deleted successfully"
}
```

---

## 👥 Team Management Endpoints

### Create Team
Creates a new team with the current user as manager.

**Endpoint:** `POST /api/teams`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "Development Team",
  "description": "Main development team"
}
```

**Response:** `201 Created`
```json
{
  "id": "675328a1d5e8f23b4c1a2b40",
  "name": "Development Team",
  "description": "Main development team",
  "managerEmail": "john@example.com",
  "members": ["john@example.com"],
  "createdAt": "2025-12-06T12:00:00"
}
```

---

### List User's Teams
Returns all teams the authenticated user is a member of.

**Endpoint:** `GET /api/teams`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
[
  {
    "id": "675328a1d5e8f23b4c1a2b40",
    "name": "Development Team",
    "managerEmail": "john@example.com",
    "members": ["john@example.com", "jane@example.com"]
  }
]
```

---

### Get Team by ID
Returns specific team details.

**Endpoint:** `GET /api/teams/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b40",
  "name": "Development Team",
  "description": "Main development team",
  "managerEmail": "john@example.com",
  "members": ["john@example.com", "jane@example.com"],
  "createdAt": "2025-12-06T12:00:00"
}
```

**Errors:**
- `403 Forbidden` - User is not a team member

---

### Invite User to Team
Invites a user to join the team (manager only).

**Endpoint:** `POST /api/teams/{id}/invite`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "email": "jane@example.com"
}
```

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b40",
  "members": ["john@example.com", "jane@example.com"]
}
```

**Validation:**
- Only team manager can invite users
- Team must have less than 10 members
- User must exist

**Errors:**
- `403 Forbidden` - User is not team manager
- `400 Bad Request` - Team is full (10 members) or user already invited

---

### Join Team
Joins a team using invitation.

**Endpoint:** `POST /api/teams/join`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "teamId": "675328a1d5e8f23b4c1a2b40"
}
```

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b40",
  "members": ["john@example.com", "jane@example.com"]
}
```

**Errors:**
- `400 Bad Request` - User already in team or team is full

---

### Get Team's Projects
Returns all projects belonging to a team.

**Endpoint:** `GET /api/teams/{id}/projects`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
[
  {
    "id": "675328a1d5e8f23b4c1a2b41",
    "name": "Mobile App",
    "teamId": "675328a1d5e8f23b4c1a2b40"
  }
]
```

---

## 📊 Project Management Endpoints

### Create Project
Creates a new project within a team.

**Endpoint:** `POST /api/projects`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "Mobile App",
  "description": "iOS and Android application",
  "teamId": "675328a1d5e8f23b4c1a2b40"
}
```

**Response:** `201 Created`
```json
{
  "id": "675328a1d5e8f23b4c1a2b41",
  "name": "Mobile App",
  "description": "iOS and Android application",
  "teamId": "675328a1d5e8f23b4c1a2b40",
  "ownerEmail": "john@example.com",
  "members": ["john@example.com"],
  "createdAt": "2025-12-06T12:00:00"
}
```

**Validation:**
- User must be team member
- Team must exist

---

### Get Project by ID
Returns specific project details.

**Endpoint:** `GET /api/projects/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b41",
  "name": "Mobile App",
  "description": "iOS and Android application",
  "teamId": "675328a1d5e8f23b4c1a2b40",
  "ownerEmail": "john@example.com",
  "members": ["john@example.com", "jane@example.com"]
}
```

**Errors:**
- `403 Forbidden` - User is not project member

---

### Update Project
Updates project details.

**Endpoint:** `PUT /api/projects/{id}`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "Mobile App v2",
  "description": "Updated mobile application"
}
```

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b41",
  "name": "Mobile App v2",
  "description": "Updated mobile application"
}
```

**Errors:**
- `403 Forbidden` - User is not project owner

---

### Delete Project
Deletes a project (owner only).

**Endpoint:** `DELETE /api/projects/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `204 No Content`

**Errors:**
- `403 Forbidden` - User is not project owner

---

### Add Project Member
Adds a user to the project.

**Endpoint:** `POST /api/projects/{id}/members`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "email": "jane@example.com"
}
```

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b41",
  "members": ["john@example.com", "jane@example.com"]
}
```

**Errors:**
- `403 Forbidden` - User is not project owner

---

### Remove Project Member
Removes a user from the project.

**Endpoint:** `DELETE /api/projects/{id}/members/{userId}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b41",
  "members": ["john@example.com"]
}
```

**Errors:**
- `403 Forbidden` - User is not project owner

---

## ✅ Task Management Endpoints

### Create Task
Creates a new task in a project.

**Endpoint:** `POST /api/projects/{projectId}/tasks`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "title": "Implement login screen",
  "description": "Create UI for user login",
  "status": "TODO",
  "assignedTo": "jane@example.com",
  "priority": "HIGH",
  "dueDate": "2025-12-31T23:59:59",
  "tags": ["frontend", "ui"]
}
```

**Response:** `201 Created`
```json
{
  "id": "675328a1d5e8f23b4c1a2b42",
  "projectId": "675328a1d5e8f23b4c1a2b41",
  "title": "Implement login screen",
  "description": "Create UI for user login",
  "status": "TODO",
  "assignedTo": "jane@example.com",
  "priority": "HIGH",
  "dueDate": "2025-12-31T23:59:59",
  "tags": ["frontend", "ui"],
  "createdAt": "2025-12-06T12:00:00"
}
```

**Validation:**
- User must be project member
- Status: TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED
- Priority: LOW, MEDIUM, HIGH, URGENT

**Errors:**
- `403 Forbidden` - User is not project member

---

### List Tasks
Returns tasks for a project with advanced filtering and pagination.

**Endpoint:** `GET /api/projects/{projectId}/tasks`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `status` (optional) - Filter by status (TODO, IN_PROGRESS, IN_REVIEW, DONE, BLOCKED)
- `assigneeId` (optional) - Filter by assignee user ID
- `tagId` (optional) - Filter by tag ID
- `priority` (optional) - Filter by priority (LOW, MEDIUM, HIGH, URGENT)
- `dueDateStart` (optional) - Filter tasks with due date >= this (ISO 8601 format)
- `dueDateEnd` (optional) - Filter tasks with due date <= this (ISO 8601 format)
- `page` (default: 0) - Page number
- `size` (default: 20) - Page size
- `sortBy` (default: dueDate) - Sort field
- `sortDirection` (default: ASC) - Sort direction (ASC/DESC)

**Example:**
```
GET /api/projects/675328a1d5e8f23b4c1a2b41/tasks?status=TODO&assigneeId=675328a1d5e8f23b4c1a2b40&priority=HIGH&dueDateStart=2025-12-01T00:00:00&dueDateEnd=2025-12-31T23:59:59&page=0&size=20
```

**Response:** `200 OK`
```json
{
  "tasks": [
    {
      "id": "675328a1d5e8f23b4c1a2b42",
      "title": "Implement login screen",
      "description": "Create UI for user login",
      "status": "TODO",
      "priority": "HIGH",
      "dueDate": "2025-12-15T23:59:59",
      "assigneeIds": ["675328a1d5e8f23b4c1a2b40"],
      "tagIds": ["675328a1d5e8f23b4c1a2b43"]
    }
  ],
  "currentPage": 0,
  "totalItems": 15,
  "totalPages": 1
}
```

**Errors:**
- `403 Forbidden` - User is not project member

---

### Search Tasks
Full-text search in task titles and descriptions.

**Endpoint:** `GET /api/projects/{projectId}/tasks/search`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `q` (required) - Search query text
- `page` (default: 0) - Page number
- `size` (default: 20) - Page size
- `sortBy` (default: dueDate) - Sort field
- `sortDirection` (default: ASC) - Sort direction (ASC/DESC)

**Example:**
```
GET /api/projects/675328a1d5e8f23b4c1a2b41/tasks/search?q=login&page=0&size=20
```

**Response:** `200 OK`
```json
{
  "tasks": [
    {
      "id": "675328a1d5e8f23b4c1a2b42",
      "title": "Implement login screen",
      "description": "Create UI for user login with validation"
    }
  ],
  "currentPage": 0,
  "totalItems": 5,
  "totalPages": 1
}
```

**Errors:**
- `403 Forbidden` - User is not project member

---

### Get Overdue Tasks
Returns tasks that are past their due date and not completed.

**Endpoint:** `GET /api/projects/{projectId}/tasks/overdue`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `page` (default: 0) - Page number
- `size` (default: 20) - Page size
- `sortBy` (default: dueDate) - Sort field
- `sortDirection` (default: ASC) - Sort direction (ASC/DESC)

**Example:**
```
GET /api/projects/675328a1d5e8f23b4c1a2b41/tasks/overdue?page=0&size=20
```

**Response:** `200 OK`
```json
{
  "tasks": [
    {
      "id": "675328a1d5e8f23b4c1a2b42",
      "title": "Fix critical bug",
      "status": "IN_PROGRESS",
      "dueDate": "2025-11-30T23:59:59",
      "priority": "URGENT"
    }
  ],
  "currentPage": 0,
  "totalItems": 3,
  "totalPages": 1
}
```

**Errors:**
- `403 Forbidden` - User is not project member

---

### Get Task Statistics
Returns aggregated task counts by status and overdue count.

**Endpoint:** `GET /api/projects/{projectId}/tasks/statistics`

**Headers:** `Authorization: Bearer <token>`

**Example:**
```
GET /api/projects/675328a1d5e8f23b4c1a2b41/tasks/statistics
```

**Response:** `200 OK`
```json
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

**Errors:**
- `403 Forbidden` - User is not project member

---

### Get Task by ID
Returns specific task details.

**Endpoint:** `GET /api/tasks/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b42",
  "projectId": "675328a1d5e8f23b4c1a2b41",
  "title": "Implement login screen",
  "description": "Create UI for user login",
  "status": "TODO",
  "assignedTo": "jane@example.com",
  "priority": "HIGH",
  "dueDate": "2025-12-31T23:59:59",
  "tags": ["frontend", "ui"],
  "createdAt": "2025-12-06T12:00:00",
  "updatedAt": "2025-12-06T12:00:00"
}
```

**Errors:**
- `403 Forbidden` - User is not project member

---

### Update Task
Updates task details.

**Endpoint:** `PUT /api/tasks/{id}`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "title": "Implement login screen with validation",
  "status": "IN_PROGRESS",
  "priority": "URGENT"
}
```

**Response:** `200 OK`
```json
{
  "id": "675328a1d5e8f23b4c1a2b42",
  "title": "Implement login screen with validation",
  "status": "IN_PROGRESS",
  "priority": "URGENT",
  "updatedAt": "2025-12-06T13:00:00"
}
```

**Errors:**
- `403 Forbidden` - User is not project member

---

### Delete Task
Deletes a task.

**Endpoint:** `DELETE /api/tasks/{id}`

**Headers:** `Authorization: Bearer <token>`

**Response:** `204 No Content`

**Errors:**
- `403 Forbidden` - User is not project member

---

## 💬 Comment Management Endpoints

### Add Comment
Adds a comment to a task.

**Endpoint:** `POST /api/tasks/{taskId}/comments`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "content": "I'm working on this now",
  "parentId": null
}
```

**Response:** `201 Created`
```json
{
  "id": "675328a1d5e8f23b4c1a2b43",
  "taskId": "675328a1d5e8f23b4c1a2b42",
  "userId": "675328a1d5e8f23b4c1a2b3c",
  "content": "I'm working on this now",
  "parentId": null,
  "createdAt": "2025-12-06T12:00:00"
}
```

**Note:** To reply to a comment, include the parent comment's ID in `parentId`.

**Errors:**
- `403 Forbidden` - User is not project member

---

### List Comments
Returns comments for a task with pagination.

**Endpoint:** `GET /api/tasks/{taskId}/comments`

**Headers:** `Authorization: Bearer <token>`

**Query Parameters:**
- `limit` (default: 50) - Maximum comments to return
- `before` (optional) - ISO datetime for cursor pagination

**Example:**
```
GET /api/tasks/675328a1d5e8f23b4c1a2b42/comments?limit=20&before=2025-12-06T12:00:00
```

**Response:** `200 OK`
```json
[
  {
    "id": "675328a1d5e8f23b4c1a2b43",
    "userId": "675328a1d5e8f23b4c1a2b3c",
    "content": "I'm working on this now",
    "parentId": null,
    "createdAt": "2025-12-06T12:00:00"
  }
]
```

**Errors:**
- `403 Forbidden` - User is not project member

---

## 🏷 Tag Management Endpoints

### Create Tag
Creates a new tag for a project.

**Endpoint:** `POST /api/projects/{projectId}/tags`

**Headers:** `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "name": "bug",
  "color": "#FF0000"
}
```

**Response:** `201 Created`
```json
{
  "id": "675328a1d5e8f23b4c1a2b44",
  "projectId": "675328a1d5e8f23b4c1a2b41",
  "name": "bug",
  "color": "#FF0000"
}
```

**Validation:**
- Tag name must be unique per project
- Color must be valid hex code (#RRGGBB)
- Default color: #808080 (gray)

**Errors:**
- `400 Bad Request` - Tag with name already exists in project
- `403 Forbidden` - User is not project member

---

### List Tags
Returns all tags for a project.

**Endpoint:** `GET /api/projects/{projectId}/tags`

**Headers:** `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
[
  {
    "id": "675328a1d5e8f23b4c1a2b44",
    "projectId": "675328a1d5e8f23b4c1a2b41",
    "name": "bug",
    "color": "#FF0000"
  },
  {
    "id": "675328a1d5e8f23b4c1a2b45",
    "projectId": "675328a1d5e8f23b4c1a2b41",
    "name": "feature",
    "color": "#00FF00"
  }
]
```

**Errors:**
- `403 Forbidden` - User is not project member

---

## 💬 Real-time Chat Endpoints

### Get Message History
Returns message history for a team or task channel (REST fallback).

**Endpoint:** `GET /api/chat/{channelType}/{channelId}`

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**
- `channelType`: "team" or "task"
- `channelId`: Team ID or Task ID

**Query Parameters:**
- `page` (default: 0) - Page number
- `size` (default: 50) - Page size

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "675328a1d5e8f23b4c1a2b46",
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

**Validation:**
- User must be a member of the team (for team channels)
- User must be a project member (for task channels)

**Errors:**
- `403 Forbidden` - User is not a member of the channel

---

### Send Message (REST Fallback)
Sends a message to a channel via REST API.

**Endpoint:** `POST /api/chat/{channelType}/{channelId}`

**Headers:** `Authorization: Bearer <token>`

**Path Parameters:**
- `channelType`: "team" or "task"
- `channelId`: Team ID or Task ID

**Request Body:**
```json
{
  "text": "Hello everyone!"
}
```

**Response:** `201 Created`
```json
{
  "id": "675328a1d5e8f23b4c1a2b47",
  "channelType": "team",
  "channelId": "675328a1d5e8f23b4c1a2b40",
  "senderId": "john@example.com",
  "senderName": "John Doe",
  "text": "Hello everyone!",
  "createdAt": "2025-12-06T12:05:00"
}
```

**Validation:**
- Message text: 1-2000 characters
- User must be a member of the channel

**Errors:**
- `403 Forbidden` - User is not a member of the channel

---

### WebSocket Chat

#### Connection
Connect to WebSocket endpoint with JWT authentication.

**Endpoint:** `ws://localhost:8080/ws/chat`

**Connection Headers:**
```javascript
{
  Authorization: 'Bearer <your-jwt-token>'
}
```

#### Join Channel
Subscribe to a team or task channel.

**Destination:** `/app/chat.join`

**Message:**
```json
{
  "channelType": "team",
  "channelId": "675328a1d5e8f23b4c1a2b40"
}
```

**Response:** Recent messages (up to 50) sent to `/topic/chat/{channelType}/{channelId}`

#### Leave Channel
Unsubscribe from a channel.

**Destination:** `/app/chat.leave`

**Message:**
```json
{
  "channelType": "team",
  "channelId": "675328a1d5e8f23b4c1a2b40"
}
```

#### Send Message
Send a real-time message to a channel.

**Destination:** `/app/chat.message`

**Message:**
```json
{
  "channelType": "team",
  "channelId": "675328a1d5e8f23b4c1a2b40",
  "text": "Hello from WebSocket!"
}
```

**Broadcast:** Message sent to all subscribers of `/topic/chat/{channelType}/{channelId}`

#### Subscribe to Messages
Receive real-time messages from a channel.

**Topic:** `/topic/chat/{channelType}/{channelId}`

**Message Format:**
```json
{
  "id": "675328a1d5e8f23b4c1a2b48",
  "channelType": "team",
  "channelId": "675328a1d5e8f23b4c1a2b40",
  "senderId": "jane@example.com",
  "senderName": "Jane Smith",
  "text": "Hi everyone!",
  "createdAt": "2025-12-06T12:10:00"
}
```

#### Example: JavaScript Client (SockJS + STOMP)
```javascript
// Import libraries
// <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
// <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>

const token = 'your-jwt-token';
const socket = new SockJS('http://localhost:8080/ws/chat');
const stompClient = Stomp.over(socket);

// Connect with JWT
stompClient.connect(
  { Authorization: 'Bearer ' + token },
  frame => {
    console.log('Connected:', frame);
    
    // Join a team channel
    stompClient.send('/app/chat.join', {}, JSON.stringify({
      channelType: 'team',
      channelId: '675328a1d5e8f23b4c1a2b40'
    }));
    
    // Subscribe to channel messages
    stompClient.subscribe('/topic/chat/team/675328a1d5e8f23b4c1a2b40', message => {
      const msg = JSON.parse(message.body);
      console.log(`${msg.senderName}: ${msg.text}`);
    });
    
    // Send a message after 2 seconds
    setTimeout(() => {
      stompClient.send('/app/chat.message', {}, JSON.stringify({
        channelType: 'team',
        channelId: '675328a1d5e8f23b4c1a2b40',
        text: 'Hello from JavaScript!'
      }));
    }, 2000);
  },
  error => {
    console.error('Connection error:', error);
  }
);

// Disconnect
// stompClient.disconnect(() => console.log('Disconnected'));
```

---

## 🔍 Health Check

### Health Check
Simple endpoint to verify service is running.

**Endpoint:** `GET /api/users/health/check`

**Response:** `200 OK`
```
User service is healthy
```

---

## ⚠️ Error Responses

All endpoints return standard error responses:

### 400 Bad Request
```json
{
  "message": "Validation failed",
  "details": "Email format is invalid"
}
```

### 401 Unauthorized
```json
{
  "message": "Authentication failed",
  "details": "Invalid credentials"
}
```

### 403 Forbidden
```json
{
  "message": "Access denied",
  "details": "User is not a project member"
}
```

### 404 Not Found
```json
{
  "message": "Resource not found",
  "details": "Task with id 123 not found"
}
```

### 500 Internal Server Error
```json
{
  "message": "Internal server error",
  "details": "An unexpected error occurred"
}
```

---

## 📋 Status Codes Summary

| Code | Description |
|------|-------------|
| 200  | OK - Request successful |
| 201  | Created - Resource created |
| 204  | No Content - Successful deletion |
| 400  | Bad Request - Invalid input |
| 401  | Unauthorized - Authentication failed |
| 403  | Forbidden - Insufficient permissions |
| 404  | Not Found - Resource doesn't exist |
| 500  | Internal Server Error - Server error |

---

## 🔐 Authentication Flow

1. **Register** - POST `/api/auth/register` with user details
2. **Login** - POST `/api/auth/login` with email/password
3. **Receive Token** - Store JWT token from response
4. **Use Token** - Include in Authorization header for all protected endpoints
5. **Token Expiration** - Tokens expire after 24 hours, login again

---

## 💡 Tips

- Always include `Authorization: Bearer <token>` for protected endpoints
- Use appropriate Content-Type headers (`application/json` or `multipart/form-data`)
- Validate input data before sending requests
- Handle error responses appropriately in client applications
- Store JWT tokens securely (not in localStorage for production apps)

---

For more examples and testing scenarios, see **TESTING_GUIDE.md**.
