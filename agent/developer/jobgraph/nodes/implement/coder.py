import json
from developer.llm import chat
from developer.logger import agent_logger
from developer.utils import load_prompt, load_structure


def coder_node(state: dict):
    agent_logger.info('[coder] coding 시작')
    template = load_prompt("coder.txt")
    package_structure = load_structure('package-structure.md')
    erd = load_structure('erd.md')

    coding_step_idx = state["coding_step"]
    coding_step = state["plan"]["coding_steps"][coding_step_idx]

    files_context = {}
    for file_path in coding_step.get("target_files", []):
        try:
            with open(file_path) as f:
                files_context[file_path] = f.read()
        except:
            files_context[file_path] = ""

    prompt = template.format(
        subtask_title=coding_step["subtask_title"],
        package_structure=package_structure,
        acceptance=coding_step["acceptance"],
        files=json.dumps(files_context, indent=2)
    )
    agent_logger.info(f'[coder]코더 프롬프트 \n{prompt}\n 프롬프트 끝')


    res = chat([{"role": "user", "content": prompt}], model='qwen3-coder:30b')
    patch = json.loads(res)

    agent_logger.info(f'[coder] 코더 결과 \n{res}\n 결과 끝')


    return {
        "patch": patch,
    }