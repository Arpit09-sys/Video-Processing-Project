import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_URL || '/api/v1';

const api = axios.create({
  baseURL: API_BASE,
  timeout: 120000,
});

export const videoApi = {
  upload: (file, title, onProgress) => {
    const formData = new FormData();
    formData.append('file', file);
    if (title) formData.append('title', title);

    return api.post('/videos/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        if (onProgress) onProgress(percent);
      },
    });
  },

  list: (page = 0, size = 20) =>
    api.get(`/videos?page=${page}&size=${size}`),

  get: (id) =>
    api.get(`/videos/${id}`),

  delete: (id) =>
    api.delete(`/videos/${id}`),

  getStats: () =>
    api.get('/dashboard/stats'),
};

export default api;
