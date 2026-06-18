import { useEffect, useState } from "react";
import api from "../api/api";
import { useAuth } from "../context/AuthContext";
import "../styles/delivery-dashboard.css";

function DeliveryDashboard() {
  const { authUser } = useAuth();

  const [manualOrderId, setManualOrderId] = useState("");
  const [activeDeliveries, setActiveDeliveries] = useState([]);
  const [selectedDelivery, setSelectedDelivery] = useState(null);
  const [selectedOrder, setSelectedOrder] = useState(null);

  const [loadingActive, setLoadingActive] = useState(false);
  const [loadingManual, setLoadingManual] = useState(false);
  const [updatingStatus, setUpdatingStatus] = useState(false);

  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (authUser?.userId) {
      loadMyActiveDeliveries();
    }
  }, [authUser?.userId]);

  const clearMessages = () => {
    setMessage("");
    setErrorMessage("");
  };

  const loadOrderDetails = async (orderId) => {
    try {
      const response = await api.get(`/api/orders/${orderId}`);
      setSelectedOrder(response.data);
    } catch (error) {
      setSelectedOrder(null);
    }
  };

  const loadMyActiveDeliveries = async () => {
    try {
      setLoadingActive(true);
      clearMessages();

      const response = await api.get(
        `/api/delivery/partner/user/${authUser.userId}/active`
      );

      const deliveries = response.data || [];
      setActiveDeliveries(deliveries);

      if (deliveries.length > 0) {
        setSelectedDelivery(deliveries[0]);
        setManualOrderId(String(deliveries[0].orderId));
        await loadOrderDetails(deliveries[0].orderId);
        setMessage("Assigned deliveries loaded successfully.");
      } else {
        setSelectedDelivery(null);
        setSelectedOrder(null);
        setMessage("No active delivery assigned right now.");
      }
    } catch (error) {
      setActiveDeliveries([]);
      setSelectedDelivery(null);
      setSelectedOrder(null);

      const backendMessage =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.response?.data ||
        "No delivery partner profile found or no active delivery assigned.";

      setErrorMessage(String(backendMessage));
    } finally {
      setLoadingActive(false);
    }
  };

  const loadDeliveryByOrderId = async () => {
    if (!manualOrderId.trim()) {
      setErrorMessage("Please enter order ID.");
      return;
    }

    try {
      setLoadingManual(true);
      clearMessages();
      setSelectedDelivery(null);
      setSelectedOrder(null);

      const response = await api.get(`/api/delivery/order/${manualOrderId}`);
      setSelectedDelivery(response.data);

      await loadOrderDetails(response.data.orderId);

      setMessage("Delivery details loaded successfully.");
    } catch (error) {
      setSelectedDelivery(null);
      setSelectedOrder(null);
      setErrorMessage(
        "No delivery found for this order ID or delivery is not assigned yet."
      );
    } finally {
      setLoadingManual(false);
    }
  };

  const updateDeliveryStatus = async (deliveryStatus) => {
    if (!selectedDelivery?.deliveryId) {
      setErrorMessage("Please select a delivery first.");
      return;
    }

    try {
      setUpdatingStatus(true);
      clearMessages();

      const response = await api.put(
        `/api/delivery/status/${selectedDelivery.deliveryId}`,
        {
          deliveryStatus,
        }
      );

      const updatedDelivery = response.data;
      setSelectedDelivery(updatedDelivery);

      if (updatedDelivery?.orderId) {
        await loadOrderDetails(updatedDelivery.orderId);
      }

      setMessage(`Delivery status updated to ${deliveryStatus}.`);

      if (deliveryStatus === "DELIVERED" || deliveryStatus === "CANCELLED") {
        await loadMyActiveDeliveries();
      }
    } catch (error) {
      const backendMessage =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        error?.response?.data ||
        "Unable to update delivery status.";

      setErrorMessage(String(backendMessage));
    } finally {
      setUpdatingStatus(false);
    }
  };

  const getStepClass = (step) => {
    const statusOrder = [
      "ASSIGNED",
      "PICKED_UP",
      "OUT_FOR_DELIVERY",
      "DELIVERED",
    ];

    const currentIndex = statusOrder.indexOf(selectedDelivery?.deliveryStatus);
    const stepIndex = statusOrder.indexOf(step);

    return currentIndex >= stepIndex ? "delivery-step active" : "delivery-step";
  };

  const handleSelectActiveDelivery = async (delivery) => {
    setSelectedDelivery(delivery);
    setManualOrderId(String(delivery.orderId));
    await loadOrderDetails(delivery.orderId);
    clearMessages();
  };

  const canPickUp = selectedDelivery?.deliveryStatus === "ASSIGNED";
  const canOutForDelivery = selectedDelivery?.deliveryStatus === "PICKED_UP";
  const canDeliver = selectedDelivery?.deliveryStatus === "OUT_FOR_DELIVERY";
  const isDelivered = selectedDelivery?.deliveryStatus === "DELIVERED";

  return (
    <div className="delivery-page">
      <section className="delivery-hero">
        <div>
          <p className="delivery-tag">Delivery Partner Dashboard</p>
          <h1>Welcome, {authUser?.fullName}</h1>
          <p>
            Your active assigned deliveries are loaded automatically after login.
          </p>

          <div className="delivery-user-info">
            <span>User ID: {authUser?.userId}</span>
            <span>Email: {authUser?.email}</span>
            <span>Role: {authUser?.role}</span>
          </div>
        </div>

        <div className="delivery-hero-icon">🚚</div>
      </section>

      {message && <div className="success-message">{message}</div>}
      {errorMessage && <div className="error-message">{errorMessage}</div>}

      <section className="delivery-search-card">
        <div>
          <h2>My Active Deliveries</h2>
          <p>
            Assigned deliveries linked to your delivery partner profile will
            appear here automatically.
          </p>
        </div>

        <button onClick={loadMyActiveDeliveries} disabled={loadingActive}>
          {loadingActive ? "Refreshing..." : "Refresh My Deliveries"}
        </button>
      </section>

      {activeDeliveries.length === 0 && !selectedDelivery && (
        <div className="delivery-card no-active-delivery">
          <h2>No Active Delivery Assigned</h2>
          <p>
            No order is currently assigned to this delivery partner. Once an
            order is assigned by admin or auto-assigned after customer payment,
            it will appear here.
          </p>
        </div>
      )}

      {activeDeliveries.length > 0 && (
        <section className="delivery-card active-deliveries-card">
          <h2>Assigned Deliveries</h2>

          <div className="active-delivery-list">
            {activeDeliveries.map((delivery) => (
              <button
                key={delivery.deliveryId}
                className={
                  selectedDelivery?.deliveryId === delivery.deliveryId
                    ? "active-delivery-item selected"
                    : "active-delivery-item"
                }
                onClick={() => handleSelectActiveDelivery(delivery)}
              >
                <span>Order #{delivery.orderId}</span>
                <strong>{delivery.deliveryStatus}</strong>
              </button>
            ))}
          </div>
        </section>
      )}

      <section className="delivery-search-card">
        <div>
          <h2>Find Assigned Delivery Manually</h2>
          <p>Optional: enter order ID to fetch a delivery assignment.</p>
        </div>

        <div className="delivery-search-row">
          <input
            type="number"
            placeholder="Enter Order ID"
            value={manualOrderId}
            onChange={(event) => setManualOrderId(event.target.value)}
          />

          <button onClick={loadDeliveryByOrderId} disabled={loadingManual}>
            {loadingManual ? "Loading..." : "Fetch Delivery"}
          </button>
        </div>
      </section>

      {selectedDelivery && (
        <section className="delivery-layout">
          <div className="delivery-card">
            <h2>Delivery Progress</h2>

            <div className="delivery-status-badge">
              Current Status: {selectedDelivery.deliveryStatus}
            </div>

            <div className="delivery-steps">
              <div className={getStepClass("ASSIGNED")}>
                <span>1</span>
                <div>
                  <h3>Assigned</h3>
                  <p>Delivery assigned to partner.</p>
                </div>
              </div>

              <div className={getStepClass("PICKED_UP")}>
                <span>2</span>
                <div>
                  <h3>Picked Up</h3>
                  <p>Order picked up from restaurant.</p>
                </div>
              </div>

              <div className={getStepClass("OUT_FOR_DELIVERY")}>
                <span>3</span>
                <div>
                  <h3>Out For Delivery</h3>
                  <p>Order is on the way.</p>
                </div>
              </div>

              <div className={getStepClass("DELIVERED")}>
                <span>4</span>
                <div>
                  <h3>Delivered</h3>
                  <p>Order delivered successfully.</p>
                </div>
              </div>
            </div>

            <div className="delivery-action-grid">
              <button
                onClick={() => updateDeliveryStatus("PICKED_UP")}
                disabled={!canPickUp || updatingStatus}
              >
                Mark Picked Up
              </button>

              <button
                onClick={() => updateDeliveryStatus("OUT_FOR_DELIVERY")}
                disabled={!canOutForDelivery || updatingStatus}
              >
                Mark Out For Delivery
              </button>

              <button
                onClick={() => updateDeliveryStatus("DELIVERED")}
                disabled={!canDeliver || updatingStatus}
              >
                Mark Delivered
              </button>
            </div>

            {isDelivered && (
              <div className="delivered-note">
                Delivery completed. Partner is available for new orders.
              </div>
            )}
          </div>

          <div className="delivery-card">
            <h2>Delivery Details</h2>

            <div className="delivery-info-row">
              <span>Delivery ID</span>
              <strong>#{selectedDelivery.deliveryId}</strong>
            </div>

            <div className="delivery-info-row">
              <span>Order ID</span>
              <strong>#{selectedDelivery.orderId}</strong>
            </div>

            <div className="delivery-info-row">
              <span>Partner Name</span>
              <strong>{selectedDelivery.partnerName}</strong>
            </div>

            <div className="delivery-info-row">
              <span>Phone</span>
              <strong>{selectedDelivery.phone}</strong>
            </div>

            <div className="delivery-info-row">
              <span>Vehicle</span>
              <strong>{selectedDelivery.vehicleNumber}</strong>
            </div>

            <div className="delivery-info-row">
              <span>Distance</span>
              <strong>{selectedDelivery.distanceKm} km</strong>
            </div>

            <div className="delivery-info-row">
              <span>ETA</span>
              <strong>{selectedDelivery.estimatedTimeMinutes} mins</strong>
            </div>
          </div>

          {selectedOrder && (
            <div className="delivery-card">
              <h2>Order Summary</h2>

              <div className="delivery-info-row">
                <span>Order Status</span>
                <strong>{selectedOrder.orderStatus}</strong>
              </div>

              <div className="delivery-info-row">
                <span>Payment Status</span>
                <strong>{selectedOrder.paymentStatus}</strong>
              </div>

              <div className="delivery-info-row">
                <span>Final Amount</span>
                <strong>₹{selectedOrder.finalAmount}</strong>
              </div>

              <div className="delivery-address-box">
                <h3>Delivery Address</h3>
                <p>{selectedOrder.deliveryAddress}</p>
              </div>
            </div>
          )}
        </section>
      )}
    </div>
  );
}

export default DeliveryDashboard;