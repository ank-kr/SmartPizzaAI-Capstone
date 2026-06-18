import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
});

const clearSessionAndRedirect = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("authUser");
  localStorage.removeItem("user");

  window.location.replace("/login");
};

const isTokenExpired = (token) => {
  if (!token) {
    return true;
  }

  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    const expiryTime = payload.exp * 1000;

    return Date.now() >= expiryTime;
  } catch (error) {
    return true;
  }
};

// Add JWT token automatically for protected APIs
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      if (isTokenExpired(token)) {
        clearSessionAndRedirect();
        return Promise.reject(new Error("JWT token expired"));
      }

      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Handle expired/invalid JWT token globally
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      clearSessionAndRedirect();
    }

    return Promise.reject(error);
  }
);

export default api;