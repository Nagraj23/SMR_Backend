# SMR API - Quick Reference Guide & cURL Commands

## 🚀 Quick Start Commands

### 1. Register User (cURL)
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "mail": "user@example.com",
    "password": "SecurePassword123!",
    "name": "John Doe"
  }'
```

### 2. Verify OTP (cURL)
```bash
curl -X POST http://localhost:8081/api/auth/verify \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "otp": "1234",
    "type": "VERIFICATION"
  }'
```

### 3. Complete Profile (cURL with file)
```bash
curl -X POST http://localhost:8081/api/auth/profile/complete \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "name=John Doe" \
  -F "phone=+14155552671" \
  -F "file=@/path/to/image.jpg"
```

### 4. Login (cURL)
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "mail": "user@example.com",
    "password": "SecurePassword123!"
  }'
```

### 5. Forgot Password (cURL)
```bash
curl -X POST http://localhost:8081/api/auth/forgot \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'
```

### 6. Reset Password (cURL)
```bash
curl -X PUT http://localhost:8081/api/auth/reset \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "pass": "NewPassword@456"
  }'
```

### 7. Get Face Embedding (cURL)
```bash
curl -X GET http://localhost:8081/api/auth/users/550e8400-e29b-41d4-a716-446655440000/embedding
```

### 8. Add Vehicle (cURL)
```bash
curl -X POST http://localhost:8081/api/vehicles/add/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "model": "Honda Activa",
    "color": "Black",
    "licensePlateNumber": "DL01AB1234"
  }'
```

---

## 📋 All Endpoints Summary Table

| # | Method | Endpoint | Auth | Description |
|---|--------|----------|------|-------------|
| 1 | POST | `/api/auth/register` | ❌ | Register new user |
| 2 | POST | `/api/auth/verify` | ❌ | Verify email with OTP |
| 3 | POST | `/api/auth/profile/complete` | ✅ | Complete user profile |
| 4 | POST | `/api/auth/login` | ❌ | Login user |
| 5 | POST | `/api/auth/forgot` | ❌ | Forgot password |
| 6 | PUT | `/api/auth/reset` | ❌ | Reset password |
| 7 | GET | `/api/auth/users/{id}/embedding` | ❌ | Get face embedding |
| 8 | POST | `/api/vehicles/add/{ownerId}` | ❌ | Add vehicle |

---

## 🧪 Test Scenarios

### Scenario 1: Full Registration Flow
```
1. Register → Save user_id
2. Check console for OTP
3. Verify OTP with VERIFICATION type
4. Complete Profile with JWT token
5. Add Vehicle with user_id
```

### Scenario 2: Login Flow
```
1. Login with email + password
2. Receive JWT token
3. Use token for authenticated endpoints
```

### Scenario 3: Password Reset Flow
```
1. Forgot Password → Receive OTP
2. Verify OTP with RESET type
3. Reset password with new password
4. Login with new password
```

---

## 📊 Mock Data Quick Copy-Paste

### Register Request
```json
{
  "mail": "john.doe@gmail.com",
  "password": "Password@123",
  "name": "John Doe"
}
```

### Verify Request
```json
{
  "email": "john.doe@gmail.com",
  "otp": "1234",
  "type": "VERIFICATION"
}
```

### Profile Complete (Form Data)
```
name: John Doe
phone: +14155552671
file: [image.jpg]
```

### Login Request
```json
{
  "mail": "john.doe@gmail.com",
  "password": "Password@123"
}
```

### Add Vehicle Request
```json
{
  "model": "Honda Activa 6G",
  "color": "Black",
  "licensePlateNumber": "DL01AB1234"
}
```

---

## 🔐 JWT Token Header
```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## ✅ Valid Test Data

### Phone Numbers (E.164 Format)
- `+14155552671` (USA)
- `+919876543210` (India)
- `+442071838750` (UK)
- `+33123456789` (France)
- `+81312345678` (Japan)

### License Plates
- `DL01AB1234`
- `MH02CD5678`
- `KA03EF9101`
- `TN04GH1121`
- `UP05IJ3141`

### Vehicle Models
- Honda Activa 6G
- Hero Splendor Plus
- Maruti Swift
- Bajaj Pulsar 150
- Royal Enfield Classic 350

### Vehicle Colors
- Black
- White
- Red
- Blue
- Silver
- Gray

---

## 🎯 Expected Responses

### Success Responses
```
200 OK
201 CREATED
```

### Error Responses
```
400 BAD REQUEST - Invalid input
401 UNAUTHORIZED - Missing token
404 NOT FOUND - Resource not found
500 INTERNAL SERVER ERROR - Server issue
```

---

## ⏱️ Timing Notes
- OTP expires in: 2 minutes
- JWT token expires in: 24 hours
- Application server port: 8081
- Database: PostgreSQL (localhost:5432)

---

## 🔍 Debugging Tips

### Check OTP
Look for this in application console:
```
🎯 LOCAL TEST OTP FOR NEW USER -> XXXX
🎯 LOCAL TEST OTP FOR EXISTING USER -> XXXX
```

### Verify Response Format
All responses should be valid JSON with:
```json
{
  "id": "UUID",
  "mail": "string",
  "name": "string",
  "token": "JWT_STRING"
}
```

### Common Issues
1. **OTP not found**: Run register/forgot first
2. **Email already registered**: Use different email
3. **Token expired**: Run login/verify again
4. **Duplicate vehicle**: Use unique license plate
5. **Invalid phone format**: Use `+countrycode...` format

---

## 📱 Postman Setup

### Import Collection
1. Open Postman
2. Click "Import"
3. Upload `SMR_Postman_Collection.json`
4. Set variables in collection settings:
   - `BASE_URL` = `http://localhost:8081`
   - `jwt_token` = (auto-filled after login)
   - `user_id` = (auto-filled after register)

### Pre-request Script
```javascript
// Optional: Set any defaults before request
```

### Tests Script
```javascript
// Auto-save token from response
if (pm.response.code === 200 || pm.response.code === 201) {
    var jsonData = pm.response.json();
    if (jsonData.token) {
        pm.globals.set("jwt_token", jsonData.token);
    }
    if (jsonData.id) {
        pm.globals.set("user_id", jsonData.id);
    }
}
```

---

## 🚨 Security Checklist

- [ ] Don't commit credentials to Git
- [ ] Use strong passwords (min 8 chars, mix case, numbers, symbols)
- [ ] Don't share JWT tokens
- [ ] Change default database password
- [ ] Use HTTPS in production
- [ ] Implement rate limiting
- [ ] Sanitize all user inputs
- [ ] Never expose private keys

---

## 📞 Support Info

### Database Connection
```
Host: localhost
Port: 5432
Database: smr
Username: postgres
Password: Nagraj@2005
```

### SMTP Settings
```
Host: smtp.gmail.com
Port: 587
Username: shieldx.app@gmail.com
Protocol: TLS
```

### Application
```
Port: 8081
Context: /api
```

---

## ✨ Additional Notes

- All endpoints return JSON responses
- All timestamps are ISO 8601 format
- UUIDs are version 4 (random)
- Passwords are bcrypt encrypted
- Face embeddings are stored as Double arrays
- Vehicles are mapped to users via user_id

---

**Last Updated**: 2026-08-18  
**Format**: Quick Reference  
**Status**: ✅ Ready to Use
