import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  DndContext,
  type DragEndEvent,
  DragOverlay,
  type DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { taskService, type Task } from '../services/task.service';
import { projectService, type Project } from '../services/project.service';
import { useAuthStore } from '../store/authStore';
import { CreateTaskModal } from '../components/CreateTaskModal';
import { TaskDetailModal } from '../components/TaskDetailModal';

interface TaskCardProps {
  task: Task;
  onClick: () => void;
  isSelected: boolean;
}

interface TaskCardPropsExtended extends TaskCardProps {
  onTagClick?: (tag: string) => void;
  onStatusChange?: (newStatus: 'TODO' | 'IN_PROGRESS' | 'DONE') => void;
}

const TaskCard = ({ task, onClick, isSelected, onTagClick, onStatusChange }: TaskCardPropsExtended) => {
  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'HIGH': return 'bg-red-100 text-red-800 border-red-300';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800 border-yellow-300';
      case 'LOW': return 'bg-green-100 text-green-800 border-green-300';
      default: return 'bg-gray-100 text-gray-800 border-gray-300';
    }
  };

  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && task.status !== 'DONE';

  const getNextStatuses = (): Array<{ status: 'TODO' | 'IN_PROGRESS' | 'DONE'; label: string }> => {
    switch (task.status) {
      case 'TODO': return [{ status: 'IN_PROGRESS', label: '▶ Start' }, { status: 'DONE', label: '✓ Done' }];
      case 'IN_PROGRESS': return [{ status: 'TODO', label: '◀ Back' }, { status: 'DONE', label: '✓ Done' }];
      case 'DONE': return [{ status: 'TODO', label: '↺ Reopen' }, { status: 'IN_PROGRESS', label: '▶ Resume' }];
      default: return [];
    }
  };

  return (
    <div
      onClick={onClick}
      className={`p-3 bg-white rounded-lg border-2 cursor-pointer hover:shadow-md transition-shadow ${
        isSelected ? 'border-blue-500 ring-2 ring-blue-200' : 'border-gray-200'
      } ${isOverdue ? 'bg-red-50' : ''}`}
    >
      <h4 className="font-medium text-gray-900 mb-2">{task.title}</h4>
      
      <div className="flex flex-wrap gap-1 mb-2">
        <span className={`px-2 py-0.5 rounded text-xs font-medium ${getPriorityColor(task.priority)}`}>
          {task.priority}
        </span>
        {task.tags && task.tags.slice(0, 2).map((tag) => (
          <span
            key={tag}
            onClick={(e) => {
              e.stopPropagation();
              onTagClick?.(tag);
            }}
            className="px-2 py-0.5 rounded text-xs bg-blue-100 text-blue-700 hover:bg-blue-200 cursor-pointer"
          >
            {tag}
          </span>
        ))}
        {task.tags && task.tags.length > 2 && (
          <span className="px-2 py-0.5 rounded text-xs bg-gray-100 text-gray-700">
            +{task.tags.length - 2}
          </span>
        )}
      </div>

      <div className="flex justify-between items-center text-xs text-gray-500 mb-2">
        {((task.assigneeIds && task.assigneeIds.length > 0) || task.assignedUserId) && (
          <span className="truncate">👤 {task.assigneeIds && task.assigneeIds.length > 0 
            ? `${task.assigneeIds.length} assigned` 
            : (task.assignedUserId ? task.assignedUserId.slice(0, 8) : 'N/A')}</span>
        )}
        {task.dueDate && (
          <span className={isOverdue ? 'text-red-600 font-medium' : ''}>
            📅 {new Date(task.dueDate).toLocaleDateString()}
          </span>
        )}
      </div>

      {/* Quick status change buttons */}
      <div className="flex gap-1 pt-2 border-t border-gray-100">
        {getNextStatuses().map(({ status, label }) => (
          <button
            key={status}
            onClick={(e) => {
              e.stopPropagation();
              onStatusChange?.(status);
            }}
            className="flex-1 px-2 py-1 text-xs rounded bg-gray-50 hover:bg-gray-200 text-gray-600 transition-colors"
          >
            {label}
          </button>
        ))}
      </div>
    </div>
  );
};

export const TaskBoard = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [projectMembers, setProjectMembers] = useState<Array<{ id: string; email: string }>>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createModalStatus, setCreateModalStatus] = useState<'TODO' | 'IN_PROGRESS' | 'DONE'>('TODO');
  const [selectedTask, setSelectedTask] = useState<Task | null>(null);
  const [focusedColumn, setFocusedColumn] = useState<'TODO' | 'IN_PROGRESS' | 'DONE'>('TODO');
  const [selectedCardIndex, setSelectedCardIndex] = useState<number>(0);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [filterTag, setFilterTag] = useState<string | null>(null);
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    })
  );

  useEffect(() => {
    if (projectId) {
      loadData();
    }
  }, [projectId]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (showCreateModal || selectedTask) return;

      // Create task shortcuts - only for managers/leaders
      if ((e.key === 'n' || e.key === 'N' || e.key === 'c' || e.key === 'C') && !e.ctrlKey && !e.metaKey) {
        if (project?.canManageTasks) {
          e.preventDefault();
          setCreateModalStatus(focusedColumn);
          setShowCreateModal(true);
        }
        return;
      }

      // Column navigation
      if (e.key === 'ArrowLeft') {
        e.preventDefault();
        if (focusedColumn === 'IN_PROGRESS') setFocusedColumn('TODO');
        else if (focusedColumn === 'DONE') setFocusedColumn('IN_PROGRESS');
        setSelectedCardIndex(0);
      } else if (e.key === 'ArrowRight') {
        e.preventDefault();
        if (focusedColumn === 'TODO') setFocusedColumn('IN_PROGRESS');
        else if (focusedColumn === 'IN_PROGRESS') setFocusedColumn('DONE');
        setSelectedCardIndex(0);
      }

      // Card navigation within column
      const columnTasks = getTasksByStatus(focusedColumn);
      if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedCardIndex(prev => Math.max(0, prev - 1));
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedCardIndex(prev => Math.min(columnTasks.length - 1, prev + 1));
      }

      // Open task detail
      if (e.key === 'Enter' && columnTasks.length > 0) {
        e.preventDefault();
        setSelectedTask(columnTasks[selectedCardIndex]);
      }

      // Toggle done
      if (e.key === 'd' || e.key === 'D') {
        e.preventDefault();
        const task = columnTasks[selectedCardIndex];
        if (task) {
          const newStatus = task.status === 'DONE' ? 'TODO' : 'DONE';
          handleUpdateTask(task.id, { status: newStatus });
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [showCreateModal, selectedTask, focusedColumn, selectedCardIndex, tasks, project?.canManageTasks]);

  const loadData = async () => {
    if (!projectId) return;
    
    setIsLoading(true);
    try {
      const [projectData, tasksData, projectTagsData] = await Promise.all([
        projectService.getProject(projectId),
        taskService.getProjectTasks(projectId),
        projectService.getTags(projectId),
      ]);
      
      // Set project data with tags (both full objects for modals, and names for display)
      setProject({ 
        ...projectData, 
        tags: projectTagsData.map(t => t.name), // For display
        tagObjects: projectTagsData // Store full objects for modals
      });
      
      // Handle case where backend returns Page object or non-array
      const tasksArray = Array.isArray(tasksData)
        ? tasksData
        : ((tasksData as any)?.content ?? []);
      setTasks(tasksArray);

      // Build member list from project data (memberIds and memberEmails are parallel arrays)
      const memberIds = projectData.memberIds ?? [];
      const memberEmails = projectData.memberEmails ?? [];
      const members = memberIds.map((id: string, index: number) => ({
        id,
        email: memberEmails[index] || id
      }));
      setProjectMembers(members);
    } catch (err) {
      setError('Failed to load task board');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateTask = async (data: any) => {
    if (!projectId) return;
    await taskService.createTask({ ...data, projectId });
    await loadData();
  };

  const handleUpdateTask = async (taskId: string, data: Partial<Task>) => {
    await taskService.updateTask(taskId, data);
    await loadData();
  };

  const handleDeleteTask = async (taskId: string) => {
    await taskService.deleteTask(taskId);
    await loadData();
  };

  const handleDragStart = (event: DragStartEvent) => {
    setActiveId(event.active.id as string);
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveId(null);

    if (!over) return;

    const taskId = active.id as string;
    const newStatus = over.id as 'TODO' | 'IN_PROGRESS' | 'DONE';
    
    const task = tasks.find(t => t.id === taskId);
    if (task && task.status !== newStatus) {
      await handleUpdateTask(taskId, { status: newStatus });
    }
  };

  const getTasksByStatus = (status: 'TODO' | 'IN_PROGRESS' | 'DONE') => {
    return (tasks ?? []).filter(task => task.status === status);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <p className="text-gray-500">Loading task board...</p>
      </div>
    );
  }

  const activeTask = activeId ? tasks.find(t => t.id === activeId) : null;

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <button
                onClick={() => navigate(`/projects/${projectId}`)}
                className="text-gray-600 hover:text-gray-900 mr-4"
              >
                ← Back
              </button>
              <h1 className="text-xl font-bold text-gray-900">{project?.name} - Task Board</h1>
            </div>
            <div className="flex items-center space-x-4">
              <button
                onClick={() => navigate('/dashboard')}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900"
              >
                Dashboard
              </button>
              <span className="text-sm text-gray-700">{user?.email}</span>
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </nav>

      <div className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          {error && (
            <div className="mb-4 rounded-md bg-red-50 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <div className="mb-4">
            <div className="text-sm text-gray-600">
              {project?.canManageTasks ? (
                <p>Keyboard shortcuts: N/C = create task, Arrow keys = navigate, Enter = open, D = toggle done</p>
              ) : (
                <p>Keyboard shortcuts: Arrow keys = navigate, Enter = open, D = toggle done</p>
              )}
              {!project?.canManageTasks && (
                <p className="text-yellow-600 mt-1">You can change task status but cannot create or delete tasks.</p>
              )}
              {filterTag && (
                <div className="mt-2 flex items-center space-x-2">
                  <span className="text-xs">Filtering by tag:</span>
                  <span className="px-2 py-1 rounded text-xs bg-blue-100 text-blue-700 font-medium">
                    {filterTag}
                  </span>
                  <button
                    onClick={() => setFilterTag(null)}
                    className="text-xs text-red-600 hover:text-red-800 font-medium"
                  >
                    Clear filter
                  </button>
                </div>
              )}
            </div>
          </div>

          <DndContext
            sensors={sensors}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
          >
            <div className="grid grid-cols-3 gap-6">
              {(['TODO', 'IN_PROGRESS', 'DONE'] as const).map((status) => {
                const columnTasks = getTasksByStatus(status);
                const statusLabel = status === 'TODO' ? 'To Do' : status === 'IN_PROGRESS' ? 'In Progress' : 'Done';
                
                return (
                  <div
                    key={status}
                    id={status}
                    className={`bg-gray-100 rounded-lg p-4 min-h-[500px] ${
                      focusedColumn === status ? 'ring-2 ring-blue-400' : ''
                    }`}
                    onClick={() => setFocusedColumn(status)}
                  >
                    <div className="flex justify-between items-center mb-4">
                      <h2 className="font-semibold text-gray-900">
                        {statusLabel} ({columnTasks.length})
                      </h2>
                      {project?.canManageTasks && (
                        <button
                          onClick={() => {
                            setCreateModalStatus(status);
                            setShowCreateModal(true);
                          }}
                          className="text-blue-600 hover:text-blue-800 text-sm"
                        >
                          + Add
                        </button>
                      )}
                    </div>
                    
                    <div className="space-y-3">
                      {columnTasks.map((task, index) => (
                        <div
                          key={task.id}
                          draggable
                          onDragStart={() => setActiveId(task.id)}
                          onDragEnd={() => setActiveId(null)}
                        >
                          <TaskCard
                            task={task}
                            onClick={() => setSelectedTask(task)}
                            isSelected={focusedColumn === status && selectedCardIndex === index}
                            onTagClick={(tag) => setFilterTag(tag)}
                            onStatusChange={(newStatus) => handleUpdateTask(task.id, { status: newStatus })}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>

            <DragOverlay>
              {activeTask && (
                <div className="opacity-50">
                  <TaskCard task={activeTask} onClick={() => {}} isSelected={false} onTagClick={() => {}} />
                </div>
              )}
            </DragOverlay>
          </DndContext>
        </div>
      </div>

      <CreateTaskModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={handleCreateTask}
        initialStatus={createModalStatus}
        projectMembers={projectMembers}
        projectTags={project?.tagObjects}
      />

      {selectedTask && (
        <TaskDetailModal
          task={selectedTask}
          isOpen={true}
          onClose={() => setSelectedTask(null)}
          onUpdate={handleUpdateTask}
          onDelete={handleDeleteTask}
          projectMembers={projectMembers}
          projectTags={project?.tagObjects}
          canDelete={project?.canManageTasks ?? false}
          canEdit={project?.canManageTasks ?? false}
        />
      )}
    </div>
  );
};
