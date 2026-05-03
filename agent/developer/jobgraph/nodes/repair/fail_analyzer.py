import json

from developer.logger import agent_logger
from developer.llm import chat

def fail_analyzer_node(state: dict):
    """
        입력:
          - state["test_result"]: {"ok": False, "log": "..."}
          - state["failing_files_context"]: 실패가 난 파일들의 현재 코드 내용(dict)

        출력:
          - solution_analysis: coder가 바로 수정할 수 있게 만든 단일 해결 방향
        """
    FAIL_ANALYZER_PROMPT = """
    You are a backend failure analyzer for a coding agent.

    Your job is to analyze a failure as a regression caused by recent code changes.
    
    Assumptions:
    - The system was working before the last patch.
    - The failure was introduced by the most recent code changes.
    
    Important rules:
    - Do NOT immediately assume the error location file is the root cause.
    - First check whether the issue was introduced by recent changes.
    - Treat the failure as a regression.
    - Prefer the smallest possible fix.
    - Expand scope only if the issue cannot be resolved locally.
    
    Scope definitions:
    - local: only last changed files
    - related: files referenced by changed files (imports, types)
    - domain: same package/domain
    - project: global

    [Test Result Log]
    {test_result}

    [Recently Modified Files]
    {files_context}

    Return ONLY valid JSON in this exact format:
    {{
      "summary": "short summary of the failure",
      "root_cause": "most likely root cause",
      "target_files": ["src/..."],
      "file_hints": [
        {{
          "file": "src/...",
          "what_to_change": "specific change guidance"
        }}
      ]
    }}
    """
    agent_logger.info("[fail_analyzer] 시작")

    test_result = state["test_result"]
    files_context = state["patch"]

    prompt = FAIL_ANALYZER_PROMPT.format(
        test_result=test_result,
        files_context=files_context
    )

    agent_logger.info(f"[fail_analyzer] 프롬프트 생성 완료 \n프롬프트:\n{prompt}\n")

    res = chat(
        [{"role": "user", "content": prompt}],
        model="qwen3.6:35b"
    )

    agent_logger.info(f"[fail_analyzer] 응답:\n{res}")

    analysis = json.loads(res)

    return {
        "fail_solution_analysis": analysis
    }