# 🚗 Ride Sharing Backend - Complete Codebase Analysis

## 📋 Table of Contents
1. [Overview & Tech Stack](#overview--tech-stack)
2. [Architecture & Design](#architecture--design)
3. [Data Model](#data-model)
4. [Complete Flow Documentation](#complete-flow-documentation)
5. [Detailed Code Flow](#detailed-code-flow)
6. [API Endpoints](#api-endpoints)
7. [Configuration & Infrastructure](#configuration--infrastructure)
8. [Key Technologies & Integrations](#key-technologies--integrations)

---

## 🎯 Overview & Tech Stack

### Project Information
- **Name**: ride
- **Version**: 0.0.1-SNAPSHOT
- **Port**: 8082
- **Language**: Java 17
- **Framework**: Spring Boot 3.5.3
- **Build Tool**: Maven

### Technology Stack
```
┌─────────────────────────────────────────────────────┐
│           RIDE SHARING BACKEND STACK                │
├─────────────────────────────────────────────────────┤
│ Backend Framework    → Spring Boot 3.5.3           │
│ Java Version         → Java 17                      │
│ Database             → PostgreSQL (Relational)      │
│ Cache/Real-time      → Redis (In-memory Store)     │
│ API Documentation    → SpringDoc OpenAPI 2.8.13   │
│ Security             → Spring Security + OAuth2    │
│ Authentication       → JWT (RSA Public Key)        │
│ Payment Processing   → Razorpay API 1.4.3          │
│ Push Notifications   → Firebase Admin 9.2.0        │
│ Async Processing     → Spring @Async               │
│ WebSocket/Reactive   → Spring WebFlux              │
│ Location Services    → JTS (Java Topology Suite)   │
│ HTTP Client          → WebClient (Reactive)        │
│ Input Validation     → Jakarta Validation          │
│ ORM                  → Spring Data JPA             │
│ Data Modeling        → Lombok                      │
└─────────────────────────────────────────────────────┘
```

---

## 🏗️ Architecture & Design

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      CLIENT APPLICATIONS                         │
│              (Mobile Apps / Web Frontend)                        │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT REST API                           │
│                    (Port: 8082)                                  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            CONTROLLERS LAYER (API Gateway)               │  │
│  │ ├─ RideController                                        │  │
│  │ ├─ PaymentController                                     │  │
│  │ ├─ DriverLocationController                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         │                                        │
│                         ▼                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │           SERVICE LAYER (Business Logic)                 │  │
│  │ ├─ RideService (Core ride orchestration)                │  │
│  │ ├─ PaymentService (Payment processing)                  │  │
│  │ ├─ DriverLocationService (Geo-spatial queries)          │  │
│  │ └─ NotificationHubService (Event broadcasting)          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         │                                        │
│                         ▼                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │      REPOSITORY LAYER (Data Access Object)               │  │
│  │ ├─ RideRepository                                        │  │
│  │ ├─ BookingRepository                                     │  │
│  │ └─ PaymentRepository                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         │                                        │
│        ┌────────────────┼────────────────────┐                 │
│        ▼                ▼                    ▼                 │
└────────┼────────────────┼────────────────────┼─────────────────┘
         │                │                    │
         ▼                ▼                    ▼
    ┌─────────┐      ┌────────┐         ┌──────────┐
    │PostgreSQL │      │ Redis  │         │ Firebase │
    │ Database  │      │ Cache  │         │  Notify  │
    │  (SQL)    │      │(Events)│         │  (Push)  │
    └─────────┘      └────────┘         └──────────┘
         │                │
         └────────────────┘
         Persistence & Real-time
         Events
```

### Layered Architecture Breakdown

**1. Controller Layer** - REST API endpoints
- Handles HTTP requests/responses
- Input validation
- Route mapping
- Response serialization

**2. Service Layer** - Business Logic
- Core application logic
- Transaction management (@Transactional)
- External service integration (Razorpay, Firebase)
- Event publishing

**3. Repository Layer** - Data Access
- Database queries via JPA
- Custom query methods
- CRUD operations

**4. Configuration Layer** - Infrastructure Setup
- Security configuration (JWT/OAuth2)
- Async execution pools
- Redis configuration
- WebClient configuration

---

## 📊 Data Model

### Entity Relationship Diagram

```
                    ┌──────────────┐
                    │    RIDE      │
                    ├──────────────┤
                    │  id: UUID    │
                    │  driver: UUID│
                    │  vehicle:UUID│
                    │  seatFare    │
                    │  seats: int  │
                    │  status      │
                    │  coordinates │
                    └────────┬──────┘
                             │ (1:Many)
                             │
                    ┌────────▼──────────┐
                    │    BOOKING        │
                    ├───────────────────┤
                    │  id: UUID         │
                    │  ride_id: UUID    │
                    │  passenger: UUID  │
                    │  seatsBooked: int │
                    │  totalPaid        │
                    │  status           │
                    │  verified flags   │
                    └────────┬──────────┘
                             │ (1:1)
                             │
                    ┌────────▼──────────┐
                    │    PAYMENT        │
                    ├───────────────────┤
                    │  id: UUID         │
                    │  ride_id: UUID    │
                    │  amount           │
                    │  paymentMode      │
                    │  status           │
                    │  razorpay_order_id│
                    └───────────────────┘
```

### Entity Details

#### 1. **Ride Entity**
```java
@Entity
@Table(name = "ride")
public class Ride {
    UUID id;                           // Primary Key (Auto-generated UUID)
    UUID driver;                       // Driver ID (Foreign Key Reference)
    UUID vehicle;                      // Vehicle ID (Foreign Key Reference)
    Double seatFare;                   // Per-seat fare amount
    Double startLatitude;              // Pickup location (Latitude)
    Double startLongitude;             // Pickup location (Longitude)
    Double endLatitude;                // Drop-off location (Latitude)
    Double endLongitude;               // Drop-off location (Longitude)
    Integer seats;                     // Available seats (Dynamic)
    LocalDateTime depart;              // Departure time
    Status status;                     // CREATED → ACTIVE → COMPLETED
    boolean deviationThresholdExceeded; // Flag for route deviation
}

Status Enum: CREATED, ACTIVE, COMPLETED, CANCELLED, PENDING, AWAITING_SETTLEMENT
```

#### 2. **Booking Entity**
```java
@Entity
@Table(name = "bookings")
public class Booking {
    UUID id;                      // Primary Key
    Ride ride;                    // Many-to-One relationship
    UUID passenger;               // Passenger ID
    Integer seatsBooked;          // Number of seats reserved
    Double totalPaid;             // Total amount for booking
    Status status;                // PENDING → CONFIRMED → ONBOARDED → COMPLETED
    LocalDateTime createdAt;      // Booking creation timestamp
    boolean driverVerified;       // Biometric verification flag (Driver)
    boolean passengerVerified;    // Biometric verification flag (Passenger)
}

Status Enum: PENDING, CONFIRMED, REJECTED, EXPIRED, ONBOARDED, CANCELLED, COMPLETED
```

#### 3. **Payment Entity**
```java
@Entity
@Table(name = "payments")
public class Payment {
    UUID id;                        // Primary Key
    UUID rideId;                    // Ride ID (Foreign Key)
    UUID passengerId;               // Passenger ID
    BigDecimal amount;              // Payment amount (Precise monetary value)
    PaymentMode paymentMode;        // COD or NETBANKING
    PaymentStatus status;           // PENDING → SUCCESS or FAILED
    String razorpayOrderId;         // Razorpay Order ID (Cloud Session)
    LocalDateTime createdAt;        // Creation timestamp (Auto-set)
    LocalDateTime updatedAt;        // Last update timestamp (Auto-set)
}

PaymentMode Enum: COD (Cash on Delivery), NETBANKING
PaymentStatus Enum: PENDING, SUCCESS, FAILED
```

---

## 🔄 Complete Flow Documentation

### 🎯 Complete Ride Lifecycle Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                    RIDE LIFECYCLE (Main Flow)                       │
└─────────────────────────────────────────────────────────────────────┘

PHASE 1: RIDE CREATION
────────────────────────
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 1: Driver Creates Ride                                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Driver sends: POST /api/rides/create                               │
│ {                                                                   │
│   "driverId": "uuid",                                              │
│   "vehicleId": "uuid",                                             │
│   "startLatitude": 17.3850,    // Pickup coordinates               │
│   "startLongitude": 78.4867,                                       │
│   "endLatitude": 17.3500,      // Drop-off coordinates             │
│   "endLongitude": 78.5000,                                         │
│   "availableSeats": 4,                                             │
│   "seatFare": 100.50           // Per-seat fare                    │
│ }                                                                   │
│                                                                      │
│ Backend Validation:                                                │
│ ✓ Check if driver already has CREATED/ACTIVE ride                 │
│ ✓ Validate all coordinates (Latitude: -90 to 90, Longitude: -180) │
│ ✓ Validate seats > 0                                              │
│ ✓ Validate fare >= 0                                              │
│                                                                      │
│ Database Action:                                                   │
│ ✓ Create Ride entity with status = CREATED                        │
│ ✓ Save to PostgreSQL                                              │
│                                                                      │
│ Response:                                                          │
│ {                                                                   │
│   "rideId": "generated-uuid",                                      │
│   "driverId": "uuid",                                              │
│   "status": "CREATED",                                             │
│   "availableSeats": 4,                                             │
│   "seatFare": 100.50                                               │
│ }                                                                   │
│                                                                      │
│ State After: Ride.status = CREATED                                │
└─────────────────────────────────────────────────────────────────────┘


PHASE 2: PASSENGER BOOKING REQUEST
────────────────────────────────────
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 2: Passenger Requests to Book Seats                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Passenger sends: POST /api/rides/{rideId}/request-book             │
│ {                                                                   │
│   "passengerId": "uuid",                                           │
│   "seatsToBook": 2                                                 │
│ }                                                                   │
│                                                                      │
│ Backend Validation:                                                │
│ ✓ Check if ride exists                                            │
│ ✓ Check if ride status is CREATED or ACTIVE (not closed)          │
│ ✓ Verify available seats >= seatsToBook                           │
│                                                                      │
│ Database Action:                                                   │
│ ✓ Create Booking entity with status = PENDING                     │
│ ✓ Calculate totalPaid = seatFare * seatsBooked                    │
│ ✓ Set createdAt = now()                                           │
│ ✓ Save to PostgreSQL                                              │
│                                                                      │
│ Notification Action: 🔴 ASYNC (Background Thread)                │
│ ┌─────────────────────────────────────────────────────────────┐   │
│ │ Send Redis notification to DRIVER:                          │   │
│ │ - Type: BOOKING_REQUEST                                    │   │
│ │ - Title: "New Ride Request! 🎯"                            │   │
│ │ - Message: "A passenger wants to book 2 seat(s)..."        │   │
│ │ - Extra Data: bookingId, rideId                            │   │
│ │                                                             │   │
│ │ Channel: ride:lifecycle:events (Redis Pub/Sub)             │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ Response:                                                          │
│ "Booking request submitted successfully! Awaiting response..."    │
│                                                                      │
│ State After:                                                       │
│ - Booking.status = PENDING                                        │
│ - Ride.status = CREATED (unchanged)                               │
│ - Driver receives notification                                    │
└─────────────────────────────────────────────────────────────────────┘


PHASE 3: DRIVER RESPONDS TO BOOKING
─────────────────────────────────────
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3A: Driver ACCEPTS the Booking                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Driver sends: PUT /api/rides/bookings/{bookingId}/respond          │
│ ?accept=true                                                       │
│                                                                      │
│ Backend Validation:                                                │
│ ✓ Check if booking exists                                         │
│ ✓ Check booking status = PENDING (not already handled)            │
│ ✓ Verify ride still has available seats                           │
│                                                                      │
│ Database Actions:                                                  │
│ ✓ Update Booking.status = CONFIRMED                               │
│ ✓ Update Ride.seats -= booking.seatsBooked                        │
│ ✓ If Ride.status = CREATED, change to ACTIVE                      │
│                                                                      │
│ Notification Action: 🔴 ASYNC (Background Thread)                │
│ ┌─────────────────────────────────────────────────────────────┐   │
│ │ Send Redis notification to PASSENGER:                       │   │
│ │ - Type: BOOKING_ACCEPTED                                   │   │
│ │ - Title: "Ride Confirmed! 🎉"                              │   │
│ │ - Message: "Your driver has approved. Meet at pickup..."   │   │
│ │ - Extra Data: rideId, updatedSeats                         │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ Response:                                                          │
│ "Passenger successfully confirmed on your route manifest..."      │
│                                                                      │
│ State After:                                                       │
│ - Booking.status = CONFIRMED                                      │
│ - Ride.status = ACTIVE (transitions from CREATED if needed)       │
│ - Ride.seats decremented                                          │
│ - Passenger receives notification                                 │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ STEP 3B: Driver REJECTS the Booking                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Driver sends: PUT /api/rides/bookings/{bookingId}/respond          │
│ ?accept=false                                                      │
│                                                                      │
│ Database Action:                                                   │
│ ✓ Update Booking.status = REJECTED                                │
│                                                                      │
│ Notification Action: 🔴 ASYNC                                    │
│ ┌─────────────────────────────────────────────────────────────┐   │
│ │ Send Redis notification to PASSENGER:                       │   │
│ │ - Type: BOOKING_REJECTED                                   │   │
│ │ - Title: "Request Update ❌"                                │   │
│ │ - Message: "The driver was unable to accept..."            │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ Response:                                                          │
│ "Booking request successfully rejected. Passenger notified."      │
│                                                                      │
│ State After:                                                       │
│ - Booking.status = REJECTED                                       │
│ - Ride remains unchanged                                          │
│ - Passenger receives rejection notification                       │
└─────────────────────────────────────────────────────────────────────┘


PHASE 4: BIOMETRIC VERIFICATION (Both Parties)
────────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4: Trip Start - Biometric Verification                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Driver/Passenger sends: POST /api/rides/start/{bookingId}          │
│ Content-Type: multipart/form-data                                  │
│ {                                                                   │
│   "type": "DRIVER",        // or "PASSENGER"                       │
│   "file": <face-image>     // Biometric image                      │
│ }                                                                   │
│                                                                      │
│ Backend Flow (COMPLEX MICROSERVICE CALL):                          │
│ ┌─────────────────────────────────────────────────────────────┐   │
│ │ 1. Fetch booking by ID                                      │   │
│ │ 2. Determine user: if DRIVER → ride.driver, else passenger │   │
│ │ 3. Call Auth Service (Port 8081):                           │   │
│ │    GET /api/auth/users/{userId}/embedding                  │   │
│ │    ↓ Response: List<Double> (Face embedding vector)        │   │
│ │ 4. Convert embedding to comma-separated string             │   │
│ │ 5. Call FastAPI Python Service (Port 8000):                │   │
│ │    POST /verify/compare                                    │   │
│ │    ├─ file: uploaded face image                            │   │
│ │    └─ stored_vector_string: embedding vector               │   │
│ │    ↓ Response: { "is_match": true/false }                  │   │
│ │ 6. If NOT match → Throw error "Biometric Mismatch"         │   │
│ │ 7. Update Booking:                                         │   │
│ │    ├─ If DRIVER → driverVerified = true                    │   │
│ │    └─ If PASSENGER → passengerVerified = true              │   │
│ │ 8. Check if BOTH verified:                                 │   │
│ │    ├─ YES: status = ONBOARDED                              │   │
│ │    └─ NO: status unchanged (awaiting peer)                 │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ Notification Action: 🔴 ASYNC (if both verified)                  │
│ ┌─────────────────────────────────────────────────────────────┐   │
│ │ Send to PASSENGER:                                          │   │
│ │ - Type: RIDE_STARTED                                       │   │
│ │ - Message: "Your node identity verified. Welcome aboard!"  │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ Response:                                                          │
│ {                                                                   │
│   "status": "MUTUAL_ONBOARDING_COMPLETE" or                       │
│            "NODE_VERIFIED_AWAITING_PEER"                          │
│ }                                                                   │
│                                                                      │
│ State After:                                                       │
│ - Booking.driverVerified = true (if driver verified)              │
│ - Booking.passengerVerified = true (if passenger verified)        │
│ - Booking.status = ONBOARDED (when both verified)                │
└─────────────────────────────────────────────────────────────────────┘


PHASE 5: RIDE COMPLETION & PAYMENT SETTLEMENT
──────────────────────────────────────────────
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 5A: Complete Ride (Phase A: End Trip)                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Driver sends: PUT /api/rides/{rideId}/complete                     │
│ ?actualKm=25.5&actualMins=45&mode=NETBANKING                       │
│                                                                      │
│ Backend Flow:                                                       │
│ ┌─────────────────────────────────────────────────────────────┐   │
│ │ 1. Fetch ride by ID                                         │   │
│ │ 2. Check if ride NOT already completed                      │   │
│ │ 3. Calculate final fare:                                    │   │
│ │    IF ride.deviationThresholdExceeded:                      │   │
│ │    ├─ kmCost = actualKm * 12.50                             │   │
│ │    ├─ minCost = actualMins * 2.00                           │   │
│ │    ├─ finalFare = 50.00 + kmCost + minCost                  │   │
│ │    ELSE:                                                    │   │
│ │    └─ finalFare = ride.seatFare                             │   │
│ │ 4. Update Ride.status = AWAITING_SETTLEMENT                 │   │
│ │ 5. Fetch all ONBOARDED bookings for this ride              │   │
│ │ 6. For each booking:                                        │   │
│ │    ├─ If deviation: booking.totalPaid = finalFare *         │   │
│ │    │                 seatsBooked                             │   │
│ │    ├─ Create Payment record with status = PENDING           │   │
│ │    └─ Send payment notification                             │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ Notification Action: 🔴 ASYNC (for each passenger)               │
│ ┌─────────────────────────────────────────────────────────────┐   │
│ │ Send to each PASSENGER:                                     │   │
│ │ - Type: PAYMENT_DUE                                         │   │
│ │ - Title: "Arrived at Destination! 🏁"                      │   │
│ │ - Message: "Please settle ₹{amount} via {mode}"            │   │
│ │ - Extra: rideId, amount                                     │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ Response:                                                          │
│ {                                                                   │
│   "status": "AWAITING_SETTLEMENT",                                 │
│   "finalFarePerSeat": "100.50"                                     │
│ }                                                                   │
│                                                                      │
│ State After:                                                       │
│ - Ride.status = AWAITING_SETTLEMENT                               │
│ - Payment records created with PENDING status                     │
│ - Passengers notified with payment amount                         │
└─────────────────────────────────────────────────────────────────────┘


PHASE 6: PAYMENT PROCESSING (Two Paths)
─────────────────────────────────────────
┌─────────────────────────────────────────────────────────────────────┐
│ PAYMENT PATH A: Cash on Delivery (COD)                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Driver sends: POST /api/rides/{rideId}/settle-cash                 │
│                                                                      │
│ Backend Flow:                                                       │
│ ├─ Call settleAndCloseRide(rideId)                                 │
│ ├─ Check ride.status = AWAITING_SETTLEMENT                         │
│ ├─ Update Payment.status = SUCCESS                                 │
│ ├─ Update Ride.status = COMPLETED                                  │
│ ├─ Update all Booking.status = COMPLETED                           │
│ └─ Send completion notification                                    │
│                                                                      │
│ State After:                                                       │
│ - Ride.status = COMPLETED                                         │
│ - Payment.status = SUCCESS                                         │
│ - Booking.status = COMPLETED                                       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ PAYMENT PATH B: Online Banking (NETBANKING) - Razorpay             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ PART 1: Payment Creation                                           │
│ ──────────────────────────                                         │
│ Backend creates Payment record:                                    │
│ ├─ Amount: booking.totalPaid (in INR)                              │
│ ├─ Call Razorpay API (via com.razorpay.RazorpayClient):           │
│ │  POST https://api.razorpay.com/v1/orders                        │
│ │  {                                                              │
│ │    "amount": 10050,      // In Paise (amount * 100)             │
│ │    "currency": "INR",                                           │
│ │    "receipt": "{rideId}"                                        │
│ │  }                                                              │
│ ├─ Razorpay Response: Order ID (e.g., "order_Kxp23Lm987")        │
│ ├─ Store razorpayOrderId in Payment record                         │
│ └─ Send payment link to passenger (Mobile)                        │
│                                                                      │
│ PART 2: Async Webhook - Payment Confirmed                         │
│ ──────────────────────────────────────────                        │
│ Bank/Razorpay calls: POST /api/payments/webhook/mock-bank          │
│ {                                                                   │
│   "event": "payment.captured",                                     │
│   "payload": {                                                     │
│     "order_id": "order_Kxp23Lm987",                               │
│     ...                                                            │
│   }                                                                │
│ }                                                                   │
│                                                                      │
│ Backend Flow:                                                       │
│ ├─ Extract event type ("payment.captured")                        │
│ ├─ Extract order_id from payload                                   │
│ ├─ Query: findByRazorpayOrderId(order_id)                          │
│ ├─ Get rideId from Payment record                                  │
│ ├─ Call settleAndCloseRide(rideId)                                 │
│ │  ├─ Update Payment.status = SUCCESS                              │
│ │  ├─ Update Ride.status = COMPLETED                               │
│ │  └─ Update Bookings.status = COMPLETED                           │
│ └─ Send completion notification                                    │
│                                                                      │
│ Webhook Response:                                                  │
│ {                                                                   │
│   "status": "Async payment cleared. Ride finalized cleanly..."    │
│ }                                                                   │
│                                                                      │
│ State After:                                                       │
│ - Ride.status = COMPLETED                                         │
│ - Payment.status = SUCCESS                                         │
│ - All Bookings.status = COMPLETED                                  │
└─────────────────────────────────────────────────────────────────────┘


PHASE 7: FINAL NOTIFICATION & ARCHIVAL
──────────────────────────────────────
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 7: Ride Completion Notification                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│ Notification Action: 🔴 ASYNC (for each passenger)               │
│ ┌─────────────────────────────────────────────────────────────┐   │
│ │ Send to each PASSENGER:                                     │   │
│ │ - Type: RIDE_COMPLETED                                      │   │
│ │ - Title: "Payment Confirmed! Clean Receipt Issued"          │   │
│ │ - Message: "Thank you! Your transaction has cleared..."    │   │
│ │ - Rating/Feedback trigger                                  │   │
│ └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│ Final State:                                                       │
│ ✓ Ride.status = COMPLETED                                         │
│ ✓ Booking.status = COMPLETED                                      │
│ ✓ Payment.status = SUCCESS                                        │
│ ✓ All records archived in database                                │
│ ✓ Ride removed from active list                                   │
│                                                                      │
│ Completion Confirmation:                                          │
│ "Solapur ride finalized cleanly, bro!"                            │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔀 Detailed Code Flow

### 1️⃣ Create Ride - End-to-End Code Flow

```
CLIENT REQUEST:
POST /api/rides/create
{
  "driverId": "550e8400-e29b-41d4-a716-446655440000",
  "vehicleId": "550e8400-e29b-41d4-a716-446655440001",
  "startLatitude": 17.3850,
  "startLongitude": 78.4867,
  "endLatitude": 17.3500,
  "endLongitude": 78.5000,
  "availableSeats": 4,
  "seatFare": 100.50
}

↓

RideController.createRide():
  ├─ @PostMapping("/create")
  ├─ @Valid @RequestBody RidecreateDTO requestDTO
  ├─ Call: rideService.create(requestDTO)
  └─ Return: ResponseEntity<RideResponseDTO> with HTTP 201 (CREATED)

↓

RideService.create(RidecreateDTO):
  ├─ @Transactional
  ├─ Query DB: rideRepo.findByDriverAndStatusIn(driverId, [CREATED, ACTIVE])
  ├─ IF rides NOT empty → throw RuntimeException("Ride already exists!")
  │
  ├─ Build Ride Entity:
  │  ├─ id: @GeneratedValue(UUID)
  │  ├─ driver: requestDTO.driverId()
  │  ├─ vehicle: requestDTO.vehicleId()
  │  ├─ seatFare: requestDTO.seatFare()
  │  ├─ startLatitude/Longitude: requestDTO coordinates
  │  ├─ endLatitude/Longitude: requestDTO coordinates
  │  ├─ seats: requestDTO.availableSeats()
  │  ├─ depart: LocalDateTime.now()
  │  └─ status: Ride.Status.CREATED
  │
  ├─ Save: rideRepo.save(rd)
  │  └─ Database Execution:
  │     INSERT INTO ride (id, driver, vehicle, seat_fare, start_latitude,
  │     start_longitude, end_latitude, end_longitude, seats, depart, status)
  │     VALUES (...)
  │
  └─ Return: RideResponseDTO
     {
       rideId: rd.getId(),
       driverId: rd.getDriver(),
       vehicleId: rd.getVehicle(),
       startLatitude: rd.getStartLatitude(),
       startLongitude: rd.getStartLongitude(),
       endLatitude: rd.getEndLatitude(),
       endLongitude: rd.getEndLongitude(),
       availableSeats: rd.getSeats(),
       seatFare: rd.getSeatFare(),
       status: "CREATED",
       createdAt: rd.getDepart()
     }

↓

HTTP RESPONSE: 201 Created
{
  "rideId": "550e8400-e29b-41d4-a716-446655440002",
  "driverId": "550e8400-e29b-41d4-a716-446655440000",
  "vehicleId": "550e8400-e29b-41d4-a716-446655440001",
  "startLatitude": 17.3850,
  "startLongitude": 78.4867,
  "endLatitude": 17.3500,
  "endLongitude": 78.5000,
  "availableSeats": 4,
  "seatFare": 100.50,
  "status": "CREATED",
  "createdAt": "2025-08-16T14:30:00"
}
```

### 2️⃣ Request Booking - Complete Flow

```
CLIENT REQUEST:
POST /api/rides/550e8400-e29b-41d4-a716-446655440002/request-book
{
  "passengerId": "550e8400-e29b-41d4-a716-446655440003",
  "seatsToBook": 2
}

↓

RideController.requestBooking():
  ├─ @PostMapping("/{rideId}/request-book")
  ├─ @PathVariable UUID rideId: "550e8400-e29b-41d4-a716-446655440002"
  ├─ @Valid @RequestBody RideBookRequestDTO rdbook
  ├─ Call: rideService.requestbook(rdbook, rideId)
  └─ Return: ResponseEntity<String>

↓

RideService.requestbook(RideBookRequestDTO, UUID):
  ├─ @Transactional
  ├─ Query DB: rideRepo.findById(rideId)
  │  └─ SELECT * FROM ride WHERE id = '550e8400-e29b-41d4-a716-446655440002'
  ├─ IF not found → throw RuntimeException("Target ride route does not exist")
  │
  ├─ Validation 1: Check Ride Status
  │  IF (rd.getStatus() != CREATED && rd.getStatus() != ACTIVE)
  │  → throw IllegalArgumentException("Onboarding Blocked: Ride closed")
  │
  ├─ Validation 2: Check Available Seats
  │  IF rd.getSeats() < seatsToBook (4 < 2? NO → continue)
  │  → throw IllegalArgumentException("Only X seats available")
  │
  ├─ Build Booking Entity:
  │  ├─ id: @GeneratedValue(UUID)
  │  ├─ ride: rd (relationship)
  │  ├─ passenger: dto.passengerId() = "550e8400-e29b-41d4-a716-446655440003"
  │  ├─ seatsBooked: dto.seatsToBook() = 2
  │  ├─ totalPaid: rd.getSeatFare() * seatsBooked = 100.50 * 2 = 201.00
  │  ├─ status: Booking.Status.PENDING
  │  └─ createdAt: LocalDateTime.now()
  │
  ├─ Save: repoBook.save(book)
  │  └─ INSERT INTO bookings (id, ride_id, passenger_id, seats_booked,
  │     total_paid, status, created_at)
  │     VALUES (new_uuid, '550e8400...', '550e8400...', 2, 201.00, 'PENDING', now)
  │
  ├─ 🔴 ASYNC NOTIFICATION (Background Thread - NotificationHubService):
  │  ├─ Call: notificationHub.sendRedisNotification()
  │  ├─ Recipient: rd.getDriver() = driver's UUID
  │  ├─ Event Type: "BOOKING_REQUEST"
  │  ├─ Title: "New Ride Request! 🎯"
  │  ├─ Body: "A passenger wants to book 2 seat(s) on your route."
  │  ├─ Extra Data: {"bookingId": booking_id, "rideId": ride_id}
  │  │
  │  └─ Redis Operations (Background):
  │     ├─ Create payload JSON:
  │     │  {
  │     │    "recipientId": driver_uuid,
  │     │    "type": "BOOKING_REQUEST",
  │     │    "title": "New Ride Request! 🎯",
  │     │    "message": "A passenger wants to book 2 seat(s)...",
  │     │    "timestamp": 1724080200000,
  │     │    "bookingId": booking_id,
  │     │    "rideId": ride_id
  │     │  }
  │     ├─ Channel: rideTopic.getTopic() = "ride:lifecycle:events"
  │     └─ Publish: redisTemplate.convertAndSend("ride:lifecycle:events", payload)
  │        (All subscribed driver apps receive this notification)
  │
  └─ Return: "Booking request submitted successfully! Awaiting driver response."

↓

HTTP RESPONSE: 200 OK
"Booking request submitted successfully! Awaiting response from driver."

DATABASE STATE:
┌─────────────────────────────────────────────────────────────────┐
│ RIDE TABLE                                                      │
├──────────────────┬─────────────────────────────────────────────┤
│ id               │ 550e8400-e29b-41d4-a716-446655440002        │
│ seats            │ 4  (unchanged, seats not decremented yet)   │
│ status           │ CREATED                                      │
└──────────────────┴─────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ BOOKINGS TABLE (NEW RECORD)                                     │
├──────────────────┬─────────────────────────────────────────────┤
│ id               │ 550e8400-e29b-41d4-a716-446655440004        │
│ ride_id          │ 550e8400-e29b-41d4-a716-446655440002        │
│ passenger_id     │ 550e8400-e29b-41d4-a716-446655440003        │
│ seats_booked     │ 2                                            │
│ total_paid       │ 201.00                                       │
│ status           │ PENDING                                      │
│ created_at       │ 2025-08-16T14:31:00                         │
│ driver_verified  │ false                                        │
│ passenger_verified│ false                                       │
└──────────────────┴─────────────────────────────────────────────┘

REDIS STATE:
Channel: "ride:lifecycle:events"
Message (in queue for driver subscription):
{
  "recipientId": "550e8400-e29b-41d4-a716-446655440000",
  "type": "BOOKING_REQUEST",
  "title": "New Ride Request! 🎯",
  "message": "A passenger wants to book 2 seat(s) on your route.",
  "timestamp": 1724080260000,
  "bookingId": "550e8400-e29b-41d4-a716-446655440004",
  "rideId": "550e8400-e29b-41d4-a716-446655440002"
}
```

### 3️⃣ Driver Responds to Booking (ACCEPT)

```
CLIENT REQUEST:
PUT /api/rides/bookings/550e8400-e29b-41d4-a716-446655440004/respond?accept=true

↓

RideController.respondToBookingRequest():
  ├─ @PutMapping("/bookings/{bookingId}/respond")
  ├─ @PathVariable UUID bookingId: "550e8400-e29b-41d4-a716-446655440004"
  ├─ @RequestParam boolean accept: true
  ├─ Call: rideService.responsebook(bookingId, true)
  └─ Return: ResponseEntity<String>

↓

RideService.responsebook(UUID, boolean):
  ├─ @Transactional
  ├─ Query: repoBook.findById(bookingId)
  │  └─ SELECT * FROM bookings WHERE id = '550e8400-e29b-41d4-a716-446655440004'
  ├─ Booking found: status = PENDING ✓
  ├─ Check: if status != PENDING → abort (already handled)
  │
  ├─ Get related Ride: Ride rd = book.getRide()
  ├─ Since accept = true:
  │  ├─ Validation: Check remaining seats: rd.getSeats() >= book.getSeatsBooked()
  │  │  4 >= 2? YES ✓
  │  │
  │  ├─ Decrement Available Seats:
  │  │  rd.setSeats(4 - 2) = 2
  │  │
  │  ├─ Update Booking Status:
  │  │  book.setStatus(Booking.Status.CONFIRMED)
  │  │
  │  ├─ Transition Ride if Needed:
  │  │  IF rd.getStatus() == CREATED:
  │  │    └─ rd.setStatus(Ride.Status.ACTIVE)
  │  │
  │  ├─ Persist Changes:
  │  │  repoBook.save(book)
  │  │  rideRepo.save(rd)
  │  │  UPDATE bookings SET status='CONFIRMED' WHERE id='550e8400...'
  │  │  UPDATE ride SET seats=2, status='ACTIVE' WHERE id='550e8400...'
  │  │
  │  └─ 🔴 ASYNC NOTIFICATION (Background Thread):
  │     ├─ Recipient: book.getPassenger()
  │     ├─ Event Type: "BOOKING_ACCEPTED"
  │     ├─ Title: "Ride Confirmed! 🎉"
  │     ├─ Body: "Your driver has approved! Meet at pickup location."
  │     ├─ Extra: {"rideId": ride_id, "updatedSeats": 2}
  │     └─ Publish to Redis channel: "ride:lifecycle:events"
  │
  └─ Return: "Passenger successfully confirmed on your route manifest ledger."

↓

HTTP RESPONSE: 200 OK
"Passenger successfully confirmed on your route manifest ledger."

DATABASE STATE AFTER:
┌────────────────────────────────────────────────────────────────┐
│ RIDE TABLE (UPDATED)                                           │
├──────────────────┬───────────────────────────────────────────┤
│ id               │ 550e8400-e29b-41d4-a716-446655440002      │
│ seats            │ 2  (4 - 2 = 2) ← SEATS DECREMENTED       │
│ status           │ ACTIVE (transitioned from CREATED)       │
└──────────────────┴───────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│ BOOKINGS TABLE (UPDATED)                                       │
├──────────────────┬───────────────────────────────────────────┤
│ status           │ CONFIRMED (was PENDING)                  │
│ driver_verified  │ false                                      │
│ passenger_verified│ false                                    │
└──────────────────┴───────────────────────────────────────────┘

REDIS STATE:
Passenger receives notification on "ride:lifecycle:events":
{
  "recipientId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "BOOKING_ACCEPTED",
  "title": "Ride Confirmed! 🎉",
  "message": "Your driver has approved. Meet at pickup location.",
  "timestamp": 1724080320000,
  "rideId": "550e8400-e29b-41d4-a716-446655440002",
  "updatedSeats": 2
}
```

### 4️⃣ Payment Processing Flow (Razorpay)

```
STEP 1: Complete Ride (End Trip)
─────────────────────────────────
PUT /api/rides/{rideId}/complete?actualKm=25.5&actualMins=45&mode=NETBANKING

↓ RideService.completebook()
├─ Calculate final fare based on route deviation
├─ Create Payment records with PENDING status:
│  ├─ paymentService.createPendingPayment(rideId, passengerId, totalPaid, NETBANKING)
│  │
│  └─ Inside PaymentService.createPendingPayment():
│     ├─ Build Payment entity
│     ├─ IF mode == NETBANKING:
│     │  ├─ amountInPaise = amount * 100 (1005000 for 100.50)
│     │  ├─ Build JSONObject orderRequest:
│     │  │  {
│     │  │    "amount": 1005000,
│     │  │    "currency": "INR",
│     │  │    "receipt": "550e8400-e29b-41d4-a716-446655440002"
│     │  │  }
│     │  ├─ 🌐 NETWORK CALL: razorpayClient.orders.create(orderRequest)
│     │  │  └─ HTTP POST to Razorpay Cloud: https://api.razorpay.com/v1/orders
│     │  │     Headers: Authorization: Basic {base64(keyId:keySecret)}
│     │  │
│     │  ├─ 📥 Razorpay Response:
│     │  │  {
│     │  │    "id": "order_Kxp23Lmn987",
│     │  │    "entity": "order",
│     │  │    "amount": 1005000,
│     │  │    "currency": "INR",
│     │  │    "status": "created",
│     │  │    "receipt": "550e8400-e29b-41d4-a716-446655440002",
│     │  │    "created_at": 1724080400
│     │  │  }
│     │  │
│     │  └─ payment.setRazorpayOrderId("order_Kxp23Lmn987")
│     │
│     ├─ payment.setStatus(PaymentStatus.PENDING)
│     ├─ payment.setPaymentMode(PaymentMode.NETBANKING)
│     └─ paymentRepository.save(payment)
│        INSERT INTO payments (id, ride_id, passenger_id, amount,
│        payment_mode, status, razorpay_order_id, created_at)
│        VALUES (uuid, ride_id, passenger_id, 100.50, 'NETBANKING',
│        'PENDING', 'order_Kxp23Lmn987', now)

├─ Send payment notification to passengers:
│  notificationHub.sendRedisNotification(
│    passengerId,
│    "PAYMENT_DUE",
│    "Arrived at Destination! 🏁",
│    "Please settle ₹100.50 via NETBANKING",
│    {"rideId": ride_id, "amount": "100.50"}
│  )
│
└─ Ride.status = AWAITING_SETTLEMENT


STEP 2: Passenger Processes Payment
───────────────────────────────────
Mobile App receives payment notification with order_id:
├─ Open Razorpay checkout (pre-filled amount, order_id)
├─ Passenger enters bank credentials
├─ Bank processes payment
└─ Payment successful


STEP 3: Razorpay Webhook Callback
──────────────────────────────────
Razorpay POST /api/payments/webhook/mock-bank

{
  "event": "payment.captured",
  "created_at": 1724080500,
  "payload": {
    "payment": {
      "id": "pay_Kxp23Lmn988",
      "entity": "payment",
      "amount": 1005000,
      "currency": "INR",
      "status": "captured",
      "order_id": "order_Kxp23Lmn987",  ← KEY: Links to our Payment record
      ...
    }
  }
}

↓ PaymentController.handleMockBankWebhook()
├─ Extract event = "payment.captured"
├─ Extract payload.order_id = "order_Kxp23Lmn987"
├─ Call: rideService.findRideIdByRazorpayOrder("order_Kxp23Lmn987")
│  └─ Query: paymentRepository.findByRazorpayOrderId("order_Kxp23Lmn987")
│     SELECT * FROM payments WHERE razorpay_order_id = 'order_Kxp23Lmn987'
│     ↓ Result: Payment record with rideId = 550e8400-e29b-41d4-a716-446655440002
│
├─ Get rideId = 550e8400-e29b-41d4-a716-446655440002
│
├─ Call: rideService.settleAndCloseRide(rideId)
│  └─ @Transactional
│     ├─ Fetch Ride: status must be AWAITING_SETTLEMENT ✓
│     ├─ Call: payment.settlePaymentLocally(rideId)
│     │  └─ PaymentService:
│     │     ├─ Query: paymentRepository.findByRideId(rideId)
│     │     ├─ payment.setStatus(PaymentStatus.SUCCESS)
│     │     └─ paymentRepository.save(payment)
│     │        UPDATE payments SET status='SUCCESS', updated_at=now
│     │        WHERE ride_id='550e8400...'
│     │
│     ├─ Update Ride:
│     │  ├─ rd.setStatus(Ride.Status.COMPLETED)
│     │  └─ rideRepo.save(rd)
│     │     UPDATE ride SET status='COMPLETED' WHERE id='550e8400...'
│     │
│     ├─ Fetch all ONBOARDED bookings:
│     │  └─ repoBook.findByRideAndStatus(rd, ONBOARDED)
│     │
│     ├─ For each booking:
│     │  ├─ booking.setStatus(Booking.Status.COMPLETED)
│     │  ├─ repoBook.save(booking)
│     │  │  UPDATE bookings SET status='COMPLETED'...
│     │  │
│     │  └─ 🔴 ASYNC NOTIFICATION:
│     │     notificationHub.sendRedisNotification(
│     │       passengerId,
│     │       "RIDE_COMPLETED",
│     │       "Payment Confirmed! Clean Receipt Issued",
│     │       "Thank you for riding! Transaction cleared.",
│     │       null
│     │     )
│     │
│     └─ Return: "Solapur ride finalized cleanly, bro!"
│
└─ HTTP Response: "Async payment cleared. Ride finalized cleanly!"

DATABASE FINAL STATE:
┌─────────────────────────────────────────────────────────────┐
│ RIDE TABLE                                                  │
├────────────────────┬─────────────────────────────────────┤
│ id                 │ 550e8400-e29b-41d4-a716-446655440002│
│ status             │ COMPLETED (was AWAITING_SETTLEMENT) │
└────────────────────┴─────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ BOOKINGS TABLE                                              │
├────────────────────┬─────────────────────────────────────┤
│ status             │ COMPLETED (was ONBOARDED)           │
└────────────────────┴─────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ PAYMENTS TABLE                                              │
├────────────────────┬─────────────────────────────────────┤
│ status             │ SUCCESS (was PENDING)               │
│ razorpay_order_id  │ order_Kxp23Lmn987                  │
│ updated_at         │ 2025-08-16T14:38:45                │
└────────────────────┴─────────────────────────────────────┘
```

---

## 📡 API Endpoints

### 🚗 Ride Management

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/rides/create` | Create a new ride |
| POST | `/api/rides/{rideId}/request-book` | Passenger requests booking |
| PUT | `/api/rides/bookings/{bookingId}/respond` | Driver accepts/rejects booking |
| POST | `/api/rides/start/{bookingId}` | Biometric verification (start trip) |
| PUT | `/api/rides/{rideId}/complete` | Complete ride & initiate payment |
| POST | `/api/rides/{rideId}/settle-cash` | Settlement for cash payment |
| GET | `/api/rides/bookings/{user}` | Get user's booking history |

### 💳 Payment Processing

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/payments/webhook/mock-bank` | Razorpay webhook callback |

### 📍 Driver Location

| Method | Endpoint | Purpose |
|--------|----------|---------|
| PUT | `/api/drivers/location` | Update driver's current location |
| GET | `/api/drivers/nearby` | Find drivers near a location |
| DELETE | `/api/drivers/location` | Remove driver from active pool |

---

## ⚙️ Configuration & Infrastructure

### Database Configuration
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/smr
spring.datasource.username=postgres
spring.datasource.password=Nagraj@2005
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**Key Points:**
- PostgreSQL DBMS
- Automatic schema generation (update mode)
- SQL query logging enabled for debugging
- UTF-8 character set

### Security Configuration (SecurityConfig.java)
```
┌──────────────────────────────────────────────────────┐
│           SPRING SECURITY FILTER CHAIN               │
├──────────────────────────────────────────────────────┤
│ 1. CSRF Protection         → Disabled (Stateless)    │
│ 2. Session Management      → STATELESS (JWT only)    │
│ 3. Authorized Endpoints:                            │
│    ├─ /swagger-ui/**       → permitAll()             │
│    ├─ /v3/api-docs/**      → permitAll()             │
│    ├─ /api/rides/create    → @authenticated()        │
│    └─ /**                  → permitAll()             │
│ 4. OAuth2 Resource Server  → Validates JWT          │
└──────────────────────────────────────────────────────┘

JWT Validation Process:
├─ Public Key Path: classpath:keys/public.key
├─ Key Format: RSA Public Key (PEM encoded)
├─ Decoder: NimbusJwtDecoder (RFC 7515/7518)
├─ Base64 Decode PEM → X509EncodedKeySpec
└─ Verify signature on every request
```

### Async Configuration (AsyncConfig.java)
```
┌──────────────────────────────────────────────────────┐
│       THREAD POOL FOR NOTIFICATIONS                  │
├──────────────────────────────────────────────────────┤
│ Bean Name              → "notificationExecutor"      │
│ Core Pool Size         → 5 threads (baseline)        │
│ Max Pool Size          → 25 threads (peak traffic)   │
│ Queue Capacity         → 200 tasks                   │
│ Thread Naming          → NotifyWorker-{number}       │
│ Task Overflow Policy   → Block (waits for slot)      │
└──────────────────────────────────────────────────────┘

Execution Flow:
┌──────────────────┐
│ Main Thread      │ (HTTP Request Handler)
│ calls            │ notificationHub.sendRedisNotification()
└────────┬─────────┘
         │ @Async("notificationExecutor")
         ▼
    ┌──────────────────────┐
    │ Thread Pool Queue     │ (up to 200 pending tasks)
    └──────────────────────┘
         │
         ├─→ NotifyWorker-1 (sends to Redis)
         ├─→ NotifyWorker-2
         ├─→ NotifyWorker-3
         └─→ ... (up to 25 concurrent)

Benefits:
✓ Non-blocking request handling (no client lag)
✓ Scalable notification dispatch
✓ Backpressure management via queue limits
✓ Graceful degradation under high load
```

### Redis Configuration (RedisConfig.java)
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
  RedisTemplate<String, Object> template = new RedisTemplate<>();
  template.setConnectionFactory(cf);
  template.setKeySerializer(new StringRedisSerializer());
  template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
  return template;
}

@Bean
public ChannelTopic rideTopic() {
  return new ChannelTopic("ride:lifecycle:events");
}
```

**Purpose:**
- Key Serialization: Plain strings for key readability
- Value Serialization: JSON (enables complex object passing)
- Pub/Sub Channel: `ride:lifecycle:events` for real-time events
- TTL: Used for driver location heartbeat (60 seconds)

### WebClient Configuration (WebConfig.java)
```java
@Bean
public WebClient webClient() {
  return WebClient.builder()
    .baseUrl("http://10.158.244.135:8000") // FastAPI server
    .build();
}
```

**Use Cases:**
- Fetch user facial embeddings from Auth Service (port 8081)
- Send face comparison requests to Python FastAPI (port 8000)
- Biometric verification workflow

---

## 🔧 Key Technologies & Integrations

### 1. **Spring Boot 3.5.3**
- Latest LTS version
- Modern Spring ecosystem
- Spring Native support ready

### 2. **PostgreSQL Database**
- ACID compliance
- UUID support (native)
- Geographic data (PostGIS potential)
- Transactional consistency

### 3. **Redis (Pub/Sub & Caching)**
```
Use Cases:
├─ Real-time notifications via Pub/Sub channels
├─ Driver location tracking (Geo commands)
├─ Session management cache
├─ Rate limiting
└─ Event broadcasting

Operations Used:
├─ GEO ADD: Store driver coordinates
├─ GEO RADIUS: Find nearby drivers
├─ PUBLISH/SUBSCRIBE: Event streaming
└─ STRING: Heartbeat tracking
```

### 4. **Firebase Admin SDK**
- Push notifications to mobile apps
- Cloud messaging integration
- User authentication (potential)

### 5. **Razorpay Payment Gateway**
```
Flow:
┌─────────────────────────────────────────┐
│ CREATE ORDER (Backend)                  │
├─────────────────────────────────────────┤
│ Amount: in Paise (* 100)                │
│ Receipt: Ride ID (for tracking)         │
│ Return: Order ID                        │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ CHECKOUT (Mobile/Web Frontend)          │
├─────────────────────────────────────────┤
│ Display Razorpay modal                  │
│ Passenger enters payment details        │
│ Bank processes transaction              │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ WEBHOOK CALLBACK (Asynchronous)         │
├─────────────────────────────────────────┤
│ Event: payment.captured                 │
│ Order ID: Links to our Payment record  │
│ Settle transaction in DB                │
└─────────────────────────────────────────┘

Security:
✓ Test keys used (rzp_test_*)
✓ Signature verification (HMAC)
✓ Order isolation per ride
```

### 6. **JWT Authentication**
```
Flow:
┌────────────────────┐
│ Client Request     │ Header: Authorization: Bearer <JWT>
└─────────┬──────────┘
          │
          ▼
┌────────────────────────────────────────┐
│ SecurityFilterChain                    │
├────────────────────────────────────────┤
│ 1. Extract token from Authorization   │
│ 2. Decode with public key (RSA)       │
│ 3. Verify signature                   │
│ 4. Extract claims (userId, roles)     │
│ 5. Create Authentication object       │
└─────────┬──────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────┐
│ Controller (@AuthenticationPrincipal)  │
├────────────────────────────────────────┤
│ Jwt jwt = ... get claims               │
│ UUID userId = jwt.getClaimAsString()   │
│ Use authenticated user context         │
└────────────────────────────────────────┘
```

### 7. **Spring Data JPA**
```
Query Generation:
├─ findByDriverAndStatusIn()
│  → SELECT * FROM ride 
│    WHERE driver = ? AND status IN (?, ?)
│
├─ findByPassenger()
│  → SELECT * FROM bookings 
│    WHERE passenger_id = ?
│
└─ findByRazorpayOrderId()
   → SELECT * FROM payments 
     WHERE razorpay_order_id = ?

Transactional Guarantees:
├─ @Transactional: ACID compliance
├─ Rollback on exception
├─ Read consistency
└─ Dirty read prevention
```

### 8. **Lombok**
```java
@Getter @Setter          // Automatic getters/setters
@Builder                 // Fluent object construction
@AllArgsConstructor      // Constructor with all fields
@NoArgsConstructor       // Default no-args constructor
@Data                    // @Getter + @Setter + @toString + @equals + @hashCode
```

---

## 📊 Transaction Flow Summary

```
┌─────────────────────────────────────────────────────────────┐
│         COMPLETE SYSTEM TRANSACTION SEQUENCE                │
└─────────────────────────────────────────────────────────────┘

[1] CREATE RIDE
    POST /api/rides/create
    ↓ Save to DB
    Ride.status = CREATED

[2] PASSENGER BOOKS
    POST /api/rides/{id}/request-book
    ↓ Save to DB
    Booking.status = PENDING
    ↓ ASYNC send notification
    Driver receives booking alert

[3] DRIVER RESPONDS
    PUT /api/rides/bookings/{id}/respond?accept=true
    ↓ Update DB
    Booking.status = CONFIRMED
    Ride.seats -= booked seats
    Ride.status = ACTIVE
    ↓ ASYNC send notification
    Passenger receives confirmation

[4] BIOMETRIC VERIFICATION (Both parties)
    POST /api/rides/start/{bookingId}
    ├─ Fetch face embedding
    ├─ Call FastAPI for face comparison
    └─ Update verification flags
    ↓
    When both verified:
    Booking.status = ONBOARDED

[5] COMPLETE RIDE
    PUT /api/rides/{id}/complete
    ├─ Calculate final fare
    └─ Create Payment records
    ↓
    Ride.status = AWAITING_SETTLEMENT
    Payment.status = PENDING
    ↓ ASYNC send payment notifications

[6] PAYMENT SETTLEMENT
    OPTION A: Cash on Delivery
    └─ POST /api/rides/{id}/settle-cash
       ↓
       Payment.status = SUCCESS
       Ride.status = COMPLETED
    
    OPTION B: Online Banking (Razorpay)
    └─ POST /api/payments/webhook/mock-bank
       (Asynchronous webhook from Razorpay)
       ↓
       Payment.status = SUCCESS
       Ride.status = COMPLETED

[7] FINAL NOTIFICATION
    ↓ ASYNC send completion notification
    Passenger receives receipt & rating prompt
    
    Final State:
    ✓ Ride.status = COMPLETED
    ✓ Booking.status = COMPLETED
    ✓ Payment.status = SUCCESS
    ✓ All records archived
```

---

## 🎯 Key Design Patterns

### 1. **Service Layer Pattern**
- Controllers → Services → Repositories
- Clear separation of concerns
- Business logic isolated

### 2. **Async/Non-blocking Pattern**
- @Async methods for notifications
- Thread pool management
- WebClient reactive calls

### 3. **Transaction Management**
- @Transactional for data consistency
- Automatic rollback on exception
- Read-only queries for performance

### 4. **Pub/Sub Event Broadcasting**
- Redis channels for real-time updates
- Decoupled service communication
- Scalable notification delivery

### 5. **Webhook Integration**
- Asynchronous payment callbacks
- Idempotent processing
- Event-driven architecture

---

## 📈 Performance Considerations

```
Database Indices (Recommended):
├─ rides(driver, status)      [Composite]
├─ bookings(ride_id)          [Foreign Key]
├─ bookings(passenger_id)     [Query filtering]
└─ payments(razorpay_order_id)[Webhook lookup]

Connection Pooling:
├─ PostgreSQL: Hikari CP (default in Spring)
├─ Pool Size: 10-20 connections
└─ Timeout: 30 seconds

Redis Configuration:
├─ Persistence: RDB (snapshot)
├─ Eviction: LRU (if memory exceeds max)
└─ TTL: Driver heartbeat = 60 seconds

Caching Strategy:
├─ Driver locations: Real-time (Geo index)
├─ Ride details: TTL-based
└─ User embeddings: Request-time fetch
```

---

## 🛡️ Security Features

```
1. CSRF Protection: DISABLED (Stateless JWT)
2. CORS: Configure per environment
3. JWT Signing: RSA-2048 (Asymmetric)
4. Secret Management: External key files
5. Session: Stateless (No cookies)
6. OAuth2: Resource server (Bearer token)
7. Input Validation: Jakarta Validation
8. SQL Injection: Parameterized queries (JPA)
9. Sensitive Data: BigDecimal for money (precision)
10. Logging: SQL queries visible (development only)
```

---

This comprehensive analysis covers every aspect of your ride-sharing backend system!
