"""Offline orchestration and workflow gates; no remote calls."""
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import suite_release_coordinator as coordinator
from suite_release_plan import REPOS, UnsafeInventory


class CoordinatorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.state = self.root / "state"
        self.record = json.loads((Path(__file__).resolve().parents[1] /
                                  "megamek/testresources/suite-records/complete.json").read_text())
        self.commits = {repo: self.record["products"][p]["commit"]
                        for p, repo in REPOS.items()}
        self.commits["mm-data"] = self.record["mmData"]["commit"]
        self.inventory = dict(commits=self.commits.copy(), previous=self.record,
                              floor=self.record["version"], membership="milestone",
                              tags={repo: [] for repo in REPOS.values()},
                              releases={repo: [] for repo in REPOS.values()})
        self.calls = []

    def discover(self, membership, **kwargs):
        self.assertEqual(membership, "milestone")
        self.assertIn("retain", kwargs)
        self.assertIn("attest", kwargs)
        return self.inventory

    def builder(self, inventory, sources, prior, output, **kwargs):
        self.assertEqual(set(prior), set(REPOS))
        output.mkdir()
        paths = {}
        for product in REPOS:
            path = output / self.record["products"][product]["asset"]["name"]
            path.write_bytes(product.encode())
            paths[product] = str(path)
        return dict(status="OFFLINE_NONPUBLISHING", versionCandidate="0.52.00",
                    productVersions={p: self.record["products"][p]["version"] for p in REPOS},
                    archives=paths)

    def test_record_only_dry_and_live_bindings(self):
        coordinator.prepare("milestone", False, None, self.root, self.state,
                            discover=self.discover, builder=self.builder)
        def publisher(inventory, result, sources, **kwargs):
            self.calls.append((inventory, result, kwargs))
        self.assertIn("DRY_RUN_ONLY", coordinator.finish(
            self.state, self.root, False, False, publisher=publisher))
        self.assertFalse(self.calls)
        self.assertEqual(coordinator.finish(
            self.state, self.root, False, True, publisher=publisher),
            "PUBLISHED_COMPLETE_RECORD")
        self.assertEqual(len(self.calls), 1)
        self.assertEqual(self.calls[0][1]["versionCandidate"], "0.52.00")
        path = self.state / "inventory.json"
        path.write_text(path.read_text() + " ")
        with self.assertRaisesRegex(UnsafeInventory, "sealed inventory"):
            coordinator.finish(self.state, self.root, False, True, publisher=publisher)
        self.assertEqual(len(self.calls), 1)

    def test_weekly_noop_does_not_call_publisher(self):
        self.inventory["membership"] = "weekly"
        self.inventory["previous"]["membership"] = "weekly"
        def discover(membership, **kwargs):
            return self.inventory
        coordinator.prepare("weekly", False, None, self.root, self.state,
                            discover=discover, builder=self.builder)
        self.assertIn("VERIFIED_WEEKLY_NOOP", coordinator.finish(
            self.state, self.root, False, True,
            publisher=lambda *a, **k: self.fail("publisher called"),
            noop=lambda *a, **k: self.record))

    def test_workflow_shape(self):
        workflow = (Path(__file__).resolve().parents[1] /
                    ".github/workflows/game-suite-release.yml").read_text()
        self.assertIn("workflow_dispatch:", workflow)
        self.assertNotIn("schedule:", workflow)
        self.assertIn("default: true", workflow)
        self.assertIn("suite-release-live", workflow)
        self.assertIn("SUITE_RELEASES_ENABLED", workflow)
        self.assertIn("actions/create-github-app-token@v3", workflow)
        self.assertIn("repositories: megamek,megameklab,mekhq", workflow)
        self.assertIn("steps.prepare.outputs.noop == 'false'", workflow)
        self.assertNotIn("steps.prepare.outputs.changed", workflow)
        self.assertLess(workflow.index("id: prepare"), workflow.index("id: app"))


if __name__ == "__main__":
    unittest.main()
