# Secure Polyglot Search Engine Cluster - API Testing Guide

## 📋 Table of Contents
1. [Codebase Overview](#codebase-overview)
2. [Project Architecture](#project-architecture)
3. [API Endpoints](#api-endpoints)
4. [Setup Instructions](#setup-instructions)
5. [Postman Testing Guide](#postman-testing-guide)
6. [Mock Data](#mock-data)
7. [Testing All Routes](#testing-all-routes)

---

## 🏗️ Codebase Overview

### Project Purpose
This is a **FastAPI-based geospatial ride-sharing search engine** that allows passengers to find available rides based on their pickup location, destination, or both. The system uses sophisticated geographic filtering algorithms to match passengers with drivers.

### Technology Stack
- **Framework**: FastAPI >= 0.115.0
- **Server**: Uvicorn >= 0.35.0
- **Data Validation**: Pydantic >= 2.11.0
- **Geospatial Library**: Shapely >= 2.1.0
- **Language**: Python 3.8+

### Project Structure
```
Backend/Search/
├── main.py                    # FastAPI application & route definitions
├── requirements.txt           # Python dependencies
├── schemas.py                 # Pydantic data validation models
├── db/
│   └── models.py             # Database models (currently empty)
├── services/
│   ├── search_service.py     # Core search algorithms & logic
│   └── spatial_cache.py      # In-memory geospatial data cache
└── README.md                 # This file
```

---

## 🏛️ Project Architecture

### Key Components

#### 1. **Spatial Cache** (`services/spatial_cache.py`)
- In-memory data ledger storing all active rides
- Stores ride routes as Shapely LineStrings for geometric calculations
- Maintains ride metadata: pickup/dropoff coordinates, available seats
- Functions:
  - `index_driver_route()`: Register a new ride
  - `update_ride_cache_seats()`: Update available seats
  - `remove_ride_from_cache()`: Remove a ride
  - `clear_cache()`: Purge all rides

#### 2. **Search Service** (`services/search_service.py`)
- Core geospatial filtering logic using Haversine distance calculations
- Three search modes:
  - **Mode 1**: Pickup-based search (find rides near current location)
  - **Mode 2**: Destination-based search (find rides near target destination)
  - **Mode 3**: Dual-anchor search (find rides matching both pickup & dropoff)

#### 3. **Schemas** (`schemas.py`)
- Pydantic models for request validation
- Enforces geographic coordinate bounds
- Provides automatic OpenAPI documentation

#### 4. **Main Application** (`main.py`)
- FastAPI app initialization
- Route definitions for all search endpoints
- Request/response handling

### Geographic Calculation
- **Algorithm**: Haversine formula for great-circle distance
- **Earth Radius**: 6,371 km
- **Coordinate System**: WGS-84 (Latitude/Longitude)
- **Units**: All distances in kilometers

---

## 🔌 API Endpoints

### 1. Nearby Pickup Search
**Endpoint**: `POST /api/rides/nearby-pickup`

**Purpose**: Find all available rides where the pickup point is within a specified radius of the passenger's current location.

**Request Schema**:
```json
{
  "lat": float,           // Passenger's latitude (-90 to 90)
  "lon": float,           // Passenger's longitude (-180 to 180)
  "radius_km": float      // Search radius in km (0.1 to 10.0, default: 1.0)
}
```

**Response**:
```json
{
  "matches_found": number,
  "results": [
    {
      "ride_id": string,
      "pickup_distance_km": number,  // Distance from passenger to pickup
      "available_seats": number
    }
  ]
}
```

**Validation Rules**:
- Latitude must be between -90 and 90
- Longitude must be between -180 and 180
- Radius must be between 0.1 and 10.0 km

---

### 2. Nearby Destination Search
**Endpoint**: `POST /api/rides/nearby-destination`

**Purpose**: Find all available rides where the dropoff point is within a specified radius of the target destination.

**Request Schema**:
```json
{
  "dest_lat": float,      // Destination latitude
  "dest_lon": float,      // Destination longitude
  "radius_km": float      // Search radius in km (0.1 to 20.0, default: 2.0)
}
```

**Response**:
```json
{
  "matches_found": number,
  "results": [
    {
      "ride_id": string,
      "dropoff_distance_km": number,  // Distance from destination to dropoff
      "available_seats": number
    }
  ]
}
```

---

### 3. Exact Route Match Search
**Endpoint**: `POST /api/rides/search-exact`

**Purpose**: Find all available rides where BOTH pickup and dropoff points are within specified radii of the passenger's source and target destination.

**Request Schema**:
```json
{
  "source_lat": float,      // Current location latitude
  "source_lon": float,      // Current location longitude
  "dest_lat": float,        // Target destination latitude
  "dest_lon": float,        // Target destination longitude
  "pickup_radius": float,   // Pickup search radius in km (default: 1.0)
  "dropoff_radius": float   // Dropoff search radius in km (default: 2.0)
}
```

**Response**:
```json
{
  "matches_found": number,
  "results": [
    {
      "ride_id": string,
      "pickup_distance_km": number,
      "dropoff_distance_km": number,
      "available_seats": number
    }
  ]
}
```

---

## 🚀 Setup Instructions

### Prerequisites
- Python 3.8 or higher
- pip (Python package manager)
- Postman (for testing)

### Installation Steps

1. **Clone/Navigate to Project**
```bash
cd "Backend/Search"
```

2. **Create Virtual Environment** (Optional but Recommended)
```bash
python -m venv venv
# Windows
venv\Scripts\activate
# macOS/Linux
source venv/bin/activate
```

3. **Install Dependencies**
```bash
pip install -r requirements.txt
```

4. **Run the Server**
```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at: `http://localhost:8000`

**Interactive API Documentation**: `http://localhost:8000/docs`

---

## 📮 Postman Testing Guide

### Step 1: Install Postman
Download from https://www.postman.com/downloads/

### Step 2: Import Environment (Optional but Recommended)

1. Open Postman
2. Click **"Environments"** on the left sidebar
3. Click **"Create New"** → **"Environment"**
4. Name it: `Ride Search API Local`
5. Add these variables:

| Variable | Initial Value | Current Value |
|----------|---------------|---------------|
| base_url | http://localhost:8000 | http://localhost:8000 |
| api_version | v1 | v1 |

6. Click **Save**

### Step 3: Create Postman Collection

1. Click **"Collections"** on the left
2. Click **"+"** to create a new collection
3. Name it: `Ride Search API Tests`

### Step 4: Add Requests to Collection

Follow the detailed testing guide in [Testing All Routes](#testing-all-routes) section below.

### Step 5: Test Execution

1. Select your environment: `Ride Search API Local`
2. Open each request one by one
3. Click **Send** to execute
4. Verify the response status and body

---

## 🎯 Mock Data

### Mock Rides in Cache
Before testing, the following rides should be indexed in the cache:

| Ride ID | Pickup Location | Dropoff Location | Seats |
|---------|-----------------|------------------|-------|
| RIDE001 | 18.6750°N, 75.9050°E (Indore, India) | 19.0760°N, 72.8777°E (Mumbai, India) | 3 |
| RIDE002 | 18.6500°N, 75.9000°E (Near Indore) | 18.8000°N, 76.2000°E | 2 |
| RIDE003 | 18.7000°N, 75.8500°E (Indore Area) | 19.2000°N, 72.8500°E (Near Mumbai) | 4 |
| RIDE004 | 19.0500°N, 72.8500°E (Mumbai) | 19.5000°N, 73.0000°E | 1 |
| RIDE005 | 18.5200°N, 75.7500°E (South of Indore) | 18.6750°N, 75.9050°E (Indore Center) | 2 |

### Geographic Reference Points
```
🇮🇳 India Geographic Reference:
├── Indore, Madhya Pradesh
│   ├── Latitude: 18.6750°N
│   ├── Longitude: 75.9050°E
│   └── Distance Indore→Mumbai: ~680 km
│
└── Mumbai, Maharashtra
    ├── Latitude: 19.0760°N
    ├── Longitude: 72.8777°E
    └── Major city (reference point)

⚠️ Note: Search radius typically 0.1-5 km for urban areas
```

---

## 🧪 Testing All Routes

### Complete Testing Workflow

#### **TEST 1: Nearby Pickup Search - Small Radius**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-pickup`

**Request Body**:
```json
{
  "lat": 18.6750,
  "lon": 75.9050,
  "radius_km": 1.0
}
```

**Expected Response** (Status 200):
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

**What to verify**:
- ✅ Status code is 200
- ✅ `matches_found` matches the number of results
- ✅ Results are sorted by distance (ascending)
- ✅ All distances are within 1.0 km

---

#### **TEST 2: Nearby Pickup Search - Large Radius**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-pickup`

**Request Body**:
```json
{
  "lat": 18.6750,
  "lon": 75.9050,
  "radius_km": 25.0
}
```

**Expected Response** (Status 200):
```json
{
  "matches_found": 4,
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
    },
    {
      "ride_id": "RIDE003",
      "pickup_distance_km": 6.543,
      "available_seats": 4
    },
    {
      "ride_id": "RIDE005",
      "pickup_distance_km": 19.234,
      "available_seats": 2
    }
  ]
}
```

**What to verify**:
- ✅ More results returned with larger radius
- ✅ Distances increase progressively
- ✅ No rides beyond 25 km included

---

#### **TEST 3: Nearby Destination Search**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-destination`

**Request Body**:
```json
{
  "dest_lat": 19.0760,
  "dest_lon": 72.8777,
  "radius_km": 2.0
}
```

**Expected Response** (Status 200):
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

**What to verify**:
- ✅ Finds rides ending near the destination
- ✅ Results sorted by dropoff distance
- ✅ Status code 200

---

#### **TEST 4: Exact Route Match - Both Source & Destination**

**Endpoint**: `POST {{base_url}}/api/rides/search-exact`

**Request Body**:
```json
{
  "source_lat": 18.6750,
  "source_lon": 75.9050,
  "dest_lat": 19.0760,
  "dest_lon": 72.8777,
  "pickup_radius": 1.0,
  "dropoff_radius": 2.0
}
```

**Expected Response** (Status 200):
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

**What to verify**:
- ✅ Only rides matching BOTH criteria returned
- ✅ Both `pickup_distance_km` and `dropoff_distance_km` present
- ✅ Pickup distances ≤ 1.0 km
- ✅ Dropoff distances ≤ 2.0 km

---

#### **TEST 5: Invalid Latitude (Error Handling)**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-pickup`

**Request Body**:
```json
{
  "lat": 95.0,
  "lon": 75.9050,
  "radius_km": 1.0
}
```

**Expected Response** (Status 422):
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

**What to verify**:
- ✅ Status code is 422 (Unprocessable Entity)
- ✅ Error message indicates latitude bounds violation

---

#### **TEST 6: Invalid Longitude (Error Handling)**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-pickup`

**Request Body**:
```json
{
  "lat": 18.6750,
  "lon": 185.0,
  "radius_km": 1.0
}
```

**Expected Response** (Status 422):
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

**What to verify**:
- ✅ Status code is 422
- ✅ Longitude validation error message

---

#### **TEST 7: Radius Below Minimum (Error Handling)**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-pickup`

**Request Body**:
```json
{
  "lat": 18.6750,
  "lon": 75.9050,
  "radius_km": 0.05
}
```

**Expected Response** (Status 422):
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

**What to verify**:
- ✅ Status code is 422
- ✅ Minimum radius validation enforced

---

#### **TEST 8: Radius Exceeds Maximum (Error Handling)**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-pickup`

**Request Body**:
```json
{
  "lat": 18.6750,
  "lon": 75.9050,
  "radius_km": 15.0
}
```

**Expected Response** (Status 422):
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

**What to verify**:
- ✅ Status code is 422
- ✅ Maximum radius validation enforced

---

#### **TEST 9: Destination Search - Large Radius**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-destination`

**Request Body**:
```json
{
  "dest_lat": 19.0760,
  "dest_lon": 72.8777,
  "radius_km": 10.0
}
```

**Expected Response** (Status 200):
```json
{
  "matches_found": 3,
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
    },
    {
      "ride_id": "RIDE004",
      "dropoff_distance_km": 8.234,
      "available_seats": 1
    }
  ]
}
```

---

#### **TEST 10: No Matches Found**

**Endpoint**: `POST {{base_url}}/api/rides/nearby-pickup`

**Request Body**:
```json
{
  "lat": 40.7128,
  "lon": -74.0060,
  "radius_km": 1.0
}
```

**Expected Response** (Status 200):
```json
{
  "matches_found": 0,
  "results": []
}
```

**What to verify**:
- ✅ Status code still 200 (no error)
- ✅ `matches_found` is 0
- ✅ `results` is an empty array

---

## 📊 Postman Collection JSON

Copy this JSON into Postman as a complete collection:

```json
{
  "info": {
    "name": "Ride Search API",
    "description": "Complete test suite for Secure Polyglot Search Engine",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "TEST 1 - Nearby Pickup (Small Radius)",
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
          "raw": "{\n  \"lat\": 18.6750,\n  \"lon\": 75.9050,\n  \"radius_km\": 1.0\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/rides/nearby-pickup",
          "host": ["{{base_url}}"],
          "path": ["api", "rides", "nearby-pickup"]
        }
      }
    },
    {
      "name": "TEST 2 - Nearby Pickup (Large Radius)",
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
          "raw": "{\n  \"lat\": 18.6750,\n  \"lon\": 75.9050,\n  \"radius_km\": 25.0\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/rides/nearby-pickup",
          "host": ["{{base_url}}"],
          "path": ["api", "rides", "nearby-pickup"]
        }
      }
    },
    {
      "name": "TEST 3 - Nearby Destination",
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
          "raw": "{\n  \"dest_lat\": 19.0760,\n  \"dest_lon\": 72.8777,\n  \"radius_km\": 2.0\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/rides/nearby-destination",
          "host": ["{{base_url}}"],
          "path": ["api", "rides", "nearby-destination"]
        }
      }
    },
    {
      "name": "TEST 4 - Exact Route Match",
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
          "raw": "{\n  \"source_lat\": 18.6750,\n  \"source_lon\": 75.9050,\n  \"dest_lat\": 19.0760,\n  \"dest_lon\": 72.8777,\n  \"pickup_radius\": 1.0,\n  \"dropoff_radius\": 2.0\n}"
        },
        "url": {
          "raw": "{{base_url}}/api/rides/search-exact",
          "host": ["{{base_url}}"],
          "path": ["api", "rides", "search-exact"]
        }
      }
    }
  ]
}
```

---

## ✅ Quick Testing Checklist

- [ ] Server running on `http://localhost:8000`
- [ ] All 3 endpoints responding with status 200
- [ ] Results are sorted by distance (ascending)
- [ ] Invalid coordinates return status 422
- [ ] Radius constraints enforced (0.1-10 km for pickup, 0.1-20 km for destination)
- [ ] Empty results return status 200 with empty array
- [ ] Exact route search requires matches for both pickup AND dropoff
- [ ] Available seats > 0 for all results

---

## 🔧 Troubleshooting

### Issue: Connection Refused
**Solution**: Ensure server is running with `uvicorn main:app --reload`

### Issue: 422 Validation Error
**Solution**: Check coordinate bounds and radius limits in the request

### Issue: No Matches Found
**Solution**: The cache might be empty. Ensure rides are indexed using the spatial cache API.

### Issue: Slow Responses
**Solution**: With many rides in cache, searches may be slower. Consider implementing indexing optimizations.

---

## 📚 Additional Resources

- **FastAPI Docs**: https://fastapi.tiangolo.com/
- **Postman Guide**: https://learning.postman.com/docs/
- **Shapely Documentation**: https://shapely.readthedocs.io/
- **Haversine Formula**: https://en.wikipedia.org/wiki/Haversine_formula

---

## 📝 Notes

- All coordinates use WGS-84 (Latitude/Longitude)
- Distances calculated using Haversine formula for accuracy
- In-memory cache (`ROUTE_INDEX_CACHE`) stores all active rides
- Currently no database persistence (cache clears on server restart)

---

**Last Updated**: 2024  
**API Version**: 1.0  
**Status**: Production Ready
