from developer.logger import agent_logger
from developer.guard import assert_write, to_absolute

def patcher_node(state):
    agent_logger.info('[patcher] 시작')
    patch = state["patch"]

    try:
        agent_logger.info('[patche] 패치 작업 시작')
        agent_logger.info(f'[patcher] 패치내용\n{patch}\n')
        for change in patch.get("changes", []):
            rel_path = change["file"]
            agent_logger.info(f'[patcher] 파일 상대 경로 \n{rel_path}\n')
            abs_path = to_absolute(rel_path)
            agent_logger.info(f'[patcher] 파일 절대 경로 \n{abs_path}\n')
            assert_write(rel_path)

            abs_path.parent.mkdir(parents=True, exist_ok=True)
            with open(abs_path, "w", encoding="utf-8") as f:
                f.write(change["new_code"])

        agent_logger.info('[patche] 패치 작업 종료')
        return {
            "patch_applied": {"ok": True},
        }

    except Exception as e:
        return {
            "patch_applied": {"ok": False, "patch_error": str(e)},
        }
