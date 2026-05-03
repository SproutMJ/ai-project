import json

from developer.logger import agent_logger
from developer.llm import chat

def code_fixer_node(state: dict):
    CODER_PROMPT = """
    You are a senior backend code modification agent.

    Your job is to modify existing backend source code with minimal and safe changes.

    Important rules:
    - Use the analysis result below as the only repair guidance.
    - Do NOT split the work into multiple steps.
    - Apply the fix in one pass.
    - Do NOT redesign the whole feature.
    - Do NOT change unrelated code.
    - Make the smallest possible change that satisfies the analysis.
    - Preserve existing package declarations, imports, formatting style, and public APIs unless the repair explicitly requires a change.
    - If multiple files are needed, output all of them in one JSON response.
    - Output raw JSON only. No markdown code blocks.

    [Repair Analysis]
    {analysis}
    
    [File Context]
    {files}


    Return ONLY valid JSON in this exact format:
    {{
      "changes": [
        {{
          "file": "src/main/java/org/mj/trip/example.java",
          "new_code": "full updated file content"
        }}
      ]
    }}
    """

    """
        입력:
          - state["solution_analysis"]: fail_analyzer가 만든 단일 해결 방법
          - state["files_context"] 또는 state["current_files_context"]: 수정 대상 파일 내용

        출력:
          - patch: {"changes": [...]} 형식의 전체 파일 교체 패치
        """
    agent_logger.info("[coder] 시작")

    analysis = state["fail_solution_analysis"]

    files_context = {}
    for file_path in state["fail_solution_analysis"].get("file_hints", []):
        try:
            with open(file_path) as f:
                files_context[file_path] = f.read()
        except:
            files_context[file_path] = ""

    prompt = CODER_PROMPT.format(
        analysis=analysis,
        files=files_context,
    )

    agent_logger.info(f"[coder] 프롬프트 생성 완료\n{prompt}\n")

    res = chat(
        [{"role": "user", "content": prompt}],
        model="qwen3-coder:30b"
    )

    agent_logger.info(f"[code fixer] 응답:\n{res}")

    patch = json.loads(res)

    return {
        "patch": patch
    }