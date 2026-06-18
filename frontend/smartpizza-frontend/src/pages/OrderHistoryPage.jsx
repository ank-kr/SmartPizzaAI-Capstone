import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/api";
import { useAuth } from "../context/AuthContext";
import "../styles/order-history.css";

function OrderHistoryPage() {
  const { authUser } = useAuth();
  const navigate = useNavigate();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (authUser?.userId) {
      loadOrders();
    }
  }, [authUser?.userId]);

  const loadOrders = async () => {
    try {
      setLoading(true);
      setErrorMessage("");

      const response = await api.get(`/api/orders/user/${authUser.userId}`);
      setOrders(response.data || []);
    } catch (error) {
      setErrorMessage("Unable to load order history.");
    } finally {
      setLoading(false);
    }
  };

  const canTrackDelivery = (orderStatus) => {
    return [
      "ASSIGNED_TO_DELIVERY",
      "OUT_FOR_DELIVERY",
      "DELIVERED",
    ].includes(orderStatus);
  };

  if (loading) {
    return (
      <div className="order-history-page">
        <div className="order-history-card">
          <h2>Loading order history...</h2>
        </div>
      </div>
    );
  }

  return (
    <div className="order-history-page">
      <div className="order-history-header">
        <div>
          <p className="order-history-subtitle">Customer Orders</p>
          <h1>Order History</h1>
          <p>View your previous SmartPizzaAI orders and delivery progress.</p>
        </div>

        <button className="order-refresh-btn" onClick={loadOrders}>
          Refresh Orders
        </button>
      </div>

      {errorMessage && <div className="error-message">{errorMessage}</div>}

      {orders.length === 0 ? (
        <div className="empty-orders">
          <div>🍕</div>
          <h2>No orders found</h2>
          <p>Your completed and pending orders will appear here.</p>

          <button onClick={() => navigate("/customer")}>
            Explore Menu
          </button>
        </div>
      ) : (
        <div className="orders-grid">
          {orders.map((order) => (
            <div className="order-card" key={order.orderId}>
              <div className="order-card-top">
                <div>
                  <h2>Order #{order.orderId}</h2>
                  <p>{order.orderTime}</p>
                </div>

                <span className="order-status-badge">
                  {order.orderStatus}
                </span>
              </div>

              <div className="order-info-grid">
                <div>
                  <span>Payment</span>
                  <strong>{order.paymentStatus}</strong>
                </div>

                <div>
                  <span>Subtotal</span>
                  <strong>₹{order.subtotal}</strong>
                </div>

                <div>
                  <span>Discount</span>
                  <strong>- ₹{order.discountAmount}</strong>
                </div>

                <div>
                  <span>Tax</span>
                  <strong>₹{order.taxAmount}</strong>
                </div>

                <div>
                  <span>Delivery</span>
                  <strong>₹{order.deliveryCharge}</strong>
                </div>

                <div>
                  <span>Final Amount</span>
                  <strong>₹{order.finalAmount}</strong>
                </div>
              </div>

              <div className="order-address-box">
                <h3>Delivery Address</h3>
                <p>{order.deliveryAddress}</p>
              </div>

              <div className="order-items-box">
                <h3>Items</h3>

                {order.items?.map((item) => (
                  <div className="order-item-row" key={item.orderItemId}>
                    <div>
                      <strong>{item.itemName}</strong>
                      <p>
                        ₹{item.price} × {item.quantity}
                      </p>
                    </div>

                    <span>₹{item.subtotal}</span>
                  </div>
                ))}
              </div>

              <div className="order-actions">
                {order.paymentStatus === "PENDING" && (
                  <button
                    className="pay-order-btn"
                    onClick={() => navigate(`/payment/${order.orderId}`)}
                  >
                    Pay Now
                  </button>
                )}

                {canTrackDelivery(order.orderStatus) && (
                  <button
                    className="track-order-btn"
                    onClick={() => navigate(`/tracking/${order.orderId}`)}
                  >
                    Track Delivery
                  </button>
                )}

                {order.orderStatus === "DELIVERED" && (
                  <div className="delivered-confirmation">
                    Delivered Successfully
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default OrderHistoryPage;