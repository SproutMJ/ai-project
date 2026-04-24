import json
from developer.llm import chat
from developer.utils import load_prompt, load_structure


def generate_patch(task, files):
    template = load_prompt("coder.txt")
    package_structure = load_structure('package-structure.md')

    prompt = template.format(
        task=task,
        files=files,
        package_structure = package_structure
    )
    print('=== coder prompt (need erase after) ===')
    print(prompt)
    res = chat([{"role": "user", "content": prompt}], model='qwen3-coder:30b')

    print("=== RAW coder LLM RESPONSE (need erase after) ===")
    print(res)

    return json.loads(res)