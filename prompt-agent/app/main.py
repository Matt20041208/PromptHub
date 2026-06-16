from fastapi import FastAPI
from app.routers import optimize, classify, evaluate, generate, debug

app = FastAPI(title="Prompt Agent", description="AI智能体服务 - 提示词优化/分类/评测/生成/调试", version="1.0.0")

app.include_router(optimize.router, prefix="/api/agent", tags=["优化"])
app.include_router(classify.router, prefix="/api/agent", tags=["分类"])
app.include_router(evaluate.router, prefix="/api/agent", tags=["评测"])
app.include_router(generate.router, prefix="/api/agent", tags=["生成"])
app.include_router(debug.router, prefix="/api/agent", tags=["调试"])


@app.get("/api/agent/health")
async def health():
    return {"status": "ok"}
