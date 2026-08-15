import cv2
import numpy as np
from deepface import DeepFace
from fastapi import HTTPException


class FaceMatrixEngine:

    @staticmethod
    def convert_image_to_vector(file_bytes: bytes) -> list[float]:
        try:
            # 1. Convert raw bytes into OpenCV image
            np_array = np.frombuffer(file_bytes, np.uint8)
            image = cv2.imdecode(np_array, cv2.IMREAD_COLOR)

            if image is None:
                raise HTTPException(
                    status_code=400,
                    detail="Invalid image file data."
                )

            # 2. Generate face embeddings
            embedding_objs = DeepFace.represent(
                img_path=image,
                model_name="Facenet",
                enforce_detection=True,
                detector_backend="retinaface",
                normalization="Facenet"
            )

            # 3. DEBUG: Check how many faces DeepFace detected
            print("\n========== FACE DETECTION DEBUG ==========")
            print(f"Faces detected: {len(embedding_objs)}")

            for index, face_obj in enumerate(embedding_objs):
                if isinstance(face_obj, dict):
                    print(f"Face {index + 1}:")
                    print(f"  Embedding dimensions: {len(face_obj.get('embedding', []))}")
                    print(f"  Face region: {face_obj.get('facial_area')}")
                    print(f"  Detection confidence: {face_obj.get('face_confidence')}")

            print("==========================================\n")

            # 4. Make sure at least one face exists
            if not embedding_objs:
                raise HTTPException(
                    status_code=400,
                    detail="No face detected in the image."
                )

            # 5. For now, keep existing behavior:
            #    use the first detected face.
            face_objects = [
                obj for obj in embedding_objs
                if isinstance(obj, dict) and "embedding" in obj and "facial_area" in obj
            ]

            if not face_objects:
                raise HTTPException(
                    status_code=400,
                    detail="No valid face embedding detected."
                )

            largest_face_object = max(
                face_objects,
                key=lambda obj: (
                    obj["facial_area"].get("w", 0)
                    * obj["facial_area"].get("h", 0)
                )
            )

            if not isinstance(largest_face_object, dict):
                raise HTTPException(
                    status_code=400,
                    detail="Unexpected data format returned from DeepFace engine."
                )

            face_vector = largest_face_object.get("embedding")

            if not isinstance(face_vector, list):
                raise HTTPException(
                    status_code=400,
                    detail="Invalid face embedding returned by DeepFace."
                )

            # 6. Validate embedding dimension
            if len(face_vector) != 128:
                raise HTTPException(
                    status_code=400,
                    detail=f"Unexpected embedding dimension: {len(face_vector)}. Expected 128."
                )

            return face_vector

        except HTTPException:
            # Preserve our intentional HTTP errors
            raise

        except Exception as err:
            raise HTTPException(
                status_code=400,
                detail=f"Verification rejected: Face alignment failed. Details: {str(err)}"
            )