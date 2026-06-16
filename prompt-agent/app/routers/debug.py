from fastapi import APIRouter
from pydantic import BaseModel, Field
from typing import Optional, Dict
from app.services.llm_service import llm_service

router = APIRouter()


class DebugRequest(BaseModel):
    prompt_content: str = Field(..., min_length=5, description="提示词模板内容")
    variables: Optional[Dict[str, str]] = Field(default=None, description="变量替换值")
    model: Optional[str] = Field(default=None, description="模型名称，默认使用配置的默认模型")


@router.post("/debug")
async def debug_prompt(req: DebugRequest):
    content = req.prompt_content
    if req.variables:
        for key, val in req.variables.items():
            content = content.replace("{{" + key + "}}", val)

    llm = llm_service.get_default_llm()
    if req.model:
        from langchain_openai import ChatOpenAI
        from app.config import OPENAI_API_KEY, OPENAI_BASE_URL
        llm = ChatOpenAI(api_key=OPENAI_API_KEY, base_url=OPENAI_BASE_URL, model=req.model, temperature=0.7)

    response = llm.invoke(content)
    return {
        "code": 200,
        "message": "success",
        "data": {
            "output": response.content,
            "model": req.model or "default",
            "filled_prompt": content
        }
    }
