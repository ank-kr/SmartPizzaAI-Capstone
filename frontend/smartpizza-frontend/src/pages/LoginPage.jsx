import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import * as Yup from "yup";
import { useAuth } from "../context/AuthContext";
import "../styles/login.css";

const loginValidationSchema = Yup.object({
  email: Yup.string()
    .email("Enter a valid email address.")
    .required("Email address is required."),
  password: Yup.string()
    .required("Password is required."),
});

function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [showPassword, setShowPassword] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const initialValues = {
    email: "",
    password: "",
  };

  const redirectByRole = (role) => {
    if (role === "ADMIN") {
      navigate("/admin");
      return;
    }

    if (role === "DELIVERY") {
      navigate("/delivery");
      return;
    }

    navigate("/customer");
  };

  const handleLoginSubmit = async (values, { setSubmitting }) => {
    setErrorMessage("");

    try {
      const user = await login(values.email.trim(), values.password);
      redirectByRole(user.role);
    } catch (error) {
      setErrorMessage("Invalid email or password. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-left-panel">
        <div className="login-brand-card">
          <div className="login-pizza-icon">🍕</div>

          <p className="login-tag">SmartPizzaAI</p>

          <h1>AI-powered pizza ordering made simple.</h1>

          <p>
            Login to explore personalized recommendations, smart combo offers,
            seamless ordering, dummy payment, and live delivery tracking.
          </p>

          <div className="login-feature-list">
            <div>
              <span>🤖</span>
              <p>AI Recommendations</p>
            </div>

            <div>
              <span>🚚</span>
              <p>Auto Delivery Assignment</p>
            </div>

            <div>
              <span>📊</span>
              <p>Admin Analytics</p>
            </div>
          </div>
        </div>
      </div>

      <div className="login-right-panel">
        <div className="login-card">
          <div className="login-card-header">
            <p className="login-small-title">Welcome Back</p>
            <h2>Login to your account</h2>
            <p>Use your registered email and password to continue.</p>
          </div>

          {errorMessage && (
            <div className="login-error-message">{errorMessage}</div>
          )}

          <Formik
            initialValues={initialValues}
            validationSchema={loginValidationSchema}
            onSubmit={handleLoginSubmit}
          >
            {({ isSubmitting, setFieldValue, values }) => {
              const fillTestUser = (email, password) => {
                setFieldValue("email", email);
                setFieldValue("password", password);
                setErrorMessage("");
              };

              return (
                <>
                  <Form className="login-form">
                    <label htmlFor="email">Email Address</label>
                    <Field
                      id="email"
                      type="email"
                      name="email"
                      placeholder="Enter email address"
                      className="login-input"
                    />
                    <ErrorMessage
                      name="email"
                      component="div"
                      className="field-error"
                    />

                    <label htmlFor="password">Password</label>
                    <div className="login-password-wrapper">
                      <Field
                        id="password"
                        type={showPassword ? "text" : "password"}
                        name="password"
                        placeholder="Enter password"
                        className="login-input"
                      />

                      <button
                        type="button"
                        onClick={() =>
                          setShowPassword((previous) => !previous)
                        }
                      >
                        {showPassword ? "Hide" : "Show"}
                      </button>
                    </div>
                    <ErrorMessage
                      name="password"
                      component="div"
                      className="field-error"
                    />

                    <button
                      type="submit"
                      className="login-submit-btn"
                      disabled={isSubmitting}
                    >
                      {isSubmitting ? "Logging in..." : "Login"}
                    </button>
                  </Form>

                  <div className="quick-login-box">
                    <h4>Quick Test Login</h4>

                    <div className="quick-login-buttons">
                      <button
                        type="button"
                        onClick={() =>
                          fillTestUser("ankit@gmail.com", "ankit123")
                        }
                      >
                        Customer
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          fillTestUser("admin@gmail.com", "admin123")
                        }
                      >
                        Admin
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          fillTestUser("delivery@gmail.com", "delivery123")
                        }
                      >
                        Delivery
                      </button>
                    </div>
                  </div>
                </>
              );
            }}
          </Formik>

          <p className="register-helper">
            New to SmartPizzaAI?{" "}
            <span onClick={() => navigate("/register")}>Create account</span>
          </p>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;