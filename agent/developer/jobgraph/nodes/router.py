from developer.jobgraph.state import FlowMode
from developer.logger import agent_logger


def flow_router_node(state: dict):
    if state["flow_mode"] is None:
        return {
            "coding_step": 0,
            "flow_mode": FlowMode.IMPLEMENT,
        }

    if state["test_result"]["ok"] is True:
        agent_logger.info('[router] 테스트 성공 계속 진행')
        if len(state["plan"]["coding_steps"]) == state["coding_step"]:
            return {
                "flow_mode": FlowMode.END,
            }
        return {
            "coding_step": state["coding_step"] + 1,
            "flow_mode": FlowMode.IMPLEMENT,
        }
    else:
        agent_logger.info('[router] 테스트 실패 복구모드 이동')
        return {
            "flow_mode": FlowMode.REPAIR,
        }
