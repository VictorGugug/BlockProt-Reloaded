#!/usr/bin/env python3
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import edition as editions  # noqa: E402

REPO = os.environ.get("GITHUB_REPOSITORY", "VictorGugug/BlockProt-Reloaded")
PLATFORMS = ("github", "modrinth", "curseforge", "hangar")
NAMES = {"github": "GitHub Releases", "modrinth": "Modrinth", "curseforge": "CurseForge", "hangar": "Hangar"}


def request(url, token=None):
    headers = {"User-Agent": "BlockProt-Reloaded-CI", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    try:
        with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=60) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, None


def latest_stable_release(token, config):
    status, releases = request(f"https://api.github.com/repos/{REPO}/releases?per_page=100", token)
    if status != 200:
        raise SystemExit(f"GitHub releases request failed with HTTP {status}")
    stable = [r for r in releases
              if not r["prerelease"] and not r["draft"] and editions.belongs_to(r["tag_name"], config)]
    if not stable:
        return None
    release = dict(stable[0])
    release["tag"] = release["tag_name"]
    release["version"] = release["tag_name"][len(config["tagPrefix"]):]
    return release


def enabled_platforms(config):
    return ["github"] + [p for p in PLATFORMS[1:] if config["platforms"][p]["enabled"]]


def check(version, token, config):
    platforms = config["platforms"]
    enabled = enabled_platforms(config)
    status = {p: None for p in PLATFORMS}
    tag = config["tagPrefix"] + version
    status["github"] = request(f"https://api.github.com/repos/{REPO}/releases/tags/{tag}", token)[0] == 200
    if "modrinth" in enabled:
        url = f"https://api.modrinth.com/v2/project/{platforms['modrinth']['id']}/version/{version}"
        status["modrinth"] = request(url)[0] == 200
    if "curseforge" in enabled:
        _, cf = request(f"https://api.cfwidget.com/{platforms['curseforge']['id']}")
        wanted = f"{config['jarBase']}-{version}.jar"
        status["curseforge"] = bool(cf) and any(f.get("name") == wanted for f in cf.get("files", []))
    if "hangar" in enabled:
        hangar = platforms["hangar"]
        url = f"https://hangar.papermc.io/api/v1/projects/{hangar['slug']}/versions/{version}"
        status["hangar"] = request(url)[0] == 200
    return status


def write_outputs(values):
    path = os.environ.get("GITHUB_OUTPUT")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as out:
        for key, value in values.items():
            out.write(f"{key}={value}\n")


def mark(value):
    return "[-]" if value is None else "[v]" if value else "[x]"


def write_summary(title, version, status, note=""):
    path = os.environ.get("GITHUB_STEP_SUMMARY")
    if not path:
        return
    with open(path, "a", encoding="utf-8") as out:
        out.write(f"## {title}: `{version}`\n\n| Platform | Published |\n| :--- | :---: |\n")
        for platform in PLATFORMS:
            out.write(f"| {NAMES[platform]} | {mark(status[platform])} |\n")
        if note:
            out.write(f"\n> {note}\n")
        out.write("\n")


def main():
    parser = argparse.ArgumentParser(description="Check a BlockProt version on GitHub, Modrinth, CurseForge and Hangar.")
    parser.add_argument("--edition", default="bpr", help="Edition id from .github/editions.json")
    parser.add_argument("--version", default="", help="Version to check without tag prefix; defaults to the latest stable release")
    parser.add_argument("--title", default="Platform publication status")
    parser.add_argument("--min-age-hours", type=float, default=0,
                        help="Minimum release age before a missing platform is reported as needing a publish")
    parser.add_argument("--curseforge-min-age-hours", type=float, default=72,
                        help="Minimum release age for CurseForge, whose new files wait for manual review")
    args = parser.parse_args()
    token = os.environ.get("GITHUB_TOKEN")
    config = editions.load_edition(args.edition)

    version = args.version
    age_hours = None
    if not version:
        release = latest_stable_release(token, config)
        if release is None:
            print(json.dumps({"edition": args.edition, "version": "", "tag": "", "status": {},
                              "missing": [], "targets": [], "needed": False}))
            write_outputs({"version": "", "tag": "", "missing": "", "targets": "", "needed": "false"})
            return
        version = release["version"]
        published = datetime.fromisoformat(release["published_at"].replace("Z", "+00:00"))
        age_hours = (datetime.now(timezone.utc) - published).total_seconds() / 3600

    status = check(version, token, config)
    missing = [p for p in PLATFORMS if status[p] is False]

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
    tag = config["tagPrefix"] + version
    print(json.dumps({"edition": args.edition, "version": version, "tag": tag, "status": status,
                      "missing": missing, "targets": targets, "needed": needed}))
    write_outputs({"version": version, "tag": tag, "missing": ",".join(missing),
                   "targets": ",".join(targets), "needed": str(needed).lower()})
    write_summary(args.title, version, status, note)


if __name__ == "__main__":
    main()
