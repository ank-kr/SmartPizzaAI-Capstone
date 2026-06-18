import { BrowserRouter, Route, Routes } from "react-router-dom";
import Navbar from "./components/Navbar";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./routes/ProtectedRoute";
import PublicRoute from "./routes/PublicRoute";
import CartPage from "./pages/CartPage";

import DeliveryTrackingPage from "./pages/DeliveryTrackingPage";

import PaymentPage from "./pages/PaymentPage";

import OrderHistoryPage from "./pages/OrderHistoryPage";

import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import CustomerDashboard from "./pages/CustomerDashboard";
import AdminDashboard from "./pages/AdminDashboard";
import DeliveryDashboard from "./pages/DeliveryDashboard";
import UnauthorizedPage from "./pages/UnauthorizedPage";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />

        <Routes>
          <Route path="/" element={<HomePage />} />

          <Route
            path="/login"
            element={
              <PublicRoute>
                <LoginPage />
              </PublicRoute>
            }
          />
          <Route
  path="/cart"
  element={
    <ProtectedRoute allowedRoles={["CUSTOMER"]}>
      <CartPage />
    </ProtectedRoute>
  }
/>

<Route
  path="/orders"
  element={
    <ProtectedRoute allowedRoles={["CUSTOMER"]}>
      <OrderHistoryPage />
    </ProtectedRoute>
  }
/>

<Route
  path="/payment/:orderId"
  element={
    <ProtectedRoute allowedRoles={["CUSTOMER"]}>
      <PaymentPage />
    </ProtectedRoute>
  }
/>


<Route
  path="/tracking/:orderId"
  element={
    <ProtectedRoute allowedRoles={["CUSTOMER"]}>
      <DeliveryTrackingPage />
    </ProtectedRoute>
  }
/>


          <Route
            path="/register"
            element={
              <PublicRoute>
                <RegisterPage />
              </PublicRoute>
            }
          />

          <Route
            path="/customer"
            element={
              <ProtectedRoute allowedRoles={["CUSTOMER"]}>
                <CustomerDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/admin"
            element={
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <AdminDashboard />
              </ProtectedRoute>
            }
          />

          <Route
            path="/delivery"
            element={
              <ProtectedRoute allowedRoles={["DELIVERY"]}>
                <DeliveryDashboard />
              </ProtectedRoute>
            }
          />

          <Route path="/unauthorized" element={<UnauthorizedPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;