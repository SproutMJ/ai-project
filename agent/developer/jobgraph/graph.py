from langgraph.graph import StateGraph, END

from developer.jobgraph.nodes.patcher import patcher_node
from developer.jobgraph.nodes.repair.code_fixer import code_fixer_node
from developer.jobgraph.nodes.repair.fail_analyzer import fail_analyzer_node
from developer.jobgraph.nodes.repair.progress_evaluator import progress_evaluator_node
from developer.jobgraph.nodes.repair.repair_planner import repair_planner_node
from developer.jobgraph.nodes.router import flow_router_node
from developer.jobgraph.state import AgentState, FlowMode
from developer.jobgraph.nodes.planner import planner_node
from developer.jobgraph.nodes.implement.coder import coder_node
from developer.jobgraph.nodes.implement.reviewer import reviewer_node
from developer.jobgraph.nodes.tester import tester_node


def build_implement_subgraph():
    graph = StateGraph(AgentState)

    graph.add_node("coder", coder_node)
    graph.add_node("patcher", patcher_node)
    graph.add_node("reviewer", reviewer_node)
    graph.add_node("tester", tester_node)

    graph.set_entry_point("coder")

    graph.add_edge("coder", "patcher")
    graph.add_edge("patcher", "reviewer")
    graph.add_edge("reviewer", "tester")

    return graph.compile()


def build_repair_subgraph():
    graph = StateGraph(AgentState)

    graph.add_node("fail_analyzer", fail_analyzer_node)
    # graph.add_node("repair_planner", repair_planner_node)
    graph.add_node("code_fixer", code_fixer_node)
    graph.add_node("patcher", patcher_node)
    graph.add_node("tester", tester_node)
    # graph.add_node("progress_evaluator", progress_evaluator_node)

    graph.set_entry_point("fail_analyzer")

    graph.add_edge("fail_analyzer", "code_fixer")
    # graph.add_edge("repair_planner", "code_fixer")
    graph.add_edge("code_fixer", "patcher")
    graph.add_edge("patcher", "tester")
    # graph.add_edge("tester", "progress_evaluator")

    return graph.compile()


def build_graph():
    main = StateGraph(AgentState)

    implement_subgraph = build_implement_subgraph()
    repair_subgraph = build_repair_subgraph()

    main.add_node("planner", planner_node)
    main.add_node("router", flow_router_node)
    main.add_node("implement", implement_subgraph)
    main.add_node("repair", repair_subgraph)

    main.set_entry_point("planner")

    main.add_edge("planner", "router")

    def after_router(state: AgentState):
        if state['flow_mode'] == FlowMode.IMPLEMENT:
            return "implement"
        if state['flow_mode'] == FlowMode.REPAIR:
            return "repair"
        if state['flow_mode'] == FlowMode.END:
            return END
        raise Exception("not exist router edge error")

    main.add_conditional_edges("router", after_router)
    main.add_edge("implement", "router")
    main.add_edge("repair", "router")

    return main.compile()
