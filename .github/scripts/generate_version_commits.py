#!/usr/bin/env python3
import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import edition as editions  # noqa: E402

REPO_ROOT = Path(__file__).resolve().parents[2]


def git(*args, cwd=REPO_ROOT):
    result = subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else ""


def repository(cwd=REPO_ROOT):
    env_repo = os.environ.get("GITHUB_REPOSITORY", "").strip()
    if env_repo:
        return env_repo
    match = re.search(r"github\.com[:/]([^/]+)/([^/.]+)", git("config", "--get", "remote.origin.url", cwd=cwd))
    return f"{match.group(1)}/{match.group(2)}" if match else "VictorGugug/BlockProt-Reloaded"


def read_properties(repo_root=REPO_ROOT):
    props, supported = {}, []
    for line in (Path(repo_root) / "gradle.properties").read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line.startswith("# Supported on blockProtVersion"):
            supported = [v.strip() for v in line.split(":", 1)[1].split(",") if v.strip()]
        elif "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props, supported


def mc_range(supported):
    if not supported:
        return ""
    return supported[0] if len(supported) == 1 else f"{supported[0]} - {supported[-1]}"


def tag_filter(config, repo_root=REPO_ROOT):
    if config["tagPrefix"]:
        return ["--match", config["tagPrefix"] + "*"]
    prefixes = sorted({c["tagPrefix"] for c in editions.load_all(repo_root).values() if c["tagPrefix"]})
    return [arg for prefix in prefixes for arg in ("--exclude", prefix + "*")]


def release_cycle(version, config, repo_root=REPO_ROOT):
    base_version = version.split("-", 1)[0]
    pattern = f"^blockProtVersion\\s*=\\s*{re.escape(base_version)}\\s*$"
    base_commit = git("log", "-n", "1", "-G", pattern, "--format=%H", "--", "gradle.properties", cwd=repo_root)
    start = f"{base_commit}~1" if base_commit else "HEAD"
    previous_tag = git("describe", "--tags", "--abbrev=0", *tag_filter(config, repo_root), start, cwd=repo_root)
    if base_commit:
        commit_range = f"{base_commit}~1..HEAD"
    else:
        commit_range = f"{previous_tag}..HEAD" if previous_tag else "HEAD"
    count = git("rev-list", "--count", commit_range, cwd=repo_root)
    return {
        "base_commit": base_commit,
        "previous_tag": previous_tag,
        "commit_count": int(count) if count else 0,
    }


def build_markdown(version, config, repo, supported, platforms, cycle, head_commit, release_body):
    tag = config["tagPrefix"] + version
    compare_target = tag if release_body else config["branch"]
    previous = cycle["previous_tag"]
    lines = [f"# {config['displayName']} {version}", "", "| Property | Value |", "| :--- | :--- |",
             f"| Version | `{version}` |"]
    if not release_body:
        released = bool(git("tag", "--list", tag))
        lines.append(f"| Release status | {'Released' if released else 'Not yet released'} |")
    if head_commit:
        lines.append(f"| Commit | [`{head_commit[:7]}`](https://github.com/{repo}/commit/{head_commit}) |")
    if previous:
        lines.append(f"| Previous release | [`{previous}`](https://github.com/{repo}/releases/tag/{previous}) |")
    if platforms:
        lines.append(f"| Server software | {platforms} |")
    if supported:
        lines.append(f"| Minecraft | {mc_range(supported)} |")
    lines.append(f"| Commits in this cycle | {cycle['commit_count']} |")
    if previous:
        lines.append(f"| Changes | [Compare {previous}...{compare_target}](https://github.com/{repo}/compare/{previous}...{compare_target}) |")
    lines += ["", "## Downloads", ""]
    lines += [f"- [{name}]({url})" for name, url in editions.download_links(config, version, repo)]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description="Summarize the commits of the active release cycle of this edition.")
    parser.add_argument("--repo-dir", default=str(REPO_ROOT), help="Root repository directory.")
    parser.add_argument("--version", default=None, help="Version to describe (defaults to gradle.properties).")
    parser.add_argument("--output-file", default="build/reports/version-commits.md", help="Markdown output file path.")
    parser.add_argument("--release-body", action="store_true", help="Write the body of a GitHub release instead of the CI report.")
    parser.add_argument("--no-summary", action="store_true", help="Do not append the report to GITHUB_STEP_SUMMARY.")
    args = parser.parse_args()

    repo_root = Path(args.repo_dir).resolve()
    props, supported = read_properties(repo_root)
    suffix = props.get("versionSuffix", "")
    version = (args.version or "").strip() or (props["blockProtVersion"] + (f"-{suffix}" if suffix else ""))
    config = editions.load_edition(editions.current_id(repo_root), repo_root)
    cycle = release_cycle(version, config, repo_root)
    content = build_markdown(version, config, repository(repo_root), supported, props.get("supportedPlatforms", ""),
                             cycle, git("rev-parse", "HEAD", cwd=repo_root), args.release_body)

    out_path = Path(args.output_file)
    if not out_path.is_absolute():
        out_path = repo_root / out_path
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(content, encoding="utf-8")

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path and not args.release_body and not args.no_summary:
        with open(summary_path, "a", encoding="utf-8") as summary:
            summary.write(content + "\n")
    print(f"Wrote {out_path} for {version} ({cycle['commit_count']} commits)")


if __name__ == "__main__":
    main()
