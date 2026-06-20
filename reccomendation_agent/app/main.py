import os
import asyncio
import json
from fastapi import FastAPI, HTTPException
from typing import List

from app.models import RecommendationRequest, AiRecommendation, AiRouteRecommendation, RoutePlanRequest

# smolagents 모듈 임포트
from smolagents import ToolCallingAgent, OpenAIModel, CodeAgent
# smolagents의 내장 웹 검색 도구를 WebSearchTool 이름으로 매핑
from smolagents import DuckDuckGoSearchTool as WebSearchTool

app = FastAPI(title="Smolagent Recommendation API")

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
You are a professional AI travel guide. 
Your goal is to provide travel recommendations based on the user's request using the available tools.
1. Use the search tool to gather accurate information.
2. After gathering information, synthesize the data into a JSON array that strictly follows the provided schema.
3. NEVER provide explanations, intros, or outros. Output ONLY the JSON array.
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

code_agent = CodeAgent(
    tools=[WebSearchTool()],
    model=model3,
    instructions=system_prompt,
    stream_outputs=True,
    add_base_tools=False,
    verbosity_level=1,
)

@app.post("/api/recommend", response_model=List[AiRecommendation])
async def generate_recommendations(request: RecommendationRequest):
    """
    Spring Boot에서 호출할 엔드포인트입니다.
    비동기 처리(asyncio.to_thread)를 통해 FastAPI의 이벤트 루프가 블로킹되는 것을 방지합니다.
    """

    target_language = "한국어"
    prompt_text = f"""
    Analyze the user's request and provide 1-2 travel recommendations in a JSON array format.

    ### INSTRUCTION (MUST FOLLOW) ###
    1. The JSON keys must remain in English as shown in the example.
    2. ALL text values MUST be written in {target_language}.
    3. The JSON structure must match the example below.
    4. IMPORTANT: YOU MUST USE DOUBLE QUOTES (") FOR ALL KEYS AND STRING VALUES. 
       NEVER USE SINGLE QUOTES ('). YOUR OUTPUT MUST BE VALID JSON FORMAT.
    
    User Request: {request.prompt}
    
    JSON Example(REFERENCE ONLY):
    [
      {{
        "name": "Gyeongbokgung Palace",
        "recommendationScore": 9.5,
        "shortComment": "The heart of Joseon dynasty history.",
        "type": "Historical Site",
        "region": "Seoul",
        "keyword": "Culture, History, Hanbok",
        "theme": "Traditional",
        "budget": "10,000 KRW",
        "requiredTime": "3 hours",
        "howToGo": "Subway Line 3, Gyeongbokgung Station",
        "recommendedPartySize": "1-4 people",
        "weather": "All seasons",
        "language": "Korean, English",
        "disadvantage": "Very crowded on weekends",
        "description": "The main royal palace built in 1395, offering a glimpse into the majestic past of Korea."
      }}
    ]
    """

    try:
        # LLM 추론 및 웹 검색은 쓰레드 풀로 넘겨 FastAPI 서버 블로킹 방지
        response = await asyncio.to_thread(agent.run, prompt_text)

        # 에이전트가 Markdown 코드 블록(```json ... ```)으로 감싸서 줄 수 있으므로 파싱 처리
        clean_response = str(response).strip()
        if clean_response.startswith("```json"):
            clean_response = clean_response[7:]
        if clean_response.endswith("```"):
            clean_response = clean_response[:-3]

        result_json = json.loads(clean_response)

        # Pydantic을 이용해 검증 후 반환
        return [AiRecommendation(**item) for item in result_json]

    except json.JSONDecodeError as e:
        print(f"JSON Parsing Error: {e}\nResponse: {response}")
        raise HTTPException(status_code=500, detail="LLM이 올바른 JSON을 생성하지 못했습니다.")
    except Exception as e:
        print(f"Agent Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/route-plan", response_model=AiRouteRecommendation)
async def generate_route_plan(request: RoutePlanRequest):
    """
    Spring Boot에서 호출할 여행 경로 플래닝 엔드포인트입니다.
    """

    target_language = "한국어"

    # 프롬프트 구성 (요청받은 여행 정보를 모두 포함)
    prompt_text = f"""
    You are an expert travel planner. Create a highly detailed travel itinerary based on the user's request.
    
    [User Details]
    - Prompt: {request.prompt}
    - Start Date: {request.startDate}
    - End Date: {request.endDate}
    - Region: {request.region}
    - Budget: {request.budget}

    ### INSTRUCTION (MUST FOLLOW) ###
    1. Provide exactly ONE JSON object (NOT an array) representing the full travel route.
    2. ALL text values MUST be written in {target_language}.
    3. You must construct a logical sequence for each day.
    4. "scheduleItems" must alternate or logically flow between "NODE" (places to visit/eat/sleep) and "EDGE" (transportation between nodes).
    5. Dates in "startTime" and "endTime" must follow the ISO 8601 format (e.g., "YYYY-MM-DDTHH:MM:SS") and match the User's Start and End Dates.
    6. IMPORTANT: YOU MUST USE DOUBLE QUOTES (") FOR ALL KEYS AND STRING VALUES. YOUR OUTPUT MUST BE VALID JSON FORMAT.

    JSON Example (REFERENCE ONLY):
    {{
      "name": "제주도 2박 3일 힐링 투어",
      "recommendationScore": 9.5,
      "shortComment": "자연과 함께하는 여유로운 제주 여행",
      "budget": "500,000 KRW",
      "region": "제주도",
      "daySchedules": [
        {{
          "dayNumber": 1,
          "name": "제주 도착 및 서쪽 해안 코스",
          "scheduleItems": [
            {{
              "sequence": 1,
              "itemType": "NODE",
              "nodeType": "관광",
              "name": "협재 해수욕장",
              "region": "제주 제주시 한림읍",
              "shortComment": "투명한 에메랄드빛 바다",
              "budget": "0 KRW",
              "startTime": "2026-06-20T14:00:00",
              "endTime": "2026-06-20T15:30:00",
              "transportType": null,
              "travelMinutes": null,
              "description": "아름다운 일몰과 함께 사진 찍기 좋은 곳입니다."
            }},
            {{
              "sequence": 2,
              "itemType": "EDGE",
              "nodeType": null,
              "name": null,
              "region": null,
              "shortComment": null,
              "budget": null,
              "startTime": null,
              "endTime": null,
              "transportType": "렌터카",
              "travelMinutes": 30,
              "description": "협재 해수욕장에서 다음 장소로 해안도로를 따라 이동"
            }}
          ]
        }}
      ]
    }}
    """

    try:
        # LLM 추론 및 웹 검색은 쓰레드 풀로 넘겨 FastAPI 서버 블로킹 방지
        response = await asyncio.to_thread(code_agent.run, prompt_text)

        # 에이전트가 Markdown 코드 블록(```json ... ```)으로 감싸서 줄 수 있으므로 파싱 처리
        clean_response = str(response).strip()
        if clean_response.startswith("```json"):
            clean_response = clean_response[7:]
        if clean_response.endswith("```"):
            clean_response = clean_response[:-3]

        clean_response = clean_response.strip()

        result_json = json.loads(clean_response)

        # 단일 객체이므로 리스트 컴프리헨션 없이 바로 검증 후 반환
        return AiRouteRecommendation(**result_json)

    except json.JSONDecodeError as e:
        print(f"JSON Parsing Error: {e}\nResponse: {response}")
        raise HTTPException(status_code=500, detail="LLM이 올바른 JSON을 생성하지 못했습니다.")
    except Exception as e:
        print(f"Agent Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
