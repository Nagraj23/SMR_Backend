from pydantic import BaseModel
from typing import Optional

class RouteIndexRequest(BaseModel):
    ride_id: str  
    pickup_latitude: float   
    pickup_longitude: float  
  
    drop_latitude: float
    drop_longitude: float

class RideSearchRequest(BaseModel):
    passenger_latitude: float
    passenger_longitude: float
 
    destination_latitude: Optional[float] = None
    destination_longitude: Optional[float] = None