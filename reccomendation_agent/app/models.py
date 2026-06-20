from pydantic import BaseModel, Field

# 스프링부트에서 보내는 요청 형식
class RecommendationRequest(BaseModel):
    prompt: str

# 스프링부트로 반환할 응답 형식 (DTO와 동일하게)
class AiRecommendation(BaseModel):
    name: str = Field(..., description="장소명")
    recommendationScore: float = Field(..., description="추천 점수 (0.0 ~ 5.0)")
    shortComment: str = Field(..., description="한 줄 평 (300자 이내)")
    type: str = Field(..., description="카테고리 (예: 자연, 문화, 식당)")
    region: str = Field(..., description="주소/지역")
    keyword: str = Field(..., description="키워드")
    theme: str = Field(..., description="테마")
    budget: str = Field(..., description="예산")
    requiredTime: str = Field(..., description="소요 시간")
    howToGo: str = Field(..., description="가는 법")
    recommendedPartySize: str = Field(..., description="추천 인원")
    weather: str = Field(..., description="추천 날씨")
    language: str = Field(..., description="사용 언어")
    disadvantage: str = Field(..., description="단점")
    description: str = Field(..., description="상세 설명")


from pydantic import BaseModel
from typing import List, Optional
from enum import Enum
from datetime import datetime

# 요청 모델 (Spring Boot가 전송하는 Map 데이터)
class RoutePlanRequest(BaseModel):
    prompt: str
    startDate: str
    endDate: str
    region: str
    budget: float | str  # BigDecimal은 보통 숫자로 오지만, 문자열로 올 수도 있으므로 유연하게 처리

# 아이템 타입 Enum
class ItemTypeEnum(str, Enum):
    NODE = "NODE"
    EDGE = "EDGE"

# 1. 최하위 스케줄 아이템 모델
class AiRouteScheduleItem(BaseModel):
    sequence: int
    itemType: ItemTypeEnum
    # NODE 전용
    nodeType: Optional[str] = None
    name: Optional[str] = None
    region: Optional[str] = None
    shortComment: Optional[str] = None
    budget: Optional[str] = None
    startTime: Optional[datetime] = None  # Spring의 LocalDateTime과 매핑 (ISO-8601 형식)
    endTime: Optional[datetime] = None
    # EDGE 전용
    transportType: Optional[str] = None
    travelMinutes: Optional[int] = None
    description: Optional[str] = None

# 2. 일차별 스케줄 모델
class AiRouteDaySchedule(BaseModel):
    dayNumber: int
    name: str
    scheduleItems: List[AiRouteScheduleItem]

# 3. 최상위 경로 추천 모델
class AiRouteRecommendation(BaseModel):
    name: str
    recommendationScore: float
    shortComment: str
    budget: str
    region: str
    daySchedules: List[AiRouteDaySchedule]
