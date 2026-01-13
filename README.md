# TaskFlow – Collaborative Task Management System

## 📖 Project Overview

**TaskFlow** is a full-stack web application designed for collaborative task management within teams and organizations. Built with modern technologies, it provides a centralized platform where team members can organize projects, track tasks, communicate in real-time, and stay updated through notifications.

### Project Motivation

In today's distributed work environment, teams need efficient tools to collaborate on projects. TaskFlow addresses common challenges:
- **Fragmented Communication**: Centralizes discussions within team and task contexts
- **Task Visibility**: Provides a visual Kanban-style board for tracking work progress
- **Access Control**: Implements role-based permissions (Manager → Leader → Member)
- **Real-time Updates**: Instant messaging and notifications keep everyone synchronized

---

## 🎯 Use Case Description

### Target Users
- **Small to medium teams** (up to 10 members per team)
- **Project managers** who need to organize and delegate work
- **Team members** who need visibility into their assigned tasks
- **Remote teams** requiring real-time communication

### Primary Use Cases

| Actor | Use Case | Description |
|-------|----------|-------------|
| **Guest** | Register/Login | Create account with email/password, authenticate via JWT |
| **Team Manager** | Create Team | Initialize a new team, automatically becomes manager |
| **Team Manager** | Invite Members | Send email invitations (max 10 members) |
| **Team Manager** | Promote/Demote | Elevate members to leaders or demote leaders |
| **Team Member** | Join Team | Accept invitation using Team ID |
| **Manager/Leader** | Create Project | Start new projects within a team |
| **Manager/Leader** | Manage Tasks | Full CRUD operations on tasks |
| **Regular Member** | Update Task Status | Change task status (TODO → IN_PROGRESS → DONE) |
| **All Members** | Chat | Real-time messaging in team/task channels |
| **All Users** | View Notifications | Receive alerts for assignments, messages, updates |
| **All Users** | Manage Profile | Update avatar, name, view hi-score |

### User Role Hierarchy

```
Team Manager (Creator)
    ├── Full team control (invite, kick, promote, demote)
    ├── Full project control (create, edit, delete)
    └── Full task control (create, assign, edit, delete)

Team Leader (Promoted Member)
    ├── Can manage project members
    ├── Full task control within assigned projects
    └── Cannot modify team membership

Regular Member
    ├── Can view assigned tasks
    ├── Can change task status ONLY
    ├── Can participate in chat
    └── Cannot create/edit/delete tasks
```

---

## 🏗️ Application Design

### Architecture Overview

TaskFlow follows a **3-tier architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│         React + TypeScript + Vite (SPA Frontend)            │
│    • Pages: Login, Register, Dashboard, Teams, Projects     │
│    • Components: TaskBoard, ChatPanel, Modals, Avatar       │
│    • State: Zustand stores (auth, chat, toast)              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ REST API + WebSocket
┌─────────────────────────────────────────────────────────────┐
│                     BUSINESS LAYER                           │
│              Spring Boot 3.2.1 (Java 21)                     │
│    • Controllers: Auth, User, Team, Project, Task, Chat     │
│    • Services: Business logic, validation, permissions      │
│    • Security: JWT authentication, role-based access        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ JPA/Hibernate
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                              │
│                   PostgreSQL Database                        │
│    • 9 Entity Tables + 6 Junction Tables                    │
│    • Optimized indexes for performance                      │
│    • Relational integrity with foreign keys                 │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Frontend** | React 18 + TypeScript | Component-based UI |
| **Build Tool** | Vite | Fast development server & bundler |
| **State Management** | Zustand | Lightweight global state |
| **HTTP Client** | Axios | REST API communication |
| **WebSocket** | @stomp/stompjs + SockJS | Real-time messaging |
| **Drag & Drop** | @dnd-kit/core | Kanban board interactions |
| **Backend** | Spring Boot 3.2.1 | REST API framework |
| **Language** | Java 21 | Backend programming |
| **Security** | Spring Security + JWT | Authentication & authorization |
| **ORM** | Spring Data JPA (Hibernate) | Database abstraction |
| **Database** | PostgreSQL | Relational data storage |
| **Build** | Maven | Dependency management |

---

## 🖥️ UI Documentation

### Page Structure

```
App
├── Public Routes
│   ├── /login         → Login Page
│   └── /register      → Registration Page
│
└── Protected Routes (requires authentication)
    ├── /dashboard     → Dashboard (My Tasks overview)
    ├── /teams         → Teams List
    │   └── /teams/:id → Team Detail (members, chat)
    ├── /projects      → Projects List
    │   └── /projects/:id → Project Detail
    │       └── /projects/:id/tasks → Task Board (Kanban)
    ├── /profile       → User Profile (avatar, settings)
    └── /settings      → Application Settings
```

### Page Descriptions

#### 1. Login Page (`/login`)
| Element | Type | Function |
|---------|------|----------|
| Email Input | Text Field | Enter registered email |
| Password Input | Password Field | Enter password |
| Sign In Button | Button | Submit credentials, receive JWT |
| Register Link | Link | Navigate to registration |

**Keyboard Shortcuts**: `Enter` to submit form

---

#### 2. Register Page (`/register`)
| Element | Type | Function |
|---------|------|----------|
| Email Input | Text Field | Unique email address |
| Username Input | Text Field | Unique display name |
| Name Input | Text Field | Full name |
| Password Input | Password Field | Minimum 6 characters |
| Sign Up Button | Button | Create account |

---

#### 3. Dashboard (`/dashboard`)
| Element | Type | Function |
|---------|------|----------|
| My Tasks List | Card List | Shows all tasks assigned to current user |
| Task Cards | Interactive Cards | Click to view details, shows status/priority |
| Status Badges | Color Labels | TODO (yellow), IN_PROGRESS (blue), DONE (green) |
| Navigation | Top Bar | Links to Teams, Projects, Profile |
| Logout Button | Button | End session, clear tokens |

**Features**:
- Aggregates tasks from all projects across all teams
- Sorted by status (TODO first, then IN_PROGRESS, then DONE)
- Move tasks up/down in personal priority list

---

#### 4. Teams Page (`/teams`)
| Element | Type | Function |
|---------|------|----------|
| Teams List | Card Grid | All teams user belongs to |
| Create Team Button | Button | Opens team creation modal |
| Join Team Button | Button | Opens join modal (enter Team ID) |
| Team Cards | Clickable Cards | Navigate to team details |

**Keyboard Shortcuts**: `J/K` to navigate, `Enter` to select

---

#### 5. Team Detail Page (`/teams/:id`)
| Element | Type | Function |
|---------|------|----------|
| Member List | User Cards | Shows all team members with roles |
| Invite Input | Email Field | Enter email to invite (manager only) |
| Kick Button | Icon Button | Remove member (manager only) |
| Promote Button | Icon Button | Make member a leader (manager only) |
| Demote Button | Icon Button | Remove leader status (manager only) |
| Open Chat Button | Button | Opens team chat panel |
| Pending Invites | Badge List | Shows invited emails not yet joined |

---

#### 6. Projects Page (`/projects`)
| Element | Type | Function |
|---------|------|----------|
| Projects List | Card Grid | All projects from user's teams |
| Create Project Button | Button | Opens project creation modal |
| Project Cards | Clickable Cards | Navigate to task board |
| Team Badge | Label | Shows which team owns the project |

**Keyboard Shortcuts**: `N` to create new, `J/K` to navigate

---

#### 7. Task Board (`/projects/:id/tasks`)
| Element | Type | Function |
|---------|------|----------|
| Kanban Columns | 3 Columns | TODO, IN_PROGRESS, DONE |
| Task Cards | Draggable Cards | Drag between columns to update status |
| Create Task Button | Button | Opens task creation modal (manager/leader only) |
| Quick Status Buttons | Mini Buttons | Change status without opening modal |
| Filter Controls | Dropdowns | Filter by assignee, tag, priority, date |
| Tag Chips | Clickable Badges | Click to filter by tag |

**Task Card Elements**:
- Title
- Priority badge (HIGH=red, MEDIUM=yellow, LOW=green)
- Tag chips (max 2 shown, "+N" for overflow)
- Assignee count
- Due date (red if overdue)
- Quick status change buttons

**Keyboard Shortcuts**: 
- `N` or `C` to create task (manager/leader only)
- Arrow keys to navigate between columns and cards
- `Enter` to open selected task

---

#### 8. Task Detail Modal
| Element | Type | Function |
|---------|------|----------|
| Title | Editable (manager/leader) | Task name |
| Description | Text Area | Detailed task information |
| Status Dropdown | Select | TODO/IN_PROGRESS/DONE |
| Priority Dropdown | Select (manager/leader) | HIGH/MEDIUM/LOW |
| Due Date Picker | Date Input (manager/leader) | Deadline |
| Assignees | Multi-Select (manager/leader) | Assign team members |
| Tags | Multi-Select (manager/leader) | Apply project tags |
| Comments Section | Thread List | Nested comment replies |
| Delete Button | Button (manager/leader) | Remove task permanently |

**Permission Notes**:
- Regular members can ONLY change status
- All other fields are read-only for regular members

---

#### 9. Profile Page (`/profile`)
| Element | Type | Function |
|---------|------|----------|
| Avatar | Clickable Image | Click to upload new avatar |
| Upload Modal | Dialog | Preview and confirm avatar upload |
| Name Fields | Editable Text | First name, last name |
| Email | Read-only | Display email address |
| Hi-Score | Display | Gamification score |
| Delete Avatar | Button | Remove current avatar |

**Avatar Rules**:
- Max size: 5MB
- Formats: JPEG, PNG, GIF
- Old avatar automatically deleted on replacement

---

#### 10. Chat Panel (Floating)
| Element | Type | Function |
|---------|------|----------|
| Chat List | Tab List | All available team/task chats |
| Message History | Scrollable List | Previous messages (paginated) |
| Message Input | Text Field | Compose new message |
| Send Button | Button | Post message via WebSocket |
| Sender Info | Avatar + Email | Shows who sent each message |

**Features**:
- Real-time updates via WebSocket/STOMP
- Supports team-wide and task-specific channels
- Message history with "Load More" pagination
- Persistent across page navigation

---

#### 11. Notification Dropdown (Header)
| Element | Type | Function |
|---------|------|----------|
| Bell Icon | Icon Button | Toggle dropdown, shows unread count |
| Notification List | Scrollable List | Recent notifications |
| Mark as Read | Click Action | Dismiss notification |
| Clear All | Button | Remove all notifications |

**Notification Types**:
- Task assignment
- Task status changes
- New messages (when not in chat)
- Team invitations

---

## 🗄️ Database Schema

### Entity Relationship Summary

The database consists of **9 primary entity tables** and **6 junction/relationship tables**.

### Entity Tables

| Table | Description | Key Fields |
|-------|-------------|------------|
| `users` | User accounts | id, email, username, passwordHash |
| `user_profiles` | Extended profile data | id, email, avatarPath, hiScore |
| `teams` | Team organizations | id, name, managerId, code, joinMode |
| `projects` | Project containers | id, name, description, ownerId, teamId |
| `tasks` | Work items | id, title, status, priority, projectId, createdBy |
| `tags` | Task labels | id, name, color, projectId |
| `comments` | Task discussions | id, content, taskId, userId, parentId |
| `messages` | Chat messages | id, content, senderId, channelType, channelId |
| `notifications` | User alerts | id, type, title, message, userId, read |

### Junction/Relationship Tables (6 Required)

#### 1. `team_members`
**Purpose**: Links users to teams as regular members

| Column | Type | References |
|--------|------|------------|
| team_id | BIGINT | teams(id) |
| user_id | BIGINT | users(id) |

**Relationship**: Many-to-Many (Team ↔ User)

---

#### 2. `team_leaders`
**Purpose**: Designates users with leader privileges within a team

| Column | Type | References |
|--------|------|------------|
| team_id | BIGINT | teams(id) |
| user_id | BIGINT | users(id) |

**Relationship**: Many-to-Many (Team ↔ User)

---

#### 3. `team_invites`
**Purpose**: Stores pending email invitations for teams

| Column | Type | References |
|--------|------|------------|
| team_id | BIGINT | teams(id) |
| invite_email | VARCHAR | - |

**Relationship**: One-to-Many (Team → Invite Emails)

---

#### 4. `project_members`
**Purpose**: Assigns users to specific projects within a team

| Column | Type | References |
|--------|------|------------|
| project_id | BIGINT | projects(id) |
| user_id | BIGINT | users(id) |

**Relationship**: Many-to-Many (Project ↔ User)

---

#### 5. `task_assignees`
**Purpose**: Assigns one or more users to work on a task

| Column | Type | References |
|--------|------|------------|
| task_id | BIGINT | tasks(id) |
| user_id | BIGINT | users(id) |

**Relationship**: Many-to-Many (Task ↔ User)

---

#### 6. `task_tags`
**Purpose**: Applies labels/categories to tasks

| Column | Type | References |
|--------|------|------------|
| task_id | BIGINT | tasks(id) |
| tag_id | BIGINT | tags(id) |

**Relationship**: Many-to-Many (Task ↔ Tag)

---

### Entity Relationship Diagram (Text Representation)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           DATABASE RELATIONSHIPS                              │
└──────────────────────────────────────────────────────────────────────────────┘

    ┌─────────┐         ┌───────────────┐
    │  users  │◄────────│ user_profiles │  (1:1)
    └────┬────┘         └───────────────┘
         │
         │ manages (1:N)
         ▼
    ┌─────────┐
    │  teams  │
    └────┬────┘
         │
         ├──────────────── team_members ──────────────────┐
         │                 (M:N junction)                  │
         │                                                 │
         ├──────────────── team_leaders ──────────────────┤ ◄── users
         │                 (M:N junction)                  │
         │                                                 │
         ├──────────────── team_invites ──────────────────┘
         │                 (1:N emails)
         │
         │ contains (1:N)
         ▼
    ┌──────────┐
    │ projects │
    └────┬─────┘
         │
         ├──────────────── project_members ───────────────── users
         │                 (M:N junction)
         │
         │ contains (1:N)
         ├────────────────────────────────────────────────┐
         │                                                │
         ▼                                                ▼
    ┌─────────┐                                     ┌─────────┐
    │  tasks  │                                     │  tags   │
    └────┬────┘                                     └────┬────┘
         │                                               │
         ├──────────────── task_assignees ────────────── users
         │                 (M:N junction)
         │
         ├──────────────── task_tags ────────────────────┘
         │                 (M:N junction)
         │
         │ contains (1:N)
         ├──────────────────────────────────┐
         │                                  │
         ▼                                  ▼
    ┌──────────┐                     ┌───────────┐
    │ comments │ ◄─── self-ref ───►  │ (replies) │
    └──────────┘     (parent_id)     └───────────┘


    ┌──────────┐
    │ messages │ ─── polymorphic ───► teams OR tasks (via channelType/channelId)
    └──────────┘

    ┌───────────────┐
    │ notifications │ ──────────────► users (1:N)
    └───────────────┘
```

---

## 📊 Workflow Diagram Guidance

### Recommended Diagrams to Create

For your official design document, consider creating the following workflow diagrams:

#### 1. User Authentication Flow
```
[Start] → Register/Login → [JWT Token] → Access Protected Routes → [Logout] → [End]
         ↓                      ↓
    Validation Failed    Token Expired
         ↓                      ↓
    Show Error          Redirect to Login
```

#### 2. Team & Project Creation Flow
```
[User] → Create Team → [Manager Role]
                            ↓
                    Invite Members (email)
                            ↓
              [Invited User] → Join with Team ID
                            ↓
              [Manager] → Promote to Leader (optional)
                            ↓
              [Manager/Leader] → Create Project
                            ↓
                    Add Project Members
```

#### 3. Task Lifecycle Flow
```
              ┌─────────────────────────────────────┐
              ↓                                     │
[Create Task] → [TODO] → [IN_PROGRESS] → [DONE] ───┘
     ↑              ↑           ↑          (can reopen)
     │              │           │
(Manager/Leader)  (Any Member - status change only)
```

#### 4. Real-Time Chat Flow
```
[User Opens Chat] → WebSocket Connect → Subscribe to Channel
                                              ↓
                    [Send Message] → Backend Broadcast → All Subscribers
                                              ↓
                                    Message Stored in DB
```

#### 5. Permission Check Flow
```
[API Request] → JWT Validation → Extract User ID
                                      ↓
                    Check Team Membership
                           ↓
              ┌────────────┼────────────┐
              ↓            ↓            ↓
          [Manager]    [Leader]     [Member]
              ↓            ↓            ↓
          Full CRUD   Task CRUD    Status Only
```

---

## 🚀 Quick Start Guide

### Prerequisites
- Java 21+
- Node.js 18+
- PostgreSQL 14+
- Maven 3.8+

### Backend Setup

```bash
# Clone and navigate to project
cd "112 Project"

# Configure database (application.properties or environment variables)
# Set: spring.datasource.url, username, password

# Build and run
mvn clean compile -DskipTests
mvn spring-boot:run
```

Backend starts on `http://localhost:8080`

### Frontend Setup

```bash
# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Configure API URL
echo "VITE_API_URL=http://localhost:8080" > .env

# Start development server
npm run dev
```

Frontend runs on `http://localhost:5173`

---

## 📁 Project Structure

```
112 Project/
├── src/main/java/com/example/webapp/
│   ├── config/          # Security, WebSocket, CORS configuration
│   ├── controller/      # REST API endpoints
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # JPA entities (9 tables)
│   ├── exception/       # Custom exceptions & handlers
│   ├── repository/      # Spring Data repositories
│   ├── security/        # JWT filter, auth provider
│   └── service/         # Business logic
│
├── frontend/src/
│   ├── components/      # Reusable UI components
│   ├── hooks/           # Custom React hooks
│   ├── pages/           # Route-based page components
│   ├── services/        # API service modules
│   ├── store/           # Zustand state stores
│   └── App.tsx          # Main app with routing
│
├── pom.xml              # Maven dependencies
└── README.md            # This documentation
```

---

## 🔐 API Endpoints Summary

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Create new user |
| POST | `/api/auth/login` | Get JWT token |

### Teams
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/teams` | List user's teams |
| POST | `/api/teams` | Create team |
| GET | `/api/teams/{id}` | Get team details |
| POST | `/api/teams/{id}/invite` | Invite member |
| POST | `/api/teams/{id}/join` | Join team |
| POST | `/api/teams/{id}/kick/{userId}` | Remove member |
| POST | `/api/teams/{id}/promote/{userId}` | Promote to leader |
| POST | `/api/teams/{id}/demote/{userId}` | Demote to member |

### Projects
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/teams/{teamId}/projects` | List team projects |
| POST | `/api/projects` | Create project |
| GET | `/api/projects/{id}` | Get project details |
| PUT | `/api/projects/{id}` | Update project |
| DELETE | `/api/projects/{id}` | Delete project |

### Tasks
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/projects/{id}/tasks` | List project tasks |
| POST | `/api/projects/{id}/tasks` | Create task |
| GET | `/api/tasks/{id}` | Get task details |
| PUT | `/api/tasks/{id}` | Update task |
| PATCH | `/api/tasks/{id}/status` | Update status only |
| DELETE | `/api/tasks/{id}` | Delete task |

### Chat (WebSocket)
| Destination | Description |
|-------------|-------------|
| `/app/chat.send` | Send message |
| `/topic/chat/{channelType}/{channelId}` | Subscribe to channel |

---

## ✅ Summary

TaskFlow is a production-ready collaborative task management system featuring:

- ✅ **Secure Authentication**: JWT-based with role hierarchy
- ✅ **Team Collaboration**: Manager → Leader → Member permission model
- ✅ **Project Organization**: Team-scoped projects with member access control
- ✅ **Task Management**: Kanban board with filtering, assignments, tags
- ✅ **Real-time Chat**: WebSocket messaging for teams and tasks
- ✅ **Notifications**: Instant alerts for assignments and updates
- ✅ **6+ Relationship Tables**: Proper relational database design
- ✅ **Modern Stack**: Spring Boot 3.2.1 + React + TypeScript

This documentation serves as a foundation for creating an official design document with detailed workflow diagrams and specifications.
