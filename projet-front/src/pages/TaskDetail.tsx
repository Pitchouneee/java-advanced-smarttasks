import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { api, Task, Project, Attachment } from '@/services/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useToast } from '@/hooks/use-toast';

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';
const ATTACHMENT_PAGE_SIZE = 10;

export default function TaskDetail() {
  const { taskId } = useParams<{ taskId: string }>();
  const { user, token } = useAuth();
  const { toast } = useToast();
  const [task, setTask] = useState<Task | null>(null);
  const [project, setProject] = useState<Project | null>(null);
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [attachmentPage, setAttachmentPage] = useState(0);
  const [attachmentTotalPages, setAttachmentTotalPages] = useState(0);
  const [attachmentTotalElements, setAttachmentTotalElements] = useState(0);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  useEffect(() => {
    if (user && taskId) {
      loadData(attachmentPage);
    }
  }, [user, taskId, attachmentPage]);

  const loadData = async (page: number) => {
    if (!user || !taskId) return;
    try {
      const taskData = await api.getTask(taskId);
      setTask(taskData);

      if (!taskData) {
        setProject(null);
        setAttachments([]);
        setAttachmentTotalPages(0);
        setAttachmentTotalElements(0);
        return;
      }

      const [projectData, attachmentsData] = await Promise.all([
        api.getProject(taskData.projectId),
        api.getAttachments(taskId, page, ATTACHMENT_PAGE_SIZE),
      ]);

      setProject(projectData);
      setAttachments(attachmentsData.content);
      setAttachmentPage(attachmentsData.number);
      setAttachmentTotalPages(attachmentsData.totalPages);
      setAttachmentTotalElements(attachmentsData.totalElements);
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Unable to load the task details.',
        variant: 'destructive',
      });
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!task || !selectedFile) return;

    setIsUploading(true);
    try {
      await api.createAttachment(task.id, selectedFile);
      toast({
        title: 'File added',
        description: `The file "${selectedFile.name}" has been added.`,
      });
      setSelectedFile(null);
      setAttachmentPage(0);
      loadData(0);
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

  const handleDownload = async (attachment: Attachment) => {
    const downloadPath = `/api/attachments/${attachment.id}/download`;
    const base = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL;
    const url = `${base}${downloadPath}`;

    try {
      const response = await fetch(url, {
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      if (!response.ok) {
        throw new Error(`Download failed with status ${response.status}`);
      }

      const blob = await response.blob();
      const blobUrl = window.URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = attachment.originalName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      window.URL.revokeObjectURL(blobUrl);
    } catch (error) {
      toast({
        title: 'Download unavailable',
        description: 'Unable to download the file. Please check your session and try again.',
        variant: 'destructive',
      });
    }
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
              disabled={!selectedFile || isUploading || !task}
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

          <div className="flex items-center justify-between border rounded-lg px-3 py-2">
            <div className="text-sm text-muted-foreground">
              Page {attachmentPage + 1} of {Math.max(attachmentTotalPages, 1)} • {attachmentTotalElements} attachment{attachmentTotalElements === 1 ? '' : 's'}
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setAttachmentPage((p) => Math.max(p - 1, 0))}
                disabled={attachmentPage === 0}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setAttachmentPage((p) => Math.min(p + 1, attachmentTotalPages - 1))}
                disabled={attachmentPage >= attachmentTotalPages - 1 || attachmentTotalPages === 0}
              >
                Next
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
