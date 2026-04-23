from developer.guard import assert_read, to_absolute, BASE_DIR

PROMPT_DIR = BASE_DIR / "agent" / "developer" / "prompts"


def load_prompt(name: str) -> str:
    path = PROMPT_DIR / name
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def read_files(paths):
    contents = {}

    for p in paths:
        abs_path = to_absolute(p)
        assert_read(p)

        try:
            with open(abs_path, "r", encoding="utf-8") as f:
                contents[p] = f.read()
        except FileNotFoundError:
            contents[p] = ""

    return contents