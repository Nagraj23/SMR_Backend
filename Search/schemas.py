from pydantic import BaseModel, Field, field_validator
from uuid import UUID

class NearbyPickupSearchRequest(BaseModel):
    # Enforce strict float properties with geographic range limits
    lat: float = Field(..., description="Passenger's current latitude coordinates", example=18.6750)
    lon: float = Field(..., description="Passenger's current longitude coordinates", example=75.9050)
    radius_km: float = Field(1.0, description="Search perimeter constraint in kilometers", ge=0.1, le=10.0)

    @field_validator('lat')
    def validate_latitude_bounds(cls, value):
        if not (-90.0 <= value <= 90.0):
            raise ValueError("Latitude out of bounding sphere limits [-90, 90]")
        return value

    @field_validator('lon')
    def validate_longitude_bounds(cls, value):
        if not (-180.0 <= value <= 180.0):
            raise ValueError("Longitude out of bounding sphere limits [-180, 180]")
        return value


class NearbyDestinationSearchRequest(BaseModel):
    dest_lat: float = Field(..., description="Target destination latitude")
    dest_lon: float = Field(..., description="Target destination longitude")
    radius_km: float = Field(2.0, ge=0.1, le=20.0)


class DualAnchorExactSearchRequest(BaseModel):
    source_lat: float
    source_lon: float
    dest_lat: float
    dest_lon: float
    pickup_radius: float = 1.0
    dropoff_radius: float = 2.0