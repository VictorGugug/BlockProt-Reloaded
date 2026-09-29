#!/usr/bin/env python3
import argparse
import subprocess
import sys

LEGACY_BRANCHES = ("BlockProt-Reloaded-Legacy", "BlockProt-Legacy")


def git(*args, cwd=None):
    result = subprocess.run(["git", *args], capture_output=True, text=True, cwd=cwd)
    if result.returncode != 0:
        raise SystemExit(f"git {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def missing_on(branch, main="main", cwd=None):
    lines = git("cherry", "-v", branch, main, cwd=cwd).splitlines()
    return [line[2:] for line in lines if line.startswith("+ ")]


def main():
    parser = argparse.ArgumentParser(description="List commits of main not yet carried to each legacy branch.")
    parser.add_argument("--main", default="main")
    parser.add_argument("branches", nargs="*", default=list(LEGACY_BRANCHES))
    args = parser.parse_args()
    pending = 0
    for branch in args.branches:
        commits = missing_on(branch, args.main)
        pending += len(commits)
        print(f"{branch}: {len(commits)} commit(s) of {args.main} not carried over")
        for line in commits:
            print(f"  {line}")
    return 0 if pending == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
