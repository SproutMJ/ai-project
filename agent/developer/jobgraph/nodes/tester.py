import subprocess

from pathlib import Path

from developer.logger import agent_logger


def tester_node(state: dict):
    agent_logger.info('[tester] 테스트 시작')
    try:
        agent_logger.info(f'[tester] pwd 결과 \n{Path.cwd()}\n pwd 결과 끝')

        backend_root = Path("/Users/kimminjun/Desktop/ai-project/ai-project/backend/trip")

        result = subprocess.run(
            ["./gradlew", "test"],
            cwd=str(backend_root),
            capture_output=True,
            text=True
        )
        agent_logger.info(f'[tester] 테스트 결과 \n{result.stdout+result.stderr}\n 테스트 결과 끝')

        success = result.returncode == 0

        agent_logger.info(f'[tester] 테스트 결과 = {success}')
        return {
            "test_result": {
                "ok": success,
                "log": result.stdout + result.stderr
            }
        }

    except Exception as e:
        return {
            "test_result": {
                "ok": False,
                "log": str(e)
            }
        }