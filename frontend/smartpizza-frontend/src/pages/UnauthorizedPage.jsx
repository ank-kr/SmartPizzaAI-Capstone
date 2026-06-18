import { Link } from "react-router-dom";

function UnauthorizedPage() {
  return (
    <div className="page">
      <div className="auth-card">
        <h2>Unauthorized Access</h2>
        <p>You do not have permission to access this page.</p>
        <Link className="primary-btn" to="/">
          Go Home
        </Link>
      </div>
    </div>
  );
}

export default UnauthorizedPage;