import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/api";
import { useAuth } from "../context/AuthContext";
import MenuItemCard from "../components/MenuItemCard";
import "../styles/customer-dashboard.css";


function CustomerDashboard() {
  const { authUser } = useAuth();
  const navigate = useNavigate();

  const [menuItems, setMenuItems] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [trendingItems, setTrendingItems] = useState([]);

  const [searchText, setSearchText] = useState("");
  const [vegFilter, setVegFilter] = useState("ALL");

  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const [addingSmartItemKey, setAddingSmartItemKey] = useState("");
  const [addedSmartItemKey, setAddedSmartItemKey] = useState("");

  useEffect(() => {
    if (authUser?.userId) {
      loadDashboardData();
    }
  }, [authUser?.userId]);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setErrorMessage("");

      const menuResponse = await api.get("/api/menu-items");
      setMenuItems(menuResponse.data || []);

      const recommendationResponse = await api.get(
        `/ai/recommendations/${authUser.userId}`,
      );
      setRecommendations(recommendationResponse.data || []);

      const trendingResponse = await api.get("/ai/trending");
      setTrendingItems(trendingResponse.data || []);
    } catch (error) {
      setErrorMessage("Unable to load customer dashboard data.");
    } finally {
      setLoading(false);
    }
  };

const handleAddToCart = async (menuItemId) => {
  try {
    setMessage("");
    setErrorMessage("");

    await api.post("/api/cart/add", {
      userId: authUser.userId,
      menuItemId,
      quantity: 1,
    });

    window.dispatchEvent(new Event("cartUpdated"));

    setMessage("Item added to cart successfully.");

    setTimeout(() => {
      setMessage("");
    }, 2200);

    return true;
  } catch (error) {
    setErrorMessage("Unable to add item to cart.");
    return false;
  }
};

  const handleSmartCardAddToCart = async (menuItemId, cardKey) => {
    if (!menuItemId || addingSmartItemKey || addedSmartItemKey === cardKey) {
      return;
    }

    setAddingSmartItemKey(cardKey);

    const success = await handleAddToCart(menuItemId);

    setAddingSmartItemKey("");

    if (success) {
      setAddedSmartItemKey(cardKey);

      setTimeout(() => {
        setAddedSmartItemKey("");
      }, 1500);
    }
  };

  const scrollToMenu = () => {
    document.getElementById("menu-section")?.scrollIntoView({
      behavior: "smooth",
    });
  };

  const scrollToAi = () => {
    document.getElementById("ai-section")?.scrollIntoView({
      behavior: "smooth",
    });
  };

const RECOMMENDATION_IMAGE_RULES = [
  {
    keywords: ["lime soda", "lime", "lemon soda", "lemon"],
    imageUrl: "/images/lime-soda.jpg",
  },
  {
    keywords: ["coke", "cola"],
    imageUrl: "/images/coke.jpg",
  },
  {
    keywords: ["falooda", "faluda"],
    imageUrl: "/images/falooda.jpg",
  },
  {
    keywords: ["garlic bread", "garlic"],
    imageUrl: "/images/garlic-bread.jpg",
  },
  {
    keywords: ["margherita"],
    imageUrl: "/images/margherita.jpg",
  },
  {
    keywords: ["non veg", "non-veg", "chicken"],
    imageUrl: "/images/non-veg-pizza.jpg",
  },
  {
    keywords: ["cheese burst"],
    imageUrl: "/images/cheese-burst.jpg",
  },
  {
    keywords: ["farmhouse"],
    imageUrl: "/images/farmhouse.jpg",
  },
];

const getRecommendationImage = (itemName = "") => {
  const normalizedName = itemName.toLowerCase();

  const matchedRule = RECOMMENDATION_IMAGE_RULES.find((rule) =>
    rule.keywords.some((keyword) => normalizedName.includes(keyword))
  );

  return matchedRule?.imageUrl || "/images/farmhouse.jpg";
};

  const getSmartButtonText = (defaultText, cardKey) => {
    if (addingSmartItemKey === cardKey) {
      return "Adding...";
    }

    if (addedSmartItemKey === cardKey) {
      return "Added ✓";
    }

    return defaultText;
  };

  const filteredMenuItems = useMemo(() => {
    const normalizedSearchText = searchText.toLowerCase().trim();

    return menuItems.filter((item) => {
      const matchesSearch =
        normalizedSearchText === "" ||
        item.name?.toLowerCase().includes(normalizedSearchText) ||
        item.description?.toLowerCase().includes(normalizedSearchText) ||
        item.categoryName?.toLowerCase().includes(normalizedSearchText);

      const matchesVeg =
        vegFilter === "ALL" ||
        (vegFilter === "VEG" && item.veg) ||
        (vegFilter === "NON_VEG" && !item.veg);

      return matchesSearch && matchesVeg;
    });
  }, [menuItems, searchText, vegFilter]);

  if (loading) {
    return (
      <div className="customer-page">
        <div className="loading-card">
          <div className="loader-pizza">🍕</div>
          <h2>Preparing your smart pizza dashboard...</h2>
          <p>Loading menu, recommendations and trending items.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="customer-page">
      <section className="customer-hero">
        <div className="hero-left">
          <p className="hero-tag">AI Powered Pizza Ordering</p>

          <h1>
            Hi {authUser?.fullName}, find your next favorite pizza with AI.
          </h1>

          <p>
            Explore fresh pizzas, smart recommendations, trending items and
            personalized combos powered by SmartPizzaAI.
          </p>

          <div className="hero-buttons">
            <button
              type="button"
              className="hero-primary-btn"
              onClick={scrollToMenu}
            >
              Explore Menu
            </button>

            <button
              type="button"
              className="hero-secondary-btn"
              onClick={scrollToAi}
            >
              AI Recommendations
            </button>

            <button
              type="button"
              className="hero-secondary-btn"
              onClick={() => navigate("/orders")}
            >
              Order History
            </button>
          </div>
        </div>

        <div className="hero-pizza-card">
          <div className="hero-pizza-image">
            <img
              src="/images/farmhouse.jpg"
              alt="Farmhouse Pizza"
              onError={(event) => {
                event.currentTarget.src = "/images/margherita.jpg";
              }}
            />
          </div>

          <h3>Today’s Smart Pick</h3>
          <p>Cheesy, hot and personalized for your taste.</p>
        </div>
      </section>

      {message && <div className="toast-success">{message}</div>}
      {errorMessage && <div className="toast-error">{errorMessage}</div>}

      <section className="quick-stats">
        <div className="quick-stat-card">
          <span>🍕</span>
          <div>
            <h3>{menuItems.length}</h3>
            <p>Menu Items</p>
          </div>
        </div>

        <div className="quick-stat-card">
          <span>🤖</span>
          <div>
            <h3>{recommendations.length}</h3>
            <p>AI Picks</p>
          </div>
        </div>

        <div className="quick-stat-card">
          <span>🔥</span>
          <div>
            <h3>{trendingItems.length}</h3>
            <p>Trending</p>
          </div>
        </div>
      </section>

      <section className="content-section" id="ai-section">
        <div className="section-heading">
          <div>
            <p className="section-subtitle">Personalized</p>
            <h2>AI Recommended For You</h2>
          </div>
        </div>

        {recommendations.length === 0 ? (
          <p className="empty-text">No recommendations available yet.</p>
        ) : (
          <div className="recommendation-grid">
            {recommendations.map((item, index) => {
              const cardKey = `recommendation-${item.itemId || item.itemName}-${index}`;

              return (
                <div
                  className="smart-card smart-ai-card"
                  key={`${item.itemId || item.itemName}-${index}`}
                >
                  <div className="smart-card-image">
                    <img
                      src={getRecommendationImage(item.itemName)}
                      alt={item.itemName}
                      onError={(event) => {
                        event.currentTarget.src = "/images/farmhouse.jpg";
                      }}
                    />

                    <span className="smart-floating-badge">🤖 AI Pick</span>
                  </div>

                  <div className="smart-card-body">
                    <div className="smart-card-title-row">
                      <h3>{item.itemName}</h3>
                      <span className="score-badge">
                        {item.score || 90}% Match
                      </span>
                    </div>

                    <p>{item.reason}</p>

                    <div className="smart-card-footer">
                      <strong>₹{item.price}</strong>
                      <span>{item.recommendationType}</span>
                    </div>

                    {item.itemId && (
                      <button
                        type="button"
                        className={
                          addedSmartItemKey === cardKey
                            ? "mini-add-btn smart-added-btn"
                            : "mini-add-btn"
                        }
                        onClick={() =>
                          handleSmartCardAddToCart(item.itemId, cardKey)
                        }
                        disabled={
                          addingSmartItemKey === cardKey ||
                          addedSmartItemKey === cardKey
                        }
                      >
                        {getSmartButtonText("Add Recommended Item", cardKey)}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      <section className="content-section">
        <div className="section-heading">
          <div>
            <p className="section-subtitle">Popular Now</p>
            <h2>Trending Items</h2>
          </div>
        </div>

        {trendingItems.length === 0 ? (
          <p className="empty-text">No trending items available yet.</p>
        ) : (
          <div className="recommendation-grid">
            {trendingItems.map((item, index) => {
              const cardKey = `trending-${item.itemId || item.itemName}-${index}`;

              return (
                <div
                  className="smart-card smart-trending-card"
                  key={`${item.itemId || item.itemName}-${index}`}
                >
                  <div className="smart-card-image">
                    <img
                      src={getRecommendationImage(item.itemName)}
                      alt={item.itemName}
                      onError={(event) => {
                        event.currentTarget.src = "/images/farmhouse.jpg";
                      }}
                    />

                    <span className="smart-floating-badge trending-floating-badge">
                      🔥 Trending
                    </span>
                  </div>

                  <div className="smart-card-body">
                    <div className="smart-card-title-row">
                      <h3>{item.itemName}</h3>
                      <span className="score-badge trending-score">
                        Popular
                      </span>
                    </div>

                    <p>{item.reason}</p>

                    <div className="smart-card-footer">
                      <strong>₹{item.price}</strong>
                      <span>{item.recommendationType}</span>
                    </div>

                    {item.itemId && (
                      <button
                        type="button"
                        className={
                          addedSmartItemKey === cardKey
                            ? "mini-add-btn smart-added-btn"
                            : "mini-add-btn"
                        }
                        onClick={() =>
                          handleSmartCardAddToCart(item.itemId, cardKey)
                        }
                        disabled={
                          addingSmartItemKey === cardKey ||
                          addedSmartItemKey === cardKey
                        }
                      >
                        {getSmartButtonText("Add Trending Item", cardKey)}
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      <section className="content-section" id="menu-section">
        <div className="section-heading menu-heading">
          <div>
            <p className="section-subtitle">Freshly Baked</p>
            <h2>Pizza Menu</h2>
          </div>

          <div className="menu-controls">
            <input
              type="text"
              placeholder="Search pizza, drink, category..."
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
            />

            <select
              value={vegFilter}
              onChange={(event) => setVegFilter(event.target.value)}
            >
              <option value="ALL">All</option>
              <option value="VEG">Veg</option>
              <option value="NON_VEG">Non-Veg</option>
            </select>
          </div>
        </div>

        {filteredMenuItems.length === 0 ? (
          <p className="empty-text">No menu items found.</p>
        ) : (
          <div className="menu-grid">
            {filteredMenuItems.map((item) => (
              <MenuItemCard
                key={item.id || item.menuItemId}
                item={item}
                onAddToCart={handleAddToCart}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

export default CustomerDashboard;
