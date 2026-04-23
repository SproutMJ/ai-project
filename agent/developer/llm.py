import requests

OLLAMA_URL = "http://localhost:11434/api/chat"

def chat(messages, model="qwen2.5-coder"):
    res = requests.post(OLLAMA_URL, json={
        "model": model,
        "messages": messages,
        "stream": False
    })
    return res.json()["message"]["content"]