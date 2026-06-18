import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import api from "../api/api";
import { useAuth } from "../context/AuthContext";
import "../styles/navbar.css";

function Navbar() {
  const { authUser, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const [cartCount, setCartCount] = useState(0);

  const loadCartCount = async () => {
    if (
      !isAuthenticated ||
      authUser?.role !== "CUSTOMER" ||
      !authUser?.userId
    ) {
      setCartCount(0);
      return;
    }

    try {
      const response = await api.get(`/api/cart/${authUser.userId}`);
      const cartData = response.data;

      const items = cartData.items || cartData.cartItems || [];

      const totalQuantity = items.reduce((total, item) => {
        return total + Number(item.quantity || 0);
      }, 0);

      setCartCount(totalQuantity);
    } catch (error) {
      setCartCount(0);
    }
  };

  useEffect(() => {
    loadCartCount();
  }, [isAuthenticated, authUser?.userId, authUser?.role]);

  useEffect(() => {
    const handleCartUpdated = () => {
      loadCartCount();
    };

    window.addEventListener("cartUpdated", handleCartUpdated);

    return () => {
      window.removeEventListener("cartUpdated", handleCartUpdated);
    };
  }, [isAuthenticated, authUser?.userId, authUser?.role]);

  const handleLogout = () => {
    logout();
    setCartCount(0);
    navigate("/login");
  };

  return (
    <nav className="navbar">
      <div className="navbar-brand">
        <Link to="/">SmartPizzaAI</Link>
      </div>

      <div className="navbar-links">
        {!isAuthenticated && (
          <>
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </>
        )}

        {isAuthenticated && authUser?.role === "CUSTOMER" && (
          <>
            <Link to="/customer">Menu</Link>

            <Link to="/cart" className="cart-nav-link">
              Cart
              {cartCount > 0 && (
                <span className="cart-count-badge">{cartCount}</span>
              )}
            </Link>

            <Link to="/orders">Orders</Link>
          </>
        )}

        {isAuthenticated && authUser?.role === "ADMIN" && (
          <>
            <Link to="/admin">Admin Dashboard</Link>
          </>
        )}

        {isAuthenticated && authUser?.role === "DELIVERY" && (
          <>
            <Link to="/delivery">Delivery Dashboard</Link>
          </>
        )}

        {isAuthenticated && (
          <>
            <span className="user-label">
              {authUser?.fullName} ({authUser?.role})
            </span>

            <button type="button" onClick={handleLogout} className="logout-btn">
              Logout
            </button>
          </>
        )}
      </div>
    </nav>
  );
}

export default Navbar;
