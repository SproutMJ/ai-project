from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent.parent  # ai-project
PROJECT_ROOT = BASE_DIR / "backend" / "trip"

READ_ALLOWED = [
    PROJECT_ROOT,
]

WRITE_ALLOWED = [
    PROJECT_ROOT,
]

DELETE_ALLOWED = [
    PROJECT_ROOT,
]

FORBIDDEN = [
    BASE_DIR / "agent",
]


def to_absolute(path: str) -> Path:
    p = Path(path)
    if p.is_absolute():
        return p.resolve()
    return (PROJECT_ROOT / p).resolve()


def _is_under(path: Path, base: Path) -> bool:
    try:
        path.resolve().relative_to(base.resolve())
        return True
    except ValueError:
        return False


def _check_allowed(path: Path, allowed_list: list[Path]) -> bool:
    return any(_is_under(path, base) for base in allowed_list)


def _check_forbidden(path: Path) -> bool:
    return any(_is_under(path, base) for base in FORBIDDEN)


def can_read(path: str) -> bool:
    p = to_absolute(path)
    if _check_forbidden(p):
        return False
    return _check_allowed(p, READ_ALLOWED)


def can_write(path: str) -> bool:
    p = to_absolute(path)
    if _check_forbidden(p):
        return False
    return _check_allowed(p, WRITE_ALLOWED)


def can_delete(path: str) -> bool:
    p = to_absolute(path)
    if _check_forbidden(p):
        return False
    return _check_allowed(p, DELETE_ALLOWED)


def assert_read(path: str):
    if not can_read(path):
        raise PermissionError(f"Read not allowed: {path}")


def assert_write(path: str):
    if not can_write(path):
        raise PermissionError(f"Write not allowed: {path}")


def assert_delete(path: str):
    if not can_delete(path):
        raise PermissionError(f"Delete not allowed: {path}")