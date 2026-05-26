import subprocess
from pathlib import Path

from qwen_agent.tools.base import BaseTool, register_tool
from qwen_agent.agents import Assistant

import gc
import json

from logging_config import create_logger

logger = create_logger("TOOL")

logger.info("implement_change_worker_tool called")
logger.warning("something suspicious")
logger.error("tool failed")




ALLOWED_PREFIX = Path("").resolve()

OLLAMA_API = "http://localhost:11434/api/chat"
OLLAMA_MODEL = "qwen3.6:35b-a3b"

ALLOWED_TASKS = {
    "test": ["./gradlew", "test", "--console=plain"],
    "build": ["./gradlew", "build", "--console=plain"],
    "clean_test": ["./gradlew", "clean", "test", "--console=plain"],
}

TEXT_EXTENSIONS = {
    ".java", ".kt", ".groovy", ".gradle", ".kts",
    ".xml", ".yml", ".yaml", ".properties", ".md", ".txt", ".json"
}

EXCLUDED_DIRS = {
    ".git", ".gradle", ".idea", ".vscode", "build", "out", "target",
    "node_modules", ".jdtls-workspace", ".venv", "venv"
}


llm_cfg = {
    "model_type": "oai",
    "model": "qwen3.6:35b-a3b",
    "model_server": "http://localhost:11434/v1",
    "api_key": "EMPTY",
    "generate_cfg": {
        "top_p": 0.1,
        "temperature": 0.0,
        "use_raw_api": "true",
        "max_input_tokens": 58000,
    },
}

mcp_config = {
    "mcpServers": {
        "filesystem": {
            "command": "npx",
            "args": [
                "-y",
                "@modelcontextprotocol/server-filesystem",
                "",
            ],
        }
    }
}


@register_tool("analyze_project_worker_tool")
class AnalyzeProjectWorkerTool(BaseTool):
    name = "analyze_project_worker_tool"
    description = "Investigates the requested coding task by reviewing relevant source and configuration files, then returns the impacted areas, dependencies, and suggested next actions for further implementation."
    parameters = {
        "type": "object",
        "properties": {
            "task": {
                "type": "string",
                "description": "Current user task",
            },
            "project_root": {
                "type": "string",
                "description": "Project root path",
            },
        },
        "required": ["task", "project_root"],
    }

    def _close_agent(self, agent: Assistant) -> None:
        for method_name in ("close", "shutdown", "aclose"):
            method = getattr(agent, method_name, None)
            if callable(method):
                try:
                    method()
                except Exception:
                    pass
                break
        gc.collect()

    def call(self, params: str, **kwargs) -> str:
        try:
            data = json.loads(params)
            task = data["task"].strip()
            project_root = data["project_root"].strip()

            system_p = """
            You are a Project Structure Analyst Agent. Your ONLY job is to explore the repository layout, identify candidate files relevant to the task, and map their high-level relationships.
            
            [Constraints]
            CRITICAL RULE: > If you encounter an execution loop or consecutive tool-calling failures (exceeding 3 attempts), you must abort the operation immediately. Instead of continuing redundant attempts, summarize the current task status, explain the bottleneck, and stop.
            1. First read overview in projectroot/project-structure/project-overview.md
            2. Focus on file locations and architectural structure. 
            3. DO NOT perform line-by-line logic analysis, and DO NOT suggest code modifications or fixes.
            4. STRICTLY NO FULL FILE READS: NEVER load an entire file. Loading full files causes severe token overflow and immediate termination.
            5. SEMANTIC PEEKING: To examine file contents, you MUST use `find_snippet_worker_tool`. Provide the file path and a descriptive `query` (e.g., "class signature and fields", "imports", "method names", or a specific function). The tool will extract the relevant section for you.
            6. Keep your final output under 15 lines. The next agent only needs to know WHICH files to look at and HOW they are structurally related.
            7. ONLY call tools explicitly provided in your toolset. Do not hallucinate non-existent tools or file paths.
            8. Execute ONLY the requested task. No extra info, commentary, or suggestions. Strictly adhere to the scope.
            """

            prompt = f"""
            Map the project structure and locate candidate files for the following task:
            Project Root Directory: {project_root}
            Task: {task}
            
            [EXECUTION FLOW]
            Follow these exact steps:
            1. OVERVIEW: Check `project-overview.md` for the overarching structure.
            2. LOCATE: Use `filesystem-search_files` to find exact absolute paths of files that match the task requirements. DO NOT guess paths.
            3. INSPECT (IF NECESSARY): Verify file roles by calling `find_snippet_worker_tool` with a target `query` (e.g., "class definition", "endpoint mapping"). Do NOT read the entire file.
            4. REPORT: Output the final mapping using ONLY the format below.
            
            [OUTPUT RESTRICTION]
            - Provide ONLY the structured list below. 
            - Do NOT include long descriptions, assumptions, or code modification directions.
            - Keep the entire response extremely brief to prevent token overflow.
            
            ### Candidate Files
            - `[Verified Absolute File Path 1]`: [1-sentence reason why this file is relevant]
            - `[Verified Absolute File Path 2]`: [1-sentence reason why this file is relevant]
            
            ### Structural Dependencies
            - [Brief bullet point explaining how these target files call or interact with each other regarding the task]
            """

            agent = Assistant(
                llm=llm_cfg,
                system_message=system_p,
                function_list=[mcp_config, "find_snippet_worker_tool"],
            )

            try:
                final_text = None

                for chunk in agent.run(messages=[{"role": "user", "content": prompt}]):
                    if isinstance(chunk, list):
                        for item in chunk:
                            if isinstance(item, dict) and item.get("role") == "assistant":
                                final_text = item.get("content", final_text)

                return json.dumps(
                    {"ok": True, "result": final_text},
                    ensure_ascii=False,
                )
            finally:
                self._close_agent(agent)

        except Exception as e:
            return json.dumps(
                {"ok": False, "error": str(e)},
                ensure_ascii=False,
            )











@register_tool("find_snippet_worker_tool")
class FindSnippetWorkerTool(BaseTool):
    name = "find_snippet_worker_tool"
    description = "Read a file directly, find the requested code snippet, and return exact lines and code with minimal tokens."
    parameters = {
        "type": "object",
        "properties": {
            "file_path": {
                "type": "string",
                "description": "The EXACT, verified absolute path to the target file. DO NOT GUESS. If unsure of the exact path, find path first."
            },
            "query": {
                "type": "string",
                "description": "Specific logic or section to extract."
            }
        },
        "required": ["file_path", "query"]
    }

    def _close_agent(self, agent) -> None:
        for method_name in ("close", "shutdown", "aclose"):
            method = getattr(agent, method_name, None)
            if callable(method):
                try:
                    method()
                except Exception:
                    pass
                break
        gc.collect()

    def call(self, params: str, **kwargs) -> str:
        try:
            data = json.loads(params)
            file_path = data["file_path"].strip()
            query = data["query"].strip()

            path = Path(file_path)
            allowed_path = Path(ALLOWED_PREFIX)

            if path != allowed_path and allowed_path not in path.parents:
                return json.dumps({"ok": False, "error": f"access denied. Use path inside {ALLOWED_PREFIX}."}, ensure_ascii=False)

            if path.is_dir():
                return json.dumps({"ok": False, "error": "is directory. Use filesystem-search_files or list_directory."}, ensure_ascii=False)

            if not path.exists():
                return json.dumps({"ok": False, "error": "not found. Stop guessing. Use filesystem-search_files tool first."}, ensure_ascii=False)

            # Python에서 파일 직접 읽기 및 줄 번호 추가
            with open(path, 'r', encoding='utf-8', errors='replace') as f:
                lines = f.readlines()

            numbered_content = "".join([f"{idx + 1} | {line}" for idx, line in enumerate(lines)])

            # 서브 에이전트용 초경량 시스템 프롬프트
            system_p = """"""


            prompt = f"""Extract exact code snippet from the provided text.
            CRITICAL RULES:
            1. If the target is inside a function/method, extract the ENTIRE function (from signature to closing brace).
            2. If the target is outside a function (e.g., class fields, imports), extract the target lines along with its immediate logical block or minimal surrounding context.
            3. Execute ONLY the requested task. No extra info, commentary, or suggestions. Strictly adhere to the scope.
            
            Target:\n{query}\n\nPath:\n{file_path}\n\nContent:\n{numbered_content}
            
            Strictly output ONLY in this format:
            File: <path>
            Lines: <start>-<end>
            Code:
            <snippet_code>
            """

            agent = Assistant(
                llm=llm_cfg,
                system_message=system_p,
            )

            try:
                final_text = None
                for chunk in agent.run(messages=[{"role": "user", "content": prompt}]):
                    if isinstance(chunk, list):
                        for item in chunk:
                            if isinstance(item, dict) and item.get("role") == "assistant":
                                final_text = item.get("content", final_text)

                return json.dumps({"ok": True, "snippet": final_text}, ensure_ascii=False)
            finally:
                self._close_agent(agent)

        except Exception as e:
            return json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False)






@register_tool("implement_worker_tool")
class ImplementChangeWorkerTool(BaseTool):
    name = "implement_worker_tool"
    description = (
        "A tool that can perform coding tasks. It should receive tasks in an actionable form. Whether to split the work into multiple steps or execute it in a single pass is up to the user."
    )
    parameters = {
        "type": "object",
        "properties": {
            "task": {
                "type": "string",
                "description": """Current user task include 
                1. Identify which files/modules are involved
                2. Identify the constraints that must be preserved and the parts that must not be changed
                3. Suggest the current modification direction
                4. Define what should be checked after completion and the acceptance criteria"""
            },
            # "paths": {
            #     "type": "array",
            #     "items": {"type": "string"},
            #     "description": "Relative or absolute file paths under the allowed root"
            # },
        },
        "required": ["task"]
    }

    def _close_agent(self, agent: Assistant) -> None:
        for method_name in ("close", "shutdown", "aclose"):
            method = getattr(agent, method_name, None)
            if callable(method):
                try:
                    method()
                except Exception:
                    pass
                break
    gc.collect()

    def call(self, params: str, **kwargs) -> str:
        print("[tool] analyze_files_worker_tool called")
        try:
            data = json.loads(params)
            task = data["task"].strip()

            system_p = """
            You are a Precise Code Execution Agent. Your ONLY job is to modify targeted code files accurately based on the given task and direction.
            
            [Constraints]
            CRITICAL RULE: > If you encounter an execution loop or consecutive tool-calling failures (exceeding 3 attempts), you must abort the operation immediately. Instead of continuing redundant attempts, summarize the current task status, explain the bottleneck, and stop.
            1. STRICTLY NO FULL FILE READS: NEVER read the entire file using `read_file` or similar tools. Loading full files causes token overflow. You MUST extract code context strictly using `find_snippet_worker_tool`.
            2. MANDATORY PATH & SNIPPET CHECK: Before modifying any file, if you do not have the exact verified absolute path, you MUST search for it first. Then, call `find_snippet_worker_tool` to get the exact code context and line numbers. Do NOT guess file contents or paths.
            3. MINIMAL EDITS: Apply the absolute minimum code changes required to resolve the task. Do NOT perform unnecessary refactoring or rewrite adjacent code.
            4. NO REASONING CHAT: Do not explain your strategy, architectural philosophies, or reasons behind the bug. Focus 100% on tool execution.
            5. Execute ONLY the requested task. No extra info, commentary, or suggestions. Strictly adhere to the scope.
            """

            prompt = f"""
            Implement the precise code modifications for the following task:
            {task}
            
            [EXECUTION FLOW]
            Follow these exact steps in order:
            1. PATH VERIFICATION: If the absolute path is not explicitly provided, call `filesystem-search_files` to find the exact file location. Do NOT guess the path.
            2. CONTEXT EXTRACTION: Call `find_snippet_worker_tool` using the verified absolute path to fetch the target code block.
            3. CODE MODIFICATION: Use your file editing tool (e.g., `filesystem-edit_file`) to apply the exact fix.
            4. REPORTING: Provide a brief modification report using ONLY the template below.
            
            [OUTPUT RESTRICTION]
            - Do NOT include rationales, validations, opinions, or conversational filler.
            - Output ONLY the structured layout below.
            
            ### Restrict Output
            - **File Path**: [Verified Absolute File Path]
            - **Line Range Edited**: [Start Line] - [End Line]
            - **Change Summary**: [1-sentence literal description of what was changed]
            """

            agent = Assistant(
                llm=coding_llm_cfg,
                system_message=system_p,
                function_list=[mcp_config, "find_snippet_worker_tool"],
            )

            try:
                final_text = None

                for chunk in agent.run(messages=[{"role": "user", "content": prompt}]):
                    if isinstance(chunk, list):
                        for item in chunk:
                            if isinstance(item, dict) and item.get("role") == "assistant":
                                final_text = item.get("content", final_text)

                return json.dumps(
                    {"ok": True, "result": final_text},
                    ensure_ascii=False,
                )
            finally:
                self._close_agent(agent)

        except Exception as e:
            return json.dumps(
                {"ok": False, "error": str(e)},
                ensure_ascii=False,
            )


@register_tool("run_gradle_tool")
class RunGradleTool(BaseTool):
    name = "run_gradle_tool"
    description = (
        f"Run a safe Gradle command for a Spring project under the allowed directory only and analyze the result. "
        f"Return a concise success or failure summary, including likely cause and next action if it fails. "
        f"Allowed prefix is: {ALLOWED_PREFIX}. "
        f"Allowed tasks: {', '.join(ALLOWED_TASKS.keys())}"
    )
    parameters = {
        "type": "object",
        "properties": {
            "project_root": {
                "type": "string",
                "description": "Absolute path to the Gradle project root directory",
            },
            "task": {
                "type": "string",
                "description": "Gradle task name. One of: test, build, clean_test",
            },
        },
        "required": ["project_root", "task"],
    }

    def _close_agent(self, agent: Assistant) -> None:
        for method_name in ("close", "shutdown", "aclose"):
            method = getattr(agent, method_name, None)
            if callable(method):
                try:
                    method()
                except Exception:
                    pass
                break
    gc.collect()


    def call(self, params: str, **kwargs) -> str:
        print("[tool] run_gradle_tool called")
        try:
            data = json.loads(params)
            project_root = Path(data["project_root"]).expanduser().resolve()
            task = data["task"]

            if project_root != ALLOWED_PREFIX and ALLOWED_PREFIX not in project_root.parents:
                return json.dumps({
                    "ok": False,
                    "error": f"access denied. Allowed path prefix: {ALLOWED_PREFIX}",
                }, ensure_ascii=False)

            if not project_root.is_dir():
                return json.dumps({
                    "ok": False,
                    "error": f"not a directory: {project_root}",
                }, ensure_ascii=False)

            gradlew = project_root / "gradlew"
            if not gradlew.is_file():
                return json.dumps({
                    "ok": False,
                    "error": f"gradlew not found in: {project_root}",
                }, ensure_ascii=False)

            if task not in ALLOWED_TASKS:
                return json.dumps({
                    "ok": False,
                    "error": f"unsupported task: {task}",
                    "allowed_tasks": list(ALLOWED_TASKS.keys()),
                }, ensure_ascii=False)

            result = subprocess.run(
                ALLOWED_TASKS[task],
                cwd=str(project_root),
                capture_output=True,
                text=True,
                timeout=300,
            )

            log = (result.stdout or "") + (result.stderr or "")
            if result.returncode == 0:
                return json.dumps(
                    {"ok": True, "result": "Test successful"},
                    ensure_ascii=False,
                )

            retcode = result.returncode
            command = " ".join(ALLOWED_TASKS[task])
            log = log[-12000:]


            system_p = """
            You are an advanced Build & Test Log Analyzer Agent. Your job is to deeply analyze build/test failures from raw logs, identify the root causes by reviewing related files, and provide actionable resolution steps.
            
            [Constraints]
            CRITICAL RULE: > If you encounter an execution loop or consecutive tool-calling failures (exceeding 3 attempts), you must abort the operation immediately. Instead of continuing redundant attempts, summarize the current task status, explain the bottleneck, and stop.
            1. TARGETED ANALYSIS: Focus exclusively on the failed tests or tasks. Do not speculate on unrelated warnings.
            2. ROOT CAUSE IDENTIFICATION: Pinpoint exactly why the failure occurred (e.g., specific code logic bugs, missing dependencies, incorrect configurations, or environment mismatches).
            3. ACTIONABLE SOLUTIONS: Provide concrete, step-by-step guidance or code snippets showing how to fix the problem in the related files.
            4. HIGH DENSITY, NO FLUFF: Keep the output concise, structured, and free of vague conversational filler.
            5. NEVER read the entire file using `read_file` or similar tools. You MUST extract code context strictly using `find_snippet_worker_tool` around the failed line numbers. Reading full files will cause token overflow and immediate termination.
            6. Execute ONLY the requested task. No extra info, commentary, or suggestions. Strictly adhere to the scope.
            """


            prompt = f"""
            Analyze the execution results and failure facts from the raw log provided below, and diagnose the underlying issue based on the context.
            
            Project Root: {project_root}
            Command: {command}
            Exit Code: {retcode}
            
            [Raw Log to Analyze]
            {log}
            
            [Analysis Process]
            Follow these exact steps in order:
            1. Parse the raw log to extract the failed test name, exact file name (e.g., MyTest.java), line number, and error message.
            2. If you do not know the exact absolute path of the file, FIRST call `filesystem-search_files` (or equivalent search tool) using the parsed file name to find the absolute path. DO NOT guess the path.
            3. Once the absolute path is verified, call `find_snippet_worker_tool` targeting the failure line and its surrounding context (e.g., +/- 10 lines or the enclosing method).
            4. Analyze the fetched code snippet to determine the exact root cause and formulate a resolution plan.

            [OUTPUT JSON TEMPLATE]
            {{
              "status": "FAILED",
              "failed_target": "task or test method name",
              "location": "file_path:line_number",
              "error_summary": "exception type and short message",
              "root_cause": "1-2 sentences explaining why it failed based on code snippet",
              "fix_action": "brief action item"
            }}
            """

            agent = Assistant(
                llm=llm_cfg,
                system_message=system_p,
                function_list=[mcp_config, "find_snippet_worker_tool"],
            )

            try:
                final_text = None

                for chunk in agent.run(messages=[{"role": "user", "content": prompt}]):
                    if isinstance(chunk, list):
                        for item in chunk:
                            if isinstance(item, dict) and item.get("role") == "assistant":
                                final_text = item.get("content", final_text)

                return json.dumps(
                    {"ok": False, "result": final_text},
                    ensure_ascii=False,
                )
            finally:
                self._close_agent(agent)

        except subprocess.TimeoutExpired:
            return json.dumps({
                "ok": False,
                "error": "command timed out after 300 seconds",
            }, ensure_ascii=False)
        except Exception as e:
            return json.dumps({
                "ok": False,
                "error": str(e),
            }, ensure_ascii=False)
