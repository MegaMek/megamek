"""Focused Git-blob producer regression; no real mm-data checkout is modified."""

import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from suite_pinned_data import prepare
from suite_archive_verifier import GENERATED, pinned_tree, verify_zip_data


class PinnedDataTests(unittest.TestCase):
    def test_gradle_python_preflight_accepts_supported_and_rejects_missing_and_old_interpreters(self):
        root = Path(__file__).resolve().parents[1]
        gradle = root / ("gradlew.bat" if os.name == "nt" else "gradlew")
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            missing = base / "missing-python"
            executables = [(missing, "Python 3.10+")]
            old = base / ("old-python.cmd" if os.name == "nt" else "old-python")
            if os.name == "nt":
                old.write_bytes(b"@echo off\r\necho 3.9.0\r\n")
            else:
                old.write_text("#!/bin/sh\nprintf '3.9.0\\n'\n")
                old.chmod(0o755)
            executables.append((old, "Python 3.10+"))
            executables.append((Path(sys.executable), "missing or incorrectly named archive"))
            for executable, expected_detail in executables:
                with self.subTest(executable=executable):
                    command = [str(gradle), ":megamek:verifySuiteMegaMekArchive",
                               "--console=plain", "-PsuiteReleaseVersion=0.51.01",
                               "-PsuiteArchiveFile=" + str(base / "not-an-archive.tar.gz"),
                               "-PsuitePythonExecutable=" + str(executable)]
                    command += ["-P" + flag + "=" + letter * 40 for flag, letter in (
                        ("suiteMegaMekCommit", "a"), ("suiteMegaMekLabCommit", "b"),
                        ("suiteMekHQCommit", "c"), ("suiteMmDataCommit", "d"))]
                    result = subprocess.run(command, cwd=root, capture_output=True,
                                            text=True, check=False, timeout=180)
                    self.assertNotEqual(result.returncode, 0)
                    message = result.stdout + result.stderr
                    self.assertIn(expected_detail, message)
                    if executable != Path(sys.executable):
                        self.assertIn("suitePythonExecutable", message)

    def test_crlf_checkout_stages_exact_blobs_for_loose_files_and_all_zips(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            repo = base / "mm-data"
            repo.mkdir()
            subprocess.run(["git", "-C", str(repo), "init", "-q"], check=True)
            (repo / ".gitattributes").write_text("* text=auto\n")
            contents = {
                "data/boards/test.board": b"board\nrow\n",
                "data/mekfiles/notes.txt": b"loose\ntext\n",
                "data/mekfiles/sub/unit.blk": b"unit\nentry\n",
                "data/rat/default.txt": b"rat\nentry\n",
                "data/universe/planetary_systems/canon_systems/sol.xml": b"canon\nentry\n",
                "data/universe/planetary_systems/connector_systems/jump.xml": b"link\nentry\n",
                "data/fonts/test.ttf": b"\x00font\xff",
            }
            for name, body in contents.items():
                target = repo / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(body)
            subprocess.run(["git", "-C", str(repo), "add", "."], check=True)
            subprocess.run(["git", "-C", str(repo), "-c", "user.name=Fixture",
                            "-c", "user.email=fixture@example.invalid",
                            "commit", "-qm", "pin"], check=True)
            pin = subprocess.check_output(["git", "-C", str(repo), "rev-parse", "HEAD"],
                                          text=True).strip()
            subprocess.run(["git", "-C", str(repo), "config", "core.autocrlf", "true"], check=True)
            subprocess.run(["git", "-C", str(repo), "checkout-index", "-f", "-a"], check=True)
            # Some Git/FS combinations leave existing identical files untouched on
            # checkout-index. Force the Windows representation without altering HEAD.
            for name, body in contents.items():
                if b"\n" in body:
                    (repo / name).write_bytes(body.replace(b"\n", b"\r\n"))
            # Git for Windows can report a false stat-only change immediately
            # after checkout-index; renormalize the unchanged blob in the index.
            subprocess.run(["git", "-C", str(repo), "add", "--renormalize", "."], check=True)
            (repo / "data/boards/unknown.board").write_bytes(b"untracked\n")
            (repo / "data/rat/rogue.txt").write_bytes(b"untracked\n")
            (repo / "data/mekfiles/sub/rogue.blk").write_bytes(b"untracked\n")
            before = {name: (repo / name).read_bytes() for name in contents}
            self.assertIn(b"\r\n", before["data/boards/test.board"])
            self.assertIn(b"\r\n", before["data/mekfiles/sub/unit.blk"])
            output = base / "build/pinned"
            prepare(repo, pin, output)
            blobs = pinned_tree(repo, pin)
            for name, body in contents.items():
                staged = output / name
                self.assertEqual(staged.read_bytes(), body)
                self.assertEqual(subprocess.check_output(
                    ["git", "hash-object", "--no-filters", str(staged)],
                    text=True).strip(), blobs[name])
            self.assertEqual((output / "data/boards/test.board").read_bytes(),
                             contents["data/boards/test.board"])
            self.assertEqual((output / "data/mekfiles/sub/unit.blk").read_bytes(),
                             contents["data/mekfiles/sub/unit.blk"])
            self.assertFalse((output / "data/boards/unknown.board").exists())
            self.assertFalse((output / "data/rat/rogue.txt").exists())
            self.assertFalse((output / "data/mekfiles/sub/rogue.blk").exists())
            for path, expected in (
                ("data/mekfiles/unit_files.zip", {"sub/unit.blk": contents[
                    "data/mekfiles/sub/unit.blk"]}),
                ("data/rat/rat_default.zip", {"default.txt": contents["data/rat/default.txt"]}),
                ("data/universe/planetary_systems/canon_systems.zip",
                 {"sol.xml": contents["data/universe/planetary_systems/canon_systems/sol.xml"]}),
                ("data/universe/planetary_systems/connector_systems.zip",
                 {"jump.xml": contents["data/universe/planetary_systems/connector_systems/jump.xml"]}),
            ):
                with zipfile.ZipFile(output / "generated" / path) as archive:
                    self.assertEqual({name: archive.read(name) for name in archive.namelist()}, expected)
                verify_zip_data(output / "generated" / path, GENERATED[path], blobs)
            self.assertEqual({name: (repo / name).read_bytes() for name in contents}, before)
            self.assertEqual(subprocess.check_output(
                ["git", "-C", str(repo), "status", "--porcelain", "--untracked-files=no"]), b"")


if __name__ == "__main__":
    unittest.main()
