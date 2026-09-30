#!/usr/bin/env python3
import json
import sys
from pathlib import Path

import edition
import server_smoke_test as smoke

REPO_ROOT = Path(__file__).resolve().parents[2]
PROJECTS = ("paper", "folia", "purpur")
FIRST_JAVA_25_MAJOR = 26


def edition_id(repo_root):
    marker = Path(repo_root) / ".github" / "EDITION"
    return marker.read_text(encoding="utf-8").strip() if marker.is_file() else "bpr"


def java_for(version, default):
    return 25 if int(version.split(".")[0]) >= FIRST_JAVA_25_MAJOR else default


def build_matrix(repo_root, available):
    config = edition.load_edition(edition_id(repo_root), repo_root)
    default_java = config["smoke"][0]["java"]
    declared = smoke.declared_versions(Path(repo_root))
    include, missing = [], []
    for project in PROJECTS:
        for version in declared:
            if version in available.get(project, ()):
                include.append({"project": project, "version": version, "java": java_for(version, default_java)})
            else:
                missing.append(f"{project} {version}")
    return {"include": include}, missing


def flatten(groups):
    return {v for group in groups.values() for v in group}


def main():
    available = {
        "paper": flatten(smoke.fetch_json(f"{smoke.FILL_API}/paper")["versions"]),
        "folia": flatten(smoke.fetch_json(f"{smoke.FILL_API}/folia")["versions"]),
        "purpur": set(smoke.purpur_versions()),
    }
    matrix, missing = build_matrix(REPO_ROOT, available)
    if missing:
        print("No server build for: " + ", ".join(missing), file=sys.stderr)
    print(json.dumps(matrix, separators=(",", ":")))


if __name__ == "__main__":
    sys.exit(main())
