# 📦 Mock Data Setup Script

This script demonstrates how to populate the spatial cache with mock ride data for testing.

## Option 1: Using curl (Bash/PowerShell)

```bash
# Make sure the server is running on http://localhost:8000

# Example 1: Direct interaction via Python
python -c "
from services.spatial_cache import index_driver_route

# Add mock rides to cache
index_driver_route('RIDE001', 18.6750, 75.9050, 19.0760, 72.8777, 3)
index_driver_route('RIDE002', 18.6500, 75.9000, 18.8000, 76.2000, 2)
index_driver_route('RIDE003', 18.7000, 75.8500, 19.2000, 72.8500, 4)
index_driver_route('RIDE004', 19.0500, 72.8500, 19.5000, 73.0000, 1)
index_driver_route('RIDE005', 18.5200, 75.7500, 18.6750, 75.9050, 2)

print('✅ Mock data loaded successfully!')
"
```

## Option 2: Using Python Module

Create a file called `setup_mock_data.py` in your project root:

```python
#!/usr/bin/env python
"""
Setup Mock Data for Ride Search API Testing
Populates ROUTE_INDEX_CACHE with sample rides
"""

from services.spatial_cache import index_driver_route, ROUTE_INDEX_CACHE

def setup_mock_rides():
    \"\"\"Initialize cache with mock ride data\"\"\"
    
    # Mock Ride 1: Indore to Mumbai (Long-distance)
    index_driver_route(
        ride_id='RIDE001',
        p_lat=18.6750,    # Indore
        p_lon=75.9050,
        d_lat=19.0760,    # Mumbai
        d_lon=72.8777,
        seats=3
    )
    
    # Mock Ride 2: Near Indore (Short route)
    index_driver_route(
        ride_id='RIDE002',
        p_lat=18.6500,
        p_lon=75.9000,
        d_lat=18.8000,
        d_lon=76.2000,
        seats=2
    )
    
    # Mock Ride 3: Indore Area to North Mumbai
    index_driver_route(
        ride_id='RIDE003',
        p_lat=18.7000,
        p_lon=75.8500,
        d_lat=19.2000,
        d_lon=72.8500,
        seats=4
    )
    
    # Mock Ride 4: Mumbai to Northeast
    index_driver_route(
        ride_id='RIDE004',
        p_lat=19.0500,
        p_lon=72.8500,
        d_lat=19.5000,
        d_lon=73.0000,
        seats=1
    )
    
    # Mock Ride 5: South Indore to Indore Center
    index_driver_route(
        ride_id='RIDE005',
        p_lat=18.5200,
        p_lon=75.7500,
        d_lat=18.6750,
        d_lon=75.9050,
        seats=2
    )
    
    print("🎉 Mock data setup complete!")
    print(f"📊 Total rides in cache: {len(ROUTE_INDEX_CACHE)}")
    print("\nRides indexed:")
    for ride_id in ROUTE_INDEX_CACHE:
        print(f"  ✓ {ride_id}")

if __name__ == "__main__":
    setup_mock_rides()
    
    # Optional: Print cache contents
    print("\n📋 Cache Contents:")
    for ride_id, data in ROUTE_INDEX_CACHE.items():
        print(f"\n{ride_id}:")
        print(f"  Pickup: ({data['pickup_lat']}, {data['pickup_lon']})")
        print(f"  Dropoff: ({data['drop_lat']}, {data['drop_lon']})")
        print(f"  Seats: {data['available_seats']}")
```

**Run with:**
```bash
python setup_mock_data.py
```

## Option 3: Modify main.py (Startup Hook)

Add this code to `main.py` to auto-populate data on startup:

```python
from fastapi import FastAPI, HTTPException, status
from services.search_service import (
    search_rides_by_pickup_radius, 
    search_rides_by_destination_radius, 
    search_rides_by_dual_anchors
)
from schemas import (
    NearbyPickupSearchRequest, 
    NearbyDestinationSearchRequest, 
    DualAnchorExactSearchRequest
)
from services.spatial_cache import index_driver_route  # ADD THIS

app = FastAPI(title="Secure Polyglot Search Engine Cluster")

# ADD THIS STARTUP EVENT
@app.on_event("startup")
def startup_event():
    \"\"\"Populate cache with mock data on startup\"\"\"
    print("🚀 Loading mock rides into cache...")
    
    index_driver_route('RIDE001', 18.6750, 75.9050, 19.0760, 72.8777, 3)
    index_driver_route('RIDE002', 18.6500, 75.9000, 18.8000, 76.2000, 2)
    index_driver_route('RIDE003', 18.7000, 75.8500, 19.2000, 72.8500, 4)
    index_driver_route('RIDE004', 19.0500, 72.8500, 19.5000, 73.0000, 1)
    index_driver_route('RIDE005', 18.5200, 75.7500, 18.6750, 75.9050, 2)
    
    print("✅ Mock data loaded successfully!")

# ... rest of your code ...
```

---

## Mock Data Reference

### Ride Locations

```
🇮🇳 India Reference Map:

INDORE (Madhya Pradesh)
└── Center: 18.6750°N, 75.9050°E
    ├── South: 18.5200°N, 75.7500°E
    ├── North: 18.7000°N, 75.8500°E
    └── West: 18.6500°N, 75.9000°E

MUMBAI (Maharashtra)  
└── Center: 19.0760°N, 72.8777°E
    ├── South: 19.0500°N, 72.8500°E
    ├── North: 19.2000°N, 72.8500°E
    └── Northeast: 19.5000°N, 73.0000°E

DISTANCE: Indore ↔ Mumbai ≈ 680 km (straight line)
```

### Ride Details

```python
RIDE001 = {
    'id': 'RIDE001',
    'pickup': (18.6750, 75.9050),      # Indore Center
    'dropoff': (19.0760, 72.8777),     # Mumbai
    'seats': 3,
    'type': 'Long-distance intercity'
}

RIDE002 = {
    'id': 'RIDE002',
    'pickup': (18.6500, 75.9000),      # Near Indore
    'dropoff': (18.8000, 76.2000),     # East
    'seats': 2,
    'type': 'Short urban route'
}

RIDE003 = {
    'id': 'RIDE003',
    'pickup': (18.7000, 75.8500),      # Indore Area
    'dropoff': (19.2000, 72.8500),     # North Mumbai
    'seats': 4,
    'type': 'Multi-passenger'
}

RIDE004 = {
    'id': 'RIDE004',
    'pickup': (19.0500, 72.8500),      # Mumbai
    'dropoff': (19.5000, 73.0000),     # Northeast
    'seats': 1,
    'type': 'Single seat'
}

RIDE005 = {
    'id': 'RIDE005',
    'pickup': (18.5200, 75.7500),      # South Indore
    'dropoff': (18.6750, 75.9050),     # Indore Center
    'seats': 2,
    'type': 'Return ride'
}
```

---

## Verification

After setup, verify mock data with these Postman tests:

```bash
# Should find RIDE001, RIDE002, RIDE003, RIDE005 (4 rides)
POST /api/rides/nearby-pickup
{
  "lat": 18.6750,
  "lon": 75.9050,
  "radius_km": 25.0
}

# Should find RIDE001, RIDE003, RIDE004 (3 rides)  
POST /api/rides/nearby-destination
{
  "dest_lat": 19.0760,
  "dest_lon": 72.8777,
  "radius_km": 10.0
}

# Should find RIDE001, RIDE003 (2 rides)
POST /api/rides/search-exact
{
  "source_lat": 18.6750,
  "source_lon": 75.9050,
  "dest_lat": 19.0760,
  "dest_lon": 72.8777,
  "pickup_radius": 10.0,
  "dropoff_radius": 5.0
}
```

---

## Clearing Mock Data

```python
from services.spatial_cache import clear_cache

# Remove all rides from cache
clear_cache()
print("✓ Cache cleared!")
```

Or via curl:

```bash
curl -X POST "http://localhost:8000/api/cache/clear" \
  -H "Content-Type: application/json"
```

---

## Notes

- Mock data uses real geographic coordinates from India
- All distances calculated using Haversine formula
- Seats range from 1-4 passengers
- Routes are bidirectional (can search both ways)
- Data persists in memory until server restart
