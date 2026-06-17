from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from app.services.llm_service import agent_factory
from app.knowledge.base import KNOWLEDGE_BASE
import json

router = APIRouter()


class RagRequest(BaseModel):
    question: str = Field(..., min_length=2, description="用户问题")
    stream: bool = Field(default=True, description="是否流式返回")


def _tokenize(text: str) -> set:
    """简单中文分词：2-3字滑动窗口"""
    tokens = set(text)
    for n in [2, 3]:
        for i in range(len(text) - n + 1):
            tokens.add(text[i:i + n])
    return tokens


def _match(question: str, top_k: int = 3):
    q_tokens = _tokenize(question)
    scored = []
    for item in KNOWLEDGE_BASE:
        text = item["title"] + " " + item["content"]
        i_tokens = _tokenize(text)
        hits = len(q_tokens & i_tokens)
        score = hits / max(len(q_tokens), 1)
        scored.append((score, item))
    scored.sort(key=lambda x: x[0], reverse=True)
    return [dict(item, score=round(s, 3)) for s, item in scored[:top_k] if s > 0.05]


def _build_prompt(question: str, results: list) -> str:
    if results and results[0]["score"] > 0.15:
        context_parts = []
        for i, r in enumerate(results, 1):
            context_parts.append(f"[{i}] {r['title']}（{r['source']}）\n{r['content']}")
        context = "\n\n".join(context_parts)
        return f"""你是 PromptHub 网站的智能助手。请根据以下知识库内容并结合你自己的知识回答用户问题。

知识库：
{context}

用户问题：{question}

要求：基于知识库准确回答，知识库没提到的部分用你自己的知识补充。200 字以内，末尾标注来源如 [来源: xxx]。"""
    else:
        return f"""你是 PromptHub 网站的智能助手。知识库中没有找到相关记录，请根据你自己的知识回答。

用户问题：{question}

要求：200 字以内，告诉用户你所知道的信息。不知道就说不知道。"""


@router.post("/rag")
async def rag_search(req: RagRequest):
    results = _match(req.question)
    prompt = _build_prompt(req.question, results)

    if not req.stream:
        llm = agent_factory.supporter()
        resp = agent_factory.invoke(llm, prompt)
        return {"code": 200, "message": "success",
                "data": {"answer": resp.content, "sources": results}}

    llm = agent_factory.supporter()

    async def event_stream():
        buffer = ""
        async for chunk in agent_factory.astream(llm, prompt):
            text = chunk.content if hasattr(chunk, 'content') else str(chunk)
            if not text or not text.strip():
                continue
            buffer += text
            yield f"data: {json.dumps({'text': text}, ensure_ascii=False)}\n\n"
        yield f"data: {json.dumps({'done': True, 'full': buffer, 'sources': results}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
