from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from app.services.generator import generator
from app.services.llm_service import agent_factory
from typing import Optional, List
import json

router = APIRouter()


class GenerateRequest(BaseModel):
    intent: str = Field(..., min_length=5)
    variables: Optional[List[str]] = None


class StreamRequest(BaseModel):
    prompt: str = Field(..., min_length=3, description="完整的提示词文本")


@router.post("/generate")
async def generate_template(req: GenerateRequest):
    result = generator.generate(req.intent, req.variables)
    return {"code": 200, "message": "success", "data": result}


@router.post("/generate/stream")
async def generate_stream(req: GenerateRequest):
    prompt = f"请根据以下意图生成一个高质量的提示词模板：\n\n意图：{req.intent}\n"
    if req.variables:
        prompt += f"需要包含以下变量：{', '.join(req.variables)}\n"
    prompt += "\n请直接输出提示词模板，不要解释。"

    llm = agent_factory.creator()

    async def event_stream():
        buffer = ""
        async for chunk in agent_factory.astream(llm, prompt):
            if isinstance(chunk, dict) and '_retry' in chunk:
                retry_msg = f"[{chunk['_retry']}]"
                yield f"data: {json.dumps({'text': retry_msg}, ensure_ascii=False)}\n\n"
                continue
            text = chunk.content if hasattr(chunk, 'content') else str(chunk)
            if not text or not text.strip(): continue
            buffer += text
            yield f"data: {json.dumps({'text': text}, ensure_ascii=False)}\n\n"
        yield f"data: {json.dumps({'done': True, 'full': buffer}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@router.post("/debug/stream")
async def debug_stream(req: StreamRequest):
    content = req.prompt
    llm = agent_factory.creator()

    async def event_stream():
        buffer = ""
        async for chunk in agent_factory.astream(llm, content):
            if isinstance(chunk, dict) and '_retry' in chunk:
                retry_msg = f"[{chunk['_retry']}]"
                yield f"data: {json.dumps({'text': retry_msg}, ensure_ascii=False)}\n\n"
                continue
            text = chunk.content if hasattr(chunk, 'content') else str(chunk)
            if not text or not text.strip(): continue
            buffer += text
            yield f"data: {json.dumps({'text': text}, ensure_ascii=False)}\n\n"
        yield f"data: {json.dumps({'done': True, 'full': buffer}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
