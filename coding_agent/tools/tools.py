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
            You are a code analysis agent.
            Based on the task goal, determine the appropriate analysis scope on your own. Review only the files and code paths that are relevant to the request, and avoid reading unrelated parts of the repository.
            """

            prompt = f"""
            Analyze the following task:
            Project Root Directory: {project_root}
            Task: {task}
            
            
            Your response should be structured and practical, and should include:
            - relevant files
            - core logic
            - impact scope
            - suggested modification direction
            - uncertainties or assumptions
            Keep responses concise, but include the important information in summarized form.
            """

            agent = Assistant(
                llm=llm_cfg,
                system_message=system_p,
                function_list=[mcp_config],
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
            You are a coding agent.
            You are not just a file-editing machine. You are an execution agent who uses the task details to build an implementation strategy, make the changes, validate the result, and report what was done.
                        
            Rules to follow
            - Do not make changes outside the impact scope. 
            - Make minimal changes
            - Follow the existing style
            - Do not perform unnecessary refactoring
            """


            prompt = f"""
            Implement the following task:
            {task}
            
            
            What to report (Keep responses concise, but include the important information in summarized form.)
            - Modified files
            - What was changed in each file
            - Why the change was made
            - Validation results
            - Other suggested notes
            """

            agent = Assistant(
                llm=llm_cfg,
                system_message=system_p,
                function_list=[mcp_config],
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
                You are a build and test failure analysis agent.

                Your job is to analyze build or test execution logs and extract the most useful information for follow-up debugging and implementation.
                
                Focus on:
                - the executed command
                - the exit code
                - the failure stage
                  - compile failure
                  - test failure
                  - lint/format failure
                  - other failure
                - the key error message
                - the most likely first root-cause location
                - the related file, class, or test
                - the suggested fix direction
                - the command that should be re-run
                - the failed test name or failed section, if available
                
                Rules:
                - Be precise and concise.
                - Do not invent facts that are not supported by the log.
                - If something is unclear, state that it is uncertain.
                - Prefer structured output.
                - Focus on what the next agent or developer needs in order to continue the work.
                
                Return a short, practical analysis that can be used directly by a lead agent.
                """


            prompt = f"""
                Analyze the following build or test log.

                Please provide:
                - executed command
                - exit code
                - failure stage
                - key error message
                - first likely root-cause location
                - related file, class, or test
                - suggested fix direction
                - command to re-run
                - failed part or failed test name
                
                project root: {project_root}
                
                executed command: {command}
                
                return code: {retcode}
            
                Log:
                {log}
                """

            agent = Assistant(
                llm=llm_cfg,
                system_message=system_p,
                function_list=[mcp_config],
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
