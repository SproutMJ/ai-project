from developer.jobgraph.graph import build_graph

graph = build_graph()

def run_agent(task: str):
    initial_state = {
        "task": task,
        "plan": None,
        "patch": None,
        "review": None,
        "test_result": None,
        "iteration": 0,
        "max_iterations": 5
    }

    result = graph.invoke(initial_state)

    return result