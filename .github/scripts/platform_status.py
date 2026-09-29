#!/usr/bin/env python3
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

REPO = os.environ.get("GITHUB_REPOSITORY", "VictorGugug/BlockProt-Reloaded")
MODRINTH_ID = os.environ.get("MODRINTH_PROJECT_ID") or "blockprot-reloaded"
CURSEFORGE_ID = os.environ.get("CURSEFORGE_PROJECT_ID") or "1565977"
HANGAR_SLUG = "BlockProt-Reloaded"
PLATFORMS = ("github", "modrinth", "curseforge", "hangar")


def request(url, token=None):
    headers = {"User-Agent": "BlockProt-Reloaded-CI", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, None


def latest_stable_release(token):
    status, releases = request(f"https://api.github.com/repos/{REPO}/releases?per_page=30", token)
    if status != 200:
        raise SystemExit(f"GitHub releases request failed with HTTP {status}")
    stable = [r for r in releases if not r["prerelease"] and not r["draft"]]
    if not stable:
        raise SystemExit("No stable GitHub release found")
    return stable[0]


def check(version, token):
    github = request(f"https://api.github.com/repos/{REPO}/releases/tags/{version}", token)[0] == 200
    modrinth = request(f"https://api.modrinth.com/v2/project/{MODRINTH_ID}/version/{version}")[0] == 200
    _, cf = request(f"https://api.cfwidget.com/{CURSEFORGE_ID}")
    curseforge = bool(cf) and any(f.get("name") == f"BlockProtReloaded-{version}.jar" for f in cf.get("files", []))
    hangar = request(f"https://hangar.papermc.io/api/v1/projects/{HANGAR_SLUG}/versions/{version}")[0] == 200
    return {"github": github, "modrinth": modrinth, "curseforge": curseforge, "hangar": hangar}


def write_outputs(values):
    path = os.environ.get("GITHUB_OUTPUT")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as out:
        for key, value in values.items():
            out.write(f"{key}={value}\n")


def write_summary(title, version, status, note=""):
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not path:
        return
    names = {"github": "GitHub Releases", "modrinth": "Modrinth", "curseforge": "CurseForge", "hangar": "Hangar"}
    with open(path, "a", encoding="utf-8") as out:
        out.write(f"## {title}: `{version}`\n\n| Platform | Published |\n| :--- | :---: |\n")
        for platform in PLATFORMS:
            out.write(f"| {names[platform]} | {'[v]' if status[platform] else '[x]'} |\n")
        if note:
            out.write(f"\n> {note}\n")
        out.write("\n")


def main():
    parser = argparse.ArgumentParser(description="Check a BlockProt version on GitHub, Modrinth, CurseForge and Hangar.")
    parser.add_argument("--version", default="", help="Version to check; defaults to the latest stable GitHub release")
    parser.add_argument("--title", default="Platform publication status")
    parser.add_argument("--min-age-hours", type=float, default=0,
                        help="Minimum release age before a missing platform is reported as needing a publish")
    parser.add_argument("--curseforge-min-age-hours", type=float, default=72,
                        help="Minimum release age for CurseForge, whose new files wait for manual review")
    args = parser.parse_args()
    token = os.environ.get("GITHUB_TOKEN")

    version = args.version
    age_hours = None
    if not version:
        release = latest_stable_release(token)
        version = release["tag_name"]
        published = datetime.fromisoformat(release["published_at"].replace("Z", "+00:00"))
        age_hours = (datetime.now(timezone.utc) - published).total_seconds() / 3600

    status = check(version, token)
    missing = [p for p in PLATFORMS if not status[p]]

    def old_enough(platform):
        limit = args.curseforge_min_age_hours if platform == "curseforge" else args.min_age_hours
        return age_hours is None or age_hours >= limit

    targets = [p for p in missing if old_enough(p)]
    waiting = [p for p in missing if p not in targets]
    note = ""
    if waiting:
        note = (f"Release is {age_hours:.1f} h old. Not republished yet: {', '.join(waiting)} "
                "(CurseForge files wait for manual review before they are listed).")
    needed = bool(targets)
    print(json.dumps({"version": version, "status": status, "missing": missing, "targets": targets, "needed": needed}))
    write_outputs({"version": version, "missing": ",".join(missing), "targets": ",".join(targets),
                   "needed": str(needed).lower()})
    write_summary(args.title, version, status, note)


if __name__ == "__main__":
    main()
