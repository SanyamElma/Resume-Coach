import axios from 'axios';

/**
 * Central Axios instance.
 *
 * Responsibilities:
 *  - attach the bearer access token to every request,
 *  - transparently refresh an expired access token once (queueing concurrent 401s),
 *  - unwrap the backend's { success, data, error } envelope at the call sites.
 */
const TOKEN_KEY = 'ra_access_token';
const REFRESH_KEY = 'ra_refresh_token';

// Resolve the API base URL. Empty → same-origin (dev uses the Vite proxy). A value
// without a scheme (e.g. a bare Render host injected at build time) gets https:// prepended.
const rawBase = import.meta.env.VITE_API_BASE_URL || '';
export const API_BASE = rawBase && !/^https?:\/\//.test(rawBase) ? `https://${rawBase}` : rawBase;

export const tokenStore = {
  getAccess: () => localStorage.getItem(TOKEN_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  set: (access, refresh) => {
    if (access) localStorage.setItem(TOKEN_KEY, access);
    if (refresh) localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

const apiClient = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let isRefreshing = false;
let pendingQueue = [];

const flushQueue = (error, token = null) => {
  pendingQueue.forEach(({ resolve, reject }) => (error ? reject(error) : resolve(token)));
  pendingQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    const isAuthCall = original?.url?.includes('/api/auth/');

    if (status === 401 && !original._retry && !isAuthCall && tokenStore.getRefresh()) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push({ resolve, reject });
        }).then((token) => {
          original.headers.Authorization = `Bearer ${token}`;
          return apiClient(original);
        });
      }

      original._retry = true;
      isRefreshing = true;
      try {
        // Use a bare axios call (no interceptors) against the resolved base URL so the
        // refresh works both same-origin (dev proxy) and cross-origin (prod).
        const { data } = await axios.post(`${API_BASE}/api/auth/refresh`, {
          refreshToken: tokenStore.getRefresh(),
        });
        const { accessToken, refreshToken } = data.data;
        tokenStore.set(accessToken, refreshToken);
        flushQueue(null, accessToken);
        original.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(original);
      } catch (refreshError) {
        flushQueue(refreshError, null);
        tokenStore.clear();
        window.dispatchEvent(new Event('auth:logout'));
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  },
);

/** Extracts a human-readable message from the backend error envelope. */
export const extractError = (error) =>
  error?.response?.data?.error?.message ||
  error?.response?.data?.message ||
  error?.message ||
  'Something went wrong';

export default apiClient;
