import { useEffect, useState } from "react";
import api from "../api/api";
import { useAuth } from "../context/AuthContext";
import "../styles/cart.css";
import { useNavigate } from "react-router-dom";

function CartPage() {
  const navigate = useNavigate();
  const { authUser } = useAuth();

  const [cart, setCart] = useState(null);
  const [cartItems, setCartItems] = useState([]);

  const [couponCode, setCouponCode] = useState("");
  const [couponResult, setCouponResult] = useState(null);

  const [deliveryAddress, setDeliveryAddress] = useState(
    "Electronic City, Bengaluru",
  );
  const [deliveryLatitude, setDeliveryLatitude] = useState(12.8452);
  const [deliveryLongitude, setDeliveryLongitude] = useState(77.6602);

  const [placedOrder, setPlacedOrder] = useState(null);

  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    if (authUser?.userId) {
      loadCart();
    }
  }, [authUser?.userId]);

  const notifyCartUpdated = () => {
    window.dispatchEvent(new Event("cartUpdated"));
  };

  const loadCart = async () => {
    try {
      setLoading(true);
      setErrorMessage("");

      const response = await api.get(`/api/cart/${authUser.userId}`);
      const cartData = response.data;

      setCart(cartData);
      setCartItems(cartData.items || cartData.cartItems || []);
    } catch (error) {
      setErrorMessage("Unable to load cart.");
    } finally {
      setLoading(false);
    }
  };

  const getCartTotal = () => {
    if (cart?.totalAmount !== undefined && cart?.totalAmount !== null) {
      return Number(cart.totalAmount);
    }

    return cartItems.reduce((total, item) => {
      return total + Number(item.subtotal || 0);
    }, 0);
  };

  const handleUpdateQuantity = async (cartItemId, quantity) => {
    if (quantity < 1) {
      return;
    }

    try {
      setMessage("");
      setErrorMessage("");

      await api.put(`/api/cart/update/${cartItemId}`, {
        quantity,
      });

      setCartItems((previousItems) =>
        previousItems.map((item) => {
          const currentCartItemId = item.cartItemId || item.id;

          if (currentCartItemId !== cartItemId) {
            return item;
          }

          const price = Number(item.price || 0);
          const updatedSubtotal = price * quantity;

          return {
            ...item,
            quantity,
            subtotal: updatedSubtotal,
          };
        }),
      );

      setCart((previousCart) => {
        if (!previousCart) {
          return previousCart;
        }

        const updatedTotal = cartItems.reduce((total, item) => {
          const currentCartItemId = item.cartItemId || item.id;

          if (currentCartItemId === cartItemId) {
            return total + Number(item.price || 0) * quantity;
          }

          return total + Number(item.subtotal || 0);
        }, 0);

        return {
          ...previousCart,
          totalAmount: updatedTotal,
        };
      });

      setCouponResult(null);

      window.dispatchEvent(new Event("cartUpdated"));
    } catch (error) {
      setErrorMessage("Unable to update cart item.");
    }
  };
  const handleRemoveItem = async (cartItemId) => {
    try {
      setMessage("");
      setErrorMessage("");

      const itemToRemove = cartItems.find(
        (item) => (item.cartItemId || item.id) === cartItemId,
      );

      await api.delete(`/api/cart/remove/${cartItemId}`);

      setCartItems((previousItems) =>
        previousItems.filter(
          (item) => (item.cartItemId || item.id) !== cartItemId,
        ),
      );

      setCart((previousCart) => {
        if (!previousCart) {
          return previousCart;
        }

        const removedAmount = Number(itemToRemove?.subtotal || 0);
        const previousTotal = Number(previousCart.totalAmount || 0);

        return {
          ...previousCart,
          totalAmount: Math.max(0, previousTotal - removedAmount),
        };
      });

      setCouponResult(null);

      window.dispatchEvent(new Event("cartUpdated"));

      setMessage("Item removed from cart.");
    } catch (error) {
      setErrorMessage("Unable to remove item.");
    }
  };

  const handleClearCart = async () => {
    try {
      setMessage("");
      setErrorMessage("");

      await api.delete(`/api/cart/clear/${authUser.userId}`);

      setCart(null);
      setCartItems([]);
      setCouponResult(null);

      notifyCartUpdated();

      setMessage("Cart cleared successfully.");
    } catch (error) {
      setErrorMessage("Unable to clear cart.");
    }
  };

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) {
      setErrorMessage("Please enter coupon code.");
      return;
    }

    try {
      setMessage("");
      setErrorMessage("");

      const response = await api.post("/api/coupons/apply", {
        couponCode: couponCode.trim(),
        cartTotal: getCartTotal(),
      });

      setCouponResult(response.data);
      setMessage("Coupon applied successfully.");
    } catch (error) {
      setCouponResult(null);
      setErrorMessage("Invalid coupon or coupon not applicable.");
    }
  };

  const getDiscountAmount = () => {
    if (!couponResult) {
      return 0;
    }

    return Number(
      couponResult.discountAmount ||
        couponResult.discount ||
        couponResult.finalDiscount ||
        0,
    );
  };

  const getAmountAfterDiscount = () => {
    if (!couponResult) {
      return getCartTotal();
    }

    return Number(
      couponResult.amountAfterDiscount ||
        couponResult.finalAmount ||
        getCartTotal() - getDiscountAmount(),
    );
  };

  const handlePlaceOrder = async () => {
    if (cartItems.length === 0) {
      setErrorMessage("Cart is empty.");
      return;
    }

    if (!deliveryAddress.trim()) {
      setErrorMessage("Delivery address is required.");
      return;
    }

    try {
      setMessage("");
      setErrorMessage("");

      const requestBody = {
        userId: authUser.userId,
        deliveryAddress,
        deliveryLatitude: Number(deliveryLatitude),
        deliveryLongitude: Number(deliveryLongitude),
      };

      if (couponCode.trim()) {
        requestBody.couponCode = couponCode.trim();
      }

      const response = await api.post("/api/orders/place", requestBody);

      setPlacedOrder(response.data);
      setCartItems([]);
      setCart(null);
      setCouponResult(null);

      notifyCartUpdated();

      setMessage("Order placed successfully. Please proceed to payment.");
    } catch (error) {
      setErrorMessage("Unable to place order.");
    }
  };

  if (loading) {
    return (
      <div className="cart-page">
        <div className="cart-card">
          <h2>Loading cart...</h2>
        </div>
      </div>
    );
  }

  return (
    <div className="cart-page">
      <div className="cart-header">
        <div>
          <p className="cart-subtitle">Checkout</p>
          <h1>Your Cart</h1>
          <p>Review items, apply coupon and place your order.</p>
        </div>

        <button className="cart-refresh-btn" onClick={loadCart}>
          Refresh Cart
        </button>
      </div>

      {message && <div className="success-message">{message}</div>}
      {errorMessage && <div className="error-message">{errorMessage}</div>}

      {placedOrder && (
        <div className="order-success-box">
          <h3>Order Created Successfully</h3>
          <p>
            Order ID: <strong>{placedOrder.orderId}</strong>
          </p>
          <p>
            Status: <strong>{placedOrder.orderStatus}</strong>
          </p>
          <p>
            Payment Status: <strong>{placedOrder.paymentStatus}</strong>
          </p>
          <p>
            Final Amount: <strong>₹{placedOrder.finalAmount}</strong>
          </p>
          <button
            className="proceed-payment-btn"
            onClick={() => navigate(`/payment/${placedOrder.orderId}`)}
          >
            Proceed To Payment
          </button>
        </div>
      )}

      <div className="cart-layout">
        <div className="cart-items-section">
          {cartItems.length === 0 ? (
            <div className="empty-cart">
              <div>🛒</div>
              <h2>Your cart is empty</h2>
              <p>Add pizzas from the customer dashboard.</p>
            </div>
          ) : (
            cartItems.map((item) => (
              <div className="cart-item" key={item.cartItemId || item.id}>
                <div className="cart-item-icon">🍕</div>

                <div className="cart-item-info">
                  <h3>{item.itemName}</h3>
                  <p>₹{item.price} each</p>
                </div>

                <div className="cart-quantity-control">
                  <button
                    type="button"
                    onClick={() =>
                      handleUpdateQuantity(
                        item.cartItemId || item.id,
                        Number(item.quantity) - 1,
                      )
                    }
                  >
                    -
                  </button>

                  <span>{item.quantity}</span>

                  <button
                    type="button"
                    onClick={() =>
                      handleUpdateQuantity(
                        item.cartItemId || item.id,
                        Number(item.quantity) + 1,
                      )
                    }
                  >
                    +
                  </button>
                </div>

                <div className="cart-item-total">
                  <strong>₹{item.subtotal}</strong>
                  <button
                    type="button"
                    onClick={() => handleRemoveItem(item.cartItemId || item.id)}
                  >
                    Remove
                  </button>
                </div>
              </div>
            ))
          )}

          {cartItems.length > 0 && (
            <button className="clear-cart-btn" onClick={handleClearCart}>
              Clear Cart
            </button>
          )}
        </div>

        <div className="checkout-section">
          <div className="checkout-card">
            <h2>Order Summary</h2>

            <div className="summary-row">
              <span>Cart Total</span>
              <strong>₹{getCartTotal().toFixed(2)}</strong>
            </div>

            <div className="coupon-box">
              <label>Coupon Code</label>
              <div className="coupon-input-row">
                <input
                  type="text"
                  placeholder="PIZZA20"
                  value={couponCode}
                  onChange={(event) => setCouponCode(event.target.value)}
                />
                <button onClick={handleApplyCoupon}>Apply</button>
              </div>
            </div>

            <div className="summary-row">
              <span>Discount</span>
              <strong>- ₹{getDiscountAmount().toFixed(2)}</strong>
            </div>

            <div className="summary-row final-row">
              <span>Payable Before Tax</span>
              <strong>₹{getAmountAfterDiscount().toFixed(2)}</strong>
            </div>

            <div className="delivery-box">
              <h3>Delivery Details</h3>

              <label>Address</label>
              <input
                type="text"
                value={deliveryAddress}
                onChange={(event) => setDeliveryAddress(event.target.value)}
              />

              <div className="location-row">
                <div>
                  <label>Latitude</label>
                  <input
                    type="number"
                    value={deliveryLatitude}
                    onChange={(event) =>
                      setDeliveryLatitude(event.target.value)
                    }
                  />
                </div>

                <div>
                  <label>Longitude</label>
                  <input
                    type="number"
                    value={deliveryLongitude}
                    onChange={(event) =>
                      setDeliveryLongitude(event.target.value)
                    }
                  />
                </div>
              </div>
            </div>

            <button
              className="place-order-btn"
              onClick={handlePlaceOrder}
              disabled={cartItems.length === 0}
            >
              Place Order
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CartPage;
