from fastapi import FastAPI
from developer.jobgraph.runner import run_agent
from pydantic import BaseModel

app = FastAPI()

class RunRequest(BaseModel):
    task: str


@app.post("/run")
def run(req: RunRequest):
    result = run_agent(req.task)
    return result