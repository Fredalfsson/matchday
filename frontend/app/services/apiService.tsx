import axios from 'axios';

const API_BASE_URL = '';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let authToken: string | null = null;
let refreshToken: string | null = null;

export const setApiAuthToken = (token: string | null) => {
  authToken = token;
};

export const setApiRefreshToken = (token: string | null) => {
  refreshToken = token;
};

api.interceptors.request.use(
  async (config) => {
    if (authToken) {
      config.headers.Authorization = `Bearer ${authToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

let refreshPromise: Promise<string> | null = null;

// Using a plain axios instance (not `api`) avoids re-triggering the interceptor.
const refreshAccessToken = async (): Promise<string> => {
  const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
    refreshToken,
  });
  const newToken = response.data.accessToken;
  return newToken;
};

// Response interceptor for handling errors
api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    console.log(error);
    // Handle common errors here
    if (error.response?.status === 401 && !error.config._retry) {
      error.config._retry = true;

      if (refreshToken) {
        try {
          // Use shared refresh promise to prevent concurrent refreshes
          if (!refreshPromise) {
            refreshPromise = refreshAccessToken().finally(() => {
              refreshPromise = null;
            });
          }
          const newToken = await refreshPromise;
          setApiAuthToken(newToken);

          // Retry the original request with new token
          error.config.headers.Authorization = `Bearer ${newToken}`;
          return api(error.config);
        } catch (refreshError) {
          // Token refresh failed - user needs to sign in again
          console.error('Token refresh failed:', refreshError);
          refreshPromise = null;
          setApiAuthToken(null);
          setApiRefreshToken(null);
          // Trigger a logout here
          return Promise.reject(refreshError);
        }
      }
    }
    return Promise.reject(error);
  },
);

export default api;
export async function AuthUser() {
  const res = await fetch;
  if (response.ok) {
    router.push('/dashboard');
  } else {
    // Handle errors
  }
}
