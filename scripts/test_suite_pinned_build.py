"""Offline fake-runner coverage; never invokes Git or Gradle."""

import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import suite_pinned_build as pinned
import suite_release_plan as release
import suite_test_support as support
from suite_release_plan import UnsafeInventory


class PinnedBuildTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.sources = self.root / "sources"
        self.sources.mkdir()
        for repo in pinned.SOURCES:
            (self.sources / repo / ".git").mkdir(parents=True)
        self.output = self.root / "result"
        self.github = support.FakeGitHub(line="0.51.02")
        self.inventory = self.discover("experimental", "main")
        self.commits = self.inventory["commits"]
        self.calls = []

    def discover(self, channel, branch, rehearsal=False):
        return release.discovered_inventory(channel, branch, rehearsal,
                                            getter=self.github.get, fetch=self.github.fetch)

    def fake(self, command, **kwargs):
        self.calls.append((command, kwargs))
        if command[:2] == ["git", "clone"]:
            Path(command[-1]).mkdir()
            (Path(command[-1]) / "gradlew").touch()
            (Path(command[-1]) / "gradlew.bat").touch()
        if any(command[1] == tasks[1] for tasks in pinned.TASKS.values()):
            product = next(p for p, tasks in pinned.TASKS.items() if tasks[1] in command)
            version = next(flag.split("=", 1)[1] for flag in command
                           if flag.startswith("-PsuiteReleaseVersion="))
            # Model the Gradle project output independently of the builder's lookup.
            project_dir = {"MegaMek": "megamek", "MegaMekLab": "megameklab",
                           "MekHQ": "MekHQ"}[product]
            path = kwargs["cwd"] / project_dir / "build" / "distributions" / (
                f"{product}-{version}.tar.gz")
            path.parent.mkdir(parents=True)
            path.write_bytes(b"fake archive")
        if command[:2] == ["git", "-C"]:
            if "remote" in command and "get-url" in command:
                return subprocess.CompletedProcess(command, 0,
                                                   stdout=pinned.official(Path(command[2]).name))
            if "rev-parse" in command:
                value = command[-1].removesuffix("^{commit}")
                if value == "HEAD":
                    value = self.commits[Path(command[2]).name]
                return subprocess.CompletedProcess(command, 0, stdout=value)
        return subprocess.CompletedProcess(command, 0, stdout="")

    def identity(self, path, product, version):
        meta = dict(schemaVersion="1", product=product, version=version,
                    minimumJavaVersion="21", mmDataCommit=self.commits["mm-data"],
                    megamekCommit=self.commits["megamek"],
                    megameklabCommit=self.commits["megameklab"],
                    mekhqCommit=self.commits["mekhq"],
                    megamekVersion=version, megameklabVersion=version)
        return meta, {product + ".jar": b"jar", "lib/MegaMek.jar": b"jar",
                      "lib/MegaMekLab.jar": b"jar"}

    def build(self, inventory=None, runner=None, identity=None):
        with patch.object(pinned, "archive_identity", side_effect=identity or self.identity):
            return pinned.build(inventory or self.inventory, self.sources, self.output,
                                runner=runner or self.fake)

    def gradle_calls(self):
        return [c for c, _ in self.calls if c[0].endswith(("gradlew", "gradlew.bat"))]

    def test_builds_all_three_from_exact_shas_in_order(self):
        result = self.build()
        self.assertEqual(result["versionCandidate"], "0.51.02.1")
        self.assertEqual(set(result["productVersions"].values()), {"0.51.02.1"})
        self.assertEqual(list(result["archives"]), list(pinned.REPOS))
        self.assertEqual(len([c for c, _ in self.calls if c[:2] == ["git", "clone"]]), 4)
        checkouts = [c for c, _ in self.calls if "checkout" in c]
        self.assertEqual([c[-1] for c in checkouts], [self.commits[r] for r in pinned.SOURCES])
        self.assertTrue(all("--detach" in c and "main" not in c for c in checkouts))
        ancestry = [c for c, _ in self.calls if "merge-base" in c]
        self.assertEqual({c[-1] for c in ancestry}, {"refs/remotes/origin/main"})
        gradle = self.gradle_calls()
        self.assertEqual([c[1] for c in gradle],
                         [task for tasks in pinned.TASKS.values() for task in tasks])
        for command in gradle:
            self.assertNotIn("--offline", command)
            self.assertIn("--no-daemon", command)
            self.assertIn("-PsuiteReleaseVersion=0.51.02.1", command)
            for key, repo in (("suiteMegaMekCommit", "megamek"),
                              ("suiteMegaMekLabCommit", "megameklab"),
                              ("suiteMekHQCommit", "mekhq"), ("suiteMmDataCommit", "mm-data")):
                self.assertIn(f"-P{key}={self.commits[repo]}", command)
        # MegaMek has no companions; MekHQ consumes both archives built earlier in this run.
        self.assertFalse(any("ArchiveFile=" in flag and "suiteArchiveFile" not in flag
                             for flag in gradle[0]))
        self.assertTrue(any(c.startswith("-PsuiteMegaMekArchiveFile=") for c in gradle[-1]))
        self.assertTrue(any(c.startswith("-PsuiteMegaMekLabArchiveFile=") for c in gradle[-1]))
        hq_verify = next(c for c in gradle if c[1] == pinned.TASKS["MekHQ"][2])
        hq_archive = next(Path(flag.split("=", 1)[1]) for flag in hq_verify
                          if flag.startswith("-PsuiteArchiveFile="))
        self.assertEqual(hq_archive.parent.parts[-3:], ("MekHQ", "build", "distributions"))
        self.assertEqual((self.output / hq_archive.name).read_bytes(), b"fake archive")

    def test_core_build_checks_ancestry_against_its_release_branch(self):
        branch = {repo: support.sha("core" + repo) for repo in pinned.SOURCES}
        self.github.set_branch("releases/0.51.02", branch, "0.51.02")
        self.inventory = self.discover("core", None)
        self.commits = self.inventory["commits"]
        self.assertEqual(self.build()["versionCandidate"], "0.51.02.1")
        ancestry = {c[-1] for c, _ in self.calls if "merge-base" in c}
        self.assertEqual(ancestry, {"refs/remotes/origin/releases/0.51.02"})
        checkouts = [c[-1] for c, _ in self.calls if "checkout" in c]
        self.assertEqual(checkouts, [branch[r] for r in pinned.SOURCES])

    def test_noop_and_unsafe_inventories_fail_before_clone(self):
        self.github.add_suite("experimental", "0.51.02.4", support.SHA)
        noop = self.discover("experimental", "main")
        unsafe = [dict(self.inventory, commits=dict(self.commits, megamek="main")),
                  dict(self.inventory, branch="feature/x"), noop]
        for inventory in unsafe:
            with self.subTest(inventory=inventory["branch"]), self.assertRaises(UnsafeInventory):
                self.build(inventory)
            self.assertFalse(self.calls)

    def test_existing_output_is_never_reused(self):
        self.output.mkdir()
        with self.assertRaisesRegex(UnsafeInventory, "output directory"):
            self.build()
        self.assertFalse(self.calls)

    def test_wrong_remote_and_dirty_stage_block_build(self):
        for fault in ("remote", "dirty"):
            self.calls.clear()

            def runner(command, **kwargs):
                response = self.fake(command, **kwargs)
                if fault == "remote" and "get-url" in command:
                    return subprocess.CompletedProcess(command, 0,
                                                       stdout="https://evil.invalid/repo")
                if fault == "dirty" and "status" in command:
                    return subprocess.CompletedProcess(command, 0, stdout="?? unknown")
                return response

            with self.subTest(fault=fault), self.assertRaises(UnsafeInventory):
                self.build(runner=runner)
            self.assertFalse(self.gradle_calls())

    def test_identity_mismatches_block_the_build(self):
        def wrong_commit(path, product, version):
            meta, jars = self.identity(path, product, version)
            meta["mmDataCommit"] = "f" * 40
            return meta, jars

        def wrong_companion(path, product, version):
            meta, jars = self.identity(path, product, version)
            if product == "MekHQ":
                jars["lib/MegaMek.jar"] = b"other"
            return meta, jars

        for identity, message in ((wrong_commit, "identity mismatch: MegaMek"),
                                  (wrong_companion, "companion mismatch: MekHQ")):
            with self.subTest(message=message), self.assertRaisesRegex(UnsafeInventory, message):
                self.build(identity=identity)
            self.assertFalse(self.output.exists())

    def test_dependency_resolution_failure_stops_pinned_build(self):
        directory = self.root / "megamek"
        directory.mkdir()
        (directory / "gradlew").touch()
        (directory / "gradlew.bat").touch()

        def unavailable(command, **kwargs):
            self.assertNotIn("--offline", command)
            raise subprocess.CalledProcessError(
                1, command, stderr="Could not resolve dependency: repository unavailable")

        with self.assertRaisesRegex(
                UnsafeInventory,
                "MegaMek :megamek:test failed: .*Could not resolve dependency"):
            pinned.gradle("MegaMek", pinned.TASKS["MegaMek"][0],
                          {"MegaMek": directory}, [], unavailable)

    def test_gradle_failure_names_product_and_task_without_retry(self):
        directory = self.root / "megamek"
        directory.mkdir()
        (directory / "gradlew").touch()
        (directory / "gradlew.bat").touch()
        calls = []

        def failed(command, **kwargs):
            calls.append(command)
            raise subprocess.CalledProcessError(
                1, command,
                output="* What went wrong:\nExecution failed for task ':megamek:distTar'.\n"
                       "> Missing pinned data.\n* Try:\n" + "advice\n" * 10,
                stderr="JVM warning\n")

        with self.assertRaises(UnsafeInventory) as caught:
            pinned.gradle("MegaMek", pinned.TASKS["MegaMek"][1],
                          {"MegaMek": directory}, [], failed)
        self.assertIn(f"MegaMek {pinned.TASKS['MegaMek'][1]} failed:", str(caught.exception))
        self.assertIn("Execution failed for task ':megamek:distTar'.", str(caught.exception))
        self.assertIn("Missing pinned data.", str(caught.exception))
        self.assertEqual(len(calls), 1)


if __name__ == "__main__":
    unittest.main()
