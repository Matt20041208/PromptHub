from fastapi import APIRouter
from pydantic import BaseModel, Field
from app.services.optimizer import optimizer

router = APIRouter()


class OptimizeRequest(BaseModel):
    prompt: str = Field(..., description="原始提示词", min_length=10)
    intent: str = Field(default="", description="目标意图")
    style: str = Field(default="", description="期望风格")


@router.post("/optimize")
async def optimize_prompt(req: OptimizeRequest):
    result = optimizer.optimize(req.prompt, req.intent, req.style)
    return {"code": 200, "message": "success", "data": result}
