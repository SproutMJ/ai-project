import os

from mcp import StdioServerParameters

import smolsubagents
from logging_config import create_logger, setup_external_loggers

from phoenix.otel import register
from openinference.instrumentation.smolagents import SmolagentsInstrumentor

register()
SmolagentsInstrumentor().instrument()

# setup_external_loggers()

logger = create_logger("MAIN")

logger.info("main started")

from smolagents import CodeAgent, LiteLLMModel, OpenAIModel, ToolCallingAgent, GradioUI, MCPClient, PlanningStep

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

### 1. `analyze_project_worker_tool` (The "Eyes" - Discovery & Diagnosis)
* **When to use:** When you need to read the codebase, understand project structure, or identify the root cause of a bug before writing any code.
* **How to use:** Provide specific `analysis_points` targeting entity structures and related code. Gather facts, do not guess.
* **⚠️ PRECAUTIONS:** * This tool CANNOT modify code. 
    * If analysis shows the code already meets requirements, DO NOT invoke the implementation tool. Explain the findings to the user and end the task.

### 2. `implement_worker_tool` (The "Hands" - Code Modification)
* **When to use:** ONLY when you have concrete, verified absolute file paths and specific modification instructions.
* **How to use:** Pass the exact absolute file paths and the identified root cause *exactly as they are (Raw)* into the `analysis_context` and `files` parameters. 
* **⚠️ PRECAUTIONS:**
    * The Implementer is "Blind". You MUST map the specific modifications required for EACH file individually in the `analysis_context` (e.g., `- [FilePath]: [What to change]`).
    * **Global Refactoring:** Explicitly instruct this tool to "Use a Rename Trigger for [OldName] to [NewName]" instead of manually modifying strings if renaming is required across multiple files.
    * **Abort Condition:** If this tool fails, stop all actions immediately and report to the user.

### 3. `run_gradle_tool` (The "Verifier" - Testing & Feedback)
* **When to use:** To verify that the applied code modifications actually fixed the issue.
* **⚠️ PRECAUTIONS (Anti-Loop):**
    * Go back to using `analyze_project_worker_tool` to re-analyze the domain structure from a new perspective. Do not apply surface-level band-aids.
"""



mcp_config = StdioServerParameters(
    command="npx",
    args=["-y", "@modelcontextprotocol/server-filesystem", "/Users/kimminjun/IdeaProjects/www/ai-project/backend/trip"],
    env=os.environ.copy() # npx 명령어를 찾을 수 있도록 현재 환경 변수 상속
)

from smolagents import WebSearchTool
import smoltools
agent = CodeAgent(
    tools=[smoltools.RunGradleTool(), WebSearchTool()],
    managed_agents=[smolsubagents.implementSubAgent],
    # managed_agents=[smolsubagents.analyzeProjectSubAgent, smolsubagents.implementSubAgent],
    model=model2,
    instructions=system_prompt,
    stream_outputs=True,
    planning_interval=4,
    add_base_tools=False,
    verbosity_level=1
)

gradio_ui = GradioUI(agent, file_upload_folder="uploads", reset_agent_memory=False)
gradio_ui.launch()
