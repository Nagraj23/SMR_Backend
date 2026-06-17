from fastapi import FastAPI, HTTPException, status
from services.search_service import (
    search_rides_by_pickup_radius, 
    search_rides_by_destination_radius, 
    search_rides_by_dual_anchors
)
# Inject our secure data transport schemas
from schemas import (
    NearbyPickupSearchRequest, 
    NearbyDestinationSearchRequest, 
    DualAnchorExactSearchRequest
)

app = FastAPI(title="Secure Polyglot Search Engine Cluster")

# 🎯 SECURED: Search MODE 1 - Nearby Discover via Secure Payload Body
@app.post("/api/rides/nearby-pickup", status_code=status.HTTP_200_OK)
def get_nearby_pickup_rides_secure(payload: NearbyPickupSearchRequest):
    """
    HTTP POST implementation keeping user coordinate variables fully encrypted 
    within the payload body, isolating telemetry out of open access routing server logs.
    """
    # Access verified parameters cleanly from our validated schema model
    results = search_rides_by_pickup_radius(
        passenger_lat=payload.lat, 
        passenger_lon=payload.lon, 
        radius_km=payload.radius_km
    )
    return {
        "matches_found": len(results),
        "results": results
    }


# 🏁 SECURED: Search MODE 2 - Destination-Only Filter
@app.post("/api/rides/nearby-destination")
def get_destination_rides_secure(payload: NearbyDestinationSearchRequest):
    results = search_rides_by_destination_radius(
        dest_lat=payload.dest_lat, 
        dest_lon=payload.dest_lon, 
        radius_km=payload.radius_km
    )
    return {"matches_found": len(results), "results": results}


# 🔄 SECURED: Search MODE 3 - Complete Route Match (Source + Destination)
@app.post("/api/rides/search-exact")
def get_exact_route_matches_secure(payload: DualAnchorExactSearchRequest):
    results = search_rides_by_dual_anchors(
        p_lat=payload.source_lat, p_lon=payload.source_lon,
        d_lat=payload.dest_lat, d_lon=payload.dest_lon,
        start_radius_km=payload.pickup_radius, end_radius_km=payload.dropoff_radius
    )
    return {"matches_found": len(results), "results": results}