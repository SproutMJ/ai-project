import logging


def setup_logging():
    root = logging.getLogger()
    if root.handlers:
        root.handlers.clear()

    logging.basicConfig(level=logging.INFO)
    logging.basicConfig(level=logging.DEBUG)

    logging.getLogger("openai").setLevel(logging.DEBUG)
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
    logging.getLogger("qwen_agent").setLevel(logging.INFO)


setup_logging()

# logging.basicConfig(level=logging.INFO)
# logging.getLogger("openai").setLevel(logging.DEBUG)
# logging.basicConfig(level=logging.DEBUG)
# #
# logging.getLogger("qwen_agent").setLevel(logging.DEBUG)
# logging.getLogger("qwen_agent.llm").setLevel(logging.DEBUG)
# logging.getLogger("qwen_agent.agent").setLevel(logging.DEBUG)
# logging.getLogger("qwen_agent.tools").setLevel(logging.DEBUG)


from qwen_agent.agents import Assistant
from qwen_agent.gui import WebUI
from qwen_agent.tools import WebSearch, SimpleDocParser, DocParser

import tools.tools  # noqa: F401  # 중요: decorator 등록을 위해 import만 해도 됨

llm_cfg = {
    "model_type": "oai",
    "model": "qwen3.6:35b-a3b",
    # "model": "qwen3.6:27b",
    "model_server": "http://localhost:11434/v1",
    "api_key": "EMPTY",
    "generate_cfg": {
        "top_p": 0.8,
        "use_raw_api": "true",
        'max_input_tokens': 58000,
        'extra_body': {'enable_thinking': False}
    },
}

mcp_config = {
    "mcpServers": {
        "filesystem": {
            "command": "npx",
            "args": ["-y", "@modelcontextprotocol/server-filesystem",
            "/Users"]
        }
    },
}

# system_prompt = """
# When an error occurs during tool execution or task processing, try to recover if the issue appears fixable.
# If the failure is non-recoverable or caused by an external dependency, such as a tool error, missing configuration, permission issue, or missing file, explain the cause clearly and stop the task.
# Never stop without providing a final user-facing answer.
# Always return a clear final conclusion.
# """
system_prompt = 'If the scope is large or unclear, narrow it before continuing.'

bot = Assistant(
    llm=llm_cfg,
    system_message=system_prompt,
    function_list=[
        mcp_config,
        # "read_file_tool",
        # "patch_file_tool",
        "run_gradle_tool",
        WebSearch(),
        SimpleDocParser(),
        DocParser(),
    ],
)

WebUI(bot).run()

# cli
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
