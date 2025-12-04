import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { api, Task, Project, Attachment } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';

export default function TaskDetail() {
  const { taskId } = useParams<{ taskId: string }>();
  const { user } = useAuth();
  const { toast } = useToast();
  const [task, setTask] = useState<Task | null>(null);
  const [project, setProject] = useState<Project | null>(null);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (user && taskId) {
      loadData();
    }
  }, [user, taskId]);

  const loadData = async () => {
    if (!user || !taskId) return;
    const taskData = await api.getTask(taskId, user.tenantId);
    setTask(taskData);
    
    if (taskData) {
      const projectData = await api.getProject(taskData.projectId);
      setProject(projectData);
    }

    const attachmentsData = await api.getAttachments(taskId, user.tenantId);
    setAttachments(attachmentsData);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!user || !taskId || !selectedFile) return;

    setIsUploading(true);
    try {
      await api.createAttachment(taskId, selectedFile, user.tenantId);
      toast({
        title: 'File added',
        description: `The file "${selectedFile.name}" has been added.`,
      });
      setSelectedFile(null);
      loadData();
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Unable to add the file.',
        variant: 'destructive',
      });
    } finally {
      setIsUploading(false);
    }
  };

  const handleDownload = (attachment: Attachment) => {
    const link = document.createElement('a');
    link.href = attachment.data;
    link.download = attachment.originalName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  if (!task) {
    return <div className="text-center py-12">Task not found</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        {project && (
          <Link to={`/projects/${project.id}`} className="text-sm text-muted-foreground hover:underline">
            ← Back to proect {project.name}
          </Link>
        )}
        <h1 className="text-3xl font-bold mt-2">{task.title}</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Details</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {task.description && (
            <div>
              <p className="text-sm font-medium mb-1">Description</p>
              <p className="text-muted-foreground">{task.description}</p>
            </div>
          )}
          {task.dueDate && (
            <div>
              <p className="text-sm font-medium mb-1">Deadline</p>
              <p className="text-muted-foreground">
                {new Date(task.dueDate).toLocaleDateString('fr-FR')}
              </p>
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Attachments</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex gap-2">
            <Input
              type="file"
              onChange={handleFileChange}
              className="flex-1"
            />
            <Button
              onClick={handleUpload}
              disabled={!selectedFile || isUploading}
            >
              {isUploading ? 'Sending...' : 'Send'}
            </Button>
          </div>

          {attachments.length === 0 ? (
            <p className="text-muted-foreground text-center py-4">No attachments</p>
          ) : (
            <div className="space-y-2">
              {attachments.map((attachment) => (
                <div
                  key={attachment.id}
                  className="flex items-center justify-between p-3 border rounded-lg"
                >
                  <div className="flex-1 min-w-0">
                    <p className="font-medium truncate">{attachment.originalName}</p>
                    <p className="text-sm text-muted-foreground">
                      {(attachment.size / 1024).toFixed(2)} KB · {attachment.mimeType}
                    </p>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleDownload(attachment)}
                  >
                    Download
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
