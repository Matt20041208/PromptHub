from fastapi import APIRouter
from pydantic import BaseModel, Field
from app.services.generator import generator
from typing import Optional, List

router = APIRouter()


class GenerateRequest(BaseModel):
    intent: str = Field(..., min_length=5)
    variables: Optional[List[str]] = None


@router.post("/generate")
async def generate_template(req: GenerateRequest):
    result = generator.generate(req.intent, req.variables)
    return {"code": 200, "message": "success", "data": result}
