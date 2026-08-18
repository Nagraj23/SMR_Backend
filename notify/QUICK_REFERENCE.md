# SMR Notification Service - Quick Reference & Architecture

## 📊 System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Notification Sources                      │
│         (Other Microservices in Ride-Sharing Platform)      │
└──────────────────────────┬──────────────────────────────────┘
                           │ Publish to Redis
                           ▼
                   ┌───────────────┐
                   │ Redis Channel │
                   │smr:notifications
                   └───────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
   ┌─────────┐      ┌──────────────┐    ┌─────────┐
   │Redis    │      │Notification  │    │Firebase │
   │Listener │─────▶│Service       │    │Config   │
   └─────────┘      └──────┬───────┘    └─────────┘
                           │
        ┌──────────────────┴──────────────────┐
        │                                     │
        ▼                                     ▼
   ┌──────────────┐                   ┌──────────────┐
   │WebSocket     │                   │FCM Push      │
   │STOMP         │                   │Service       │
   │(Online)      │                   │(Offline)     │
   └──────┬───────┘                   └──────┬───────┘
          │                                   │
          ▼                                   ▼
   ┌─────────────────────┐          ┌──────────────────┐
   │Connected Clients    │          │Firebase Servers  │
   │(Real-time updates)  │          │(Push to Phone)   │
   └─────────────────────┘          └──────────────────┘
```

---

## 🔌 API Endpoints Summary

| # | Method | Endpoint | Purpose | Auth | Body |
|---|--------|----------|---------|------|------|
| 1 | `POST` | `/api/notify/device-token` | Register FCM token | ❌ | `{ userId, fcmToken }` |
| 2 | `WS` | `/ws` | WebSocket/STOMP connection | ✅ JWT | STOMP protocol |

---

## 🔐 Authentication Quick Reference

### JWT Token Structure
```
Header: Authorization: Bearer {jwt_token}

Example Connection to WebSocket:
CONNECT
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
accept-version:1.0,1.1,1.2
```

### JWT Payload Requirements
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "iat": 1694000000,
  "exp": 1725600000
}
```

---

## 📱 Mock Test Users

Pre-configured for testing:

```yaml
User 1 (Rider):
  ID: 550e8400-e29b-41d4-a716-446655440001
  FCM Token: cKwpP9lZ:APA91bGhijk567890abcdefghijklmnopqrstuvwxyz
  
User 2 (Driver):
  ID: 550e8400-e29b-41d4-a716-446655440002
  FCM Token: dRxqQ0mA:APA91bHijkl890123abcdefghijklmnopqrstuvwxyz
  
User 3 (Support):
  ID: 550e8400-e29b-41d4-a716-446655440003
  FCM Token: eJ4t_OlO:APA91bFxyz123456789abcdefghijklmnopqrstuvwxyz
```

---

## 🧪 Test Scenarios

### Scenario A: Online User (WebSocket Delivery)
```
1. User connects via WebSocket with JWT
2. Publish notification to Redis: smr:notifications
3. System detects user is online
4. ✅ Notification delivered via WebSocket/STOMP (instant)
5. ✅ Logs show: "User [ID] is ONLINE. Delivering via WebSocket"
```

### Scenario B: Offline User (FCM Delivery)
```
1. User has device token registered but NOT connected to WebSocket
2. Publish notification to Redis: smr:notifications
3. System detects user is offline
4. ✅ Notification sent via Firebase Cloud Messaging
5. ✅ Logs show: "User [ID] is OFFLINE. Triggering FCM Push Fallback"
6. 📱 Push notification appears on user's phone
```

### Scenario C: User Disconnects During Session
```
1. User is connected via WebSocket
2. User closes browser/app (disconnect)
3. Next notification attempts delivery
4. ✅ System detects disconnect
5. ✅ Falls back to FCM push automatically
```

---

## 🚀 Quick Start Commands

### 1. Terminal - Start Application
```bash
cd c:\Users\Nagraj Nandal\Desktop\GitHub\New folder\Backend\notify
mvn clean install
mvn spring-boot:run
```

### 2. Terminal - Verify Redis Connection
```bash
redis-cli
ping
# Response: PONG
exit
```

### 3. Terminal - Connect via WebSocket
```bash
# Install wscat if needed
npm install -g wscat

# Connect to WebSocket
wscat -c ws://localhost:8080/ws

# Then send STOMP CONNECT frame with Bearer token
```

### 4. Terminal - Publish Test Notification
```bash
redis-cli
PUBLISH smr:notifications '{"notificationId":"550e8400-e29b-41d4-a716-446655440099","senderId":"550e8400-e29b-41d4-a716-446655440003","receiverId":"550e8400-e29b-41d4-a716-446655440001","type":"BOOKING_REQUEST","payload":{"rideId":"RIDE-001","pickupLocation":"Central Station","dropoffLocation":"Airport"},"timestamp":"2024-08-18T10:30:00Z"}'
```

---

## 📡 Notification Event Format

```json
{
  "notificationId": "550e8400-e29b-41d4-a716-446655440099",  // Unique notification ID
  "senderId": "550e8400-e29b-41d4-a716-446655440003",        // Who sent it
  "receiverId": "550e8400-e29b-41d4-a716-446655440001",      // Who gets it
  "type": "BOOKING_REQUEST",                                  // Event type
  "payload": {                                                // Custom data
    "rideId": "RIDE-001",
    "pickupLocation": "Central Station",
    "dropoffLocation": "Airport",
    "fare": 25.50,
    "distance": "15 km"
  },
  "timestamp": "2024-08-18T10:30:00Z"                        // When sent
}
```

### Supported Notification Types
- ✅ `BOOKING_REQUEST` - New ride request
- ✅ `BOOKING_ACCEPTED` - Booking confirmed
- ✅ `RIDE_STARTED` - Ride in progress
- ✅ `RIDE_COMPLETED` - Ride finished
- ✅ `CUSTOM` - Any custom type

---

## 🔍 Monitoring & Debugging

### Check Registered Users
```bash
redis-cli
keys *
# Lists all stored user tokens
```

### View Active WebSocket Sessions
Look at application logs:
```
=================================
REGISTERING WEBSOCKET USER
USER ID: 550e8400-e29b-41d4-a716-446655440001
SESSION ID: abc123def456
=================================
```

### Monitor Notification Delivery
Check logs for:
```
✅ "User [ID] is ONLINE. Delivering via WebSocket STOMP..."
📱 "User [ID] is OFFLINE. Triggering FCM Push Fallback..."
✅ "FCM push delivered successfully! ID: {messageId}"
```

---

## ⚙️ Configuration

### Application Properties
File: `src/main/resources/application.properties`

```properties
spring.application.name=notify

# Redis Connection
spring.data.redis.host=localhost
spring.data.redis.port=6379

# WebSocket Endpoint
# Configured in WebSocketConfig.java
# Endpoint: /ws
# Allowed Origins: *
```

### Redis Pub/Sub Topic
```
Channel: smr:notifications
```

### Firebase Configuration
```
Location: src/main/resources/firebase-service-account.json
Required for: FCM push delivery to offline users
```

### JWT Public Key
```
Location: src/main/resources/keys/public.key
Used for: Validating WebSocket connection tokens
```

---

## 🛠️ Development Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 4.1.0 | Web framework |
| Java | 17+ | Runtime |
| Redis | 6.0+ | Message broker |
| Firebase Admin SDK | 9.2.0 | Push notifications |
| Spring WebSocket | 4.1.0 | WebSocket support |
| JJWT | 0.11.5 | JWT parsing |
| Lombok | Latest | Code generation |

---

## 🧠 Key Design Patterns

### 1. **Hybrid Delivery Pattern**
- Primary: WebSocket for connected users (instant)
- Fallback: FCM for disconnected users (reliable)

### 2. **Event-Driven Architecture**
- Events published to Redis pub/sub
- Subscriber processes and routes intelligently

### 3. **Session Management**
- In-memory ConcurrentHashMap tracking user↔session mapping
- Auto-cleanup on disconnect

### 4. **JWT Authentication**
- RSA-256 signature verification
- Stateless authentication
- Extracted userId from JWT claims

---

## 🔐 Security Considerations

| Aspect | Implementation | Status |
|--------|----------------|--------|
| Transport Security | HTTPS/WSS | ⚠️ Configure in production |
| Authentication | JWT (RS256) | ✅ Implemented |
| Authorization | Per-user isolation | ✅ Implemented |
| Token Expiration | JWT exp claim | ✅ Supported |
| Input Validation | Spring Validation | ✅ Configured |
| CORS | AllowedOrigins: * | ⚠️ Restrict in production |

---

## 📊 Testing Checklist

- [ ] Application starts without errors
- [ ] Redis connection established
- [ ] Device token registration works (HTTP 200)
- [ ] WebSocket connection accepts valid JWT
- [ ] WebSocket connection rejects invalid JWT
- [ ] Online user receives WebSocket notification
- [ ] Offline user receives FCM notification
- [ ] Notification payload is complete and accurate
- [ ] User disconnect triggers fallback to FCM
- [ ] Multiple concurrent users work independently

---

## 🐛 Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| "Cannot connect to Redis" | Redis not running | Start Redis: `redis-server` |
| "Invalid JWT" | Wrong key/expired token | Regenerate with valid private key |
| "Missing Authorization token" | No Bearer token sent | Add `Authorization: Bearer {token}` header |
| Notification not received | WebSocket not subscribed properly | Use correct destination: `/user/{userId}/queue/notifications` |
| FCM delivery fails | Invalid token or Firebase not configured | Verify `firebase-service-account.json` |

---

## 📚 Documentation Links

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring WebSocket Guide](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [JJWT Documentation](https://github.com/jwtk/jjwt)
- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [Redis Documentation](https://redis.io/documentation)

---

## 📞 Support Resources

**For WebSocket Testing:**
- Primary Tool: [wscat](https://github.com/websockets/wscat)
- Alternative: Postman v11+ with WebSocket support
- Browser Console: Can test with native WebSocket API

**For API Testing:**
- Tool: Postman or Insomnia
- See: `POSTMAN_TESTING_GUIDE.md` for detailed examples

**Logs Location:**
- Check application console output
- Look for prefixes: ✅, ⚠️, ❌, 📡, 📱

---

**Last Updated:** 2024-08-18  
**Version:** 1.0  
**Status:** Ready for Testing
