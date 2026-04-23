# app/coder.py
import json
from developer.llm import chat

def generate_patch(task, files):
    prompt = f"""
    You are a senior backend code modification agent.

    Your job is to modify existing backend source code with minimal and safe changes.

    IMPORTANT RULES:
    - Do NOT wrap the response in markdown code blocks.
    - Output raw JSON only.
    - Do NOT invent new files unless the task explicitly requires a new file.
    - Do NOT change unrelated code.
    - Preserve existing package declarations, imports, formatting style, and public APIs unless the task explicitly requires a change.
    - Make the smallest possible change that satisfies the task.
    - All file paths must be relative to the backend/trip directory.
    - Use paths that start with "src/" only.
    - If a file does not need to be changed, do not include it.
    - If the task is ambiguous, choose the safest minimal implementation.

    [Task]
    {task}

    [Current Code]
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
    print('=== coder prompt (need erase after) ===')
    print(prompt)
    res = chat([{"role": "user", "content": prompt}], model='qwen3-coder:30b')

    print("=== RAW coder LLM RESPONSE (need erase after) ===")
    print(res)

    return json.loads(res)