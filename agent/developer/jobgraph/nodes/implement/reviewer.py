from developer.logger import agent_logger


def reviewer_node(state):
    agent_logger.info('[reviewer] 시작')
    patch = state["patch"]

    # 최소 검증 (LLM 안 써도 됨)
    if not patch.get("changes"):
        agent_logger.info('[reviewer] 리뷰 실패(패치 비어있음)')
        return {
            "review": {"ok": False, "reason": "empty patch"}
        }

    for change in patch["changes"]:
        if "file" not in change or "new_code" not in change:
            agent_logger.info('[reviewer] 리뷰 실패(포맷 틀림)')
            return {
                "review": {"ok": False, "reason": "invalid format"}
            }

    agent_logger.info('[reviewer] 리뷰 성공')
    return {
        "review": {"ok": True}
    }