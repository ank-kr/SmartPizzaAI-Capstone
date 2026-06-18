import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Formik, Form, Field, ErrorMessage } from "formik";
import * as Yup from "yup";
import { useAuth } from "../context/AuthContext";
import "../styles/register.css";

const registerValidationSchema = Yup.object({
  fullName: Yup.string()
    .trim()
    .required("Full name is required."),

  email: Yup.string()
    .trim()
    .email("Enter a valid email like ankit@gmail.com.")
    .required("Email is required."),

  password: Yup.string()
    .required("Password is required.")
    .min(6, "Password must be at least 6 characters."),

  confirmPassword: Yup.string()
    .required("Confirm password is required.")
    .oneOf([Yup.ref("password")], "Password and confirm password do not match."),

  phone: Yup.string()
    .trim()
    .matches(/^[0-9]{10}$/, {
      message: "Phone number must be 10 digits.",
      excludeEmptyString: true,
    }),

  address: Yup.string()
    .trim()
    .required("Address is required."),

  role: Yup.string()
    .oneOf(["CUSTOMER", "ADMIN", "DELIVERY"], "Invalid role selected.")
    .required("Role is required."),
});

function RegisterPage() {
  const { register, checkEmailExists } = useAuth();
  const navigate = useNavigate();

  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [emailStatus, setEmailStatus] = useState("");
  const [isCheckingEmail, setIsCheckingEmail] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const initialValues = {
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    phone: "",
    address: "",
    role: "CUSTOMER",
  };

  const getPasswordStrength = (password) => {
    let score = 0;

    if (!password) {
      return "";
    }

    if (password.length >= 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[a-z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    if (score <= 2) return "Weak";
    if (score <= 4) return "Medium";
    return "Strong";
  };

  const handleEmailBlur = async (email, setFieldTouched) => {
    const normalizedEmail = email.trim();

    setFieldTouched("email", true);
    setEmailStatus("");

    if (!normalizedEmail) {
      return;
    }

    const isValidEmail = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(
      normalizedEmail
    );

    if (!isValidEmail) {
      setEmailStatus("invalid");
      return;
    }

    try {
      setIsCheckingEmail(true);

      const exists = await checkEmailExists(normalizedEmail);
      setEmailStatus(exists ? "exists" : "available");
    } catch (error) {
      setEmailStatus("");
    } finally {
      setIsCheckingEmail(false);
    }
  };

  const handleRegisterSubmit = async (values, { setSubmitting }) => {
    setMessage("");
    setErrorMessage("");

    const normalizedEmail = values.email.trim();

    try {
      setSubmitting(true);

      if (emailStatus === "exists") {
        setErrorMessage("Email already exists. Please use another email.");
        return;
      }

      if (emailStatus !== "available") {
        const exists = await checkEmailExists(normalizedEmail);

        if (exists) {
          setEmailStatus("exists");
          setErrorMessage("Email already exists. Please use another email.");
          return;
        }

        setEmailStatus("available");
      }

      const requestBody = {
        fullName: values.fullName.trim(),
        email: normalizedEmail,
        password: values.password,
        phone: values.phone.trim(),
        address: values.address.trim(),
        role: values.role,
      };

      await register(requestBody);

      setMessage("Registration successful. Please login.");

      setTimeout(() => {
        navigate("/login");
      }, 1000);
    } catch (error) {
      setErrorMessage("Registration failed. Email may already exist.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="register-page">
      <div className="register-left-panel">
        <div className="register-brand-card">
          <div className="register-pizza-icon">🍕</div>

          <p className="register-tag">SmartPizzaAI</p>

          <h1>Create your smart pizza account.</h1>

          <p>
            Register to explore AI recommendations, smart ordering, easy
            checkout, payment, delivery tracking and role-based dashboards.
          </p>

          <div className="register-feature-list">
            <div>
              <span>🤖</span>
              <p>Personalized AI recommendations</p>
            </div>

            <div>
              <span>🛒</span>
              <p>Cart, coupon and checkout flow</p>
            </div>

            <div>
              <span>🚚</span>
              <p>Smart delivery assignment</p>
            </div>
          </div>
        </div>
      </div>

      <div className="register-right-panel">
        <div className="register-card">
          <div className="register-card-header">
            <p className="register-small-title">Get Started</p>
            <h2>Create Account</h2>
            <p>Fill your details to join SmartPizzaAI.</p>
          </div>

          {message && <div className="success-message">{message}</div>}
          {errorMessage && <div className="error-message">{errorMessage}</div>}

          <Formik
            initialValues={initialValues}
            validationSchema={registerValidationSchema}
            onSubmit={handleRegisterSubmit}
          >
            {({
              values,
              errors,
              touched,
              isSubmitting,
              setFieldValue,
              setFieldTouched,
            }) => {
              const passwordStrength = getPasswordStrength(values.password);

              return (
                <Form className="register-form">
                  <div className="form-row">
                    <div className="form-group">
                      <label htmlFor="fullName">Full Name</label>
                      <Field
                        id="fullName"
                        type="text"
                        name="fullName"
                        placeholder="Enter full name"
                        onChange={(event) => {
                          setFieldValue("fullName", event.target.value);
                          setErrorMessage("");
                          setMessage("");
                        }}
                      />
                      <ErrorMessage
                        name="fullName"
                        component="small"
                        className="error-text"
                      />
                    </div>

                    <div className="form-group">
                      <label htmlFor="email">Email</label>
                      <Field
                        id="email"
                        type="text"
                        name="email"
                        placeholder="example@gmail.com"
                        onChange={(event) => {
                          setFieldValue("email", event.target.value);
                          setEmailStatus("");
                          setErrorMessage("");
                          setMessage("");
                        }}
                        onBlur={() =>
                          handleEmailBlur(values.email, setFieldTouched)
                        }
                      />

                      <ErrorMessage
                        name="email"
                        component="small"
                        className="error-text"
                      />

                      {isCheckingEmail && (
                        <small className="hint-text">Checking email...</small>
                      )}

                      {!errors.email && emailStatus === "invalid" && (
                        <small className="error-text">
                          Enter a valid email like ankit@gmail.com
                        </small>
                      )}

                      {!errors.email && emailStatus === "available" && (
                        <small className="success-text">
                          Email is available
                        </small>
                      )}

                      {!errors.email && emailStatus === "exists" && (
                        <small className="error-text">
                          Email already exists
                        </small>
                      )}
                    </div>
                  </div>

                  <div className="form-row">
                    <div className="form-group">
                      <label htmlFor="password">Password</label>

                      <div className="password-wrapper">
                        <Field
                          id="password"
                          type={showPassword ? "text" : "password"}
                          name="password"
                          placeholder="Enter password"
                          onChange={(event) => {
                            setFieldValue("password", event.target.value);
                            setErrorMessage("");
                            setMessage("");
                          }}
                        />

                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() =>
                            setShowPassword((previous) => !previous)
                          }
                        >
                          {showPassword ? "Hide" : "Show"}
                        </button>
                      </div>

                      <ErrorMessage
                        name="password"
                        component="small"
                        className="error-text"
                      />

                      {passwordStrength && !errors.password && (
                        <small
                          className={
                            passwordStrength === "Strong"
                              ? "success-text"
                              : passwordStrength === "Medium"
                              ? "warning-text"
                              : "error-text"
                          }
                        >
                          Password strength: {passwordStrength}
                        </small>
                      )}
                    </div>

                    <div className="form-group">
                      <label htmlFor="confirmPassword">Confirm Password</label>

                      <div className="password-wrapper">
                        <Field
                          id="confirmPassword"
                          type={showConfirmPassword ? "text" : "password"}
                          name="confirmPassword"
                          placeholder="Confirm password"
                          onChange={(event) => {
                            setFieldValue(
                              "confirmPassword",
                              event.target.value
                            );
                            setErrorMessage("");
                            setMessage("");
                          }}
                        />

                        <button
                          type="button"
                          className="password-toggle"
                          onClick={() =>
                            setShowConfirmPassword((previous) => !previous)
                          }
                        >
                          {showConfirmPassword ? "Hide" : "Show"}
                        </button>
                      </div>

                      <ErrorMessage
                        name="confirmPassword"
                        component="small"
                        className="error-text"
                      />
                    </div>
                  </div>

                  <div className="form-row">
                    <div className="form-group">
                      <label htmlFor="phone">Phone</label>
                      <Field
                        id="phone"
                        type="text"
                        name="phone"
                        placeholder="10 digit phone number"
                        onChange={(event) => {
                          setFieldValue("phone", event.target.value);
                          setErrorMessage("");
                          setMessage("");
                        }}
                      />
                      <ErrorMessage
                        name="phone"
                        component="small"
                        className="error-text"
                      />
                    </div>

                    <div className="form-group">
                      <label htmlFor="role">Role</label>
                      <Field
                        id="role"
                        as="select"
                        name="role"
                        onChange={(event) => {
                          setFieldValue("role", event.target.value);
                          setErrorMessage("");
                          setMessage("");
                        }}
                      >
                        <option value="CUSTOMER">CUSTOMER</option>
                        <option value="ADMIN">ADMIN</option>
                        <option value="DELIVERY">DELIVERY</option>
                      </Field>
                      <ErrorMessage
                        name="role"
                        component="small"
                        className="error-text"
                      />
                    </div>
                  </div>

                  <div className="form-group">
                    <label htmlFor="address">Address</label>
                    <Field
                      id="address"
                      type="text"
                      name="address"
                      placeholder="Enter address"
                      onChange={(event) => {
                        setFieldValue("address", event.target.value);
                        setErrorMessage("");
                        setMessage("");
                      }}
                    />
                    <ErrorMessage
                      name="address"
                      component="small"
                      className="error-text"
                    />
                  </div>

                  <button
                    type="submit"
                    className="register-submit-btn"
                    disabled={isSubmitting || isCheckingEmail}
                  >
                    {isSubmitting ? "Creating Account..." : "Register"}
                  </button>
                </Form>
              );
            }}
          </Formik>

          <p className="login-helper">
            Already have an account?{" "}
            <span onClick={() => navigate("/login")}>Login</span>
          </p>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;