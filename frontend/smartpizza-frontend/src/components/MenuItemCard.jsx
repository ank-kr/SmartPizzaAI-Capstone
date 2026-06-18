import { useState } from "react";
import "../styles/menu-card.css";

const DEFAULT_MENU_IMAGE = "/images/farmhouse.jpg";

const MENU_IMAGE_RULES = [
  {
    keywords: ["coke", "drink", "beverage"],
    imageUrl: "/images/coke.jpg",
  },
  {
    keywords: ["falooda", "faluda"],
    imageUrl: "/images/falooda.jpg",
  },
  {
    keywords: ["garlic", "bread"],
    imageUrl: "/images/garlic-bread.jpg",
  },
  {
    keywords: ["margherita"],
    imageUrl: "/images/margherita.jpg",
  },
  {
    keywords: ["farmhouse", "cheese burst"],
    imageUrl: "/images/farmhouse.jpg",
  },
];

function getFallbackImage(itemName = "") {
  const normalizedItemName = itemName.toLowerCase();

  const matchedRule = MENU_IMAGE_RULES.find((rule) =>
    rule.keywords.some((keyword) => normalizedItemName.includes(keyword))
  );

  return matchedRule?.imageUrl || DEFAULT_MENU_IMAGE;
}

function getMenuImage(item) {
  const imageUrl = item?.imageUrl?.trim();

  if (imageUrl) {
    return imageUrl;
  }

  return getFallbackImage(item?.name);
}

function formatCrustType(crustType) {
  if (!crustType) {
    return "REGULAR";
  }

  return crustType.replaceAll("_", " ");
}

function MenuItemCard({ item, onAddToCart }) {
  const [isAdding, setIsAdding] = useState(false);
  const [isAdded, setIsAdded] = useState(false);

  const imageSrc = getMenuImage(item);
  const fallbackImage = getFallbackImage(item?.name);
  const formattedCrust = formatCrustType(item?.crustType);

  const handleImageError = (event) => {
    event.currentTarget.src = fallbackImage;
  };

  const handleAddClick = async () => {
    if (!item?.id || isAdding || isAdded) {
      return;
    }

    setIsAdding(true);

    const success = await onAddToCart(item.id);

    setIsAdding(false);

    if (success) {
      setIsAdded(true);

      setTimeout(() => {
        setIsAdded(false);
      }, 1500);
    }
  };

  const getButtonText = () => {
    if (!item?.available) {
      return "Unavailable";
    }

    if (isAdding) {
      return "Adding...";
    }

    if (isAdded) {
      return "Added ✓";
    }

    return "Add +";
  };

  return (
    <div className={isAdded ? "menu-card added-card" : "menu-card"}>
      <div className="menu-image">
        <img
          src={imageSrc}
          alt={item?.name || "Menu item"}
          onError={handleImageError}
        />

        {isAdded && <div className="added-overlay">✓ Added</div>}
      </div>

      <div className="menu-content">
        <div className="menu-title-row">
          <h3>{item?.name}</h3>

          <span className={item?.veg ? "veg-badge" : "nonveg-badge"}>
            {item?.veg ? "VEG" : "NON-VEG"}
          </span>
        </div>

        <p className="menu-description">{item?.description}</p>

        <div className="menu-meta">
          {item?.size && <span>{item.size}</span>}
          {item?.crustType && <span>{formattedCrust}</span>}
          {item?.spiceLevel && <span>{item.spiceLevel}</span>}
        </div>

        <div className="menu-footer">
          <div>
            <strong className="price-text">₹{item?.price}</strong>
            <p className="rating">⭐ {item?.rating || "4.5"}</p>
          </div>

          <button
            type="button"
            className={isAdded ? "add-cart-btn added-btn" : "add-cart-btn"}
            onClick={handleAddClick}
            disabled={!item?.available || isAdding || isAdded}
          >
            {getButtonText()}
          </button>
        </div>
      </div>
    </div>
  );
}

export default MenuItemCard;