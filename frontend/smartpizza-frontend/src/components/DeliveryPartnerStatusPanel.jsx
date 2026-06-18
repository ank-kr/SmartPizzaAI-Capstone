import "../styles/delivery-partner-status.css";

function DeliveryPartnerStatusPanel({ deliveryPartners = [], onRefresh }) {
  const getStatusCount = (status) => {
    return deliveryPartners.filter(
      (partner) => partner.partnerStatus === status
    ).length;
  };

  const getStatusClass = (status) => {
    if (status === "AVAILABLE") {
      return "partner-status available";
    }

    if (status === "BUSY") {
      return "partner-status busy";
    }

    if (status === "OFFLINE") {
      return "partner-status offline";
    }

    return "partner-status";
  };

  return (
    <div className="partner-panel">
      <div className="partner-panel-header">
        <div>
          <p className="partner-subtitle">Delivery Monitoring</p>
          <h2>Delivery Partner Availability</h2>
          <p>Check who is available, busy, or offline.</p>
        </div>

        <button type="button" className="partner-refresh-btn" onClick={onRefresh}>
          Refresh Partners
        </button>
      </div>

      <div className="partner-summary-grid">
        <div className="partner-summary-card">
          <span>🚚</span>
          <div>
            <p>Total Partners</p>
            <h3>{deliveryPartners.length}</h3>
          </div>
        </div>

        <div className="partner-summary-card available-card">
          <span>✅</span>
          <div>
            <p>Available</p>
            <h3>{getStatusCount("AVAILABLE")}</h3>
          </div>
        </div>

        <div className="partner-summary-card busy-card">
          <span>📦</span>
          <div>
            <p>Busy</p>
            <h3>{getStatusCount("BUSY")}</h3>
          </div>
        </div>

        <div className="partner-summary-card offline-card">
          <span>⛔</span>
          <div>
            <p>Offline</p>
            <h3>{getStatusCount("OFFLINE")}</h3>
          </div>
        </div>
      </div>

      {deliveryPartners.length === 0 ? (
        <div className="partner-empty">
          <h3>No delivery partners found</h3>
          <p>Create delivery partners from the form below.</p>
        </div>
      ) : (
        <div className="partner-card-grid">
          {deliveryPartners.map((partner) => (
            <div
              className="partner-card"
              key={partner.deliveryPartnerId || partner.id}
            >
              <div className="partner-card-top">
                <div className="partner-avatar">🚚</div>

                <div>
                  <h3>{partner.partnerName}</h3>
                  <p>{partner.vehicleNumber}</p>
                </div>
              </div>

              <div className={getStatusClass(partner.partnerStatus)}>
                {partner.partnerStatus}
              </div>

              <div className="partner-info-row">
                <span>Partner ID</span>
                <strong>#{partner.deliveryPartnerId || partner.id}</strong>
              </div>

              <div className="partner-info-row">
                <span>User ID</span>
                <strong>{partner.userId}</strong>
              </div>

              <div className="partner-info-row">
                <span>Phone</span>
                <strong>{partner.phone}</strong>
              </div>

              <div className="partner-info-row">
                <span>Active Deliveries</span>
                <strong>{partner.activeDeliveryCount ?? 0}</strong>
              </div>

              <div className="partner-info-row">
                <span>Rating</span>
                <strong>⭐ {partner.rating ?? 0}</strong>
              </div>

              <div className="partner-location-box">
                <p>Current Location</p>
                <strong>
                  {partner.currentLatitude}, {partner.currentLongitude}
                </strong>
              </div>

              {partner.partnerStatus === "AVAILABLE" && (
                <div className="partner-note available-note">
                  Free for new orders
                </div>
              )}

              {partner.partnerStatus === "BUSY" && (
                <div className="partner-note busy-note">
                  Currently handling delivery
                </div>
              )}

              {partner.partnerStatus === "OFFLINE" && (
                <div className="partner-note offline-note">
                  Not available for assignment
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default DeliveryPartnerStatusPanel;