import os
from dotenv import load_dotenv

load_dotenv()

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1")
DEFAULT_MODEL = os.getenv("DEFAULT_MODEL", "gpt-4o")
CHEAP_MODEL = os.getenv("CHEAP_MODEL", "gpt-4o-mini")
REDIS_URL = os.getenv("REDIS_URL", "redis://127.0.0.1:6379/0")
