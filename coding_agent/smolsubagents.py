import subprocess
from pathlib import Path

from mcp import StdioServerParameters
from openai import OpenAI
from smolagents import CodeAgent, LiteLLMModel, OpenAIModel, MCPClient

import os

import smoltools

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
    # chat_template_kwargs={"enable_thinking": False},
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

import json
import time

from smolagents import Tool


analyzeProjectSubAgent = CodeAgent(
    verbosity_level=1,
    tools=[*mcp_tools, smoltools.OneFileAnalyzerTool()],
    model=model2,
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
            """,
    use_structured_outputs_internally=False,
)



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
            logger.info("Agent determined no refactorings are needed")
            results.append("Success: No global refactorings needed.")

        return results



implementSubAgent = CodeAgent(
    verbosity_level=1,
    tools=[OpenRewriteRefactorTool()],
    model=model2,
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


