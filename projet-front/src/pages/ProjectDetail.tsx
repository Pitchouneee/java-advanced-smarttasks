import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { api, Project, Task } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { useToast } from '@/hooks/use-toast';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';
const TASK_PAGE_SIZE = 10;

export default function ProjectDetail() {
  const { projectId } = useParams<{ projectId: string }>();
  const { user } = useAuth();
  const { toast } = useToast();
  const [project, setProject] = useState<Project | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [taskPage, setTaskPage] = useState(0);
  const [taskTotalPages, setTaskTotalPages] = useState(0);
  const [taskTotalElements, setTaskTotalElements] = useState(0);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newTask, setNewTask] = useState({ title: '', description: '', dueDate: '' });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!projectId) return;
    const controller = new AbortController();
    loadData(taskPage, controller.signal);
    return () => controller.abort();
  }, [projectId, taskPage]);

  const loadData = async (page: number, signal?: AbortSignal) => {
    if (!projectId) return;
    setIsLoading(true);
    setError(null);

    try {
      const base = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
      const token = localStorage.getItem('smarttasks_token');

      const response = await fetch(`${base}/api/projects/${projectId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        signal,
      });

      if (response.status === 404) {
        setProject(null);
        setTasks([]);
        setTaskTotalPages(0);
        setTaskTotalElements(0);
        setError('Project not found.');
        return;
      }

      if (!response.ok) {
        throw new Error(`Failed to fetch project (${response.status})`);
      }

      const projectData: Project = await response.json();
      setProject(projectData);

      if (user) {
        const tasksData = await api.getTasks(projectId, page, TASK_PAGE_SIZE);
        setTasks(tasksData.content);
        setTaskPage(tasksData.number);
        setTaskTotalPages(tasksData.totalPages);
        setTaskTotalElements(tasksData.totalElements);
      } else {
        setTasks([]);
        setTaskTotalPages(0);
        setTaskTotalElements(0);
      }
    } catch (err) {
      if ((err as Error).name === 'AbortError') return;
      setError('Unable to load the project. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !projectId || !newTask.title.trim()) return;

    try {
      await api.createTask(
        projectId,
        newTask.title,
        newTask.description,
        newTask.dueDate || undefined
      );
      toast({
        title: 'Task created',
        description: `The task "${newTask.title}" was successfully created.`,
      });
      setNewTask({ title: '', description: '', dueDate: '' });
      setIsDialogOpen(false);
      setTaskPage(0);
      loadData(0);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Unable to create the task.',
        variant: 'destructive',
      });
    }
  };

  if (isLoading) {
    return <div className="text-center py-12">Loading project...</div>;
  }

  if (error) {
    return <div className="text-center py-12 text-destructive">{error}</div>;
  }

  if (!project) {
    return <div className="text-center py-12">Project not found</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link to="/projects" className="text-sm text-muted-foreground hover:underline">
          ← Back to projects
        </Link>
        <h1 className="text-3xl font-bold mt-2">{project.name}</h1>
        <p className="text-muted-foreground">
          Created on {new Date(project.createdOn).toLocaleDateString('fr-FR')}
        </p>
      </div>

      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Tasks</h2>
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button>Create a task</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>New task</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleCreateTask} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="task-title">Title</Label>
                <Input
                  id="task-title"
                  value={newTask.title}
                  onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                  placeholder="Task title"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="task-description">Description</Label>
                <Textarea
                  id="task-description"
                  value={newTask.description}
                  onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
                  placeholder="Task description"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="task-due-date">Due date</Label>
                <Input
                  id="task-due-date"
                  type="date"
                  value={newTask.dueDate}
                  onChange={(e) => setNewTask({ ...newTask, dueDate: e.target.value })}
                />
              </div>
              <Button type="submit" className="w-full">Create</Button>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {tasks.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">{isLoading ? 'Loading tasks...' : 'No tasks for this project'}</p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => (
            <Link key={task.id} to={`/tasks/${task.id}`}>
              <Card className="hover:bg-accent transition-colors cursor-pointer">
                <CardHeader>
                  <CardTitle className="text-lg">{task.title}</CardTitle>
                </CardHeader>
                <CardContent>
                  {task.description && (
                    <p className="text-sm text-muted-foreground mb-2">{task.description}</p>
                  )}
                  {task.dueDate && (
                    <p className="text-sm">
                      Deadline: {new Date(task.dueDate).toLocaleDateString('fr-FR')}
                    </p>
                  )}
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}

      <div className="flex items-center justify-between border rounded-lg px-4 py-3">
        <div className="text-sm text-muted-foreground">
          Page {taskPage + 1} of {Math.max(taskTotalPages, 1)} • {taskTotalElements} task{taskTotalElements === 1 ? '' : 's'}
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setTaskPage((p) => Math.max(p - 1, 0))}
            disabled={taskPage === 0 || isLoading}
          >
            Previous
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setTaskPage((p) => Math.min(p + 1, taskTotalPages - 1))}
            disabled={taskPage >= taskTotalPages - 1 || isLoading || taskTotalPages === 0}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  );
}
