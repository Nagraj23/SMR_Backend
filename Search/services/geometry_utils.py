from shapely.geometry import Point, LineString
from typing import List
from services.spatial_cache import ROUTE_INDEX_CACHE

def index_driver_route(ride_id: str, p_lat: float, p_lon: float, d_lat: float, d_lon: float) -> None:
    """
    Converts raw coordinate floats into a Shapely LineString vector 
    and saves it inside the global ROUTE_INDEX_CACHE map.
    """
    # 1. Create a LineString route using (Longitude, Latitude) tuples.
    # Remember: Order must be (Longitude, Latitude)!
    route_line = LineString([(p_lon, p_lat), (d_lon, d_lat)])
    
    # 2. Store the spatial vector directly into our in-memory dictionary
    ROUTE_INDEX_CACHE[ride_id] = route_line


def search_matching_rides(passenger_lat: float, passenger_lon: float, radius_deg: float = 0.01) -> List[str]:
    """
    Scans the in-memory cache to find all driver ride IDs whose route vectors 
    intersect a matching perimeter surrounding the passenger's location.
    """
    matched_ride_ids = []
    
    # 1. Define the passenger's current location point
    passenger_point = Point(passenger_lon, passenger_lat)
    
    # 2. Create a circular spatial area buffer around the passenger point
    search_zone = passenger_point.buffer(radius_deg)
    
    # 3. Iterate through all active cached rides to execute the proximity check
    for ride_id, route_line in ROUTE_INDEX_CACHE.items():
        # === STRATEGIC GAP ===
        # Use Shapely's built-in intersection function to check if the driver's 'route_line' 
        # intersects or passes through the passenger's 'search_zone'.
        # If it matches, append the ride_id to our matched_ride_ids array list!
        if route_line.intersects(search_zone):
            matched_ride_ids.append(ride_id)
            
    return matched_ride_ids