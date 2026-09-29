import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import edition  # noqa: E402
import render_descriptions as render  # noqa: E402


class Render(unittest.TestCase):
    def rendered(self, edition_id):
        return render.render(render.read(render.TEMPLATE), edition.load_edition(edition_id))

    def test_bpr_legacy_carries_its_own_values(self):
        text = self.rendered("bpr-legacy")
        self.assertIn("| Minecraft | 1.20.5 - 1.21.6 |", text)
        self.assertIn("| Java | Java 21+ required |", text)
        self.assertIn("BlockProt Reloaded Legacy is a maintained fork", text)
        self.assertNotIn("1.21.7 through 26.3", text)
        self.assertNotIn("use_dialogs: true", text)

    def test_bp_legacy_drops_folia_and_uses_java_17(self):
        text = self.rendered("bp-legacy")
        self.assertIn("| Server software | Paper, Purpur |", text)
        self.assertIn("Folia: not declared for this edition.", text)
        self.assertIn("Requires Java 17+ and Paper or Purpur 1.18.2 - 1.20.4.", text)

    def test_every_generated_file_is_up_to_date(self):
        for path, content in render.outputs():
            self.assertTrue(path.is_file(), path)
            self.assertEqual(content, render.read(path), path.name)

    def test_unexpected_template_is_rejected(self):
        with self.assertRaises(SystemExit):
            render.render("nothing to replace", edition.load_edition("bpr-legacy"))


if __name__ == "__main__":
    unittest.main()
