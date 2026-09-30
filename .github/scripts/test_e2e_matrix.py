import unittest
from pathlib import Path

import e2e_matrix

ROOT = Path(__file__).resolve().parents[2]
AVAILABLE = {"paper": {"1.21.8", "26.2"}, "folia": {"1.21.8"}, "purpur": {"1.21.8", "26.2"}}


class BuildMatrix(unittest.TestCase):
    def test_each_server_appears_only_for_versions_it_publishes(self):
        matrix, missing = e2e_matrix.build_matrix(ROOT, AVAILABLE)
        pairs = {(e["project"], e["version"]) for e in matrix["include"]}
        self.assertEqual(len(pairs), len(matrix["include"]))
        self.assertTrue(pairs <= {(p, v) for p, vs in AVAILABLE.items() for v in vs})
        self.assertIn("folia 26.2", missing)

    def test_java_25_from_minecraft_26(self):
        self.assertEqual(25, e2e_matrix.java_for("26.2", 21))
        self.assertEqual(21, e2e_matrix.java_for("1.21.8", 21))
        self.assertEqual(17, e2e_matrix.java_for("1.20.4", 17))


if __name__ == "__main__":
    unittest.main()
