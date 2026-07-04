from pathlib import Path

from mcp import StdioServerParameters
from openai import OpenAI
from smolagents import CodeAgent, LiteLLMModel, OpenAIModel, MCPClient

import os

import smoltools



ALLOWED_PREFIX = Path("/workspace/trip").resolve()


ALLOWED_TASKS = {
    "test": ["./gradlew", "customTest", "--console=plain"],
    "build": ["./gradlew", "build", "--console=plain"],
}

TEXT_EXTENSIONS = {
    ".java", ".kt", ".groovy", ".gradle", ".kts",
    ".xml", ".yml", ".yaml", ".properties", ".md", ".txt", ".json"
}

EXCLUDED_DIRS = {
    ".git", ".gradle", ".idea", ".vscode", "build", "out", "target",
    "node_modules", ".jdtls-workspace", ".venv", "venv"
}


model = LiteLLMModel(
    model_id="ollama_chat/qwen3.6:35b-a3b", # This model is a bit weak for agentic behaviours though
    api_base="http://:11434", # replace with 127.0.0.1:11434 or remote open-ai compatible server if necessary
    api_key="YOUR_API_KEY", # replace with API key if necessary
    num_ctx=32000, # ollama default is 2048 which will fail horribly. 8192 works for easy tasks, more is better. Check https://huggingface.co/spaces/NyxKrage/LLM-Model-VRAM-Calculator to calculate how much VRAM this will need for the selected model.

    temperature=0.1,
    top_p=0.5,

    flatten_messages_as_text=False,
    chat_template_kwargs={"enable_thinking": False},
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

coding_llm_cfg = {
    "model_type": "oai",
    "model": "qwen3.6:35b-a3b",
    "model_server": "http://:11434/v1",
    "api_key": "EMPTY",
    "generate_cfg": {
        "top_p": 0.5,
        "temperature": 0.1,
        "use_raw_api": True,
        "max_input_tokens": 32000,
        'extra_body': {'chat_template_kwargs': {'enable_thinking': False}},
    },
}

import json
import time

from smolagents import Tool

# 개발/코딩 에이전트를 위한 필수 라이브러리 총집합
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

from smolagents import LocalPythonExecutor
custom_executor = LocalPythonExecutor(
    additional_authorized_imports=god_mode_imports, # 위에서 만든 만능 목록 주입
    timeout_seconds=600 # ★ 핵심: 30초 시간 제한 완벽 해제! (또는 300 등 넉넉한 초 단위 입력)
)


analyzeProjectSubAgent = CodeAgent(
    max_steps=3,
    code_block_tags=("```python_execute", "```"),

    executor=custom_executor,
    verbosity_level=1,
    tools=[smoltools.find_java_symbol_in_file, smoltools.grep_file, smoltools.read_file_lines, smoltools.read_file],
    model=model,
    # max_steps=10,
    name="analyze_project_subagent",
    description="""
    Analyzes exactly one specified file for the given task and analysis points,
    then returns only the file's role, dependencies, and explicit modification/deletion points.
    
    Map the project structure and locate candidate files for the following task:
    task: DETAILED task description... (e.g., 'Refactor domain entity relationship' or 'Implement target method').
    project_root: Project root path
    analysis_points: Comma-separated string of specific technical points to check (e.g., 'Check if the foreign key mapping matches the derived query, Verify @Transactional annotation').
    error_summary(any if): CRITICAL: The core error summary, exception message, or your current diagnosis (e.g., 'AssertionFailedError due to missing delete logic').
    suspected_files(any if): Comma-separated string of absolute file paths suspected in previous steps (e.g., '/path/to/A.java, /path/to/B.java').
    
    """,
    instructions="""
# ROLE & MISSION
You are a Project Structure Analyst Agent.

Your mission is to:
- Explore repository structure
- Identify exact integration points
- Trace structural root causes
- Produce implementation-ready file mappings

You DO NOT write code.
You ONLY identify where and why changes are required.

When a suitable tool is not available:
Use Python code to solve the problem.

# FILE INVESTIGATION WORKFLOW
Use tools in this order:

1. find_java_symbol
2. grep_file
3. read_file_lines
4. read_file

Avoid unnecessary file loading.

# STRATEGIC TRIAGE
- FOR BUG FIXES:
  Trace the dependency chain until the structural root cause is identified.
  Do not stop at the first failing location.

- FOR FEATURE ADDITIONS:
  Identify the complete vertical slice:
  Controller → Service → Repository → Entity

# ANALYSIS RULES
When inspecting a file:

1. Analyze ONLY the relevant code section.
2. Do not infer behavior without evidence.
3. Explicitly check:
   - method signatures
   - dependency usage
   - transactional boundaries
   - entity relationships
   - scope conflicts
   - duplicated responsibilities
4. If code must be removed or modified, state it explicitly.
5. Extract the exact code region responsible for the issue when possible.

# CONSTRAINTS
- MINIMIZE FILE READS:
   Prefer read_file_lines.
   Use read_file only when absolutely necessary.

- EVIDENCE ONLY:
   Never guess internal logic.
   Verify with tool output.

- STAY IN BOUNDS:
   Only use available tools.

# EXECUTION FLOW
- LOCATE
   Find exact absolute paths

- INSPECT
   Read only the relevant sections required to answer:
   - class role
   - dependencies
   - target methods
   - root-cause locations

- TRACE
   Follow dependencies until root cause or integration path is confirmed.

- REPORT

[OUTPUT FORMAT]
OBSERVED_FACTS

ROOT_CAUSE

AFFECTED_FILES
  Absolute Path:
  Reason:
  Action Required:
  Target Code Region:

CONFIDENCE
""",
    use_structured_outputs_internally=False,
)



# LLM 설정 (필요에 따라 클래스 외부나 내부에 정의)
llm_cfg_call = {
    "model": "qwen3.6:35b-a3b",
    "base_url": "http://:11434/v1",
    "api_key": "EMPTY",
    "top_p": 0.5,
    "temperature": 0.1,
}

class OneFileAnalyzerTool(Tool):
    name = "one_file_analyzer_tool"
    description = (
        "Analyzes exactly one specified file for the given task and analysis points, "
        "then returns only the file's role, dependencies, and explicit modification/deletion points."
    )

    inputs = {
        "task": {
            "type": "string",
            "description": "DETAILED task description. Do not use generic titles. You MUST include specific method names, missing annotations, or exact logic to analyze.",
        },
        "project_root": {
            "type": "string",
            "description": "Project root path",
        },
        "file_path": {
            "type": "string",
            "description": "Absolute path of the single target file to analyze",
        },
        "analysis_points": {
            "type": "array",
            "items": {"type": "string"},
            "description": "Specific technical points to check.",
        },
        "suspected_symbols": {
            "type": "array",
            "items": {"type": "string"},
            "description": "Specific symbols or annotations suspected of conflict",
            "nullable": True,
        },
    }

    output_type = "string"

    def forward(
            self,
            task: str,
            project_root: str,
            file_path: str,
            analysis_points: list[str],
            suspected_symbols: list[str] | None = None,
    ) -> str:
        start_time = time.monotonic()

        try:
            task = task.strip()
            project_root = project_root.strip()
            raw_file_path = file_path.strip()
            analysis_points = analysis_points or []
            suspected_symbols = suspected_symbols or []

            # 1. 경로 검증 (Path Validation)
            target_path = Path(raw_file_path).resolve()
            allowed_prefix = Path(project_root).resolve()

            if target_path != allowed_prefix and allowed_prefix not in target_path.parents:
                error_msg = (
                    f"Security Error: Access denied. File path '{target_path}' "
                    f"is outside the allowed project root '{allowed_prefix}'."
                )
                return json.dumps({"ok": False, "error": error_msg}, ensure_ascii=False)

            if not target_path.exists() or not target_path.is_file():
                error_msg = f"File Not Found Error: The file '{target_path}' does not exist or is not a file."
                return json.dumps({"ok": False, "error": error_msg}, ensure_ascii=False)

            # 2. 파이썬에서 직접 파일 읽기
            try:
                with open(target_path, "r", encoding="utf-8") as f:
                    file_content = f.read()
            except Exception as e:
                return json.dumps({"ok": False, "error": f"Failed to read file: {str(e)}"}, ensure_ascii=False)

            # 3. LLM 프롬프트 구성
            system_p = """
            You are a Single File Analyzer Agent.

            Your ONLY job is to analyze exactly ONE target file provided in the prompt.
            You must follow the provided analysis points and do not expand beyond them.

            [Constraints]
            1. Analyze ONLY the provided file content.
            2. Do not propose broad refactors.
            3. CRITICAL: When identifying the cause of a bug, explicitly check for scope conflicts.
            4. If existing code must be removed or modified, explicitly state this.
            """

            prompt = f"""
            Analyze exactly one file for the task below based on the provided file content.

            Task: {task}
            Target File: {target_path}

            [File Content]
            ```
            {file_content}
            ```

            [Analysis Points]
            {json.dumps(analysis_points, ensure_ascii=False, indent=2)}

            [Suspected Symbols / Conflict Checks]
            {json.dumps(suspected_symbols, ensure_ascii=False, indent=2) if suspected_symbols else "None specified. Check for general conflicts."}

            [Return Format]
            ### File Role
            - one sentence

            ### Dependencies
            - direct dependencies visible from this file

            ### Conflicting/Target Code (CRITICAL UNIQUE KEY)
            - [SEMANTIC_PATH]&#58; [Logical path to the target from outermost to innermost scope, e.g., 'Class > Method']
            - [UNIQUE_CODE_BLOCK]&#58; [Extract 3~5 lines of the EXACT code that needs changing, including at least 1 line of surrounding context above and below]
            - Action Required: [DELETE | MODIFY | ADD | NONE]

            ### Analysis Findings
            - one bullet per analysis point (one sentence each)

            ### Task Impact & Next Steps
            - [One sentence explicit instruction for the Implementer]
            """

            client = OpenAI(
                base_url=llm_cfg_call["base_url"],
                api_key=llm_cfg_call["api_key"],
            )

            response = client.chat.completions.create(
                model=llm_cfg_call["model"],
                messages=[
                    {"role": "system", "content": system_p},
                    {"role": "user", "content": prompt},
                ],
                temperature=llm_cfg_call["temperature"],
                top_p=llm_cfg_call["top_p"],
            )

            final_text = response.choices[0].message.content
            return json.dumps({"ok": True, "result": final_text}, ensure_ascii=False)

        except Exception as e:
            elapsed = time.monotonic() - start_time

            return json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False)

        finally:
            elapsed = time.monotonic() - start_time






def truncate_log_from_top(log: str, max_lines: int = 500) -> str:
    # 1. 문자열을 줄(Line) 단위 리스트로 쪼갭니다.
    lines = log.splitlines()

    # 2. 전체 줄 수가 max_lines보다 길 때만 자릅니다.
    if len(lines) > max_lines:
        # 처음부터 지정한 줄 수까지만 가져오기 (양수 인덱싱 슬라이싱)
        truncated_lines = lines[:max_lines]

        # 에이전트가 "아, 뒷부분이 너무 길어서 잘렸구나"를 알 수 있게 안내 문구 추가
        return "\n".join(truncated_lines) + "\n\n...[REMAINING LOG TRUNCATED]..."

    # 짧으면 그냥 그대로 반환
    return log








mcp_config = StdioServerParameters(
    command="npx",
    args=[
        "-y",
        "@modelcontextprotocol/server-filesystem",
        "/workspace/trip"
    ],
    env=os.environ.copy()
)

mcp_client = MCPClient(mcp_config)
mcp_tools = mcp_client.get_tools()

implementSubAgent = CodeAgent(
    max_steps=3,
    code_block_tags=("```python_execute", "```"),

    executor=custom_executor,
    verbosity_level=1,
    tools=[*mcp_tools, smoltools.OpenRewriteRefactorTool(), smoltools.find_java_symbol_in_file, smoltools.grep_file, smoltools.read_file_lines],
    model=model,
    # max_steps=10,
    name="implement_subagent",
    description="""
        A core coding agent that reads target files and applies precise modifications.
        It handles both direct code edits (add/update/delete) and structural refactorings.
        
        [Required Information To SubAgent]
        task: The overall feature or bug fix being implemented.
        analysis_context: Detailed, file-by-file breakdown of what to change. 
        files: Comma-separated string of absolute target file paths.
        project_root: Project root absolute path.
    """,
    instructions="""
        You are a Precise Code Implementation Orchestrator. You are fully in charge of modifying the codebase.
        You have TWO distinct execution strategies. You must analyze the task and choose the correct one:
        
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

        ### STRATEGY 1: MANUAL CODING (Add / Update / Delete)
        - Condition: Use this when you need to add new logic, update existing methods, or DELETE specific code.
        - Action: Write and execute Python code using standard file I/O (e.g., `open(file, 'r')`, `replace()`, `open(file, 'w')`) to directly modify the target files.
        - Rule: DO NOT use the openrewrite_refactor for these tasks.

        ### STRATEGY 2: REFACTORING TOOL (Rename / Move / Code Formatting / Styling / Beautification / Import Cleanup)
        - Condition: Use this ONLY when the task explicitly requires structural refactoring, such as globally RENAMING a field/method/class or MOVING a package.
        - Action: Call the `openrewrite_refactor` tool. 
        - Rule: Never attempt to globally rename variables using Python regex, as it is unsafe. Rely entirely on the tool.

        [Hard Rules - CRITICAL]
        1. Break down the workload. If a task requires both Strategy 1 and 2, execute them sequentially.
        2. If you need to DELETE a field, explicitly use Strategy 1 (Python file I/O). Do not ask the refactoring tool to delete it.
        3. MINIMALISM: Do only the required task.
            
        [OUTPUT RULES]
        Summarize each necessary part using the following format:
        - Part Name: One-line summary
    """,
    use_structured_outputs_internally=False,
)


