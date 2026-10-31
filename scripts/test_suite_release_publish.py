"""Publication tests against an in-memory GitHub; no network, Git or Gradle."""

import json
import tempfile
import unittest
from pathlib import Path

import suite_release_plan as release
import suite_release_publish as publish
import suite_test_support as support


class PublishTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.fake = support.FakeGitHub(line="0.51.02")
        self.old = dict(support.SHA, mekhq="e" * 40)
        self.validated = []
        self.attested = []

    def prepare(self, channel="experimental", branch="main", rehearsal=False):
        inventory = release.discovered_inventory(channel, branch, rehearsal, getter=self.fake.get,
                                                 fetch=self.fake.fetch)
        version = release.plan(inventory)["versionCandidate"]
        archives = {}
        for product in release.REPOS:
            path = self.root / f"{product}-{version}.tar.gz"
            path.write_bytes(f"built {product} {version}".encode())
            archives[product] = str(path)
        return inventory, archives

    def publish(self, inventory, archives, **kwargs):
        def validate(path):
            record = json.loads(Path(path).read_text(encoding="utf-8"))
            release.validate_record(record)
            self.validated.append(record)

        return publish.publish(
            inventory, archives, getter=self.fake.get, fetch=self.fake.fetch,
            writes=kwargs.pop("writes", self.fake),
            attest=lambda record, paths: self.attested.append(record["version"]),
            validate=validate, **kwargs)

    def test_experimental_publishes_prereleases_with_notes_and_record_last(self):
        self.fake.add_suite("experimental", "0.51.02.3", self.old)
        record = self.publish(*self.prepare())
        self.assertEqual((record["version"], record["channel"]), ("0.51.02.4", "experimental"))
        release.validate_record(record)
        self.assertEqual(self.attested, ["0.51.02.4"])
        kinds = [write[0] for write in self.fake.writes]
        # Notes for all three before any write; then tag, release, upload per product.
        self.assertEqual(kinds[:3], ["notes"] * 3)
        self.assertEqual(kinds[3:], ["tag", "release", "upload"] * 3 + ["upload"])
        self.assertEqual(self.fake.writes[-1], ("upload", "megamek", "suite-record-0.51.02.4.json"))
        self.assertTrue(all(w[3] == "v0.51.02.3" for w in self.fake.writes if w[0] == "notes"))
        self.assertTrue(all(w[3] is True for w in self.fake.writes if w[0] == "release"))
        for product, repo in release.REPOS.items():
            entry = self.fake.releases[repo][0]
            self.assertEqual(entry["name"], f"{product} 0.51.02.4 (Experimental)")
            self.assertIn("**Channel: Experimental**", entry["body"])
            self.assertIn(f"{repo} change since v0.51.02.3", entry["body"])
            self.assertEqual(self.fake.tags[repo]["v0.51.02.4"], support.SHA[repo])
        records = release.complete_records(self.fake.releases["megamek"], self.fake.fetch)
        self.assertEqual(release.latest(records, "experimental"), record)

    def test_first_core_release_is_latest_and_has_no_previous_tag(self):
        branch = {repo: support.sha("core" + repo) for repo in release.SOURCES}
        self.fake.add_suite("experimental", "0.51.02.32", self.old)
        self.fake.set_branch("releases/0.51.02", branch, "0.51.02")
        record = self.publish(*self.prepare("core", None))
        self.assertEqual((record["version"], record["channel"]), ("0.51.02.33", "core"))
        self.assertEqual(record["mmData"]["commit"], branch["mm-data"])
        self.assertTrue(all(w[3] is None for w in self.fake.writes if w[0] == "notes"))
        self.assertTrue(all(w[3] is False for w in self.fake.writes if w[0] == "release"))
        self.assertEqual(self.fake.releases["megamek"][0]["name"], "MegaMek 0.51.02.33")

    def test_rehearsal_and_noop_never_write(self):
        inventory, archives = self.prepare("core", "main", rehearsal=True)
        with self.assertRaisesRegex(release.UnsafeInventory, "rehearsal"):
            self.publish(inventory, archives)
        self.fake.add_suite("experimental", "0.51.02.3", support.SHA)
        with self.assertRaisesRegex(release.UnsafeInventory, "nothing changed"):
            self.publish(*self.prepare())
        self.assertEqual(self.fake.writes, [])

    def test_moves_after_the_build_block_before_any_write(self):
        inventory, archives = self.prepare()
        self.fake.tags["megameklab"]["v0.51.02.1"] = "a" * 40
        with self.assertRaisesRegex(release.UnsafeInventory, "moved after build"):
            self.publish(inventory, archives)
        inventory, archives = self.prepare()
        self.fake.add_suite("experimental", "0.51.01.9", self.old)
        with self.assertRaisesRegex(release.UnsafeInventory, "moved after build"):
            self.publish(inventory, archives)
        self.assertEqual(self.fake.writes, [])

    def test_drafts_never_move_the_inventory_but_still_block_their_tag(self):
        inventory, archives = self.prepare()
        version = release.plan(inventory)["versionCandidate"]
        # A draft is invisible to the read-only build but visible to the publish token.
        self.fake.releases["mekhq"].append({"id": 900, "tag_name": "v" + version, "draft": True})
        with self.assertRaisesRegex(release.UnsafeInventory, "suite version collision"):
            self.publish(inventory, archives)
        self.assertEqual(self.fake.writes, [])
        self.fake.releases["mekhq"][-1]["tag_name"] = "v0.51.01"
        record = self.publish(inventory, archives)
        self.assertEqual(record["version"], version)

    def test_wrong_archives_block_before_any_write(self):
        inventory, archives = self.prepare()
        renamed = Path(archives["MekHQ"]).with_name("MekHQ-0.51.02.9.tar.gz")
        Path(archives["MekHQ"]).rename(renamed)
        with self.assertRaisesRegex(release.UnsafeInventory, "archive name"):
            self.publish(inventory, dict(archives, MekHQ=str(renamed)))
        with self.assertRaises(release.UnsafeInventory):
            self.publish(inventory, {"MegaMek": archives["MegaMek"]})
        self.assertEqual(self.fake.writes, [])

    def test_a_failed_upload_leaves_no_record_and_the_next_run_moves_on(self):
        inventory, archives = self.prepare()
        fake = self.fake

        def failing(repo, release_id, path):
            if repo == "mekhq":
                raise release.UnsafeInventory("write failed (do not retry)")
            return support.FakeGitHub.upload(fake, repo, release_id, path)

        fake.upload = failing
        with self.assertRaisesRegex(release.UnsafeInventory, "do not retry"):
            self.publish(inventory, archives)
        del fake.upload
        self.assertEqual(release.complete_records(fake.releases["megamek"], fake.fetch), [])
        inventory, archives = self.prepare()
        self.assertEqual(self.publish(inventory, archives)["version"], "0.51.02.2")

    def test_presentation_truncates_very_long_notes(self):
        record = json.loads((Path(__file__).resolve().parents[1] /
                             "megamek/testresources/suite-records/complete.json").read_text())
        notes = "\n".join(f"* change {i}" for i in range(20000))
        shown = publish.release_presentation(record, "MekHQ", notes)
        self.assertLess(len(shown["body"]), 125000)
        self.assertIn("(Release notes truncated.)", shown["body"])
        self.assertIs(shown["prerelease"], False)
        self.assertEqual(shown["name"], "MekHQ 0.51.02.7")


class WriterTests(unittest.TestCase):
    def test_release_payload_goes_through_a_file_and_marks_channel(self):
        seen = {}

        def runner(args, **kwargs):
            seen["args"] = args
            seen["payload"] = json.loads(Path(args[-1]).read_text(encoding="utf-8"))
            body = dict(seen["payload"], id=7)
            return type("Result", (), {"stdout": json.dumps(body).encode()})()

        original = publish.api
        publish.api = lambda command, **kwargs: original(command, runner=runner, **kwargs)
        try:
            publish.GitHubWrites().release("megamek", "v0.51.02.1", "a" * 40,
                                           name="n", body="b" * 200000, prerelease=True)
        finally:
            publish.api = original
        self.assertEqual(seen["args"][-2], "--input")
        self.assertEqual(seen["payload"]["make_latest"], "false")
        self.assertIs(seen["payload"]["prerelease"], True)
        self.assertNotIn("b" * 1000, " ".join(seen["args"]))


if __name__ == "__main__":
    unittest.main()
