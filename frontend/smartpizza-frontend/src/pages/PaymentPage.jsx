import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../api/api";
import "../styles/payment.css";

function PaymentPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();

  const [order, setOrder] = useState(null);
  const [paymentResponse, setPaymentResponse] = useState(null);

  const [paymentMethod, setPaymentMethod] = useState("DUMMY");
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);

  const [message, setMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    loadOrder();
  }, [orderId]);

  const loadOrder = async () => {
    try {
      setLoading(true);
      setErrorMessage("");

      const response = await api.get(`/api/orders/${orderId}`);
      setOrder(response.data);
    } catch (error) {
      setErrorMessage("Unable to load order details.");
    } finally {
      setLoading(false);
    }
  };

  const handlePayment = async () => {
    try {
      setPaying(true);
      setMessage("");
      setErrorMessage("");

      const response = await api.post(`/api/payments/pay/${orderId}`, {
        paymentGateway: "DUMMY",
        paymentMethod: paymentMethod,
      });

      setPaymentResponse(response.data);
      setMessage("Payment completed successfully.");

      await loadOrder();
    } catch (error) {
      const backendMessage =
        error?.response?.data?.message ||
        error?.response?.data?.error ||
        "Unable to complete payment.";

      setErrorMessage(backendMessage);
    } finally {
      setPaying(false);
    }
  };

  if (loading) {
    return (
      <div className="payment-page">
        <div className="payment-card">
          <h2>Loading payment details...</h2>
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="payment-page">
        <div className="payment-card">
          <h2>Order not found</h2>
          <button className="payment-secondary-btn" onClick={() => navigate("/cart")}>
            Back to Cart
          </button>
        </div>
      </div>
    );
  }

  const isPaid = order.paymentStatus === "PAID";

  return (
    <div className="payment-page">
      <div className="payment-header">
        <div>
          <p className="payment-subtitle">Secure Checkout</p>
          <h1>Payment</h1>
          <p>Complete dummy payment for your SmartPizzaAI order.</p>
        </div>

        <button className="payment-secondary-btn" onClick={() => navigate("/cart")}>
          Back to Cart
        </button>
      </div>

      {message && <div className="success-message">{message}</div>}
      {errorMessage && <div className="error-message">{errorMessage}</div>}

      <div className="payment-layout">
        <div className="payment-card">
          <h2>Order Details</h2>

          <div className="payment-info-row">
            <span>Order ID</span>
            <strong>#{order.orderId}</strong>
          </div>

          <div className="payment-info-row">
            <span>Order Status</span>
            <strong>{order.orderStatus}</strong>
          </div>

          <div className="payment-info-row">
            <span>Payment Status</span>
            <strong>{order.paymentStatus}</strong>
          </div>

          <div className="payment-info-row">
            <span>Subtotal</span>
            <strong>₹{order.subtotal}</strong>
          </div>

          <div className="payment-info-row">
            <span>Discount</span>
            <strong>- ₹{order.discountAmount}</strong>
          </div>

          <div className="payment-info-row">
            <span>Tax</span>
            <strong>₹{order.taxAmount}</strong>
          </div>

          <div className="payment-info-row">
            <span>Delivery Charge</span>
            <strong>₹{order.deliveryCharge}</strong>
          </div>

          <div className="payment-info-row payment-final-row">
            <span>Final Amount</span>
            <strong>₹{order.finalAmount}</strong>
          </div>
        </div>

        <div className="payment-card">
          <h2>Payment Method</h2>

          <div className="dummy-payment-box">
            <div className="dummy-icon">💳</div>
            <div>
              <h3>Dummy Payment Gateway</h3>
              <p>
                This simulates payment success. Later this can be replaced with
                Razorpay or Stripe.
              </p>
            </div>
          </div>

          <label className="payment-label">Payment Method</label>
          <select
            className="payment-select"
            value={paymentMethod}
            onChange={(event) => setPaymentMethod(event.target.value)}
            disabled={isPaid}
          >
            <option value="DUMMY">DUMMY</option>
            <option value="CARD">CARD</option>
            <option value="UPI">UPI</option>
            <option value="COD">COD</option>
          </select>

          <button
            className="pay-now-btn"
            onClick={handlePayment}
            disabled={paying || isPaid}
          >
            {isPaid ? "Already Paid" : paying ? "Processing..." : `Pay ₹${order.finalAmount}`}
          </button>

          {paymentResponse && (
            <div className="payment-success-box">
              <h3>Payment Successful</h3>
              <p>
                Payment ID: <strong>{paymentResponse.paymentId}</strong>
              </p>
              <p>
                Gateway Payment ID:{" "}
                <strong>{paymentResponse.gatewayPaymentId}</strong>
              </p>
              <p>
                Transaction Status:{" "}
                <strong>{paymentResponse.transactionStatus}</strong>
              </p>
            </div>
          )}

          {isPaid && (
            <button
              className="track-delivery-btn"
              onClick={() => navigate(`/tracking/${order.orderId}`)}
            >
              Track Delivery
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export default PaymentPage;