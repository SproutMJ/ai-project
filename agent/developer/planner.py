import json
from developer.llm import chat

def plan(task: str):
    prompt = f"""
    You are a backend task planner.

    [Task]
    {task}

    [Project Structure]
    - Java Spring project
    - Root directory: ai-project/backend/trip
    - Source code Root: /src/main/java/org/mj/trip
    - Test code Root: /src/test/java/org/mj/trip

    IMPORTANT RULES:
    - Do NOT wrap the response in markdown code blocks
    - Output raw JSON only
    - Only use existing files under the project
    - DO NOT create arbitrary paths like src/services or tests/unit
    - All file paths MUST be relative to the backend/trip directory.
    - Target existing classes like TripService.java

    Respond ONLY in valid JSON:
    {{
      "goal": "...",
      "target_files": ["real file path"],
      "steps": ["..."],
      "risks": ["..."]
    }}
    """
    res = chat([{"role": "user", "content": prompt}], model='qwen3.6:35b')

    print("=== RAW LLM RESPONSE (need erase after) ===")
    print(res)

    return json.loads(res)