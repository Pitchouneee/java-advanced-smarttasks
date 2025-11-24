export interface Project {
  id: string;
  name: string;
  tenantId: string;
  createdAt: string;
}

export interface Task {
  id: string;
  projectId: string;
  title: string;
  description: string;
  dueDate?: string;
  tenantId: string;
  createdAt: string;
}

export interface Attachment {
  id: string;
  taskId: string;
  originalName: string;
  size: number;
  mimeType: string;
  data: string;
  createdAt: string;
}

class ApiService {
  private getStorageKey(key: string, tenantId: string) {
    return `smarttasks_${tenantId}_${key}`;
  }

  private getData<T>(key: string, tenantId: string): T[] {
    const data = localStorage.getItem(this.getStorageKey(key, tenantId));
    return data ? JSON.parse(data) : [];
  }

  private setData<T>(key: string, tenantId: string, data: T[]) {
    localStorage.setItem(this.getStorageKey(key, tenantId), JSON.stringify(data));
  }

  async getProjects(tenantId: string): Promise<Project[]> {
    return this.getData<Project>('projects', tenantId);
  }

  async getProject(id: string, tenantId: string): Promise<Project | null> {
    const projects = this.getData<Project>('projects', tenantId);
    return projects.find(p => p.id === id) || null;
  }

  async createProject(name: string, tenantId: string): Promise<Project> {
    const projects = this.getData<Project>('projects', tenantId);
    const newProject: Project = {
      id: `project_${Date.now()}`,
      name,
      tenantId,
      createdAt: new Date().toISOString(),
    };
    projects.push(newProject);
    this.setData('projects', tenantId, projects);
    return newProject;
  }

  async getTasks(projectId: string, tenantId: string): Promise<Task[]> {
    const tasks = this.getData<Task>('tasks', tenantId);
    return tasks.filter(t => t.projectId === projectId);
  }

  async getTask(id: string, tenantId: string): Promise<Task | null> {
    const tasks = this.getData<Task>('tasks', tenantId);
    return tasks.find(t => t.id === id) || null;
  }

  async createTask(projectId: string, title: string, description: string, dueDate: string | undefined, tenantId: string): Promise<Task> {
    const tasks = this.getData<Task>('tasks', tenantId);
    const newTask: Task = {
      id: `task_${Date.now()}`,
      projectId,
      title,
      description,
      dueDate,
      tenantId,
      createdAt: new Date().toISOString(),
    };
    tasks.push(newTask);
    this.setData('tasks', tenantId, tasks);
    return newTask;
  }

  async getAttachments(taskId: string, tenantId: string): Promise<Attachment[]> {
    const attachments = this.getData<Attachment>('attachments', tenantId);
    return attachments.filter(a => a.taskId === taskId);
  }

  async createAttachment(taskId: string, file: File, tenantId: string): Promise<Attachment> {
    const attachments = this.getData<Attachment>('attachments', tenantId);
    
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const newAttachment: Attachment = {
          id: `attachment_${Date.now()}`,
          taskId,
          originalName: file.name,
          size: file.size,
          mimeType: file.type,
          data: reader.result as string,
          createdAt: new Date().toISOString(),
        };
        attachments.push(newAttachment);
        this.setData('attachments', tenantId, attachments);
        resolve(newAttachment);
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }
}

export const api = new ApiService();
