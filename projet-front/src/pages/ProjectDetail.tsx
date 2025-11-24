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

export default function ProjectDetail() {
  const { projectId } = useParams<{ projectId: string }>();
  const { user } = useAuth();
  const { toast } = useToast();
  const [project, setProject] = useState<Project | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newTask, setNewTask] = useState({ title: '', description: '', dueDate: '' });

  useEffect(() => {
    if (user && projectId) {
      loadData();
    }
  }, [user, projectId]);

  const loadData = async () => {
    if (!user || !projectId) return;
    const projectData = await api.getProject(projectId, user.tenantId);
    setProject(projectData);
    const tasksData = await api.getTasks(projectId, user.tenantId);
    setTasks(tasksData);
  };

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !projectId || !newTask.title.trim()) return;

    try {
      await api.createTask(
        projectId,
        newTask.title,
        newTask.description,
        newTask.dueDate || undefined,
        user.tenantId
      );
      toast({
        title: 'Tâche créée',
        description: `La tâche "${newTask.title}" a été créée avec succès.`,
      });
      setNewTask({ title: '', description: '', dueDate: '' });
      setIsDialogOpen(false);
      loadData();
    } catch (error) {
      toast({
        title: 'Erreur',
        description: 'Impossible de créer la tâche.',
        variant: 'destructive',
      });
    }
  };

  if (!project) {
    return <div className="text-center py-12">Projet introuvable</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link to="/projects" className="text-sm text-muted-foreground hover:underline">
          ← Retour aux projets
        </Link>
        <h1 className="text-3xl font-bold mt-2">{project.name}</h1>
        <p className="text-muted-foreground">
          Créé le {new Date(project.createdAt).toLocaleDateString('fr-FR')}
        </p>
      </div>

      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">Tâches</h2>
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button>Créer une tâche</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Nouvelle tâche</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleCreateTask} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="task-title">Titre</Label>
                <Input
                  id="task-title"
                  value={newTask.title}
                  onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
                  placeholder="Titre de la tâche"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="task-description">Description</Label>
                <Textarea
                  id="task-description"
                  value={newTask.description}
                  onChange={(e) => setNewTask({ ...newTask, description: e.target.value })}
                  placeholder="Description de la tâche"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="task-due-date">Date d'échéance</Label>
                <Input
                  id="task-due-date"
                  type="date"
                  value={newTask.dueDate}
                  onChange={(e) => setNewTask({ ...newTask, dueDate: e.target.value })}
                />
              </div>
              <Button type="submit" className="w-full">Créer</Button>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {tasks.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">Aucune tâche pour ce projet</p>
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
                      Échéance: {new Date(task.dueDate).toLocaleDateString('fr-FR')}
                    </p>
                  )}
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
