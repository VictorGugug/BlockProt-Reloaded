#!/usr/bin/env python3
import argparse
import json
import re
import shutil
import subprocess
import sys
import time
import urllib.request
from pathlib import Path

FILL_API = "https://fill.papermc.io/v3/projects"
SUMMARY_RE = re.compile(r"Console diagnostics finished: (\d+) passed, (\d+) failed \((\d+) total\)")
ANSI_RE = re.compile(r"\x1b\[[0-9;]*m")


def fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": "BlockProt-Reloaded-CI"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def declared_versions(repo_root):
    for line in (repo_root / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if line.strip().startswith("# Supported on blockProtVersion"):
            return [v.strip() for v in line.split(":", 1)[1].split(",") if v.strip()]
    return []


def resolve_build(project, requested, repo_root):
    available = fetch_json(f"{FILL_API}/{project}")["versions"]
    flat = [v for group in available.values() for v in group]
    candidates = [requested] if requested else list(reversed(declared_versions(repo_root)))
    for version in candidates:
        if version in flat:
            builds = fetch_json(f"{FILL_API}/{project}/versions/{version}/builds")
            if builds:
                return version, builds[0]["id"], builds[0]["downloads"]["server:default"]["url"]
    raise SystemExit(f"No {project} build found for {requested or 'any declared version'}")


def prepare_server(workdir, jar, server_url, port):
    workdir.mkdir(parents=True, exist_ok=True)
    plugins = workdir / "plugins"
    if plugins.exists():
        shutil.rmtree(plugins)
    plugins.mkdir()
    shutil.copy(jar, plugins / jar.name)
    server_jar = workdir / "server.jar"
    request = urllib.request.Request(server_url, headers={"User-Agent": "BlockProt-Reloaded-CI"})
    with urllib.request.urlopen(request, timeout=300) as response, open(server_jar, "wb") as out:
        shutil.copyfileobj(response, out)
    (workdir / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (workdir / "server.properties").write_text(
        f"server-port={port}\nonline-mode=false\nlevel-type=minecraft\\:flat\n"
        "generate-structures=false\nspawn-protection=0\n", encoding="utf-8")
    return server_jar


def read_log(path):
    return ANSI_RE.sub("", path.read_text(encoding="utf-8", errors="replace"))


def wait_for(path, predicate, timeout):
    deadline = time.time() + timeout
    while time.time() < deadline:
        time.sleep(2)
        text = read_log(path)
        if predicate(text):
            return text
    return read_log(path)


def run(args):
    repo_root = Path(__file__).resolve().parents[2]
    jar = Path(args.jar).resolve()
    workdir = Path(args.workdir).resolve()
    version, build, url = resolve_build(args.project, args.version, repo_root)
    print(f"{args.project} {version} build {build}")
    server_jar = prepare_server(workdir, jar, url, args.port)
    log_path = workdir / "console.log"
    with open(log_path, "w", encoding="utf-8") as log:
        process = subprocess.Popen([args.java, "-Xmx2G", "-jar", server_jar.name, "--nogui"], cwd=workdir,
                                   stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True)
        try:
            text = wait_for(log_path, lambda t: "Done (" in t or process.poll() is not None, args.boot_timeout)
            if "Done (" not in text:
                print(text[-4000:])
                return 1, version, None
            process.stdin.write("bp debug\n")
            process.stdin.flush()
            text = wait_for(log_path, lambda t: SUMMARY_RE.search(t) is not None, args.debug_timeout)
            process.stdin.write("stop\n")
            process.stdin.flush()
            process.wait(timeout=120)
        finally:
            if process.poll() is None:
                process.kill()
    text = read_log(log_path)
    match = SUMMARY_RE.search(text)
    plugin_errors = [line for line in text.splitlines()
                     if ("ERROR" in line or "Exception" in line) and ("BlockProt" in line or "de.sean.blockprot" in line)]
    for line in plugin_errors:
        print(f"plugin error: {line}")
    if match is None:
        print("Diagnostics summary not found")
        return 1, version, None
    passed, failed, total = (int(g) for g in match.groups())
    print(f"{args.project} {version}: {passed} passed, {failed} failed ({total} total)")
    return (0 if failed == 0 and not plugin_errors else 1), version, (passed, failed, total)


def main():
    parser = argparse.ArgumentParser(description="Boot a real Paper or Folia server with the plugin and run console diagnostics.")
    parser.add_argument("--jar", required=True)
    parser.add_argument("--project", choices=["paper", "folia"], default="paper")
    parser.add_argument("--version", default="", help="Minecraft version; defaults to the newest declared version available")
    parser.add_argument("--workdir", default="build/smoke-server")
    parser.add_argument("--port", type=int, default=25599)
    parser.add_argument("--boot-timeout", type=int, default=420)
    parser.add_argument("--debug-timeout", type=int, default=180)
    parser.add_argument("--summary", default="", help="Markdown file to append the result to")
    parser.add_argument("--java", default="java", help="Java executable used to run the server")
    parser.add_argument("--summary-title", default="", help="Label used in the summary row instead of the project name")
    args = parser.parse_args()
    code, version, result = run(args)
    if args.summary:
        status = "PASS" if code == 0 else "FAIL"
        detail = f"{result[0]} passed, {result[1]} failed ({result[2]} total)" if result else "no diagnostics summary"
        with open(args.summary, "a", encoding="utf-8") as out:
            label = args.summary_title or args.project.capitalize()
            out.write(f"| {label} {version} | {status} | {detail} |\n")
    sys.exit(code)


if __name__ == "__main__":
    main()
