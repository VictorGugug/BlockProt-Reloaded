import unittest
from pathlib import Path

import e2e_matrix
import server_smoke_test as smoke

ROOT = Path(__file__).resolve().parents[2]
DECLARED = smoke.declared_versions(ROOT)


class BuildMatrix(unittest.TestCase):
    def test_each_server_appears_only_for_versions_it_publishes(self):
        first = DECLARED[0]
        available = {"paper": set(DECLARED), "folia": set(), "purpur": {first}}
        matrix, missing = e2e_matrix.build_matrix(ROOT, available)
        pairs = {(e["project"], e["version"]) for e in matrix["include"]}
        self.assertEqual(len(pairs), len(matrix["include"]))
        self.assertEqual({("paper", v) for v in DECLARED} | {("purpur", first)}, pairs)
        self.assertIn(f"folia {first}", missing)

    def test_java_25_from_minecraft_26(self):
        self.assertEqual(25, e2e_matrix.java_for("26.2", 21))
        self.assertEqual(21, e2e_matrix.java_for("1.21.8", 21))
        self.assertEqual(17, e2e_matrix.java_for("1.20.4", 17))


class ServerVersion(unittest.TestCase):
    def test_exact_build_wins(self):
        self.assertEqual("26.1", e2e_matrix.server_version(["26.1", "26.2"], 0, {"26.1", "26.1.2"}))

    def test_newest_hotfix_stands_in_for_a_version_without_its_own_build(self):
        available = {"26.1.1", "26.1.2", "26.1.2-rc-1", "26.2"}
        self.assertEqual("26.1.2", e2e_matrix.server_version(["26.1", "26.2"], 0, available))

    def test_hotfix_never_reaches_the_next_declared_version(self):
        self.assertIsNone(e2e_matrix.server_version(["1.21", "1.21.1"], 0, {"1.21.1", "1.21.11"}))

    def test_versions_without_any_server_are_reported(self):
        missing = ["paper 26.1", "folia 26.1", "purpur 26.1", "folia 26.2"]
        self.assertEqual(["26.1"], e2e_matrix.untested(["26.1", "26.2"], missing))


if __name__ == "__main__":
    unittest.main()
