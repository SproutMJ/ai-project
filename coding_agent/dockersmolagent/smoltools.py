import subprocess
from pathlib import Path

from mcp import StdioServerParameters
from openai import OpenAI
from smolagents import CodeAgent, LiteLLMModel, OpenAIModel, MCPClient, ToolCallingAgent

import os

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
)

model2 = OpenAIModel(
    model_id="qwen3.6:35b-a3b", # This model is a bit weak for agentic behaviours though
    api_base="http://:11434/v1", # replace with 127.0.0.1:11434 or remote open-ai compatible server if necessary

    api_key="YOUR_API_KEY",
    temperature=0.1,
    max_tokens=32000,
    top_p=0.5,

    parallel_tool_calls=False,
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


import gc
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

# LLM 설정 (필요에 따라 클래스 외부나 내부에 정의)
llm_cfg_call = {
    "model": "qwen3.6:35b-a3b",
    "base_url": "http://:11434/v1",
    "api_key": "EMPTY",
    "top_p": 0.5,
    "temperature": 0.1,
}

llm_cfg_call2 = {
    "model": "unsloth/Qwen3.6-35B-A3B-GGUF:UD-Q4_K_M",
    "base_url": "http://:8080/v1",
    "api_key": "EMPTY",
    "top_p": 0.5,
    "temperature": 0.1,
}


class AnalyzeProjectWorkerTool(Tool):
    name = "analyze_project_worker_tool"
    description = """
    Analyzes exactly one specified file for the given task and analysis points,
    then returns only the file's role, dependencies, and explicit file_create/modification/deletion points.
    """

    inputs = {
        "task": {
            "type": "string",
            "description": "DETAILED task description... (e.g., 'Refactor domain entity relationship' or 'Implement target method').",
        },
        "project_root": {
            "type": "string",
            "description": "Project root path",
        },
        "analysis_points": {
            "type": "string",
            "description": "Comma-separated string of specific technical points to check (e.g., 'Check if the foreign key mapping matches the derived query, Verify @Transactional annotation').",
        },
        "error_summary": {
            "type": "string",
            "description": "The core error summary, exception message, or your current diagnosis.(If before error exist)",
            "nullable": True,
        },
        "suspected_files": {
            "type": "string",
            "description": "Comma-separated string of absolute file paths suspected in previous steps (e.g., '/path/to/A.java, /path/to/B.java').(If exist)",
            "nullable": True,
        },
    }

    output_type = "string"

    def forward(
            self,
            task: str,
            project_root: str,
            analysis_points: str,
            error_summary: str | None = None,
            suspected_files: str | None = None,
    ) -> str:

        start_time = time.monotonic()

        try:
            task = task.strip()
            project_root = project_root.strip()
            analysis_points = str(analysis_points or "")
            error_summary = error_summary or ""
            suspected_files = str(suspected_files or "")

            system_p = """
            # ROLE & MISSION
            You are a Project Structure Analyst Agent. Your mission is to explore the repository layout, identify exact integration points for new features, and trace the structural root causes of bugs or, suggest code file creation. You do not write code; you provide the precise "map and diagnosis" for the Implementer.
            
            Your mission is to:
            - START HERE: Always analyze the overview in `project root directory/project-structure/project-overview.md` using one_file_analyzer_tool first to understand the task.
            - Explore repository structure
            - Identify exact integration points
            - Trace structural root causes
            - STRICTLY NO FULL FILE READS: NEVER load an entire file. It causes token overflow and immediate termination.
            - CONCISE OUTPUT: Keep your final output under 5 lines by one code file. The Orchestrator only needs to know WHICH specific absolute paths to modify and WHAT the exact structural root cause or integration strategy is.
            - EXPLORE FIRST: You do not magically know file locations. Use filesystem tools to find exact absolute paths. NEVER guess or use truncated paths.
            - EVIDENCE-BASED DIAGNOSIS: Never guess the internal logic of a code file. If you suspect a file contains the root cause, you MUST inspect it using `one_file_analyzer_tool` before finalizing your candidate list.
            
            # Tool Usage Rules
            SINGLE-PASS ANALYSIS: Assume this is the only analysis opportunity. Gather all implementation-required information now. Do not defer investigation.
            - Filesystem Tool Usage Rule
            Use the filesystem tool only for repository navigation, path discovery, file existence checks, directory exploration, and project structure investigation.
            Do not use the filesystem tool to analyze file contents.
            - one_file_analyzer_tool Rule
            Whenever file content analysis, dependency analysis, code understanding, modification point identification, or implementation impact assessment is required, you must use one_file_analyzer_tool instead.
            PRECISE DELEGATION: When querying `one_file_analyzer_tool`, pass the exact `analysis_points` directed by the Orchestrator. Do not dilute or summarize the critical analysis points.
            - STAY IN BOUNDS: Only call explicit tools. Do not hallucinate paths or tools.
            
            You DO NOT write code.
            You ONLY identify where and why changes are required.
            
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
            6. If existing files are insufficient to implement the requested behavior,
               explicitly identify new files that must be created.
               For each required new file provide:
               - file path
               - responsibility
               - key dependencies
               Do NOT generate code.
            7. Never instruct file deletion as an implementation action.
                Only report candidates for human or later verification.
                
            COMPLETENESS CHECK: Before finalizing, ensure an Next Step(e.g., Implementer) can complete the task using only this report. If additional repository investigation would be required, the analysis is incomplete.
            """

            prompt = f"""
            Analyze the project structure and locate candidate files for the following task:
            Project Root Directory: {project_root}
            Task: {task}
            Analysis Points: {analysis_points}
            Error Summary (if any): {error_summary or "(none)"}
            Suspected Files (if any): {suspected_files or "(none)"}

            [EXECUTION FLOW]
            Follow these exact steps:
            1. OVERVIEW: Check `project-overview.md` for the overarching structure.
            2. LOCATE: Use filesystem-search_files to find exact absolute paths of files that match the task requirements. DO NOT guess paths.
            3. INSPECT (IF NECESSARY): Verify file roles by calling `one_file_analyzer_tool` with a target query (e.g., "class definition", "endpoint mapping"). Do NOT read the entire file.
            4. REPORT: Output the final mapping using ONLY the format below.

            [OUTPUT FORMAT]
            OBSERVED_FACTS
            
            ROOT_CAUSE
            
            AFFECT_FILES
              Absolute Path:
              Reason:
              Action Required:
              Target Code Region:
            """

            # mcp_tools 는 기존 mcp_config에 해당하는 smolagents Tool 목록으로 넣어주세요.
            agent = ToolCallingAgent(
                verbosity_level=1,
                model=model3,
                tools=[*mcp_tools, OneFileAnalyzerTool()],
                instructions=system_p,
            )

            final_text = agent.run(prompt)

            return json.dumps(
                {"ok": True, "result": str(final_text)},
                ensure_ascii=False,
            )

        except Exception as e:
            elapsed = time.monotonic() - start_time

            return json.dumps(
                {"ok": False, "error": str(e)},
                ensure_ascii=False,
            )





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
                base_url=llm_cfg_call2["base_url"],
                api_key=llm_cfg_call2["api_key"],
            )

            response = client.chat.completions.create(
                model=llm_cfg_call2["model"],
                messages=[
                    {"role": "system", "content": system_p},
                    {"role": "user", "content": prompt},
                ],
                temperature=llm_cfg_call2["temperature"],
                top_p=llm_cfg_call2["top_p"],
            )

            final_text = response.choices[0].message.content
            return json.dumps({"result": final_text}, ensure_ascii=False)

        except Exception as e:
            elapsed = time.monotonic() - start_time

            return json.dumps({"error": str(e)}, ensure_ascii=False)

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

class RunGradleTool(Tool):
    name = "run_gradle_tool"
    description = (
        f"Run a safe Gradle command for a Spring project under the allowed directory only and analyze the result. "
        f"Return a concise success or failure summary, explicitly checking if previous actions caused scope conflicts. "
        f"Allowed prefix is: {ALLOWED_PREFIX}. "
        f"Allowed tasks: {', '.join(ALLOWED_TASKS.keys())}"
    )
    inputs = {
        "project_root": {
            "type": "string",
            "description": "Absolute path to the Gradle project root directory",
        },
        "task": {
            "type": "string",
            "description": "Gradle task name. One of: test, build",
        },
        "context_summary": {
            "type": "string",
            "description": "detailed summary of the implementation(or fix) intent and why this Gradle run is being executed.",
        },
        "recent_modified_files": {
            "type": "string",
            "description": "Comma-separated string of files changed most recently (e.g., 'fileA.java, fileB.java').",
            "nullable": True,
        },
        "suspected_symbols": {
            "type": "string",
            "description": "Comma-separated string of symbols, methods, classes, or fields likely related to the failure (e.g., 'saveAndFindById_success, @Rollback').",
            "nullable": True,
        },
        "previous_failure_summary": {
            "type": "string",
            "description": "Brief summary of the last failure and what was already tried.",
            "nullable": True,
        },
        "previous_actions_taken": {
            "type": "string",
            "description": "Comma-separated string of explicit coding actions taken right before this run (e.g., 'Deleted @Rollback, Added flush()').",
            "nullable": True,
        }
    }

    output_type = "string"


    def forward(
            self,
            project_root: str,
            task: str,
            context_summary: str,
            recent_modified_files: str = "",
            suspected_symbols: str = "",
            previous_failure_summary: str = "",
            previous_actions_taken: str = ""
    ) -> str:

        start_time = time.monotonic()

        try:
            root_path = Path(project_root).expanduser().resolve()
            allowed_path = Path(ALLOWED_PREFIX).expanduser().resolve()

            # Security: Path traversal checks
            if root_path != allowed_path and allowed_path not in root_path.parents:
                return json.dumps({
                    "ok": False,
                    "error": f"access denied. Allowed path prefix: {ALLOWED_PREFIX}",
                }, ensure_ascii=False)

            if not root_path.is_dir():
                return json.dumps({
                    "ok": False,
                    "error": f"not a directory: {root_path}",
                }, ensure_ascii=False)

            gradlew = root_path / "gradlew"
            if not gradlew.is_file():
                return json.dumps({
                    "ok": False,
                    "error": f"gradlew not found in: {root_path}",
                }, ensure_ascii=False)

            if task not in ALLOWED_TASKS:
                return json.dumps({
                    "ok": False,
                    "error": f"unsupported task: {task}",
                    "allowed_tasks": list(ALLOWED_TASKS.keys()),
                }, ensure_ascii=False)

            # Execution
            command_list = ALLOWED_TASKS[task]
            result = subprocess.run(
                command_list,
                cwd=str(root_path),
                capture_output=True,
                text=True,
                timeout=300,
            )

            if result.returncode == 0:
                return json.dumps(
                    {"ok": True, "result": "Test successful"},
                    ensure_ascii=False,
                )

            retcode = result.returncode
            command = " ".join(ALLOWED_TASKS[task])
            stdout_text = result.stdout or ""
            stderr_text = result.stderr or ""

            stdout_text = truncate_log_from_top(stdout_text, 50)
            stderr_text = truncate_log_from_top(stderr_text, 300)

            log = f"""
            [STDOUT]
            {stdout_text}

            [STDERR]
            {stderr_text}
            """


            # 2. Identify the smallest set of suspicious files related to the failure.
            # 3. Analyze each suspicious file with one_file_analyzer_tool.

            # # 5. NEVER read the entire file using `read_file` or similar tools. You MUST extract code context strictly using `one_file_analyzer_tool` around the failed line numbers. Reading full files will cause token overflow and immediate termination.
            # 7. If the error log explicitly contains a file path and/or line number, should call one_file_analyzer_tool.
            # 8. Limit the analysis to at most 5 files, and prefer 3 files or fewer when possible.

            system_p = """
            You are a Gradle Failure Triage Agent.

            Your job:
            1. Read the build/test log.
            2. Return a compact final diagnosis.

            [Constraints]
            CRITICAL RULE: > If you encounter an execution loop or consecutive tool-calling failures (exceeding 3 attempts), you must abort the operation immediately. Instead of continuing redundant attempts, summarize the current task status, explain the bottleneck, and stop.
            1. TARGETED ANALYSIS: Focus exclusively on the failed tests or tasks. Do not speculate on unrelated warnings.
            2. ROOT CAUSE IDENTIFICATION: Pinpoint exactly why the failure occurred (e.g., specific code logic bugs, missing dependencies, incorrect configurations, or environment mismatches).
            3. ACTIONABLE SOLUTIONS: Provide concrete, step-by-step guidance or code snippets showing how to fix the problem in the related files.
            4. HIGH DENSITY, NO FLUFF: Keep the output concise, structured, and free of vague conversational filler.
            5. Execute ONLY the requested task. No extra info, commentary, or suggestions. Strictly adhere to the scope.
            6. Use the file analysis only to confirm the failure cause, failed target, location, and fix action.
            7. BLIND SPOT CHECK: If `previous_failure_summary` indicates a fix was attempted but the error remains identical, actively suspect that the previous fix was purely additive (e.g., adding to class level) and was SHADOWED/OVERRIDDEN by existing local configurations (e.g., method-level annotations). Direct the next action to DELETE the conflicting local code.
            """

            prompt = f"""
            Analyze the execution results and failure facts from the raw log provided below, and diagnose the underlying issue based on the context.

            Project Root: {project_root}
            Command: {command}
            Exit Code: {retcode}

            [Handoff Summary]
            Implementation intent: {context_summary or "(none)"}
            Recent modified files: {recent_modified_files or "(none)"}
            Suspected symbols: {suspected_symbols or "(none)"}
            Previous failure summary: {previous_failure_summary or "(none)"}
            Previous actions taken: {previous_actions_taken or "(none)"}

            [Raw Log to Analyze]
            {log}

            [Required Workflow]

            1. Read the failure log only.
            2. Extract only explicit facts from the log (failed test, exact error, exception type, location).
            3. Choose one problem to solve first.
            4. Classify the chosen failure type.
            5. Infer the most likely cause.
            6. Extract a candidate file bundle.
            7. Suggest a fix direction.

            [Decision Rules]

            * Prefer log evidence over speculation.
            * If the current error is identical to the `previous_failure_summary`, explicitly evaluate if a structural mismatch (e.g., Entity mapping vs Database schema vs Test data setup) is the root cause. Do not default to modifying transaction scopes.

            [Output Format]
            STATUS
            FAILED
            
            ROOT_CAUSE
            ...
            
            CANDIDATE_FILES
            ...
            
            PROPOSED_FIX_OPTIONS
            1.
            2.
            3.
            """


            client = OpenAI(
                base_url=llm_cfg_call2["base_url"],
                api_key=llm_cfg_call2["api_key"],
            )

            response = client.chat.completions.create(
                model=llm_cfg_call2["model"],
                messages=[
                    {"role": "system", "content": system_p},
                    {"role": "user", "content": prompt},
                ],
                temperature=llm_cfg_call2["temperature"],
                top_p=llm_cfg_call2["top_p"],
            )

            final_text = response.choices[0].message.content
            return json.dumps({"result": final_text}, ensure_ascii=False)

        except subprocess.TimeoutExpired:
            elapsed = time.monotonic() - start_time

            return json.dumps({
                "error": "command timed out after 300 seconds",
            }, ensure_ascii=False)
        except Exception as e:
            elapsed = time.monotonic() - start_time

            return json.dumps({
                "error": str(e),
            }, ensure_ascii=False)

        finally:
            elapsed = time.monotonic() - start_time



class TriggerImplementWorkerTool(Tool):
    name = "implement_worker_tool"
    description = (
        "A tool that performs coding tasks. It reads target files and applies precise modifications "
        "using a Search/Replace text format. It handles one file at a time."
    )
    inputs = {
        "task": {
            "type": "string",
            "description": "The overall feature or bug fix being implemented (Big Picture)."
        },
        "goal": {
            "type": "string",
            "description": "The final expected outcome or behavior after this task is complete."
        },
        "analysis_context": {
            "type": "string",
            "description":
                """
                CRITICAL: Must contain a detailed, file-by-file breakdown of what to change.
                Format: '- [File Path]: [Specific modification instructions]'.
                """
        },
        "files": {
            "type": "string",
            "description": "Comma-separated string of absolute target file paths."
        },
        "project_root": {
            "type": "string",
            "description": "Project root absolute path",
        },
    }

    output_type = "string"

    def forward(self, task: str, goal: str, analysis_context: str, files: str, project_root: str) -> str:

        start_time = time.monotonic()
        results_summary = {}

        try:
            task = task.strip()
            goal = goal.strip()
            files_str = str(files)
            project_root = str(project_root) # Gradle 명령어를 실행할 프로젝트 루트 경로
            global_refactor_triggers = []

            # 1. 파일 목록 파싱 및 검증
            file_paths = [Path(f.strip()) for f in files_str.split(",") if f.strip()]
            valid_file_paths = []
            not_valid_file_paths = []

            for file_path in file_paths:
                # is_relative_to는 Python 3.9 이상 지원 (하위 버전이면 ALLOWED_PREFIX in p.parents 사용)
                if file_path != ALLOWED_PREFIX and ALLOWED_PREFIX not in file_path.parents:
                    not_valid_file_paths.append(file_path)

                else:
                    valid_file_paths.append(file_path)


            if len(not_valid_file_paths) > 0:
                return json.dumps({"ok": False, "error": f"ABORTED: No valid files: {not_valid_file_paths} within: {ALLOWED_PREFIX}."})

            file_paths = valid_file_paths



            # 2. 시스템 프롬프트 (출력 형식 강제)
            system_p = """
            You are a Precise Code Implementation Orchestrator. First, evaluate if changes or refactorings are actually required based on the task.
            - Condition A (Code Changes): If modifying existing code is required, use the `<<<< SEARCH ==== >>>> REPLACE` format. If NO modifications are required, you MUST output exactly `NO_CHANGES_NEEDED`.
            - Condition B (Refactoring): If renaming/moving is required, use the Trigger formats. If NO global refactoring is required, you MUST output exactly `NO_REFACTORINGS_NEEDED`.

            You MUST NOT use tool calls. Output in plain text using the EXACT format below.

            [Hard Rules - CRITICAL]
            1. TASK FILTERING (IGNORE STYLING): If the user or `task` explicitly requests code formatting, styling, beautification, or import cleanup, YOU MUST SILENTLY IGNORE those specific requests. Execute ONLY the logical/functional code changes.
            2. MINIMALISM: Do only the required task.
            3. NO IMPORTS (ABSOLUTE): DO NOT modify, add, or delete `import` statements under any circumstances, EVEN IF the task asks you to. External tools will handle imports automatically.
            4. FIX PRIORITY: Resolve root architectural issues first. When adding configurations, always check and DELETE/MODIFY existing local method-level annotations that might shadow/override your changes.
            5. MULTIPLE CHANGES: If a file needs changes in multiple separate places, output multiple distinct SEARCH/REPLACE blocks. DO NOT output the unchanged code in between them.
            """

            # 3. 파일 단위 개별 루프 실행 (One File at a Time)
            for file_path in file_paths:

                #코드 생성 모드
                if not file_path.exists():

                    prompt = f"""
                    Task: {task}
                    Goal: {goal}
                    Analysis Context: {analysis_context}
                    [Target File: {file_path}]
                    
                    You are a code generator.

                    The target file does not exist.
                    
                    Generate the COMPLETE file.
                    
                    Rules:
                    1. Output only code.
                    2. Do not explain.
                    """

                    client = OpenAI(
                        base_url=llm_cfg_call2["base_url"],
                        api_key=llm_cfg_call2["api_key"],
                    )

                    response = client.chat.completions.create(
                        model=llm_cfg_call2["model"],
                        messages=[
                            {"role": "system", "content": system_p},
                            {"role": "user", "content": prompt},
                        ],
                        temperature=llm_cfg_call2["temperature"],
                        top_p=llm_cfg_call2["top_p"],
                    )

                    result = response.choices[0].message.content

                    file_path.write_text(
                        result
                    )

                    results_summary[str(file_path)] = {"create_code_status": f"{file_path} OK"}
                    continue

                # 원본 파일 읽기
                original_content = file_path.read_text(encoding="utf-8")

                # 에이전트 프롬프트에 파일의 현재 상태를 통째로 제공 (Context)
                prompt = f"""
                Task: {task}
                Goal: {goal}
                Analysis Context: {analysis_context}
                [Target File: {file_path}]

                [Current File Content]
                ````
                {original_content}
                ````

                [OUTPUT FORMAT]
                1. EXACT MATCH: The SEARCH block must exactly match the existing code. Include 1-2 lines of unchanged code before and after the modified lines to ensure unique matching.
                2. If NO renaming or modifying is required, YOU MUST NOT output any `<<<< SEARCH ==== >>>> REPLACE` blocks. Simply respond 'NO_CHANGES_NEEDED'.
                3. You are a Precise Code Implementation Orchestrator. Provide the SEARCH/REPLACE blocks and OpenRewrite Triggers to execute the requested changes for this file.
                4. You MUST NOT use JSON tool calls. You must output the code changes in plain text using the EXACT format below.

                [ORDER OF OUTPUT - CRITICAL]
                You MUST follow this strict order to prevent parsing errors:
                1. CODE PATCH SECTION: Output ALL `<<<< SEARCH ==== >>>> REPLACE` blocks. (Or output `NO_CHANGES_NEEDED` if none).
                2. REFACTORING SECTION: Output ALL refactoring triggers at the VERY END. (Or output `NO_REFACTORINGS_NEEDED` if none).
                   - [SCOPE ORDERING RULE] If outputting multiple triggers, order them from SMALLEST to LARGEST scope: (1) RENAME_FIELD/VARIABLE (2) RENAME_METHOD (3) RENAME_CLASS (4) MOVE_PACKAGE.
                3. Do NOT mix or interleave the code patches and the refactoring triggers.

                [SAMPLE: WHEN NO CHANGES ARE NEEDED]
                NO_CHANGES_NEEDED
                NO_REFACTORINGS_NEEDED

                [SAMPLE: WHEN CHANGES ARE NEEDED (Code Patch)]
                <<<< SEARCH
                [Extract exact lines of existing code to replace. Include 1-2 unchanged context lines. Include exact spaces/tabs.]
                ====
                [Write the new code. Leave blank to delete the search block.]
                >>>> REPLACE

                [SAMPLE: WHEN REFACTORINGS ARE NEEDED (Triggers)]
                [SAMPLE 1]: for rename method
                <<<< RENAME_METHOD
                old: package.ClassName oldMethodName(..)
                new: newMethodName
                >>>> RENAME_METHOD

                [SAMPLE 2]: for rename class
                <<<< RENAME_CLASS
                old: fully.qualified.OldClassName
                new: fully.qualified.NewClassName
                >>>> RENAME_CLASS

                [SAMPLE 3]: for rename field
                <<<< RENAME_FIELD
                class: fully.qualified.ClassName
                old: oldFieldName
                new: newFieldName
                >>>> RENAME_FIELD

                [SAMPLE 4]: for move package
                <<<< MOVE_PACKAGE
                old: old.package.name
                new: new.package.name
                >>>> MOVE_PACKAGE
                """

                client = OpenAI(
                    base_url=llm_cfg_call2["base_url"],
                    api_key=llm_cfg_call2["api_key"],
                )

                response = client.chat.completions.create(
                    model=llm_cfg_call2["model"],
                    messages=[
                        {"role": "system", "content": system_p},
                        {"role": "user", "content": prompt},
                    ],
                    temperature=llm_cfg_call2["temperature"],
                    top_p=llm_cfg_call2["top_p"],
                )

                result = response.choices[0].message.content

                # 2. 코드 패치 및 RENAME 트리거 추출 실행
                patch_status = self._extract_and_apply_regex_patch(result, file_path, original_content)
                # 에이전트의 의도를 추출하여 Gradle 명령어로 즉시 실행

                refactoring_triggers = self._extract_refactoring_triggers(result)
                global_refactor_triggers.extend(refactoring_triggers)

                results_summary[str(file_path)] = {"patch_status": patch_status}

            results_summary["global_refactoring"] = self._execute_global_refactorings(global_refactor_triggers, project_root)

            return json.dumps({
                "results": results_summary
            }, ensure_ascii=False)

        except Exception as e:

            return json.dumps({"error": str(e)}, ensure_ascii=False)

        finally:
            elapsed = time.monotonic() - start_time



    def _extract_and_apply_regex_patch(self, agent_text: str, file_path: Path, original_content: str) -> str:
        if "NO_CHANGES_NEEDED" in agent_text:

            return f"Success. No changes needed."
        """
        에이전트가 뱉은 텍스트에서 SEARCH/REPLACE 블록을 파싱하여 파일에 적용합니다.
        들여쓰기가 틀린 경우를 대비해 Regex Fallback(유연한 공백 검색)을 포함합니다.
        """

        # < 나 > 기호가 3개 이상(3~5개) 오거나 뒤에 공백이 있어도 매칭되도록 유연성 부여
        pattern = r"<{3,5}\s*SEARCH\n(.*?)\n=+\n(.*?)\n>{3,5}\s*REPLACE"

        # re.DOTALL: 개행문자(\n)를 포함하여 매칭
        matches = re.findall(pattern, agent_text, flags=re.DOTALL)

        if not matches:

            return f"Failed. No valid SEARCH/REPLACE block found."

        current_content = original_content
        applied_count = 0

        failed_match = []
        for search_block, replace_block in matches:
            # 블록의 앞뒤 불필요한 개행만 제거 (들여쓰기는 유지)
            search_block = search_block.strip('\r\n')
            replace_block = replace_block.strip('\r\n')

            # [전략 1] Exact Match
            if search_block in current_content:
                current_content = current_content.replace(search_block, replace_block, 1)
                applied_count += 1
                continue

            # [전략 2] Regex Fallback (유연한 공백 무시 검색)

            tokens = search_block.split()
            if not tokens:
                continue

            escaped_tokens = [re.escape(token) for token in tokens]
            # 토큰 사이의 임의의 공백(\s+) 허용
            regex_pattern = r'\s+'.join(escaped_tokens)
            regex = re.compile(regex_pattern)

            match = regex.search(current_content)
            if match:
                current_content = regex.sub(replace_block, current_content, count=1)
                applied_count += 1
            else:
                failed_match.append(search_block)


        if applied_count == len(matches):
            file_path.write_text(current_content, encoding="utf-8")
            return f"Success. Applied {applied_count} patch(es)."
        else:
            return f"Failed. No patches could be applied. failed match search block: {failed_match}"

    def _extract_refactoring_triggers(self, agent_text: str) -> list:
        if "NO_REFACTORINGS_NEEDED" in agent_text:
            return []

        """
        텍스트 맨 뒤에 추가된 다양한 OpenRewrite 트리거를 파싱하여 리스트로 반환합니다.
        LLM의 포맷팅 오타(<, > 기호 누락/추가) 및 환각(Dummy 데이터 생성)을 철저히 방어합니다.
        """
        triggers = []
        dummy_keywords = {"none", "n/a", "null", "-", "na", ""}

        # 1. 2개의 파라미터 (old, new)를 가지는 트리거들 탐지
        # 대상: RENAME_METHOD, RENAME_CLASS, MOVE_PACKAGE
        # 닫는 태그에 액션명이 포함되어 있을 경우(?:\s*\1)? 도 유연하게 처리
        pattern_basic = r"<{3,5}\s*(RENAME_METHOD|RENAME_CLASS|MOVE_PACKAGE)\nold:\s*(.*?)\nnew:\s*(.*?)\n>{3,5}(?:\s*\1)?"
        matches_basic = re.findall(pattern_basic, agent_text, flags=re.IGNORECASE)

        for action, old_val, new_val in matches_basic:
            action = action.strip().upper()
            old_val = old_val.strip()
            new_val = new_val.strip()

            # [방어 로직] 의미 없는 빈 값이나 더미 키워드가 들어온 경우 무시
            if not old_val or not new_val or old_val.lower() in dummy_keywords or new_val.lower() in dummy_keywords:

                continue

            triggers.append({
                "action": action,
                "old": old_val,
                "new": new_val
            })

        # 2. 3개의 파라미터 (class, old, new)를 가지는 필드/변수 트리거 탐지
        # 대상: RENAME_VARIABLE (또는 RENAME_FIELD)
        pattern_var = r"<{3,5}\s*(RENAME_VARIABLE|RENAME_FIELD)\nclass:\s*(.*?)\nold:\s*(.*?)\nnew:\s*(.*?)\n>{3,5}(?:\s*\1)?"
        matches_var = re.findall(pattern_var, agent_text, flags=re.IGNORECASE)

        for action, cls_val, old_val, new_val in matches_var:
            action = action.strip().upper()
            cls_val = cls_val.strip()
            old_val = old_val.strip()
            new_val = new_val.strip()

            # [방어 로직] 필드 리네임 시 필요한 3가지 값이 하나라도 비정상이면 무시
            if not cls_val or not old_val or not new_val or old_val.lower() in dummy_keywords or cls_val.lower() in dummy_keywords:

                continue

            triggers.append({
                "action": "RENAME_FIELD",  # 파이썬 핸들러 처리를 위해 내부 액션명 통일
                "class": cls_val,
                "old": old_val,
                "new": new_val
            })

        return triggers

    def _execute_global_refactorings(self, triggers: list, project_root: str) -> list:
        results = []


        fields = [t for t in triggers if t['action'] == 'RENAME_FIELD']
        methods = [t for t in triggers if t['action'] == 'RENAME_METHOD']
        classes = [t for t in triggers if t['action'] == 'RENAME_CLASS']
        packages = [t for t in triggers if t['action'] == 'MOVE_PACKAGE']


        yaml_path = os.path.join(project_root, 'rewrite.yml')
        backup_path = os.path.join(project_root, 'rewrite.yml.bak')

        # 기존 rewrite.yml이 있다면 안전하게 백업
        if os.path.exists(yaml_path):
            os.rename(yaml_path, backup_path)

        # 단일 레시피 실행용 헬퍼 함수
        def execute_single_recipe(recipe_yaml_snippet: str, fail_msg: str, success_msg: str):
            yaml_content = (
                               "type: specs.openrewrite.org/v1beta/recipe\n"
                               "name: com.agent.SingleRefactoring\n"
                               "displayName: Single Isolated Refactoring\n"
                               "recipeList:\n"
                           ) + recipe_yaml_snippet

            # 1. 이번 루프 작업만을 위한 임시 yaml 작성
            with open(yaml_path, 'w', encoding='utf-8') as f:
                f.write(yaml_content)

            cmd = './gradlew rewriteRun -Drewrite.activeRecipes=com.agent.SingleRefactoring'
            run_result = self._run_gradle(cmd, project_root)

            if run_result.returncode != 0:
                results.append(fail_msg)
            else:
                results.append(success_msg)

        try:
            # 1. 필드(변수) 이름 변경
            for field in fields:
                snippet = (
                    "  - org.openrewrite.java.ChangeFieldName:\n"
                    f"      classType: \"{field['class']}\"\n"
                    f"      oldFieldName: \"{field['old']}\"\n"
                    f"      newFieldName: \"{field['new']}\""
                )
                execute_single_recipe(snippet,
                                      f"필드 변경 실패: [class: {field['class']}] '{field['old']}' -> '{field['new']}'",
                                      f"필드 변경 성공: [class: {field['class']}] '{field['old']}' -> '{field['new']}'")

            # 2. 메서드 이름 변경 (NPE 원인이었던 띄어쓰기 문제를 yaml로 완벽 격리)
            for method in methods:
                snippet = (
                    "  - org.openrewrite.java.ChangeMethodName:\n"
                    f"      methodPattern: \"{method['old']}\"\n"
                    f"      newMethodName: \"{method['new']}\""
                )
                execute_single_recipe(snippet,
                                      f"메서드 변경 실패: '{method['old']}' -> '{method['new']}'",
                                      f"메서드 변경 성공: '{method['old']}' -> '{method['new']}'")

            # 3. 클래스 이름 변경
            for cls in classes:
                snippet = (
                    "  - org.openrewrite.java.ChangeType:\n"
                    f"      oldFullyQualifiedTypeName: \"{cls['old']}\"\n"
                    f"      newFullyQualifiedTypeName: \"{cls['new']}\""
                )
                execute_single_recipe(snippet,
                                      f"클래스 변경 실패: '{cls['old']}' -> '{cls['new']}'",
                                      f"클래스 변경 성공: '{cls['old']}' -> '{cls['new']}'")

            # 4. 패키지 이동
            for pkg in packages:
                snippet = (
                    "  - org.openrewrite.java.ChangePackage:\n"
                    f"      oldPackageName: \"{pkg['old']}\"\n"
                    f"      newPackageName: \"{pkg['new']}\""
                )
                execute_single_recipe(snippet,
                                      f"패키지 이동 실패: '{pkg['old']}' -> '{pkg['new']}'",
                                      f"패키지 이동 성공: '{pkg['old']}' -> '{pkg['new']}'")

            # 5. 마무리 정리 (이것만은 묶어서 한 번에 실행)
            cleanup_snippet = (
                "  - org.openrewrite.java.RemoveUnusedImports\n"
                "  - org.openrewrite.java.OrderImports\n"
                "  - org.openrewrite.java.format.AutoFormat"
            )
            execute_single_recipe(cleanup_snippet,
                                  "마무리 정리 실패: Import 정리 및 AutoFormat 적용 실패",
                                  "마무리 정리 성공: Import 정리 및 AutoFormat 적용 성공")

        finally:
            # 모든 작업이 끝나면 임시 yaml 파일 삭제 및 원본 복구
            if os.path.exists(backup_path):
                os.replace(backup_path, yaml_path)
            elif os.path.exists(yaml_path):
                os.remove(yaml_path)

        if len(triggers) == 0:

            results.append("success. No global refactorings needed.")

        return results

    # 편의성을 위한 헬퍼 메서드 분리
    def _run_gradle(self, command: str, cwd: str) -> dict:
        import subprocess
        #
        return subprocess.run(command, shell=True, cwd=cwd, capture_output=True, text=True)


from smolagents import tool
import re

@tool
def grep_file(
        file_path: str,
        keyword: str
) -> str:
    """
    Search keyword in a file and return matching lines with line numbers.

    Args:
        file_path: Path to the file.
        keyword: Keyword to search for.

    Returns:
        Matching lines with line numbers.
    """

    results = []

    with open(file_path, "r", encoding="utf-8") as f:
        for idx, line in enumerate(f, start=1):
            if keyword.lower() in line.lower():
                results.append(
                    f"{idx}: {line.rstrip()}"
                )

    if not results:
        return "No matches found."

    return "\n".join(results[:50])


@tool
def read_file_lines(
        file_path: str,
        start_line: int,
        end_line: int
) -> str:
    """
    Read specific line range from a file.

    Args:
        file_path: Path to the file.
        start_line: First line number.
        end_line: Last line number.

    Returns:
        File content in the specified range.
    """

    if start_line < 1:
        start_line = 1

    if end_line < start_line:
        return "Invalid line range."

    with open(file_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    selected = lines[start_line - 1:end_line]

    return "".join(
        f"{i + start_line}: {line}"
        for i, line in enumerate(selected)
    )

@tool
def read_file(
        file_path: str,
        max_chars: int = 30000
) -> str:
    """
    Read full file content.

    Args:
        file_path: File path.
        max_chars: Maximum characters to return.

    Returns:
        Entire file content.
    """

    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    return content[:max_chars]

@tool
def find_java_symbol_in_file(
        java_file_path: str,
        symbol_name: str
) -> str:
    """
    Find a Java class, interface, enum, field, or method
    and return matching line numbers.

    Args:
        java_file_path: Path to the Java file.
        symbol_name: Symbol to locate.

    Returns:
        Matching locations.
    """

    patterns = [
        rf"\bclass\s+{re.escape(symbol_name)}\b",
        rf"\binterface\s+{re.escape(symbol_name)}\b",
        rf"\benum\s+{re.escape(symbol_name)}\b",

        rf"\b{re.escape(symbol_name)}\s*\(",

        rf"\b{re.escape(symbol_name)}\b\s*[;=]"
    ]

    matches = []

    try:
        with open(java_file_path, "r", encoding="utf-8") as f:
            for line_num, line in enumerate(f, start=1):

                for pattern in patterns:
                    if re.search(pattern, line):
                        matches.append(
                            f"Line {line_num}: {line.strip()}"
                        )
                        break

        if not matches:
            return f"Symbol '{symbol_name}' not found."

        return "\n".join(matches[:20])

    except Exception as e:
        return f"Error: {str(e)}"













class OpenRewriteRefactorTool(Tool):
    name = "openrewrite_refactor"
    description = """
    Performs global refactoring (changing fields, methods, classes, packages) of a Java project using OpenRewrite.
    """
    inputs = {
        "triggers": {
            "type": "array",
            "description": """
            List of refactoring tasks to perform. Strictly adhere to the JSON format in the description above.
            
            The 'triggers' input must be a list of dictionaries strictly following one of these 4 formats:
            
            1. Rename Field: {"action": "RENAME_FIELD", "class": "com.example.MyClass", "old": "oldVar", "new": "newVar"} (Note: RENAME_FIELD must include the 'class' key)
            2. Rename Method: {"action": "RENAME_METHOD", "old": "com.example.MyClass oldMethod(..)", "new": "newMethod"}
            3. Rename Class: {"action": "RENAME_CLASS", "old": "com.example.OldClass", "new": "com.example.NewClass"}
            4. Move Package: {"action": "MOVE_PACKAGE", "old": "com.example.oldpkg", "new": "com.example.newpkg"}
            """,
        },
        "project_root": {
            "type": "string",
            "description": "Absolute or relative path to the root directory of the Java project to be refactored."
        }
    }
    output_type = "array"

    def _run_gradle(self, command: str, cwd: str) -> subprocess.CompletedProcess:
        return subprocess.run(command, shell=True, cwd=cwd, capture_output=True, text=True)

    def forward(self, triggers: list, project_root: str) -> list:
        # 1. Pre-validation of input values for the sub-agent
        # If the agent formats it incorrectly, return an error message to induce self-correction.
        valid_actions = ['RENAME_FIELD', 'RENAME_METHOD', 'RENAME_CLASS', 'MOVE_PACKAGE']
        for idx, t in enumerate(triggers):
            if not isinstance(t, dict):
                return [f"Agent Error: Item at index {idx} in triggers is not a dictionary. Check JSON format. Input: {t}"]

            action = t.get('action')
            if action not in valid_actions:
                return [f"Agent Error: Unknown action '{action}'. Supported actions: {valid_actions}"]

            if 'old' not in t or 'new' not in t:
                return [f"Agent Error: The 'old' or 'new' key is missing for the '{action}' action. Input: {t}"]

            if action == 'RENAME_FIELD' and 'class' not in t:
                return [f"Agent Error: The 'class' key specifying the target class is required for the RENAME_FIELD action. Input: {t}"]

        results = []

        # Safely extract values (using get)
        fields = [t for t in triggers if t.get('action') == 'RENAME_FIELD']
        methods = [t for t in triggers if t.get('action') == 'RENAME_METHOD']
        classes = [t for t in triggers if t.get('action') == 'RENAME_CLASS']
        packages = [t for t in triggers if t.get('action') == 'MOVE_PACKAGE']

        yaml_path = os.path.join(project_root, 'rewrite.yml')
        backup_path = os.path.join(project_root, 'rewrite.yml.bak')

        if os.path.exists(yaml_path):
            os.rename(yaml_path, backup_path)

        def execute_single_recipe(recipe_yaml_snippet: str, fail_msg: str, success_msg: str):
            yaml_content = (
                               "type: specs.openrewrite.org/v1beta/recipe\n"
                               "name: com.agent.SingleRefactoring\n"
                               "displayName: Single Isolated Refactoring\n"
                               "recipeList:\n"
                           ) + recipe_yaml_snippet

            with open(yaml_path, 'w', encoding='utf-8') as f:
                f.write(yaml_content)

            cmd = './gradlew rewriteRun -Drewrite.activeRecipes=com.agent.SingleRefactoring'
            run_result = self._run_gradle(cmd, project_root)

            if run_result.returncode != 0:
                results.append(fail_msg)
            else:
                results.append(success_msg)

        try:
            for field in fields:
                snippet = (
                    "  - org.openrewrite.java.ChangeFieldName:\n"
                    f"      classType: \"{field.get('class')}\"\n"
                    f"      oldFieldName: \"{field.get('old')}\"\n"
                    f"      newFieldName: \"{field.get('new')}\"\n"
                )
                execute_single_recipe(snippet,
                                      f"Field rename failed: [class: {field.get('class')}] '{field.get('old')}' -> '{field.get('new')}'",
                                      f"Field rename success: [class: {field.get('class')}] '{field.get('old')}' -> '{field.get('new')}'")

            for method in methods:
                snippet = (
                    "  - org.openrewrite.java.ChangeMethodName:\n"
                    f"      methodPattern: \"{method.get('old')}\"\n"
                    f"      newMethodName: \"{method.get('new')}\"\n"
                )
                execute_single_recipe(snippet,
                                      f"Method rename failed: '{method.get('old')}' -> '{method.get('new')}'",
                                      f"Method rename success: '{method.get('old')}' -> '{method.get('new')}'")

            for cls in classes:
                snippet = (
                    "  - org.openrewrite.java.ChangeType:\n"
                    f"      oldFullyQualifiedTypeName: \"{cls.get('old')}\"\n"
                    f"      newFullyQualifiedTypeName: \"{cls.get('new')}\"\n"
                )
                execute_single_recipe(snippet,
                                      f"Class rename failed: '{cls.get('old')}' -> '{cls.get('new')}'",
                                      f"Class rename success: '{cls.get('old')}' -> '{cls.get('new')}'")

            for pkg in packages:
                snippet = (
                    "  - org.openrewrite.java.ChangePackage:\n"
                    f"      oldPackageName: \"{pkg.get('old')}\"\n"
                    f"      newPackageName: \"{pkg.get('new')}\"\n"
                )
                execute_single_recipe(snippet,
                                      f"Package move failed: '{pkg.get('old')}' -> '{pkg.get('new')}'",
                                      f"Package move success: '{pkg.get('old')}' -> '{pkg.get('new')}'")

            cleanup_snippet = (
                "  - org.openrewrite.java.RemoveUnusedImports\n"
                "  - org.openrewrite.java.OrderImports\n"
                "  - org.openrewrite.java.format.AutoFormat\n"
            )
            execute_single_recipe(cleanup_snippet,
                                  "Cleanup failed: Failed to apply Import/Format",
                                  "Cleanup success: Successfully applied Import/Format")

        finally:
            if os.path.exists(backup_path):
                os.replace(backup_path, yaml_path)
            elif os.path.exists(yaml_path):
                os.remove(yaml_path)

        if len(triggers) == 0:

            results.append("Success: No global refactorings needed.")

        return results








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

class ImplementWorkerTool(Tool):
    name = "implement_worker_tool"
    description = (
        "A tool that performs coding tasks. It reads target files and applies precise modifications "
        "using a Search/Replace text format. It handles one file at a time."
    )
    inputs = {
        "task": {
            "type": "string",
            "description": "The overall feature or bug fix being implemented (Big Picture)."
        },
        "goal": {
            "type": "string",
            "description": "The final expected outcome or behavior after this task is complete."
        },
        "analysis_context": {
            "type": "string",
            "description":
                """
                CRITICAL: Must contain a detailed, file-by-file breakdown of what to change.
                Format: '- [File Path]: [Specific modification instructions]'.
                """
        },
        "files": {
            "type": "string",
            "description": "Comma-separated string of absolute target file paths."
        },
        "project_root": {
            "type": "string",
            "description": "Project root absolute path",
        },
    }

    output_type = "string"

    def forward(self, task: str, goal: str, analysis_context: str, files: str, project_root: str) -> str:
        task = task.strip()
        goal = goal.strip()
        files_str = str(files)
        project_root = str(project_root) # Gradle 명령어를 실행할 프로젝트 루트 경로

        # 2. 시스템 프롬프트 (출력 형식 강제)
        system_p = """
        You are a Precise Code Implementation Orchestrator. You are fully in charge of modifying the codebase.
        You have TWO distinct execution strategies. You must analyze the task and choose the correct one:
        
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
        """

        prompt = f"""
        Please process the information below and strictly adhere to the [OUTPUT RULES] for your response.
        
        Task: {task}
        Goal: {goal}
        Analysis Context: {analysis_context}
        Project Root: {project_root}
        [Target File: {files_str}]
        
        [OUTPUT RULES]
        Summarize each necessary part using the following format:
        - Part Name: One-line summary
        """

        agent = CodeAgent(
            model=model,
            verbosity_level=1,
            tools=[*mcp_tools, OpenRewriteRefactorTool(), find_java_symbol_in_file, grep_file, read_file_lines],

            instructions=system_p,
            max_steps=10,
            executor=custom_executor,
            use_structured_outputs_internally=True
        )

        final_text = agent.run(prompt)

        return json.dumps(
            {"ok": True, "result": str(final_text)},
            ensure_ascii=False,
        )
