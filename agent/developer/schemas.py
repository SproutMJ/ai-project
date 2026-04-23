from pydantic import BaseModel
from typing import List

class Plan(BaseModel):
    goal: str
    target_files: List[str]
    steps: List[str]
    risks: List[str]

class Patch(BaseModel):
    changes: list