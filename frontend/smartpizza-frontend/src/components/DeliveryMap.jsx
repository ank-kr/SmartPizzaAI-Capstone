import {
  MapContainer,
  Marker,
  Polyline,
  Popup,
  TileLayer,
} from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import "../styles/delivery-map.css";

const DEFAULT_PICKUP_LOCATION = {
  lat: 12.9352,
  lng: 77.6245,
};

const DEFAULT_DROP_LOCATION = {
  lat: 12.8452,
  lng: 77.6602,
};

function createEmojiIcon(emoji, className) {
  return L.divIcon({
    html: `<div class="${className}">${emoji}</div>`,
    className: "custom-map-marker",
    iconSize: [38, 38],
    iconAnchor: [19, 19],
    popupAnchor: [0, -18],
  });
}

const pickupIcon = createEmojiIcon("🍕", "pickup-marker");
const deliveryIcon = createEmojiIcon("🚚", "delivery-marker");
const dropIcon = createEmojiIcon("🏠", "drop-marker");

function getSafeCoordinate(value, fallbackValue) {
  const numberValue = Number(value);

  if (Number.isFinite(numberValue)) {
    return numberValue;
  }

  return fallbackValue;
}

function getDeliveryMarkerPosition(delivery, pickupLocation, dropLocation) {
  const status = delivery?.deliveryStatus;

  if (status === "DELIVERED") {
    return dropLocation;
  }

  if (status === "OUT_FOR_DELIVERY") {
    return {
      lat: pickupLocation.lat + (dropLocation.lat - pickupLocation.lat) * 0.72,
      lng: pickupLocation.lng + (dropLocation.lng - pickupLocation.lng) * 0.72,
    };
  }

  if (status === "PICKED_UP") {
    return {
      lat: pickupLocation.lat + (dropLocation.lat - pickupLocation.lat) * 0.35,
      lng: pickupLocation.lng + (dropLocation.lng - pickupLocation.lng) * 0.35,
    };
  }

  return pickupLocation;
}

function DeliveryMap({ delivery, compact = false }) {
  const pickupLocation = {
    lat: getSafeCoordinate(
      delivery?.pickupLatitude,
      DEFAULT_PICKUP_LOCATION.lat
    ),
    lng: getSafeCoordinate(
      delivery?.pickupLongitude,
      DEFAULT_PICKUP_LOCATION.lng
    ),
  };

  const dropLocation = {
    lat: getSafeCoordinate(delivery?.dropLatitude, DEFAULT_DROP_LOCATION.lat),
    lng: getSafeCoordinate(delivery?.dropLongitude, DEFAULT_DROP_LOCATION.lng),
  };

  const deliveryLocation = getDeliveryMarkerPosition(
    delivery,
    pickupLocation,
    dropLocation
  );

  const mapCenter = {
    lat: (pickupLocation.lat + dropLocation.lat) / 2,
    lng: (pickupLocation.lng + dropLocation.lng) / 2,
  };

  const routePositions = [
    [pickupLocation.lat, pickupLocation.lng],
    [deliveryLocation.lat, deliveryLocation.lng],
    [dropLocation.lat, dropLocation.lng],
  ];

  return (
    <div
      className={
        compact ? "delivery-map-card compact-map-card" : "delivery-map-card"
      }
    >
      <div className="delivery-map-header">
        <div>
          <p className="delivery-map-subtitle">Live Map Preview</p>
          <h2>Delivery Route</h2>
          <p>
            Simulated route from restaurant to customer based on delivery status.
          </p>
        </div>

        <div className="delivery-map-status">
          {delivery?.deliveryStatus || "ASSIGNED"}
        </div>
      </div>

      <div className="delivery-map-container">
        <MapContainer
          center={[mapCenter.lat, mapCenter.lng]}
          zoom={12}
          scrollWheelZoom={false}
          className="leaflet-delivery-map"
        >
          <TileLayer
            attribution='&copy; OpenStreetMap contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          <Polyline
            positions={routePositions}
            pathOptions={{
              color: "#d62828",
              weight: 5,
              opacity: 0.85,
            }}
          />

          <Marker
            position={[pickupLocation.lat, pickupLocation.lng]}
            icon={pickupIcon}
          >
            <Popup>
              <strong>Restaurant Pickup</strong>
              <br />
              SmartPizzaAI Kitchen
            </Popup>
          </Marker>

          <Marker
            position={[deliveryLocation.lat, deliveryLocation.lng]}
            icon={deliveryIcon}
          >
            <Popup>
              <strong>Delivery Partner</strong>
              <br />
              {delivery?.partnerName || "Assigned Partner"}
              <br />
              Status: {delivery?.deliveryStatus || "ASSIGNED"}
            </Popup>
          </Marker>

          <Marker position={[dropLocation.lat, dropLocation.lng]} icon={dropIcon}>
            <Popup>
              <strong>Customer Location</strong>
              <br />
              Delivery drop point
            </Popup>
          </Marker>
        </MapContainer>
      </div>

      <div className="delivery-map-summary">
        <div>
          <span>Distance</span>
          <strong>{delivery?.distanceKm ?? "--"} km</strong>
        </div>

        <div>
          <span>ETA</span>
          <strong>{delivery?.estimatedTimeMinutes ?? "--"} mins</strong>
        </div>

        <div>
          <span>Partner</span>
          <strong>{delivery?.partnerName || "Not Assigned"}</strong>
        </div>
      </div>
    </div>
  );
}

export default DeliveryMap;