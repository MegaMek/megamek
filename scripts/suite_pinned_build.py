#!/usr/bin/env python3
"""Nonpublishing build of a frozen suite inventory. No floating refs."""

import argparse
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from suite_archive_attestation import archive_identity
from suite_release_plan import (REPOS, SOURCES, UnsafeInventory, branch_line, failure_detail,
                                parse_json, plan)

TASKS = {
    "MegaMek": (":megamek:test", ":megamek:buildMegaMekPackage",
                ":megamek:verifySuiteMegaMekArchive"),
    "MegaMekLab": (":megameklab:test", ":megameklab:distTar",
                   ":megameklab:verifySuiteArchive"),
    "MekHQ": (":MekHQ:test", ":MekHQ:distTar", ":MekHQ:verifySuiteMekHQArchive"),
}
# Gradle project names are case-sensitive and need not match Git checkout names.
PROJECT_DIRS = {"MegaMek": "megamek", "MegaMekLab": "megameklab", "MekHQ": "MekHQ"}


def run(command, runner=subprocess.run, **kwargs):
    try:
        return runner(command, check=True, capture_output=True, text=True, **kwargs).stdout.strip()
    except (OSError, subprocess.CalledProcessError) as error:
        raise UnsafeInventory(f"local command failed: {failure_detail(error)}") from None


def official(repo):
    return f"https://github.com/MegaMek/{repo}.git"


def inspect_source(path, repo, sha, runner, branch="main"):
    if not (path / ".git").exists():
        raise UnsafeInventory(f"missing Git checkout: {repo}")
    if branch != "main" and branch_line(branch) is None:
        raise UnsafeInventory(f"unexpected source branch: {branch}")
    remote = run(["git", "-C", str(path), "remote", "get-url", "origin"], runner)
    if remote not in (official(repo), official(repo)[:-4],
                      f"git@github.com:MegaMek/{repo}.git"):
        raise UnsafeInventory(f"nonofficial origin: {repo}")
    # The local remote-tracking branch is a provenance check, not a build input or checkout target.
    run(["git", "-C", str(path), "merge-base", "--is-ancestor", sha,
         f"refs/remotes/origin/{branch}"], runner)
    if run(["git", "-C", str(path), "rev-parse", f"{sha}^{{commit}}"], runner) != sha:
        raise UnsafeInventory(f"unavailable frozen commit: {repo}")


def clean(path, sha, runner):
    if run(["git", "-C", str(path), "rev-parse", "HEAD"], runner) != sha:
        raise UnsafeInventory(f"staged HEAD mismatch: {path.name}")
    if run(["git", "-C", str(path), "status", "--porcelain=v1",
            "--untracked-files=all"], runner):
        raise UnsafeInventory(f"dirty staged checkout: {path.name}")


def stage(sources, commits, parent, runner, *, branch="main"):
    for repo in SOURCES:
        inspect_source(Path(sources) / repo, repo, commits[repo], runner, branch)
    for repo in SOURCES:
        dest = parent / repo
        # No fetch, branch checkout, or network operation. Clone copies only Git objects.
        run(["git", "clone", "--no-local", "--no-checkout", "--origin", "origin",
             str(Path(sources) / repo), str(dest)], runner)
        run(["git", "-C", str(dest), "remote", "set-url", "origin", official(repo)], runner)
        run(["git", "-C", str(dest), "checkout", "--detach", commits[repo]], runner)
        if run(["git", "-C", str(dest), "remote", "get-url", "origin"], runner) != official(repo):
            raise UnsafeInventory(f"staged remote mismatch: {repo}")
        clean(dest, commits[repo], runner)
    return {product: parent / repo for product, repo in REPOS.items()}


def flags(version, versions, commits, archive=None, companions=None):
    result = {
        "suiteReleaseVersion": version,
        "suiteMegaMekVersion": versions["MegaMek"],
        "suiteMegaMekLabVersion": versions["MegaMekLab"],
        "suiteMekHQVersion": versions["MekHQ"],
        "suiteMegaMekCommit": commits["megamek"],
        "suiteMegaMekLabCommit": commits["megameklab"],
        "suiteMekHQCommit": commits["mekhq"],
        "suiteMmDataCommit": commits["mm-data"],
    }
    if archive is not None:
        result["suiteArchiveFile"] = str(archive)
    for name, path in (companions or {}).items():
        result["suite" + name + "ArchiveFile"] = str(path)
    return [f"-P{k}={v}" for k, v in result.items()]


def gradle(product, task, worktrees, properties, runner):
    directory = worktrees[product]
    executable = directory / ("gradlew.bat" if os.name == "nt" else "gradlew")
    if not executable.is_file():
        raise UnsafeInventory(f"missing pinned Gradle wrapper: {product}")
    try:
        run([str(executable), task, "--no-daemon", *properties],
            runner, cwd=directory)
    except UnsafeInventory as error:
        raise UnsafeInventory(f"{product} {task} failed: {error}") from None


def build(inventory, sources, output, *, runner=subprocess.run):
    """Build, test and verify all three products from the frozen commits."""
    proposal = plan(inventory)
    if proposal["noop"]:
        raise UnsafeInventory("nothing changed since the previous release; nothing to build")
    if output.exists():
        raise UnsafeInventory("output directory must not exist")
    commits = proposal["frozenCommits"]
    version = proposal["versionCandidate"]
    versions = {product: version for product in REPOS}
    paths = {}
    with tempfile.TemporaryDirectory(prefix="suite-pinned-") as temporary:
        root = Path(temporary)
        worktrees = stage(sources, commits, root, runner, branch=inventory["branch"])
        for product, repo in REPOS.items():
            for source in SOURCES:
                clean(root / source, commits[source], runner)
            # Downstream producers consume the exact bytes built earlier in this run.
            companions = dict(paths)
            common = flags(version, versions, commits, companions=companions)
            gradle(product, TASKS[product][0], worktrees, common, runner)
            gradle(product, TASKS[product][1], worktrees, common, runner)
            archive = root / repo / PROJECT_DIRS[product] / "build" / "distributions" / (
                f"{product}-{version}.tar.gz")
            if not archive.is_file() or archive.is_symlink() or archive.stat().st_size == 0:
                raise UnsafeInventory(f"missing produced archive: {product}")
            gradle(product, TASKS[product][2], worktrees,
                   flags(version, versions, commits, archive.resolve(), companions), runner)
            metadata, jars = archive_identity(archive, product, version)
            own = {"MegaMek": "megamekCommit", "MegaMekLab": "megameklabCommit",
                   "MekHQ": "mekhqCommit"}[product]
            if (metadata.get("schemaVersion") != "1"
                    or metadata.get("product") != product
                    or metadata.get("version") != version
                    or metadata.get(own) != commits[repo]
                    or metadata.get("mmDataCommit") != commits["mm-data"]
                    or metadata.get("minimumJavaVersion") != "21"):
                raise UnsafeInventory(f"built archive identity mismatch: {product}")
            for companion, path in companions.items():
                companion_meta, companion_jars = archive_identity(path, companion, version)
                key = {"MegaMek": "megamekCommit", "MegaMekLab": "megameklabCommit"}[companion]
                if (metadata.get(key) != companion_meta[key]
                        or metadata.get("megamekVersion" if companion == "MegaMek"
                                        else "megameklabVersion") != version
                        or jars.get("lib/" + companion + ".jar") != companion_jars.get(companion + ".jar")):
                    raise UnsafeInventory(f"built archive companion mismatch: {product}")
            paths[product] = archive.resolve()
        for source in SOURCES:
            clean(root / source, commits[source], runner)
        output.mkdir(parents=True)
        results = {}
        for product in REPOS:
            destination = output / f"{product}-{version}.tar.gz"
            shutil.copyfile(paths[product], destination)
            results[product] = str(destination.resolve())
        return {"status": "OFFLINE_NONPUBLISHING", "versionCandidate": version,
                "productVersions": versions, "archives": results}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--sources", type=Path, required=True,
                        help="directory of four local official Git mirrors")
    parser.add_argument("--output", type=Path, required=True,
                        help="new directory for the three resulting archives")
    args = parser.parse_args()
    try:
        inventory = parse_json(args.inventory.read_bytes())
        print(json.dumps(build(inventory, args.sources, args.output), indent=2))
        return 0
    except (UnsafeInventory, ValueError, OSError) as error:
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())