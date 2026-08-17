# SMR Notification Service

> A high-performance, real-time notification service built with Spring Boot and WebSocket technology for instant message delivery.

## 📋 Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Usage](#api-usage)
- [Key Components](#key-components)
- [Authentication Flow](#authentication-flow)
- [Message Flow](#message-flow)
- [Deployment](#deployment)
- [Future Enhancements](#future-enhancements)
- [Troubleshooting](#troubleshooting)

---

## Overview

The **SMR Notification Service** is a lightweight, scalable notification system that:

✅ Delivers real-time notifications via **WebSocket (STOMP)**  
✅ Validates clients using **JWT (RSA) authentication**  
✅ Subscribes to events from **Redis Pub/Sub**  
✅ Maintains **per-user session management**  
✅ Supports **flexible, extensible notification payloads**  
✅ Detects **online/offline status** automatically  

**Perfect for**: Real-time alerts, order updates, chat notifications, push notifications, and event streaming.

---

## Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Framework** | Spring Boot | 4.1.0 | Core application framework |
| **Runtime** | Java | 17+ | Programming language |
| **Real-time Protocol** | WebSocket + STOMP | Latest | Bidirectional messaging |
| **Message Queue** | Redis Pub/Sub | 6.0+ | Event streaming |
| **Authentication** | JWT (RSA) | jjwt 0.11.5 | Token-based security |
| **Security** | Spring Security | 4.1.0 | Authorization & filtering |
| **Serialization** | Jackson | Latest | JSON serialization |
| **Utilities** | Lombok | Latest | Boilerplate reduction |
| **Build Tool** | Maven | 3.6+ | Project building |

---

## Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────┐
│                   Client Browsers/Apps              │
│        (WebSocket STOMP Connection)                 │
└────────────────────────┬────────────────────────────┘
                         │
        ╔════════════════▼═════════════════════╗
        ║   WebSocket Channel (STOMP)          ║
        ║  - JWT Validation                    ║
        ║  - Session Management                ║
        ╚════════════════╤═════════════════════╝
                         │
        ┌────────────────▼─────────────────┐
        │  WebSocket Event Listeners        │
        │  (Session Connect/Disconnect)     │
        └────────────────┬─────────────────┘
                         │
    ╔════════════════════▼══════════════════╗
    ║  Session Manager (In-Memory)          ║
    ║  User → Session Mapping               ║
    ║  sendNotification() →                 ║
    ║  SimpMessagingTemplate                ║
    ╚════════════════════╤══════════════════╝
                         │
        ┌────────────────▼─────────────────┐
        │  Redis Pub/Sub Listener           │
        │  (Topic: smr:notifications)       │
        └────────────────┬─────────────────┘
                         │
        ╔════════════════▼═════════════════════╗
        ║   Message Deserialization            ║
        ║   (NotificationEvent Object)         ║
        ║   Error Handling & Logging           ║
        ╚════════════════╤═════════════════════╝
                         │
        ┌────────────────▼─────────────────┐
        │  Notification Service             │
        │  Business Logic Processing        │
        └────────────────┬─────────────────┘
                         │
        ┌────────────────▼─────────────────┐
        │  Deliver to User's WebSocket      │
        │  or Skip if Offline               │
        └──────────────────────────────────┘
```

---

## Project Structure

```
notify/
├── README.md                          # This file
├── pom.xml                           # Maven configuration
├── HELP.md                           # Spring Boot help
├── mvnw / mvnw.cmd                   # Maven wrapper
│
├── src/
│   ├── main/
│   │   ├── java/com/smr/notify/
│   │   │   ├── NotifyApplication.java         # Entry point
│   │   │   │
│   │   │   ├── config/
│   │   │   │   └── RedisConfig.java           # Redis setup
│   │   │   │
│   │   │   ├── controller/                    # REST endpoints (TODO: Add here)
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   └── NotificationEvent.java     # Notification data model
│   │   │   │
│   │   │   ├── listener/
│   │   │   │   └── RedisEventListener.java    # Handles Redis messages
│   │   │   │
│   │   │   ├── security/
│   │   │   │   ├── JWTservice.java            # JWT validation & claims
│   │   │   │   └── SecurityConfig.java        # Spring Security config
│   │   │   │
│   │   │   ├── service/
│   │   │   │   └── NotificationService.java   # Business logic
│   │   │   │
│   │   │   └── websocket/
│   │   │       ├── WebSocketConfig.java       # STOMP broker config
│   │   │       ├── WebSocketChannelInterceptor.java  # JWT validation
│   │   │       ├── WebSocketEventListener.java       # Connection lifecycle
│   │   │       ├── WebSocketSessionManager.java      # Session tracking
│   │   │       └── WebSocketUserPrincipal.java       # User identity
│   │   │
│   │   └── resources/
│   │       ├── application.properties         # App configuration
│   │       ├── Keys/
│   │       │   └── public.key                 # RSA public key (from issuer)
│   │       ├── static/                        # Static files
│   │       └── templates/                     # Thymeleaf templates
│   │
│   └── test/
│       └── java/com/smr/notify/
│           └── NotifyApplicationTests.java    # Unit tests
│
└── target/                           # Build output
```

---

## Prerequisites

Ensure you have the following installed:

- **Java 17+** - [Download](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6+** - [Download](https://maven.apache.org/download.cgi)
- **Redis 6.0+** - [Download](https://redis.io/download)
- **Git** (optional)

### Verify Installation

```bash
java -version
mvn -version
redis-cli --version
```

---

## Installation & Setup

### 1. Clone the Repository

```bash
git clone <your-repo-url>
cd notify
```

### 2. Set Up Redis

**Option A: Local Installation**
```bash
# Start Redis server
redis-server

# Verify connection (in another terminal)
redis-cli ping
# Expected output: PONG
```

**Option B: Docker Container**
```bash
docker run -d -p 6379:6379 redis:7-alpine
redis-cli ping
```

### 3. Configure RSA Public Key

Place your **RSA public key** in: `src/main/resources/keys/public.key`

The file should be in PEM format:
```
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBIjANBgkqhkiG9w0B...
-----END PUBLIC KEY-----
```

**To generate a test key pair** (for development only):

```bash
# Generate private key
openssl genrsa -out private.key 2048

# Generate public key from private key
openssl rsa -in private.key -pubout -out public.key
```

### 4. Build the Project

```bash
# Using Maven
mvn clean install

# Or using Maven wrapper
./mvnw clean install
```

---

## Configuration

### `application.properties`

Located at: `src/main/resources/application.properties`

```properties
# Application name
spring.application.name=notify

# Redis configuration
spring.data.redis.host=localhost        # Redis server host
spring.data.redis.port=6379             # Redis server port
spring.data.redis.timeout=2000ms        # Connection timeout (optional)
spring.data.redis.password=              # If Redis requires auth (optional)
```

### Environment Variables (Optional)

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=your-password
```

### Logging Configuration (Optional)

Add to `application.properties`:

```properties
logging.level.com.smr.notify=DEBUG
logging.level.org.springframework=INFO
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

---

## Running the Application

### Start the Application

```bash
# Using Maven
mvn spring-boot:run

# Or run the JAR
java -jar target/notify-0.0.1-SNAPSHOT.jar
```

### Expected Output

```
2026-08-17 10:30:00 INFO NotifyApplication - Starting NotifyApplication
2026-08-17 10:30:01 INFO NotifyApplication - Started NotifyApplication in 2.3 seconds
```

### Access Points

- **WebSocket Endpoint**: `ws://localhost:8080/ws`
- **Health Check**: `http://localhost:8080/actuator/health` (if enabled)

---

## API Usage

### WebSocket Connection (Client-Side)

#### JavaScript Example

```javascript
// 1. Connect to WebSocket
const socket = new WebSocket('ws://localhost:8080/ws');

// 2. When connected, send STOMP CONNECT frame
socket.onopen = () => {
  const connectFrame = `
CONNECT
Authorization:Bearer YOUR_JWT_TOKEN_HERE
accept-version:1.0,1.1,1.2

\0
`;
  socket.send(connectFrame);
};

// 3. Listen for CONNECTED response
socket.onmessage = (event) => {
  console.log('Message:', event.data);
  
  // If CONNECTED, subscribe to notifications
  if (event.data.includes('CONNECTED')) {
    const subscribeFrame = `
SUBSCRIBE
id:sub-1
destination:/user/queue/notifications

\0
`;
    socket.send(subscribeFrame);
  }
};

// 4. Receive notifications
socket.onmessage = (event) => {
  const notification = JSON.parse(event.data);
  console.log('Notification received:', notification);
  // Handle notification (display, store, etc.)
};

// 5. Disconnect
socket.close();
```

#### Using `stomp-js` Library

```javascript
const client = new StompJs.Client({
  brokerURL: 'ws://localhost:8080/ws',
  headers: {
    Authorization: 'Bearer YOUR_JWT_TOKEN'
  }
});

client.onConnect = () => {
  console.log('Connected to WebSocket');
  
  // Subscribe to notifications
  client.subscribe('/user/queue/notifications', (message) => {
    const notification = JSON.parse(message.body);
    console.log('Received:', notification);
  });
};

client.activate();
```

---

### Publishing Notifications (Server-Side)

#### Using Redis CLI

```bash
redis-cli

# Publish notification
PUBLISH smr:notifications '{"notificationId":"550e8400-e29b-41d4-a716-446655440000","senderId":"550e8400-e29b-41d4-a716-446655440001","receiverId":"550e8400-e29b-41d4-a716-446655440002","type":"ORDER_UPDATE","payload":{"orderId":"ORD-123","status":"shipped"},"timestamp":"2026-08-17T10:30:00Z"}'
```

#### Using Python

```python
import redis
import json
from datetime import datetime
from uuid import uuid4

r = redis.Redis(host='localhost', port=6379, decode_responses=True)

notification = {
    "notificationId": str(uuid4()),
    "senderId": str(uuid4()),
    "receiverId": "550e8400-e29b-41d4-a716-446655440002",
    "type": "ORDER_UPDATE",
    "payload": {
        "orderId": "ORD-123",
        "status": "shipped",
        "trackingId": "TRACK-456"
    },
    "timestamp": datetime.utcnow().isoformat() + "Z"
}

r.publish("smr:notifications", json.dumps(notification))
print("Notification published!")
```

#### Using Node.js

```javascript
const redis = require('redis');
const { v4: uuid4 } = require('uuid');

const client = redis.createClient({ host: 'localhost', port: 6379 });

client.connect().then(() => {
  const notification = {
    notificationId: uuid4(),
    senderId: uuid4(),
    receiverId: "550e8400-e29b-41d4-a716-446655440002",
    type: "ORDER_UPDATE",
    payload: {
      orderId: "ORD-123",
      status: "shipped"
    },
    timestamp: new Date().toISOString()
  };

  client.publish('smr:notifications', JSON.stringify(notification));
  console.log('Notification published!');
});
```

---

## Key Components

### 1. **NotifyApplication.java**
Entry point for the Spring Boot application. Bootstraps the entire context.

### 2. **WebSocketConfig.java**
- Enables WebSocket message broker
- Configures STOMP endpoints
- Registers channel interceptor for JWT validation
- Sets up application destination prefixes

### 3. **WebSocketChannelInterceptor.java**
- Intercepts all STOMP frames at channel level
- Validates JWT token on CONNECT
- Extracts user ID from JWT claims
- Creates WebSocketUserPrincipal for authenticated users

### 4. **WebSocketEventListener.java**
- Listens to `SessionConnectedEvent`
- Registers user session in memory
- Maps User ID → Session ID bidirectionally
- Handles disconnection cleanup

### 5. **WebSocketSessionManager.java**
- Maintains in-memory maps of active sessions
- Maps User ID to Session ID
- Sends notifications to specific users
- Detects online/offline status

### 6. **JWTservice.java**
- Loads RSA public key from resources
- Validates JWT signatures
- Extracts user ID from claims
- Handles JWT parsing and exceptions

### 7. **RedisConfig.java**
- Sets up Redis message listener container
- Subscribes to `smr:notifications` topic
- Registers `RedisEventListener` as callback

### 8. **RedisEventListener.java**
- Implements `MessageListener` interface
- Deserializes JSON messages to `NotificationEvent`
- Delegates to `NotificationService`
- Handles parsing errors gracefully

### 9. **NotificationService.java**
- Processes incoming notifications
- Logs notification details
- Delegates to `WebSocketSessionManager` for delivery

### 10. **NotificationEvent.java**
```java
- notificationId: UUID          // Unique identifier
- senderId: UUID                // Notification sender
- receiverId: UUID              // Notification recipient
- type: String                  // Notification type/category
- payload: Map<String, Object>  // Flexible data structure
- timestamp: Instant            // Creation time
```

---

## Authentication Flow

```
1. External Service Issues JWT
   ├─ Uses Private RSA Key
   ├─ Includes userId in claims
   └─ Sends JWT to client

2. Client Connects to WebSocket
   ├─ Opens connection: ws://localhost:8080/ws
   ├─ Sends STOMP CONNECT with: Authorization: Bearer {JWT}
   └─ Waits for CONNECTED response

3. WebSocketChannelInterceptor Validates
   ├─ Intercepts CONNECT frame
   ├─ Extracts JWT from Authorization header
   ├─ Calls JWTservice.validateToken(token)
   │  ├─ Loads RSA Public Key
   │  ├─ Verifies signature
   │  └─ Returns true/false
   ├─ Calls JWTservice.extractUserId(token)
   │  └─ Gets userId claim
   └─ Sets WebSocketUserPrincipal with userId

4. Connection Established
   ├─ Spring fires SessionConnectedEvent
   ├─ WebSocketEventListener.handleWebSocketConnectListener()
   ├─ Registers user in SessionManager
   └─ User ready to receive notifications

5. Disconnection
   ├─ Spring fires SessionDisconnectEvent
   ├─ WebSocketEventListener.handleWebSocketDisconnectListener()
   ├─ Unregisters user from SessionManager
   └─ Session cleaned up
```

---

## Message Flow

### Complete End-to-End Flow

```
External System
      │
      │ Publishes to Redis
      ▼
Redis Channel (smr:notifications)
      │
      │ Message pushed to subscribers
      ▼
RedisMessageListenerContainer
      │
      │ Invokes onMessage()
      ▼
RedisEventListener
      │
      ├─ Deserializes JSON → NotificationEvent
      ├─ Handles exceptions
      │
      │ Calls processNotification()
      ▼
NotificationService
      │
      ├─ Logs notification details
      │
      │ Calls sendNotification()
      ▼
WebSocketSessionManager
      │
      ├─ Checks if user has active session
      ├─ If yes:
      │  ├─ Gets sessionId from userSessions map
      │  ├─ Calls messagingTemplate.convertAndSendToUser()
      │  ├─ Routes to /user/{userId}/queue/notifications
      │  └─ Delivers to all sessions of that user
      │
      └─ If no (user offline):
         └─ Silently skips
            (No persistence, message lost)
      │
      ▼
Client WebSocket
      │
      │ Receives STOMP MESSAGE frame
      ▼
Client Application
      │
      │ Processes notification
      └─ Display to user
```

---

## Deployment

### Docker Deployment

#### Dockerfile

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/notify-0.0.1-SNAPSHOT.jar notify.jar

ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "notify.jar"]
```

#### Build & Run

```bash
# Build Docker image
docker build -t smr-notify:latest .

# Run container
docker run -d \
  --name notify \
  -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  smr-notify:latest
```

### Kubernetes Deployment

#### deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notify-service
  labels:
    app: notify
spec:
  replicas: 3
  selector:
    matchLabels:
      app: notify
  template:
    metadata:
      labels:
        app: notify
    spec:
      containers:
      - name: notify
        image: smr-notify:latest
        ports:
        - containerPort: 8080
        env:
        - name: REDIS_HOST
          value: redis-service
        - name: REDIS_PORT
          value: "6379"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
```

```bash
kubectl apply -f deployment.yaml
```

---

## Future Enhancements

### 🔄 Short-term Improvements

- [ ] **REST API Endpoints**
  - `POST /api/notifications` - Send notification
  - `GET /api/notifications/{id}` - Fetch notification
  - `DELETE /api/notifications/{id}` - Delete notification

- [ ] **Database Persistence**
  - Store notifications in database
  - Enable offline message retrieval
  - Notification history/archive

- [ ] **Message Queue Persistence**
  - Handle message queue failures
  - Implement retry logic
  - Dead letter queue support

### 📈 Medium-term Enhancements

- [ ] **Rate Limiting**
  - Per-user rate limits
  - Backpressure handling
  - Quota management

- [ ] **Notification Filtering**
  - User preferences/subscriptions
  - Priority levels
  - Notification categories

- [ ] **Advanced Security**
  - OAuth2 integration
  - Multi-factor authentication
  - Role-based access control

- [ ] **Observability**
  - Structured logging (SLF4J)
  - Metrics (Micrometer/Prometheus)
  - Distributed tracing (Jaeger)
  - Health checks

### 🚀 Long-term Enhancements

- [ ] **High Availability**
  - Redis Cluster support
  - Database clustering
  - Load balancing
  - Failover strategies

- [ ] **Advanced Features**
  - Notification templates
  - Multi-channel delivery (SMS, Email, Push)
  - Scheduling & delayed delivery
  - Analytics & reporting

- [ ] **Developer Experience**
  - API documentation (Swagger/OpenAPI)
  - SDK libraries
  - Webhook support
  - Testing utilities

- [ ] **Performance Optimization**
  - Message batching
  - Connection pooling
  - Caching strategies
  - Compression

---

## Troubleshooting

### Issue: "Redis Connection Refused"

```
Error: Unable to connect to localhost:6379
```

**Solutions:**
```bash
# 1. Verify Redis is running
redis-cli ping

# 2. Check Redis configuration
redis-cli CONFIG GET port

# 3. Restart Redis
redis-server

# 4. Or use Docker
docker run -d -p 6379:6379 redis:7-alpine
```

### Issue: "JWT Validation Failed"

```
Error: Invalid WebSocket JWT
```

**Solutions:**
1. Verify RSA public key is in `src/main/resources/keys/public.key`
2. Check JWT format: `Bearer eyJhbGciOiJSUzI1NiIs...`
3. Ensure token is not expired
4. Validate token signature with correct public key

### Issue: "Missing Public Key File"

```
Error: Could not load RSA public key
```

**Solutions:**
```bash
# 1. Generate test key pair
openssl genrsa -out private.key 2048
openssl rsa -in private.key -pubout -out public.key

# 2. Copy public key to resources
cp public.key src/main/resources/keys/public.key

# 3. Rebuild application
mvn clean install
```

### Issue: "WebSocket Connection Timeout"

**Solutions:**
1. Check firewall: `telnet localhost 8080`
2. Verify app is running: `curl http://localhost:8080/actuator/health`
3. Check browser console for CORS errors
4. Verify endpoint URL: `ws://localhost:8080/ws` (not `http://`)

### Issue: "Notification Not Delivered"

**Solutions:**
1. Check if user is connected: Log in `WebSocketSessionManager`
2. Verify notification in Redis: `redis-cli PUBLISH smr:notifications '...'`
3. Check console logs for deserialization errors
4. Ensure receiverId matches connected user's ID

---

## License

This project is provided as-is for the SMR application.

---

## Support & Contribution

For issues, feature requests, or contributions, please contact the development team.

**Last Updated**: 2026-08-17  
**Version**: 0.0.1-SNAPSHOT
