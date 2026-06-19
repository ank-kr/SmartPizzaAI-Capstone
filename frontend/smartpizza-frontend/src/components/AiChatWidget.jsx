import { useState } from "react";
import api from "../api/api";
import "../styles/AiChatWidget.css";

const AiChatWidget = () => {
  const [open, setOpen] = useState(false);
  const [message, setMessage] = useState("");
  const [chatHistory, setChatHistory] = useState([
    {
      sender: "ai",
      text: "Hi! I am SmartPizzaAI assistant. I can help with recommendations, order status, coupons, payment and delivery tracking.",
      suggestions: [
        "Recommend me a pizza",
        "What is my order status?",
        "How can I apply coupon?",
      ],
    },
  ]);
  const [loading, setLoading] = useState(false);

  const getLoggedInUserId = () => {
    const directUserId = localStorage.getItem("userId");

    if (directUserId) {
      return Number(directUserId);
    }

    const token = localStorage.getItem("token");

    if (token) {
      try {
        const payload = JSON.parse(atob(token.split(".")[1]));

        if (payload?.userId) {
          return Number(payload.userId);
        }
      } catch (error) {
        return null;
      }
    }

    return null;
  };

  const sendMessage = async (customMessage) => {
    const finalMessage = customMessage || message.trim();

    if (!finalMessage) {
      return;
    }

    const userId = getLoggedInUserId();

    if (!userId) {
      setChatHistory((prev) => [
        ...prev,
        {
          sender: "ai",
          text: "Please login again. I could not identify your user session.",
          suggestions: [],
        },
      ]);
      return;
    }

    setChatHistory((prev) => [
      ...prev,
      {
        sender: "user",
        text: finalMessage,
        suggestions: [],
      },
    ]);

    setMessage("");
    setLoading(true);

    try {
      const response = await api.post("/ai/chat", {
        userId,
        message: finalMessage,
      });

      setChatHistory((prev) => [
        ...prev,
        {
          sender: "ai",
          text: response.data.reply,
          responseType: response.data.responseType,
          suggestions: response.data.suggestions || [],
        },
      ]);
    } catch (error) {
      setChatHistory((prev) => [
        ...prev,
        {
          sender: "ai",
          text: "Sorry, I am unable to respond right now. Please try again after some time.",
          suggestions: [
            "Recommend me a pizza",
            "Track my order",
            "Help with payment",
          ],
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter") {
      sendMessage();
    }
  };

  return (
    <>
      <button
        className={`ai-chat-floating-button ${open ? "open" : ""}`}
        onClick={() => setOpen(!open)}
        title="SmartPizzaAI Assistant"
      >
        {open ? (
          <span className="ai-close-icon">×</span>
        ) : (
          <>
            <span className="ai-spark">✦</span>
            <span className="ai-text">AI</span>
            <span className="ai-online-dot"></span>
          </>
        )}
      </button>

      {open && (
        <div className="ai-chat-container">
          <div className="ai-chat-header">
            <div>
              <h3>SmartPizzaAI Assistant</h3>
              <p>Ask about orders, menu, coupons, payment or delivery</p>
            </div>
            <button onClick={() => setOpen(false)}>×</button>
          </div>

          <div className="ai-chat-body">
            {chatHistory.map((chat, index) => (
              <div key={index} className={`ai-chat-message ${chat.sender}`}>
                <div className="ai-chat-bubble">
                  <p>{chat.text}</p>

                  {chat.responseType && (
                    <span className="ai-response-type">
                      {chat.responseType}
                    </span>
                  )}
                </div>

                {chat.suggestions && chat.suggestions.length > 0 && (
                  <div className="ai-suggestion-list">
                    {chat.suggestions.map((suggestion, suggestionIndex) => (
                      <button
                        key={suggestionIndex}
                        onClick={() => sendMessage(suggestion)}
                        disabled={loading}
                      >
                        {suggestion}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {loading && (
              <div className="ai-chat-message ai">
                <div className="ai-chat-bubble">
                  <p>Thinking...</p>
                </div>
              </div>
            )}
          </div>

          <div className="ai-chat-input-area">
            <input
              type="text"
              placeholder="Ask SmartPizzaAI..."
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              onKeyDown={handleKeyDown}
              disabled={loading}
            />

            <button
              onClick={() => sendMessage()}
              disabled={loading || !message.trim()}
            >
              Send
            </button>
          </div>
        </div>
      )}
    </>
  );
};

export default AiChatWidget;
