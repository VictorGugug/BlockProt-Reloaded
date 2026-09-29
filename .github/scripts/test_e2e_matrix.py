import unittest
from pathlib import Path

import e2e_matrix

ROOT = Path(__file__).resolve().parents[2]


class BuildMatrix(unittest.TestCase):
    def test_paper_covers_every_declared_version_and_folia_only_where_it_exists(self):
        matrix = e2e_matrix.build_matrix(ROOT, {"1.20.4"})["include"]
        paper = [entry["version"] for entry in matrix if entry["project"] == "paper"]
        folia = [entry["version"] for entry in matrix if entry["project"] == "folia"]
        self.assertEqual(sorted(set(paper)), sorted(paper))
        self.assertTrue(paper)
        self.assertTrue(set(folia) <= {"1.20.4"})
        self.assertEqual(1, len({entry["java"] for entry in matrix}))


if __name__ == "__main__":
    unittest.main()
