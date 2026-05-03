import json
import logging
from pathlib import Path

import http_client
from qwen_agent.agents import Assistant
from qwen_agent.tools.base import BaseTool, register_tool
from qwen_agent.tools import WebSearch, WebExtractor, CodeInterpreter, PythonExecutor, SimpleDocParser, DocParser
from qwen_agent.utils.output_beautify import typewriter_print
from qwen_agent.gui import WebUI



import logging

# logging.basicConfig(level=logging.DEBUG)

# logging.getLogger("httpx").setLevel(logging.DEBUG)
# logging.getLogger("httpcore").setLevel(logging.DEBUG)
#
logging.basicConfig(level=logging.INFO)
#
logging.getLogger("openai").setLevel(logging.DEBUG)
logging.basicConfig(level=logging.DEBUG)
#
# logging.getLogger("qwen_agent").setLevel(logging.DEBUG)
# logging.getLogger("qwen_agent.llm").setLevel(logging.DEBUG)
# logging.getLogger("qwen_agent.agent").setLevel(logging.DEBUG)
# logging.getLogger("qwen_agent.tools").setLevel(logging.DEBUG)


ALLOWED_PREFIX = Path("/Users/kimminjun/Desktop/ai-project/ai-project/backend").resolve()


@register_tool("read_file_tool")
class ReadFileTool(BaseTool):
    name = "read_file_tool"
    description = "Read a file from local filesystem, only under the allowed backend directory."
    parameters = {
        "type": "object",
        "properties": {
            "path": {
                "type": "string",
                "description": "Absolute file path"
            }
        },
        "required": ["path"]
    }

    def call(self, params: str, **kwargs) -> str:
        print("[tool] read_file_tool called")
        try:
            data = json.loads(params)
            raw_path = data["path"]

            path = Path(raw_path).expanduser().resolve()

            if path != ALLOWED_PREFIX and ALLOWED_PREFIX not in path.parents:
                return f"ERROR: access denied. Allowed path prefix: {ALLOWED_PREFIX}"

            if not path.is_file():
                return f"ERROR: not a file: {path}"

            with path.open("r", encoding="utf-8") as f:
                return f.read()

        except Exception as e:
            return f"ERROR: {str(e)}"


llm_cfg = {
    "model_type": "oai",
    "model": "qwen3.6:35b-a3b",
    "model_server": "http://localhost:11434/v1",
    "api_key": "EMPTY",
    "generate_cfg": {
        "top_p": 0.8,
        "use_raw_api": "true"
    }
}

system_prompt = """
You are a production-grade AI assistant with access to tools.

# PRIMARY OBJECTIVE
Deliver accurate, concise, and useful answers while minimizing tool usage, latency, and cost.

---

# CORE PRINCIPLES

1. Accuracy first. Never fabricate facts.
2. Minimize tool usage. Tools are expensive and may fail.
3. Use at most ONE tool unless absolutely necessary.
4. If one tool result is sufficient, do not call another tool.
5. Prefer reasoning over tool usage whenever possible.

---

# TOOL USAGE DECISION

Before calling any tool, you MUST internally decide:

"Can I answer this confidently without a tool?"

- If YES → Answer directly.
- If NO → Use the most appropriate SINGLE tool.

---

# TOOL SELECTION RULES

## web_search
Use ONLY when:
- The query requires recent, real-time, or external information
- The answer cannot be reliably inferred from general knowledge

After using web_search:
- If results contain a clear answer → STOP and respond
- Do NOT call another tool

## web_extractor
Use ONLY when:
- The user explicitly asks for source content
- OR search results are insufficient and exact page content is required

Never use it if a search snippet already answers the question.

## code_interpreter
Use ONLY when:
- Complex computation is required
- Structured data processing is needed
- Code execution is necessary for correctness

DO NOT use for:
- Simple math
- Date checks
- Formatting
- Basic logic

---

# HARD CONSTRAINTS

- Never call tools for:
  - Greetings
  - Explanations
  - Summaries
  - Rewriting
  - Debugging discussions (unless external docs are required)

- Never call multiple tools in sequence without a clear necessity
- Never retry a tool more than once
- Never use a second tool just to “double-check”

---

# FAILURE HANDLING

If a tool fails:
1. Retry at most once
2. If it still fails:
   - Briefly state the failure
   - Continue with best possible answer without the tool

---

# RESPONSE STYLE

- Be concise and direct
- Avoid unnecessary verbosity
- Clearly separate:
  - Known facts
  - Assumptions
  - Uncertainty

- Do NOT hallucinate missing information

---

# PRIORITY ORDER

1. User intent
2. Accuracy
3. Tool minimization
4. Clarity

---

# SUMMARY RULE

Use tools ONLY when they are clearly necessary.
One tool is usually enough.
No tool is preferred when possible.
"""



bot = Assistant(
    llm=llm_cfg,
    system_message=system_prompt,
    function_list=[
        "read_file_tool",
        WebSearch(),
        WebExtractor(),
        CodeInterpreter(),
        PythonExecutor(),
        SimpleDocParser(),
        DocParser()
    ]
)


WebUI(bot).run()

# messages = [
#     {
#         "role": "user",
#         "content": (
#             "다음 경로의 파일을 읽고 어떤 파일인지 분석해줘 /Users/kimminjun/Desktop/ai-project/ai-project/backend/text.txt"
#         )
#     }
# ]

# Step 4: Run the agent as a chatbot.
# messages = []  # This stores the chat history.
# while True:
#     # For example, enter the query "draw a dog and rotate it 90 degrees".
#     query = input('\nuser query: ')
#     # Append the user query to the chat history.
#     messages.append({'role': 'user', 'content': query})
#     response = []
#     response_plain_text = ''
#     print('bot response:')
#     for response in bot.run(messages=messages):
#         # Streaming output.
#         response_plain_text = typewriter_print(response, response_plain_text)
#     # Append the bot responses to the chat history.
#     messages.extend(response)