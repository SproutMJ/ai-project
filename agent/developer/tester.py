import subprocess

from developer.guard import PROJECT_ROOT

def run_tests():
    result = subprocess.run(
        ["./gradlew", "test"],
        cwd=PROJECT_ROOT,
        capture_output=True,
        text=True
    )

    return {
        "success": result.returncode == 0,
        "output": result.stdout + result.stderr
    }