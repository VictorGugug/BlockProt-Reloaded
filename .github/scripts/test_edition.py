import json
import os
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import edition  # noqa: E402

REQUIRED_KEYS = {"displayName", "branch", "commitBranch", "tagPrefix", "jarBase", "makeLatest", "smoke", "platforms"}


class ConfigShape(unittest.TestCase):
    def test_every_edition_has_the_required_keys(self):
        for edition_id, config in edition.load_all().items():
            self.assertTrue(REQUIRED_KEYS <= set(config), edition_id)
            self.assertEqual({"modrinth", "curseforge", "hangar"}, set(config["platforms"]), edition_id)
            for leg in config["smoke"]:
                self.assertEqual({"project", "version", "java"}, set(leg), edition_id)

    def test_only_the_main_edition_publishes_and_makes_latest(self):
        editions = edition.load_all()
        self.assertTrue(editions["bpr"]["makeLatest"])
        for edition_id in ("bpr-legacy", "bp-legacy"):
            self.assertFalse(editions[edition_id]["makeLatest"])
            for platform in editions[edition_id]["platforms"].values():
                self.assertFalse(platform["enabled"])

    def test_tag_prefixes_are_unique_and_never_numeric(self):
        prefixes = [c["tagPrefix"] for c in edition.load_all().values() if c["tagPrefix"]]
        self.assertEqual(len(prefixes), len(set(prefixes)))
        for prefix in prefixes:
            self.assertFalse(prefix[0].isdigit())

    def test_unknown_edition_is_rejected(self):
        with self.assertRaises(SystemExit):
            edition.load_edition("nope")


class Resolve(unittest.TestCase):
    def test_push_on_main(self):
        out = edition.resolve("push", ref_name="main")
        self.assertEqual(("bpr", "ci", "main"), (out["edition"], out["operation"], out["branch"]))

    def test_push_on_legacy_branch_reads_the_marker(self):
        with tempfile.TemporaryDirectory() as tmp:
            github = Path(tmp) / ".github"
            github.mkdir()
            (github / "editions.json").write_text(json.dumps(edition.load_all()), encoding="utf-8")
            (github / "EDITION").write_text("bp-legacy\n", encoding="utf-8")
            out = edition.resolve("push", ref_name="legacy/bp", repo_root=tmp)
        self.assertEqual(("bp-legacy", "ci", "legacy-bp", "bpl-"),
                         (out["edition"], out["operation"], out["commit_branch"], out["tag_prefix"]))

    def test_unmapped_ref_fails(self):
        with self.assertRaises(SystemExit):
            edition.resolve("push", ref_name="feature/x")

    def test_dispatch_maps_every_operation_label(self):
        for label, operation in edition.OPERATIONS.items():
            out = edition.resolve("workflow_dispatch", input_edition="bpr-legacy", input_operation=label)
            self.assertEqual(("bpr-legacy", operation), (out["edition"], out["operation"]))

    def test_dispatch_rejects_unknown_operation(self):
        with self.assertRaises(SystemExit):
            edition.resolve("workflow_dispatch", input_edition="bpr", input_operation="Explode")

    def test_every_cron_line(self):
        expected = {
            "0 6 * * *": ("bpr", "sync"),
            "20 6 * * *": ("bpr-legacy", "sync"),
            "40 6 * * *": ("bp-legacy", "sync"),
            "0 7 * * 1": ("bpr", "maintenance"),
            "10 7 * * 1": ("bpr-legacy", "maintenance"),
            "20 7 * * 1": ("bp-legacy", "maintenance"),
        }
        for cron, pair in expected.items():
            out = edition.resolve("schedule", schedule=cron)
            self.assertEqual(pair, (out["edition"], out["operation"]), cron)

    def test_unknown_cron_fails(self):
        with self.assertRaises(SystemExit):
            edition.resolve("schedule", schedule="1 1 * * *")

    def test_outputs_are_workflow_safe(self):
        out = edition.resolve("workflow_dispatch", input_edition="bpr-legacy", input_operation="Run CI")
        self.assertEqual("false", out["make_latest"])
        self.assertEqual("false", out["modrinth_enabled"])
        self.assertEqual("", out["curseforge_id"])
        self.assertEqual(7, len(json.loads(out["smoke_matrix"])["include"]))
        for value in out.values():
            self.assertNotIn("\n", value)

    def test_main_ids_can_be_overridden_by_environment(self):
        os.environ["MODRINTH_PROJECT_ID"] = "custom-slug"
        try:
            self.assertEqual("custom-slug", edition.load_edition("bpr")["platforms"]["modrinth"]["id"])
            self.assertEqual("blockprot-reloaded-legacy",
                             edition.load_edition("bpr-legacy")["platforms"]["modrinth"]["id"])
        finally:
            del os.environ["MODRINTH_PROJECT_ID"]


if __name__ == "__main__":
    unittest.main()
