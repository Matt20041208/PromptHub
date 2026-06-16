from fastapi import APIRouter
from pydantic import BaseModel, Field
from app.services.evaluator import evaluator
from typing import Optional, List

router = APIRouter()


class TestCase(BaseModel):
    input: str = ""
    expected: str = ""


class EvaluateRequest(BaseModel):
    prompt_content: str = Field(..., min_length=10)
    test_cases: Optional[List[TestCase]] = None


@router.post("/evaluate")
async def evaluate_prompt(req: EvaluateRequest):
    cases = [c.model_dump() for c in req.test_cases] if req.test_cases else None
    result = evaluator.evaluate(req.prompt_content, cases)
    return {"code": 200, "message": "success", "data": result}
