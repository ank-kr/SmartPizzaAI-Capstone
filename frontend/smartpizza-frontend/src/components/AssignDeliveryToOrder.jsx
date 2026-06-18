import { useState } from "react";
import api from "../api/api";
import "../styles/assign-delivery.css";

function AssignDeliveryToOrder({ onAssigned }) {
  const [orderId, setOrderId] = useState("");
  const [assignedDelivery, setAssignedDelivery] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const handleAssignDelivery = async () => {
    if (!orderId.trim()) {
      setErrorMessage("Please enter order ID.");
      setMessage("");
      setAssignedDelivery(null);
      return;
    }

    try {
      setLoading(true);
      setMessage("");
      setErrorMessage("");
      setAssignedDelivery(null);

      const response = await api.post(`/api/delivery/assign/${orderId.trim()}`);

      setAssignedDelivery(response.data);
      setMessage("Delivery partner assigned successfully.");

      if (onAssigned) {
        onAssigned();
      }
    } catch (error) {
      const backendMessage =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        "Unable to assign delivery partner. Make sure order is PAID and an AVAILABLE partner exists.";

      setErrorMessage(backendMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="assign-delivery-card">
      <div className="assign-delivery-header">
        <p className="assign-subtitle">Manual Assignment</p>
        <h2>Assign Delivery Partner To Order</h2>
        <p>
          Use this when payment is completed but delivery was not auto-assigned.
        </p>
      </div>

      <div className="assign-input-row">
        <input
          type="number"
          placeholder="Enter Order ID"
          value={orderId}
          onChange={(event) => setOrderId(event.target.value)}
        />

        <button type="button" onClick={handleAssignDelivery} disabled={loading}>
          {loading ? "Assigning..." : "Assign Delivery"}
        </button>
      </div>

      {message && <div className="assign-success">{message}</div>}
      {errorMessage && <div className="assign-error">{errorMessage}</div>}

      {assignedDelivery && (
        <div className="assign-result-card">
          <div className="assign-result-top">
            <div className="assign-icon">🚚</div>

            <div>
              <h3>Order #{assignedDelivery.orderId}</h3>
              <p>Delivery ID: #{assignedDelivery.deliveryId}</p>
            </div>

            <span>{assignedDelivery.deliveryStatus}</span>
          </div>

          <div className="assign-info-grid">
            <div>
              <label>Assigned Partner</label>
              <strong>{assignedDelivery.partnerName}</strong>
            </div>

            <div>
              <label>Vehicle</label>
              <strong>{assignedDelivery.vehicleNumber}</strong>
            </div>

            <div>
              <label>Phone</label>
              <strong>{assignedDelivery.phone}</strong>
            </div>

            <div>
              <label>Distance</label>
              <strong>{assignedDelivery.distanceKm} km</strong>
            </div>

            <div>
              <label>ETA</label>
              <strong>{assignedDelivery.estimatedTimeMinutes} mins</strong>
            </div>

            <div>
              <label>Partner ID</label>
              <strong>#{assignedDelivery.deliveryPartnerId}</strong>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AssignDeliveryToOrder;