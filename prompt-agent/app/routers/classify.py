from fastapi import APIRouter
from pydantic import BaseModel, Field
from app.services.classifier import classifier

router = APIRouter()


class ClassifyRequest(BaseModel):
    title: str = Field(..., min_length=1)
    description: str = Field(default="")
    content: str = Field(..., min_length=10)


@router.post("/classify")
async def classify_prompt(req: ClassifyRequest):
    result = classifier.classify(req.title, req.description, req.content)
    return {"code": 200, "message": "success", "data": result}
