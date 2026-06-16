from app.services.llm_service import llm_service
import json


class PromptEvaluator:
    def evaluate(self, prompt_content: str, test_cases: list = None) -> dict:
        llm = llm_service.get_default_llm()
        system_prompt = """你是一个提示词质量评估专家。请从以下维度评估提示词：
1. 清晰度 (clarity): 指令是否明确
2. 完整性 (completeness): 是否包含足够上下文
3. 有效性 (effectiveness): 是否能产生预期输出
4. 通用性 (universality): 是否适用于多种场景
每个维度1-10分，最后给出总分和改善建议。"""

        user_message = f"请评估以下提示词：\n\n{prompt_content}\n"
        if test_cases:
            user_message += f"\n测试用例：{json.dumps(test_cases, ensure_ascii=False)}"
        user_message += "\n\n请返回JSON格式：{\"total_score\": 85, \"dimensions\": {\"clarity\": 8, \"completeness\": 7, \"effectiveness\": 9, \"universality\": 8}, \"suggestions\": [\"建议1\", \"建议2\"]}"

        response = llm.invoke([("system", system_prompt), ("user", user_message)])
        try:
            return json.loads(response.content)
        except Exception:
            return {"total_score": 0, "dimensions": {}, "suggestions": []}


evaluator = PromptEvaluator()
