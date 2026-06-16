from app.services.llm_service import llm_service
import json


class TemplateGenerator:
    def generate(self, intent: str, variables: list = None) -> dict:
        llm = llm_service.get_default_llm()
        system_prompt = """你是一个提示词模板生成专家。请根据用户意图生成一个带变量的提示词模板。
在模板中使用 {{变量名}} 格式表示需要用户填充的变量。
同时生成 template_schema (JSON Schema格式) 描述每个变量的类型和说明。"""

        user_message = f"意图：{intent}\n"
        if variables:
            user_message += f"要求的变量：{', '.join(variables)}\n"
        user_message += "\n请返回JSON格式：{\"template\": \"请用{{style}}风格写一首关于{{topic}}的诗\", \"schema\": {\"variables\": [{\"name\": \"style\", \"type\": \"string\", \"description\": \"写作风格\"}]}, \"description\": \"模板使用说明\"}"

        response = llm.invoke([("system", system_prompt), ("user", user_message)])
        try:
            return json.loads(response.content)
        except Exception:
            return {"template": "", "schema": {}, "description": ""}


generator = TemplateGenerator()
