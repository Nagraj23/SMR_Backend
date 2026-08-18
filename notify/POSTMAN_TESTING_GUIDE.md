# Postman Testing Guide - SMR Notification Service

Complete guide to test all API routes and WebSocket endpoints of the SMR Notification Service with mock data.

---

## 📋 Table of Contents

- [Setup & Prerequisites](#setup--prerequisites)
- [Authentication Setup](#authentication-setup)
- [API Testing](#api-testing)
- [WebSocket Testing](#websocket-testing)
- [Mock Data & Scenarios](#mock-data--scenarios)
- [Complete Test Collection](#complete-test-collection)
- [Troubleshooting](#troubleshooting)

---

## Setup & Prerequisites

### 1. **Installation Requirements**

Before testing, ensure the following are running:

```bash
# Required Services
✅ Java 17+ (check with: java -version)
✅ Maven (check with: mvn -v)
✅ Redis Server (localhost:6379)
✅ Firebase Service Account (firebase-service-account.json)
```

### 2. **Start the Application**

```bash
# From project root
mvn clean install
mvn spring-boot:run
```

The application starts at: `http://localhost:8080`

### 3. **Install Postman**

- Download from [postman.com](https://www.postman.com/downloads/)
- Or use [Insomnia](https://insomnia.rest/) as an alternative

### 4. **WebSocket Support in Postman**

For WebSocket testing, you can use:
- **Postman**: Limited WebSocket support (v11+)
- **Better Alternative**: Use [wscat](https://github.com/websockets/wscat) CLI tool:

```bash
# Install wscat
npm install -g wscat

# Connect to WebSocket
wscat -c ws://localhost:8080/ws
```

---

## Authentication Setup

### Understanding JWT Authentication

The SMR Notify Service uses **JWT (RS256) with RSA Public Key** validation.

**Important Notes:**
- Tokens must be generated using the private key paired with the public key in `src/main/resources/keys/public.key`
- JWT must contain a `userId` claim
- Token is passed as Bearer token in WebSocket Authorization header

### Generating Test JWT Token

Since you need to generate a valid JWT with the matching private key, here's how:

#### Option 1: Use JWT.io (For Testing Only)

1. Go to [jwt.io](https://jwt.io/)
2. **Header:**
   ```json
   {
     "alg": "RS256",
     "typ": "JWT"
   }
   ```
3. **Payload:**
   ```json
   {
     "userId": "550e8400-e29b-41d4-a716-446655440000",
     "iat": 1694000000,
     "exp": 1725600000
   }
   ```
4. **Signature**: Select RS256, paste your private key
5. Copy the generated token from the left panel

#### Option 2: Generate via Code (Recommended)

Create a Java utility class to generate tokens:

```java
// TokenGenerator.java
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class TokenGenerator {
    public static void main(String[] args) throws Exception {
        // Load private key from file
        String key = new String(Files.readAllBytes(
            Paths.get("src/main/resources/keys/private.key")
        ));
        
        key = key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        
        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(keySpec);
        
        // Generate token
        String token = Jwts.builder()
            .setSubject("1")
            .claim("userId", UUID.randomUUID().toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 3600000))
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact();
        
        System.out.println("Generated Token:");
        System.out.println(token);
    }
}
```

#### Option 3: Use Pre-generated Test Token

For quick testing, use this sample structure (replace with real token):

```
Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE2OTQwMDAwMDAsImV4cCI6MTcyNTYwMDAwMH0.{SIGNATURE}
```

---

## API Testing

### Endpoint 1: Register Device Token

#### Request Details

| Property | Value |
|----------|-------|
| **HTTP Method** | `POST` |
| **URL** | `http://localhost:8080/api/notify/device-token` |
| **Content-Type** | `application/json` |
| **Authentication** | ❌ Not Required |

#### Request Body

```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "fcmToken": "eJ4t_OlO:APA91bFxyz123456789abcdefghijklmnopqrstuvwxyz"
}
```

#### Example Mock Data (Multiple Users)

**User 1:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "fcmToken": "cKwpP9lZ:APA91bGhijk567890abcdefghijklmnopqrstuvwxyz"
}
```

**User 2:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440002",
  "fcmToken": "dRxqQ0mA:APA91bHijkl890123abcdefghijklmnopqrstuvwxyz"
}
```

#### Postman Setup

1. Create a new **POST** request
2. Set URL: `http://localhost:8080/api/notify/device-token`
3. Go to **Body** tab → Select **raw** → **JSON**
4. Paste the request body from above
5. Click **Send**

#### Expected Response

```
Status: 200 OK
Body: (empty)
```

#### Validation

- ✅ Check Redis for stored token: `redis-cli`
  ```bash
  redis-cli
  > keys *
  > get "550e8400-e29b-41d4-a716-446655440001"
  ```
- ✅ Application logs should show registration confirmation

---

## WebSocket Testing

### WebSocket Endpoint Setup

| Property | Value |
|----------|-------|
| **Protocol** | `ws://` (or `wss://` for HTTPS) |
| **URL** | `ws://localhost:8080/ws` |
| **Protocol** | STOMP |
| **Authentication** | JWT Bearer Token Required |

### Using wscat for WebSocket Testing

#### Step 1: Connect to WebSocket

```bash
wscat -c ws://localhost:8080/ws
```

#### Step 2: Send STOMP CONNECT Frame

After connection, send:

```
CONNECT
Authorization: Bearer YOUR_JWT_TOKEN_HERE
accept-version:1.0,1.1,1.2
heart-beat:0,0

```

#### Expected Response

```
CONNECTED
version:1.2
heart-beat:0,0
server:RabbitMQ/3.8.0
```

#### Step 3: Subscribe to Notification Queue

```
SUBSCRIBE
id:0
destination:/user/550e8400-e29b-41d4-a716-446655440001/queue/notifications

```

#### Expected Response

```
RECEIPT
id:0
```

### Simulating Notifications via Redis CLI

#### Terminal 1: Subscriber (WebSocket Connected)

```bash
wscat -c ws://localhost:8080/ws
# Then send CONNECT and SUBSCRIBE frames as shown above
```

#### Terminal 2: Publisher (Publish to Redis)

```bash
redis-cli
> PUBLISH smr:notifications '{"notificationId":"550e8400-e29b-41d4-a716-446655440099","senderId":"550e8400-e29b-41d4-a716-446655440003","receiverId":"550e8400-e29b-41d4-a716-446655440001","type":"BOOKING_REQUEST","payload":{"rideId":"RIDE-001","pickupLocation":"Central Station","dropoffLocation":"Airport","fare":25.50},"timestamp":"2024-08-18T10:30:00Z"}'
```

#### Expected Result

User connected via WebSocket receives:

```
MESSAGE
destination:/user/550e8400-e29b-41d4-a716-446655440001/queue/notifications
message-id:ID:notif:1:1:1:1
content-length:...

{"notificationId":"550e8400-e29b-41d4-a716-446655440099","senderId":"550e8400-e29b-41d4-a716-446655440003","receiverId":"550e8400-e29b-41d4-a716-446655440001","type":"BOOKING_REQUEST","payload":{"rideId":"RIDE-001","pickupLocation":"Central Station","dropoffLocation":"Airport","fare":25.50},"timestamp":"2024-08-18T10:30:00Z"}
```

---

## Mock Data & Scenarios

### Scenario 1: New Booking Request

**Notification Event:**
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440099",
  "senderId": "550e8400-e29b-41d4-a716-446655440003",
  "receiverId": "550e8400-e29b-41d4-a716-446655440001",
  "type": "BOOKING_REQUEST",
  "payload": {
    "rideId": "RIDE-001",
    "pickupLocation": "Central Station",
    "dropoffLocation": "Airport",
    "fare": 25.50,
    "distance": "15 km",
    "estimatedTime": "25 minutes"
  },
  "timestamp": "2024-08-18T10:30:00Z"
}
```

**Redis Command:**
```bash
redis-cli PUBLISH smr:notifications '{"notificationId":"550e8400-e29b-41d4-a716-446655440099","senderId":"550e8400-e29b-41d4-a716-446655440003","receiverId":"550e8400-e29b-41d4-a716-446655440001","type":"BOOKING_REQUEST","payload":{"rideId":"RIDE-001","pickupLocation":"Central Station","dropoffLocation":"Airport","fare":25.50,"distance":"15 km","estimatedTime":"25 minutes"},"timestamp":"2024-08-18T10:30:00Z"}'
```

---

### Scenario 2: Booking Accepted

**Notification Event:**
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440100",
  "senderId": "550e8400-e29b-41d4-a716-446655440001",
  "receiverId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "BOOKING_ACCEPTED",
  "payload": {
    "rideId": "RIDE-001",
    "driverName": "John Smith",
    "vehicleModel": "Toyota Prius",
    "licensePlate": "ABC-1234",
    "etaPickup": "5 minutes"
  },
  "timestamp": "2024-08-18T10:35:00Z"
}
```

**Redis Command:**
```bash
redis-cli PUBLISH smr:notifications '{"notificationId":"550e8400-e29b-41d4-a716-446655440100","senderId":"550e8400-e29b-41d4-a716-446655440001","receiverId":"550e8400-e29b-41d4-a716-446655440003","type":"BOOKING_ACCEPTED","payload":{"rideId":"RIDE-001","driverName":"John Smith","vehicleModel":"Toyota Prius","licensePlate":"ABC-1234","etaPickup":"5 minutes"},"timestamp":"2024-08-18T10:35:00Z"}'
```

---

### Scenario 3: Ride Started

**Notification Event:**
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440101",
  "senderId": "550e8400-e29b-41d4-a716-446655440001",
  "receiverId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "RIDE_STARTED",
  "payload": {
    "rideId": "RIDE-001",
    "liveTrackingUrl": "https://maps.google.com/?q=50.1234,14.5678",
    "currentLocation": "Main Street",
    "etaDestination": "20 minutes"
  },
  "timestamp": "2024-08-18T10:40:00Z"
}
```

**Redis Command:**
```bash
redis-cli PUBLISH smr:notifications '{"notificationId":"550e8400-e29b-41d4-a716-446655440101","senderId":"550e8400-e29b-41d4-a716-446655440001","receiverId":"550e8400-e29b-41d4-a716-446655440003","type":"RIDE_STARTED","payload":{"rideId":"RIDE-001","liveTrackingUrl":"https://maps.google.com/?q=50.1234,14.5678","currentLocation":"Main Street","etaDestination":"20 minutes"},"timestamp":"2024-08-18T10:40:00Z"}'
```

---

### Scenario 4: Ride Completed

**Notification Event:**
```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440102",
  "senderId": "550e8400-e29b-41d4-a716-446655440001",
  "receiverId": "550e8400-e29b-41d4-a716-446655440003",
  "type": "RIDE_COMPLETED",
  "payload": {
    "rideId": "RIDE-001",
    "totalDistance": "15.2 km",
    "totalAmount": 25.50,
    "rating": 5,
    "receiptUrl": "https://smr.com/receipts/RIDE-001.pdf"
  },
  "timestamp": "2024-08-18T11:00:00Z"
}
```

**Redis Command:**
```bash
redis-cli PUBLISH smr:notifications '{"notificationId":"550e8400-e29b-41d4-a716-446655440102","senderId":"550e8400-e29b-41d4-a716-446655440001","receiverId":"550e8400-e29b-41d4-a716-446655440003","type":"RIDE_COMPLETED","payload":{"rideId":"RIDE-001","totalDistance":"15.2 km","totalAmount":25.50,"rating":5,"receiptUrl":"https://smr.com/receipts/RIDE-001.pdf"},"timestamp":"2024-08-18T11:00:00Z"}'
```

---

## Complete Test Collection

### Postman Collection JSON

Import this collection into Postman for quick setup:

```json
{
  "info": {
    "name": "SMR Notification Service",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Register Device Token - User 1",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"userId\": \"550e8400-e29b-41d4-a716-446655440001\",\n  \"fcmToken\": \"cKwpP9lZ:APA91bGhijk567890abcdefghijklmnopqrstuvwxyz\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/notify/device-token",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "notify", "device-token"]
        }
      }
    },
    {
      "name": "2. Register Device Token - User 2",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"userId\": \"550e8400-e29b-41d4-a716-446655440002\",\n  \"fcmToken\": \"dRxqQ0mA:APA91bHijkl890123abcdefghijklmnopqrstuvwxyz\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/notify/device-token",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "notify", "device-token"]
        }
      }
    },
    {
      "name": "3. Register Device Token - User 3",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"userId\": \"550e8400-e29b-41d4-a716-446655440003\",\n  \"fcmToken\": \"eJ4t_OlO:APA91bFxyz123456789abcdefghijklmnopqrstuvwxyz\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/notify/device-token",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "notify", "device-token"]
        }
      }
    }
  ]
}
```

### Step-by-Step Testing Workflow

#### Phase 1: Setup
1. ✅ Start application: `mvn spring-boot:run`
2. ✅ Verify Redis is running: `redis-cli ping` → Should return PONG
3. ✅ Generate JWT token (using one of the methods above)

#### Phase 2: REST API Testing
1. Register device tokens for 3 test users (using Postman)
2. Verify tokens are stored in Redis

#### Phase 3: WebSocket Testing
1. Connect User 1 via WebSocket with valid JWT
2. In another terminal, publish notification to Redis
3. Verify User 1 receives notification via WebSocket
4. Disconnect User 1, publish another notification
5. Verify notification is sent via FCM (check logs)

#### Phase 4: Multi-User Scenarios
1. Connect User 1 via WebSocket
2. Keep User 2 offline (only has device token)
3. Publish notification for User 1 (expects WebSocket delivery)
4. Publish notification for User 2 (expects FCM delivery)
5. Verify in logs which delivery method was used

---

## Troubleshooting

### Issue 1: "Missing WebSocket Authorization token"

**Problem:** WebSocket connection rejected with authorization error

**Solution:**
```bash
# Ensure Authorization header is sent with CONNECT frame
CONNECT
Authorization: Bearer YOUR_VALID_JWT_TOKEN
accept-version:1.0,1.1,1.2
heart-beat:0,0

```

### Issue 2: Invalid JWT Token

**Problem:** `"Invalid WebSocket JWT"` error

**Solution:**
1. Verify token was signed with the correct private key
2. Ensure the token hasn't expired (check `exp` claim)
3. Verify `userId` claim is present
4. Re-generate token using the token generator

### Issue 3: Redis Connection Error

**Problem:** `Cannot get a resource, pool error` in logs

**Solution:**
```bash
# Check Redis is running
redis-cli ping

# If not running, start Redis
# On Windows: 
redis-server

# On Mac:
brew services start redis

# On Linux:
sudo service redis-server start
```

### Issue 4: Firebase Configuration Error

**Problem:** `Failed to initialize Firebase App` in logs

**Solution:**
1. Ensure `firebase-service-account.json` exists in `src/main/resources/`
2. Verify the JSON is valid (not corrupted)
3. Restart application
4. For offline testing, Firebase is only used for FCM delivery

### Issue 5: Notification Not Received

**Problem:** Published notification to Redis but not received via WebSocket

**Checklist:**
- [ ] User is connected via WebSocket (check logs for "REGISTERING WEBSOCKET USER")
- [ ] JWT token is valid (check `exp` claim)
- [ ] User is subscribed to `/user/{userId}/queue/notifications`
- [ ] Redis topic is `smr:notifications`
- [ ] Notification JSON is valid
- [ ] Redis is running and accepting PUBLISH commands

### Issue 6: wscat Connection Refused

**Problem:** `Cannot connect to ws://localhost:8080/ws`

**Solution:**
```bash
# Verify application is running
curl http://localhost:8080

# Verify WebSocket endpoint is accessible
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  http://localhost:8080/ws
```

---

## Mock Data Reference

### User IDs
```
User 1: 550e8400-e29b-41d4-a716-446655440001
User 2: 550e8400-e29b-41d4-a716-446655440002
User 3: 550e8400-e29b-41d4-a716-446655440003
```

### FCM Tokens (Example)
```
Token 1: cKwpP9lZ:APA91bGhijk567890abcdefghijklmnopqrstuvwxyz
Token 2: dRxqQ0mA:APA91bHijkl890123abcdefghijklmnopqrstuvwxyz
Token 3: eJ4t_OlO:APA91bFxyz123456789abcdefghijklmnopqrstuvwxyz
```

### Notification Types
- `BOOKING_REQUEST` - User received a new booking request
- `BOOKING_ACCEPTED` - Booking request was accepted
- `RIDE_STARTED` - Ride has started
- `RIDE_COMPLETED` - Ride has finished
- `CUSTOM` - Any custom notification type

---

## Summary

| Component | Tested | Status |
|-----------|--------|--------|
| Device Token Registration (REST) | ✅ | Ready |
| WebSocket STOMP Connection | ✅ | Ready |
| JWT Authentication | ✅ | Ready |
| Online Notification Delivery (WebSocket) | ✅ | Ready |
| Offline Notification Delivery (FCM) | ✅ | Ready |
| Redis Pub/Sub Integration | ✅ | Ready |

**Start testing today with the guide above!** 🚀
