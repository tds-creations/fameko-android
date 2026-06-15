# Fameko Driver & Rental Platform

Fameko is a comprehensive ride-hailing and vehicle rental platform designed for the African market. It features dedicated applications for drivers and customers, a robust backend, and specialized routing services.

## Project Structure

The project is organized into several modules:

- **`:app`**: The Android application for Drivers. Handles registration, document uploads, live location tracking, delivery acceptance, and SOS alerts.
- **`:app-customer`**: The Android application for Customers. Features ride booking, rental vehicle catalog, real-time tracking, and payment history.
- **`:backend`**: A Kotlin/Ktor server that serves as the central API. It manages:
  - User authentication (Drivers & Customers)
  - Admin Dashboard (Thymeleaf-based)
  - Database operations (PostgreSQL)
  - Payment processing via Paystack
  - Real-time communication via WebSockets
  - Firebase Cloud Messaging (FCM) for notifications
- **`:core`**: An Android library containing shared UI components, domain models, and utilities used by both mobile apps.
- **`:shared-models`**: A Kotlin library containing data classes and models shared between the backend and the Android applications to ensure type safety.
- **`routing-service`**: A Python-based service focused on route calculations and optimization.

## Key Features

### For Drivers
- **Onboarding**: Upload license, insurance, and roadworthy certificates.
- **Wallet & Payments**: Pay daily service fees via Paystack to go online.
- **Live Orders**: Receive and accept nearby delivery/ride requests.
- **Fleet Management**: Owners can add and manage multiple vehicles for rental.
- **Safety**: One-tap SOS button to alert emergency contacts and admins.

### For Customers
- **Ride Hailing**: Get fare estimates and book rides based on vehicle category (Economy, Comfort, Package).
- **Vehicle Rental**: Browse a catalog of available vehicles, book for specific durations, and choose between self-drive or chauffeur-driven.
- **Real-time Tracking**: Track drivers and rental vehicles in real-time.
- **In-app Chat**: Secure communication with drivers.

### Admin Panel
- **Dashboard**: Overview of platform stats, active deliveries, and revenue.
- **Driver Verification**: Approve or reject driver registrations based on submitted documents.
- **Financials**: Monitor revenue from rides, rental fees, and daily driver fees.
- **System Settings**: Manage app versions, support contacts, and maintenance mode.

## API Documentation (Summary)

The backend exposes several REST endpoints for the mobile applications and admin panel:

### Auth & User Management
- `POST /customer/login`, `POST /customer/register`
- `POST /driver/login`, `POST /driver/register`
- `POST /update-fcm-token`: Register device tokens for push notifications.

### Ride & Delivery
- `POST /orders/estimates`: Get fare estimates for different vehicle types.
- `POST /orders/create`: Create a new ride or delivery order.
- `GET /orders/status/{orderId}`: Get real-time status of an order.
- `POST /driver/accept-delivery`: Accept a pending delivery (Drivers).

### Rentals
- `GET /rentals/vehicles`: List all available vehicles for rent.
- `POST /rentals/book`: Initialize a rental booking with Paystack payment.
- `GET /rentals/history/{customerId}`: Get rental history for a user.

### Safety & Communication
- `POST /safety/sos`: Trigger an emergency alert.
- `POST /chat/send`, `GET /chat/history/{convId}`: In-app messaging.

## Tech Stack

- **Mobile**: Kotlin, Jetpack Compose, Google Maps SDK, Retrofit, Firebase.
- **Backend**: Kotlin, Ktor, Exposed (ORM), PostgreSQL, Thymeleaf, WebSockets.
- **Integrations**: Paystack API (Payments), Firebase Cloud Messaging.
- **Infrastructure**: Docker support, Railway (Deployment).

## Getting Started

### Prerequisites
- Android Studio Ladybug or newer.
- JDK 17.
- PostgreSQL Database.
- Paystack API Key.

### Building the Project
1. Clone the repository.
2. Configure `local.properties` with your Google Maps API key.
3. For the backend, set environment variables:
   - `DB_URL`, `DB_USER`, `DB_PASSWORD`
   - `PAYSTACK_SECRET`
4. Sync Gradle and build.

---
© 2024 TDS Creations. All rights reserved.
