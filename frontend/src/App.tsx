import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/AppLayout';
import { ErrorBoundary } from './components/ErrorBoundary';
import { ToastContainer } from './components/ToastContainer';
import { useAuthStore } from './store/authStore';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Dashboard } from './pages/Dashboard';
import { Profile } from './pages/Profile';
import { Settings } from './pages/Settings';
import { Teams } from './pages/Teams';
import TeamDetail from './pages/TeamDetail';
import { Projects } from './pages/Projects';
import { ProjectDetail } from './pages/ProjectDetail';
import { TaskBoard } from './pages/TaskBoard';

function App() {
  const { isAuthenticated } = useAuthStore();

  return (
    <ErrorBoundary>
      <ToastContainer />
      <Routes>
        <Route
          path="/"
          element={isAuthenticated ? <Navigate to="/dashboard" /> : <Navigate to="/login" />}
        />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <AppLayout>
              <Dashboard />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <AppLayout>
              <Profile />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/teams"
        element={
          <ProtectedRoute>
            <AppLayout>
              <Teams />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/teams/:teamId"
        element={
          <ProtectedRoute>
            <AppLayout>
              <TeamDetail />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects"
        element={
          <ProtectedRoute>
            <AppLayout>
              <Projects />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:projectId"
        element={
          <ProtectedRoute>
            <AppLayout>
              <ProjectDetail />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/projects/:projectId/board"
        element={
          <ProtectedRoute>
            <AppLayout>
              <TaskBoard />
            </AppLayout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/settings"
        element={
          <ProtectedRoute>
            <AppLayout>
              <Settings />
            </AppLayout>
          </ProtectedRoute>
        }
      />
    </Routes>
    </ErrorBoundary>
  );
}

export default App;
