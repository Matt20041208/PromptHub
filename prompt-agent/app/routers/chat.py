from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from app.services.llm_service import agent_factory
from typing import Optional
import json
import time

router = APIRouter()

try:
    import redis.asyncio as aioredis
    r = aioredis.Redis(host='127.0.0.1', port=6379, db=0, decode_responses=True)
except Exception:
    r = None

CHAT_TTL = 1800  # 30 分钟
MAX_HISTORY = 20
COMPRESS_OLD = 10

SYSTEM_PROMPT = """你是 PromptHub 的 AI 提示词优化助手。你的工作：

1. 帮助用户迭代优化提示词——理解用户的需求，修改提示词内容
2. 你可以：生成新提示词、优化现有提示词、评估质量、分类打标签
3. 每次回复直接给出修改后的提示词，附上简短的修改说明
4. 保持对话自然流畅，像助手一样工作
5. 当用户说"发布"/"上架"/"可以了"，回复时末尾加上 [ACTION:publish]"""


class ChatRequest(BaseModel):
    sessionId: str = Field(..., min_length=1, description="会话 ID")
    message: str = Field(..., min_length=1, description="用户消息")


def _get_history_key(session_id: str) -> str:
    return f"chat:{session_id}"


async def _load_history(session_id: str) -> list:
    if r:
        try:
            data = await r.get(_get_history_key(session_id))
            if data:
                return json.loads(data)
        except Exception:
            pass
    return [{"role": "system", "content": SYSTEM_PROMPT}]


async def _save_history(session_id: str, history: list):
    if r:
        try:
            await r.setex(_get_history_key(session_id), CHAT_TTL, json.dumps(history, ensure_ascii=False))
        except Exception:
            pass


def _compress_history(messages: list) -> list:
    """压缩旧消息，保留最近的消息"""
    if len(messages) <= MAX_HISTORY:
        return messages

    old = messages[1:COMPRESS_OLD + 1]
    recent = messages[COMPRESS_OLD + 1:]

    old_text_lines = []
    for msg in old:
        role = "用户" if msg["role"] == "user" else "AI"
        content = msg.get("content", "")[:200]
        if content.strip():
            old_text_lines.append(f"{role}: {content}")

    if old_text_lines:
        summary_prompt = f"用 80 字以内总结这段对话的关键信息（只写用户的需求和AI做了什么，不要提压缩）：\n" + "\n".join(old_text_lines)
        try:
            summary_llm = agent_factory.cheap()
            summary_resp = agent_factory.invoke(summary_llm, summary_prompt)
            summary = f"[历史摘要] {summary_resp.content.strip()}"
        except Exception:
            summary = f"[历史摘要] 用户之前的对话涉及提示词优化，包含 {len(old_text_lines)} 轮交互"

        return [{"role": "system", "content": SYSTEM_PROMPT + "\n\n" + summary}] + recent

    return [{"role": "system", "content": SYSTEM_PROMPT}] + recent


@router.post("/chat")
async def chat(req: ChatRequest):
    messages = await _load_history(req.sessionId)

    if len(messages) == 0 or messages[0].get("role") != "system":
        messages.insert(0, {"role": "system", "content": SYSTEM_PROMPT})

    messages.append({"role": "user", "content": req.message})

    messages = _compress_history(messages)

    llm = agent_factory.creator()

    async def event_stream():
        buffer = ""
        try:
            async for chunk in agent_factory.astream(llm, messages):
                if isinstance(chunk, dict) and '_retry' in chunk:
                    retry_msg = f"[{chunk['_retry']}]"
                    yield f"data: {json.dumps({'text': retry_msg}, ensure_ascii=False)}\n\n"
                    continue
                text = chunk.content if hasattr(chunk, 'content') else str(chunk)
                if not text or not text.strip():
                    continue
                buffer += text
                yield f"data: {json.dumps({'text': text}, ensure_ascii=False)}\n\n"
        except Exception as e:
            yield f"data: {json.dumps({'error': str(e)}, ensure_ascii=False)}\n\n"
            return

        messages.append({"role": "assistant", "content": buffer})
        await _save_history(req.sessionId, messages)

        action = None
        if "[ACTION:publish]" in buffer:
            action = "publish"

        yield f"data: {json.dumps({'done': True, 'full': buffer, 'action': action}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")


@router.delete("/chat/{sessionId}")
async def clear_chat(sessionId: str):
    if r:
        try:
            await r.delete(_get_history_key(sessionId))
        except Exception:
            pass
    return {"code": 200, "message": "cleared"}
