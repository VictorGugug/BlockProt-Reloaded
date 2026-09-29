import sys
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parent))
import edition  # noqa: E402
import platform_status  # noqa: E402


def release(tag, prerelease=False, draft=False):
    return {"tag_name": tag, "prerelease": prerelease, "draft": draft, "published_at": "2026-09-01T00:00:00Z"}


RELEASES = [
    release("bprl-1.3.9.2-BEDev.1", prerelease=True),
    release("bprl-1.3.9.1"),
    release("bpl-1.3.9.1"),
    release("1.3.9-BEDev.1", prerelease=True),
    release("1.3.8"),
]


class LatestStable(unittest.TestCase):
    def latest(self, edition_id):
        with mock.patch.object(platform_status, "request", return_value=(200, RELEASES)):
            return platform_status.latest_stable_release(None, edition.load_edition(edition_id))

    def test_main_ignores_prefixed_tags(self):
        found = self.latest("bpr")
        self.assertEqual(("1.3.8", "1.3.8"), (found["version"], found["tag"]))

    def test_legacy_strips_the_prefix_but_keeps_the_tag(self):
        found = self.latest("bpr-legacy")
        self.assertEqual(("1.3.9.1", "bprl-1.3.9.1"), (found["version"], found["tag"]))
        found = self.latest("bp-legacy")
        self.assertEqual(("1.3.9.1", "bpl-1.3.9.1"), (found["version"], found["tag"]))

    def test_drafts_and_prereleases_are_skipped(self):
        only_pre = [release("bprl-1.3.9.2", prerelease=True), release("bprl-1.3.9.3", draft=True)]
        with mock.patch.object(platform_status, "request", return_value=(200, only_pre)):
            self.assertIsNone(platform_status.latest_stable_release(None, edition.load_edition("bpr-legacy")))


class Check(unittest.TestCase):
    def test_disabled_platforms_are_not_queried_and_are_reported_as_none(self):
        calls = []

        def fake(url, token=None):
            calls.append(url)
            return 200, {}

        with mock.patch.object(platform_status, "request", side_effect=fake):
            status = platform_status.check("1.3.9.1", None, edition.load_edition("bpr-legacy"))
        self.assertEqual({"github": True, "modrinth": True, "curseforge": None, "hangar": None}, status)
        self.assertEqual(2, len(calls))
        self.assertTrue(calls[0].endswith("/releases/tags/bprl-1.3.9.1"))

    def test_main_checks_every_platform_with_its_own_ids(self):
        urls = []

        def fake(url, token=None):
            urls.append(url)
            if "cfwidget" in url:
                return 200, {"files": [{"name": "BlockProtReloaded-1.3.8.jar"}]}
            return 200, {}

        with mock.patch.object(platform_status, "request", side_effect=fake):
            status = platform_status.check("1.3.8", None, edition.load_edition("bpr"))
        self.assertEqual({"github": True, "modrinth": True, "curseforge": True, "hangar": True}, status)
        self.assertTrue(any("/projects/BlockProt-Reloaded/versions/1.3.8" in u for u in urls))

    def test_enabled_platforms_missing_the_version_are_false(self):
        config = edition.load_edition("bpr-legacy")
        config["platforms"]["hangar"]["enabled"] = True
        with mock.patch.object(platform_status, "request", return_value=(404, None)):
            status = platform_status.check("1.3.9.1", None, config)
        self.assertEqual({"github": False, "modrinth": False, "curseforge": None, "hangar": False}, status)

    def test_mark_renders_disabled_as_dash(self):
        self.assertEqual(("[-]", "[v]", "[x]"),
                         (platform_status.mark(None), platform_status.mark(True), platform_status.mark(False)))


if __name__ == "__main__":
    unittest.main()
