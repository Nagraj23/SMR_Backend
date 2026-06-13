import cv2
import numpy as np
from deepface import DeepFace
from fastapi import HTTPException

class FaceMatrixEngine:

    @staticmethod
    def convert_image_to_vector(file_bytes: bytes) -> list[float]:
        try:
            # 1. Convert raw network binary buffers into an OpenCV image matrix
            np_array = np.frombuffer(file_bytes, np.uint8)
            image = cv2.imdecode(np_array, cv2.IMREAD_COLOR)
            if image is None:
                raise HTTPException(status_code=400, detail="Invalid image file data.")

            # 2. Extract facial embeddings with strict alignment rules
            embedding_objs = DeepFace.represent(
                img_path=image,
                model_name="Facenet",
                enforce_detection=True,
                detector_backend="retinaface",
                normalization="Facenet"
            )

            # 🚀 FIX: Explicitly tell Pylance that this object is a dictionary item
            first_face_object = embedding_objs[0]
            
            if isinstance(first_face_object, dict):
                face_vector = first_face_object["embedding"]
                return face_vector
            else:
                # Fallback handler to satisfy absolute type safety constraints
                raise HTTPException(status_code=400, detail="Unexpected data format returned from DeepFace engine.")

        except Exception as err:
            raise HTTPException(
                status_code=400, 
                detail=f"Verification rejected: Face alignment failed. Details: {str(err)}"
            )