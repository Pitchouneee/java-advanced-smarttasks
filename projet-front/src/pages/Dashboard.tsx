import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { api, Project, Task } from '@/services/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

export default function Dashboard() {
  const { user } = useAuth();
  const [projects, setProjects] = useState<Project[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);

  useEffect(() => {
    if (user) {
      loadData();
    }
  }, [user]);

  const loadData = async () => {
    if (!user) return;
    const allProjects = await api.getProjects(user.tenantId);
    setProjects(allProjects);

    const allTasks: Task[] = [];
    for (const project of allProjects) {
      const projectTasks = await api.getTasks(project.id, user.tenantId);
      allTasks.push(...projectTasks);
    }
    setTasks(allTasks);
  };

  const overdueTasks = tasks.filter(t => t.dueDate && new Date(t.dueDate) < new Date());

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Hello, {user?.name}</h1>
        <p className="text-muted-foreground">Here is an overview of your projects and tasks</p>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium">Active projects</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{projects.length}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium">Total tasks</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{tasks.length}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium">Overdue tasks</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-destructive">{overdueTasks.length}</div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle>Latest projects</CardTitle>
          <Button asChild variant="outline" size="sm">
            <Link to="/projects">View all projects</Link>
          </Button>
        </CardHeader>
        <CardContent>
          {projects.length === 0 ? (
            <p className="text-muted-foreground text-center py-8">No projects for now</p>
          ) : (
            <div className="space-y-2">
              {projects.slice(-3).reverse().map((project) => (
                <Link
                  key={project.id}
                  to={`/projects/${project.id}`}
                  className="block p-3 rounded-lg border hover:bg-accent transition-colors"
                >
                  <div className="font-medium">{project.name}</div>
                  <div className="text-sm text-muted-foreground">
                    Created on {new Date(project.createdOn).toLocaleDateString('fr-FR')}
                  </div>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
