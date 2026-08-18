# 📋 API Endpoint Reference Card

## Quick Reference for All Endpoints

### 🔴 Endpoint 1: Nearby Pickup Search
```
POST /api/rides/nearby-pickup
```

**Description**: Find rides based on passenger's current pickup location

**Request Body**:
```json
{
  "lat": 18.6750,           // Passenger latitude
  "lon": 75.9050,           // Passenger longitude  
  "radius_km": 1.0          // Search radius (0.1-10.0)
}
```

**Response (200 OK)**:
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

**Postman Tab**: TEST 1 - Nearby Pickup (1 km) / TEST 2 - Nearby Pickup (25 km)

---

### 🟢 Endpoint 2: Nearby Destination Search
```
POST /api/rides/nearby-destination
```

**Description**: Find rides based on target destination

**Request Body**:
```json
{
  "dest_lat": 19.0760,      // Destination latitude
  "dest_lon": 72.8777,      // Destination longitude
  "radius_km": 2.0          // Search radius (0.1-20.0)
}
```

**Response (200 OK)**:
```json
{
  "matches_found": 2,
  "results": [
    {
      "ride_id": "RIDE001",
      "dropoff_distance_km": 0.245,
      "available_seats": 3
    },
    {
      "ride_id": "RIDE003",
      "dropoff_distance_km": 1.834,
      "available_seats": 4
    }
  ]
}
```

**Postman Tab**: TEST 3 - Destination (2 km) / TEST 4 - Destination (10 km)

---

### 🔵 Endpoint 3: Exact Route Match Search
```
POST /api/rides/search-exact
```

**Description**: Find rides matching BOTH source AND destination criteria

**Request Body**:
```json
{
  "source_lat": 18.6750,    // Current location latitude
  "source_lon": 75.9050,    // Current location longitude
  "dest_lat": 19.0760,      // Target destination latitude
  "dest_lon": 72.8777,      // Target destination longitude
  "pickup_radius": 1.0,     // Pickup search radius (0.1-10.0)
  "dropoff_radius": 2.0     // Dropoff search radius (0.1-20.0)
}
```

**Response (200 OK)**:
```json
{
  "matches_found": 2,
  "results": [
    {
      "ride_id": "RIDE001",
      "pickup_distance_km": 0.245,
      "dropoff_distance_km": 0.245,
      "available_seats": 3
    },
    {
      "ride_id": "RIDE003",
      "pickup_distance_km": 6.543,
      "dropoff_distance_km": 1.834,
      "available_seats": 4
    }
  ]
}
```

**Postman Tab**: TEST 5 - Exact Route / TEST 6 - Exact Route (Strict)

---

## 🚨 Error Responses

### Invalid Latitude (Out of Bounds)
```
POST /api/rides/nearby-pickup
Content-Type: application/json

{
  "lat": 95.0,
  "lon": 75.9050,
  "radius_km": 1.0
}
```

**Response (422 Unprocessable Entity)**:
```json
{
  "detail": [
    {
      "type": "value_error",
      "loc": ["body", "lat"],
      "msg": "Latitude out of bounding sphere limits [-90, 90]",
      "input": 95.0
    }
  ]
}
```

---

### Invalid Longitude (Out of Bounds)
```
POST /api/rides/nearby-pickup
Content-Type: application/json

{
  "lat": 18.6750,
  "lon": 185.0,
  "radius_km": 1.0
}
```

**Response (422 Unprocessable Entity)**:
```json
{
  "detail": [
    {
      "type": "value_error",
      "loc": ["body", "lon"],
      "msg": "Longitude out of bounding sphere limits [-180, 180]",
      "input": 185.0
    }
  ]
}
```

---

### Radius Below Minimum
```
POST /api/rides/nearby-pickup
Content-Type: application/json

{
  "lat": 18.6750,
  "lon": 75.9050,
  "radius_km": 0.05
}
```

**Response (422 Unprocessable Entity)**:
```json
{
  "detail": [
    {
      "type": "greater_than_equal",
      "loc": ["body", "radius_km"],
      "msg": "Input should be greater than or equal to 0.1",
      "input": 0.05
    }
  ]
}
```

---

### Radius Exceeds Maximum
```
POST /api/rides/nearby-pickup
Content-Type: application/json

{
  "lat": 18.6750,
  "lon": 75.9050,
  "radius_km": 15.0
}
```

**Response (422 Unprocessable Entity)**:
```json
{
  "detail": [
    {
      "type": "less_than_equal",
      "loc": ["body", "radius_km"],
      "msg": "Input should be less than or equal to 10",
      "input": 15.0
    }
  ]
}
```

---

### No Results Found (Empty Cache or No Matches)
```
POST /api/rides/nearby-pickup
Content-Type: application/json

{
  "lat": 40.7128,
  "lon": -74.0060,
  "radius_km": 1.0
}
```

**Response (200 OK - No Error)**:
```json
{
  "matches_found": 0,
  "results": []
}
```

---

## 🧪 Validation Constraints

### Latitude
- **Required**: Yes
- **Type**: Float
- **Range**: -90.0 to 90.0
- **Example**: 18.6750

### Longitude
- **Required**: Yes
- **Type**: Float
- **Range**: -180.0 to 180.0
- **Example**: 75.9050

### Radius (Nearby Pickup)
- **Required**: No
- **Type**: Float
- **Default**: 1.0
- **Range**: 0.1 to 10.0 km
- **Example**: 1.0, 2.5, 10.0

### Radius (Nearby Destination)
- **Required**: No
- **Type**: Float
- **Default**: 2.0
- **Range**: 0.1 to 20.0 km
- **Example**: 2.0, 5.0, 20.0

### Pickup Radius (Exact Route)
- **Required**: No
- **Type**: Float
- **Default**: 1.0
- **Range**: 0.1 to 10.0 km

### Dropoff Radius (Exact Route)
- **Required**: No
- **Type**: Float
- **Default**: 2.0
- **Range**: 0.1 to 20.0 km

---

## 🗺️ Mock Data Coordinates

### Test Rides

| Ride | Pickup Point | Dropoff Point | Seats | Details |
|------|--------------|---------------|-------|---------|
| RIDE001 | 18.6750, 75.9050 (Indore Center) | 19.0760, 72.8777 (Mumbai) | 3 | Long-distance ride |
| RIDE002 | 18.6500, 75.9000 (Near Indore) | 18.8000, 76.2000 (East) | 2 | Short route |
| RIDE003 | 18.7000, 75.8500 (Indore Area) | 19.2000, 72.8500 (N. Mumbai) | 4 | Multi-passenger |
| RIDE004 | 19.0500, 72.8500 (Mumbai) | 19.5000, 73.0000 (NE) | 1 | Single seat |
| RIDE005 | 18.5200, 75.7500 (S. Indore) | 18.6750, 75.9050 (Center) | 2 | Return ride |

### Test Search Points

- **Indore**: 18.6750°N, 75.9050°E
- **Mumbai**: 19.0760°N, 72.8777°E  
- **New York**: 40.7128°N, -74.0060°W (No matches)

---

## 💾 Response Fields Reference

### Nearby Pickup Response
```json
{
  "matches_found": "Integer: Total number of matching rides",
  "results": [
    {
      "ride_id": "String: Unique ride identifier",
      "pickup_distance_km": "Float: Distance from passenger to ride pickup",
      "available_seats": "Integer: Number of empty seats in ride"
    }
  ]
}
```

### Nearby Destination Response
```json
{
  "matches_found": "Integer: Total number of matching rides",
  "results": [
    {
      "ride_id": "String: Unique ride identifier",
      "dropoff_distance_km": "Float: Distance from destination to ride dropoff",
      "available_seats": "Integer: Number of empty seats in ride"
    }
  ]
}
```

### Exact Route Response
```json
{
  "matches_found": "Integer: Total number of matching rides",
  "results": [
    {
      "ride_id": "String: Unique ride identifier",
      "pickup_distance_km": "Float: Distance from source to ride pickup",
      "dropoff_distance_km": "Float: Distance from destination to ride dropoff",
      "available_seats": "Integer: Number of empty seats in ride"
    }
  ]
}
```

---

## 🔗 Related Files

- [README.md](./README.md) - Complete codebase documentation
- [QUICK_START.md](./QUICK_START.md) - 5-minute setup guide
- [postman_collection.json](./postman_collection.json) - Importable Postman tests
- [main.py](./main.py) - FastAPI application
- [schemas.py](./schemas.py) - Pydantic validation models
- [services/search_service.py](./services/search_service.py) - Core search logic

---

**Last Updated**: 2024  
**API Version**: 1.0
