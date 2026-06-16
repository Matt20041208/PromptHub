from langchain_openai import ChatOpenAI
from langchain.agents import create_openai_functions_agent, AgentExecutor
from langchain.tools import tool
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from app.config import OPENAI_API_KEY, OPENAI_BASE_URL, DEFAULT_MODEL


@tool
def analyze_prompt(prompt: str) -> str:
    """分析提示词的结构和要素，返回结构分析结果。"""
    elements = []
    if "请" in prompt:
        elements.append("包含礼貌请求")
    if "步骤" in prompt or "step" in prompt.lower():
        elements.append("包含步骤指引")
    if "角色" in prompt or "role" in prompt.lower():
        elements.append("包含角色设定")
    if "格式" in prompt or "format" in prompt.lower():
        elements.append("包含输出格式要求")
    if "示例" in prompt or "example" in prompt.lower():
        elements.append("包含示例")
    if not elements:
        elements.append("基础直接指令")
    return f"提示词分析结果：长度={len(prompt)}字符，要素：{', '.join(elements)}"


@tool
def enhance_clarity(prompt: str) -> str:
    """增强提示词的清晰度，添加缺失的结构要素。"""
    enhanced = prompt.strip()
    if "请" not in enhanced:
        enhanced = "请" + enhanced
    if not enhanced.endswith("。") and not enhanced.endswith("."):
        enhanced += "。"
    return f"增强后的清晰版本：{enhanced}"


@tool
def score_prompt(prompt: str) -> str:
    """基于启发式规则对提示词质量进行评分。"""
    score = 50
    feedback = []
    if len(prompt) > 20:
        score += 10
        feedback.append("长度适中")
    else:
        feedback.append("建议增加细节")
    if "角色" in prompt or "作为" in prompt:
        score += 10
        feedback.append("角色设定清晰")
    if "示例" in prompt:
        score += 10
        feedback.append("有示例参考")
    if "格式" in prompt:
        score += 10
        feedback.append("输出格式明确")
    if "限制" in prompt or "不要" in prompt or "避免" in prompt:
        score += 10
        feedback.append("有约束条件")
    return f"评分：{score}/100。反馈：{'；'.join(feedback)}"


def create_optimizer_agent():
    llm = ChatOpenAI(api_key=OPENAI_API_KEY, base_url=OPENAI_BASE_URL, model=DEFAULT_MODEL, temperature=0.7)
    tools = [analyze_prompt, enhance_clarity, score_prompt]

    prompt = ChatPromptTemplate.from_messages([
        ("system", "你是一个专业的提示词优化专家。使用工具分析提示词结构、增强清晰度、进行评分，然后给出综合优化建议。"),
        ("user", "{input}"),
        MessagesPlaceholder(variable_name="agent_scratchpad"),
    ])

    agent = create_openai_functions_agent(llm, tools, prompt)
    return AgentExecutor(agent=agent, tools=tools, verbose=True)


optimizer_agent = create_optimizer_agent()
