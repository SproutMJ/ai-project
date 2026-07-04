import nest_asyncio
# nest_asyncio.apply()


import subprocess
from pathlib import Path
import re

from mcp import StdioServerParameters
from openai import OpenAI
from smolagents import CodeAgent, LiteLLMModel, OpenAIModel, ToolCallingAgent, MCPClient

import os

from logging_config import create_logger

logger = create_logger("TOOL")

ALLOWED_PREFIX = Path("/Users/kimminjun/IdeaProjects/www/ai-project/backend/trip").resolve()


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
    api_base="http://192.168.2.16:11434", # replace with 127.0.0.1:11434 or remote open-ai compatible server if necessary
    api_key="YOUR_API_KEY", # replace with API key if necessary
    num_ctx=58000, # ollama default is 2048 which will fail horribly. 8192 works for easy tasks, more is better. Check https://huggingface.co/spaces/NyxKrage/LLM-Model-VRAM-Calculator to calculate how much VRAM this will need for the selected model.
    max_tokens=58000,

    temperature=0.1,
    top_p=0.5,

    flatten_messages_as_text=False,
    chat_template_kwargs={"enable_thinking": False},
)

model2 = OpenAIModel(
    model_id="qwen3.6:35b-a3b", # This model is a bit weak for agentic behaviours though
    api_base="http://192.168.2.16:11434/v1", # replace with 127.0.0.1:11434 or remote open-ai compatible server if necessary

    api_key="YOUR_API_KEY",
    temperature=0.1,
    max_tokens=150000,
    top_p=0.5,

    parallel_tool_calls=False,

    reasoning_effort="none",
)

coding_llm_cfg = {
    "model_type": "oai",
    "model": "qwen3.6:35b-a3b",
    "model_server": "http://192.168.2.16:11434/v1",
    "api_key": "EMPTY",
    "generate_cfg": {
        "top_p": 0.5,
        "temperature": 0.1,
        "use_raw_api": True,
        "max_input_tokens": 58000,
        'extra_body': {'chat_template_kwargs': {'enable_thinking': False}},
    },
}

server_parameters = StdioServerParameters(
    command="npx",
    args=["-y", "@modelcontextprotocol/server-filesystem", "/Users/kimminjun/IdeaProjects/www/ai-project/backend/trip"],
    env=os.environ.copy() # npx 명령어를 찾을 수 있도록 현재 환경 변수 상속
)
mcp_client = MCPClient(server_parameters)
mcp_tools = mcp_client.get_tools()

from smolagents import Tool, ToolCallingAgent, LiteLLMModel

import gc
import json
import time

from smolagents import Tool, ToolCallingAgent


class AnalyzeProjectWorkerTool(Tool):
    name = "analyze_project_worker_tool"
    description = """
    Analyzes exactly one specified file for the given task and analysis points,
    then returns only the file's role, dependencies, and explicit modification/deletion points.
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
            "description": "CRITICAL: The core error summary, exception message, or your current diagnosis (e.g., 'AssertionFailedError due to missing delete logic').",
            "nullable": True,
        },
        "suspected_files": {
            "type": "string",
            "description": "Comma-separated string of absolute file paths suspected in previous steps (e.g., '/path/to/A.java, /path/to/B.java').",
            "nullable": True,
        },
    }

    output_type = "string"

    def _close_agent(self, agent) -> None:
        for method_name in ("close", "shutdown", "aclose", "cleanup"):
            method = getattr(agent, method_name, None)
            if callable(method):
                try:
                    method()
                except Exception:
                    pass
                break
        gc.collect()

    def forward(
            self,
            task: str,
            project_root: str,
            analysis_points: str,
            error_summary: str | None = None,
            suspected_files: str | None = None,
    ) -> str:
        logger.info("analyze_project_worker_tool called")
        start_time = time.monotonic()
        agent = None

        try:
            task = task.strip()
            project_root = project_root.strip()
            analysis_points = str(analysis_points or "")
            error_summary = error_summary or ""
            suspected_files = str(suspected_files or "")

            system_p = """
            # ROLE & MISSION
            You are a Project Structure Analyst Agent. Your mission is to explore the repository layout, identify exact integration points for new features, and trace the structural root causes of bugs. You do not write code; you provide the precise "map and diagnosis" for the Implementer.

            # STRATEGIC TRIAGE (CRITICAL)
            Stop blindly searching for symptoms. You must investigate based on the task type:
            - FOR BUG FIXES: Do not stop at the surface error. Trace the dependency chain to find the underlying structural flaw.
            - FOR FEATURE ADDITIONS: Identify the complete vertical slice required. Map the exact Controller (Entry), Service (Business Logic), and Repository/Entity (State) files needed for the integration.

            # CONSTRAINTS & EXECUTION RULES
            1. FAIL-SAFE TRIGGER: If you encounter an execution loop or consecutive tool-calling failures (exceeding 3 attempts), ABORT IMMEDIATELY. Summarize the task status, explain the bottleneck, and stop.
            2. START HERE: Always read the overview in `project root directory/project-structure/project-overview.md` first to understand the domain.
            3. STRICTLY NO FULL FILE READS: NEVER load an entire file. It causes token overflow and immediate termination.
            4. SEMANTIC PEEKING (`one_file_analyzer_tool`): You MUST use this tool to examine file contents. Formulate highly specific `query` strings targeted at the root cause (e.g., "Extract JPA entity relationships and FK constraints", "Check transactional boundaries", "List method signatures for [X]").
            5. CONCISE OUTPUT: Keep your final output under 15 lines. The Orchestrator only needs to know WHICH specific absolute paths to modify and WHAT the exact structural root cause or integration strategy is.
            6. STAY IN BOUNDS: Only call explicit tools. Do not hallucinate paths or tools.

            # SUB-AGENT DELEGATION & TOOL RULES
            1. EXPLORE FIRST: You do not magically know file locations. Use filesystem tools to find exact absolute paths. NEVER guess or use truncated paths.
            2. EVIDENCE-BASED DIAGNOSIS: Never guess the internal logic of a code file. If you suspect a file contains the root cause, you MUST inspect it using `one_file_analyzer_tool` before finalizing your candidate list.
            3. PRECISE DELEGATION: When querying `one_file_analyzer_tool`, pass the exact `analysis_points` directed by the Orchestrator. Do not dilute or summarize the critical analysis points.
            """

            prompt = f"""
            Map the project structure and locate candidate files for the following task:
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

            [OUTPUT RESTRICTION]
            - Provide ONLY the structured list below.
            - Based on your analytical findings and the direction for improvement, briefly outline the required structural changes (DELETE/MODIFY/ADD) to guide the implementer.
            - Keep the entire response extremely brief to prevent token overflow.

            ### Candidate Files
            - `[Verified Absolute File Path 1]`: [1-sentence reason why this file is relevant] | **Action Required**: [explain (example: Requires MODIFYING entity relationship) or "Ready to implement"]
            ...

            ### Structural Dependencies
            - [Brief bullet point explaining how these target files call or interact with each other regarding the task]
            """

            # mcp_tools 는 기존 mcp_config에 해당하는 smolagents Tool 목록으로 넣어주세요.
            agent = CodeAgent(
                model=model2,
                tools=[*mcp_tools, OneFileAnalyzerTool()],
                instructions=system_p,
                max_steps=20,
            )

            final_text = agent.run(prompt)

            return json.dumps(
                {"ok": True, "result": str(final_text)},
                ensure_ascii=False,
            )

        except Exception as e:
            elapsed = time.monotonic() - start_time
            logger.info("analyze_project_worker_tool failed in %.2f sec", elapsed)
            return json.dumps(
                {"ok": False, "error": str(e)},
                ensure_ascii=False,
            )

        finally:
            if agent is not None:
                self._close_agent(agent)

            elapsed = time.monotonic() - start_time
            logger.info("analyze_project_worker_tool finished in %.2f sec", elapsed)



# LLM 설정 (필요에 따라 클래스 외부나 내부에 정의)
llm_cfg_call = {
    "model": "qwen3.6:35b-a3b",
    "base_url": "http://192.168.2.16:11434/v1",
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
        logger.info("one_file_analyzer_tool called")
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
                logger.warning(error_msg)
                return json.dumps({"ok": False, "error": error_msg}, ensure_ascii=False)

            if not target_path.exists() or not target_path.is_file():
                error_msg = f"File Not Found Error: The file '{target_path}' does not exist or is not a file."
                logger.warning(error_msg)
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
            logger.error("one_file_analyzer_tool failed in %.2f sec: %s", elapsed, str(e))
            return json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False)

        finally:
            elapsed = time.monotonic() - start_time
            logger.info("one_file_analyzer_tool finished in %.2f sec", elapsed)


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
        logger.info("run_gradle_tool called")
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
            Use the exact section headers below and do not use JSON.

            STATUS
            FAILED

            SELECTED_ISSUE
            the one problem chosen to solve first

            FAILURE_TYPE
            one of the allowed categories

            FAILED_TARGET
            task or test method name

            LOCATION
            file_path:symbol or unknown

            ERROR_SUMMARY
            exception type and short message

            LOG_BASED_INFERENCE
            1-2 sentences explaining what the log most strongly suggests

            CANDIDATE_FILE_BUNDLE
            1. absolute file path — why this file belongs to the chosen issue bundle — confidence: high|medium|low

            ROOT_CAUSE
            1-2 sentences. If `previous_actions_taken` failed, explicitly state if existing local/method-level code is shadowing/overriding the added code.

            REQUIRED_OPERATOR
            Must be one of: [REQUIRE_DELETE], [REQUIRE_MODIFY], [REQUIRE_ADD].

            * Use REQUIRE_ADD for missing implementations.
            * If a previous REQUIRE_ADD failed, strongly consider switching to REQUIRE_DELETE or REQUIRE_MODIFY to resolve scope conflicts.

            FIX_DIRECTION
            Brief action item describing what should be changed first based on the REQUIRED_OPERATOR.

            CONFIDENCE
            high|medium|low
            """


            agent = CodeAgent(
                model=model2,
                tools=[],
                instructions=system_p,
                max_steps=20,
                use_structured_outputs_internally=True,
                verbosity_level=1
            )

            final_text = agent.run(prompt)

            return json.dumps(
                {"result": str(final_text)},
                ensure_ascii=False,
            )

        except subprocess.TimeoutExpired:
            elapsed = time.monotonic() - start_time
            logger.info( "run_gradle_tool failed in %.2f sec", elapsed )
            return json.dumps({
                "error": "command timed out after 300 seconds",
            }, ensure_ascii=False)
        except Exception as e:
            elapsed = time.monotonic() - start_time
            logger.info( "run_gradle_tool failed in %.2f sec", elapsed )
            return json.dumps({
                "error": str(e),
            }, ensure_ascii=False)

        finally:
            elapsed = time.monotonic() - start_time
            logger.info( "run_gradle_tool finished in %.2f sec", elapsed )


class ImplementChangeWorkerTool(Tool):
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

    # def _close_agent(self, agent: Assistant) -> None:
    #     for method_name in ("close", "shutdown", "aclose"):
    #         method = getattr(agent, method_name, None)
    #         if callable(method):
    #             try:
    #                 method()
    #             except Exception:
    #                 pass
    #             break
    #     gc.collect()

    def forward(self, task: str, goal: str, analysis_context: str, files: str, project_root: str) -> str:
        logger.info("implement_worker_tool called (Text Parsing Mode)")
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
                    logger.error(f"Access Denied: {file_path} is outside ALLOWED_PREFIX")
                else:
                    valid_file_paths.append(file_path)


            if len(not_valid_file_paths) > 0:
                return json.dumps({"ok": False, "error": f"ABORTED: No valid files: {not_valid_file_paths} within: {ALLOWED_PREFIX}."})

            file_paths = valid_file_paths
            logger.info(f"deletetodo수정할 파일들: {file_paths}")


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
                logger.info(f"Targeting single file: {file_path}")

                if not file_path.exists() or not file_path.is_file():
                    results_summary[str(file_path)] = "Error: File not found."
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

                agent = CodeAgent(
                    model=model2,
                    tools=[],
                    instructions=system_p,
                    max_steps=20,
                    use_structured_outputs_internally=True
                )

                agent_text = str(agent.run(prompt))

                # return json.dumps(
                #     {"result": str(final_text)},
                #     ensure_ascii=False,
                # )

                logger.info("")
                logger.info(f"deletetodo파일코딩 블럭 결과: {file_path}")
                logger.info(f"{agent_text}")
                logger.info("")

                # 2. 코드 패치 및 RENAME 트리거 추출 실행
                patch_status = self._extract_and_apply_regex_patch(agent_text, file_path, original_content)
                # 에이전트의 의도를 추출하여 Gradle 명령어로 즉시 실행

                file_triggers = self._extract_refactoring_triggers(agent_text)
                global_refactor_triggers.extend(file_triggers)

                results_summary[str(file_path)] = {"patch_status": patch_status}

                # logger.info("")
                # logger.info(f"파일코딩 results_summary: {file_path}")
                # logger.info(f"{results_summary}")
                # logger.info("")


            results_summary["global_refactoring"] = self._execute_global_refactorings(global_refactor_triggers, project_root)

            return json.dumps({
                "results": results_summary
            }, ensure_ascii=False)

        except Exception as e:
            logger.error(f"implement_worker_tool failed: {e}")
            return json.dumps({"error": str(e)}, ensure_ascii=False)

        finally:
            elapsed = time.monotonic() - start_time
            logger.info(f"implement_worker_tool finished in {elapsed:.2f} sec")


    def _extract_and_apply_regex_patch(self, agent_text: str, file_path: Path, original_content: str) -> str:
        if "NO_CHANGES_NEEDED" in agent_text:
            logger.info(f"Agent determined no changes are needed for {file_path.name}")
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
            logger.warning(f"No valid SEARCH/REPLACE block found for {file_path.name}")
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
            logger.warning(f"Exact match failed. Attempting Regex fallback...")
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
                logger.error(f"Regex match also failed for a block in {file_path.name}")

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
                logger.warning(f"Ignored hallucinated basic trigger [{action}]: old='{old_val}', new='{new_val}'")
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
                logger.warning(f"Ignored hallucinated field trigger [{action}]: class='{cls_val}', old='{old_val}'")
                continue

            triggers.append({
                "action": "RENAME_FIELD",  # 파이썬 핸들러 처리를 위해 내부 액션명 통일
                "class": cls_val,
                "old": old_val,
                "new": new_val
            })

        # 트리거가 하나도 없을 경우의 로그 처리
        if not triggers:
            logger.info("No valid OpenRewrite triggers found or needed.")

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
            logger.info("Agent determined no refactorings are needed")
            results.append("success. No global refactorings needed.")

        return results

    # 편의성을 위한 헬퍼 메서드 분리
    def _run_gradle(self, command: str, cwd: str) -> dict:
        import subprocess
        # logger.info(f"Executing Global Refactoring: {command}")
        return subprocess.run(command, shell=True, cwd=cwd, capture_output=True, text=True)
