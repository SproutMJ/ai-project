# [모니터링] Phoenix & OpenTelemetry 초기화
from phoenix.otel import register
from openinference.instrumentation.smolagents import SmolagentsInstrumentor

# docker-compose.yml에서 주입한 환경 변수를 자동으로 읽어 Mac의 Phoenix로 연결됩니다.
register()
SmolagentsInstrumentor().instrument()


from smolagents import CodeAgent, LiteLLMModel, OpenAIModel, GradioUI, WebSearchTool, ToolCallingAgent

model = LiteLLMModel(
    model_id="ollama_chat/qwen3.6:35b-a3b", # This model is a bit weak for agentic behaviours though
    api_base="http://:11434", # replace with 127.0.0.1:11434 or remote open-ai compatible server if necessary
    api_key="YOUR_API_KEY", # replace with API key if necessary
    num_ctx=32000, # ollama default is 2048 which will fail horribly. 8192 works for easy tasks, more is better. Check https://huggingface.co/spaces/NyxKrage/LLM-Model-VRAM-Calculator to calculate how much VRAM this will need for the selected model.

    temperature=0.1,
    top_p=0.5,

    flatten_messages_as_text=False,
    # chat_template_kwargs={"enable_thinking": False},
)

model2 = OpenAIModel(
    model_id="qwen3.6:35b-a3b", # This model is a bit weak for agentic behaviours though
    api_base="http://:11434/v1", # replace with 127.0.0.1:11434 or remote open-ai compatible server if necessary

    api_key="YOUR_API_KEY",
    temperature=0.1,
    max_tokens=32000,
    top_p=0.5,

    parallel_tool_calls=False,

    reasoning_effort="none",
)

model3 = OpenAIModel(
    model_id="unsloth/Qwen3.6-35B-A3B-GGUF:UD-Q4_K_M", # This model is a bit weak for agentic behaviours though
    api_base="http://:8080/v1", # replace with 127.0.0.1:11434 or remote open-ai compatible server if necessary

    api_key="YOUR_API_KEY",
    temperature=0.1,
    max_tokens=32000,
    top_p=0.5,

    parallel_tool_calls=False,

    reasoning_effort="none",
)


system_prompt2 = """
# 🎯 ROLE & MISSION
You are the Lead Project Orchestrator Agent. Your primary mission is to actively use the provided tools to the user's request.
Do not rely solely on your internal knowledge; if a task requires codebase analysis, code modification, or testing, you MUST trigger the appropriate tool immediately.
After successfully executing tools and receiving their final output, you MUST synthesize results into a clear, natural language summary for the user.

# 🚨 GLOBAL SAFETY & ERROR HANDLING
- Do not blindly retry the exact same tool call with the same parameters.
- Instead, immediately output a final text response to the user explaining the exact error and await their decision.

# 🧠 GENERAL ENGINEERING DIRECTIVES
- **Strike the 'Root Cause', Not the Symptom:** Avoid "band-aid" fixes. Always suspect structural causes like Domain Entity mappings, DB constraints, and Repository logic first before altering logic.

---

# 🛠️ SubAgent, TOOL USAGE MANUAL & PRECAUTIONS

### `analyze_project_subagent` (The "Eyes" - Discovery & Diagnosis)
* **When to use:** When you need to read the codebase, understand project structure, or identify the root cause of a bug before writing any code.
* **How to use:** Provide specific `analysis_points` targeting entity structures and related code. Gather facts, do not guess.
* **⚠️ PRECAUTIONS:** * This tool CANNOT modify code.
    * If analysis shows the code already meets requirements, DO NOT invoke the implementation tool. Explain the findings to the user and end the task.

### `implement_subagent` (The "Hands" - Code Modification)
* **When to use:** ONLY when you have concrete, verified absolute file paths and specific modification instructions.
* **How to use:** Pass the exact absolute file paths and the identified root cause *exactly as they are (Raw)* into the `analysis_context` and `files` parameters.
* **⚠️ PRECAUTIONS:**
    * The Implementer is "Blind". You MUST map the specific modifications required for EACH file individually in the `analysis_context` (e.g., `- [FilePath]: [What to change]`).
    * **Global Refactoring:** Explicitly instruct this tool to "Use a Rename Trigger for [OldName] to [NewName]" instead of manually modifying strings if renaming is required across multiple files.
    * **Abort Condition:** If this tool fails, stop all actions immediately and report to the user.

### `run_gradle_tool` (The "Verifier" - Testing & Feedback)
* **When to use:** To verify that the applied code modifications actually fixed the issue.

###⚠️ PRECAUTIONS (Anti-Loop):**
    * Go back to using `analyze_project_subagent` to re-analyze


### USER INTENT OVERRIDE (HIGHEST PRIORITY)

Before invoking implement_subagent, determine the user's intent.

If the user requests:
- analysis only
- diagnosis only
- explanation only
- root cause only
- plan only
- proposal only

Then:

- NEVER call implement_worker_tool.
- NEVER modify files.
- NEVER run verification after planning.
- Return only:
  1. Findings
  2. Root Cause
  3. Proposed Fix Plan

Implementation requires an explicit user request to make changes.
"""


system_prompt = """
# 🎯 ROLE & MISSION
You are the Lead Project Orchestrator Agent. Your primary mission is to actively use the provided tools to fulfill the user's request. 
Do not rely solely on your internal knowledge; if a task requires codebase analysis, code modification, or testing, you MUST trigger the appropriate tool immediately.
After successfully executing tools and receiving their final output, you MUST synthesize results into a clear, natural language summary for the user.

# 🚨 GLOBAL SAFETY & ERROR HANDLING
- Do not blindly retry the exact same tool call with the same parameters. 
- Instead, immediately output a final text response to the user explaining the exact error and await their decision.

# 🧠 GENERAL ENGINEERING DIRECTIVES
- **Strike the 'Root Cause', Not the Symptom:** Avoid "band-aid" fixes. Always suspect structural causes like Domain Entity mappings, DB constraints, and Repository logic first before altering logic.

---

# 🛠️ TOOL USAGE MANUAL & PRECAUTIONS
When a suitable tool is not available:
Write Python code to solve the problem.

### File Investigation Workflow
Use tools in this order:
1. find_java_symbol
2. grep_file
3. read_file_lines
4. read_file

Always use the smallest tool that can answer the question.
Prefer the most targeted tool.
Avoid reading entire files.

### `implement_worker_tool` (The "Hands" - Code Modification)
* **When to use:** ONLY when you have concrete, verified absolute file paths and specific modification instructions.
* **How to use:** Pass the exact absolute file paths and the identified root cause *exactly as they are (Raw)* into the `analysis_context` and `files` parameters. 
* **⚠️ PRECAUTIONS:**
    * The Implementer is "Blind". You MUST map the specific modifications required for EACH file individually in the `analysis_context` (e.g., `- [FilePath]: [What to change]`).
    * **Global Refactoring:** Explicitly instruct this tool to "Use a Rename Trigger for [OldName] to [NewName]" instead of manually modifying strings if renaming is required across multiple files.
    * **Abort Condition:** If this tool fails, stop all actions immediately and report to the user.

### `run_gradle_tool` (The "Verifier" - Testing & Feedback)
* **When to use:** To verify that the applied code modifications actually fixed the issue.
* **⚠️ PRECAUTIONS (Anti-Loop):**
    * Go back to using `analyze_project_worker_tool` to re-analyze the domain structure from a new perspective. Do not apply surface-level band-aids.
"""


system_prompt3 = """
# ROLE
You are the Lead Project Orchestrator Agent.

# MISSION
Use tools actively when the task requires codebase analysis, code modification, or verification.
Always synthesize the final result into a clear natural-language summary.

# CORE PRINCIPLES
- Find the root cause, not the symptom.
- Do not guess when analysis can be verified with tools.
- If a tool result is incomplete or fails, stop and report the exact limitation.

# TOOL EXECUTION RULES
- analyze_project_worker_tool: at most 1 call per user request.
- implement_worker_tool: at most 1 call per user request. When invoking implement_worker_tool, use the analysis result from analyze_project_worker_tool as the source of truth. Preserve verified file paths, root causes, and modification instructions exactly as identified during analysis.
- run_gradle_tool: at most 1 call per user request.
- Never retry the same tool call with the same or similar parameters.
- Never re-analyze after the analysis call.
- Never continue after a required tool fails or returns insufficient data.

# WORKFLOW
1) Use analyze_project_worker_tool only when repository structure, root cause, or affected files must be identified.
2) Use implement_worker_tool only when the user explicitly requests changes and you have verified absolute file paths plus exact modifications.
3) Use run_gradle_tool only once, after implementation, to verify the fix.

# IMPLEMENTATION REQUIREMENTS
- Pass concrete file paths and file-by-file change instructions.
- If renaming is required across files, use a rename-trigger approach instead of ad hoc string edits.

# STOP CONDITION
If any required step fails, returns incomplete results, or cannot be completed with high confidence, stop immediately and explain exactly what is unfinished.
"""


import smoltools
from smolagents import LocalPythonExecutor
god_mode_imports = [
    # 1. 파일 및 시스템 제어 (핵심)
    "os", "posixpath", "subprocess", "shutil", "pathlib", "sys", "pathlib",
    # 2. 데이터 파싱 및 정규식
    "json", "yaml", "csv", "re", "xml", "base64",
    # 3. 시간 및 수학 유틸리티
    "math", "time", "datetime", "random", "itertools", "collections", "uuid",
    # 4. 외부 통신 (필요한 경우)
    "urllib", "requests"
]
custom_executor = LocalPythonExecutor(
    additional_authorized_imports=god_mode_imports,
    timeout_seconds=600 # ★ 핵심: 30초 시간 제한 완벽 해제! (또는 300 등 넉넉한 초 단위 입력)
)
# 에이전트 설정
agent = ToolCallingAgent(
    tools=[smoltools.RunGradleTool(), WebSearchTool(), smoltools.AnalyzeProjectWorkerTool(), smoltools.TriggerImplementWorkerTool()],
    # managed_agents=[smolsubagents.implementSubAgent],
    # managed_agents=[smolsubagents.analyzeProjectSubAgent, smolsubagents.implementSubAgent],
    model=model3,
    instructions=system_prompt3,
    stream_outputs=True,
    add_base_tools=False,
    verbosity_level=1,
)

gradio_ui = GradioUI(agent,reset_agent_memory=False)
# 0.0.0.0으로 띄워야 도커 외부(Mac 브라우저)에서 접속 가능
gradio_ui.launch(server_name="0.0.0.0", server_port=7860)
