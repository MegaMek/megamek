"""Read-only planner tests: python -m unittest discover -s scripts -p 'test_suite_*.py'."""

import copy
import json
import subprocess
import unittest
from pathlib import Path

import suite_release_plan as release
import suite_test_support as support

ROOT = Path(__file__).resolve().parents[1]
FIXTURE = ROOT / "megamek/testresources/suite-records/complete.json"

class FailureDetailTests(unittest.TestCase):
    def test_gradle_failure_survives_advice_footer_in_either_stream(self):
        output = (
            "FAILURE: Build failed with an exception.\n\n"
            "* What went wrong:\n"
            "Execution failed for task ':megamek:test'.\n"
            "> There were failing tests.\n\n"
            "* Try:\n"
            "> Run with --stacktrace option to get the stack trace.\n"
            "> Run with --info or --debug option to get more log output.\n"
            "> Run with --scan to get full insights.\n"
            "> Get more help at https://help.gradle.org.\n\n"
            "BUILD FAILED in 15s\n"
        )
        for stream in ("stdout", "stderr"):
            with self.subTest(stream=stream):
                error = subprocess.CalledProcessError(
                    1, ["gradlew", ":megamek:test"],
                    output=output if stream == "stdout" else "JVM warning\n",
                    stderr=output if stream == "stderr" else "JVM warning\n")
                detail = release.failure_detail(error)
                self.assertTrue(detail.startswith("exit 1: "))
                self.assertIn("Execution failed for task ':megamek:test'.", detail)
                self.assertIn("There were failing tests.", detail)
                self.assertNotIn("Run with", detail)
                self.assertNotIn("JVM warning", detail)

    def test_non_gradle_failure_retains_both_streams(self):
        error = subprocess.CalledProcessError(
            2, ["tool"], output=b"output cause\n", stderr=b"error cause\n")
        self.assertEqual(release.failure_detail(error),
                         "exit 2: error cause | output cause")

    def test_credentials_and_commands_are_not_exposed_in_either_stream(self):
        output = (
            "* What went wrong:\n"
            "Execution failed for task ':megamek:distTar'.\n"
            "Authorization: bearer credential-value\n"
            "password=credential-value\n"
            "cookie=credential-value\n"
            "download https://user:credential-value@example.invalid/file\n"
            + "opaque " + "a" * 40 + "\n"
            "* Try:\n"
            "token=credential-value\n"
        )
        error = subprocess.CalledProcessError(
            1, ["tool", "command-credential-value"],
            output=output.encode("utf-8"), stderr=output)
        detail = release.failure_detail(error)
        self.assertIn("Execution failed for task ':megamek:distTar'.", detail)
        self.assertIn("opaque [redacted]", detail)
        self.assertNotIn("credential-value", detail)
        self.assertNotIn("a" * 40, detail)
        self.assertNotIn("https://", detail)

    def test_long_gradle_sections_and_lines_are_bounded(self):
        output = "* What went wrong:\n" + "".join(
            f"cause {i}: " + "diagnostic text " * 40 + "\n" for i in range(100))
        output += "* Try:\n" + "footer\n" * 100
        error = subprocess.CalledProcessError(1, ["tool"], output=output, stderr=output)
        detail = release.failure_detail(error)
        self.assertLessEqual(len(detail), len("exit 1: ") + 20 * 200 + 19 * 3)
        self.assertIn("cause 0:", detail)
        self.assertIn("cause 8:", detail)
        self.assertNotIn("cause 9:", detail)
        self.assertNotIn("footer", detail)

    def test_missing_executable_remains_explicit(self):
        self.assertEqual(
            release.failure_detail(FileNotFoundError("private executable path")),
            "could not start executable (FileNotFoundError; check installation/PATH)")


class VersionTests(unittest.TestCase):
    def test_line_and_branch_parsing(self):
        self.assertEqual(release.properties_line("major=0\nminor=51\npatch=2\n"), "0.51.02")
        self.assertEqual(release.properties_line("# c\nmajor = 0\nminor=51\npatch=01\n"),
                         "0.51.01")
        for text in ("major=0\nminor=51\n", "major=0\nminor=x\npatch=1\n"):
            with self.assertRaises(release.UnsafeInventory):
                release.properties_line(text)
        self.assertEqual(release.branch_line("releases/0.51.02"), "0.51.02")
        for name in ("main", "releases/0.51.2", "releases/0.51.02.1", "release/0.51.02",
                     "releases/0.51.02/x", None):
            self.assertIsNone(release.branch_line(name), name)

    def test_reserve_continues_the_line_counter_across_repositories(self):
        empty = {repo: [] for repo in release.REPOS.values()}
        self.assertEqual(release.reserve(empty, empty, "0.51.02"), "0.51.02.1")
        tags = dict(empty, megamek=["v0.51.02.31", "v0.51.03.9", "v0.51.01.40", "nightly"])
        releases = dict(empty, mekhq=["v0.51.02.32"], megameklab=["v0.51.2.33"])
        self.assertEqual(release.reserve(tags, releases, "0.51.02"), "0.51.02.34")
        self.assertEqual(release.reserve(tags, releases, "0.51.03"), "0.51.03.10")
        with self.assertRaises(release.UnsafeInventory):
            release.reserve(tags, releases, "0.51.2")
        with self.assertRaises(release.UnsafeInventory):
            release.reserve({"megamek": []}, releases, "0.51.02")

    def test_canonical_versions(self):
        self.assertTrue(release.canonical("0.51.02.7"))
        for value in ("0.51.2.7", "0.51.02.07", "0.51.02.0"):
            self.assertFalse(release.canonical(value) if release.VERSION.fullmatch(value)
                             else False, value)


class RecordTests(unittest.TestCase):
    def setUp(self):
        self.record = json.loads(FIXTURE.read_text(encoding="utf-8"))

    def test_fixture_and_both_channels(self):
        release.validate_record(self.record)
        self.record["channel"] = "experimental"
        release.validate_record(self.record)

    def test_rejects_old_schema_and_mixed_versions(self):
        changes = [
            lambda r: r.update(schemaVersion=1),
            lambda r: r.update(channel="weekly"),
            lambda r: r.pop("channel"),
            lambda r: r.update(membership="milestone"),
            lambda r: r.update(version="0.51.02", tag="v0.51.02"),
            lambda r: r["products"]["MekHQ"].update(version="0.51.02.6"),
            lambda r: r["products"]["MekHQ"].update(tag="v0.51.02.6"),
            lambda r: r["products"]["MegaMek"]["asset"].update(name="MegaMek-0.51.02.6.tar.gz"),
            lambda r: r["mmData"].update(commit="main"),
        ]
        for index, change in enumerate(changes):
            record = copy.deepcopy(self.record)
            change(record)
            with self.subTest(index=index), self.assertRaises(release.UnsafeInventory):
                release.validate_record(record)


def inventory(channel="experimental", branch="main", line="0.51.02", previous=None,
              rehearsal=False, commits=None, tags=None):
    empty = {repo: [] for repo in release.REPOS.values()}
    return {"channel": channel, "branch": branch, "rehearsal": rehearsal, "line": line,
            "commits": dict(commits or support.SHA), "tags": tags or empty,
            "releases": copy.deepcopy(empty), "previous": previous}


class PlanTests(unittest.TestCase):
    def test_first_experimental(self):
        proposal = release.plan(inventory())
        self.assertEqual(proposal["versionCandidate"], "0.51.02.1")
        self.assertIsNone(proposal["previousTag"])
        self.assertFalse(proposal["noop"])

    def test_noop_only_when_every_source_is_unchanged(self):
        fake = support.FakeGitHub()
        previous = fake.add_suite("experimental", "0.51.02.4", support.SHA)
        tags = {repo: list(fake.tags[repo]) for repo in release.REPOS.values()}
        proposal = release.plan(inventory(previous=previous, tags=tags))
        self.assertTrue(proposal["noop"])
        self.assertEqual(proposal["versionCandidate"], "0.51.02.5")
        self.assertEqual(proposal["previousTag"], "v0.51.02.4")
        for repo in release.SOURCES:
            commits = dict(support.SHA, **{repo: "f" * 40})
            self.assertFalse(release.plan(inventory(previous=previous, tags=tags,
                                                    commits=commits))["noop"], repo)

    def test_channel_and_branch_rules(self):
        for bad in (inventory(branch="releases/0.51.02"),
                    inventory(rehearsal=True),
                    inventory("core", "main"),
                    inventory("core", "releases/0.51.01"),
                    inventory("core", "releases/0.51.02", rehearsal=True),
                    inventory("stable"),
                    inventory(line="0.51.2"),
                    dict(inventory(), surprise=True)):
            with self.subTest(bad=bad), self.assertRaises(release.UnsafeInventory):
                release.plan(bad)
        self.assertEqual(release.plan(inventory("core", "releases/0.51.02"))["channel"], "core")
        self.assertTrue(release.plan(inventory("core", "main", rehearsal=True))["rehearsal"])

    def test_previous_must_match_channel_and_versions_increase(self):
        fake = support.FakeGitHub()
        core = fake.add_suite("core", "0.51.02.9", support.SHA)
        with self.assertRaisesRegex(release.UnsafeInventory, "another channel"):
            release.plan(inventory(previous=core))
        # A Core line older than the previous Core release cannot be published again.
        with self.assertRaisesRegex(release.UnsafeInventory, "not newer"):
            release.plan(inventory("core", "releases/0.51.01", line="0.51.01", previous=core))


class GetTests(unittest.TestCase):
    def test_whitelist(self):
        allowed = [
            "repos/MegaMek/megamek/git/ref/heads/main",
            "repos/MegaMek/mm-data/git/ref/heads/releases/0.51.02",
            "repos/MegaMek/mekhq/git/matching-refs/heads/releases/?per_page=100",
            "repos/MegaMek/megamek/contents/megamek/resources/Version.properties?ref=" + "a" * 40,
            "repos/MegaMek/megameklab/commits/v0.51.02.3",
            "repos/MegaMek/mekhq/git/ref/tags/v0.51.02.3",
            "repos/MegaMek/megamek/tags?per_page=100&page=2",
            "repos/MegaMek/megamek/releases?per_page=100&page=1",
        ]
        denied = [
            "repos/MegaMek/megamek/git/ref/heads/feature",
            "repos/MegaMek/megamek/git/ref/heads/releases/0.51.2",
            "repos/MegaMek/mekhq/contents/megamek/resources/Version.properties?ref=" + "a" * 40,
            "repos/MegaMek/megamek/contents/megamek/resources/Version.properties?ref=main",
            "repos/MegaMek/mm-data/releases?per_page=100&page=1",
            "repos/MegaMek/other/git/ref/heads/main",
            "repos/Other/megamek/git/ref/heads/main",
            "repos/MegaMek/megamek/commits/v0.51.2.3",
            "repos/MegaMek/megamek/releases/generate-notes",
        ]
        ok = lambda command, **kwargs: subprocess.CompletedProcess(command, 0, stdout=b"{}")
        for endpoint in allowed:
            self.assertEqual(release.gh_get(endpoint, runner=ok), {}, endpoint)
        for endpoint in denied:
            with self.subTest(endpoint=endpoint), self.assertRaises(release.UnsafeInventory):
                release.gh_get(endpoint, runner=ok)


class DiscoveryTests(unittest.TestCase):
    def setUp(self):
        self.fake = support.FakeGitHub(line="0.51.02")

    def discover(self, channel, branch=None, rehearsal=False):
        return release.discovered_inventory(channel, branch, rehearsal,
                                            getter=self.fake.get, fetch=self.fake.fetch)

    def test_experimental_builds_main_after_previous_experimental(self):
        old = dict(support.SHA, megamek="e" * 40)
        self.fake.add_suite("experimental", "0.51.02.3", old)
        self.fake.add_suite("core", "0.51.01.20", old)
        found = self.discover("experimental")
        self.assertEqual(found["branch"], "main")
        self.assertEqual(found["previous"]["version"], "0.51.02.3")
        proposal = release.plan(found)
        self.assertEqual(proposal["versionCandidate"], "0.51.02.4")
        self.assertFalse(proposal["noop"])

    def test_experimental_refuses_a_line_that_was_already_cut(self):
        self.fake.set_branch("releases/0.51.02", support.SHA)
        with self.assertRaisesRegex(release.UnsafeInventory, "bump Version.properties"):
            self.discover("experimental")

    def test_core_uses_the_newest_release_branch_and_continues_its_counter(self):
        with self.assertRaisesRegex(release.UnsafeInventory, "Core cut first"):
            self.discover("core")
        self.fake.add_suite("experimental", "0.51.02.32", support.SHA)
        branch = {repo: support.sha("core" + repo) for repo in release.SOURCES}
        self.fake.set_branch("releases/0.51.01", support.SHA, "0.51.01")
        self.fake.set_branch("releases/0.51.02", branch, "0.51.02")
        self.fake.set_branch("main", dict(support.SHA, megamek="9" * 40), "0.51.03")
        found = self.discover("core")
        self.assertEqual(found["branch"], "releases/0.51.02")
        self.assertEqual(found["commits"], branch)
        self.assertIsNone(found["previous"])
        self.assertEqual(release.plan(found)["versionCandidate"], "0.51.02.33")

    def test_core_branch_must_exist_everywhere_and_match_its_line(self):
        self.fake.set_branch("releases/0.51.02", {"megamek": "a" * 40}, "0.51.02")
        with self.assertRaises(release.UnsafeInventory):
            self.discover("core")
        self.fake.set_branch("releases/0.51.02", support.SHA, "0.51.03")
        with self.assertRaisesRegex(release.UnsafeInventory, "own line"):
            self.discover("core")

    def test_rehearsal_builds_main_as_core(self):
        found = self.discover("core", rehearsal=True)
        self.assertEqual((found["branch"], found["channel"]), ("main", "core"))

    def test_records_are_found_per_channel_and_malformed_ones_block(self):
        self.fake.add_suite("core", "0.51.01.5", support.SHA)
        self.fake.add_suite("experimental", "0.51.02.1", support.SHA)
        records = release.complete_records(self.fake.releases["megamek"], self.fake.fetch)
        self.assertEqual([r["version"] for r in records], ["0.51.01.5", "0.51.02.1"])
        self.assertEqual(release.latest(records, "core")["version"], "0.51.01.5")
        self.assertIsNone(release.latest(records[:1], "experimental"))
        asset = self.fake.releases["megamek"][0]["assets"][-1]
        self.fake.assets[asset["id"]] = b"{}" + b" " * (asset["size"] - 2)
        with self.assertRaises(release.UnsafeInventory):
            release.complete_records(self.fake.releases["megamek"], self.fake.fetch)

    def test_drafts_are_left_out_of_the_inventory(self):
        # Only write tokens can list drafts, so they never take part in version allocation.
        before = release.plan(self.discover("experimental"))["versionCandidate"]
        self.fake.releases["mekhq"].append({"id": 5, "tag_name": "v0.51.02.7", "draft": True})
        inventory = self.discover("experimental")
        self.assertNotIn("v0.51.02.7", inventory["releases"]["mekhq"])
        self.assertEqual(before, release.plan(inventory)["versionCandidate"])
        self.fake.releases["mekhq"].append({"id": 6, "tag_name": "v0.51.02.8", "draft": "no"})
        with self.assertRaises(release.UnsafeInventory):
            self.discover("experimental")


if __name__ == "__main__":
    unittest.main()
