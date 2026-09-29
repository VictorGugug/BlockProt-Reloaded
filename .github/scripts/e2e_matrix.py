#!/usr/bin/env python3
import json
import sys
from pathlib import Path

import edition
import server_smoke_test as smoke

REPO_ROOT = Path(__file__).resolve().parents[2]


def edition_id(repo_root):
    marker = Path(repo_root) / ".github" / "EDITION"
    return marker.read_text(encoding="utf-8").strip() if marker.is_file() else "bpr"


def build_matrix(repo_root, folia_versions):
    config = edition.load_edition(edition_id(repo_root), repo_root)
    java = config["smoke"][0]["java"]
    declared = smoke.declared_versions(Path(repo_root))
    include = [{"project": "paper", "version": v, "java": java} for v in declared]
    include += [{"project": "folia", "version": v, "java": java} for v in declared if v in folia_versions]
    return {"include": include}


def main():
    folia = {v for group in smoke.fetch_json(f"{smoke.FILL_API}/folia")["versions"].values() for v in group}
    print(json.dumps(build_matrix(REPO_ROOT, folia), separators=(",", ":")))


if __name__ == "__main__":
    sys.exit(main())
