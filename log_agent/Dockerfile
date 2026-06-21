FROM python:3.10-slim

WORKDIR /app

# 필수 패키지 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 스크립트 복사
COPY agent_monitor.py .

# 로그 파일이 저장될 볼륨 디렉토리 생성
RUN mkdir -p /app/logs

# 스크립트 실행
CMD ["python", "agent_monitor.py"]
