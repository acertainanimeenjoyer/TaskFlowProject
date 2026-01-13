import { useState, useEffect } from 'react';
import type { Task } from '../services/task.service';
import { commentService, type Comment } from '../services/comment.service';
import { useAuthStore } from '../store/authStore';
import { useChat } from '../hooks/useChat';

interface TaskDetailModalProps {
  task: Task;
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (taskId: string, data: Partial<Task>) => Promise<void>;
  onDelete: (taskId: string) => Promise<void>;
  projectMembers: Array<{ id: string; email: string }>;
  projectTags?: Array<{id: string, name: string, color: string}>;
  canDelete?: boolean;
  canEdit?: boolean;
}

export const TaskDetailModal = ({ 
  task, 
  isOpen, 
  onClose, 
  onUpdate, 
  onDelete,
  projectMembers,
  projectTags = [],
  canDelete = true,
  canEdit = true
}: TaskDetailModalProps) => {
  const { user } = useAuthStore();
  const { registerAndOpenChat } = useChat();
  const [isEditing, setIsEditing] = useState(false);
  const [name, setName] = useState(task.title);
  const [description, setDescription] = useState(task.description || '');
  const [priority, setPriority] = useState(task.priority);
  const [status, setStatus] = useState(task.status);
  const [dueDate, setDueDate] = useState(
    task.dueDate ? new Date(task.dueDate).toISOString().split('T')[0] : ''
  );
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>(task.tags || []);
  const [assigneeIds, setAssigneeIds] = useState<string[]>(task.assigneeIds || (task.assignedUserId ? [task.assignedUserId] : []));
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showTagSuggestions, setShowTagSuggestions] = useState(false);
  const [showAssigneeDropdown, setShowAssigneeDropdown] = useState(false);

  // Comments state
  const [comments, setComments] = useState<Comment[]>([]);
  const [newCommentText, setNewCommentText] = useState('');
  const [editingCommentId, setEditingCommentId] = useState<string | null>(null);
  const [editingCommentText, setEditingCommentText] = useState('');
  const [isLoadingComments, setIsLoadingComments] = useState(false);
  const [isSendingComment, setIsSendingComment] = useState(false);

  useEffect(() => {
    if (isOpen) {
      loadComments();
    }
  }, [isOpen, task.id]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isLoading) {
        if (isEditing) {
          handleCancelEdit();
        } else {
          onClose();
        }
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, isLoading, isEditing]);

  const handleCancelEdit = () => {
    setIsEditing(false);
    setName(task.title);
    setDescription(task.description || '');
    setPriority(task.priority);
    setStatus(task.status);
    setDueDate(task.dueDate ? new Date(task.dueDate).toISOString().split('T')[0] : '');
    setTags(task.tags || []);
    setAssigneeIds(task.assigneeIds || (task.assignedUserId ? [task.assignedUserId] : []));
    setError('');
  };

  const handleAddTag = (e?: React.KeyboardEvent, suggestedTag?: string) => {
    if (e && e.key !== 'Enter') return;
    e?.preventDefault();
    
    const tag = suggestedTag || tagInput.trim();
    if (!tag) return;
    
    if (tags.includes(tag)) {
      setError('Tag already added');
      return;
    }

    setTags([...tags, tag]);
    setTagInput('');
    setShowTagSuggestions(false);
    setError('');
  };

  const handleRemoveTag = (tag: string) => {
    setTags(tags.filter(t => t !== tag));
  };

  const getFilteredSuggestions = () => {
    if (!tagInput.trim()) return projectTags.filter(t => !tags.includes(t.name));
    return projectTags.filter(t => 
      !tags.includes(t.name) && 
      t.name.toLowerCase().includes(tagInput.toLowerCase())
    );
  };

  const handleUpdate = async () => {
    if (!name.trim()) {
      setError('Task name is required');
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      // Convert tag names to tag IDs
      const tagIds = tags.length > 0 
        ? tags.map(tagName => {
            const tag = projectTags.find(t => t.name === tagName);
            return tag?.id;
          }).filter(Boolean) as string[]
        : undefined;

      await onUpdate(task.id, {
        title: name.trim(),
        description: description.trim() || undefined,
        status,
        priority,
        dueDate: dueDate ? `${dueDate}T00:00:00` : undefined,
        tagIds,
        assigneeIds: assigneeIds.length > 0 ? assigneeIds : undefined,
      });
      setIsEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update task');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this task?')) return;

    setIsLoading(true);
    try {
      await onDelete(task.id);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete task');
    } finally {
      setIsLoading(false);
    }
  };

  const loadComments = async () => {
    setIsLoadingComments(true);
    try {
      const data = await commentService.getTaskComments(task.id);
      setComments(data);
    } catch (err) {
      console.error('Failed to load comments:', err);
    } finally {
      setIsLoadingComments(false);
    }
  };

  const handleSendComment = async (e?: React.KeyboardEvent<HTMLTextAreaElement>) => {
    // If called from keyboard event, only proceed if it's Enter without Shift
    if (e) {
      if (e.key !== 'Enter' || e.shiftKey) return;
      e.preventDefault();
    }

    const text = newCommentText.trim();
    if (!text || isSendingComment) return;

    setIsSendingComment(true);
    try {
      const newComment = await commentService.createComment(task.id, text);
      setComments([...comments, newComment]);
      setNewCommentText('');
    } catch (err) {
      console.error('Failed to send comment:', err);
      setError('Failed to send comment');
    } finally {
      setIsSendingComment(false);
    }
  };

  const handleEditCommentClick = (comment: Comment) => {
    if (comment.userId !== user?.id) return;
    setEditingCommentId(comment.id);
    setEditingCommentText(comment.text);
  };

  const handleCancelCommentEdit = () => {
    setEditingCommentId(null);
    setEditingCommentText('');
  };

  const handleSaveEdit = async (commentId: string) => {
    const text = editingCommentText.trim();
    if (!text) return;

    try {
      const updatedComment = await commentService.updateComment(task.id, commentId, text);
      setComments(comments.map(c => c.id === commentId ? updatedComment : c));
      handleCancelCommentEdit();
    } catch (err) {
      console.error('Failed to update comment:', err);
      setError('Failed to update comment');
    }
  };

  const handleDeleteComment = async (commentId: string) => {
    if (!confirm('Are you sure you want to delete this comment?')) return;

    try {
      await commentService.deleteComment(task.id, commentId);
      setComments(comments.filter(c => c.id !== commentId));
    } catch (err) {
      console.error('Failed to delete comment:', err);
      setError('Failed to delete comment');
    }
  };

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'HIGH': return 'bg-red-100 text-red-800';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800';
      case 'LOW': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'TODO': return 'To Do';
      case 'IN_PROGRESS': return 'In Progress';
      case 'DONE': return 'Done';
      default: return status;
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        {error && (
          <div className="mb-4 rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        {!isEditing ? (
          <div>
            <div className="flex justify-between items-start mb-4">
              <h3 className="text-2xl font-bold text-gray-900">{task.title}</h3>
              <button
                onClick={onClose}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            <div className="mb-4 flex flex-wrap gap-2">
              <span className={`px-2 py-1 rounded-md text-xs font-medium ${getPriorityColor(task.priority)}`}>
                {task.priority}
              </span>
              <span className="px-2 py-1 rounded-md text-xs font-medium bg-blue-100 text-blue-800">
                {getStatusLabel(task.status)}
              </span>
              {task.tags && task.tags.map((tag) => (
                <span key={tag} className="px-2 py-1 rounded-md text-xs font-medium bg-gray-100 text-gray-800">
                  {tag}
                </span>
              ))}
            </div>

            {task.description && (
              <div className="mb-4">
                <h4 className="text-sm font-medium text-gray-700 mb-2">Description</h4>
                <p className="text-gray-600 whitespace-pre-wrap">{task.description}</p>
              </div>
            )}

            <div className="mb-4 space-y-2">
              {((task.assigneeIds && task.assigneeIds.length > 0) || task.assignedUserId) && (
                <div>
                  <span className="text-sm font-medium text-gray-700">Assigned to: </span>
                  <span className="text-sm text-gray-600">
                    {(task.assigneeIds && task.assigneeIds.length > 0)
                      ? task.assigneeIds.map(id => projectMembers.find(m => m.id === id)?.email || id).join(', ')
                      : (projectMembers.find(m => m.id === task.assignedUserId)?.email || task.assignedUserId)}
                  </span>
                </div>
              )}
              {task.dueDate && (
                <div>
                  <span className="text-sm font-medium text-gray-700">Due date: </span>
                  <span className="text-sm text-gray-600">
                    {new Date(task.dueDate).toLocaleDateString()}
                  </span>
                </div>
              )}
              <div>
                <span className="text-sm font-medium text-gray-700">Created: </span>
                <span className="text-sm text-gray-600">
                  {new Date(task.createdAt).toLocaleString()}
                </span>
              </div>
            </div>

            <div className="flex space-x-3">
              {canEdit && (
                <button
                  onClick={() => setIsEditing(true)}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md"
                >
                  Edit Task
                </button>
              )}
              <button
                onClick={() => registerAndOpenChat('task', task.id, task.title)}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
              >
                Open Task Chat
              </button>
              {canDelete && (
                <button
                  onClick={handleDelete}
                  disabled={isLoading}
                  className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md disabled:opacity-50"
                >
                  {isLoading ? 'Deleting...' : 'Delete Task'}
                </button>
              )}
            </div>
          </div>
        ) : (
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4">Edit Task</h3>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Task Name *
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div className="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Status
                </label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as Task['status'])}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="TODO">To Do</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="DONE">Done</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Priority
                </label>
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as Task['priority'])}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="LOW">Low</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HIGH">High</option>
                </select>
              </div>
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Due Date
              </label>
              <input
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>

            <div className="mb-4 relative">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Assign To
              </label>
              <button
                type="button"
                onClick={() => setShowAssigneeDropdown(!showAssigneeDropdown)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md text-left bg-white hover:bg-gray-50 focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              >
                {assigneeIds.length === 0 
                  ? <span className="text-gray-500">Select assignees...</span>
                  : <span className="text-gray-900">{assigneeIds.length} member(s) selected</span>
                }
              </button>
              {showAssigneeDropdown && (
                <div className="absolute z-20 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg max-h-48 overflow-y-auto">
                  {projectMembers.length === 0 ? (
                    <p className="px-3 py-2 text-sm text-gray-500">No members available</p>
                  ) : (
                    projectMembers.map((member) => (
                      <label key={member.id} className="flex items-center px-3 py-2 hover:bg-gray-50 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={assigneeIds.includes(member.id)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setAssigneeIds([...assigneeIds, member.id]);
                            } else {
                              setAssigneeIds(assigneeIds.filter(id => id !== member.id));
                            }
                          }}
                          className="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                        />
                        <span className="ml-2 text-sm text-gray-700">{member.email}</span>
                      </label>
                    ))
                  )}
                </div>
              )}
              {assigneeIds.length > 0 && (
                <div className="flex flex-wrap gap-1 mt-2">
                  {assigneeIds.map(id => {
                    const member = projectMembers.find(m => m.id === id);
                    return (
                      <span key={id} className="inline-flex items-center px-2 py-1 rounded-md text-xs font-medium bg-blue-100 text-blue-800">
                        {member?.email || id}
                        <button
                          type="button"
                          onClick={() => setAssigneeIds(assigneeIds.filter(aid => aid !== id))}
                          className="ml-1 text-blue-600 hover:text-blue-800"
                        >
                          ×
                        </button>
                      </span>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Tags
              </label>
              <div className="flex space-x-2 mb-2 relative">
                <div className="flex-1 relative">
                  <input
                    type="text"
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyDown={handleAddTag}
                    onFocus={() => setShowTagSuggestions(true)}
                    onBlur={() => setTimeout(() => setShowTagSuggestions(false), 200)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder="Enter tag and press Enter"
                  />
                  {showTagSuggestions && getFilteredSuggestions().length > 0 && (
                    <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg max-h-40 overflow-y-auto">
                      {getFilteredSuggestions().map((tag) => (
                        <button
                          key={tag.id}
                          type="button"
                          onClick={() => handleAddTag(undefined, tag.name)}
                          className="w-full text-left px-3 py-2 hover:bg-blue-50 text-sm"
                        >
                          {tag.name}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => handleAddTag()}
                  className="px-4 py-2 text-sm font-medium text-white bg-gray-600 hover:bg-gray-700 rounded-md"
                >
                  Add
                </button>
              </div>
              {tags.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {tags.map((tag) => (
                    <span
                      key={tag}
                      className="inline-flex items-center px-2 py-1 rounded-md text-xs font-medium bg-blue-100 text-blue-800"
                    >
                      {tag}
                      <button
                        type="button"
                        onClick={() => handleRemoveTag(tag)}
                        className="ml-1 text-blue-600 hover:text-blue-800"
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>

            <div className="flex space-x-3">
              <button
                onClick={handleUpdate}
                disabled={isLoading}
                className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
              >
                {isLoading ? 'Saving...' : 'Save Changes'}
              </button>
              <button
                onClick={handleCancelEdit}
                disabled={isLoading}
                className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
              >
                Cancel
              </button>
            </div>
          </div>
        )}

        {/* Comments Section */}
        <div className="border-t border-gray-200 pt-4">
          <h3 className="text-lg font-medium text-gray-900 mb-4">Comments</h3>
          
          {isLoadingComments ? (
            <p className="text-sm text-gray-500">Loading comments...</p>
          ) : (
            <>
              {/* Comment List */}
              <div className="space-y-3 mb-4 max-h-60 overflow-y-auto">
                {comments.length === 0 ? (
                  <p className="text-sm text-gray-500">No comments yet. Be the first to comment!</p>
                ) : (
                  comments.map((comment) => (
                    <div key={comment.id} className="bg-gray-50 p-3 rounded-md">
                      {editingCommentId === comment.id ? (
                        <div className="space-y-2">
                          <textarea
                            value={editingCommentText}
                            onChange={(e) => setEditingCommentText(e.target.value)}
                            onKeyDown={(e) => {
                              if (e.key === 'Escape') {
                                handleCancelCommentEdit();
                              } else if (e.key === 'Enter' && !e.shiftKey) {
                                e.preventDefault();
                                handleSaveEdit(comment.id);
                              }
                            }}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                            rows={2}
                            autoFocus
                          />
                          <div className="flex space-x-2">
                            <button
                              onClick={() => handleSaveEdit(comment.id)}
                              className="px-3 py-1 text-xs font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md"
                            >
                              Save
                            </button>
                            <button
                              onClick={handleCancelCommentEdit}
                              className="px-3 py-1 text-xs font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
                            >
                              Cancel
                            </button>
                          </div>
                        </div>
                      ) : (
                        <>
                          <div className="flex items-start justify-between mb-1">
                            <span className="text-sm font-medium text-gray-900">
                              {comment.userEmail}
                            </span>
                            <span className="text-xs text-gray-500">
                              {new Date(comment.createdAt).toLocaleString()}
                            </span>
                          </div>
                          <p
                            onClick={() => handleEditCommentClick(comment)}
                            className={`text-sm text-gray-700 whitespace-pre-wrap ${
                              comment.userId === user?.id ? 'cursor-pointer hover:bg-gray-100 p-1 rounded' : ''
                            }`}
                          >
                            {comment.text}
                          </p>
                          {comment.userId === user?.id && (
                            <div className="flex space-x-2 mt-2">
                              <button
                                onClick={() => handleEditCommentClick(comment)}
                                className="text-xs text-blue-600 hover:text-blue-800"
                              >
                                Edit
                              </button>
                              <button
                                onClick={() => handleDeleteComment(comment.id)}
                                className="text-xs text-red-600 hover:text-red-800"
                              >
                                Delete
                              </button>
                            </div>
                          )}
                        </>
                      )}
                    </div>
                  ))
                )}
              </div>

              {/* New Comment Input */}
              <div className="space-y-2">
                <textarea
                  value={newCommentText}
                  onChange={(e) => setNewCommentText(e.target.value)}
                  onKeyDown={handleSendComment}
                  placeholder="Add a comment... (Enter to send, Shift+Enter for new line)"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  rows={3}
                  disabled={isSendingComment}
                />
                <button
                  onClick={() => handleSendComment()}
                  disabled={isSendingComment || !newCommentText.trim()}
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
                >
                  {isSendingComment ? 'Sending...' : 'Send'}
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
