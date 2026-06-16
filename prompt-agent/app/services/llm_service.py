from langchain_openai import ChatOpenAI
from app.config import OPENAI_API_KEY, OPENAI_BASE_URL, DEFAULT_MODEL, CHEAP_MODEL


class LLMService:
    def __init__(self):
        self.default_llm = ChatOpenAI(
            api_key=OPENAI_API_KEY,
            base_url=OPENAI_BASE_URL,
            model=DEFAULT_MODEL,
            temperature=0.7
        )
        self.cheap_llm = ChatOpenAI(
            api_key=OPENAI_API_KEY,
            base_url=OPENAI_BASE_URL,
            model=CHEAP_MODEL,
            temperature=0.3
        )

    def get_default_llm(self):
        return self.default_llm

    def get_cheap_llm(self):
        return self.cheap_llm


llm_service = LLMService()
