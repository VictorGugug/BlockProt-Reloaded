import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import legacy_status  # noqa: E402


def run(cwd, *args):
    subprocess.run(["git", "-c", "user.name=t", "-c", "user.email=t@example.invalid", *args],
                   cwd=cwd, check=True, capture_output=True)


def commit(cwd, name, text):
    (Path(cwd) / name).write_text(text, encoding="utf-8")
    run(cwd, "add", name)
    run(cwd, "commit", "-m", f"add {name}")


class MissingCommits(unittest.TestCase):
    def test_cherry_picked_commits_are_not_reported(self):
        with tempfile.TemporaryDirectory() as tmp:
            run(tmp, "init", "-b", "main")
            commit(tmp, "base.txt", "base")
            run(tmp, "branch", "legacy")
            commit(tmp, "carried.txt", "carried")
            commit(tmp, "pending.txt", "pending")
            run(tmp, "checkout", "legacy")
            run(tmp, "cherry-pick", "main~1")
            found = legacy_status.missing_on("legacy", cwd=tmp)
        self.assertEqual(1, len(found))
        self.assertTrue(found[0].endswith("add pending.txt"))

    def test_up_to_date_branch_reports_nothing(self):
        with tempfile.TemporaryDirectory() as tmp:
            run(tmp, "init", "-b", "main")
            commit(tmp, "base.txt", "base")
            run(tmp, "branch", "legacy")
            self.assertEqual([], legacy_status.missing_on("legacy", cwd=tmp))


if __name__ == "__main__":
    unittest.main()
