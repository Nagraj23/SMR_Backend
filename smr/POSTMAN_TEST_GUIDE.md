# SMR (Smart Mobility Ride) Application - Postman Testing Guide

## 📋 Application Overview
- **Base URL**: `http://localhost:8081`
- **Port**: 8081
- **Database**: PostgreSQL (localhost:5432, database: smr)
- **Authentication**: JWT Token-based

---

## 🔐 Authentication Flow

### Flow Sequence (Do in this order):
1. **Register** → Get User ID and receive OTP (printed in console)
2. **Verify OTP** → Get JWT Token for further requests
3. **Complete Profile** → Upload profile with photo
4. **Login** → Get JWT Token (for already verified users)
5. **Add Vehicle** → Register vehicle to user account
6. **Get Face Embedding** → Retrieve stored face embeddings

---

## 📡 API ENDPOINTS

### 1️⃣ REGISTER USER
**Endpoint**: `POST /api/auth/register`  
**Authentication**: ❌ Not Required  
**Content-Type**: `application/json`

#### Request Body:
```json
{
  "mail": "user@example.com",
  "password": "SecurePassword123!",
  "name": "John Doe"
}
```

#### Mock Data Examples:
```json
// Example 1: New User
{
  "mail": "john.doe@gmail.com",
  "password": "Password@123",
  "name": "John Doe"
}

// Example 2: Another User
{
  "mail": "sara.smith@outlook.com",
  "password": "SecurePass456!",
  "name": "Sara Smith"
}

// Example 3: Test User
{
  "mail": "test.rider@example.com",
  "password": "TestPass789@",
  "name": "Test Rider"
}
```

#### Response (Success - 201):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "mail": "john.doe@gmail.com",
  "name": "John Doe",
  "token": null
}
```

#### ⚠️ Important Notes:
- OTP is printed in **console/terminal** output with format: `🎯 LOCAL TEST OTP FOR NEW USER -> XXXX`
- Email must be unique
- Password must be strong
- Copy the `id` for next steps

---

### 2️⃣ VERIFY OTP
**Endpoint**: `POST /api/auth/verify`  
**Authentication**: ❌ Not Required  
**Content-Type**: `application/json`

#### Request Body:
```json
{
  "email": "john.doe@gmail.com",
  "otp": "1234",
  "type": "VERIFICATION"
}
```

#### Mock Data Examples:
```json
// After registering john.doe@gmail.com, check console for OTP
{
  "email": "john.doe@gmail.com",
  "otp": "1234",
  "type": "VERIFICATION"
}

// For password reset
{
  "email": "sara.smith@outlook.com",
  "otp": "5678",
  "type": "RESET"
}
```

#### Response (Success - 200):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "mail": "john.doe@gmail.com",
  "name": "John Doe",
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huLmRvZUBnbWFpbC5jb20iLCJ1c2VySWQiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE2OTQ3MzM0NzUsImV4cCI6MTY5NDgxOTg3NX0.V5uB..."
}
```

#### ⚠️ Important:
- **type** must be: `VERIFICATION` or `RESET`
- OTP expires after 2 minutes
- Copy the **token** for authenticated requests

---

### 3️⃣ COMPLETE USER PROFILE
**Endpoint**: `POST /api/auth/profile/complete`  
**Authentication**: ✅ Required (JWT Token)  
**Content-Type**: `multipart/form-data`  
**Header**: `Authorization: Bearer {TOKEN}`

#### Request Form Data:
```
name: "John Doe"
phone: "+14155552671"
file: [image_file]
```

#### Mock Data Examples:
```
// Example 1: US Number
name: John Doe
phone: +14155552671
file: [select profile-picture.jpg]

// Example 2: International Number
name: Sara Smith
phone: +919876543210
file: [select profile.png]

// Example 3: Different Format
name: Test Rider
phone: +442071838750
file: [select avatar.jpg]
```

#### Phone Number Format:
- Must use **E.164 standard** format
- Examples:
  - `+14155552671` (USA)
  - `+919876543210` (India)
  - `+442071838750` (UK)
  - `+33123456789` (France)

#### Response (Success - 200):
```json
"Profile registration finalized successfully. Account status mutated to VERIFIED."
```

#### ⚠️ Important:
- Must have **verified email** first
- File upload must be **multipart/form-data**
- Name: 2-100 characters
- Phone: Valid E.164 format
- After this, profile status becomes `VERIFIED`

---

### 4️⃣ LOGIN
**Endpoint**: `POST /api/auth/login`  
**Authentication**: ❌ Not Required  
**Content-Type**: `application/json`

#### Request Body:
```json
{
  "mail": "john.doe@gmail.com",
  "password": "Password@123"
}
```

#### Mock Data Examples:
```json
// Example 1: John's Login
{
  "mail": "john.doe@gmail.com",
  "password": "Password@123"
}

// Example 2: Sara's Login
{
  "mail": "sara.smith@outlook.com",
  "password": "SecurePass456!"
}

// Example 3: Test User Login
{
  "mail": "test.rider@example.com",
  "password": "TestPass789@"
}
```

#### Response (Success - 200):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "mail": "john.doe@gmail.com",
  "name": "John Doe",
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### ⚠️ Important:
- User must be **verified** before login
- Credentials must match exactly
- Returns JWT token for authenticated requests

---

### 5️⃣ FORGOT PASSWORD
**Endpoint**: `POST /api/auth/forgot`  
**Authentication**: ❌ Not Required  
**Content-Type**: `application/json`

#### Request Body:
```json
{
  "email": "john.doe@gmail.com"
}
```

#### Mock Data Examples:
```json
{
  "email": "john.doe@gmail.com"
}

{
  "email": "sara.smith@outlook.com"
}

{
  "email": "test.rider@example.com"
}
```

#### Response (Success - 200):
```json
"OTP has been sent to your email."
```

#### ⚠️ Important:
- Check console for OTP (not sent via email currently)
- OTP format: `🎯 LOCAL TEST OTP FOR EXISTING USER -> XXXX`
- OTP expires in 2 minutes

---

### 6️⃣ RESET PASSWORD
**Endpoint**: `PUT /api/auth/reset`  
**Authentication**: ❌ Not Required  
**Content-Type**: `application/json`

#### Request Body:
```json
{
  "email": "john.doe@gmail.com",
  "pass": "NewPassword@456"
}
```

#### Mock Data Examples:
```json
// After forgot password OTP verification
{
  "email": "john.doe@gmail.com",
  "pass": "NewSecurePassword123!"
}

{
  "email": "sara.smith@outlook.com",
  "pass": "UpdatedPass@789"
}

{
  "email": "test.rider@example.com",
  "pass": "ResetPassword@999"
}
```

#### Response (Success - 200):
```json
"Password reset successfully"
```

#### ⚠️ Important:
- Must verify OTP first (type: "RESET")
- New password should be strong
- Password must be different from previous

---

### 7️⃣ GET USER FACE EMBEDDING
**Endpoint**: `GET /api/auth/users/{id}/embedding`  
**Authentication**: ❌ Not Required  
**Path Parameter**: `id` = User UUID

#### URL Examples:
```
GET http://localhost:8081/api/auth/users/550e8400-e29b-41d4-a716-446655440000/embedding
GET http://localhost:8081/api/auth/users/660f9400-e29b-41d4-a716-446655440000/embedding
```

#### Response (Success - 200):
```json
[
  0.123456,
  0.234567,
  0.345678,
  0.456789,
  0.567890,
  -0.123456,
  -0.234567,
  -0.345678,
  -0.456789,
  -0.567890
]
```

#### ⚠️ Important:
- Returns list of Double values (face embedding vector)
- Returns empty array if no embedding found
- Use the user ID from registration/login response

---

### 8️⃣ ADD VEHICLE
**Endpoint**: `POST /api/vehicles/add/{ownerId}`  
**Authentication**: ❌ Not Required (but should be JWT protected)  
**Content-Type**: `application/json`  
**Path Parameter**: `ownerId` = User UUID

#### Request Body:
```json
{
  "model": "Honda Activa",
  "color": "Black",
  "licensePlateNumber": "DL01AB1234"
}
```

#### Mock Data Examples:
```json
// Example 1: Two-Wheeler (Scooter)
{
  "model": "Honda Activa 6G",
  "color": "Black",
  "licensePlateNumber": "DL01AB1234"
}

// Example 2: Two-Wheeler (Motorcycle)
{
  "model": "Hero Splendor Plus",
  "color": "Red",
  "licensePlateNumber": "MH02CD5678"
}

// Example 3: Car
{
  "model": "Maruti Swift",
  "color": "White",
  "licensePlateNumber": "KA03EF9101"
}

// Example 4: Another Bike
{
  "model": "Bajaj Pulsar 150",
  "color": "Blue",
  "licensePlateNumber": "TN04GH1121"
}

// Example 5: Luxury Two-Wheeler
{
  "model": "Royal Enfield Classic 350",
  "color": "Chrome",
  "licensePlateNumber": "UP05IJ3141"
}
```

#### URL Example:
```
POST http://localhost:8081/api/vehicles/add/550e8400-e29b-41d4-a716-446655440000
```

#### Response (Success - 201):
```json
"Vehicle registered successfully"
```

#### Response (Failure - 400):
```json
"Vehicle already exists"
```

#### ⚠️ Important:
- License plate must be **unique**
- Use verified user's UUID
- Model: Any vehicle model name
- Color: Any vehicle color
- License plate format: Can be any format (e.g., `DL01AB1234`)

---

## 🧪 Complete Testing Workflow

### Step 1: Register New User
```
POST /api/auth/register
{
  "mail": "test@example.com",
  "password": "TestPassword@123",
  "name": "Test User"
}
```
✅ **Save**: User ID from response

---

### Step 2: Check Console for OTP
Look at application console/terminal for:
```
🎯 LOCAL TEST OTP FOR NEW USER -> 1234
```
✅ **Save**: OTP code

---

### Step 3: Verify Email with OTP
```
POST /api/auth/verify
{
  "email": "test@example.com",
  "otp": "1234",
  "type": "VERIFICATION"
}
```
✅ **Save**: JWT Token from response

---

### Step 4: Complete Profile
```
POST /api/auth/profile/complete
Headers: Authorization: Bearer {TOKEN}
Form Data:
  name: Test User
  phone: +14155552671
  file: [any image file]
```
✅ Verify response: "Profile registration finalized successfully..."

---

### Step 5: Add Vehicle
```
POST /api/vehicles/add/{USER_ID}
{
  "model": "Honda Activa",
  "color": "Black",
  "licensePlateNumber": "DL01AB1234"
}
```
✅ Verify response: "Vehicle registered successfully"

---

### Step 6: Login (for verified users)
```
POST /api/auth/login
{
  "mail": "test@example.com",
  "password": "TestPassword@123"
}
```
✅ **Get**: New JWT Token

---

### Step 7: Get Face Embedding
```
GET /api/auth/users/{USER_ID}/embedding
```
✅ Verify response: Array of doubles (face vector)

---

## 📊 Data Validation Rules

### Email:
- ✅ Must be valid email format
- ✅ Must be unique
- ✅ Required for all auth endpoints

### Password:
- ✅ Minimum strong password recommended
- ✅ Encrypted with BCrypt
- ✅ Required for register and login

### Name:
- ✅ 2-100 characters
- ✅ Required for registration
- ✅ Can be updated in profile completion

### Phone:
- ✅ E.164 international format
- ✅ Starts with `+` and country code
- ✅ Examples: `+14155552671`, `+919876543210`, `+442071838750`
- ✅ Required in profile completion

### License Plate:
- ✅ Must be unique (no duplicates)
- ✅ Any alphanumeric format
- ✅ Required for vehicle registration

### OTP:
- ✅ 4-digit numeric code
- ✅ Expires after 2 minutes
- ✅ Only for VERIFICATION and RESET types

---

## 🔑 Important Environment Details

### Application Properties:
```
spring.application.name=smr-auth-service
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/smr
spring.datasource.username=postgres
spring.datasource.password=Nagraj@2005

app.security.jwt.private-key-path=keys/private.key
app.security.jwt.public-key-path=keys/public.key
app.security.jwt.expiration-ms=86400000 (24 hours)

spring.mail.host=smtp.gmail.com
spring.mail.username=shieldx.app@gmail.com
```

### JWT Token:
- **Algorithm**: RS256 (RSA)
- **Expiration**: 24 hours (86400000 ms)
- **Subject**: User email
- **Claims**: userId, name

---

## ❌ Common Error Messages & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Email already registered and verified!` | Email exists | Use different email |
| `Please verify your email address before logging in!` | Email not verified | Run OTP verification first |
| `Invalid email or password credentials!` | Wrong password | Check password spelling |
| `NO OTP found for this email` | OTP not generated | Run forgot/register first |
| `OTP expired` | OTP older than 2 mins | Generate new OTP |
| `OTP didn't match` | Wrong OTP code | Check console for correct OTP |
| `Vehicle already exists` | License plate duplicate | Use unique plate number |
| `User not found` | Invalid user ID | Use correct UUID from registration |
| `Invalid phone number format` | Wrong phone format | Use E.164 format: `+14155552671` |
| `Name must be between 2 and 100 characters` | Invalid name length | Keep name 2-100 chars |

---

## 🚀 Testing Checklist

- [ ] Register new user with unique email
- [ ] Check console for OTP
- [ ] Verify email with correct OTP
- [ ] Complete user profile with phone and photo
- [ ] Add vehicle with unique license plate
- [ ] Login with registered credentials
- [ ] Retrieve face embedding by user ID
- [ ] Try forgot password workflow
- [ ] Try registering with duplicate email (should fail)
- [ ] Try login before email verification (should fail)
- [ ] Try login with wrong password (should fail)
- [ ] Try adding duplicate vehicle (should fail)
- [ ] Test expired OTP scenario
- [ ] Test invalid phone number format

---

## 📱 Postman Collection Import

To use this in Postman:
1. Create new Collection: "SMR-Auth-API"
2. Add Requests for each endpoint
3. Use Pre-request Scripts to extract tokens:
```javascript
// After verify/login, save token:
pm.globals.set("jwt_token", pm.response.json().token);
pm.globals.set("user_id", pm.response.json().id);
```

4. Set Authorization header:
```
Bearer {{jwt_token}}
```

---

## 🔒 Security Notes

- ⚠️ **Never** commit private keys to repository
- ⚠️ Change default database password before production
- ⚠️ Change Gmail app password to actual secure password
- ⚠️ Enable HTTPS in production
- ⚠️ Add rate limiting to OTP endpoints
- ⚠️ Implement CORS properly
- ⚠️ Add input sanitization for all user inputs

---

## 📞 API Status Codes

| Code | Meaning |
|------|---------|
| 200 | ✅ OK - Request successful |
| 201 | ✅ Created - Resource created |
| 400 | ❌ Bad Request - Invalid input |
| 401 | ❌ Unauthorized - Missing/invalid token |
| 403 | ❌ Forbidden - Not allowed |
| 404 | ❌ Not Found - Resource doesn't exist |
| 500 | ❌ Server Error - Internal issue |

---

**Last Updated**: 2026-08-18  
**Status**: Ready for Testing ✅
