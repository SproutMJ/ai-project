import json
from developer.llm import chat
from developer.utils import load_prompt


def generate_patch(task, files):
    template = load_prompt("coder.txt")

    prompt = template.format(
        task=task,
        files=files
    )
    print('=== coder prompt (need erase after) ===')
    print(prompt)
    res = chat([{"role": "user", "content": prompt}], model='qwen3-coder:30b')

    print("=== RAW coder LLM RESPONSE (need erase after) ===")
    print(res)

    return json.loads(res)