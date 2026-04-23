from developer.guard import assert_write, to_absolute

def apply_patch(patch):
    for change in patch["changes"]:
        rel_path = change["file"]
        abs_path = to_absolute(rel_path)

        assert_write(rel_path)

        abs_path.parent.mkdir(parents=True, exist_ok=True)
        with open(abs_path, "w", encoding="utf-8") as f:
            f.write(change["new_code"])