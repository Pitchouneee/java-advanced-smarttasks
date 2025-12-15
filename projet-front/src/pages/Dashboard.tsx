import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { api, DashboardResponse } from '@/services/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

export default function Dashboard() {
  const { user } = useAuth();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      loadData();
    }
  }, [user]);

  const loadData = async () => {
    if (!user) return;
    try {
      setIsLoading(true);
      setError(null);
      const response = await api.getDashboard();
      setDashboard(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to load dashboard data');
    } finally {
      setIsLoading(false);
    }
  };

  const latestProjects = dashboard?.latestProjects ?? [];

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
            <div className="text-2xl font-bold">
              {dashboard?.activeProjectsCount ?? 0}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium">Total tasks</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {dashboard?.totalTasksCount ?? 0}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-medium">Overdue tasks</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-destructive">
              {dashboard?.overdueTasksCount ?? 0}
            </div>
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
          {error && <p className="text-destructive text-center py-4">{error}</p>}
          {isLoading && !error && (
            <p className="text-muted-foreground text-center py-8">Loading dashboard...</p>
          )}
          {!isLoading && !error && latestProjects.length === 0 && (
            <p className="text-muted-foreground text-center py-8">No projects for now</p>
          )}
          {!isLoading && !error && latestProjects.length > 0 && (
            <div className="space-y-2">
              {latestProjects.map((project) => (
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
