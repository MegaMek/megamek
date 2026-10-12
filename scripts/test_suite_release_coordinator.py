"""Offline orchestration and workflow gates; no remote calls."""
import json
import os
import shutil
import subprocess
import tempfile
import textwrap
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

    def test_previous_attestation_uses_its_pinned_read_only_contract(self):
        paths = {product: self.root / product for product in REPOS}
        worktrees = {product: self.root / repo for product, repo in REPOS.items()}
        def discover(membership, **kwargs):
            kwargs["attest"](self.record, paths)
            return self.inventory
        with (patch.object(coordinator, "stage", return_value=worktrees),
              patch.object(coordinator, "attest_archives") as attest,
              patch.object(coordinator, "clean") as clean):
            coordinator.prepare("milestone", False, None, self.root, self.state,
                                discover=discover, builder=self.builder)
            attest.assert_called_once_with(
                self.record, paths, worktrees=worktrees, portable=False)
            self.assertEqual(clean.call_count, 4)

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

    def test_app_check_workflow_is_manual_protected_and_nonpublishing(self):
        workflow = (Path(__file__).resolve().parents[1] /
                    ".github/workflows/game-suite-app-check.yml").read_text()
        for required in ("workflow_dispatch:", "contents: read", "suite-release-live",
                         "group: game-suite-release-global", "cancel-in-progress: false",
                         "timeout-minutes: 5", "SUITE_RELEASE_APP_ID",
                         "SUITE_RELEASE_APP_PRIVATE_KEY", "actions/create-github-app-token@v3",
                         "repositories: megamek,megameklab,mekhq", "permission-contents: write",
                         "GH_TOKEN: ${{ steps.app.outputs.token }}",
                         "'installation/repositories?per_page=100'",
                         '[.repositories[].full_name] | sort | join(",")'):
            self.assertIn(required, workflow)
        for forbidden in ("schedule:", "pull_request:", "push:", "actions/checkout",
                          "SUITE_RELEASES_ENABLED", "skip-token-revoke:", "--publish",
                          "suite_release_coordinator", "gh release", "git push",
                          "permission-actions:", "mm-data"):
            self.assertNotIn(forbidden, workflow)
        requests = [line.strip() for line in workflow.splitlines() if "gh api " in line]
        self.assertEqual(len(requests), 2)
        self.assertTrue(all("gh api --method GET " in line for line in requests))


class AppAccessCheckTests(unittest.TestCase):
    def setUp(self):
        self.bash = shutil.which("bash")
        if not self.bash:
            self.skipTest("Bash is required to execute the workflow's GET-only access check")
        workflow = (Path(__file__).resolve().parents[1] /
                    ".github/workflows/game-suite-app-check.yml").read_text()
        blocks = workflow.split("        run: |\n")
        self.script = textwrap.dedent(blocks[-1])
        self.configuration = textwrap.dedent(blocks[1].split("\n      - name:", 1)[0])

    def run_check(self, repositories, denied=""):
        stub = r"""
gh() {
  echo "REQUEST: $*" >&2
  if [[ "$1" != "api" || "$2" != "--method" || "$3" != "GET" ]]; then
    echo "Unexpected or non-GET request" >&2
    return 90
  fi
  case "$4" in
    installation/repositories\?per_page=100)
      if [[ "$CHECK_REPOSITORIES" == "FAIL" ]]; then return 22; fi
      echo "$CHECK_REPOSITORIES"
      ;;
    repos/MegaMek/megamek/git/ref/heads/main|repos/MegaMek/megameklab/git/ref/heads/main|repos/MegaMek/mekhq/git/ref/heads/main)
      if [[ "$4" == "$CHECK_DENIED" ]]; then return 23; fi
      ;;
    *)
      echo "Unexpected endpoint" >&2
      return 91
      ;;
  esac
}
"""
        with tempfile.TemporaryDirectory() as directory:
            summary = Path(directory) / "summary.txt"
            environment = dict(os.environ, CHECK_REPOSITORIES=repositories,
                               CHECK_DENIED=denied, GITHUB_STEP_SUMMARY=str(summary))
            result = subprocess.run([self.bash], input=stub + self.script, env=environment,
                                    capture_output=True, text=True, timeout=10)
            report = summary.read_text() if summary.exists() else ""
        return result, report

    def test_missing_configuration_fails_explicitly_and_valid_configuration_passes(self):
        for app_id, key_present in (("", "false"), ("12345", "false"), ("", "true"),
                                    ("12345", "true")):
            with self.subTest(app_id=app_id, key_present=key_present):
                result = subprocess.run(
                    [self.bash], input=self.configuration,
                    env=dict(os.environ, APP_ID=app_id, APP_KEY_PRESENT=key_present),
                    capture_output=True, text=True, timeout=10)
                expected = 0 if app_id and key_present == "true" else 1
                self.assertEqual(result.returncode, expected, result.stderr)
                if expected:
                    self.assertIn("::error::Missing suite release App configuration", result.stdout)

    def test_exact_scope_reads_all_three_repositories_without_writes(self):
        result, report = self.run_check("MegaMek/megamek,MegaMek/megameklab,MegaMek/mekhq")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr.count("REQUEST: api --method GET "), 4)
        for repo in REPOS.values():
            self.assertIn(f"repos/MegaMek/{repo}/git/ref/heads/main", result.stderr)
        self.assertIn("No repository writes or game builds were performed.", report)

    def test_wrong_missing_duplicate_or_extra_repository_scope_fails_before_reads(self):
        for repositories in ("", "MegaMek/megamek",
                             "MegaMek/megamek,MegaMek/megameklab,MegaMek/wrong",
                             "MegaMek/megamek,MegaMek/megameklab,MegaMek/mekhq,MegaMek/mm-data",
                             "MegaMek/megamek,MegaMek/megameklab,MegaMek/mekhq,MegaMek/mekhq"):
            with self.subTest(repositories=repositories):
                result, report = self.run_check(repositories)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("::error::App token repository scope", result.stdout)
                self.assertNotIn("git/ref/", result.stderr)
                self.assertEqual(report, "")

    def test_repository_listing_failure_is_not_success(self):
        result, report = self.run_check("FAIL")
        self.assertEqual(result.returncode, 22)
        self.assertNotIn("git/ref/", result.stderr)
        self.assertEqual(report, "")

    def test_any_repository_read_failure_stops_without_a_success_report(self):
        for repo in REPOS.values():
            with self.subTest(repo=repo):
                result, report = self.run_check(
                    "MegaMek/megamek,MegaMek/megameklab,MegaMek/mekhq",
                    f"repos/MegaMek/{repo}/git/ref/heads/main")
                self.assertEqual(result.returncode, 23)
                self.assertNotIn("access verified", result.stdout)
                self.assertEqual(report, "")


if __name__ == "__main__":
    unittest.main()
