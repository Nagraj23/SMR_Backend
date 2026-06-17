import math
from typing import List, Dict, Any
from shapely.geometry import Point
from services.spatial_cache import ROUTE_INDEX_CACHE

def calculate_haversine_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Calculates the absolute great-circle distance between two points on Earth in km."""
    EARTH_RADIUS_KM = 6371.0

    rad_lat1 = math.radians(lat1)
    rad_lon1 = math.radians(lon1)
    rad_lat2 = math.radians(lat2)
    rad_lon2 = math.radians(lon2)

    delta_lat = rad_lat2 - rad_lat1
    delta_lon = rad_lon2 - rad_lon1

    haversine_theta = (math.sin(delta_lat / 2) ** 2 + 
                       math.cos(rad_lat1) * math.cos(rad_lat2) * math.sin(delta_lon / 2) ** 2)
    
    angular_distance_radians = 2 * math.asin(math.sqrt(haversine_theta))
    return EARTH_RADIUS_KM * angular_distance_radians


# =====================================================================
# 🛠️ ORIGINAL SEARCH: Full-Trip Line Intersection (Preserved)
# =====================================================================
def search_matching_rides(passenger_lat: float, passenger_lon: float, radius_deg: float = 0.01) -> List[str]:
    """Finds all ride IDs whose full trip LineString intersects the passenger buffer."""
    matched_ride_ids = []
    passenger_point = Point(passenger_lon, passenger_lat)
    search_zone = passenger_point.buffer(radius_deg)
    
    for ride_id, cache_data in ROUTE_INDEX_CACHE.items():
        route_line = cache_data["route_line"]
        if route_line.intersects(search_zone):
            matched_ride_ids.append(ride_id)
            
    return matched_ride_ids


# =====================================================================
# 🎯 SEARCH MODE 1: Nearby Discover (Current Pickup Point Only)
# =====================================================================
def search_rides_by_pickup_radius(passenger_lat: float, passenger_lon: float, radius_km: float = 1.0) -> List[Dict[str, Any]]:
    """Filters available rides solely based on how close their start point is to the passenger."""
    matched_results = []

    for ride_id, cache_data in ROUTE_INDEX_CACHE.items():
        if cache_data["available_seats"] <= 0:
            continue

        distance = calculate_haversine_distance(
            passenger_lat, passenger_lon,
            cache_data["pickup_lat"], cache_data["pickup_lon"]
        )

        if distance <= radius_km:
            matched_results.append({
                "ride_id": ride_id,
                "pickup_distance_km": round(distance, 3),
                "available_seats": cache_data["available_seats"]
            })

    matched_results.sort(key=lambda x: x["pickup_distance_km"])
    return matched_results


# =====================================================================
# 🏁 SEARCH MODE 2: Destination-Only Radius Filter
# =====================================================================
def search_rides_by_destination_radius(dest_lat: float, dest_lon: float, radius_km: float = 2.0) -> List[Dict[str, Any]]:
    """Filters available rides based on how close their final drop-off point is to the target destination."""
    matched_results = []

    for ride_id, cache_data in ROUTE_INDEX_CACHE.items():
        if cache_data["available_seats"] <= 0:
            continue

        distance = calculate_haversine_distance(
            dest_lat, dest_lon,
            cache_data["drop_lat"], cache_data["drop_lon"]
        )

        if distance <= radius_km:
            matched_results.append({
                "ride_id": ride_id,
                "dropoff_distance_km": round(distance, 3),
                "available_seats": cache_data["available_seats"]
            })

    matched_results.sort(key=lambda x: x["dropoff_distance_km"])
    return matched_results


# =====================================================================
# 🔄 SEARCH MODE 3: Perfect Route Match (Start Point + End Point)
# =====================================================================
def search_rides_by_dual_anchors(
    p_lat: float, p_lon: float, 
    d_lat: float, d_lon: float, 
    start_radius_km: float = 1.0, 
    end_radius_km: float = 2.0
) -> List[Dict[str, Any]]:
    """Guarantees the driver is starting near the passenger AND dropping off near their destination."""
    matched_results = []

    for ride_id, cache_data in ROUTE_INDEX_CACHE.items():
        if cache_data["available_seats"] <= 0:
            continue

        # Check pickup proximity
        distance_start = calculate_haversine_distance(
            p_lat, p_lon,
            cache_data["pickup_lat"], cache_data["pickup_lon"]
        )
        if distance_start > start_radius_km:
            continue

        # Check drop-off proximity
        distance_end = calculate_haversine_distance(
            d_lat, d_lon,
            cache_data["drop_lat"], cache_data["drop_lon"]
        )
        if distance_end > end_radius_km:
            continue

        matched_results.append({
            "ride_id": ride_id,
            "pickup_distance_km": round(distance_start, 3),
            "dropoff_distance_km": round(distance_end, 3),
            "available_seats": cache_data["available_seats"]
        })

    matched_results.sort(key=lambda x: x["pickup_distance_km"])
    return matched_results