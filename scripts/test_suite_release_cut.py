"""Core cut tests against an in-memory GitHub; no network."""

import unittest
from pathlib import Path

import suite_release_cut as cut
import suite_release_plan as release
import suite_test_support as support


class CutTests(unittest.TestCase):
    def setUp(self):
        self.fake = support.FakeGitHub(line="0.51.02")

    def branches(self, line="0.51.02", dry_run=False):
        return cut.branches(line, dry_run, getter=self.fake.get, fetch=self.fake.fetch,
                            writes=self.fake)

    def bump(self, line="0.51.02", dry_run=False):
        return cut.bump(line, dry_run, getter=self.fake.get, fetch=self.fake.fetch,
                        writes=self.fake)

    def test_dry_run_creates_nothing(self):
        report = self.branches(dry_run=True)
        self.assertEqual(len(report), 4)
        self.assertTrue(all("would create releases/0.51.02" in line for line in report))
        self.assertEqual(self.fake.writes, [])

    def test_branches_are_created_from_main_and_reruns_keep_them(self):
        self.branches()
        for repo in release.SOURCES:
            self.assertEqual(self.fake.heads[(repo, "releases/0.51.02")], support.SHA[repo])
        del self.fake.heads[("mm-data", "releases/0.51.02")]
        self.fake.writes.clear()
        report = self.branches()
        self.assertEqual(self.fake.writes, [("branch", "mm-data", "releases/0.51.02")])
        self.assertEqual(sum("kept" in line for line in report), 3)

    def test_line_must_be_mains_line_and_newest(self):
        for line in ("0.51.03", "0.51.01", "0.51.2"):
            with self.subTest(line=line), self.assertRaises(release.UnsafeInventory):
                self.branches(line)
        self.fake.set_branch("releases/0.51.05", support.SHA)
        with self.assertRaisesRegex(release.UnsafeInventory, "newer"):
            self.branches()
        self.assertEqual(self.fake.writes, [])

    def test_partial_cut_rerun_refuses_branches_behind_main(self):
        self.branches()
        del self.fake.heads[("mm-data", "releases/0.51.02")]
        self.fake.heads[("megameklab", "main")] = "f" * 40
        self.fake.writes.clear()
        with self.assertRaisesRegex(release.UnsafeInventory, "megameklab.*no longer main"):
            self.branches()
        self.assertEqual(self.fake.writes, [])

    def test_rerun_after_main_moved_needs_a_core_release_of_the_line(self):
        self.branches()
        self.fake.add_suite("experimental", "0.51.02.5", support.SHA)
        self.fake.add_suite("core", "0.51.02.6", support.SHA)
        self.bump()
        self.fake.writes.clear()
        self.assertEqual(sum("kept" in line for line in self.branches()), 4)
        self.assertEqual(self.fake.writes, [])

    def test_stale_cut_is_refused_when_main_moved_without_core(self):
        self.branches()
        self.fake.add_suite("experimental", "0.51.02.5", support.SHA)
        moved = {repo: support.sha(f"{repo}-0.51.03") for repo in release.SOURCES}
        self.fake.set_branch("main", moved, "0.51.03")
        self.fake.writes.clear()
        with self.assertRaisesRegex(release.UnsafeInventory, "main is on line 0.51.03"):
            self.branches()
        self.assertEqual(self.fake.writes, [])

    def test_bump_waits_for_a_core_release_of_the_line(self):
        self.assertIn("after Core 0.51.02 publishes", self.bump(dry_run=True)[0])
        with self.assertRaisesRegex(release.UnsafeInventory, "no Core release"):
            self.bump()
        self.fake.add_suite("experimental", "0.51.02.5", support.SHA)
        with self.assertRaisesRegex(release.UnsafeInventory, "no Core release"):
            self.bump()
        self.fake.add_suite("core", "0.51.02.6", support.SHA)
        self.assertEqual(self.bump(dry_run=True), ["would move main from 0.51.02 to 0.51.03"])
        self.assertEqual(self.fake.writes, [])
        self.assertIn("moved main from 0.51.02 to 0.51.03", self.bump()[0])
        self.assertEqual(self.fake.writes, [("version_file", "Start 0.51.03 after Core 0.51.02.6")])
        main = self.fake.heads[("megamek", "main")]
        self.assertIn("patch=03\n", self.fake.props[main])
        self.assertIn("# MegaMek version\n", self.fake.props[main])
        # A rerun after success changes nothing.
        self.assertEqual(self.bump(), ["main is already on line 0.51.03; nothing to do"])
        self.assertEqual(len(self.fake.writes), 1)

    def test_bumped_text(self):
        self.assertEqual(cut.bumped("major=0\nminor=51\npatch=09\n"), "major=0\nminor=51\npatch=10\n")
        self.assertEqual(cut.bumped("patch = 1\r\nx=1"), "patch = 02\r\nx=1")
        for text in ("major=0\n", "patch=1\npatch=2\n"):
            with self.assertRaises(release.UnsafeInventory):
                cut.bumped(text)

    def stamp(self, branch="main", dry_run=False):
        return cut.stamp(branch, dry_run, getter=self.fake.get, fetch=self.fake.fetch,
                         writes=self.fake)

    def main_text(self):
        return self.fake.props[self.fake.heads[("megamek", "main")]]

    def test_stamp_raises_main_to_the_newest_build_of_its_line(self):
        self.assertIn("no release yet", self.stamp()[0])
        self.fake.add_suite("core", "0.51.01.20", support.SHA)
        self.assertIn("no release yet", self.stamp()[0])
        self.fake.add_suite("experimental", "0.51.02.5", support.SHA)
        self.assertEqual(self.stamp(dry_run=True),
                         ["would set main revision 0 -> 5 to match 0.51.02.5"])
        self.assertEqual(self.fake.writes, [])
        self.assertIn("set main revision 0 -> 5 to match 0.51.02.5", self.stamp()[0])
        self.assertEqual(self.fake.writes,
                         [("version_file", "Set version to 0.51.02.5 after its release")])
        self.assertIn("patch=02\nrevision=5\n", self.main_text())
        # Reruns change nothing, and a revision already ahead is never lowered.
        self.assertIn("already carries revision 5", self.stamp()[0])
        self.fake.version_file(self.main_text().replace("revision=5", "revision=9"), None, "manual")
        self.fake.writes.clear()
        self.assertIn("already carries revision 9", self.stamp()[0])
        self.assertEqual(self.fake.writes, [])

    def test_stamp_core_branch_leaves_main_alone(self):
        self.branches()
        self.fake.add_suite("experimental", "0.51.02.5", support.SHA)
        self.fake.add_suite("core", "0.51.02.6", support.SHA)
        self.fake.writes.clear()
        self.assertIn("revision 0 -> 6", self.stamp("releases/0.51.02")[0])
        branch = self.fake.heads[("megamek", "releases/0.51.02")]
        self.assertIn("revision=6\n", self.fake.props[branch])
        self.assertEqual(self.fake.heads[("megamek", "main")], support.SHA["megamek"])
        for name in ("feature", "releases/0.51.2"):
            with self.subTest(branch=name), self.assertRaises(release.UnsafeInventory):
                self.stamp(name)

    def test_stamp_leaves_the_next_run_a_noop(self):
        self.fake.add_suite("experimental", "0.51.02.5", support.SHA)
        self.stamp()
        found = release.discovered_inventory("experimental", getter=self.fake.get,
                                             fetch=self.fake.fetch)
        self.assertTrue(release.plan(found)["noop"])

    def test_bump_clears_the_old_lines_revision(self):
        self.fake.add_suite("experimental", "0.51.02.5", support.SHA)
        self.stamp()
        self.branches()
        self.fake.add_suite("core", "0.51.02.6", support.SHA)
        self.bump()
        self.assertIn("patch=03\n#revision=5\n", self.main_text())
        self.assertEqual(release.properties_revision(self.main_text()), 0)
        self.assertIn("no release yet", self.stamp()[0])

    def test_the_real_version_file_can_be_stamped_and_cleared(self):
        text = (Path(__file__).resolve().parents[1] / release.VERSION_PROPERTIES).read_text(
            encoding="utf-8")
        line = release.properties_line(text)
        cleared = cut.cleared(text)
        self.assertEqual(release.properties_revision(cleared), 0)
        restamped = cut.stamped(cleared, 7)
        self.assertEqual(release.properties_revision(restamped), 7)
        self.assertEqual(release.properties_line(restamped), line)
        self.assertEqual(release.without_revision(restamped), release.without_revision(text))

    def test_stamped_and_cleared_text(self):
        head = "major=0\nminor=51\n"
        template = head + "patch=01\n#\n#revision=1\n#\n# revision notes\n"
        self.assertEqual(cut.stamped(template, 2),
                         head + "patch=01\n#\nrevision=2\n#\n# revision notes\n")
        self.assertEqual(cut.stamped("major=0\r\nminor=51\r\npatch=01\r\nrevision=1\r\n", 3),
                         "major=0\r\nminor=51\r\npatch=01\r\nrevision=3\r\n")
        self.assertEqual(cut.stamped("major=0\r\nminor=51\r\npatch=01\r\nx=1", 4),
                         "major=0\r\nminor=51\r\npatch=01\r\nrevision=4\r\nx=1")
        self.assertEqual(cut.cleared("patch=01\n revision=4\n"), "patch=01\n#revision=4\n")
        self.assertEqual(cut.cleared(template), template)
        for text, revision in ((head + "patch=01\n", 0), (head, 2),
                               (head + "patch=1\npatch=2\n", 2),
                               (head + "patch=01\nrevision=1\nrevision=2\n", 3)):
            with self.subTest(text=text), self.assertRaises(release.UnsafeInventory):
                cut.stamped(text, revision)


if __name__ == "__main__":
    unittest.main()
