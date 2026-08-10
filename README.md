# 🚗 SMR Ride-Sharing Platform — Backend

> **A production-grade, microservices-based ride-sharing platform** built with **Spring Boot** (Java 17) and **Python FastAPI**, featuring biometric identity verification, real-time Redis pub/sub notifications, Razorpay payment integration, and geospatial ride search.

---

## 📋 Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Tech Stack](#-tech-stack)
- [Microservices Breakdown](#-microservices-breakdown)
  - [1. SMR Auth Service (`smr/`)](#1-smr-auth-service---port-8081)
  - [2. Ride Service (`ride/`)](#2-ride-service---port-8082)
  - [3. Search Service (`Search/`)](#3-search-service---python-fastapi-port-8000)
  - [4. Verify Service (`Verify/`)](#4-verify-service---python-fastapi)
- [System Flow & Ride Lifecycle](#-system-flow--ride-lifecycle)
- [API Endpoints](#-api-endpoints)
- [Database Schema](#-database-schema)
- [Security & Authentication](#-security--authentication)
- [Real-Time Notifications](#-real-time-notifications)
- [Biometric Face Verification](#-biometric-face-verification)
- [Geospatial Search Engine](#-geospatial-search-engine)
- [Payment Integration](#-payment-integration)
- [Configuration](#-configuration)
- [Running the Project](#-running-the-project)
- [Project Structure](#-project-structure)

---

## 🏗 Architecture Overview

The backend follows a **polyglot microservices architecture** with **4 independent services** communicating via HTTP (WebClient) and Redis Pub/Sub:

```
┌────────────────────────────────────────────────────────────────┐
│                     Mobile / Web Client                        │
└──────────┬──────────────────────────────┬──────────────────────┘
           │                              │
           ▼                              ▼
┌──────────────────────┐    ┌──────────────────────────────┐
│   SMR Auth Service   │    │       Ride Service            │
│   (Spring Boot)      │    │    (Spring Boot)              │
│   Port: 8081         │◄──►│    Port: 8082                 │
│                      │    │                              │
│  • User Registration │    │  • Ride CRUD                │
│  • JWT Auth          │    │  • Booking Management        │
│  • OTP Verification  │    │  • Razorpay Payments        │
│  • Vehicle Mgmt      │    │  • Redis Notifications       │
│  • Face Embeddings   │    │  • Biometric Verification    │
└──────────────────────┘    └──────────┬───────────────────┘
                                       │
          ┌────────────────────────────┤
          │                            │
          ▼                            ▼
┌──────────────────────┐    ┌──────────────────────────────┐
│  Search Service      │    │    Verify Service             │
│  (Python FastAPI)    │    │  (Python FastAPI)             │
│  Port: 8000          │    │  Port: 8000 (same host)      │
│                      │    │                              │
│  • Geospatial Search │    │  • Face Detection            │
│  • Haversine Calc    │    │  • DeepFace/Facenet          │
│  • In-Memory Cache   │    │  • Cosine Similarity         │
│  • 3 Search Modes    │    │  • Biometric Verification    │
└──────────────────────┘    └──────────────────────────────┘
```

**Communication Protocols:**
- **Synchronous**: Spring WebClient ↔ Python FastAPI (HTTP)
- **Asynchronous**: Redis Pub/Sub for real-time notifications
- **Inter-service**: JWT-secured HTTP calls between microservices

---

## 🛠 Tech Stack

| Category                | Technology                                             |
|-------------------------|--------------------------------------------------------|
| **Languages**           | Java 17, Python 3.10+                                  |
| **Frameworks**          | Spring Boot 3.5.3 / 3.4.3, FastAPI 0.115+              |
| **Database**            | PostgreSQL + PostGIS, Hibernate JPA, Flyway            |
| **Cache/Messaging**     | Redis (Pub/Sub + Template)                             |
| **Security**            | JWT (jjwt + Auth0), OAuth2 Resource Server, BCrypt     |
| **Payments**            | Razorpay Java SDK v1.4.3                               |
| **Biometrics**          | DeepFace (Facenet), RetinaFace, OpenCV                 |
| **Spatial**             | JTS (Java Topology Suite), Shapely (Python), Haversine |
| **Notifications**       | Firebase Admin SDK, JavaMail (SMTP)                    |
| **API Documentation**   | Swagger UI / OpenAPI (springdoc-openapi)               |
| **Build Tools**         | Maven (Java), pip (Python)                             |
| **Async**               | `@Async` with ThreadPoolTaskExecutor, WebFlux          |

---

## 📦 Microservices Breakdown

### 1. SMR Auth Service — Port `8081`

**Purpose**: Identity & access management, user registration, vehicle registration.

**Location**: `smr/`

**Key Features:**
- **User Registration** with email + password, OTP verification via email/console
- **JWT-based Authentication** (Login, Token Generation, Validation)
- **Password Reset Flow** (Forgot → OTP → Reset)
- **Profile Completion** with face embedding extraction (calls Verify Service)
- **Vehicle Registration** with license plate uniqueness enforcement
- **CORS Support** for mobile/web clients
- **Swagger API Docs** at `/swagger-ui.html`

**Core Components:**
| Component       | File                                      | Responsibility                      |
|-----------------|-------------------------------------------|-------------------------------------|
| Authcontroller  | `controller/Authcontroller.java`          | REST endpoints for auth             |
| Authservice     | `service/Authservice.java`                | Business logic, OTP store, login    |
| JWTservice      | `Security/JWTservice.java`                | JJWT token generation & validation  |
| JwtProvider     | `Security/JwtProvider.java`               | Auth0 JWT token generation          |
| JWTfilter       | `Security/JWTfilter.java`                 | Once-per-request JWT filter         |
| SecurityConfig  | `Security/SecurityConfig.java`            | Spring Security + CORS config       |
| EmailService    | `Security/EmailService.java`              | Async email sending via SMTP        |
| Vehiclecontroller| `controller/Vehiclecontroller.java`       | Vehicle CRUD endpoints              |
| Vehicleservice  | `service/Vehicleservice.java`             | Vehicle registration logic          |

---

### 2. Ride Service — Port `8082`

**Purpose**: Core ride lifecycle management, booking, payments, notifications.

**Location**: `ride/`

**Key Features:**
- **Ride Creation** by drivers with start/end coordinates, seat fare, vehicle info
- **Booking Request/Response** flow (passenger requests → driver accepts/rejects)
- **Biometric Trip Start** (mutual face verification of driver & passenger via Verify Service)
- **Dynamic Fare Calculation** with deviation threshold logic
- **Razorpay Payment Integration** (COD & NETBANKING modes)
- **Webhook Handling** for asynchronous payment confirmations
- **Redis Pub/Sub Notifications** for real-time ride lifecycle events
- **Booking History** for both drivers and passengers

**Core Components:**
| Component            | File                                         | Responsibility                        |
|----------------------|----------------------------------------------|---------------------------------------|
| RideController       | `controller/RideController.java`             | REST endpoints for ride lifecycle     |
| PaymentController    | `controller/PaymentController.java`          | Webhook endpoint for payment events   |
| RideService          | `service/RideService.java`                   | Core business logic for rides/booking |
| PaymentService       | `service/PaymentService.java`                | Razorpay integration & payment ledger |
| NotificationHubService| `service/NotificationHubService.java`        | Async Redis pub/sub notifications     |
| SecurityConfig       | `config/SecurityConfig.java`                 | OAuth2 JWT resource server config     |
| RedisConfig          | `config/RedisConfig.java`                    | Redis template & channel topics       |
| AsyncConfig          | `config/AsyncConfig.java`                    | Thread pool for async notifications   |
| WebConfig            | `config/WebConfig.java`                      | WebClient for Python service calls    |

---

### 3. Search Service — Python FastAPI (Port `8000`)

**Purpose**: Geospatial ride search with 3 modes of proximity matching.

**Location**: `Search/`

**Key Features:**
- **3 Search Modes**: Nearby Pickup, Nearby Destination, Dual Anchor (exact route match)
- **Haversine Distance Calculation** for accurate great-circle distance
- **In-Memory Spatial Cache** using Shapely LineString geometry
- **Seat Availability Filtering** — automatically excludes full rides
- **Sorted Results** by proximity distance

**Search Modes:**
| Mode    | Endpoint                    | Description                                      |
|---------|-----------------------------|--------------------------------------------------|
| Mode 1  | `/api/rides/nearby-pickup`  | Find rides where pickup is near passenger        |
| Mode 2  | `/api/rides/nearby-destination` | Find rides where drop-off is near destination |
| Mode 3  | `/api/rides/search-exact`   | Both pickup AND drop-off within specified radii  |

---

### 4. Verify Service — Python FastAPI

**Purpose**: Biometric face verification using deep learning.

**Location**: `Verify/`

**Key Features:**
- **Face Embedding Extraction** (`/verify/extract`) — converts profile photo to 128-dim vector
- **Face Comparison** (`/verify/compare`) — cosine similarity between stored & live selfie
- **Facenet Model** with RetinaFace detector for high-accuracy face detection
- **Cosine Distance Metric** with configurable threshold (0.40)

**Verification Flow:**
```
1. Registration: User uploads profile photo → Facenet extracts embedding → stored in DB
2. Trip Start: Driver & Passenger take live selfies → embedding compared with stored → mutual verification
```

---

## 🔄 System Flow & Ride Lifecycle

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      RIDE LIFECYCLE STATE MACHINE                       │
└─────────────────────────────────────────────────────────────────────────┘

  REGISTER ──► VERIFY ──► COMPLETE_PROFILE ──► CREATE_RIDE
                                                    │
                                                    ▼
                                            BOOKING_REQUEST
                                                    │
                                                    ▼
                                      ┌─── ACCEPT ───┐ REJECT ──► END
                                      │              │
                                      ▼              │
                                  CONFIRMED          │
                                      │              │
                                      ▼              │
                              BIOMETRIC_ONBOARD      │
                              (Driver + Passenger)   │
                                      │              │
                                      ▼              │
                                  ONBOARDED          │
                                      │              │
                                      ▼              │
                              RIDE_COMPLETE          │
                              (Fare Calculation)     │
                                      │              │
                                      ▼              │
                            AWAITING_SETTLEMENT       │
                              │              │       │
                              ▼              ▼       │
                            COD          NETBANKING  │
                              │              │       │
                              ▼              ▼       │
                          SETTLE & CLOSE ◄───────────┘
                              │
                              ▼
                          COMPLETED
```

**Detailed Step-by-Step Flow:**

1. **User Registration** → OTP verification via email/console
2. **Profile Completion** → Upload face photo → embedding extracted by Verify Service
3. **Vehicle Registration** → Add vehicle with license plate, model, color
4. **Ride Creation** → Driver posts ride with route, seats, fare
5. **Search & Discovery** → Passengers search via 3 geospatial modes
6. **Booking Request** → Passenger requests seats → Redis notification to driver
7. **Driver Response** → Accept/Reject → Redis notification to passenger
8. **Biometric Onboarding** → Both driver & passenger take live selfies → Verify Service compares
9. **Ride Completion** → Driver marks complete → fare calculated (dynamic if deviation)
10. **Payment** → COD or NETBANKING (Razorpay) → Webhook confirms
11. **Settlement** → Phase B: Payment settled → ride & booking status → COMPLETED

---

## 📡 API Endpoints

### SMR Auth Service (Port `8081`)

| Method | Endpoint                        | Description                    | Auth Required |
|--------|---------------------------------|--------------------------------|---------------|
| POST   | `/api/auth/register`            | Register new user              | ❌            |
| POST   | `/api/auth/verify`              | Verify OTP                     | ❌            |
| POST   | `/api/auth/login`               | Login & get JWT token          | ❌            |
| POST   | `/api/auth/forgot`              | Request password reset OTP     | ❌            |
| PUT    | `/api/auth/reset`               | Reset password with verified   | ❌            |
| POST   | `/api/auth/profile/complete`    | Upload profile pic & complete  | ✅            |
| GET    | `/api/auth/users/{id}/embedding`| Get user's face embedding      | ❌            |
| POST   | `/api/vehicles/add/{ownerId}`   | Register a vehicle             | ✅            |

### Ride Service (Port `8082`)

| Method | Endpoint                                  | Description                         | Auth Required |
|--------|-------------------------------------------|-------------------------------------|---------------|
| POST   | `/api/rides/create`                       | Create a new ride                   | ✅            |
| POST   | `/api/rides/{rideId}/request-book`        | Request booking on a ride           | ✅            |
| PUT    | `/api/rides/bookings/{bookingId}/respond` | Driver accepts/rejects booking      | ✅            |
| POST   | `/api/rides/start/{bookingId}`            | Biometric verification (mutual)     | ✅            |
| PUT    | `/api/rides/{rideId}/complete`            | Complete ride & calculate fare      | ✅            |
| POST   | `/api/rides/{rideId}/settle-cash`         | Settle cash payment (Phase B)       | ✅            |
| GET    | `/api/rides/bookings/{user}`              | Get booking history for user        | ✅            |
| POST   | `/api/payments/webhook/mock-bank`         | Razorpay webhook handler            | ❌            |

### Search Service (Port `8000`)

| Method | Endpoint                          | Description                    |
|--------|-----------------------------------|--------------------------------|
| POST   | `/api/rides/nearby-pickup`        | Search by pickup proximity     |
| POST   | `/api/rides/nearby-destination`   | Search by destination proximity|
| POST   | `/api/rides/search-exact`         | Search by both anchors         |

### Verify Service

| Method | Endpoint              | Description                          |
|--------|-----------------------|--------------------------------------|
| POST   | `/verify/extract`     | Extract face embedding from image    |
| POST   | `/verify/compare`     | Compare live selfie with stored vector|

---

## 🗄 Database Schema

### `users` Table
| Column         | Type         | Description                        |
|----------------|--------------|------------------------------------|
| user_id        | UUID (PK)    | Primary key                        |
| name           | VARCHAR      | User's full name                   |
| email          | VARCHAR      | Unique email address               |
| phone          | VARCHAR      | Phone number (E.164 format)        |
| password       | VARCHAR      | BCrypt-encrypted password          |
| is_verified    | BOOLEAN      | Email verification flag            |
| gov_id_url     | VARCHAR      | Government ID document URL         |
| profile_status | VARCHAR      | Profile completion status          |
| created_at     | TIMESTAMP    | Account creation timestamp         |
| updated_at     | TIMESTAMP    | Last update timestamp              |

### `user_face_embeddings` Table
| Column         | Type         | Description                        |
|----------------|--------------|------------------------------------|
| user_id        | UUID (FK)    | References users.user_id           |
| embedding_value| DOUBLE       | Face embedding vector values       |

### `vehicles` Table
| Column              | Type         | Description                        |
|---------------------|--------------|------------------------------------|
| id                  | UUID (PK)    | Primary key                        |
| license_plate_number| VARCHAR      | Unique license plate (UNIQUE)      |
| model               | VARCHAR      | Vehicle model (e.g., "Splendor")   |
| color               | VARCHAR      | Vehicle color                      |
| is_verified         | BOOLEAN      | Verification flag                  |
| user_id             | UUID (FK)    | References users.user_id           |
| created_at          | TIMESTAMP    | Creation timestamp                 |
| updated_at          | TIMESTAMP    | Last update timestamp              |

### `ride` Table
| Column                     | Type         | Description                              |
|----------------------------|--------------|------------------------------------------|
| id                         | UUID (PK)    | Primary key                              |
| driver                     | UUID         | Driver's user ID                         |
| vehicle                    | UUID         | Vehicle ID                               |
| seat_fare                  | DOUBLE       | Fare per seat                            |
| start_latitude             | DOUBLE       | Pickup latitude                          |
| start_longitude            | DOUBLE       | Pickup longitude                         |
| end_latitude               | DOUBLE       | Drop-off latitude                        |
| end_longitude              | DOUBLE       | Drop-off longitude                       |
| seats                      | INTEGER      | Available seats                          |
| depart                     | TIMESTAMP    | Departure time                           |
| status                     | ENUM         | CREATED, ACTIVE, COMPLETED, CANCELLED, PENDING, AWAITING_SETTLEMENT |
| deviation_threshold_exceeded| BOOLEAN     | Route deviation flag                     |

### `bookings` Table
| Column              | Type         | Description                              |
|---------------------|--------------|------------------------------------------|
| id                  | UUID (PK)    | Primary key                              |
| ride_id             | UUID (FK)    | References ride.id                       |
| passenger_id        | UUID         | Passenger's user ID                      |
| seats_booked        | INTEGER      | Number of seats booked                   |
| total_paid          | DOUBLE       | Total fare amount                        |
| status              | ENUM         | PENDING, CONFIRMED, REJECTED, EXPIRED, ONBOARDED, CANCELLED, COMPLETED |
| driver_verified     | BOOLEAN      | Driver biometric verified                |
| passenger_verified  | BOOLEAN      | Passenger biometric verified             |
| created_at          | TIMESTAMP    | Booking creation timestamp               |

### `payments` Table
| Column              | Type         | Description                              |
|---------------------|--------------|------------------------------------------|
| id                  | UUID (PK)    | Primary key                              |
| ride_id             | UUID (FK)    | References ride.id (UNIQUE per ride)     |
| passenger_id        | UUID         | Passenger's user ID                      |
| amount              | DECIMAL(10,2)| Payment amount                           |
| payment_mode        | ENUM         | COD, NETBANKING                          |
| status              | ENUM         | PENDING, SUCCESS, FAILED                 |
| razorpay_order_id   | VARCHAR      | Razorpay order session ID                |
| created_at          | TIMESTAMP    | Payment creation timestamp               |
| updated_at          | TIMESTAMP    | Last update timestamp                    |

---

## 🔐 Security & Authentication

### JWT Token Flow
```
1. User registers → receives OTP → verifies → account activated
2. User logs in → receives JWT token (24-hour expiry)
3. Token is sent via `Authorization: Bearer <token>` header
4. Ride Service validates token via OAuth2 Resource Server (HMAC-SHA256)
5. SMR Auth Service validates token via JJWT filter
```

### Token Details
- **Algorithm**: HMAC-SHA256
- **Secret Key**: 256-bit key shared across services
- **Claims**: `sub` (email), `userId`, `name`, `iat`, `exp`
- **Expiry**: 24 hours (configurable via `app.security.jwt.expiration-ms`)

### Security Features
- **BCrypt** password hashing
- **OTP-based** email verification (2-minute expiry)
- **JWT validation** on every authenticated request
- **OAuth2 Resource Server** configuration
- **CORS** restricted to specific origins
- **Stateless sessions** (no HTTP session storage)

---

## 📨 Real-Time Notifications

Built on **Redis Pub/Sub** with async thread pool execution.

**Channel**: `ride:lifecycle:events`

**Notification Types:**
| Type                | Trigger                           | Recipient    |
|---------------------|-----------------------------------|--------------|
| BOOKING_REQUEST     | Passenger requests booking        | Driver       |
| BOOKING_ACCEPTED    | Driver accepts booking            | Passenger    |
| BOOKING_REJECTED    | Driver rejects booking            | Passenger    |
| RIDE_STARTED        | Both biometrics verified          | Passenger    |
| PAYMENT_DUE         | Ride completed, payment pending   | Passenger    |
| RIDE_COMPLETED      | Payment settled successfully      | Passenger    |

**Architecture:**
```
RideService → NotificationHubService (async) → RedisTemplate.convertAndSend()
                                                      ↓
                                              Redis Pub/Sub
                                                      ↓
                                              Subscribers (mobile/web)
```

**Thread Pool Configuration:**
- Core Pool: 5 threads
- Max Pool: 25 threads
- Queue Capacity: 200
- Thread Prefix: `NotifyWorker-`

---

## 🧬 Biometric Face Verification

**Pipeline**: 
```
Profile Upload → Face Detection (RetinaFace) → Embedding (Facenet) → DB Storage
                                      ↓
Live Selfie → Face Detection → Embedding → Cosine Similarity → Match (≤0.40)
```

**Technology**: DeepFace library with Facenet model, RetinaFace detector backend.

**Threshold**: Cosine distance ≤ 0.40 is considered a match.

**Integration**: 
- SMR Auth Service calls `/verify/extract` during profile completion
- Ride Service calls `/verify/compare` during trip start for both driver & passenger
- Mutual verification required before trip can begin (ONBOARDED status)

---

## 🌍 Geospatial Search Engine

**3 Search Modes:**
1. **Nearby Pickup** — Finds rides starting within N km of passenger's current location
2. **Nearby Destination** — Finds rides ending within N km of passenger's target destination
3. **Dual Anchor** — Finds rides where BOTH pickup AND drop-off are within specified radii

**Algorithm**: Haversine formula for great-circle distance calculation

**Cache**: In-memory Python dictionary (`ROUTE_INDEX_CACHE`) with Shapely LineString geometry

**Cache Operations:**
- `index_driver_route(ride_id, p_lat, p_lon, d_lat, d_lon, seats)` — Add/update route
- `update_ride_cache_seats(ride_id, seats)` — Update available seats
- `remove_ride_from_cache(ride_id)` — Remove ride on cancellation
- `clear_cache()` — Purge entire cache

---

## 💳 Payment Integration

**Provider**: Razorpay (Test Mode)

**Payment Modes:**
- **COD (Cash on Delivery)** — Driver collects cash, clicks settle
- **NETBANKING** — Razorpay order creation → client-side payment → webhook confirmation

**Flow:**
```
1. Ride completed → fare calculated
2. For NETBANKING: Razorpay order created with amount in paise (×100)
3. Client processes payment with Razorpay SDK
4. Razorpay sends webhook → /api/payments/webhook/mock-bank
5. Webhook triggers Phase B: payment settlement → ride/booking → COMPLETED
```

**Keys**: Test keys configured in `application.properties`:
- Key ID: `rzp_test_...`
- Key Secret: Configured in properties

---

## ⚙️ Configuration

### SMR Auth Service (`smr/src/main/resources/application.properties`)
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/smr
spring.datasource.username=postgres
spring.datasource.password=****
spring.jpa.hibernate.ddl-auto=update
app.security.jwt.secret-key=your-256-bit-secret
app.security.jwt.expiration-ms=86400000
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

### Ride Service (`ride/src/main/resources/application.properties`)
```properties
server.port=8082
spring.datasource.url=jdbc:postgresql://localhost:5432/smr
spring.datasource.username=postgres
spring.datasource.password=****
spring.jpa.hibernate.ddl-auto=update
razorpay.key.id=rzp_test_xxxx
razorpay.key.secret=your-secret
spring.security.oauth2.resourceserver.jwt.secret-key-string=your-256-bit-secret
```

### Search Service (`Search/requirements.txt`)
```
fastapi>=0.115.0
uvicorn>=0.35.0
pydantic>=2.11.0
shapely>=2.1.0
```

### Verify Service (`Verify/requirements.txt`)
```
fastapi>=0.110.0
uvicorn>=0.28.0
pydantic>=2.6.0
python-multipart>=0.0.9
opencv-python-headless>=4.9.0.80
numpy>=1.24.0
deepface>=0.0.93
tf-keras>=2.16.0
```

---

## 🚀 Running the Project

### Prerequisites
- Java 17+
- Maven
- Python 3.10+
- PostgreSQL 14+ with PostGIS extension
- Redis Server
- Razorpay Test Account (for payments)

### Step 1: Database Setup
```bash
# Create PostgreSQL database
createdb smr

# Enable PostGIS extension (optional, for spatial queries)
psql -d smr -c "CREATE EXTENSION IF NOT EXISTS postgis;"
```

### Step 2: Start Redis
```bash
redis-server
```

### Step 3: Run Python Microservices
```bash
# Install dependencies
cd Search && pip install -r requirements.txt
cd Verify && pip install -r requirements.txt

# Start Search Service (Background)
cd Search && uvicorn main:app --host 0.0.0.0 --port 8000

# Start Verify Service (Background)
cd Verify && uvicorn main:app --host 0.0.0.0 --port 8000
```

### Step 4: Run SMR Auth Service
```bash
cd smr
./mvnw spring-boot:run
# Starts on port 8081
# Swagger: http://localhost:8081/swagger-ui.html
```

### Step 5: Run Ride Service
```bash
cd ride
./mvnw spring-boot:run
# Starts on port 8082
# Swagger: http://localhost:8082/swagger-ui.html
```

### Step 6: Verify Everything
```bash
# Health check
curl http://localhost:8081/actuator/health
curl http://localhost:8000/docs  # FastAPI auto-docs
```

---

## 📁 Project Structure

```
Backend/
├── ride/                           # Ride Service (Spring Boot)
│   ├── pom.xml
│   └── src/main/java/com/smr/ride/
│       ├── RideApplication.java
│       ├── config/
│       │   ├── AsyncConfig.java
│       │   ├── RedisConfig.java
│       │   ├── SecurityConfig.java
│       │   └── WebConfig.java
│       ├── controller/
│       │   ├── PaymentController.java
│       │   └── RideController.java
│       ├── dto/
│       │   ├── bookingDTO.java
│       │   ├── RideBookRequestDTO.java
│       │   ├── RidecreateDTO.java
│       │   └── RideResponseDTO.java
│       ├── entity/
│       │   ├── Booking.java
│       │   ├── Payment.java
│       │   └── Ride.java
│       ├── repo/
│       │   ├── BookingRepository.java
│       │   ├── PaymentRepository.java
│       │   └── RideRepository.java
│       └── service/
│           ├── NotificationHubService.java
│           ├── PaymentService.java
│           └── RideService.java
│
├── smr/                            # Auth Service (Spring Boot)
│   ├── pom.xml
│   └── src/main/java/com/spring/smr/
│       ├── SmrApplication.java
│       ├── controller/
│       │   ├── Authcontroller.java
│       │   └── Vehiclecontroller.java
│       ├── dto/
│       │   ├── AuthResponse.java
│       │   ├── LoginDTO.java
│       │   ├── ProfileDTO.java
│       │   ├── RegisterDTO.java
│       │   ├── ResetDTO.java
│       │   ├── updateVehicelDTO.java
│       │   ├── VehicleDTO.java
│       │   └── VerifyRequestDTO.java
│       ├── entity/
│       │   ├── Users.java
│       │   └── Vehicles.java
│       ├── repo/
│       │   ├── UsersRepository.java
│       │   └── VehiclesRepository.java
│       ├── Security/
│       │   ├── EmailService.java
│       │   ├── JWTfilter.java
│       │   ├── JwtProvider.java
│       │   ├── JWTservice.java
│       │   ├── SecurityConfig.java
│       │   └── WebClientConfig.java
│       └── service/
│           ├── Authservice.java
│           └── Vehicleservice.java
│
├── Search/                         # Geospatial Search Service (Python FastAPI)
│   ├── main.py
│   ├── requirements.txt
│   ├── schemas.py
│   ├── db/
│   │   └── models.py
│   └── services/
│       ├── search_service.py
│       └── spatial_cache.py
│
├── Verify/                         # Biometric Verification Service (Python FastAPI)
│   ├── main.py
│   ├── requirements.txt
│   └── services/
│       └── matrix_engine.py
│
└── README.md
```

---

## 🧪 Testing

```bash
# Run Spring Boot tests
cd smr && ./mvnw test
cd ride && ./mvnw test

# Python tests (if available)
cd Search && python -m pytest
cd Verify && python -m pytest
```

---

## 📚 API Documentation

Once services are running, access Swagger UI:

| Service     | URL                                      |
|-------------|------------------------------------------|
| Auth        | http://localhost:8081/swagger-ui.html    |
| Ride        | http://localhost:8082/swagger-ui.html    |
| Search      | http://localhost:8000/docs               |
| Verify      | http://localhost:8000/docs               |

---

## 🏁 Conclusion

This backend provides a **complete, production-ready ride-sharing platform** with:

- ✅ **Secure authentication** with JWT + OTP verification
- ✅ **Biometric identity verification** using deep learning (Facenet)
- ✅ **Geospatial ride search** with 3 modes of proximity matching
- ✅ **Real-time notifications** via Redis Pub/Sub
- ✅ **Payment integration** with Razorpay (COD & NETBANKING)
- ✅ **Microservices architecture** for scalability and maintainability
- ✅ **Swagger documentation** for all services
- ✅ **Polyglot tech stack** leveraging Java + Python strengths

---

*Built with ❤️ using Spring Boot, FastAPI, PostgreSQL, Redis, and Deep Learning*
