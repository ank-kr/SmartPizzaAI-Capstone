import { useState } from "react";
import api from "../api/api";
import "../styles/assigned-delivery-lookup.css";

function AssignedDeliveryLookup({ title = "Check Assigned Delivery Partner" }) {
  const [orderId, setOrderId] = useState("");
  const [delivery, setDelivery] = useState(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleCheckAssignment = async () => {
    if (!orderId.trim()) {
      setErrorMessage("Please enter order ID.");
      setDelivery(null);
      return;
    }

    try {
      setLoading(true);
      setErrorMessage("");
      setDelivery(null);

      const response = await api.get(`/api/delivery/order/${orderId.trim()}`);
      setDelivery(response.data);
    } catch (error) {
      setErrorMessage(
        "No delivery assignment found for this order. Payment may not be done or no partner was available."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="assignment-lookup-card">
      <div className="assignment-lookup-header">
        <div>
          <p className="assignment-subtitle">Delivery Assignment</p>
          <h2>{title}</h2>
          <p>Enter order ID to see which delivery partner is assigned.</p>
        </div>
      </div>

      <div className="assignment-input-row">
        <input
          type="number"
          placeholder="Enter Order ID"
          value={orderId}
          onChange={(event) => setOrderId(event.target.value)}
        />

        <button
          type="button"
          onClick={handleCheckAssignment}
          disabled={loading}
        >
          {loading ? "Checking..." : "Check Assigned Partner"}
        </button>
      </div>

      {errorMessage && <div className="assignment-error">{errorMessage}</div>}

      {delivery && (
        <div className="assignment-result-card">
          <div className="assignment-result-top">
            <div className="assignment-icon">🚚</div>

            <div>
              <h3>Order #{delivery.orderId}</h3>
              <p>Delivery ID: #{delivery.deliveryId}</p>
            </div>

            <span className="assignment-status">{delivery.deliveryStatus}</span>
          </div>

          <div className="assignment-info-grid">
            <div>
              <span>Assigned Partner</span>
              <strong>{delivery.partnerName}</strong>
            </div>

            <div>
              <span>Vehicle</span>
              <strong>{delivery.vehicleNumber}</strong>
            </div>

            <div>
              <span>Phone</span>
              <strong>{delivery.phone}</strong>
            </div>

            <div>
              <span>Distance</span>
              <strong>{delivery.distanceKm} km</strong>
            </div>

            <div>
              <span>ETA</span>
              <strong>{delivery.estimatedTimeMinutes} mins</strong>
            </div>

            <div>
              <span>Partner ID</span>
              <strong>#{delivery.deliveryPartnerId}</strong>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AssignedDeliveryLookup;