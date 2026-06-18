import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function PublicRoute({ children }) {
  const { authUser, isAuthenticated } = useAuth();

  if (isAuthenticated && authUser) {
    if (authUser.role === "ADMIN") {
      return <Navigate to="/admin" replace />;
    }

    if (authUser.role === "DELIVERY") {
      return <Navigate to="/delivery" replace />;
    }

    return <Navigate to="/customer" replace />;
  }

  return children;
}

export default PublicRoute;