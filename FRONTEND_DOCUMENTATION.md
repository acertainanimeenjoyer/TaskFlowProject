# Frontend Documentation

## Project Overview

React-based Single Page Application (SPA) built with TypeScript and Vite, designed to connect to the Spring Boot backend Task Management System.

**Tech Stack:**
- React 18.3.1
- TypeScript 5.6.2
- Vite 7.2.6
- Tailwind CSS 3.4.17
- React Router DOM 7.1.1
- Axios 1.7.9
- Zustand 5.0.2
- TanStack Query 5.62.11
- STOMP.js (WebSocket)

---

## Architecture

### Directory Structure

```
frontend/
├── src/
│   ├── components/         # Reusable UI components
│   │   └── ProtectedRoute.tsx
│   ├── hooks/             # Custom React hooks
│   │   └── useWebSocket.ts
│   ├── pages/             # Page components
│   │   ├── Login.tsx
│   │   ├── Register.tsx
│   │   └── Dashboard.tsx
│   ├── services/          # API service layer
│   │   ├── api.ts
│   │   ├── auth.service.ts
│   │   ├── chat.service.ts
│   │   ├── project.service.ts
│   │   ├── task.service.ts
│   │   ├── team.service.ts
│   │   └── websocket.service.ts
│   ├── store/             # State management
│   │   └── authStore.ts
│   ├── App.tsx            # Main app component with routing
│   ├── main.tsx           # App entry point
│   └── index.css          # Global styles
├── public/                # Static assets
├── vite.config.ts         # Vite configuration
├── tailwind.config.js     # Tailwind configuration
├── postcss.config.js      # PostCSS configuration
└── package.json           # Dependencies
```

---

## Backend Integration

### API Base URL
- **Development:** `http://localhost:8080/api`
- **WebSocket:** `http://localhost:8080/ws`

The Vite dev server (port 5173) is configured with proxies to forward requests to the Spring Boot backend (port 8080).

### Authentication Flow
1. User submits credentials via `/login` page
2. Frontend calls `POST /api/auth/login` → Backend validates credentials
3. Backend returns JWT token + user data
4. Frontend stores token in localStorage and Zustand store
5. All subsequent API requests include token in `Authorization: Bearer {token}` header
6. On 401 response, frontend auto-logs out and redirects to `/login`

---

## Service Layer

All API communication is abstracted through service files that wrap Axios calls.

### `services/api.ts`
Central Axios instance with request/response interceptors.

**Features:**
- Base URL: `http://localhost:8080/api`
- Request interceptor: Automatically adds JWT token from localStorage
- Response interceptor: Handles 401 errors (auto-logout + redirect)

**Usage:**
```typescript
import api from './api';
const response = await api.get('/endpoint');
```

### `services/auth.service.ts`
Authentication operations.

**Methods:**
| Method | Endpoint | Description |
|--------|----------|-------------|
| `login(email, password)` | `POST /auth/login` | Login user, returns JWT + user data |
| `register(data)` | `POST /auth/register` | Register new user |
| `logout()` | - | Clears localStorage (client-side only) |
| `getCurrentUser()` | - | Retrieves user from localStorage |
| `isAuthenticated()` | - | Checks if token exists |

**Backend Reference:** See `API_REFERENCE_OLD.md` → Authentication section

### `services/team.service.ts`
Team management operations.

**Methods:**
| Method | Backend Endpoint | Description |
|--------|------------------|-------------|
| `createTeam(name)` | `POST /teams` | Create new team |
| `getTeam(teamId)` | `GET /teams/{id}` | Get team details |
| `getUserTeams()` | `GET /teams/user` | Get user's teams |
| `inviteMember(teamId, email)` | `POST /teams/{id}/invite` | Invite member to team |
| `joinTeam(teamId)` | `POST /teams/{id}/join` | Join team |
| `removeMember(teamId, userId)` | `DELETE /teams/{id}/members/{userId}` | Remove member |

**TypeScript Interface:**
```typescript
interface Team {
  id: string;
  name: string;
  managerEmail: string;
  memberIds: string[];
  inviteEmails: string[];
  createdAt: string;
}
```

**Backend Reference:** `TeamController.java` (8 endpoints total)

### `services/project.service.ts`
Project management operations.

**Methods:**
| Method | Backend Endpoint | Description |
|--------|------------------|-------------|
| `createProject(data)` | `POST /projects` | Create new project |
| `getProject(projectId)` | `GET /projects/{id}` | Get project details |
| `getTeamProjects(teamId)` | `GET /teams/{teamId}/projects` | Get team's projects |
| `updateProject(projectId, data)` | `PUT /projects/{id}` | Update project |
| `deleteProject(projectId)` | `DELETE /projects/{id}` | Delete project |
| `addMember(projectId, email)` | `POST /projects/{id}/members` | Add project member |
| `removeMember(projectId, userId)` | `DELETE /projects/{id}/members/{userId}` | Remove member |

**TypeScript Interface:**
```typescript
interface Project {
  id: string;
  name: string;
  description?: string;
  teamId: string;
  ownerUserId: string;
  memberEmails: string[];
  createdAt: string;
  updatedAt: string;
}
```

**Backend Reference:** `ProjectController.java` (9 endpoints total)

### `services/task.service.ts`
Task management with advanced filtering.

**Methods:**
| Method | Backend Endpoint | Description |
|--------|------------------|-------------|
| `createTask(data)` | `POST /tasks` | Create new task |
| `getTask(taskId)` | `GET /tasks/{id}` | Get task details |
| `getProjectTasks(projectId)` | `GET /projects/{projectId}/tasks` | Get project tasks |
| `updateTask(taskId, data)` | `PUT /tasks/{id}` | Update task |
| `deleteTask(taskId)` | `DELETE /tasks/{id}` | Delete task |
| `searchTasks(params)` | `GET /tasks/search` | Advanced search with filters |
| `getOverdueTasks(userId?)` | `GET /tasks/overdue` | Get overdue tasks |
| `getTaskStatistics(projectId)` | `GET /projects/{projectId}/tasks/statistics` | Task counts by status |

**TypeScript Interfaces:**
```typescript
interface Task {
  id: string;
  name: string;
  description?: string;
  projectId: string;
  assignedUserId?: string;
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  dueDate?: string;
  tags?: string[];
  createdAt: string;
  updatedAt: string;
}

interface TaskSearchParams {
  projectId?: string;
  assignedUserId?: string;
  status?: string;
  priority?: string;
  tags?: string[];
  dueDateFrom?: string;
  dueDateTo?: string;
}

interface TaskStatistics {
  totalTasks: number;
  todoTasks: number;
  inProgressTasks: number;
  doneTasks: number;
  overdueTasks: number;
  highPriorityTasks: number;
}
```

**Backend Reference:** `TaskController.java` (11 endpoints total) + Search/Filter feature with 6 MongoDB compound indexes

### `services/chat.service.ts`
Chat history retrieval (REST fallback).

**Methods:**
| Method | Backend Endpoint | Description |
|--------|------------------|-------------|
| `getTeamChatHistory(teamId, page, size)` | `GET /chat/team/{teamId}/history` | Get team chat messages |
| `getTaskChatHistory(taskId, page, size)` | `GET /chat/task/{taskId}/history` | Get task chat messages |

**TypeScript Interface:**
```typescript
interface ChatMessage {
  id: string;
  channelType: 'TEAM' | 'TASK';
  channelId: string;
  senderId: string;
  senderEmail: string;
  text: string;
  messageType: 'MESSAGE' | 'JOIN' | 'LEAVE';
  timestamp: string;
}
```

**Backend Reference:** `ChatController.java` (2 REST endpoints for history)

### `services/websocket.service.ts`
Real-time chat via WebSocket (STOMP protocol).

**Class:** `WebSocketService` (singleton)

**Methods:**
| Method | Description |
|--------|-------------|
| `connect(token)` | Establish STOMP connection via SockJS with JWT |
| `disconnect()` | Close connection and cleanup subscriptions |
| `subscribeToChannel(type, id, callback)` | Subscribe to team/task chat channel |
| `unsubscribeFromChannel(type, id)` | Unsubscribe from channel |
| `sendMessage(type, id, text, messageType)` | Send chat message (MESSAGE/JOIN/LEAVE) |
| `isConnected()` | Check connection status |

**WebSocket Configuration:**
- Connection URL: `http://localhost:8080/ws/chat?token={jwt}`
- Topic pattern: `/topic/chat/{channelType}/{channelId}`
- Send destination: `/app/chat.message`

**Usage:**
```typescript
import { wsService } from './websocket.service';

// Connect
await wsService.connect(token);

// Subscribe to team chat
wsService.subscribeToChannel('team', teamId, (message) => {
  console.log('New message:', message);
});

// Send message
await wsService.sendMessage('team', teamId, 'Hello!', 'MESSAGE');

// Cleanup
wsService.unsubscribeFromChannel('team', teamId);
wsService.disconnect();
```

**Backend Reference:** `WebSocketController.java` + `WebSocketConfig.java` (STOMP over SockJS)

---

## State Management

### Zustand Store: `store/authStore.ts`

Global authentication state with localStorage persistence.

**State:**
```typescript
interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}
```

**Actions:**
| Action | Description |
|--------|-------------|
| `login(email, password)` | Authenticate user, store token + user data |
| `register(data)` | Register new user (then redirect to login) |
| `logout()` | Clear auth state and localStorage |
| `setUser(user)` | Update user data |

**Usage:**
```typescript
import { useAuthStore } from './store/authStore';

function Component() {
  const { user, isAuthenticated, login, logout } = useAuthStore();
  
  // Use state and actions
}
```

---

## Custom Hooks

### `hooks/useWebSocket.ts`

React hook for real-time chat functionality.

**Parameters:**
- `channelType`: 'team' | 'task'
- `channelId`: string (team/task ID)

**Returns:**
```typescript
{
  messages: WebSocketMessage[];  // Received messages
  isConnected: boolean;          // Connection status
  error: string | null;          // Error message
  sendMessage: (text, type?) => Promise<void>;  // Send function
}
```

**Usage:**
```typescript
import { useWebSocket } from '../hooks/useWebSocket';

function ChatComponent({ teamId }) {
  const { messages, isConnected, sendMessage } = useWebSocket('team', teamId);
  
  return (
    <div>
      {messages.map(msg => <div key={msg.id}>{msg.text}</div>)}
      <button onClick={() => sendMessage('Hello!')}>Send</button>
    </div>
  );
}
```

**Backend Integration:** Auto-connects to WebSocket server and manages subscriptions/cleanup

---

## Routing

### React Router Configuration

**Routes:**
| Path | Component | Protected | Description |
|------|-----------|-----------|-------------|
| `/` | Navigate → `/dashboard` or `/login` | - | Root redirect |
| `/login` | `Login.tsx` | No | Login form |
| `/register` | `Register.tsx` | No | Registration form |
| `/dashboard` | `Dashboard.tsx` | Yes | Main dashboard |
| `/teams/:teamId` | `TeamDetail.tsx` | Yes | Team management (TODO) |
| `/projects/:projectId` | `ProjectDetail.tsx` | Yes | Project board (TODO) |

**Protected Routes:**
All routes marked "Yes" use the `ProtectedRoute` component which:
1. Checks `isAuthenticated` from Zustand store
2. Redirects to `/login` if not authenticated
3. Renders children if authenticated

---

## Pages

### `pages/Login.tsx`
Login form with email/password authentication.

**Features:**
- Form validation (required fields)
- Error display (red alert box)
- Loading state (disabled submit button)
- Calls `authStore.login()` → Redirects to `/dashboard` on success
- Link to `/register`

**Backend Endpoint:** `POST /api/auth/login`

### `pages/Register.tsx`
Registration form for new users.

**Features:**
- Email, password, confirm password, first name, last name fields
- Password match validation
- Error display
- Loading state
- Calls `authStore.register()` → Redirects to `/login` on success
- Link to `/login`

**Backend Endpoint:** `POST /api/auth/register`

### `pages/Dashboard.tsx`
Main dashboard after login.

**Features:**
- Navigation bar with user email and logout button
- Welcome message with user's name
- Three card placeholders (Teams, Projects, Tasks)
- Logout functionality

**Status:** Functional placeholder (needs integration with actual data)

---

## Development Setup

### Installation
```bash
cd frontend
npm install
```

### Run Dev Server
```bash
npm run dev
```
Dev server starts at `http://localhost:5173`

### Build for Production
```bash
npm run build
```

### Environment Requirements
- Node.js 22.11.0+ (recommended 22.12.0+)
- Backend server running on `http://localhost:8080`

---

## Configuration Files

### `vite.config.ts`
Vite build tool configuration.

**Key Settings:**
```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
    '/ws': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      ws: true,  // WebSocket proxy
    },
  },
}
```

**Purpose:** Proxies API and WebSocket requests to backend during development

### `tailwind.config.js`
Tailwind CSS configuration.

**Content Paths:**
```javascript
content: [
  "./index.html",
  "./src/**/*.{js,ts,jsx,tsx}",
]
```

**Purpose:** Scans files for Tailwind classes, enables tree-shaking

### `postcss.config.js`
PostCSS configuration for Tailwind processing.

**Plugins:**
- `tailwindcss` - Processes Tailwind directives
- `autoprefixer` - Adds vendor prefixes

---

## Package Dependencies

### Production Dependencies (23 packages)
| Package | Version | Purpose |
|---------|---------|---------|
| react | 18.3.1 | UI library |
| react-dom | 18.3.1 | React DOM rendering |
| react-router-dom | 7.1.1 | Client-side routing |
| axios | 1.7.9 | HTTP client |
| zustand | 5.0.2 | State management |
| @tanstack/react-query | 5.62.11 | Data fetching/caching |
| @stomp/stompjs | 7.0.0 | WebSocket STOMP client |
| sockjs-client | 1.6.1 | SockJS WebSocket fallback |
| react-hook-form | 7.54.2 | Form state management |
| zod | 3.24.1 | Schema validation |

### Dev Dependencies (251 packages)
| Package | Version | Purpose |
|---------|---------|---------|
| vite | 7.2.6 | Build tool |
| typescript | 5.6.2 | Type checking |
| tailwindcss | 3.4.17 | Utility-first CSS |
| @vitejs/plugin-react | 4.3.4 | Vite React plugin |
| autoprefixer | 10.4.20 | CSS vendor prefixes |
| postcss | 8.4.49 | CSS processing |
| @types/react | 18.3.18 | React TypeScript types |
| @types/sockjs-client | 1.5.5 | SockJS TypeScript types |

**Total:** 274 packages, 0 vulnerabilities

---

## Implementation Status

### ✅ Completed
- [x] Project scaffolding (Vite + React + TypeScript)
- [x] Tailwind CSS configuration
- [x] Service layer (7 files covering all backend endpoints)
- [x] Authentication flow with JWT
- [x] State management (Zustand auth store)
- [x] Routing setup (React Router)
- [x] Protected routes
- [x] Login page
- [x] Register page
- [x] Dashboard page
- [x] WebSocket service for real-time chat
- [x] Custom WebSocket hook
- [x] API proxy configuration
- [x] TanStack Query setup

### 🚧 In Progress / TODO
- [ ] Team management UI (list, detail, members)
- [ ] Project board UI (Kanban view)
- [ ] Task cards with drag-and-drop
- [ ] Chat interface (UI for WebSocket messages)
- [ ] Task filtering UI (status, priority, tags, dates)
- [ ] User profile page
- [ ] Notifications
- [ ] File upload/download UI
- [ ] Error boundaries
- [ ] Loading skeletons
- [ ] Toast notifications
- [ ] Form validation with Zod schemas
- [ ] Unit tests (React Testing Library)
- [ ] E2E tests (Playwright/Cypress)

---

## Backend API Reference

For detailed backend API documentation, see:
- **Main API Reference:** `API_REFERENCE_OLD.md`
- **Backend Source:** `src/main/java/com/taskmanager/`

**Backend Collections (MongoDB):**
1. `users` - User accounts
2. `teams` - Team management
3. `projects` - Project information
4. `tasks` - Task data
5. `chat_messages` - Chat history
6. `files` - File metadata (GridFS)
7. `team_invites` - Pending invitations
8. `project_members` - Project access control

**Backend Features:**
- 71 compiled source files
- 39+ REST endpoints
- WebSocket chat (STOMP protocol)
- JWT authentication
- MongoDB with 6 compound indexes for search/filter
- GridFS file storage
- Spring Boot 3.2.1 + Java 21

---

## Development Workflow

### Adding New Features

1. **Create Service Method** (if new endpoint)
   ```typescript
   // src/services/example.service.ts
   export const exampleService = {
     getData: async () => {
       const response = await api.get('/endpoint');
       return response.data;
     }
   };
   ```

2. **Create/Update Component**
   ```typescript
   // src/components/Example.tsx
   import { exampleService } from '../services/example.service';
   
   export const Example = () => {
     // Component logic
   };
   ```

3. **Add Route** (if new page)
   ```typescript
   // src/App.tsx
   <Route path="/example" element={<Example />} />
   ```

### Backend Integration Checklist
- [ ] Verify backend endpoint exists and is documented
- [ ] Create TypeScript interface matching backend DTO
- [ ] Create service method with proper error handling
- [ ] Test with backend running on localhost:8080
- [ ] Handle loading states
- [ ] Handle error states
- [ ] Add authentication if required

---

## Testing

### Manual Testing
1. Start backend: `mvn spring-boot:run` (port 8080)
2. Start frontend: `npm run dev` (port 5173)
3. Open browser: `http://localhost:5173`
4. Test authentication flow:
   - Register new user → Should redirect to login
   - Login with credentials → Should redirect to dashboard
   - Check localStorage for token
   - Logout → Should clear token and redirect to login

### API Testing with Frontend
Use browser DevTools Network tab to inspect:
- Request headers (Authorization: Bearer token)
- Response status codes
- Response data structure
- WebSocket connection (ws:// protocol)

---

## Troubleshooting

### Common Issues

**1. CORS Errors**
- **Symptom:** Browser console shows CORS policy error
- **Solution:** Backend must have `@CrossOrigin` annotations or global CORS config
- **Check:** `WebConfig.java` should allow origin `http://localhost:5173`

**2. 401 Unauthorized on Every Request**
- **Symptom:** Auto-logout loop
- **Solution:** Check token storage in localStorage
- **Debug:** `localStorage.getItem('token')` in browser console

**3. WebSocket Connection Failed**
- **Symptom:** Chat not working, connection error in console
- **Solution:** Verify backend WebSocket endpoint is accessible
- **Check:** Backend logs for WebSocket connection attempts

**4. Proxy Not Working**
- **Symptom:** 404 on API calls
- **Solution:** Restart Vite dev server after config changes
- **Verify:** Check `vite.config.ts` proxy settings

**5. TypeScript Errors**
- **Symptom:** Red squiggles in VSCode
- **Solution:** Run `npm install` to ensure all types are installed
- **Check:** Verify `@types/*` packages in `package.json`

---

## Security Considerations

### JWT Token Storage
- Stored in localStorage (accessible to JavaScript)
- Automatically included in all API requests
- Cleared on logout or 401 response
- **Note:** Consider httpOnly cookies for production (requires backend changes)

### API Security
- All authenticated endpoints require valid JWT
- Backend validates token on every request
- Token expiration handled by backend
- Frontend clears token on 401 (unauthorized)

### WebSocket Security
- JWT passed as query parameter during connection
- Backend validates token before accepting connection
- Connection rejected if token invalid/expired

---

## Performance Optimization

### Current Optimizations
- Vite's fast HMR (Hot Module Replacement)
- Code splitting via dynamic imports (future)
- Tailwind CSS purging (removes unused styles in production)
- TanStack Query caching (reduces redundant API calls)

### Future Improvements
- Lazy loading routes with `React.lazy()`
- Image optimization
- Service worker for offline support
- Bundle size analysis and optimization
- Memoization for expensive computations

---

## Deployment

### Production Build
```bash
npm run build
```
Outputs to `dist/` directory (static files).

### Deployment Options
1. **Static Hosting:** Vercel, Netlify, GitHub Pages
2. **Traditional Server:** Nginx, Apache
3. **Container:** Docker with Nginx
4. **Cloud:** AWS S3 + CloudFront, Azure Static Web Apps

### Environment Variables
Create `.env` file for production:
```env
VITE_API_BASE_URL=https://api.yourdomain.com
VITE_WS_URL=wss://api.yourdomain.com/ws
```

Update `src/services/api.ts`:
```typescript
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
```

---

## Next Steps

### Immediate Priorities
1. **Team Management UI** - Display teams, create teams, manage members
2. **Project Board** - Kanban-style task board with drag-and-drop
3. **Chat Interface** - Real-time chat UI using WebSocket hook
4. **Task Filtering** - Advanced search UI with all filter options

### Future Enhancements
- Dark mode toggle
- Responsive mobile design
- Internationalization (i18n)
- Progressive Web App (PWA) features
- Analytics integration
- Error tracking (Sentry)

---

## Contact & Support

For backend-related questions, refer to:
- `API_REFERENCE_OLD.md` - Complete backend API documentation
- Backend source code: `src/main/java/com/taskmanager/`

For frontend issues:
- Check browser console for errors
- Verify backend is running on port 8080
- Ensure all npm packages are installed
- Review this documentation for integration details

---

**Last Updated:** December 6, 2025  
**Frontend Version:** 1.0.0 (Initial Setup)  
**Backend Compatibility:** Spring Boot 3.2.1 / Java 21
