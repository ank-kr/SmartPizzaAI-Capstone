import { useEffect, useState } from "react";
import api from "../api/api";
import { useAuth } from "../context/AuthContext";
import "../styles/admin-dashboard.css";

import DeliveryPartnerStatusPanel from "../components/DeliveryPartnerStatusPanel";

import AssignDeliveryToOrder from "../components/AssignDeliveryToOrder";

import AssignedDeliveryLookup from "../components/AssignedDeliveryLookup";

function AdminDashboard() {
  const { authUser } = useAuth();

  const [activeTab, setActiveTab] = useState("analytics");

  const [deliveryPartners, setDeliveryPartners] = useState([]);

  const [analyticsSummary, setAnalyticsSummary] = useState(null);
  const [topItems, setTopItems] = useState([]);
  const [deliveryPerformance, setDeliveryPerformance] = useState(null);
  const [categories, setCategories] = useState([]);

  const [categoryForm, setCategoryForm] = useState({
    name: "",
    description: "",
  });

  const [menuItemForm, setMenuItemForm] = useState({
    name: "",
    description: "",
    price: "",
    imageUrl: "",
    size: "MEDIUM",
    crustType: "REGULAR",
    spiceLevel: "MEDIUM",
    veg: "true",
    available: "true",
    rating: "4.5",
    categoryId: "",
  });

  const [partnerForm, setPartnerForm] = useState({
    userId: "",
    partnerName: "",
    phone: "",
    vehicleNumber: "",
    partnerStatus: "AVAILABLE",
    currentLatitude: "12.9345",
    currentLongitude: "77.6238",
    activeDeliveryCount: "0",
    rating: "4.8",
  });

  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadAdminData();
  }, []);

  const loadAdminData = async () => {
    try {
      setLoading(true);
      setErrorMessage("");

      const summaryResponse = await api.get("/analytics/summary");
      setAnalyticsSummary(summaryResponse.data);

      const partnersResponse = await api.get("/api/delivery/partners");
      setDeliveryPartners(partnersResponse.data || []);

      const topItemsResponse = await api.get("/analytics/top-items");
      setTopItems(topItemsResponse.data || []);

      const deliveryResponse = await api.get("/analytics/delivery-performance");
      setDeliveryPerformance(deliveryResponse.data);

      const categoriesResponse = await api.get("/api/categories");
      setCategories(categoriesResponse.data || []);
    } catch (error) {
      setErrorMessage("Unable to load admin dashboard data.");
    } finally {
      setLoading(false);
    }
  };

  const handleCategoryChange = (event) => {
    const { name, value } = event.target;

    setCategoryForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleMenuItemChange = (event) => {
    const { name, value } = event.target;

    setMenuItemForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handlePartnerChange = (event) => {
    const { name, value } = event.target;

    setPartnerForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const handleCreateCategory = async (event) => {
    event.preventDefault();

    try {
      setMessage("");
      setErrorMessage("");

      await api.post("/api/admin/categories", {
        name: categoryForm.name.trim(),
        description: categoryForm.description.trim(),
      });

      setCategoryForm({
        name: "",
        description: "",
      });

      setMessage("Category created successfully.");
      await loadAdminData();
    } catch (error) {
      setErrorMessage("Unable to create category.");
    }
  };

  const handleCreateMenuItem = async (event) => {
    event.preventDefault();

    try {
      setMessage("");
      setErrorMessage("");

      await api.post("/api/admin/menu-items", {
        name: menuItemForm.name.trim(),
        description: menuItemForm.description.trim(),
        price: Number(menuItemForm.price),
        imageUrl: menuItemForm.imageUrl.trim(),
        size: menuItemForm.size,
        crustType: menuItemForm.crustType,
        spiceLevel: menuItemForm.spiceLevel,
        veg: menuItemForm.veg === "true",
        available: menuItemForm.available === "true",
        rating: Number(menuItemForm.rating),
        categoryId: Number(menuItemForm.categoryId),
      });

      setMenuItemForm({
        name: "",
        description: "",
        price: "",
        imageUrl: "",
        size: "MEDIUM",
        crustType: "REGULAR",
        spiceLevel: "MEDIUM",
        veg: "true",
        available: "true",
        rating: "4.5",
        categoryId: "",
      });

      setMessage("Menu item created successfully.");
      await loadAdminData();
    } catch (error) {
      setErrorMessage("Unable to create menu item. Please check category and data.");
    }
  };

  const handleCreateDeliveryPartner = async (event) => {
    event.preventDefault();

    try {
      setMessage("");
      setErrorMessage("");

      await api.post("/api/delivery/partners", {
        userId: Number(partnerForm.userId),
        partnerName: partnerForm.partnerName.trim(),
        phone: partnerForm.phone.trim(),
        vehicleNumber: partnerForm.vehicleNumber.trim(),
        partnerStatus: partnerForm.partnerStatus,
        currentLatitude: Number(partnerForm.currentLatitude),
        currentLongitude: Number(partnerForm.currentLongitude),
        activeDeliveryCount: Number(partnerForm.activeDeliveryCount),
        rating: Number(partnerForm.rating),
      });

      setPartnerForm({
        userId: "",
        partnerName: "",
        phone: "",
        vehicleNumber: "",
        partnerStatus: "AVAILABLE",
        currentLatitude: "12.9345",
        currentLongitude: "77.6238",
        activeDeliveryCount: "0",
        rating: "4.8",
      });

      setMessage("Delivery partner created successfully.");
      await loadAdminData();
    } catch (error) {
      setErrorMessage("Unable to create delivery partner. User ID may already be linked.");
    }
  };

  if (loading) {
    return (
      <div className="admin-page">
        <div className="admin-card">
          <h2>Loading admin dashboard...</h2>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <section className="admin-hero">
        <div>
          <p className="admin-tag">Admin Control Center</p>
          <h1>Welcome, {authUser?.fullName}</h1>
          <p>
            Manage menu, delivery partners and monitor SmartPizzaAI business analytics.
          </p>
        </div>

        <button className="admin-refresh-btn" onClick={loadAdminData}>
          Refresh Dashboard
        </button>
      </section>

      {message && <div className="success-message">{message}</div>}
      {errorMessage && <div className="error-message">{errorMessage}</div>}

      <div className="admin-tab-grid">
        <button
          className={activeTab === "analytics" ? "admin-tab active" : "admin-tab"}
          onClick={() => setActiveTab("analytics")}
        >
          📊 Analytics
        </button>

        <button
          className={activeTab === "category" ? "admin-tab active" : "admin-tab"}
          onClick={() => setActiveTab("category")}
        >
          🧾 Add Category
        </button>

        <button
          className={activeTab === "menu" ? "admin-tab active" : "admin-tab"}
          onClick={() => setActiveTab("menu")}
        >
          🍕 Add Menu Item
        </button>

        <button
          className={activeTab === "delivery" ? "admin-tab active" : "admin-tab"}
          onClick={() => setActiveTab("delivery")}
        >
          🚚 Delivery Partner
        </button>
      </div>

      {activeTab === "analytics" && (
        <section className="admin-section">
          <div className="analytics-grid">
            <div className="analytics-card">
              <p>Total Orders</p>
              <h2>{analyticsSummary?.totalOrders ?? 0}</h2>
            </div>

            <div className="analytics-card">
              <p>Paid Orders</p>
              <h2>{analyticsSummary?.paidOrders ?? 0}</h2>
            </div>

            <div className="analytics-card">
              <p>Delivered Orders</p>
              <h2>{analyticsSummary?.deliveredOrders ?? 0}</h2>
            </div>

            <div className="analytics-card">
              <p>Total Revenue</p>
              <h2>₹{analyticsSummary?.totalRevenue ?? 0}</h2>
            </div>

            <div className="analytics-card">
              <p>Average Order Value</p>
              <h2>₹{analyticsSummary?.averageOrderValue ?? 0}</h2>
            </div>

            <div className="analytics-card">
              <p>Pending Orders</p>
              <h2>{analyticsSummary?.pendingOrders ?? 0}</h2>
            </div>
          </div>

          <div className="admin-two-column">
            <div className="admin-card">
              <h2>Top Selling Items</h2>

              {topItems.length === 0 ? (
                <p className="admin-empty">No top items available.</p>
              ) : (
                <div className="admin-list">
                  {topItems.map((item) => (
                    <div className="admin-list-row" key={item.menuItemId}>
                      <div>
                        <h3>{item.itemName}</h3>
                        <p>Quantity Sold: {item.totalQuantitySold}</p>
                      </div>
                      <strong>₹{item.totalRevenue}</strong>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="admin-card">
              <h2>Delivery Performance</h2>

              <div className="delivery-performance-grid">
                <div>
                  <span>Total Partners</span>
                  <strong>{deliveryPerformance?.totalPartners ?? 0}</strong>
                </div>

                <div>
                  <span>Available</span>
                  <strong>{deliveryPerformance?.availablePartners ?? 0}</strong>
                </div>

                <div>
                  <span>Busy</span>
                  <strong>{deliveryPerformance?.busyPartners ?? 0}</strong>
                </div>

                <div>
                  <span>Offline</span>
                  <strong>{deliveryPerformance?.offlinePartners ?? 0}</strong>
                </div>

                <div>
                  <span>Active Deliveries</span>
                  <strong>{deliveryPerformance?.totalActiveDeliveries ?? 0}</strong>
                </div>

                <div>
                  <span>Average Rating</span>
                  <strong>{deliveryPerformance?.averageRating ?? 0}</strong>
                </div>
              </div>
            </div>
          </div>
        </section>
      )}

      {activeTab === "category" && (
        <section className="admin-section">
          <div className="admin-card">
            <h2>Add Category</h2>

            <form className="admin-form" onSubmit={handleCreateCategory}>
              <div className="admin-form-row">
                <div className="admin-form-group">
                  <label>Category Name</label>
                  <input
                    type="text"
                    name="name"
                    value={categoryForm.name}
                    onChange={handleCategoryChange}
                    placeholder="Veg Pizza"
                    required
                  />
                </div>

                <div className="admin-form-group">
                  <label>Description</label>
                  <input
                    type="text"
                    name="description"
                    value={categoryForm.description}
                    onChange={handleCategoryChange}
                    placeholder="Fresh vegetarian pizzas"
                    required
                  />
                </div>
              </div>

              <button className="admin-submit-btn" type="submit">
                Create Category
              </button>
            </form>
          </div>
        </section>
      )}

      {activeTab === "menu" && (
        <section className="admin-section">
          <div className="admin-card">
            <h2>Add Menu Item</h2>

            <form className="admin-form" onSubmit={handleCreateMenuItem}>
              <div className="admin-form-row">
                <div className="admin-form-group">
                  <label>Name</label>
                  <input
                    type="text"
                    name="name"
                    value={menuItemForm.name}
                    onChange={handleMenuItemChange}
                    placeholder="Farmhouse Pizza"
                    required
                  />
                </div>

                <div className="admin-form-group">
                  <label>Price</label>
                  <input
                    type="number"
                    name="price"
                    value={menuItemForm.price}
                    onChange={handleMenuItemChange}
                    placeholder="399"
                    required
                  />
                </div>
              </div>

              <div className="admin-form-group">
                <label>Description</label>
                <input
                  type="text"
                  name="description"
                  value={menuItemForm.description}
                  onChange={handleMenuItemChange}
                  placeholder="Loaded with onion, capsicum, tomato and mushrooms"
                  required
                />
              </div>

              <div className="admin-form-row">
                <div className="admin-form-group">
                  <label>Image URL</label>
                  <input
                    type="text"
                    name="imageUrl"
                    value={menuItemForm.imageUrl}
                    onChange={handleMenuItemChange}
                    placeholder="/images/farmhouse.jpg"
                    required
                  />
                </div>

                <div className="admin-form-group">
                  <label>Category</label>
                  <select
                    name="categoryId"
                    value={menuItemForm.categoryId}
                    onChange={handleMenuItemChange}
                    required
                  >
                    <option value="">Select Category</option>
                    {categories.map((category) => (
                      <option
                        key={category.id || category.categoryId}
                        value={category.id || category.categoryId}
                      >
                        {category.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="admin-form-row three-column">
                <div className="admin-form-group">
                  <label>Size</label>
                  <select
                    name="size"
                    value={menuItemForm.size}
                    onChange={handleMenuItemChange}
                  >
                    <option value="SMALL">SMALL</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="LARGE">LARGE</option>
                  </select>
                </div>

                <div className="admin-form-group">
                  <label>Crust Type</label>
                  <select
                    name="crustType"
                    value={menuItemForm.crustType}
                    onChange={handleMenuItemChange}
                  >
                    <option value="REGULAR">REGULAR</option>
                    <option value="THIN_CRUST">THIN CRUST</option>
                    <option value="CHEESE_BURST">CHEESE BURST</option>
                    <option value="PAN_BASE">PAN BASE</option>
                  </select>
                </div>

                <div className="admin-form-group">
                  <label>Spice Level</label>
                  <select
                    name="spiceLevel"
                    value={menuItemForm.spiceLevel}
                    onChange={handleMenuItemChange}
                  >
                    <option value="LOW">LOW</option>
                    <option value="MEDIUM">MEDIUM</option>
                    <option value="HIGH">HIGH</option>
                  </select>
                </div>
              </div>

              <div className="admin-form-row three-column">
                <div className="admin-form-group">
                  <label>Veg</label>
                  <select
                    name="veg"
                    value={menuItemForm.veg}
                    onChange={handleMenuItemChange}
                  >
                    <option value="true">Yes</option>
                    <option value="false">No</option>
                  </select>
                </div>

                <div className="admin-form-group">
                  <label>Available</label>
                  <select
                    name="available"
                    value={menuItemForm.available}
                    onChange={handleMenuItemChange}
                  >
                    <option value="true">Yes</option>
                    <option value="false">No</option>
                  </select>
                </div>

                <div className="admin-form-group">
                  <label>Rating</label>
                  <input
                    type="number"
                    step="0.1"
                    name="rating"
                    value={menuItemForm.rating}
                    onChange={handleMenuItemChange}
                    required
                  />
                </div>
              </div>

              <button className="admin-submit-btn" type="submit">
                Create Menu Item
              </button>
            </form>
          </div>
        </section>
      )}

{activeTab === "delivery" && (
  <section className="admin-section">
    <AssignDeliveryToOrder onAssigned={loadAdminData} />

    <AssignedDeliveryLookup title="Check Assigned Partner By Order ID" />

    <DeliveryPartnerStatusPanel
      deliveryPartners={deliveryPartners}
      onRefresh={loadAdminData}
    />

    <div className="admin-card">
      <h2>Add Delivery Partner</h2>

            <form className="admin-form" onSubmit={handleCreateDeliveryPartner}>
              <div className="admin-form-row">
                <div className="admin-form-group">
                  <label>Delivery User ID</label>
                  <input
                    type="number"
                    name="userId"
                    value={partnerForm.userId}
                    onChange={handlePartnerChange}
                    placeholder="3"
                    required
                  />
                </div>

                <div className="admin-form-group">
                  <label>Partner Name</label>
                  <input
                    type="text"
                    name="partnerName"
                    value={partnerForm.partnerName}
                    onChange={handlePartnerChange}
                    placeholder="Rahul Delivery"
                    required
                  />
                </div>
              </div>

              <div className="admin-form-row">
                <div className="admin-form-group">
                  <label>Phone</label>
                  <input
                    type="text"
                    name="phone"
                    value={partnerForm.phone}
                    onChange={handlePartnerChange}
                    placeholder="7777777777"
                    required
                  />
                </div>

                <div className="admin-form-group">
                  <label>Vehicle Number</label>
                  <input
                    type="text"
                    name="vehicleNumber"
                    value={partnerForm.vehicleNumber}
                    onChange={handlePartnerChange}
                    placeholder="KA-01-AB-1234"
                    required
                  />
                </div>
              </div>

              <div className="admin-form-row three-column">
                <div className="admin-form-group">
                  <label>Status</label>
                  <select
                    name="partnerStatus"
                    value={partnerForm.partnerStatus}
                    onChange={handlePartnerChange}
                  >
                    <option value="AVAILABLE">AVAILABLE</option>
                    <option value="BUSY">BUSY</option>
                    <option value="OFFLINE">OFFLINE</option>
                  </select>
                </div>

                <div className="admin-form-group">
                  <label>Active Deliveries</label>
                  <input
                    type="number"
                    name="activeDeliveryCount"
                    value={partnerForm.activeDeliveryCount}
                    onChange={handlePartnerChange}
                  />
                </div>

                <div className="admin-form-group">
                  <label>Rating</label>
                  <input
                    type="number"
                    step="0.1"
                    name="rating"
                    value={partnerForm.rating}
                    onChange={handlePartnerChange}
                  />
                </div>
              </div>

              <div className="admin-form-row">
                <div className="admin-form-group">
                  <label>Current Latitude</label>
                  <input
                    type="number"
                    step="0.0001"
                    name="currentLatitude"
                    value={partnerForm.currentLatitude}
                    onChange={handlePartnerChange}
                  />
                </div>

                <div className="admin-form-group">
                  <label>Current Longitude</label>
                  <input
                    type="number"
                    step="0.0001"
                    name="currentLongitude"
                    value={partnerForm.currentLongitude}
                    onChange={handlePartnerChange}
                  />
                </div>
              </div>

              <button className="admin-submit-btn" type="submit">
                Create Delivery Partner
              </button>
            </form>
          </div>
        </section>
      )}
    </div>
  );
}

export default AdminDashboard;
