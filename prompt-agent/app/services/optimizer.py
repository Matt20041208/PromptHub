from app.services.llm_service import agent_factory
import json


class PromptOptimizer:
    def optimize(self, prompt: str, intent: str = "", style: str = "") -> dict:
        llm = agent_factory.optimizer()
        system_prompt = "你是一个专业的提示词优化师。请优化用户提供的提示词，使其更加清晰、完整、有效。"
        user_message = f"原始提示词：\n{prompt}\n"
        if intent:
            user_message += f"\n目标意图：{intent}"
        if style:
            user_message += f"\n期望风格：{style}"
        user_message += "\n\n请返回JSON格式：{\"optimized\": \"优化后的提示词\", \"changes\": [\"改动1说明\", \"改动2说明\"], \"score\": 85}"

        response = agent_factory.invoke(llm, [("system", system_prompt), ("user", user_message)])
        try:
            result = json.loads(response.content)
            return result
        except Exception:
            return {"optimized": response.content, "changes": [], "score": 0}


optimizer = PromptOptimizer()
