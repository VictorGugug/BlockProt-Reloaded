import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import server_smoke_test as smoke  # noqa: E402

MARKER = "[08:20:05 ERROR]: --- DO NOT REPORT THIS TO PAPER - THIS IS NOT A BUG OR A CRASH  - git-Paper-388 (MC: 1.18.2) ---"


class PluginErrors(unittest.TestCase):
    def test_watchdog_thread_dumps_are_not_plugin_errors(self):
        text = "\n".join([
            "[08:20:04 INFO]: [BlockProt] Running console diagnostics",
            MARKER,
            "[08:20:05 ERROR]: The server has not responded for 10 seconds! Creating thread dump",
            "[08:20:05 ERROR]: \t\tBlockProtLegacy-1.3.9.1-BEDev.jar//de.sean.blockprot.bukkit.commands.DebugCommand.checkNbt(DebugCommand.java:737)",
            MARKER,
            "[08:20:06 INFO]: [BlockProt] Console diagnostics finished: 65 passed, 0 failed (65 total).",
        ])
        self.assertEqual([], smoke.plugin_error_lines(text))

    def test_real_plugin_errors_are_reported(self):
        text = "\n".join([
            "[08:20:04 ERROR]: [BlockProt Reloaded] Something broke",
            "java.lang.IllegalStateException: at de.sean.blockprot.bukkit.Foo",
        ])
        self.assertEqual(2, len(smoke.plugin_error_lines(text)))

    def test_errors_after_a_dump_are_still_reported(self):
        text = "\n".join([MARKER, "[x ERROR]: BlockProt frame", MARKER, "[x ERROR]: [BlockProt] real failure"])
        self.assertEqual(["[x ERROR]: [BlockProt] real failure"], smoke.plugin_error_lines(text))


if __name__ == "__main__":
    unittest.main()
