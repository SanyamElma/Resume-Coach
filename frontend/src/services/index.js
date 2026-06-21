import apiClient from './apiClient';

// Each helper unwraps the { success, data } envelope and returns `data` directly,
// so React Query caches the payload, not the transport wrapper.
const unwrap = (promise) => promise.then((res) => res.data.data);

export const authApi = {
  register: (payload) => unwrap(apiClient.post('/api/auth/register', payload)),
  login: (payload) => unwrap(apiClient.post('/api/auth/login', payload)),
  refresh: (refreshToken) => unwrap(apiClient.post('/api/auth/refresh', { refreshToken })),
  logout: (refreshToken) => apiClient.post('/api/auth/logout', { refreshToken }),
  forgotPassword: (email) => apiClient.post('/api/auth/forgot-password', { email }),
  resetPassword: (payload) => apiClient.post('/api/auth/reset-password', payload),
};

export const profileApi = {
  get: () => unwrap(apiClient.get('/api/profile')),
  update: (payload) => unwrap(apiClient.put('/api/profile', payload)),
};

export const resumeApi = {
  upload: (file, name) => {
    const form = new FormData();
    form.append('file', file);
    if (name) form.append('name', name);
    return unwrap(
      apiClient.post('/api/resume/upload', form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }),
    );
  },
  list: (page = 0, size = 10) => unwrap(apiClient.get('/api/resume', { params: { page, size } })),
  get: (id) => unwrap(apiClient.get(`/api/resume/${id}`)),
  remove: (id) => apiClient.delete(`/api/resume/${id}`),
};

export const jobApi = {
  create: (payload) => unwrap(apiClient.post('/api/job', payload)),
  list: (page = 0, size = 10) => unwrap(apiClient.get('/api/job', { params: { page, size } })),
  get: (id) => unwrap(apiClient.get(`/api/job/${id}`)),
};

export const analysisApi = {
  run: (resumeId, jobDescriptionId) =>
    unwrap(apiClient.post('/api/analysis/run', { resumeId, jobDescriptionId })),
  get: (id) => unwrap(apiClient.get(`/api/analysis/${id}`)),
  history: (page = 0, size = 10) =>
    unwrap(apiClient.get('/api/analysis', { params: { page, size } })),
};

export const interviewApi = {
  generateQuestions: (payload) => unwrap(apiClient.post('/api/interview/questions', payload)),
  start: (payload) => unwrap(apiClient.post('/api/interview/start', payload)),
  sendMessage: (sessionId, message) =>
    unwrap(apiClient.post('/api/interview/message', { sessionId, message })),
  complete: (id) => unwrap(apiClient.post(`/api/interview/${id}/complete`)),
  get: (id) => unwrap(apiClient.get(`/api/interview/${id}`)),
  history: (page = 0, size = 10) =>
    unwrap(apiClient.get('/api/interview/history', { params: { page, size } })),
};

export const dashboardApi = {
  get: () => unwrap(apiClient.get('/api/dashboard')),
};

export const adminApi = {
  metrics: () => unwrap(apiClient.get('/api/admin/metrics')),
  users: (page = 0, size = 20) => unwrap(apiClient.get('/api/admin/users', { params: { page, size } })),
  deleteUser: (id) => apiClient.delete(`/api/admin/users/${id}`),
};
