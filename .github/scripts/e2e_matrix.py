#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path

import edition
import server_smoke_test as smoke

REPO_ROOT = Path(__file__).resolve().parents[2]
PROJECTS = ("paper", "folia", "purpur")
FIRST_JAVA_25_MAJOR = 26


def java_for(version, default):
    return 25 if int(version.split(".")[0]) >= FIRST_JAVA_25_MAJOR else default


def version_key(version):
    return tuple(int(part) for part in version.split("."))


def server_version(declared, index, available):
    version = declared[index]
    if version in available:
        return version
    following = declared[index + 1] if index + 1 < len(declared) else None
    hotfix = re.compile(re.escape(version) + r"\.\d+$")
    candidates = [v for v in available
                  if hotfix.match(v) and (following is None or version_key(v) < version_key(following))]
    return max(candidates, key=version_key) if candidates else None


def build_matrix(repo_root, available):
    config = edition.load_edition(edition.current_id(repo_root), repo_root)
    default_java = config["smoke"][0]["java"]
    declared = smoke.declared_versions(Path(repo_root))
    include, missing = [], []
    for project in PROJECTS:
        for index, version in enumerate(declared):
            server = server_version(declared, index, available.get(project, ()))
            if server:
                include.append({"project": project, "version": server, "java": java_for(server, default_java)})
            else:
                missing.append(f"{project} {version}")
    return {"include": include}, missing


def untested(declared, missing):
    return [v for v in declared if all(f"{p} {v}" in missing for p in PROJECTS)]


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
    orphans = untested(smoke.declared_versions(REPO_ROOT), missing)
    if orphans:
        print("No server software publishes a build for: " + ", ".join(orphans), file=sys.stderr)
        return 1
    print(json.dumps(matrix, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
