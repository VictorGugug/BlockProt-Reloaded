import unittest
from pathlib import Path

import e2e_matrix

ROOT = Path(__file__).resolve().parents[2]


class BuildMatrix(unittest.TestCase):
    def test_paper_covers_every_declared_version_and_other_servers_only_where_they_exist(self):
        matrix = e2e_matrix.build_matrix(ROOT, {"folia": {"1.21.8"}, "purpur": {"1.21.8", "1.21.9"}})["include"]
        by_project = {}
        for entry in matrix:
            by_project.setdefault(entry["project"], []).append(entry["version"])
        self.assertTrue(by_project["paper"])
        self.assertEqual(sorted(set(by_project["paper"])), sorted(by_project["paper"]))
        self.assertTrue(set(by_project.get("folia", [])) <= {"1.21.8"})
        self.assertTrue(set(by_project.get("purpur", [])) <= {"1.21.8", "1.21.9"})
        self.assertEqual(1, len({entry["java"] for entry in matrix}))


if __name__ == "__main__":
    unittest.main()
