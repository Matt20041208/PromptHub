from langchain_openai import ChatOpenAI
from langchain.agents import create_openai_functions_agent, AgentExecutor
from langchain.tools import tool
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from app.config import OPENAI_API_KEY, OPENAI_BASE_URL, DEFAULT_MODEL


@tool
def rate_dimension(dimension: str, score: str, reason: str) -> str:
    """记录单个维度的评分和理由，dimension为维度名称，score为1-10的分数，reason为评分理由。"""
    return f"维度[{dimension}]评分：{score}/10，理由：{reason}"


@tool
def check_completeness(prompt: str) -> str:
    """检查提示词的完整性，检查是否包含关键要素：目标、上下文、输出格式、约束条件。"""
    checks = []
    has_goal = any(kw in prompt for kw in ["生成", "写", "创建", "分析", "generate", "write", "create", "analyze"])
    checks.append(f"目标明确：{'是' if has_goal else '否'}")
    checks.append(f"上下文：{'是' if len(prompt) > 50 else '否'}")
    has_format = "格式" in prompt or "format" in prompt.lower()
    checks.append(f"输出格式：{'是' if has_format else '否'}")
    has_constraint = any(kw in prompt for kw in ["不要", "限制", "只有", "仅", "don't", "only"])
    checks.append(f"约束条件：{'是' if has_constraint else '否'}")
    result = "完整性检查结果：\n" + "\n".join(f"  - {c}" for c in checks)
    score = sum(1 for c in checks if c.endswith("是")) / len(checks) * 10
    result += f"\n  完整性评分：{score:.0f}/10"
    return result


@tool
def suggest_improvement(prompt: str, weakness: str) -> str:
    """基于提示词的弱点给出改进建议。weakness为识别到的弱点描述。"""
    suggestions = {
        "不清晰": "建议在提示词中明确说明期望的输出格式和内容结构",
        "缺少上下文": "建议添加背景信息和具体场景说明",
        "缺少约束": "建议添加限制条件，如字数、风格、禁止事项等",
        "缺少示例": "建议添加1-2个输入输出示例",
        "指令模糊": "建议使用具体、可操作的动词描述任务",
    }
    advice = suggestions.get(weakness, f"针对'{weakness}'的改进建议：请补充更多细节和具体要求")
    return advice


def create_evaluator_agent():
    llm = ChatOpenAI(api_key=OPENAI_API_KEY, base_url=OPENAI_BASE_URL, model=DEFAULT_MODEL, temperature=0.5)
    tools = [rate_dimension, check_completeness, suggest_improvement]

    prompt = ChatPromptTemplate.from_messages([
        ("system", "你是一个提示词质量评估专家。使用工具从清晰度、完整性、有效性、通用性四个维度评估提示词，给出综合评分和改进建议。"),
        ("user", "{input}"),
        MessagesPlaceholder(variable_name="agent_scratchpad"),
    ])

    agent = create_openai_functions_agent(llm, tools, prompt)
    return AgentExecutor(agent=agent, tools=tools, verbose=True)


evaluator_agent = create_evaluator_agent()
