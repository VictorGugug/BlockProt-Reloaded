#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import platform
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import edition as editions  # noqa: E402
import generate_version_commits as cycles  # noqa: E402

REPO_ROOT = Path(__file__).resolve().parents[2]


def java_version():
    try:
        result = subprocess.run(["java", "-version"], capture_output=True, text=True)
    except OSError:
        return "not found"
    lines = (result.stderr or result.stdout).splitlines()
    return lines[0].strip() if lines else "unknown"


def gradle_version(repo_root):
    wrapper = repo_root / "gradle" / "wrapper" / "gradle-wrapper.properties"
    match = re.search(r"gradle-([\d.]+)-", wrapper.read_text(encoding="utf-8")) if wrapper.is_file() else None
    return match.group(1) if match else "unknown"


def test_totals(repo_root):
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    for report in repo_root.glob("*/build/test-results/test/*.xml"):
        suite = ET.parse(report).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, 0))
    return totals


def plugin_jar(repo_root, jar_base):
    for jar in sorted((repo_root / "spigot" / "build" / "libs").glob(f"{jar_base}-*.jar")):
        if "-sources" not in jar.name and "-javadoc" not in jar.name:
            return jar
    return None


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def audit_sections(repo_root):
    path = repo_root / "build" / "reports" / "dependency-audit.json"
    if not path.is_file():
        return []
    data = json.loads(path.read_text(encoding="utf-8"))
    md = ["## Upstream server builds", "",
          "| Software | Support | Declared latest | Latest stable build | Latest experimental build | Upstream |",
          "| :--- | :--- | :--- | :--- | :--- | :--- |"]
    for p in data.get("platform_results", []):
        md.append(f"| {p['platform']} | {p['tier']} | `{p['declared_mc']}` | `{p['stable_build']}` "
                  f"| `{p['experimental_build']}` | {p['upstream_status']} |")
    md += ["", f"## Dependencies ({data.get('up_to_date_count', 0)} of {data.get('total_count', 0)} up to date)", "",
           "| Status | Dependency | Category | Declared | Latest |", "| :---: | :--- | :--- | :--- | :--- |"]
    for d in data.get("dependency_results", []):
        md.append(f"| `{d['status']}` | {d['name']} | {d['category']} | `{d['current']}` | `{d['latest']}` |")
    return md + [""]


def summary_markdown(repo_root):
    props, supported = cycles.read_properties(repo_root)
    suffix = props.get("versionSuffix", "")
    version = props.get("blockProtVersion", "unknown") + (f"-{suffix}" if suffix else "")
    config = editions.load_edition(editions.current_id(repo_root), repo_root)
    repo = cycles.repository(repo_root)
    commit = cycles.git("rev-parse", "HEAD", cwd=repo_root)
    released = bool(cycles.git("tag", "--list", config["tagPrefix"] + version, cwd=repo_root))
    cycle = cycles.release_cycle(version, config, repo_root)
    tests = test_totals(repo_root)
    jar = plugin_jar(repo_root, config["jarBase"])
    runner = os.environ.get("ImageOS") or os.environ.get("RUNNER_OS") or f"{platform.system()} {platform.release()}"

    md = [f"# {config['displayName']} CI Summary", "",
          "| Property | Value |", "| :--- | :--- |",
          f"| Version | `{version}` ({'released' if released else 'not yet released'}) |",
          f"| Commit | [`{commit[:7]}`](https://github.com/{repo}/commit/{commit}) |",
          f"| Branch | `{os.environ.get('GITHUB_REF_NAME') or cycles.git('branch', '--show-current', cwd=repo_root)}` |",
          f"| Event | `{os.environ.get('GITHUB_EVENT_NAME', 'local')}` |",
          f"| Runner | {runner} |",
          f"| Java | {java_version()} |",
          f"| Gradle wrapper | {gradle_version(repo_root)} |",
          f"| Unit tests | {tests['tests']} run, {tests['failures']} failed, {tests['errors']} errors, {tests['skipped']} skipped |"]
    if jar:
        md.append(f"| Plugin JAR | `{jar.name}` ({jar.stat().st_size / (1024 * 1024):.2f} MB) |")
        md.append(f"| SHA-256 | `{sha256(jar)}` |")
    else:
        md.append(f"| Plugin JAR | not found under `spigot/build/libs/{config['jarBase']}-*.jar` |")
    if cycle["previous_tag"]:
        md.append(f"| Since `{cycle['previous_tag']}` | {cycle['commit_count']} commits, "
                  f"[compare](https://github.com/{repo}/compare/{cycle['previous_tag']}...{commit}) |")

    md += ["", "## Declared support (gradle.properties)", "",
           f"- Minecraft: {', '.join(supported) or 'none declared'}",
           f"- Server software: {props.get('supportedPlatforms', 'none declared')}"]
    if props.get("legacyPlatforms"):
        md.append(f"- Legacy, not tested: {props['legacyPlatforms']}")
    if props.get("unsupportedPlatforms"):
        md.append(f"- Not supported: {props['unsupportedPlatforms']}")
    if props.get("unsupportedVersions"):
        md.append(f"- Other Minecraft versions: {props['unsupportedVersions']}")

    md += ["", "## Downloads for this version", ""]
    md += [f"- [{name}]({url})" for name, url in editions.download_links(config, version, repo)]
    if not released:
        md.append("- Not published yet: these links work after the release is published.")
    md.append("")
    return "\n".join(md + audit_sections(repo_root))


def main():
    parser = argparse.ArgumentParser(description="Write the CI summary of this build.")
    parser.add_argument("--repo-dir", default=str(REPO_ROOT), help="Root repository directory.")
    parser.add_argument("--output-file", default="build/reports/ci-summary.md", help="Markdown output file path.")
    args = parser.parse_args()

    repo_root = Path(args.repo_dir).resolve()
    content = summary_markdown(repo_root)
    out_path = Path(args.output_file)
    if not out_path.is_absolute():
        out_path = repo_root / out_path
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(content, encoding="utf-8")

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as summary:
            summary.write(content)
    print(f"Wrote {out_path}")


if __name__ == "__main__":
    main()
