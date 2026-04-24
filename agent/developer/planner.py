import json
from developer.llm import chat
from developer.utils import load_prompt, load_structure


def plan(task: str):

    template = load_prompt("planner.txt")
    package_structure = load_structure('package-structure.md')
    erd = load_structure('erd.md')
    prompt = template.format(
        task=task,
        package_structure = package_structure,
        erd = erd
    )
    res = chat([{"role": "user", "content": prompt}], model='qwen3.6:35b')

    print("=== RAW LLM RESPONSE (need erase after) ===")
    print(res)

    return json.loads(res)