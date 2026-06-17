from app.services.llm_service import agent_factory
import json


class PromptClassifier:
    def classify(self, title: str, description: str, content: str) -> dict:
        llm = agent_factory.cheap()
        system_prompt = """你是一个提示词分类和标签专家。请根据提示词内容推荐分类和标签。
分类选项：写作创作、编程开发、商业营销、教育培训、设计创意、生活娱乐、学术研究、其他
标签：选择3-5个最合适的标签。"""

        user_message = f"""标题：{title}
描述：{description}
内容：{content}

请返回JSON格式：{{"category": "写作创作", "tags": ["文案", "创意", "营销"]}}"""

        response = agent_factory.invoke(llm, [("system", system_prompt), ("user", user_message)])
        try:
            return json.loads(response.content)
        except Exception:
            return {"category": "其他", "tags": []}


classifier = PromptClassifier()
