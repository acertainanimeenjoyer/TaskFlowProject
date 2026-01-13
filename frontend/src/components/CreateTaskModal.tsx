import { useState, useEffect } from 'react';

interface CreateTaskModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: {
    title: string;
    description?: string;
    status?: string;
    priority?: string;
    dueDate?: string;
    tagIds?: string[];
    assigneeIds?: string[];
  }) => Promise<void>;
  initialStatus?: 'TODO' | 'IN_PROGRESS' | 'DONE';
  projectMembers: Array<{ id: string; email: string }>;
  projectTags?: Array<{id: string, name: string, color: string}>;
}

export const CreateTaskModal = ({ 
  isOpen, 
  onClose, 
  onSubmit, 
  initialStatus = 'TODO',
  projectMembers,
  projectTags = []
}: CreateTaskModalProps) => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH'>('MEDIUM');
  const [dueDate, setDueDate] = useState('');
  const [tagInput, setTagInput] = useState('');
  const [tags, setTags] = useState<string[]>([]);
  const [assigneeIds, setAssigneeIds] = useState<string[]>([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showTagSuggestions, setShowTagSuggestions] = useState(false);
  const [showAssigneeDropdown, setShowAssigneeDropdown] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isLoading) {
        handleClose();
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
      return () => window.removeEventListener('keydown', handleKeyDown);
    }
  }, [isOpen, isLoading]);

  const handleClose = () => {
    setName('');
    setDescription('');
    setPriority('MEDIUM');
    setDueDate('');
    setTagInput('');
    setTags([]);
    setAssigneeIds([]);
    setError('');
    onClose();
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
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

      await onSubmit({
        title: name.trim(),
        description: description.trim() || undefined,
        status: initialStatus,
        priority,
        dueDate: dueDate ? `${dueDate}T00:00:00` : undefined,
        tagIds,
        assigneeIds: assigneeIds.length > 0 ? assigneeIds : undefined,
      });
      handleClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create task');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
        <h3 className="text-lg font-medium text-gray-900 mb-4">Create New Task</h3>
        
        {error && (
          <div className="mb-4 rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-4">
            <label htmlFor="taskName" className="block text-sm font-medium text-gray-700 mb-2">
              Task Name *
            </label>
            <input
              id="taskName"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter task name"
              autoFocus
            />
          </div>

          <div className="mb-4">
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
              Description
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter task description (optional)"
            />
          </div>

          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label htmlFor="priority" className="block text-sm font-medium text-gray-700 mb-2">
                Priority
              </label>
              <select
                id="priority"
                value={priority}
                onChange={(e) => setPriority(e.target.value as 'LOW' | 'MEDIUM' | 'HIGH')}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
              </select>
            </div>

            <div>
              <label htmlFor="dueDate" className="block text-sm font-medium text-gray-700 mb-2">
                Due Date
              </label>
              <input
                id="dueDate"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
            </div>
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
            <label htmlFor="tags" className="block text-sm font-medium text-gray-700 mb-2">
              Tags
            </label>
            <div className="flex space-x-2 mb-2 relative">
              <div className="flex-1 relative">
                <input
                  id="tags"
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
              type="submit"
              disabled={isLoading}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50"
            >
              {isLoading ? 'Creating...' : 'Create Task'}
            </button>
            <button
              type="button"
              onClick={handleClose}
              disabled={isLoading}
              className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 rounded-md"
            >
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
