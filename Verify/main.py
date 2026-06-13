from fastapi import FastAPI, File, UploadFile, Form, status, HTTPException
from services.matrix_engine import FaceMatrixEngine
from deepface import DeepFace
import numpy as np

app = FastAPI(title="ShieldX Biometric Verification Service")

@app.post("/verify/extract", status_code=status.HTTP_200_OK)
async def extract_profile_vector(file: UploadFile = File(...)):
    image_bytes = await file.read()
    # 🎯 Force using our engine that has Facenet and retinaface locked down
    vector_array = FaceMatrixEngine.convert_image_to_vector(image_bytes)
    return vector_array

@app.post("/verify/compare", status_code=status.HTTP_200_OK)
async def compare_live_selfie(
    file: UploadFile = File(...), 
    stored_vector_string: str = Form(...)
):
    # 1. Convert the live selfie using the exact same robust engine method
    live_bytes = await file.read()
    live_vector = FaceMatrixEngine.convert_image_to_vector(live_bytes)

    # 2. Automatically clean any brackets, newlines, or formatting clutter
    cleaned_string = (
        stored_vector_string
        .replace("[", "")
        .replace("]", "")
        .replace("\n", "")
        .replace(" ", "")
    )

    # 3. Convert string components back to a high-performance numpy matrix array
    try:
        stored_list = [float(x) for x in cleaned_string.split(",") if x.strip()]
        stored_vector = np.array(stored_list)
        live_vector_np = np.array(live_vector)
    except Exception as parse_err:
        raise HTTPException(status_code=400, detail=f"Parsing error: {str(parse_err)}")

    # 4. Enforce a final safety check: Ensure dimensions are identical!
    if len(stored_vector) != len(live_vector_np):
        raise HTTPException(
            status_code=400, 
            detail=f"Dimension Mismatch! Stored: {len(stored_vector)}, Live: {len(live_vector_np)}. Ensure both use Facenet."
        )

    # 5. Run Euclidean spatial distance verification
    dot_product = np.dot(stored_vector, live_vector_np)
    norm_stored = np.linalg.norm(stored_vector)
    norm_live = np.linalg.norm(live_vector_np)
    
    # Cosine Distance formula: 1 - (A . B / (||A|| * ||B||))
    cosine_distance = float(1.0 - (dot_product / (norm_stored * norm_live)))
    
    # 🎯 For Facenet model with RetinaFace, a Cosine Distance threshold <= 0.40 means it is the SAME human!
    is_match = cosine_distance <= 0.40

    return {
        "is_match": is_match,
        "distance_score": cosine_distance,
        "dimensions_checked": len(stored_vector),
        "metric_used": "cosine"
    }