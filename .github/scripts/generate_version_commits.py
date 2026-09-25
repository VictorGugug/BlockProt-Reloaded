#!/usr/bin/env python3
"""
Generate Version Commits Report

Dynamically aggregates and categorizes all commits belonging to the active
plugin release cycle without hardcoded version numbers.

Version detection logic:
1. Resolves `blockProtVersion` from `gradle.properties`.
2. Locates the base commit introducing this version using git log regex on `gradle.properties`.
3. Resolves the previous release tag and version from the parent of that commit.
4. Collects and parses all commits in the version range (`base_commit~1..HEAD`).
5. Generates structured Markdown reports, GitHub Actions step summaries, and release notes bodies.
"""

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path


def run_command(args, cwd=None, allow_failure=False):
    """Execute a git or shell command safely and return standard output."""
    try:
        result = subprocess.run(
            args,
            cwd=cwd,
            capture_output=True,
            text=True,
            check=True,
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as err:
        if allow_failure:
            return ""
        sys.stderr.write(f"Command failed: {' '.join(args)}\nError: {err.stderr}\n")
        raise


def resolve_repository_url(cwd=None):
    """Resolve GitHub owner/repo path from environment or git remote."""
    env_repo = os.environ.get("GITHUB_REPOSITORY", "").strip()
    if env_repo:
        return env_repo

    remote_url = run_command(["git", "config", "--get", "remote.origin.url"], cwd=cwd, allow_failure=True)
    if remote_url:
        match = re.search(r"github\.com[:/]([^/]+)/([^/\.]+)(?:\.git)?", remote_url)
        if match:
            return f"{match.group(1)}/{match.group(2)}"

    return "VictorGugug/BlockProt-Reloaded"


def parse_gradle_properties(root_dir):
    """Extract blockProtVersion, supportedPlatforms, and MC range from gradle.properties."""
    props_path = Path(root_dir) / "gradle.properties"
    if not props_path.exists():
        raise FileNotFoundError(f"gradle.properties not found at: {props_path}")

    content = props_path.read_text(encoding="utf-8")
    match = re.search(r"^blockProtVersion\s*=\s*(.+)$", content, re.MULTILINE)
    if not match:
        raise ValueError("Could not find blockProtVersion in gradle.properties")
    version = match.group(1).strip()

    plat_match = re.search(r"^supportedPlatforms\s*=\s*(.+)$", content, re.MULTILINE)
    platforms = plat_match.group(1).strip() if plat_match else "Paper, Purpur, Folia"

    mc_match = re.search(r"^#\s*Supported on blockProtVersion.*:\s*(.+)$", content, re.MULTILINE)
    mc_range = ""
    if mc_match:
        mc_list = [x.strip() for x in mc_match.group(1).split(",") if x.strip()]
        if mc_list:
            mc_range = f"{mc_list[0]} - {mc_list[-1]}" if len(mc_list) > 1 else mc_list[0]

    return version, platforms, mc_range


def resolve_version_commit_range(version, cwd=None):
    """
    Determine the commit range for the active release cycle dynamically.
    Returns (base_commit, previous_tag, previous_version, range_spec).
    """
    regex = f"^blockProtVersion\\s*=\\s*{re.escape(version)}"
    base_commit = run_command(
        ["git", "log", "-n", "1", "-G", regex, "--format=%H", "gradle.properties"],
        cwd=cwd,
        allow_failure=True,
    )

    if not base_commit:
        # Fallback if version line commit cannot be found
        prev_tag = run_command(["git", "describe", "--tags", "--abbrev=0"], cwd=cwd, allow_failure=True)
        if prev_tag:
            return None, prev_tag, prev_tag, f"{prev_tag}..HEAD"
        return None, "", "", "HEAD"

    parent_commit = f"{base_commit}~1"

    # Resolve previous tag from parent commit if available
    prev_tag = run_command(
        ["git", "describe", "--tags", "--abbrev=0", parent_commit],
        cwd=cwd,
        allow_failure=True,
    )

    # Resolve previous version from parent gradle.properties if available
    prev_props = run_command(
        ["git", "show", f"{parent_commit}:gradle.properties"],
        cwd=cwd,
        allow_failure=True,
    )
    prev_version_match = re.search(r"^blockProtVersion\s*=\s*(.+)$", prev_props, re.MULTILINE)
    prev_version = prev_version_match.group(1).strip() if prev_version_match else prev_tag

    range_spec = f"{parent_commit}..HEAD"
    return base_commit, prev_tag, prev_version, range_spec


def collect_commits(range_spec, cwd=None):
    """Retrieve commits in range with full metadata."""
    format_spec = "%H%x00%h%x00%an%x00%ad%x00%s"
    raw_log = run_command(
        ["git", "log", range_spec, f"--pretty=format:{format_spec}", "--date=short"],
        cwd=cwd,
        allow_failure=True,
    )

    commits = []
    if not raw_log:
        return commits

    for line in raw_log.splitlines():
        if not line.strip():
            continue
        parts = line.split("\x00")
        if len(parts) >= 5:
            commits.append({
                "full_hash": parts[0],
                "short_hash": parts[1],
                "author": parts[2],
                "date": parts[3],
                "subject": parts[4].strip(),
            })

    return commits


def categorize_commit(subject):
    """Categorize commit message based on conventional commits or BPR convention."""
    lower = subject.lower()
    if re.search(r"bpr:[a-z0-9_\-\.]+\(add\):|bpr:[a-z0-9_\-\.]+\(feat\):|^feat(\(.*?\))?:|^add:", lower):
        return "Features"
    if re.search(r"bpr:[a-z0-9_\-\.]+\(fix\):|^fix(\(.*?\))?:", lower):
        return "Fixes"
    if re.search(r"bpr:[a-z0-9_\-\.]+\(refactor\):|^refactor(\(.*?\))?:", lower):
        return "Refactoring"
    if re.search(r"bpr:[a-z0-9_\-\.]+\(docs\):|^docs(\(.*?\))?:", lower):
        return "Documentation"
    if re.search(r"bpr:[a-z0-9_\-\.]+\(chore\):|^chore(\(.*?\))?:|^ci(\(.*?\))?:|^build(\(.*?\))?:", lower):
        return "Maintenance"
    return "General"


def resolve_release_commit(version, cwd=None):
    """Resolve commit hash for active version (tag if exists, else HEAD)."""
    commit = run_command(["git", "rev-parse", f"{version}^{{commit}}"], cwd=cwd, allow_failure=True)
    if not commit:
        commit = run_command(["git", "rev-parse", version], cwd=cwd, allow_failure=True)
    if not commit:
        commit = run_command(["git", "rev-parse", "HEAD"], cwd=cwd, allow_failure=True)
    return commit


def resolve_tag_commit(tag, cwd=None):
    """Resolve commit hash for a tag."""
    if not tag:
        return ""
    commit = run_command(["git", "rev-parse", f"{tag}^{{commit}}"], cwd=cwd, allow_failure=True)
    if not commit:
        commit = run_command(["git", "rev-parse", tag], cwd=cwd, allow_failure=True)
    return commit


def collect_previous_releases(current_version, repo_url, cwd=None):
    """Collect previous releases from git tags in reverse chronological order."""
    raw_tags = run_command(
        ["git", "for-each-ref", "--sort=-creatordate", "--format=%(refname:short)|%(creatordate:short)", "refs/tags"],
        cwd=cwd,
        allow_failure=True,
    )

    releases = []
    if not raw_tags:
        return releases

    seen_tags = set()
    for line in raw_tags.splitlines():
        if not line.strip():
            continue
        parts = line.split("|")
        tag = parts[0].strip()
        date = parts[1].strip() if len(parts) > 1 else ""

        norm_tag = tag.lstrip("v").lower()
        norm_curr = current_version.lstrip("v").lower()

        if norm_tag == norm_curr or tag in seen_tags:
            continue
        seen_tags.add(tag)

        commit = resolve_tag_commit(tag, cwd=cwd)
        short_commit = commit[:7] if commit else ""

        releases.append({
            "tag": tag,
            "date": date,
            "short_commit": short_commit,
            "commit": commit,
            "github_url": f"https://github.com/{repo_url}/releases/tag/{tag}",
            "modrinth_url": f"https://modrinth.com/plugin/blockprot-reloaded/version/{tag}",
            "curseforge_url": "https://www.curseforge.com/minecraft/bukkit-plugins/blockprot-reloaded1565977",
            "hangar_url": "https://hangar.papermc.io/VictorGugug/BlockProt-Reloaded/versions",
        })

    return releases


def is_version_released(version, cwd=None):
    """Check if a git tag already exists for the specified version."""
    existing = run_command(["git", "tag", "--list", version], cwd=cwd, allow_failure=True)
    if not existing:
        existing = run_command(["git", "tag", "--list", f"v{version}"], cwd=cwd, allow_failure=True)
    return bool(existing.strip())


def build_markdown_report(version, release_commit, prev_tag, prev_commit, prev_version, base_commit, commits, repo_url, previous_releases, platforms="", mc_range="", released=False):
    """Generate comprehensive Markdown report without bulky inlined commits."""
    compare_ref = prev_tag if prev_tag else (f"{base_commit[:7]}~1" if base_commit else "")
    compare_url = f"https://github.com/{repo_url}/compare/{compare_ref}...main" if compare_ref else ""
    status_label = "Released" if released else "Not yet released"
    plat_status = "Available" if released else "Not yet released"

    lines = []
    lines.append(f"# BlockProt Reloaded {version} Release Summary")
    lines.append("")
    lines.append("## Release Cycle Metadata")
    lines.append("")
    lines.append("| Property | Value |")
    lines.append("| :--- | :--- |")
    lines.append(f"| **Active Version** | `{version}` |")
    lines.append(f"| **Release Status** | `{status_label}` |")
    if release_commit:
        lines.append(f"| **Release Commit** | [`{release_commit[:7]}`](https://github.com/{repo_url}/commit/{release_commit}) |")
    if platforms:
        lines.append(f"| **Supported Platforms** | `{platforms}` |")
    if mc_range:
        lines.append(f"| **Supported Minecraft Range** | `{mc_range}` |")
    lines.append(f"| **Previous Version** | `{prev_version if prev_version else 'N/A'}` |")
    if prev_tag:
        lines.append(f"| **Previous Release Tag** | [`{prev_tag}`](https://github.com/{repo_url}/releases/tag/{prev_tag}) |")
    if prev_commit:
        lines.append(f"| **Previous Release Commit** | [`{prev_commit[:7]}`](https://github.com/{repo_url}/commit/{prev_commit}) |")
    if base_commit:
        lines.append(f"| **Cycle Start Commit** | [`{base_commit[:7]}`](https://github.com/{repo_url}/commit/{base_commit}) |")
    lines.append(f"| **Total Commits in Cycle** | `{len(commits)}` |")
    if compare_url:
        lines.append(f"| **Full Comparison Diff** | [Compare {compare_ref}...main on GitHub]({compare_url}) |")
    lines.append("")

    lines.append("## Distribution Platforms")
    lines.append("")
    lines.append("| Platform | Target / URL | Status |")
    lines.append("| :--- | :--- | :---: |")
    lines.append(f"| **GitHub Release** | [View on GitHub](https://github.com/{repo_url}/releases/tag/{version}) | {plat_status} |")
    lines.append(f"| **Modrinth** | [View on Modrinth](https://modrinth.com/plugin/blockprot-reloaded/version/{version}) | {plat_status} |")
    lines.append(f"| **CurseForge** | [View on CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/blockprot-reloaded1565977) | {plat_status} |")
    lines.append(f"| **Hangar** | [View on Hangar](https://hangar.papermc.io/VictorGugug/BlockProt-Reloaded/versions) | {plat_status} |")
    lines.append("")

    lines.append("## Commits & Differential Changes")
    lines.append("")
    if compare_url:
        lines.append("All commits and differential changes for this release cycle can be viewed directly on GitHub:")
        lines.append(f"-> [View Full Commit History and Diff on GitHub ({compare_ref}...main - {len(commits)} commits)]({compare_url})")
    else:
        lines.append(f"Total commits in cycle: {len(commits)}")
    lines.append("")

    if previous_releases:
        lines.append("## Previous Releases History")
        lines.append("")
        lines.append("| Version | Release Date | Release Commit | GitHub | Modrinth | CurseForge | Hangar |")
        lines.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: |")
        for r in previous_releases:
            c_link = f"[`{r['short_commit']}`](https://github.com/{repo_url}/commit/{r['commit']})" if r["commit"] else "N/A"
            gh_link = f"[GitHub]({r['github_url']})"
            mod_link = f"[Modrinth]({r['modrinth_url']})"
            curse_link = f"[CurseForge]({r['curseforge_url']})"
            hangar_link = f"[Hangar]({r['hangar_url']})"
            lines.append(f"| **{r['tag']}** | `{r['date']}` | {c_link} | {gh_link} | {mod_link} | {curse_link} | {hangar_link} |")
        lines.append("")

    return "\n".join(lines), compare_url


def build_release_body(version, release_commit, prev_tag, prev_commit, base_commit, commits, repo_url, previous_releases, platforms="", mc_range="", released=False):
    """Generate concise changelog body for GitHub Releases and platform publications."""
    compare_ref = prev_tag if prev_tag else (f"{base_commit[:7]}~1" if base_commit else "")
    compare_url = f"https://github.com/{repo_url}/compare/{compare_ref}...{version}" if compare_ref else ""
    status_label = "Released" if released else "Not yet released"
    status_suffix = "" if released else " *(Not yet released)*"

    lines = []
    lines.append(f"# BlockProt Reloaded {version}")
    lines.append("")
    lines.append("### Release Information")
    lines.append("")
    lines.append("| Property | Value |")
    lines.append("| :--- | :--- |")
    lines.append(f"| **Release Version** | `{version}` |")
    lines.append(f"| **Release Status** | `{status_label}` |")
    if release_commit:
        lines.append(f"| **Release Commit** | [`{release_commit[:7]}`](https://github.com/{repo_url}/commit/{release_commit}) |")
    if prev_tag:
        lines.append(f"| **Previous Release** | [`{prev_tag}`](https://github.com/{repo_url}/releases/tag/{prev_tag}) |")
    if prev_commit:
        lines.append(f"| **Previous Release Commit** | [`{prev_commit[:7]}`](https://github.com/{repo_url}/commit/{prev_commit}) |")
    if platforms:
        lines.append(f"| **Supported Platforms** | `{platforms}` |")
    if mc_range:
        lines.append(f"| **Supported Minecraft Range** | `{mc_range}` |")
    if compare_url:
        lines.append(f"| **Full Changelog & Commits** | [Compare with {compare_ref}]({compare_url}) |")
    lines.append("")

    lines.append("### Distribution Platforms")
    lines.append("")
    lines.append(f"- **GitHub Release:** [View on GitHub](https://github.com/{repo_url}/releases/tag/{version}){status_suffix}")
    lines.append(f"- **Modrinth:** [View on Modrinth](https://modrinth.com/plugin/blockprot-reloaded/version/{version}){status_suffix}")
    lines.append(f"- **CurseForge:** [View on CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/blockprot-reloaded1565977){status_suffix}")
    lines.append(f"- **Hangar:** [View on Hangar](https://hangar.papermc.io/VictorGugug/BlockProt-Reloaded/versions){status_suffix}")
    lines.append("")

    lines.append("### Changes")
    lines.append("")
    if compare_url:
        lines.append("All commits and differential changes for this release can be inspected directly on GitHub:")
        lines.append(f"-> [Compare changes on GitHub ({compare_ref}...{version} - {len(commits)} commits)]({compare_url})")
    else:
        lines.append(f"Total commits in this release: {len(commits)}")
    lines.append("")

    if previous_releases:
        lines.append("### Previous Releases History")
        lines.append("")
        lines.append("| Version | Release Date | Release Commit | GitHub | Modrinth | CurseForge | Hangar |")
        lines.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: |")
        for r in previous_releases:
            c_link = f"[`{r['short_commit']}`](https://github.com/{repo_url}/commit/{r['commit']})" if r["commit"] else "N/A"
            gh_link = f"[GitHub]({r['github_url']})"
            mod_link = f"[Modrinth]({r['modrinth_url']})"
            curse_link = f"[CurseForge]({r['curseforge_url']})"
            hangar_link = f"[Hangar]({r['hangar_url']})"
            lines.append(f"| **{r['tag']}** | `{r['date']}` | {c_link} | {gh_link} | {mod_link} | {curse_link} | {hangar_link} |")
        lines.append("")

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Generate version commits report dynamically.")
    parser.add_argument("--repo-dir", default=".", help="Root repository directory.")
    parser.add_argument("--version", default=None, help="Optional version override (defaults to gradle.properties).")
    parser.add_argument("--output-file", default="build/reports/version-commits.md", help="Markdown output file path.")
    parser.add_argument("--release-body", action="store_true", help="Generate concise release notes body only.")
    parser.add_argument("--no-summary", action="store_true", help="Do not write directly to GITHUB_STEP_SUMMARY.")
    args = parser.parse_args()

    repo_dir = os.path.abspath(args.repo_dir)
    prop_ver, platforms, mc_range = parse_gradle_properties(repo_dir)
    version = args.version.strip() if args.version else prop_ver
    repo_url = resolve_repository_url(cwd=repo_dir)

    base_commit, prev_tag, prev_version, range_spec = resolve_version_commit_range(version, cwd=repo_dir)
    commits = collect_commits(range_spec, cwd=repo_dir)
    release_commit = resolve_release_commit(version, cwd=repo_dir)
    prev_commit = resolve_tag_commit(prev_tag, cwd=repo_dir)
    previous_releases = collect_previous_releases(version, repo_url, cwd=repo_dir)
    released = is_version_released(version, cwd=repo_dir)

    if args.release_body:
        content = build_release_body(
            version, release_commit, prev_tag, prev_commit, base_commit, commits, repo_url, previous_releases, platforms, mc_range, released=released
        )
    else:
        content, compare_url = build_markdown_report(
            version, release_commit, prev_tag, prev_commit, prev_version, base_commit, commits, repo_url, previous_releases, platforms, mc_range, released=released
        )

    out_path = Path(args.output_file)
    if not out_path.is_absolute():
        out_path = Path(repo_dir) / out_path
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(content, encoding="utf-8")

    # If running in GitHub Actions, append to GITHUB_STEP_SUMMARY
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path and not args.release_body and not args.no_summary:
        with open(summary_path, "a", encoding="utf-8") as f:
            f.write(content + "\n")

    # If GITHUB_OUTPUT exists, emit outputs
    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        compare_ref = prev_tag if prev_tag else (f"{base_commit[:7]}~1" if base_commit else "")
        compare_url = f"https://github.com/{repo_url}/compare/{compare_ref}...main" if compare_ref else ""
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"version={version}\n")
            f.write(f"release_commit={release_commit}\n")
            f.write(f"released={'true' if released else 'false'}\n")
            f.write(f"release_status={'Released' if released else 'Not yet released'}\n")
            f.write(f"previous_tag={prev_tag}\n")
            f.write(f"previous_commit={prev_commit}\n")
            f.write(f"previous_version={prev_version}\n")
            f.write(f"commit_count={len(commits)}\n")
            f.write(f"compare_url={compare_url}\n")
            f.write(f"commits_file={out_path}\n")

    print(f"Generated version commit report for {version} ({len(commits)} commits) -> {out_path}")
    if args.release_body:
        print(content)


if __name__ == "__main__":
    main()
