from celery import Celery
from app.config import REDIS_URL
import json

celery_app = Celery('prompt_agent', broker=REDIS_URL, backend=REDIS_URL)


@celery_app.task(name='batch_evaluate')
def batch_evaluate(task_data: dict):
    """异步批量评测任务"""
    prompts = task_data.get('prompts', [])
    results = []
    from app.services.evaluator import evaluator
    for prompt in prompts:
        result = evaluator.evaluate(prompt.get('content', ''))
        result['prompt_id'] = prompt.get('id')
        results.append(result)
    return json.dumps(results, ensure_ascii=False, default=str)


@celery_app.task(name='batch_optimize')
def batch_optimize(task_data: dict):
    """异步批量优化任务"""
    prompts = task_data.get('prompts', [])
    results = []
    from app.services.optimizer import optimizer
    for prompt in prompts:
        content = prompt.get('content', '')
        intent = prompt.get('intent', '')
        style = prompt.get('style', '')
        result = optimizer.optimize(content, intent, style)
        result['prompt_id'] = prompt.get('id')
        results.append(result)
    return json.dumps(results, ensure_ascii=False, default=str)


@celery_app.task(name='batch_classify')
def batch_classify(task_data: dict):
    """异步批量分类任务"""
    prompts = task_data.get('prompts', [])
    results = []
    from app.services.classifier import classifier
    for prompt in prompts:
        result = classifier.classify(
            prompt.get('title', ''),
            prompt.get('description', ''),
            prompt.get('content', '')
        )
        result['prompt_id'] = prompt.get('id')
        results.append(result)
    return json.dumps(results, ensure_ascii=False, default=str)
