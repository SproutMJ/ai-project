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

# ================= [추가된 필터 클래스] =================
class OpenAIStreamliningFilter(logging.Filter):
    def filter(self, record):
        # 인자값이 결합된 전체 메시지를 가져옵니다.
        full_message = record.getMessage()

        # 첫 번째 줄만 추출 (\n 기준 분리)
        first_line = full_message.split('\n')[0]

        # 100자를 초과하면 자르고 '...' 추가
        if len(first_line) > 100:
            record.msg = first_line[:100] + "..."
        else:
            record.msg = first_line

        # 메시지가 이미 결합되었으므로 포맷팅 인자는 비워줍니다.
        record.args = ()
        return True
# =======================================================

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
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
    logging.getLogger("qwen_agent").setLevel(logging.INFO)

    # openai 로거 설정 (하위 로거까지 DEBUG 레벨을 허용하도록 상위에서 설정)
    logging.getLogger("openai").setLevel(logging.DEBUG)
    openai_logger = logging.getLogger("openai")

    if not openai_logger.handlers:
        handler = logging.StreamHandler()

        formatter = ColoredFormatter(
            "%(log_color)s%(asctime)s [OPENAI] %(levelname)-8s %(message)s",
            log_colors=LOG_COLORS,
        )

        handler.setFormatter(formatter)

        # [핵심 변경점] 하위 로거에서 전파(Propagate)되어 올라오는
        # 대용량 로그를 최종 출력 직전에 커팅하기 위해 핸들러에 필터를 등록합니다.
        handler.addFilter(OpenAIStreamliningFilter())

        openai_logger.addHandler(handler)

    openai_logger.propagate = False
