import json
import subprocess
from pathlib import Path

from qwen_agent.tools.base import BaseTool, register_tool

ALLOWED_PREFIX = Path("").resolve()

ALLOWED_TASKS = {
    "test": ["./gradlew", "test"],
    "build": ["./gradlew", "build"],
    "clean_test": ["./gradlew", "clean", "test"],
}


def _resolve_under_allowed_prefix(raw_path: str) -> Path:
    path = Path(raw_path).expanduser()

    if path.is_absolute():
        resolved = path.resolve()
    else:
        resolved = (ALLOWED_PREFIX / path).resolve()

    if resolved != ALLOWED_PREFIX and ALLOWED_PREFIX not in resolved.parents:
        raise PermissionError(f"access denied. Allowed path prefix: {ALLOWED_PREFIX}")

    return resolved


@register_tool("read_file_tool")
class ReadFileTool(BaseTool):
    name = "read_file_tool"
    description = (
        f"Read one or more files from local filesystem, only under the allowed directory. "
        f"allowed prefix is: {ALLOWED_PREFIX}"
    )
    parameters = {
        "type": "object",
        "properties": {
            "paths": {
                "type": "array",
                "items": {
                    "type": "string",
                    "description": "Absolute file path",
                },
                "description": "List of absolute file paths",
            }
        },
        "required": ["paths"],
    }

    def call(self, params: str, **kwargs) -> str:
        print("[tool] read_file_tool called")
        try:
            data = json.loads(params)
            raw_paths = data["paths"]

            results = []
            for raw_path in raw_paths:
                path = Path(raw_path).expanduser().resolve()

                if path != ALLOWED_PREFIX and ALLOWED_PREFIX not in path.parents:
                    results.append({
                        "path": str(path),
                        "error": f"access denied. Allowed path prefix: {ALLOWED_PREFIX}",
                    })
                    continue

                if not path.is_file():
                    results.append({
                        "path": str(path),
                        "error": f"not a file: {path}",
                    })
                    continue

                with path.open("r", encoding="utf-8") as f:
                    results.append({
                        "path": str(path),
                        "content": f.read(),
                    })

            return json.dumps({"files": results}, ensure_ascii=False)

        except Exception as e:
            return f"ERROR: {str(e)}"


@register_tool("patch_file_tool")
class PatchFileTool(BaseTool):
    name = "patch_file_tool"
    description = (
        f"Apply patch-style file writes only under the allowed directory. "
        f"allowed prefix is: {ALLOWED_PREFIX}"
    )
    parameters = {
        "type": "object",
        "properties": {
            "patch": {
                "type": "object",
                "properties": {
                    "changes": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "properties": {
                                "file": {
                                    "type": "string",
                                    "description": "Relative or absolute file path",
                                },
                                "new_code": {
                                    "type": "string",
                                    "description": "Full updated file content",
                                },
                            },
                            "required": ["file", "new_code"],
                        },
                    },
                },
                "required": ["changes"],
            }
        },
        "required": ["patch"],
    }

    def call(self, params: str, **kwargs) -> str:
        print("[tool] patch_file_tool called")
        try:
            data = json.loads(params)
            patch = data["patch"]
            changes = patch.get("changes", [])

            if not isinstance(changes, list):
                return "ERROR: patch.changes must be a list"

            resolved_changes = []

            for change in changes:
                raw_path = change["file"]
                new_code = change["new_code"]

                abs_path = _resolve_under_allowed_prefix(raw_path)
                resolved_changes.append((abs_path, new_code))

            for abs_path, _ in resolved_changes:
                abs_path.parent.mkdir(parents=True, exist_ok=True)

            for abs_path, new_code in resolved_changes:
                with abs_path.open("w", encoding="utf-8") as f:
                    f.write(new_code)

            return json.dumps(
                {
                    "ok": True,
                    "changed_files": [str(path) for path, _ in resolved_changes],
                },
                ensure_ascii=False,
            )

        except Exception as e:
            return f"ERROR: {str(e)}"


@register_tool("run_gradle_tool")
class RunGradleTool(BaseTool):
    name = "run_gradle_tool"
    description = (
        f"Run a safe Gradle command for a Spring project under the allowed directory only. "
        f"Allowed prefix is: {ALLOWED_PREFIX}. "
        f"Allowed tasks: {', '.join(ALLOWED_TASKS.keys())}"
    )
    parameters = {
        "type": "object",
        "properties": {
            "project_root": {
                "type": "string",
                "description": "Absolute path to the Gradle project root directory",
            },
            "task": {
                "type": "string",
                "description": "Gradle task name. One of: test, build, clean_test",
            },
        },
        "required": ["project_root", "task"],
    }

    def call(self, params: str, **kwargs) -> str:
        print("[tool] run_gradle_tool called")
        try:
            data = json.loads(params)
            project_root = Path(data["project_root"]).expanduser().resolve()
            task = data["task"]

            if project_root != ALLOWED_PREFIX and ALLOWED_PREFIX not in project_root.parents:
                return json.dumps({
                    "ok": False,
                    "error": f"access denied. Allowed path prefix: {ALLOWED_PREFIX}",
                }, ensure_ascii=False)

            if not project_root.is_dir():
                return json.dumps({
                    "ok": False,
                    "error": f"not a directory: {project_root}",
                }, ensure_ascii=False)

            gradlew = project_root / "gradlew"
            if not gradlew.is_file():
                return json.dumps({
                    "ok": False,
                    "error": f"gradlew not found in: {project_root}",
                }, ensure_ascii=False)

            if task not in ALLOWED_TASKS:
                return json.dumps({
                    "ok": False,
                    "error": f"unsupported task: {task}",
                    "allowed_tasks": list(ALLOWED_TASKS.keys()),
                }, ensure_ascii=False)

            result = subprocess.run(
                ALLOWED_TASKS[task],
                cwd=str(project_root),
                capture_output=True,
                text=True,
                timeout=300,
            )

            log = (result.stdout or "") + (result.stderr or "")
            return json.dumps({
                "ok": result.returncode == 0,
                "returncode": result.returncode,
                "project_root": str(project_root),
                "task": task,
                "command": " ".join(ALLOWED_TASKS[task]),
                "log": log[-12000:],
            }, ensure_ascii=False)

        except subprocess.TimeoutExpired:
            return json.dumps({
                "ok": False,
                "error": "command timed out after 300 seconds",
            }, ensure_ascii=False)
        except Exception as e:
            return json.dumps({
                "ok": False,
                "error": str(e),
            }, ensure_ascii=False)