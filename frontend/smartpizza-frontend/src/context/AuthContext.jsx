import { createContext, useContext, useEffect, useState } from "react";
import api from "../api/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [authUser, setAuthUser] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem("token");
    const userId = localStorage.getItem("userId");
    const fullName = localStorage.getItem("fullName");
    const email = localStorage.getItem("email");
    const role = localStorage.getItem("role");

    if (token && userId && role) {
      setAuthUser({
        token,
        userId: Number(userId),
        fullName,
        email,
        role,
      });
    }
  }, []);

  const login = async (email, password) => {
    const response = await api.post("/auth/login", {
      email,
      password,
    });

    const data = response.data;

    localStorage.setItem("token", data.token);
    localStorage.setItem("userId", data.userId);
    localStorage.setItem("fullName", data.fullName);
    localStorage.setItem("email", data.email);
    localStorage.setItem("role", data.role);

    const user = {
      token: data.token,
      userId: data.userId,
      fullName: data.fullName,
      email: data.email,
      role: data.role,
    };

    setAuthUser(user);

    return user;
  };

  const register = async (formData) => {
    const response = await api.post("/auth/register", formData);
    return response.data;
  };

  const checkEmailExists = async (email) => {
    const response = await api.get(
      `/auth/check-email?email=${encodeURIComponent(email)}`
    );

    return response.data.exists;
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("userId");
    localStorage.removeItem("fullName");
    localStorage.removeItem("email");
    localStorage.removeItem("role");

    setAuthUser(null);
  };

  const isAuthenticated = Boolean(authUser?.token);

  return (
    <AuthContext.Provider
      value={{
        authUser,
        login,
        register,
        checkEmailExists,
        logout,
        isAuthenticated,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}