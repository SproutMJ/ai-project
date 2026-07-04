# 1. 기본 이미지
FROM python:3.10-bullseye

# 2. 필수 시스템 패키지 및 Java 설치 (반드시 root 권한일 때 한 번에 실행!)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        build-essential \
        python3-dev \
        openjdk-17-jdk \
        curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash -

RUN apt-get install -y nodejs

# 3. 작업 디렉토리 설정
WORKDIR /app

# 4. 파이썬 패키지 설치 (캐싱 최적화)
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir smolagents && \
    pip install --no-cache-dir -r requirements.txt

# 5. 에이전트 소스 코드 전체 복사
COPY . /app

# 6. Gradio 접속을 위한 포트 노출
EXPOSE 7860

# 7. 최종 실행 명령어
CMD ["python", "main.py"]
