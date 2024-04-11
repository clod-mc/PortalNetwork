#!/usr/bin/env python3
import re
import subprocess
from pathlib import Path


def main() -> None:
    root_path = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        capture_output=True,
        check=True,
        encoding="utf8",
    ).stdout.strip()
    with Path(f"{root_path}/SUMMARY.md").open() as f:
        print(f.read())

    print("## Recent Changes")

    tag_count = 0
    last_tag = None
    commit_log = subprocess.run(
        ["git", "log", "--pretty=format:[%d] %h %s"],
        capture_output=True,
        encoding="utf8",
        check=True,
    ).stdout
    for commit_line in [ln.strip() for ln in commit_log.splitlines()]:
        m = re.search(r"^\[([^\]]*)\] (\S+) (.+)$", commit_line)
        assert m
        meta, sha, desc = m[1].strip(), m[2], m[3]

        tag = None
        if m := re.search(r"\(tag: ([^\)]+)\)", meta):
            tag_count += 1
            if tag_count == 4:
                break
            tag = m[1]
            if tag != last_tag:
                print(f"\n#### {tag}")
                last_tag = tag

        print(f"- {sha} {desc}")


main()
