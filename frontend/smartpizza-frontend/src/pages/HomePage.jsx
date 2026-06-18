import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

function HomePage() {
  const { authUser, isAuthenticated } = useAuth();

  const getDashboardPath = () => {
    if (!authUser) {
      return "/login";
    }

    if (authUser.role === "ADMIN") {
      return "/admin";
    }

    if (authUser.role === "DELIVERY") {
      return "/delivery";
    }

    return "/customer";
  };

  return (
    <div className="page home-page">
      <div className="hero-card">
        <h1>SmartPizzaAI</h1>
        <p>
          AI-enabled pizza ordering, smart recommendations, payment, delivery
          assignment, and admin analytics.
        </p>

        {isAuthenticated ? (
          <Link className="primary-btn" to={getDashboardPath()}>
            Go to Dashboard
          </Link>
        ) : (
          <div className="hero-actions">
            <Link className="primary-btn" to="/login">
              Login
            </Link>
            <Link className="secondary-btn" to="/register">
              Register
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}

export default HomePage;