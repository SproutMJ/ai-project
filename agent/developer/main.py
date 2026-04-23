from fastapi import FastAPI
from pydantic import BaseModel

from developer.planner import plan
from developer.coder import generate_patch
from developer.patcher import apply_patch
from developer.tester import run_tests
from developer.utils import read_files

app = FastAPI()

MAX_RETRY = 3


class RunRequest(BaseModel):
    task: str

@app.post("/run")
def run_agent(req: RunRequest):
    task = req.task

    for i in range(MAX_RETRY):
        print(f"\n=== ITERATION {i} ===")

        # 1. plan
        plan_result = plan(task)
        print(f"\n=== PLAN RESULT === \n{plan_result}")

        # 2. 파일 읽기
        files = read_files(plan_result["target_files"])

        # 3. 코드 생성
        patch = generate_patch(task, files)
        print(f"\n=== PATCH RESULT === \n{patch}")

        # 4. 패치 적용
        apply_patch(patch)

        # 5. 테스트
        result = run_tests()

        if result["success"]:
            return {
                "status": "success",
                "iteration": i
            }

        # 실패 로그를 다음 입력에 추가
        task += f"\n[test fail log]\n{result['output']}"

    return {
        "status": "failed",
        "message": "max retry reached"
    }