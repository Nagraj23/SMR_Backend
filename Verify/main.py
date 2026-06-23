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
    # 1. Convert live selfie using the exact same robust engine
    live_bytes = await file.read()
    live_vector = FaceMatrixEngine.convert_image_to_vector(live_bytes)

    # 2. Clean EVERYTHING: strip spaces, newlines, brackets, and quotes
    cleaned_string = (
        stored_vector_string
        .replace("[", "")
        .replace("]", "")
        .replace("\n", "")
        .replace(" ", "")
        .replace('"', "")
        .replace("'", "")
    )

    # 3. Robust parsing check
    try:
        stored_list = [float(x) for x in cleaned_string.split(",") if x.strip()]
        stored_vector = np.array(stored_list)
        live_vector_np = np.array(live_vector)
    except Exception as parse_err:
        raise HTTPException(status_code=400, detail=f"Parsing error: {str(parse_err)}")

    # 🚨 CRITICAL CHECK: Print out exactly what is being sent from Spring Boot
    print("\n================= 🎯 BIOMETRIC DEBUG INTERCEPT =================")
    print(f"RAW STRING RECEIVED FROM JAVA : {stored_vector_string[:100]}...")
    print(f"PARSED STORED VECTOR LENGTH   : {len(stored_vector)}")
    print(f"LIVE SELFIE VECTOR LENGTH     : {len(live_vector_np)}")
    
    if len(stored_vector) == 0:
        print("❌ ERROR: Stored vector is empty! Java passed an invalid array string.")
        print("=================================================================\n")
        raise HTTPException(status_code=400, detail="Database profile vector data is empty or corrupt.")

    # 4. Enforce identical dimension checks
    if len(stored_vector) != len(live_vector_np):
        print(f"❌ DIMENSION MISMATCH DETECTED! Stored: {len(stored_vector)}, Live: {len(live_vector_np)}")
        print("=================================================================\n")
        raise HTTPException(
            status_code=400, 
            detail=f"Dimension Mismatch! Stored: {len(stored_vector)}, Live: {len(live_vector_np)}."
        )

    # 5. Run Euclidean spatial distance verification
    dot_product = np.dot(stored_vector, live_vector_np)
    norm_stored = np.linalg.norm(stored_vector)
    norm_live = np.linalg.norm(live_vector_np)
    
    cosine_distance = float(1.0 - (dot_product / (norm_stored * norm_live)))
    
    # Since you are uploading the exact same image, it should be close to 0.0!
    is_match = cosine_distance <= 0.40

    print(f"CALCULATED COSINE DISTANCE SCORE : {cosine_distance}")
    print(f"EVALUATED IS_MATCH FLAG          : {is_match}")
    print("=================================================================\n")

    return {
        "is_match": is_match,
        "distance_score": cosine_distance,
        "dimensions_checked": len(stored_vector),
        "metric_used": "cosine"
    }