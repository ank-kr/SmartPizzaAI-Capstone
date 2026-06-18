import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../api/api";
import DeliveryMap from "../components/DeliveryMap";
import "../styles/delivery-tracking.css";

function DeliveryTrackingPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();

  const [delivery, setDelivery] = useState(null);
  const [order, setOrder] = useState(null);

  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadTrackingData();
  }, [orderId]);

  const loadTrackingData = async () => {
    try {
      setLoading(true);
      setErrorMessage("");

      const orderResponse = await api.get(`/api/orders/${orderId}`);
      setOrder(orderResponse.data);

      const deliveryResponse = await api.get(`/api/delivery/order/${orderId}`);
      setDelivery(deliveryResponse.data);
    } catch (error) {
      setDelivery(null);
      setOrder(null);
      setErrorMessage(
        "Delivery tracking is not available yet. Please refresh after a few seconds."
      );
    } finally {
      setLoading(false);
    }
  };

  const getStepClass = (step) => {
    const statusOrder = [
      "ASSIGNED",
      "PICKED_UP",
      "OUT_FOR_DELIVERY",
      "DELIVERED",
    ];

    const currentIndex = statusOrder.indexOf(delivery?.deliveryStatus);
    const stepIndex = statusOrder.indexOf(step);

    return currentIndex >= stepIndex ? "tracking-step active" : "tracking-step";
  };

  if (loading) {
    return (
      <div className="tracking-page">
        <div className="tracking-card">
          <h2>Loading delivery tracking...</h2>
        </div>
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className="tracking-page">
        <div className="tracking-card">
          <h2>Delivery Tracking</h2>
          <div className="error-message">{errorMessage}</div>

          <div className="tracking-actions">
            <button
              type="button"
              className="tracking-primary-btn"
              onClick={loadTrackingData}
            >
              Refresh Tracking
            </button>

            <button
              type="button"
              className="tracking-secondary-btn"
              onClick={() => navigate("/customer")}
            >
              Back To Menu
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="tracking-page">
      <div className="tracking-header">
        <div>
          <p className="tracking-subtitle">Live Delivery</p>
          <h1>Track Your Order</h1>
          <p>Order #{orderId} delivery status and partner details.</p>
        </div>

        <button
          type="button"
          className="tracking-secondary-btn"
          onClick={loadTrackingData}
        >
          Refresh
        </button>
      </div>

      <div className="tracking-layout">
        <div className="tracking-left-column">
          <div className="tracking-card tracking-status-card">
            <h2>Delivery Status</h2>

            <div className="tracking-status-badge">
              {delivery?.deliveryStatus}
            </div>

            <div className="tracking-steps">
              <div className={getStepClass("ASSIGNED")}>
                <span>1</span>
                <div>
                  <h3>Assigned</h3>
                  <p>Delivery partner assigned</p>
                </div>
              </div>

              <div className={getStepClass("PICKED_UP")}>
                <span>2</span>
                <div>
                  <h3>Picked Up</h3>
                  <p>Order picked up from restaurant</p>
                </div>
              </div>

              <div className={getStepClass("OUT_FOR_DELIVERY")}>
                <span>3</span>
                <div>
                  <h3>Out For Delivery</h3>
                  <p>Order is on the way</p>
                </div>
              </div>

              <div className={getStepClass("DELIVERED")}>
                <span>4</span>
                <div>
                  <h3>Delivered</h3>
                  <p>Order delivered successfully</p>
                </div>
              </div>
            </div>
          </div>

          <div className="tracking-card tracking-summary-card">
            <h2>Order Summary</h2>

            <div className="tracking-info-row">
              <span>Order ID</span>
              <strong>#{order?.orderId}</strong>
            </div>

            <div className="tracking-info-row">
              <span>Order Status</span>
              <strong>{order?.orderStatus}</strong>
            </div>

            <div className="tracking-info-row">
              <span>Payment Status</span>
              <strong>{order?.paymentStatus}</strong>
            </div>

            <div className="tracking-info-row">
              <span>Final Amount</span>
              <strong>₹{order?.finalAmount}</strong>
            </div>

            <div className="tracking-address">
              <h3>Delivery Address</h3>
              <p>{order?.deliveryAddress}</p>
            </div>
          </div>
        </div>

        <div className="tracking-right-column">
          <div className="tracking-card tracking-partner-card">
            <h2>Delivery Partner</h2>

            <div className="partner-box">
              <div className="partner-avatar">🚚</div>

              <div>
                <h3>{delivery?.partnerName}</h3>
                <p>{delivery?.vehicleNumber}</p>
              </div>
            </div>

            <div className="tracking-info-row">
              <span>Phone</span>
              <strong>{delivery?.phone}</strong>
            </div>

            <div className="tracking-info-row">
              <span>Delivery ID</span>
              <strong>#{delivery?.deliveryId}</strong>
            </div>

            <div className="tracking-info-row">
              <span>Distance</span>
              <strong>{delivery?.distanceKm} km</strong>
            </div>

            <div className="tracking-info-row">
              <span>Estimated Time</span>
              <strong>{delivery?.estimatedTimeMinutes} mins</strong>
            </div>
          </div>

          {delivery && <DeliveryMap delivery={delivery} compact />}
        </div>
      </div>
    </div>
  );
}

export default DeliveryTrackingPage;