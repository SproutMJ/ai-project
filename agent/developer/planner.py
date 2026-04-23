import json
from developer.llm import chat
from developer.utils import load_prompt


def plan(task: str):

    template = load_prompt("planner.txt")
    prompt = template.format(
        task=task
    )
    res = chat([{"role": "user", "content": prompt}], model='qwen3.6:35b')

    print("=== RAW LLM RESPONSE (need erase after) ===")
    print(res)

    return json.loads(res)