export interface ProjectListResponse {
  id: string;
  name: string;
  createdOn: string;
}

export type Project = ProjectListResponse & { tenantId?: string };

export interface ProjectCreateRequest {
  name: string;
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

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

class ApiService {
  private readonly baseUrl = API_BASE_URL;

  private getAuthToken() {
    return localStorage.getItem('smarttasks_token');
  }

  private handleUnauthorized() {
    localStorage.removeItem('smarttasks_user');
    localStorage.removeItem('smarttasks_token');
    window.dispatchEvent(new Event('smarttasks:logout'));
  }

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

  private buildUrl(path: string, params?: Record<string, string | undefined>) {
    const base = this.baseUrl.endsWith('/') ? this.baseUrl.slice(0, -1) : this.baseUrl;
    const fullPath = path.startsWith('/') ? path : `/${path}`;
    const searchParams = params
      ? new URLSearchParams(
          Object.entries(params).filter(([, value]) => value !== undefined),
        )
      : null;

    const queryString = searchParams && searchParams.toString();
    return `${base}${fullPath}${queryString ? `?${queryString}` : ''}`;
  }

  private async request<T>(
    path: string,
    options: RequestInit = {},
    params?: Record<string, string | undefined>,
    allowNotFound = false,
  ): Promise<T> {
    const token = this.getAuthToken();
    const response = await fetch(this.buildUrl(path, params), {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.headers || {}),
      },
    });

    if (allowNotFound && response.status === 404) {
      return null as T;
    }

    if (response.status === 401) {
      this.handleUnauthorized();
    }

    if (!response.ok) {
      const errorText = await response.text().catch(() => '');
      throw new Error(errorText || `Request failed with status ${response.status}`);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    return response.json() as Promise<T>;
  }

  async getProjects(tenantId: string): Promise<Project[]> {
    return this.request<ProjectListResponse[]>('/api/projects', { method: 'GET' }, { tenantId });
  }

  async getProject(id: string, tenantId: string): Promise<Project | null> {
    return this.request<Project | null>(
      `/api/projects/${id}`,
      { method: 'GET' },
      { tenantId },
      true,
    );
  }

  async createProject(name: string, tenantId: string): Promise<Project> {
    const payload: ProjectCreateRequest = { name };
    return this.request<Project>(
      '/api/projects',
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
      { tenantId },
    );
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
