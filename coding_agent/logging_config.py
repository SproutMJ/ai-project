import logging
from colorlog import ColoredFormatter

LOG_FORMAT = (
    "%(log_color)s%(asctime)s "
    "[%(tag)s] "
    "%(levelname)-8s "
    "%(name)s: "
    "%(message)s"
)

LOG_COLORS = {
    "DEBUG": "cyan",
    "INFO": "green",
    "WARNING": "yellow",
    "ERROR": "red",
    "CRITICAL": "red,bg_white",
}


def create_logger(tag: str, level=logging.INFO):
    """
    tag:
        MAIN / TOOL / MCP / WORKER ...
    """

    logger = logging.getLogger(tag)

    # 중복 handler 방지
    if logger.handlers:
        return logger

    logger.setLevel(level)
    logger.propagate = False

    handler = logging.StreamHandler()

    formatter = ColoredFormatter(
        LOG_FORMAT,
        log_colors=LOG_COLORS,
    )

    handler.setFormatter(formatter)

    # formatter에서 %(tag)s 사용 가능하게
    handler.addFilter(lambda record: setattr(record, "tag", tag) or True)

    logger.addHandler(handler)

    return logger


def setup_external_loggers():
    """
    외부 라이브러리 로그 레벨 관리
    """

    logging.getLogger("openai").setLevel(logging.DEBUG)

    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)

    logging.getLogger("qwen_agent").setLevel(logging.INFO)

    # openai logger도 컬러 출력 붙이기
    openai_logger = logging.getLogger("openai")

    if not openai_logger.handlers:
        handler = logging.StreamHandler()

        formatter = ColoredFormatter(
            "%(log_color)s%(asctime)s [OPENAI] %(levelname)-8s %(message)s",
            log_colors=LOG_COLORS,
        )

        handler.setFormatter(formatter)
        openai_logger.addHandler(handler)

    openai_logger.propagate = False
