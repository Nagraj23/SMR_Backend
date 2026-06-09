# The global in-memory dictionary acting as our ultra-fast spatial indexing engine
ROUTE_INDEX_CACHE = {}

def clear_cache():
    """Utility helper to clear cache during tests or service resets."""
    ROUTE_INDEX_CACHE.clear()