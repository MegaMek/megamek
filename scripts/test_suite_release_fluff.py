"""Unit fluff download tests against an in-memory GitHub and a local mm-data repository."""

import json
import os
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path

import suite_release_fluff as fluff
import suite_release_plan as release
import suite_test_support as support

UUID = "0a1b2c3d-0000-4000-8000-000000000001"


def git(directory, *args):
    environment = {**os.environ, "GIT_AUTHOR_DATE": "2026-01-01T00:00:00Z",
                   "GIT_COMMITTER_DATE": "2026-01-01T00:00:00Z"}
    result = subprocess.run(["git", "-C", str(directory), "-c", "user.name=Test",
                             "-c", "user.email=test@example.com", "-c", "commit.gpgsign=false",
                             *args], capture_output=True, check=True, env=environment)
    return result.stdout.decode().strip()


class FluffTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temporary = tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
        cls.sources = Path(cls.temporary.name) / "sources"
        data = cls.sources / "mm-data"
        (data / "data").mkdir(parents=True)
        git(data, "init", "-q")
        (data / "data" / "units.txt").write_text("units\n", encoding="utf-8")
        git(data, "add", "-A")
        git(data, "commit", "-q", "-m", "before the split")
        cls.before = git(data, "rev-parse", "HEAD")
        (data / "unitfluff").mkdir()
        for index in (1, 2):
            (data / "unitfluff" / UUID.replace("1", str(index))).with_suffix(".yml").write_text(
                f"overview: Unit {index}\n", encoding="utf-8")
        git(data, "add", "-A")
        git(data, "commit", "-q", "-m", "split the fluff")
        cls.after = git(data, "rev-parse", "HEAD")

    @classmethod
    def tearDownClass(cls):
        cls.temporary.cleanup()

    def setUp(self):
        self.fake = support.FakeGitHub()

    def commits(self, data):
        return {**support.SHA, "mm-data": data}

    def attach(self, channel="experimental", dry_run=False, state=None, writes="fake"):
        return fluff.attach(channel, self.sources, dry_run, state, getter=self.fake.get,
                            fetch=self.fake.fetch,
                            writes=self.fake if writes == "fake" else writes)

    def megamek_assets(self):
        return [a["name"] for a in self.fake.releases["megamek"][0]["assets"]]

    def test_upload_attaches_the_zip_and_reruns_keep_it(self):
        self.fake.add_suite("experimental", "0.51.02.5", self.commits(self.after))
        report = self.attach()
        self.assertIn("Attached `unit-fluff-0.51.02.5.zip` (2 files", report[0])
        self.assertEqual(self.fake.writes, [("upload", "megamek", "unit-fluff-0.51.02.5.zip")])
        self.assertEqual(self.megamek_assets(), ["MegaMek-0.51.02.5.tar.gz",
                                                 "suite-record-0.51.02.5.json",
                                                 "unit-fluff-0.51.02.5.zip"])
        asset = self.fake.releases["megamek"][0]["assets"][-1]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "fluff.zip"
            self.fake.fetch("megamek", asset["id"], path)
            with zipfile.ZipFile(path) as archive:
                self.assertEqual(archive.read(f"data/unitfluff/{UUID}.yml"), b"overview: Unit 1\n")
        # The record still describes the suite, so the launcher sees the same release.
        found = release.complete_records(release.release_details(self.fake.get)["megamek"],
                                         self.fake.fetch)
        self.assertEqual([r["version"] for r in found], ["0.51.02.5"])
        self.assertIn("already attached", self.attach()[0])
        self.assertEqual(len(self.fake.writes), 1)

    def test_only_the_channels_latest_suite_is_attached(self):
        self.fake.add_suite("core", "0.51.01.3", self.commits(self.before))
        self.fake.add_suite("experimental", "0.51.02.5", self.commits(self.after))
        self.assertIn("has no `unitfluff` folder", self.attach("core")[0])
        self.assertEqual(self.fake.writes, [])
        self.assertIn("0.51.02.5", self.attach()[0])
        self.assertEqual(len(self.fake.writes), 1)

    def test_suites_before_the_split_and_without_records_are_skipped(self):
        self.assertIn("No published experimental suite", self.attach()[0])
        self.fake.add_suite("experimental", "0.51.02.5", self.commits(self.before))
        self.assertIn(f"mm-data `{self.before[:10]}` of 0.51.02.5 has no `unitfluff` folder",
                      self.attach()[0])
        self.assertEqual(self.fake.writes, [])

    def test_dry_run_builds_but_writes_nothing(self):
        self.fake.add_suite("experimental", "0.51.02.5", self.commits(self.after))
        report = self.attach(dry_run=True, writes=None)
        self.assertRegex(report[0], r"^Would attach `unit-fluff-0\.51\.02\.5\.zip` \(2 files, "
                                    r"[0-9]+ bytes, SHA-256 `[0-9a-f]{64}`\)")
        self.assertTrue(report[0].endswith("to the MegaMek 0.51.02.5 release."))
        self.assertEqual(self.fake.writes, [])
        self.assertEqual(self.megamek_assets()[-1], "suite-record-0.51.02.5.json")

    def state(self, directory, inventory):
        state = Path(directory) / "suite-state"
        state.mkdir()
        (state / "inventory.json").write_text(json.dumps(inventory), encoding="utf-8")
        return state

    def test_dry_run_uses_this_runs_frozen_mm_data(self):
        self.fake = support.FakeGitHub(commits=self.commits(self.after))
        inventory = release.discovered_inventory("experimental", getter=self.fake.get,
                                                 fetch=self.fake.fetch)
        with tempfile.TemporaryDirectory() as directory:
            state = self.state(directory, inventory)
            report = self.attach(dry_run=True, state=state, writes=None)
            self.assertIn(f"`unit-fluff-0.51.02.1.zip` (2 files", report[0])
            self.assertIn(f"from mm-data `{self.after[:10]}`", report[0])
            self.assertTrue(report[0].endswith("release once it is published."))
            with self.assertRaisesRegex(release.UnsafeInventory, "another channel"):
                self.attach("core", dry_run=True, state=state, writes=None)
            # Live, the record this run should have published must be the latest one.
            with self.assertRaisesRegex(release.UnsafeInventory, "not this run's 0.51.02.1"):
                self.attach(state=state)
        self.assertEqual(self.fake.writes, [])

    def test_no_op_state_falls_back_to_the_latest_record(self):
        self.fake = support.FakeGitHub(commits=self.commits(self.after))
        self.fake.add_suite("experimental", "0.51.02.5", self.commits(self.after))
        inventory = release.discovered_inventory("experimental", getter=self.fake.get,
                                                 fetch=self.fake.fetch)
        self.assertTrue(release.plan(inventory)["noop"])
        with tempfile.TemporaryDirectory() as directory:
            report = self.attach(state=self.state(directory, inventory))
        self.assertIn("Attached `unit-fluff-0.51.02.5.zip`", report[0])

    def test_commit_missing_from_the_clone_blocks(self):
        self.fake.add_suite("experimental", "0.51.02.5", self.commits("e" * 40))
        with self.assertRaisesRegex(release.UnsafeInventory, "is not in"):
            self.attach()
        self.assertEqual(self.fake.writes, [])

    def test_incomplete_or_unconfirmed_uploads_block(self):
        self.fake.add_suite("experimental", "0.51.02.5", self.commits(self.after))
        host = self.fake.releases["megamek"][0]
        host["assets"].append({"id": 1, "name": "unit-fluff-0.51.02.5.zip", "size": 0,
                               "state": "starter"})
        with self.assertRaisesRegex(release.UnsafeInventory, "not a complete upload"):
            self.attach()
        host["assets"].pop()
        fake = self.fake

        class Broken:
            def upload(self, repo, release_id, path):
                asset_id = fake.upload(repo, release_id, path)
                host["assets"][-1]["state"] = "starter"
                return asset_id

        with self.assertRaisesRegex(release.UnsafeInventory, "upload not confirmed"):
            self.attach(writes=Broken())
        with self.assertRaisesRegex(release.UnsafeInventory, "missing writes"):
            self.attach(writes=None)

    def test_zip_layout_and_determinism(self):
        with tempfile.TemporaryDirectory() as first, tempfile.TemporaryDirectory() as second:
            path, files = fluff.build(self.sources, self.after, "0.51.02.5", first, subprocess.run)
            again, _ = fluff.build(self.sources, self.after, "0.51.02.5", second, subprocess.run)
            self.assertEqual(path.name, "unit-fluff-0.51.02.5.zip")
            self.assertEqual(files, 2)
            self.assertEqual(path.read_bytes(), again.read_bytes())
            with zipfile.ZipFile(path) as archive:
                self.assertEqual(sorted(archive.namelist()), [
                    "data/", "data/unitfluff/", f"data/unitfluff/{UUID}.yml",
                    f"data/unitfluff/{UUID.replace('1', '2')}.yml"])
            for names in (["data/unitfluff/"], ["data/unitfluff/a.yml", "data/units.txt"],
                          ["unitfluff/a.yml"]):
                with self.subTest(names=names):
                    bad = Path(first) / "bad.zip"
                    with zipfile.ZipFile(bad, "w") as archive:
                        for name in names:
                            archive.writestr(name, "" if name.endswith("/") else "x")
                    with self.assertRaisesRegex(release.UnsafeInventory, "layout"):
                        fluff.checked(bad)


if __name__ == "__main__":
    unittest.main()
