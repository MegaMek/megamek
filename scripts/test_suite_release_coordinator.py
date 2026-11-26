"""Coordinator tests: sealing, no-op skipping and dry-run safety. No network, Git or Gradle."""

import json
import tempfile
import unittest
from pathlib import Path

import suite_release_coordinator as coordinator
import suite_release_plan as release
import suite_test_support as support


class CoordinatorTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.state = self.root / "state"
        self.fake = support.FakeGitHub(line="0.51.02")
        self.builds = []
        self.published = []

    def discover(self, channel, branch, rehearsal):
        return release.discovered_inventory(channel, branch, rehearsal, getter=self.fake.get,
                                            fetch=self.fake.fetch)

    def builder(self, inventory, sources, output):
        self.builds.append(inventory["branch"])
        version = release.plan(inventory)["versionCandidate"]
        output.mkdir()
        archives = {}
        for product in release.REPOS:
            path = output / f"{product}-{version}.tar.gz"
            path.write_bytes(product.encode())
            archives[product] = str(path)
        return {"status": "OFFLINE_NONPUBLISHING", "versionCandidate": version,
                "productVersions": {p: version for p in release.REPOS}, "archives": archives}

    def prepare(self, channel="experimental", branch=None, rehearsal=False):
        return coordinator.prepare(channel, branch, rehearsal, self.root, self.state,
                                   discover=self.discover, builder=self.builder)

    def finish(self, publish):
        return coordinator.finish(self.state, self.root, publish,
                                  publisher=lambda *args, **kwargs: self.published.append(args))

    def test_dry_run_builds_but_never_publishes(self):
        proposal = self.prepare()
        self.assertEqual(proposal["versionCandidate"], "0.51.02.1")
        self.assertEqual(self.builds, ["main"])
        self.assertTrue(self.finish(False).startswith("DRY_RUN_ONLY"))
        self.assertEqual(self.published, [])
        self.assertEqual(self.finish(True), "PUBLISHED experimental 0.51.02.1")
        self.assertEqual(len(self.published), 1)

    def test_noop_skips_the_build_entirely(self):
        self.fake.add_suite("experimental", "0.51.02.6", support.SHA)
        proposal = self.prepare()
        self.assertTrue(proposal["noop"])
        self.assertEqual(self.builds, [])
        self.assertFalse((self.state / "build_result.json").exists())
        self.assertIn("NOOP", self.finish(True))
        self.assertEqual(self.published, [])
        summary = coordinator.prepared_summary(proposal, self.state)
        self.assertTrue(any("Nothing changed since v0.51.02.6" in line for line in summary))

    def test_rehearsal_builds_main_but_refuses_to_publish(self):
        proposal = self.prepare("core", rehearsal=True)
        self.assertEqual((proposal["channel"], self.builds), ("core", ["main"]))
        self.assertTrue(self.finish(False).startswith("DRY_RUN_ONLY"))
        with self.assertRaisesRegex(release.UnsafeInventory, "rehearsal"):
            self.finish(True)
        self.assertEqual(self.published, [])

    def test_seal_detects_changes(self):
        self.prepare()
        inventory = json.loads((self.state / "inventory.json").read_text())
        inventory["commits"]["mekhq"] = "f" * 40
        (self.state / "inventory.json").write_text(json.dumps(inventory))
        with self.assertRaisesRegex(release.UnsafeInventory, "sealed inventory"):
            self.finish(True)

    def test_archive_tamper_blocks_publication(self):
        self.prepare()
        result = json.loads((self.state / "build_result.json").read_text())
        Path(result["archives"]["MekHQ"]).write_bytes(b"changed")
        with self.assertRaisesRegex(release.UnsafeInventory, "sealed archive"):
            self.finish(True)
        self.assertEqual(self.published, [])

    def test_existing_state_is_never_reused(self):
        self.state.mkdir()
        with self.assertRaisesRegex(release.UnsafeInventory, "already exists"):
            self.prepare()


if __name__ == "__main__":
    unittest.main()
