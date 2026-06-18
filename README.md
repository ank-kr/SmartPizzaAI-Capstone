# 🍕 SmartPizzaAI

SmartPizzaAI is a full-stack AI-powered pizza ordering and delivery platform built using **Spring Boot Microservices** and **React**. The system supports customer ordering, admin management, delivery partner workflow, AI-based recommendations, analytics dashboard, coupon management, payment flow, and delivery tracking with map preview.

\---

## 📌 Project Overview

SmartPizzaAI is designed as an enterprise-style food ordering system where different users interact with the platform based on their role.

The application supports three main roles:

```text
CUSTOMER
ADMIN
DELIVERY
```

### Main Objective

The main objective of this project is to build a complete pizza ordering and delivery platform with:

* Secure authentication and authorization
* Role-based dashboards
* Menu and category management
* Cart and coupon flow
* Order and payment flow
* Delivery partner assignment
* Delivery status tracking
* AI recommendations and analytics
* Clean backend architecture
* Service-layer testing using JUnit and Mockito
* Logging using SLF4J and Logback

\---

## 🚀 Key Features

### Customer Features

* Register and login
* View pizza menu
* Search menu items
* Filter veg and non-veg items
* View AI recommended items
* View trending items
* Add items to cart
* Instant add-to-cart feedback
* Cart count badge in navbar
* Update cart item quantity
* Remove cart item
* Apply coupon
* Place order
* Dummy payment
* Track delivery
* View order history
* View delivery partner details
* View delivery route map using Leaflet

### Admin Features

* Admin dashboard
* View order analytics
* View revenue summary
* View pending order count
* View top selling items
* View delivery performance
* Add category
* Add menu item
* Create coupon
* Create delivery partner profile
* View all delivery partners
* Assign delivery partner manually if needed
* Check delivery assignment by order ID

### Delivery Partner Features

* Delivery partner login
* View active assigned deliveries
* Update delivery status
* Delivery status flow:

```text
ASSIGNED
PICKED\\\\\\\_UP
OUT\\\\\\\_FOR\\\\\\\_DELIVERY
DELIVERED
```

* Partner becomes available after delivery completion

\---

## 🧠 AI Features

SmartPizzaAI includes AI-style recommendation and analytics functionality.

### Implemented AI Features

* AI recommended items for customers
* Trending items
* Admin analytics summary
* Top selling item insights
* Delivery performance analytics

### Future AI Enhancements

The following enhancements are planned as future scope:

* Kafka consumers for event-driven analytics
* Python AI API for machine learning models
* Daily analytics summary table
* Customer behavior profile table
* Recommendation ML model
* Demand forecasting model
* Coupon personalization model
* Delivery ETA prediction model
* Agentic AI layer for business automation

> \\\\\\\*\\\\\\\*Note:\\\\\\\*\\\\\\\* Kafka, Python ML API, ML models, Customer Behavior Profile table, and Agentic AI layer are future enhancements and are not part of the current implemented flow.

\---

## 🏗️ System Architecture

### Current Architecture

```text
React Frontend
      |
      v
Spring Cloud API Gateway
      |
      +------------------------+
      |                        |
      v                        v
Auth Service              Core Service
JWT Auth                  Business Logic
User Management           Menu, Cart, Order,
                           Payment, Delivery
                                |
                                v
                         MySQL Database

AI Analytics Service
      |
      v
Recommendation and Analytics APIs

Eureka Service Registry
      |
      v
Service Discovery
```

\---

## 🧩 Microservices

### 1\. API Gateway

The API Gateway acts as the single entry point for frontend requests.

Responsibilities:

* Route requests to proper services
* Handle JWT-based authentication
* Apply role-based access control
* Centralize API routing

### 2\. Auth Service

The Auth Service handles user authentication and authorization.

Responsibilities:

* User registration
* User login
* Password encryption
* JWT token generation
* Role management

Main role enum:

```text
CUSTOMER
ADMIN
DELIVERY
```

### 3\. Core Service

The Core Service contains the main business logic.

Modules inside Core Service:

* Category Module
* Menu Item Module
* Cart Module
* Coupon Module
* Order Module
* Payment Module
* Delivery Module
* Delivery Partner Module

### 4\. AI Analytics Service

The AI Analytics Service provides recommendation and analytics APIs.

Current responsibilities:

* Customer recommendations
* Trending items
* Admin analytics
* Top selling items
* Delivery performance data

Future responsibilities:

* Kafka event consumers
* Customer behavior profiling
* Python ML API integration
* Agentic AI orchestration

### 5\. Eureka Service Registry

Eureka is used for service discovery.

Responsibilities:

* Register microservices
* Discover services dynamically
* Support Spring Cloud microservice communication

\---

## 🛠️ Technology Stack

### Frontend

```text
React
Vite
React Router
Axios
Formik
Yup
Leaflet
React-Leaflet
CSS
```

### Backend

```text
Java
Spring Boot
Spring Security
JWT
Spring Data JPA
Hibernate
Spring Cloud Gateway
Eureka Server
OpenFeign
Lombok
JUnit 5
Mockito
SLF4J
Logback
```

### Database

```text
MySQL
```

### Tools

```text
Spring Tool Suite
VS Code
MySQL Workbench
Postman
Maven
```

\---

## 📁 Project Structure

### Backend Structure

```text
smartpizza-backend
│
├── smartpizza-api-gateway
│
├── smartpizza-auth-service
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   ├── security
│   └── service
│
├── smartpizza-core-service
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── enums
│   ├── repository
│   └── service
│
├── smartpizza-ai-analytics-service
│
└── smartpizza-service-registry
```

### Frontend Structure

```text
smartpizza-frontend
│
├── public
│   └── images
│
├── src
│   ├── api
│   ├── components
│   ├── context
│   ├── pages
│   ├── styles
│   └── App.jsx
```

\---

## 🧱 Backend Layered Architecture

The backend follows layered architecture:

```text
Controller Layer
      |
      v
Service Layer
      |
      v
Repository Layer
      |
      v
Database
```

### Controller Layer

Handles HTTP requests and responses.

Examples:

```text
CartController
OrderController
PaymentController
DeliveryController
```

### Service Layer

Contains business logic.

Examples:

```text
CartService
OrderService
PaymentService
DeliveryService
CouponService
MenuService
AuthService
```

### Repository Layer

Handles database operations using Spring Data JPA.

Examples:

```text
CartRepository
OrderRepository
PaymentRepository
DeliveryRepository
```

\---

## 🎯 Design Patterns Used

### 1\. Layered Architecture

The project separates responsibilities into controller, service, repository, DTO, and entity layers.

Benefits:

* Clean structure
* Easy maintenance
* Easy testing
* Separation of concerns

### 2\. DTO Pattern

DTOs are used to transfer data between frontend and backend.

Examples:

```text
AddToCartRequest
CartResponse
OrderResponse
PaymentResponse
DeliveryResponse
CouponResponse
```

Benefits:

* Avoid exposing entity directly
* Clean API response
* Better control over request and response data

### 3\. Repository Pattern

Spring Data JPA repositories abstract database operations.

Example:

```java
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}
```

Benefits:

* Avoid boilerplate SQL
* Clean database access
* Easy unit testing with mocks

### 4\. Builder Pattern

Lombok `@Builder` is used to create objects cleanly.

Example:

```java
CartResponse.builder()
        .cartId(cart.getId())
        .userId(cart.getUserId())
        .totalAmount(cart.getTotalAmount())
        .items(itemResponses)
        .build();
```

Benefits:

* Avoid long constructors
* Improves readability
* Useful for DTO and entity creation

### 5\. Dependency Injection

Spring injects dependencies using constructor injection.

Example:

```java
@RequiredArgsConstructor
@Service
public class CartService {
    private final CartRepository cartRepository;
}
```

Benefits:

* Loose coupling
* Better testing
* Cleaner service classes

### 6\. Enum-Based State Management

Enums are used for status fields.

Examples:

```text
OrderStatus
PaymentStatus
DeliveryStatus
PartnerStatus
DiscountType
Role
```

Benefits:

* Avoid random string values
* Improves readability
* Controls valid state transitions

\---

## 🗃️ Important Entities

### User Entity

Belongs to Auth Service.

Main fields:

```text
id
fullName
email
password
phone
address
role
active
```

### Category Entity

Represents menu category.

Main fields:

```text
id
name
description
active
```

Relationship:

```text
One Category has many MenuItems
```

### MenuItem Entity

Represents pizza, drink, or side item.

Main fields:

```text
id
name
description
price
imageUrl
size
crustType
spiceLevel
veg
available
rating
category
```

Relationship:

```text
Many MenuItems belong to one Category
```

### Cart Entity

Represents customer cart.

Main fields:

```text
id
userId
totalAmount
cartItems
```

Relationship:

```text
One Cart has many CartItems
```

### CartItem Entity

Represents individual item inside cart.

Main fields:

```text
id
cart
menuItem
itemName
price
quantity
subtotal
```

Relationship:

```text
Many CartItems belong to one Cart
Many CartItems reference one MenuItem
```

### Coupon Entity

Represents discount coupon.

Main fields:

```text
id
code
description
discountType
discountValue
minOrderAmount
maxDiscount
startDate
expiryDate
active
```

### Order Entity

Represents customer order.

Main fields:

```text
id
userId
couponCode
orderStatus
paymentStatus
subtotal
discountAmount
taxAmount
deliveryCharge
finalAmount
deliveryAddress
deliveryLatitude
deliveryLongitude
orderTime
```

Relationship:

```text
One Order has many OrderItems
```

### OrderItem Entity

Represents item snapshot inside an order.

Main fields:

```text
id
order
menuItemId
itemName
price
quantity
subtotal
```

### Payment Entity

Represents dummy payment record.

Main fields:

```text
id
orderId
userId
paymentGateway
gatewayOrderId
gatewayPaymentId
amount
currency
transactionStatus
paymentMethod
paidAt
```

### DeliveryPartner Entity

Represents delivery partner profile.

Main fields:

```text
id
userId
partnerName
phone
vehicleNumber
partnerStatus
currentLatitude
currentLongitude
activeDeliveryCount
rating
```

### Delivery Entity

Represents delivery assignment.

Main fields:

```text
id
orderId
deliveryPartner
deliveryStatus
pickupLatitude
pickupLongitude
dropLatitude
dropLongitude
distanceKm
estimatedTimeMinutes
assignedAt
pickedUpAt
outForDeliveryAt
deliveredAt
```

\---

## 🔗 JPA Relationships

### Category and MenuItem

```text
Category 1 ---- \\\\\\\* MenuItem
```

One category can contain many menu items.

### Cart and CartItem

```text
Cart 1 ---- \\\\\\\* CartItem
```

One cart can contain many cart items.

### MenuItem and CartItem

```text
MenuItem 1 ---- \\\\\\\* CartItem
```

One menu item can be added to many cart items.

### Order and OrderItem

```text
Order 1 ---- \\\\\\\* OrderItem
```

One order can contain many order items.

### DeliveryPartner and Delivery

```text
DeliveryPartner 1 ---- \\\\\\\* Delivery
```

One delivery partner can complete many deliveries over time.

\---

## 🧾 ER Diagram

```text
+-------------+        +-------------+
|  Category   | 1    \\\\\\\* |  MenuItem   |
+-------------+--------+-------------+
| id          |        | id          |
| name        |        | name        |
| description |        | price       |
| active      |        | category\\\\\\\_id |
+-------------+        +-------------+

+----------+        +------------+        +-------------+
|  Cart    | 1    \\\\\\\* | CartItem   | \\\\\\\*    1 |  MenuItem   |
+----------+--------+------------+--------+-------------+
| id       |        | id         |        | id          |
| userId   |        | cart\\\\\\\_id    |        | name        |
| totalAmt |        | menuItemId |        | price       |
+----------+        | quantity   |        +-------------+
                    | subtotal   |
                    +------------+

+----------+        +-------------+
|  Order   | 1    \\\\\\\* | OrderItem   |
+----------+--------+-------------+
| id       |        | id          |
| userId   |        | order\\\\\\\_id    |
| status   |        | itemName    |
| amount   |        | price       |
+----------+        | quantity    |
                    +-------------+

+-----------------+        +-------------+
| DeliveryPartner | 1    \\\\\\\* |  Delivery   |
+-----------------+--------+-------------+
| id              |        | id          |
| userId          |        | orderId     |
| partnerStatus   |        | partner\\\\\\\_id  |
| rating          |        | status      |
+-----------------+        +-------------+
```

\---

## 🧩 UML Class Diagram

```text
+------------------+
|   CartService    |
+------------------+
| - cartRepository |
| - itemRepository |
| - menuRepository |
+------------------+
| + addToCart()       |
| + getCartByUserId() |
| + updateCartItem()  |
| + removeCartItem()  |
| + clearCart()       |
+------------------+

+------------------+
|   OrderService   |
+------------------+
| - cartRepository |
| - orderRepository|
| - couponRepository |
+------------------+
| + placeOrder()   |
| + getOrderById() |
| + getOrdersByUserId() |
+------------------+

+------------------+
|  DeliveryService |
+------------------+
| - partnerRepository |
| - deliveryRepository |
| - orderRepository |
+------------------+
| + assignDeliveryPartner() |
| + updateDeliveryStatus()  |
| + getDeliveryByOrderId()  |
+------------------+
```

\---

## 👤 Use Case Diagram

```text
                  +----------------------+
                  |     SmartPizzaAI     |
                  +----------------------+

Customer
   |
   |-- Register/Login
   |-- View Menu
   |-- Add to Cart
   |-- Apply Coupon
   |-- Place Order
   |-- Make Payment
   |-- Track Delivery
   |-- View Order History

Admin
   |
   |-- Manage Categories
   |-- Manage Menu Items
   |-- Manage Coupons
   |-- Create Delivery Partner
   |-- View Analytics
   |-- Assign Delivery Partner

Delivery Partner
   |
   |-- Login
   |-- View Active Deliveries
   |-- Update Delivery Status
```

\---

## 🔄 DFD Level 0

```text
+----------+          +----------------+          +------------+
| Customer | -------> | SmartPizzaAI   | -------> | Database   |
+----------+          | System         |          +------------+
                      +----------------+
+-------+                     ^
| Admin | --------------------|
+-------+                     |
                              |
+------------------+----------+
| Delivery Partner |
+------------------+
```

## 🔄 DFD Level 1

```text
Customer
   |
   v
Authentication Process
   |
   v
Menu and Cart Process
   |
   v
Order Process
   |
   v
Payment Process
   |
   v
Delivery Tracking Process

Admin
   |
   v
Menu Management Process
   |
   v
Analytics Process
   |
   v
Delivery Partner Management Process

Delivery Partner
   |
   v
Delivery Status Update Process
```

\---

## 🔐 Security

Security is implemented using:

```text
Spring Security
JWT
Role-Based Authorization
API Gateway
```

### JWT Flow

```text
User Login
    |
    v
Auth Service validates credentials
    |
    v
JWT token generated
    |
    v
Frontend stores token
    |
    v
API Gateway validates token
    |
    v
Request routed to target service
```

### Roles

```text
CUSTOMER
ADMIN
DELIVERY
```

### Security Note

Some current APIs use `userId` in request bodies for customer operations. For stronger IDOR protection, a future improvement should derive authenticated `userId` directly from JWT or validate request `userId` against JWT user identity.

\---

## 🛒 Cart Flow

```text
Customer clicks Add +
      |
      v
CartService.addToCart()
      |
      v
Validate request
      |
      v
Fetch MenuItem
      |
      v
Get or create Cart
      |
      v
Check if item already exists
      |
      +----------------------+
      |                      |
      v                      v
Create new CartItem     Update existing quantity
      |                      |
      +----------+-----------+
                 |
                 v
          Update cart total
                 |
                 v
          Return CartResponse
```

\---

## 🧾 Order Flow

```text
Customer places order
      |
      v
Validate request
      |
      v
Fetch cart items
      |
      v
Calculate subtotal
      |
      v
Apply coupon discount
      |
      v
Calculate tax and delivery charge
      |
      v
Create order with PAYMENT\\\\\\\_PENDING
      |
      v
Create order items from cart items
      |
      v
Clear cart
      |
      v
Return OrderResponse
```

\---

## 💳 Payment Flow

```text
Customer pays order
      |
      v
PaymentService.payOrder()
      |
      v
Validate order
      |
      v
Create dummy payment record
      |
      v
Set paymentStatus = PAID
      |
      v
Set orderStatus = CONFIRMED
      |
      v
Trigger delivery assignment
      |
      v
Return PaymentResponse
```

\---

## 🚚 Delivery Flow

```text
Payment completed
      |
      v
Order confirmed
      |
      v
DeliveryService.assignDeliveryPartner()
      |
      v
Find available delivery partner
      |
      v
Select best partner
      |
      v
Create delivery record
      |
      v
Mark partner BUSY
      |
      v
Update order status to ASSIGNED\\\\\\\_TO\\\\\\\_DELIVERY
      |
      v
Delivery partner updates status
      |
      v
DELIVERED
      |
      v
Partner becomes AVAILABLE
```

\---

## 🗺️ Delivery Map Tracking

The project uses:

```text
Leaflet
React-Leaflet
OpenStreetMap
```

Map tracking shows:

* Restaurant pickup marker
* Delivery partner marker
* Customer drop marker
* Route line between pickup and drop
* ETA and distance

The current delivery marker is simulated based on delivery status.

\---

## 📜 Logging

The backend uses SLF4J logging with Lombok:

```java
@Slf4j
```

Logger levels used:

```text
INFO  -> successful business operations
WARN  -> validation failures and expected business errors
DEBUG -> internal calculations
```

Example logs:

```text
Cart updated successfully
Delivery assigned successfully
Payment completed successfully
Delivery status updated successfully
```

A log file is generated using Logback configuration.

Recommended log path:

```text
logs/smartpizza-core-service.log
```

Hibernate SQL logs are reduced to avoid excessive log file size.

\---

## 🧪 Testing

Service-layer unit testing is implemented using:

```text
JUnit 5
Mockito
```

Tested services include:

```text
AuthService
MenuService
CartService
CouponService
OrderService
PaymentService
DeliveryService
```

Test cases cover:

* Success scenarios
* Validation failures
* Duplicate checks
* Not found cases
* Status changes
* Coupon discount calculation
* Payment completion
* Delivery partner assignment
* Delivery status update

\---

## ⚙️ Setup Instructions

### Prerequisites

Install:

```text
Java 17+
Maven
MySQL
Node.js
npm
Spring Tool Suite
VS Code
```

\---

## 🗄️ Database Setup

Create databases:

```sql
CREATE DATABASE smartpizza\\\\\\\_auth\\\\\\\_db;
CREATE DATABASE smartpizza\\\\\\\_core\\\\\\\_db;
```

Update credentials in each service:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartpizza\\\\\\\_core\\\\\\\_db
spring.datasource.username=root
spring.datasource.password=your\\\\\\\_password
```

Do not commit real passwords in public repositories.

\---

## ▶️ Backend Run Order

Start services in this order:

```text
1. smartpizza-service-registry
2. smartpizza-auth-service
3. smartpizza-core-service
4. smartpizza-ai-analytics-service
5. smartpizza-api-gateway
```

Default ports:

```text
Eureka Server        -> 8761
API Gateway          -> 8080
Auth Service         -> 8081
Core Service         -> 8082
AI Analytics Service -> 8083
Frontend             -> 5173
```

\---

## ▶️ Frontend Setup

Go to frontend folder:

```bash
cd smartpizza-frontend
```

Install dependencies:

```bash
npm install
```

Run frontend:

```bash
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

\---

## 🔑 Demo Login Users

Use these only if they exist in your local database.

```text
Customer:
email: ankit@gmail.com
password: ankit123

Admin:
email: admin@gmail.com
password: admin123

Delivery:
email: delivery@gmail.com
password: delivery123
```

\---

## 📡 Important API Examples

### Auth APIs

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/check-email?email=
```

### Menu APIs

```http
POST /api/categories
GET  /api/categories
POST /api/menu-items
GET  /api/menu-items
GET  /api/menu-items/{id}
```

### Cart APIs

```http
POST   /api/cart/add
GET    /api/cart/{userId}
PUT    /api/cart/update/{cartItemId}
DELETE /api/cart/remove/{cartItemId}
DELETE /api/cart/clear/{userId}
```

### Coupon APIs

```http
POST /api/coupons
GET  /api/coupons/active
POST /api/coupons/apply
```

### Order APIs

```http
POST /api/orders/place
GET  /api/orders/{orderId}
GET  /api/orders/user/{userId}
GET  /api/orders/admin/all
```

### Payment APIs

```http
POST /api/payments/pay/{orderId}
GET  /api/payments/order/{orderId}
GET  /api/payments/user/{userId}
```

### Delivery APIs

```http
POST /api/delivery/partners
GET  /api/delivery/partners
POST /api/delivery/assign/{orderId}
PUT  /api/delivery/status/{deliveryId}
GET  /api/delivery/order/{orderId}
GET  /api/delivery/{deliveryId}
GET  /api/delivery/partner/user/{userId}/active
```

\---

## 🧠 Future Enhancement Architecture

```text
smartpizza-core-service
        |
        | publishes business events
        v
Kafka Topics
        |
        v
Kafka Consumers in AI Analytics Service
        |
        +--> Daily Analytics Summary Table
        |
        +--> Customer Behavior Profile Table
        |
        v
Python AI API
        |
        v
ML Models
        |
        v
Recommendations, Forecasting, Coupons, ETA, Admin Alerts
```

### Future Components

```text
Kafka Consumers
Python AI API
Daily Analytics Summary Table
Customer Behavior Profile
Recommendation ML Model
Demand Forecasting Model
Coupon Personalization Model
Delivery ETA Prediction Model
Agentic AI Layer
```

\---

## 📌 Current Implementation Status

### Implemented

```text
JWT Authentication
Role-Based Access
API Gateway Routing
Eureka Service Discovery
Menu Management
Cart Management
Coupon Management
Order Management
Payment Flow
Delivery Management
Delivery Tracking Map
AI Recommendations
Admin Analytics
Logging
JUnit Service Tests
Formik/Yup Auth Form Validation
```

### Future Scope

```text
Kafka Event Streaming
Real GPS Tracking
Python ML API
Advanced ML Recommendation Model
Customer Behavior Profile
Agentic AI Automation
Real Payment Gateway
WebSocket Notifications
Docker Deployment
Cloud Deployment
```

\---

## ✅ Project Status

The project is currently demo-ready with working:

```text
Customer flow
Admin flow
Delivery partner flow
Order placement
Payment
Delivery assignment
Delivery tracking
AI recommendations
Analytics
Backend logging
Service-layer testing
```

\---

## 👨‍💻 Developed By

```text
Ankit Kumar
Project Engineer
SmartPizzaAI Full-Stack Microservices Project
```

\---

## 📄 License

This project is created for learning, training, and project demonstration purposes.

