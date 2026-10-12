"""Attestation of downloaded suite archives. Never publishes or extracts tar files."""

import os
import subprocess
import sys
from pathlib import Path

from suite_release_plan import REPOS, UnsafeInventory, failure_detail, validate_previous, valid_sha
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "gradle"))
import suite_archive_verifier as shared

ROOT = Path(__file__).resolve().parents[1].parent
WORKTREES = {name: ROOT / repo for name, repo in REPOS.items()}
def archive_identity(path, product, version, *, portable=True):
    """Use the canonical bounded scanner before invoking the Gradle adapter."""
    try:
        props, files, _, lease = shared.scan(path, product, version, portable=portable)
        lease.cleanup()
        jars = {name: bytes.fromhex(digest[0]) for name, digest in files.items()
                if name in {jar for product_name in ("MegaMek", "MegaMekLab", "MekHQ")
                            for jar in (product_name + ".jar", "lib/" + product_name + ".jar")}}
        return props, jars
    except (shared.VerificationError, EOFError, UnicodeError, OSError, ValueError) as error:
        raise UnsafeInventory(f"unreadable {product} archive: {error}") from error


def attest_archives(record, paths, runner=subprocess.run, worktrees=WORKTREES, *, portable=True):
    """Verify record closure and invoke each external verifier on retained asset bytes.

    worktrees must contain the approved verifier code and pinned mm-data checkout.
    This function does not check out sources, build archives or write remotely.
    """
    validate_previous(record)
    if set(paths) != set(REPOS) or set(worktrees) != set(REPOS):
        raise UnsafeInventory("missing archive or verifier worktree")
    items = record["products"]
    meta = {}
    jars = {}
    for product in REPOS:
        item = items[product]
        path = Path(paths[product])
        if path.name != item["asset"]["name"] or not path.is_file():
            raise UnsafeInventory(f"wrong {product} archive path")
        props, jars[product] = archive_identity(
            path, product, item["version"], portable=portable)
        own = {"MegaMek": "megamekCommit", "MegaMekLab": "megameklabCommit",
               "MekHQ": "mekhqCommit"}[product]
        if (props["schemaVersion"] != "1" or props["product"] != product
                or props["version"] != item["version"]
                or props[own] != item["commit"]
                or props["mmDataCommit"] != record["mmData"]["commit"]
                or props["minimumJavaVersion"] != "21"
                or any(not valid_sha(props[key]) for key in
                       ("megamekCommit", "megameklabCommit", "mekhqCommit", "mmDataCommit"))):
            raise UnsafeInventory(f"{product} archive identity differs from record")
        meta[product] = props
    mm, lab, hq = (meta[name] for name in REPOS)
    if (lab["megamekCommit"] != mm["megamekCommit"]
            or lab["megamekVersion"] != mm["version"]
            or hq["megamekCommit"] != mm["megamekCommit"]
            or hq["megamekVersion"] != mm["version"]
            or hq["megameklabCommit"] != lab["megameklabCommit"]
            or hq["megameklabVersion"] != lab["version"]):
        raise UnsafeInventory("archive dependency closure differs from companions")
    for product in REPOS:
        own_jar = product + ".jar"
        if (own_jar not in jars[product] or "lib/" + own_jar not in jars[product]
                or jars[product][own_jar] != jars[product]["lib/" + own_jar]):
            raise UnsafeInventory(f"{product} missing or divergent launcher jar: {own_jar}")
    for product, companion in (("MegaMekLab", "MegaMek"),
                               ("MekHQ", "MegaMek"), ("MekHQ", "MegaMekLab")):
        jar = companion + ".jar"
        if ("lib/" + jar not in jars[product]
                or jars[product]["lib/" + jar] != jars[companion][jar]):
            raise UnsafeInventory(f"{product} missing or divergent companion jar: {jar}")
    # HQ ships predecessor launchers at root as well; Lab ships MegaMek only in lib.
    for jar in ("MegaMek.jar", "MegaMekLab.jar"):
        if (jar not in jars["MekHQ"]
                or jars["MekHQ"][jar] != jars["MekHQ"]["lib/" + jar]):
            raise UnsafeInventory(f"MekHQ missing or divergent launcher jar: {jar}")
    for product, task in (("MegaMek", ":megamek:verifySuiteMegaMekArchive"),
                          ("MegaMekLab", ":megameklab:verifySuiteArchive"),
                          ("MekHQ", ":MekHQ:verifySuiteMekHQArchive")):
        own_meta = meta[product]
        flags = {
            "suiteReleaseVersion": record["version"],
            "suiteMegaMekVersion": mm["version"],
            "suiteMegaMekLabVersion": lab["version"],
            "suiteMekHQVersion": hq["version"],
            "suiteMegaMekCommit": own_meta["megamekCommit"],
            "suiteMegaMekLabCommit": own_meta["megameklabCommit"],
            "suiteMekHQCommit": own_meta["mekhqCommit"],
            "suiteMmDataCommit": own_meta["mmDataCommit"],
            "suiteArchiveFile": str(Path(paths[product]).resolve()),
        }
        if product != "MegaMek":
            flags["suiteMegaMekArchiveFile"] = str(Path(paths["MegaMek"]).resolve())
        if product == "MekHQ":
            flags["suiteMegaMekLabArchiveFile"] = str(Path(paths["MegaMekLab"]).resolve())
        directory = Path(worktrees[product])
        if not (directory / "gradlew").is_file() and not (directory / "gradlew.bat").is_file():
            raise UnsafeInventory(f"missing {product} verifier worktree")
        executable = directory / ("gradlew.bat" if os.name == "nt" else "gradlew")
        command = [str(executable), task, "--no-daemon"]
        command += [f"-P{key}={value}" for key, value in flags.items()]
        try:
            runner(command, cwd=directory, check=True, capture_output=True, text=True)
        except (OSError, subprocess.CalledProcessError) as error:
            raise UnsafeInventory(
                f"{product} external archive verifier failed: "
                f"{failure_detail(error)}") from None
