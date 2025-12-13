import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { api, Project } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { useToast } from '@/hooks/use-toast';

const PAGE_SIZE = 9;

export default function Projects() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [projects, setProjects] = useState<Project[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newProjectName, setNewProjectName] = useState('');

  useEffect(() => {
    if (user) {
      loadProjects(page);
    }
  }, [page, user]);

  const loadProjects = async (pageToLoad: number) => {
    if (!user) return;
    setIsLoading(true);
    try {
      const data = await api.getProjects(pageToLoad, PAGE_SIZE);
      setProjects(data.content);
      setPage(data.number);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreateProject = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedName = newProjectName.trim();
    if (!user || !trimmedName) return;
    if (trimmedName.length < 3 || trimmedName.length > 50) {
      toast({
        title: 'Invalid name',
        description: 'Project name must be between 3 and 50 characters.',
        variant: 'destructive',
      });
      return;
    }

    try {
      await api.createProject(trimmedName);
      toast({
        title: 'Project created',
        description: `The project "${trimmedName}" was successfully created.`,
        variant: 'success',
      });
      setNewProjectName('');
      setIsDialogOpen(false);
      setPage(0);
      loadProjects(0);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Unable to create the project.',
        variant: 'destructive',
      });
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Projects</h1>
          <p className="text-muted-foreground">Manage your projects</p>
        </div>
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button>Create a project</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>New project</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleCreateProject} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="project-name">Project name</Label>
                <Input
                  id="project-name"
                  value={newProjectName}
                  onChange={(e) => setNewProjectName(e.target.value)}
                  placeholder="My new project"
                  required
                />
              </div>
              <Button type="submit" className="w-full">Create</Button>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {projects.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">{isLoading ? 'Loading projects...' : 'No projects for now'}</p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <Link key={project.id} to={`/projects/${project.id}`}>
              <Card className="hover:bg-accent transition-colors cursor-pointer h-full">
                <CardHeader>
                  <CardTitle>{project.name}</CardTitle>
                </CardHeader>
                <CardContent>
                  <p className="text-sm text-muted-foreground">
                    Created on {new Date(project.createdOn).toLocaleDateString('fr-FR')}
                  </p>
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}

      <div className="flex items-center justify-between border rounded-lg px-4 py-3">
        <div className="text-sm text-muted-foreground">
          Page {page + 1} of {Math.max(totalPages, 1)} • {totalElements} project{totalElements === 1 ? '' : 's'}
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((p) => Math.max(p - 1, 0))}
            disabled={page === 0 || isLoading}
          >
            Previous
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
            disabled={page >= totalPages - 1 || isLoading || totalPages === 0}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  );
}
