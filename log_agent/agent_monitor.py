import asyncio
import os
import aiofiles

# 환경변수에서 감지할 로그 파일 경로를 가져옴 (기본값 설정)
LOG_FILE_PATH = os.getenv("LOG_FILE_PATH", "/app/logs/target.log")

# smolagents 모듈 임포트
from smolagents import ToolCallingAgent, OpenAIModel, CodeAgent
# smolagents의 내장 웹 검색 도구를 WebSearchTool 이름으로 매핑
from smolagents import DuckDuckGoSearchTool as WebSearchTool


# 로컬/원격 LLM 설정 (OpenAI API 호환 서버 연결)
# 도커 컴포즈에 설정한 환경 변수를 최우선으로 가져오고, 없으면 기본값(하드코딩) 사용
model3 = OpenAIModel(
    model_id="unsloth/Qwen3.6-35B-A3B-GGUF:UD-Q4_K_M",
    api_base=os.getenv("LLM_API_BASE", ""),
    api_key=os.getenv("LLM_API_KEY", "YOUR_API_KEY"),
    temperature=0.1,
    max_tokens=32000,
    top_p=0.5,

    # 일부 kwargs는 서버 구현체(llama.cpp, vLLM 등)에 따라 무시될 수 있으나 에이전트 설정으로 전달
    # parallel_tool_calls=False,
    # reasoning_effort="none",
)

# 에이전트에 지시할 시스템 프롬프트 (필요시 구체화)
system_prompt = """
Your mission is to analyze system error logs and provide fast, accurate, and immediately applicable solutions for production environments.

[Instructions]
1. Absolutely NO unnecessary greetings, filler words, or introductions.
2. Solutions must be concrete and actionable (e.g., exact code modifications, configuration changes, or specific shell commands), not abstract concepts.
3. If there are multiple possible causes, list them in order of highest probability.
4. All responses MUST be written in English.
5. Strictly use Markdown formatting to maximize readability.
"""

# WebSearchTool을 장착한 에이전트 초기화
agent = ToolCallingAgent(
    tools=[WebSearchTool()],
    model=model3,
    instructions=system_prompt,
    stream_outputs=True,
    add_base_tools=False,
    verbosity_level=1,
)

async def send_notification(log_msg: str, analysis: str):
    """[비동기 작업 3] 분석 결과를 알림으로 전송 (Slack, Discord 등으로 확장 가능)"""
    # 실제 운영 환경에서는 여기서 httpx.AsyncClient()를 사용하여 Slack Webhook 등을 호출하면 됩니다.
    print("\n" + "🔥" * 30, flush=True)
    print("🚨 [에러 감지 및 해결 알림] 🚨", flush=True)
    print(f"▶ 발생 로그: {log_msg}", flush=True)
    print("-" * 60, flush=True)
    print(f"💡 [smolagents 분석 결과]\n{analysis}", flush=True)
    print("🔥" * 30 + "\n", flush=True)

async def analyze_and_alert(log_msg: str):
    """[비동기 작업 2] 에이전트 분석 및 알림을 처리하는 독립된 작업 단위"""
    print(f"🔍 [진행중] 에러 로그 감지! 에이전트 원인 분석 시작... : {log_msg}", flush=True)
    prompt = f"""
    Analyze the following system error log and provide your response exactly in the format below.

    [Error Log]
    {log_msg}
    
    [Response Format]
    ### 🔍 1. Error Summary (1-2 clear sentences)
    (Write the exact cause here)
    
    ### 🚨 2. Detailed Analysis
    (Explain the technical background of why this error occurred)
    
    ### 🛠️ 3. Immediate Actionable Solutions (At least 1 concrete action plan)
    - [ ] Solution 1: (1-2 clear sentences)
    """

    try:
        # 핵심 비동기화 1: smolagents의 agent.run()은 내부적으로 동기(Sync) 방식으로 작동하여 스레드를 블로킹합니다.
        # 이를 이벤트 루프가 멈추지 않도록 별도의 워커 스레드에서 실행(to_thread)시킵니다. (Python 3.9+ 제공)
        analysis_result = await asyncio.to_thread(agent.run, prompt)

        # 분석이 완료되면 알림 전송
        await send_notification(log_msg, analysis_result)

    except Exception as e:
        print(f"❌ 에러 분석 중 오류 발생: {e}", flush=True)

async def tail_log_file():
    """[비동기 작업 1] 로그 파일을 멈춤 없이 실시간으로 모니터링"""
    # 디렉토리 및 파일이 없을 경우 사전 생성
    os.makedirs(os.path.dirname(LOG_FILE_PATH), exist_ok=True)
    if not os.path.exists(LOG_FILE_PATH):
        with open(LOG_FILE_PATH, 'w') as f:
            f.write("")

    async with aiofiles.open(LOG_FILE_PATH, mode='r') as f:
        # 파일의 맨 끝으로 이동하여 스크립트 실행 이후의 로그부터 감지
        await f.seek(0, 2)
        print(f"👀 [{LOG_FILE_PATH}] 실시간 에러 모니터링 파이프라인이 가동되었습니다...", flush=True)

        while True:
            line = await f.readline()
            if not line:
                await asyncio.sleep(0.5) # 새로운 로그가 없으면 잠시 대기 (CPU 과부하 방지)
                continue

            line = line.strip()
            # 감지할 키워드 조건 지정
            if "ERROR" in line or "Exception" in line:
                # 핵심 비동기화 2: 에러가 발생하면 분석 함수를 백그라운드 태스크로 던집니다(Fire-and-Forget).
                # 이로 인해 LLM이 분석하느라 수 초가 걸려도, 이 루프는 즉시 다음 줄의 로그를 읽으러 이동합니다.
                asyncio.create_task(analyze_and_alert(line))

if __name__ == "__main__":
    # 비동기 메인 루프 실행
    asyncio.run(tail_log_file())
