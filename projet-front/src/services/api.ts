export interface ProjectListResponse {
  id: string;
  name: string;
  createdOn: string;
}

export type Project = ProjectListResponse;

export interface ProjectCreateRequest {
  name: string;
}

export interface Task {
  id: string;
  projectId: string;
  title: string;
  description: string;
  dueDate?: string;
  createdAt: string;
}

export interface Attachment {
  id: number;
  originalName: string;
  size: number;
  mimeType: string;
  /**
   * Relative or absolute endpoint to download the file.
   */
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

  private buildUrl(path: string, params?: Record<string, string | undefined>) {
    const base = this.baseUrl.endsWith('/') ? this.baseUrl.slice(0, -1) : this.baseUrl;
    const fullPath = path.startsWith('/') ? path : `/${path}`;
    return `${base}${fullPath}`;
  }

  private async request<T>(
    path: string,
    options: RequestInit = {},
    allowNotFound = false,
  ): Promise<T> {
    const token = this.getAuthToken();
    const isFormData = options.body instanceof FormData;
    const headers: Record<string, string> = {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers as Record<string, string> | undefined),
    };

    if (!isFormData && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(this.buildUrl(path), {
      ...options,
      headers,
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

  async getProjects(): Promise<Project[]> {
    return this.request<ProjectListResponse[]>('/api/projects', { method: 'GET' });
  }

  async getProject(id: string): Promise<Project | null> {
    return this.request<Project | null>(
      `/api/projects/${id}`,
      { method: 'GET' },
      true,
    );
  }

  async createProject(name: string): Promise<Project> {
    const payload: ProjectCreateRequest = { name };
    return this.request<Project>(
      '/api/projects',
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
    );
  }

  async getTasks(projectId: string): Promise<Task[]> {
    return this.request<Task[]>(`/api/projects/${projectId}/tasks`, { method: 'GET' });
  }

  async getTask(id: string): Promise<Task | null> {
    return this.request<Task | null>(`/api/tasks/${id}`, { method: 'GET' }, true);
  }

  async createTask(projectId: string, title: string, description: string, dueDate: string | undefined): Promise<Task> {
    const payload = {
      title,
      description,
      dueDate,
    };

    return this.request<Task>(
      `/api/projects/${projectId}/tasks`,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
    );
  }

  async getAttachments(taskId: string): Promise<Attachment[]> {
    return this.request<Attachment[]>(`/api/tasks/${taskId}/attachments`, { method: 'GET' });
  }

  async createAttachment(taskId: string, file: File): Promise<Attachment> {
    const formData = new FormData();
    formData.append('file', file);

    return this.request<Attachment>(
      `/api/tasks/${taskId}/attachments`,
      {
        method: 'POST',
        body: formData,
      },
    );
  }
}

export const api = new ApiService();
