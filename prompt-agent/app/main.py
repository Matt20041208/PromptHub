from fastapi import FastAPI
from app.routers import optimize, classify, evaluate, generate, debug, route, chat, rag, workflow

app = FastAPI(title="Prompt Agent", description="AI智能体服务 - 多Agent架构", version="1.0.0")

app.include_router(optimize.router, prefix="/api/agent", tags=["优化"])
app.include_router(classify.router, prefix="/api/agent", tags=["分类"])
app.include_router(evaluate.router, prefix="/api/agent", tags=["评测"])
app.include_router(generate.router, prefix="/api/agent", tags=["生成"])
app.include_router(debug.router, prefix="/api/agent", tags=["调试"])
app.include_router(route.router, prefix="/api/agent", tags=["路由"])
app.include_router(chat.router, prefix="/api/agent", tags=["对话"])
app.include_router(rag.router, prefix="/api/agent", tags=["知识库"])
app.include_router(workflow.router, prefix="/api/agent", tags=["工作流"])


@app.get("/api/agent/health")
async def health():
    return {"status": "ok"}
