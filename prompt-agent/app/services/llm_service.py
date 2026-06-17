from langchain_openai import ChatOpenAI
from openai import RateLimitError, AuthenticationError, BadRequestError, APITimeoutError, APIConnectionError
from app.config import OPENAI_API_KEY, OPENAI_BASE_URL
from typing import Dict, Optional, Callable, Any
import time
import asyncio


class AgentFactory:
    """5 个专用 Agent + 统一重试机制"""

    _instances: Dict[str, ChatOpenAI] = {}
    MAX_RETRIES = 3

    @classmethod
    def _get_or_create(cls, name: str, model: str, temperature: float, **kwargs) -> ChatOpenAI:
        key = f"{name}:{model}:{temperature}"
        if key not in cls._instances:
            cls._instances[key] = ChatOpenAI(
                api_key=OPENAI_API_KEY,
                base_url=OPENAI_BASE_URL,
                model=model,
                temperature=temperature,
                **kwargs
            )
        return cls._instances[key]

    # ============ Agent 实例 ============

    @classmethod
    def creator(cls) -> ChatOpenAI:
        return cls._get_or_create("creator", "deepseek-v4-pro", 0.8)

    @classmethod
    def optimizer(cls) -> ChatOpenAI:
        return cls._get_or_create("optimizer", "deepseek-v4-pro", 0.4)

    @classmethod
    def evaluator(cls) -> ChatOpenAI:
        return cls._get_or_create("evaluator", "deepseek-v4-pro", 0.0)

    @classmethod
    def trader(cls) -> ChatOpenAI:
        return cls._get_or_create("trader", "deepseek-v4-pro", 0.0)

    @classmethod
    def supporter(cls) -> ChatOpenAI:
        return cls._get_or_create("supporter", "deepseek-v4-pro", 0.5)

    @classmethod
    def cheap(cls) -> ChatOpenAI:
        return cls._get_or_create("cheap", "deepseek-v4-pro", 0.0)

    # ============ 带重试的同步调用 ============

    @staticmethod
    def _should_retry(error: Exception) -> bool:
        return isinstance(error, (RateLimitError, APITimeoutError, APIConnectionError))

    @staticmethod
    def _should_not_retry(error: Exception) -> bool:
        return isinstance(error, (AuthenticationError, BadRequestError))

    @classmethod
    def invoke(cls, llm: ChatOpenAI, messages: list) -> Any:
        last_error = None
        for attempt in range(cls.MAX_RETRIES):
            try:
                return llm.invoke(messages)
            except Exception as e:
                last_error = e
                if cls._should_not_retry(e):
                    raise
                if not cls._should_retry(e) and attempt == cls.MAX_RETRIES - 1:
                    raise
                time.sleep(2 ** attempt)
        raise last_error

    @classmethod
    async def astream(cls, llm: ChatOpenAI, messages: list):
        last_error = None
        for attempt in range(cls.MAX_RETRIES):
            try:
                async for chunk in llm.astream(messages):
                    yield chunk
                return
            except Exception as e:
                last_error = e
                if cls._should_not_retry(e):
                    raise
                if not cls._should_retry(e) and attempt == cls.MAX_RETRIES - 1:
                    raise
                yield {"_retry": f"服务繁忙，正在重试（{attempt + 1}/{cls.MAX_RETRIES}）..."}
                await asyncio.sleep(2 ** attempt)
        raise last_error


agent_factory = AgentFactory()
