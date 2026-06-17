from typing import Dict, Any
from shapely.geometry import LineString

# 🚀 UNIFIED IN-MEMORY GEOSPATIAL DATA LEDGER
# Maps ride_id (str) -> Dict with LineString and primitive coordinate values
ROUTE_INDEX_CACHE: Dict[str, Dict[str, Any]] = {}

def clear_cache() -> None:
    """Purges the entire memory cache."""
    ROUTE_INDEX_CACHE.clear()
    print("🧹 Spatial Cache Index successfully purged from memory heap.")


def index_driver_route(ride_id: str, p_lat: float, p_lon: float, d_lat: float, d_lon: float, seats: int) -> None:
    """
    Indexes the complete route. Stores the original Shapely LineString 
    alongside explicit start/end metadata coordinates and seat values.
    """
    # Order must be (Longitude, Latitude) for Shapely geometric objects!
    route_line = LineString([(p_lon, p_lat), (d_lon, d_lat)])
    
    ROUTE_INDEX_CACHE[ride_id] = {
        "route_line": route_line,
        "pickup_lat": p_lat,
        "pickup_lon": p_lon,
        "drop_lat": d_lat,
        "drop_lon": d_lon,
        "available_seats": seats
    }
    print(f"🎯 Cache Updated: Ride [{ride_id}] | Seats: {seats}")


def update_ride_cache_seats(ride_id: str, updated_seats: int) -> None:
    """Updates available seats. Evicts the ride from cache if fully booked."""
    if ride_id in ROUTE_INDEX_CACHE:
        if updated_seats <= 0:
            ROUTE_INDEX_CACHE.pop(ride_id)
            print(f"♻️ Cache Eviction: Ride [{ride_id}] is full.")
        else:
            ROUTE_INDEX_CACHE[ride_id]["available_seats"] = updated_seats


def remove_ride_from_cache(ride_id: str) -> None:
    """Forcibly removes a ride due to cancellation or state updates."""
    if ride_id in ROUTE_INDEX_CACHE:
        ROUTE_INDEX_CACHE.pop(ride_id)