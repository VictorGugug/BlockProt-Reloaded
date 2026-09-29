#!/usr/bin/env python3
import argparse
import json
import os
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

SCHEDULES = {
    "0 6 * * *": ("bpr", "sync"),
    "20 6 * * *": ("bpr-legacy", "sync"),
    "40 6 * * *": ("bp-legacy", "sync"),
    "0 7 * * 1": ("bpr", "maintenance"),
    "10 7 * * 1": ("bpr-legacy", "maintenance"),
    "20 7 * * 1": ("bp-legacy", "maintenance"),
}

OPERATIONS = {
    "Preview release (dry run)": "preview",
    "Publish release": "publish",
    "Sync platforms (publish the latest stable release where missing)": "sync",
    "Maintenance report": "maintenance",
    "Run CI": "ci",
}


def load_all(repo_root=REPO_ROOT):
    return json.loads((Path(repo_root) / ".github" / "editions.json").read_text(encoding="utf-8"))


def load_edition(edition_id, repo_root=REPO_ROOT):
    editions = load_all(repo_root)
    if edition_id not in editions:
        raise SystemExit(f"Unknown edition '{edition_id}'. Known: {', '.join(editions)}")
    edition = json.loads(json.dumps(editions[edition_id]))
    edition["id"] = edition_id
    if edition_id == "bpr":
        modrinth = os.environ.get("MODRINTH_PROJECT_ID")
        curseforge = os.environ.get("CURSEFORGE_PROJECT_ID")
        if modrinth:
            edition["platforms"]["modrinth"]["id"] = modrinth
        if curseforge:
            edition["platforms"]["curseforge"]["id"] = curseforge
    return edition


def edition_from_ref(ref_name, repo_root):
    if ref_name == "main":
        return "bpr"
    if ref_name.startswith("legacy/"):
        marker = Path(repo_root) / ".github" / "EDITION"
        if not marker.is_file():
            raise SystemExit(f"Branch {ref_name} has no .github/EDITION file")
        return marker.read_text(encoding="utf-8").strip()
    raise SystemExit(f"Ref '{ref_name}' does not map to an edition")


def resolve(event, schedule="", input_edition="", input_operation="", ref_name="", repo_root=REPO_ROOT):
    if event in ("push", "pull_request"):
        edition_id, operation = edition_from_ref(ref_name, repo_root), "ci"
    elif event == "workflow_dispatch":
        if input_operation not in OPERATIONS:
            raise SystemExit(f"Unknown operation '{input_operation}'")
        edition_id, operation = input_edition or "bpr", OPERATIONS[input_operation]
    elif event == "schedule":
        if schedule not in SCHEDULES:
            raise SystemExit(f"Unknown schedule '{schedule}'")
        edition_id, operation = SCHEDULES[schedule]
    else:
        raise SystemExit(f"Unsupported event '{event}'")

    edition = load_edition(edition_id, repo_root)
    platforms = edition["platforms"]
    curseforge = platforms["curseforge"]
    hangar = platforms["hangar"]
    return {
        "edition": edition_id,
        "operation": operation,
        "branch": edition["branch"],
        "commit_branch": edition["commitBranch"],
        "tag_prefix": edition["tagPrefix"],
        "jar_base": edition["jarBase"],
        "title": edition["displayName"],
        "make_latest": str(edition["makeLatest"]).lower(),
        "smoke_matrix": json.dumps({"include": edition["smoke"]}, separators=(",", ":")),
        "modrinth_enabled": str(platforms["modrinth"]["enabled"]).lower(),
        "modrinth_id": platforms["modrinth"]["id"] or "",
        "curseforge_enabled": str(curseforge["enabled"]).lower(),
        "curseforge_id": curseforge["id"] or "",
        "curseforge_slug": curseforge.get("slug", ""),
        "hangar_enabled": str(hangar["enabled"]).lower(),
        "hangar_slug": hangar["slug"],
        "hangar_owner": hangar["owner"],
    }


def main():
    parser = argparse.ArgumentParser(description="Resolve the BlockProt edition and operation for a workflow run.")
    sub = parser.add_subparsers(dest="command", required=True)
    resolver = sub.add_parser("resolve")
    resolver.add_argument("--event", required=True)
    resolver.add_argument("--schedule", default="")
    resolver.add_argument("--input-edition", default="")
    resolver.add_argument("--input-operation", default="")
    resolver.add_argument("--ref-name", default="")
    resolver.add_argument("--repo-root", default=str(REPO_ROOT))
    args = parser.parse_args()
    outputs = resolve(args.event, args.schedule, args.input_edition, args.input_operation,
                      args.ref_name, args.repo_root)
    for key, value in outputs.items():
        print(f"{key}={value}")
    path = os.environ.get("GITHUB_OUTPUT")
    if path:
        with open(path, "a", encoding="utf-8") as out:
            for key, value in outputs.items():
                out.write(f"{key}={value}\n")


if __name__ == "__main__":
    sys.exit(main())
