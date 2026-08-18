# 🚀 Quick Start Guide - Postman Testing

## 5-Minute Setup

### Step 1: Start the Server
```bash
cd "Backend/Search"
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

**Expected Output**:
```
INFO:     Uvicorn running on http://127.0.0.1:8000
INFO:     Application startup complete
```

### Step 2: Import Postman Collection

1. **Open Postman**
2. Click **File** → **Import**
3. Select: `postman_collection.json` from this folder
4. Click **Import**

### Step 3: Set Environment Variable

1. In Postman, click the **Environment** icon (top-right)
2. Select or create "Ride Search API Local"
3. Set `base_url` = `http://localhost:8000`
4. Click **Save**

### Step 4: Run Tests

1. Click on any request in the collection
2. Click **Send**
3. Verify Status Code = **200** (or **422** for error tests)
4. Check **Response Body** matches expected output

---

## 📊 Test Collection Overview

| # | Test Name | Endpoint | Status | Purpose |
|---|-----------|----------|--------|---------|
| 1 | Nearby Pickup (1 km) | POST /api/rides/nearby-pickup | 200 | Find rides near pickup |
| 2 | Nearby Pickup (25 km) | POST /api/rides/nearby-pickup | 200 | Large radius search |
| 3 | Destination Search (2 km) | POST /api/rides/nearby-destination | 200 | Find rides near destination |
| 4 | Destination Search (10 km) | POST /api/rides/nearby-destination | 200 | Large destination search |
| 5 | Exact Route Match | POST /api/rides/search-exact | 200 | Match both pickup & destination |
| 6 | Exact Route (Strict) | POST /api/rides/search-exact | 200 | Stricter radius limits |
| 7 | Invalid Latitude | POST /api/rides/nearby-pickup | 422 | Validate bounds checking |
| 8 | Invalid Longitude | POST /api/rides/nearby-pickup | 422 | Validate bounds checking |
| 9 | Radius Too Small | POST /api/rides/nearby-pickup | 422 | Validate minimum radius |
| 10 | Radius Too Large | POST /api/rides/nearby-pickup | 422 | Validate maximum radius |
| 11 | No Matches Found | POST /api/rides/nearby-pickup | 200 | Empty results handling |

---

## 🎯 Test Coordinates (Mock Data)

### Rides in Cache

```javascript
RIDE001: Indore → Mumbai
  Pickup:  18.6750°N, 75.9050°E
  Dropoff: 19.0760°N, 72.8777°E
  Seats: 3

RIDE002: Near Indore → East
  Pickup:  18.6500°N, 75.9000°E
  Dropoff: 18.8000°N, 76.2000°E
  Seats: 2

RIDE003: Indore Area → North Mumbai
  Pickup:  18.7000°N, 75.8500°E
  Dropoff: 19.2000°N, 72.8500°E
  Seats: 4

RIDE004: Mumbai → Northeast
  Pickup:  19.0500°N, 72.8500°E
  Dropoff: 19.5000°N, 73.0000°E
  Seats: 1

RIDE005: South Indore → Indore Center
  Pickup:  18.5200°N, 75.7500°E
  Dropoff: 18.6750°N, 75.9050°E
  Seats: 2
```

### Search Locations

- **Indore City Center**: 18.6750°N, 75.9050°E
- **Mumbai City Center**: 19.0760°N, 72.8777°E
- **New York (No Matches)**: 40.7128°N, -74.0060°W

---

## ✅ Validation Rules

### Nearby Pickup Search
```json
{
  "lat": "Required. Range: -90 to 90",
  "lon": "Required. Range: -180 to 180",
  "radius_km": "Range: 0.1 to 10.0 (default: 1.0)"
}
```

### Nearby Destination Search
```json
{
  "dest_lat": "Required. Range: -90 to 90",
  "dest_lon": "Required. Range: -180 to 180",
  "radius_km": "Range: 0.1 to 20.0 (default: 2.0)"
}
```

### Exact Route Match Search
```json
{
  "source_lat": "Required. Range: -90 to 90",
  "source_lon": "Required. Range: -180 to 180",
  "dest_lat": "Required. Range: -90 to 90",
  "dest_lon": "Required. Range: -180 to 180",
  "pickup_radius": "Range: 0.1 to 10.0 (default: 1.0)",
  "dropoff_radius": "Range: 0.1 to 20.0 (default: 2.0)"
}
```

---

## 🔍 Sample Requests & Responses

### Request 1: Find Nearby Rides (1 km radius)
```bash
curl -X POST "http://localhost:8000/api/rides/nearby-pickup" \
  -H "Content-Type: application/json" \
  -d '{
    "lat": 18.6750,
    "lon": 75.9050,
    "radius_km": 1.0
  }'
```

**Response**:
```json
{
  "matches_found": 2,
  "results": [
    {
      "ride_id": "RIDE002",
      "pickup_distance_km": 0.035,
      "available_seats": 2
    },
    {
      "ride_id": "RIDE001",
      "pickup_distance_km": 0.245,
      "available_seats": 3
    }
  ]
}
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| `Connection refused` | Start server with `uvicorn main:app --reload` |
| `422 Validation Error` | Check coordinate bounds and radius limits |
| `No matches found` | Cache might be empty; verify rides are indexed |
| `Slow response` | More rides in cache = slower search |
| `CORS error in Postman` | Disable Web Security: Click Settings → Disable 'Automatically follow redirects' |

---

## 📈 Performance Tips

1. **Small Radius**: Use 1-2 km for detailed urban searches
2. **Large Radius**: Use 25-50 km for cross-city searches
3. **Exact Route**: Most restrictive, use for precise matching
4. **Batch Testing**: Use Postman Runner to test all endpoints sequentially

---

## 📚 API Documentation

**Interactive API Docs (Swagger UI)**:
```
http://localhost:8000/docs
```

**Alternative OpenAPI Docs (ReDoc)**:
```
http://localhost:8000/redoc
```

---

## 🎓 Learning Outcomes

After completing this test suite, you'll understand:
- ✅ FastAPI request/response handling
- ✅ Pydantic validation patterns
- ✅ Geographic coordinate systems (Latitude/Longitude)
- ✅ Haversine distance calculations
- ✅ RESTful API testing with Postman
- ✅ Mock data strategy for API testing
- ✅ HTTP status codes and error handling
- ✅ In-memory data caching patterns

---

## 📝 Notes

- All distances calculated using **Haversine formula**
- Coordinates use **WGS-84 (Latitude/Longitude)**
- Cache is in-memory; data persists until server restart
- No database required for local testing
- Full source code available in `main.py`, `schemas.py`, and `services/`

---

**Ready to test?** Start the server and import the Postman collection! 🚀
