from typing import TypedDict, Optional, Dict, Any

class AgentState(TypedDict):
    task: str

    plan: Optional[Dict[str, Any]]
    # example
    # {
    #     "goal": "...",
    #     "coding_steps": [
    #         {
    #             "index": "0",
    #             "subtask_title": "...",
    #             "target_files": ["...", "..."],
    #             "acceptance": ["...", "..."],
    #             "depends_on": []
    #         }
    #     ],
    #     "risks": ["..."]
    # }
    flow_mode: str
    coding_step: int
    iteration: int

    patch: Optional[Dict[str, Any]]
    # maked code example
    # {
    #     "changes": [
    #         {
    #             "file": "src/main/java/org/mj/trip/example.java",
    #             "new_code": "full updated file content"
    #         }
    #     ]
    # }

    review: Optional[Dict[str, Any]]
    test_result: Optional[Dict[str, Any]]
    patch_applied: Optional[Dict[str, Any]]

    fail_solution_analysis: Optional[Dict[str, Any]]

    max_iterations: int

from enum import Enum

class FlowMode(str, Enum):
    IMPLEMENT = "implement"
    REPAIR = "repair"
    END = "END"