from fastapi import APIRouter
from pydantic import BaseModel, Field
from app.services.llm_service import agent_factory
import hashlib
import json

router = APIRouter()


class RouteRequest(BaseModel):
    message: str = Field(..., min_length=2, description="用户输入的自然语言")


SYSTEM_PROMPT = """你是PromptHub意图路由分类器。分析用户输入，判断用户想做什么。

分类规则（target.action）：
  agent.generate    生成/写/帮我做/创作/制作 提示词或模板
  agent.optimize    优化/改进/润色/修改 提示词
  agent.classify    分类/打标签/归类 提示词
  agent.debug       测试/运行/试一下/调试 提示词
  agent.evaluate    评价/评分/评测 提示词质量
  market.search     搜索/找/有没有/求/看看 提示词
  market.buy        买/购买/付钱/下单
  account.balance   余额/多少钱/还剩多少
  account.recharge  充值/充钱/加钱
  vip.buy           VIP/会员/升级/全部免费/订阅
  knowledge.ask     问网站怎么用/怎么发布/怎么购买/怎么充值/有什么功能/VIP是什么/退款/怎么联系/技术架构/怎么操作/使用帮助/新手引导/教程
  unknown           不明确/闲聊/不知道要干什么

参数提取：
  将用户输入中的关键信息提取到params：
    keyword: 搜索关键词
    intent: 生成目标
    topic: 主题

严格返回JSON，不要任何解释文本：
{"target":"market","action":"search","params":{"keyword":"PPT"},"confidence":0.9}"""


CACHE_TTL = 600


@router.post("/route")
async def route_intent(req: RouteRequest):
    message = req.message.strip()

    key = "route:" + hashlib.md5(message.encode()).hexdigest()[:12]

    try:
        from app.main import app
        redis = app.state.redis
        cached = await redis.get(key)
        if cached:
            return {"code": 200, "message": "success", "data": json.loads(cached)}
    except Exception:
        pass

    llm = agent_factory.cheap()
    response = agent_factory.invoke(llm, [
        ("system", SYSTEM_PROMPT),
        ("user", message)
    ])

    raw = response.content.strip()
    if raw.startswith("```"):
        raw = raw.split("\n", 1)[1]
        if raw.endswith("```"):
            raw = raw[:-3]
    result = json.loads(raw)

    result.setdefault("target", "unknown")
    result.setdefault("action", "unknown")
    result.setdefault("params", {})
    result.setdefault("confidence", 0.0)
    result["original"] = message

    try:
        from app.main import app
        redis = app.state.redis
        await redis.set(key, json.dumps(result, ensure_ascii=False), ex=CACHE_TTL)
    except Exception:
        pass

    return {"code": 200, "message": "success", "data": result}
