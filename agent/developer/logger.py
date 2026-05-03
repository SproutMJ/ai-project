import logging
import sys

agent_logger = logging.getLogger("agent")
agent_logger.setLevel(logging.INFO)

if not agent_logger.handlers:
    handler = logging.StreamHandler(sys.stdout)
    formatter = logging.Formatter(
        "[%(asctime)s] [%(levelname)s] [%(name)s] %(message)s"
    )
    handler.setFormatter(formatter)
    agent_logger.addHandler(handler)

agent_logger.propagate = False