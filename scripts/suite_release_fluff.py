#!/usr/bin/env python3
"""Unit fluff download: attach mm-data's ``unitfluff`` folder to the published MegaMek release.

Unit fluff text lives in mm-data's top-level ``unitfluff`` folder, outside ``data``, so the
product archives never carry it. After publication this step zips that folder from the suite
record's pinned mm-data commit, laid out as ``data/unitfluff/<uuid>.yml`` for extraction into a
user directory or the install's ``userdata`` folder, and uploads it to the MegaMek release as
``unit-fluff-<version>.zip``. The launcher only installs ``<Product>-<version>.tar.gz`` and reads
``suite-record-*.json``, so it ignores this asset; the suite record does not list it.

The step never changes the products or the record and is idempotent: an existing upload is kept,
so rerunning (also on a no-op) repairs a missed upload. Suites from before the split have no
``unitfluff`` folder and are skipped. ``--dry-run`` builds the zip, from this run's frozen
mm-data commit when it has something to publish, and writes nothing.
"""

import argparse
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

from suite_release_plan import (
    CHANNELS, UnsafeInventory, complete_records, download_asset, failure_detail, gh_get, latest,
    parse_json, plan, release_details, valid_sha,
)
from suite_release_publish import GitHubWrites, sha_size

FOLDER = "unitfluff"
PREFIX = "data/"


def asset_name(version):
    return f"unit-fluff-{version}.zip"


def git(sources, args, runner):
    # Fixed line endings, so the zip only depends on the commit and not on the machine.
    command = ["git", "-C", str(Path(sources) / "mm-data"), "-c", "core.autocrlf=false",
               "-c", "core.eol=lf", *args]
    try:
        result = runner(command, capture_output=True, check=True)
    except (OSError, subprocess.CalledProcessError) as error:
        raise UnsafeInventory(f"git {args[0]} failed in mm-data: {failure_detail(error)}") from None
    return result.stdout.decode("utf-8", errors="replace")


def has_fluff(sources, commit, runner):
    """Whether the mm-data ``commit``, which must be in the local clone, has a unitfluff tree."""
    if not valid_sha(commit):
        raise UnsafeInventory("invalid mm-data commit")
    try:
        git(sources, ["cat-file", "-e", f"{commit}^{{commit}}"], runner)
    except UnsafeInventory:
        raise UnsafeInventory(f"mm-data commit {commit} is not in {sources}/mm-data") from None
    return bool(git(sources, ["ls-tree", "-d", commit, FOLDER], runner).strip())


def checked(path):
    """Number of files in the zip; everything must sit under data/unitfluff/."""
    root = f"{PREFIX}{FOLDER}/"
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
    files = [name for name in names if not name.endswith("/")]
    if not files or any(not name.startswith(root) and not root.startswith(name)
                        for name in names):
        raise UnsafeInventory("unexpected unit fluff zip layout")
    return len(files)


def build(sources, commit, version, directory, runner):
    """Zip unitfluff at exactly ``commit``; git archive is deterministic for a commit."""
    path = (Path(directory) / asset_name(version)).resolve()
    git(sources, ["archive", "--format=zip", f"--prefix={PREFIX}", "-o", str(path), commit,
                  FOLDER], runner)
    return path, checked(path)


def host_release(record, details):
    """The published MegaMek release named by ``record``."""
    item = record["products"]["MegaMek"]
    matches = [r for r in details if r.get("id") == item["releaseId"]
               and r.get("tag_name") == record["tag"] and r.get("draft") is False]
    if len(matches) != 1 or not isinstance(matches[0].get("assets"), list):
        raise UnsafeInventory(f"MegaMek release of {record['version']} not found")
    return matches[0]


def uploaded(release, name):
    """The complete asset called ``name``, or None; a broken upload needs an operator."""
    matches = [a for a in release["assets"] if isinstance(a, dict) and a.get("name") == name]
    if not matches:
        return None
    asset = matches[0]
    if (len(matches) != 1 or asset.get("state") != "uploaded"
            or type(asset.get("size")) is not int or asset["size"] <= 0):
        raise UnsafeInventory(f"{name} exists but is not a complete upload; delete it from the "
                              "MegaMek release and rerun")
    return asset


def frozen(state):
    """This run's plan when it has something to publish; None for a no-op or without state."""
    path = None if state is None else Path(state) / "inventory.json"
    if path is None or not path.is_file():
        return None
    proposal = plan(parse_json(path.read_bytes()))
    return None if proposal["noop"] else proposal


def attach(channel, sources, dry_run, state=None, *, getter=gh_get, fetch=download_asset,
           writes=None, runner=subprocess.run):
    """Upload the unit fluff zip of the latest ``channel`` suite unless it is already there."""
    if channel not in CHANNELS:
        raise UnsafeInventory("unknown channel")
    if not dry_run and writes is None:
        raise UnsafeInventory("missing writes")
    proposal = frozen(state)
    if proposal is not None and proposal["channel"] != channel:
        raise UnsafeInventory("state belongs to another channel")
    if dry_run and proposal is not None:
        # Not published yet: rehearse with the mm-data commit this run froze.
        record, release = None, None
        version = proposal["versionCandidate"]
        commit = proposal["frozenCommits"]["mm-data"]
    else:
        details = release_details(getter)["megamek"]
        record = latest(complete_records(details, fetch), channel)
        if proposal is not None and (record is None
                                     or record["version"] != proposal["versionCandidate"]):
            raise UnsafeInventory(f"latest {channel} record is not this run's "
                                  f"{proposal['versionCandidate']}; nothing attached")
        if record is None:
            return [f"No published {channel} suite yet; nothing to attach."]
        version, commit = record["version"], record["mmData"]["commit"]
        release = host_release(record, details)
    name = asset_name(version)
    if release is not None and (asset := uploaded(release, name)) is not None:
        return [f"`{name}` is already attached to the MegaMek {version} release "
                f"({asset['size']} bytes); nothing to do."]
    if not has_fluff(sources, commit, runner):
        return [f"mm-data `{commit[:10]}` of {version} has no `{FOLDER}` folder; "
                "no unit fluff download."]
    with tempfile.TemporaryDirectory(prefix="suite-fluff-") as temporary:
        path, files = build(sources, commit, version, temporary, runner)
        digest, size = sha_size(path)
        built = (f"`{name}` ({files} files, {size} bytes, SHA-256 `{digest}`) "
                 f"from mm-data `{commit[:10]}`")
        if dry_run:
            when = " once it is published" if release is None else ""
            return [f"Would attach {built} to the MegaMek {version} release{when}."]
        asset_id = writes.upload("megamek", release["id"], path)
        current = host_release(record, release_details(getter)["megamek"])
        if len([a for a in current["assets"] if isinstance(a, dict) and a.get("id") == asset_id
                and a.get("name") == name and a.get("size") == size
                and a.get("state") == "uploaded"]) != 1:
            raise UnsafeInventory(f"{name} upload not confirmed; check the MegaMek release")
        downloaded = Path(temporary) / "downloaded.zip"
        fetch("megamek", asset_id, downloaded)
        if sha_size(downloaded) != (digest, size):
            raise UnsafeInventory(f"{name} bytes not confirmed; delete the asset and rerun")
    return [f"Attached {built} to the MegaMek {version} release."]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--channel", choices=CHANNELS, required=True)
    parser.add_argument("--sources", type=Path, required=True,
                        help="directory holding the full mm-data clone")
    parser.add_argument("--state", type=Path, help="the run's suite-state directory, if any")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    try:
        writes = None if args.dry_run else GitHubWrites()
        report = attach(args.channel, args.sources, args.dry_run, args.state, writes=writes)
        print("### Unit fluff download\n")
        for line in report:
            print(line)
        return 0
    except (UnsafeInventory, ValueError, OSError, KeyError, TypeError) as error:
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
