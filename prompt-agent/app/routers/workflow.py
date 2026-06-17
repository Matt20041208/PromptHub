from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field
from app.services.llm_service import agent_factory
from app.services.generator import generator
from app.services.optimizer import optimizer
from app.services.evaluator import evaluator
import json
import httpx

router = APIRouter()


class WorkflowRequest(BaseModel):
    intent: str = Field(..., min_length=5, description="提示词意图")
    style: str = Field(default="", description="期望风格")
    auto_publish: bool = Field(default=False, description="评分达标自动发布")
    user_id: int = Field(default=1, description="发布者用户 ID")


@router.post("/workflow/publish")
async def publish_workflow(req: WorkflowRequest):

    async def event_stream():
        report = {"steps": [], "final_score": 0, "published": False}

        def emit(step: str, status: str, data: dict = None):
            report["steps"].append({"step": step, "status": status, "data": data})
            return f"data: {json.dumps({'step': step, 'status': status, 'data': data}, ensure_ascii=False)}\n\n"

        # Step 1: 生成
        yield emit("generate", "running")
        try:
            gen_result = generator.generate(req.intent)
            yield emit("generate", "done", {"template": gen_result.get("template", "")})
        except Exception as e:
            yield emit("generate", "error", {"msg": str(e)})
            return

        # Step 2: 优化
        yield emit("optimize", "running")
        try:
            opt_llm = agent_factory.optimizer()
            opt_prompt = f"请优化以下提示词，使其更加清晰、完整、有效：\n\n{gen_result.get('template', '')}"
            if req.style:
                opt_prompt += f"\n\n期望风格：{req.style}"
            opt_response = agent_factory.invoke(opt_llm, opt_prompt)
            opt_text = opt_response.content.strip()
            yield emit("optimize", "done", {"optimized": opt_text})
        except Exception as e:
            yield emit("optimize", "error", {"msg": str(e)})
            return

        # Step 3: 评测
        yield emit("evaluate", "running")
        try:
            eval_result = evaluator.evaluate(opt_text)
            total = eval_result.get("total_score", 0)
            dims = eval_result.get("dimensions", {})
            report["final_score"] = total
            yield emit("evaluate", "done", {"score": total, "dimensions": dims, "suggestions": eval_result.get("suggestions", [])})
        except Exception as e:
            yield emit("evaluate", "error", {"msg": str(e)})
            return

        # Step 4: 发布
        if req.auto_publish and total >= 70:
            yield emit("publish", "running")
            try:
                async with httpx.AsyncClient() as client:
                    resp = await client.post(
                        "http://127.0.0.1:9102/api/prompt",
                        json={
                            "title": f"{req.intent}提示词",
                            "description": f"由 AI 自动生成并优化的{req.intent}提示词",
                            "content": opt_text,
                            "price": 0,
                        },
                        headers={"X-User-Id": str(req.user_id)},
                        timeout=10
                    )
                if resp.status_code == 200:
                    report["published"] = True
                    yield emit("publish", "done", {"msg": "已发布到市场"})
                else:
                    yield emit("publish", "error", {"msg": f"发布失败: HTTP {resp.status_code}"})
            except Exception as e:
                yield emit("publish", "error", {"msg": str(e)})
        else:
            auto_msg = "评分未达标(需≥70)，请手动修改后发布" if req.auto_publish else "可手动发布到市场"
            yield emit("publish", "skipped", {"msg": auto_msg})

        yield f"data: {json.dumps({'done': True, 'report': report}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")
