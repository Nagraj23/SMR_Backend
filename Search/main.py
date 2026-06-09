from fastapi import FastAPI, status
from schemas import RouteIndexRequest, RideSearchRequest
from services import geometry_utils
from services.spatial_cache import ROUTE_INDEX_CACHE

app = FastAPI(
    title="High-Performance Geo-Matching Engine",
    description="Stateless, in-memory geometric calculator for ultra-fast ride matching."
)

@app.get("/health")
def health_check():
    """Simple health checker endpoint for container orchestrators or gateway probes."""
    return {"status": "UP", "cached_routes_count": len(ROUTE_INDEX_CACHE)}


@app.post("/api/search/index", status_code=status.HTTP_201_CREATED)
def index_route(payload: RouteIndexRequest):
    """
    Invoked by Spring Boot whenever a new ride lifecycle transaction begins.
    Transforms data floats into memory-mapped shapes.
    """
    # === STRATEGIC GAP 1 ===
    # Call your service module function to calculate and cache the driver's LineString path vector.
    # Pass the variables from the incoming 'payload' object!
    geometry_utils.index_driver_route(
        ride_id=payload.ride_id,
        p_lat=payload.pickup_latitude,
        p_lon=payload.pickup_longitude,
        d_lat=payload.drop_latitude,
        d_lon=payload.drop_longitude
    )
    return {"message": "Route cached successfully", "ride_id": payload.ride_id}


@app.post("/api/search/match", status_code=status.HTTP_200_OK)
def match_rides(payload: RideSearchRequest):
    """
    Calculates spatial overlaps to return active, available matching driver ride IDs near the passenger.
    """
    # === STRATEGIC GAP 2 ===
    # Invoke your matching matrix engine using the passenger's current coordinates.
    # Store the returned list of ride IDs inside a variable named 'matching_ids'.
    matching_ids = geometry_utils.search_matching_rides(
        passenger_lat=payload.passenger_latitude,
        passenger_lon=payload.passenger_longitude,
        radius_deg=0.01  # Defaulting search boundary circle to approx ~1km
    )
    
    return {
        "passenger_location": {
            "lat": payload.passenger_latitude,
            "lon": payload.passenger_longitude
        },
        "matched_ride_ids": matching_ids
    }