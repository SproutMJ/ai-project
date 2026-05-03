import json

from developer.jobgraph.state import FlowMode
from developer.logger import agent_logger
from developer.llm import chat
from developer.utils import load_prompt, load_structure


def planner_node(state):
    agent_logger.info('[planner] 시작')
    template = load_prompt("planner.txt")

    package_structure = load_structure('package-structure.md')
    erd = load_structure('erd.md')
    prompt = template.format(
        task=state['task'],
        package_structure=package_structure,
        erd=erd
    )

    res = chat([{"role": "user", "content": prompt}], model='qwen3.6:35b')
    plan = json.loads(res)

    agent_logger.info(f'[planner] 플래너 플랜 결과 \n{res}\n 플랜 끝')

    return {
        "plan": plan,
        "flow_mode": None,
    }
