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


if __name__ == "__main__":
    unittest.main()
