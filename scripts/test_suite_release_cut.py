"""Core cut tests against an in-memory GitHub; no network."""

import unittest

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


if __name__ == "__main__":
    unittest.main()
