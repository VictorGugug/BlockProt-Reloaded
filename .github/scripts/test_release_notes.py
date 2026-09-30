import re
import unittest
from pathlib import Path

NOTES = Path(__file__).resolve().parents[2] / "docs" / "RELEASE_NOTES"
RELATIVE_LINK = re.compile(r"\]\((?!https?://|#|mailto:)[^)]+\)")


class ReleaseNotes(unittest.TestCase):
    def test_links_are_absolute_because_the_notes_are_published_outside_the_repository(self):
        offenders = []
        for note in sorted(NOTES.glob("*.RELEASE_NOTES.md")):
            for number, line in enumerate(note.read_text(encoding="utf-8").splitlines(), 1):
                offenders += [f"{note.name}:{number} {m.group(0)}" for m in RELATIVE_LINK.finditer(line)]
        self.assertEqual([], offenders)


if __name__ == "__main__":
    unittest.main()
