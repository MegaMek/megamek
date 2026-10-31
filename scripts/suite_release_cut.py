#!/usr/bin/env python3
"""Core cut: branch the four repositories for a new Core line, then move main to the next line.

``branches`` creates ``releases/<line>`` from the current ``main`` heads of megamek, megameklab,
mekhq and mm-data. ``bump`` runs after the first Core release of that line is published and
moves MegaMek's ``Version.properties`` on ``main`` to the next patch line. Both steps are
idempotent so a failed cut can simply be rerun. ``--dry-run`` only reports what would happen.
"""

import argparse
import base64
import re
import sys

from suite_release_plan import (
    SOURCES, VERSION_PROPERTIES, UnsafeInventory, branch_head, canonical_line, complete_records,
    download_asset, freeze, gh_get, numbers, padded, properties_line, release_branches,
    release_details,
)
from suite_release_publish import api

PATCH = re.compile(r"^(\s*patch\s*=\s*)([0-9]+)(\s*)$", re.M)


class CutWrites:
    def branch(self, repo, name, sha):
        result = api(["--method", "POST", f"repos/MegaMek/{repo}/git/refs"],
                     payload={"ref": f"refs/heads/{name}", "sha": sha})
        if result.get("ref") != f"refs/heads/{name}" or result.get("object", {}).get("sha") != sha:
            raise UnsafeInventory("branch creation response mismatch")

    def version_file(self, text, blob, message):
        result = api(["--method", "PUT", f"repos/MegaMek/megamek/contents/{VERSION_PROPERTIES}"],
                     payload={"message": message, "branch": "main", "sha": blob,
                              "content": base64.b64encode(text.encode("utf-8")).decode("ascii")})
        if result.get("content", {}).get("path") != VERSION_PROPERTIES:
            raise UnsafeInventory("Version.properties update response mismatch")
        return result.get("commit", {}).get("sha")


def require_line(line):
    if canonical_line(line) is None:
        raise UnsafeInventory(f"line must look like 0.51.02, not {line}")
    return line


def main_version(getter):
    """(line, text, blob SHA) of Version.properties at the current megamek main head."""
    sha = branch_head("megamek", "main", getter)
    data = getter(f"repos/MegaMek/megamek/contents/{VERSION_PROPERTIES}?ref={sha}")
    if (not isinstance(data, dict) or data.get("encoding") != "base64"
            or not isinstance(data.get("content"), str) or not isinstance(data.get("sha"), str)):
        raise UnsafeInventory("unreadable Version.properties")
    text = base64.b64decode(data["content"]).decode("utf-8")
    return properties_line(text), text, data["sha"]


def core_versions(line, getter, fetch):
    """Versions of the published Core records on ``line``, oldest first."""
    records = complete_records(release_details(getter)["megamek"], fetch)
    return [r["version"] for r in records
            if r["channel"] == "core" and numbers(r["version"])[:3] == numbers(line)[:3]]


def branches(line, dry_run, *, getter=gh_get, fetch=download_asset, writes=None):
    """Create releases/<line> from main in all four repositories; existing branches are kept."""
    require_line(line)
    name = f"releases/{line}"
    current, _, _ = main_version(getter)
    existing = {repo: name in release_branches(repo, getter) for repo in SOURCES}
    if current != line:
        # Only a rerun of a cut that already published Core on this line may find main moved on.
        if not all(existing.values()) or not core_versions(line, getter, fetch):
            raise UnsafeInventory(f"main is on line {current}, not {line}; check the line to cut")
    newest = release_branches("megamek", getter)
    if newest and numbers(newest[-1].removeprefix("releases/")) > numbers(line):
        raise UnsafeInventory(f"{newest[-1]} is newer than {name}")
    heads = freeze("main", getter)
    if not all(existing.values()):
        # Finishing a partial cut: every kept branch must still match main, or the four branches
        # would come from different snapshots.
        moved = [repo for repo in SOURCES
                 if existing[repo] and branch_head(repo, name, getter) != heads[repo]]
        if moved:
            raise UnsafeInventory(
                f"{name} exists in {', '.join(moved)} at a commit that is no longer main; "
                f"delete the partial {name} branches and rerun the cut")
    report = []
    for repo in SOURCES:
        if existing[repo]:
            report.append(f"{repo}: {name} already exists at "
                          f"{branch_head(repo, name, getter)[:10]}; kept")
        elif dry_run:
            report.append(f"{repo}: would create {name} at {heads[repo][:10]}")
        else:
            writes.branch(repo, name, heads[repo])
            if branch_head(repo, name, getter) != heads[repo]:
                raise UnsafeInventory(f"created branch mismatch: {repo}")
            report.append(f"{repo}: created {name} at {heads[repo][:10]}")
    return report


def bumped(text):
    match = PATCH.search(text)
    if match is None or len(PATCH.findall(text)) != 1:
        raise UnsafeInventory("Version.properties must contain exactly one patch entry")
    return PATCH.sub(lambda m: f"{m[1]}{int(m[2]) + 1:02d}{m[3]}", text, count=1)


def bump(line, dry_run, *, getter=gh_get, fetch=download_asset, writes=None):
    """Move main from ``line`` to the next line once ``line`` has a Core release."""
    require_line(line)
    current, text, blob = main_version(getter)
    following = padded((*numbers(line)[:2], numbers(line)[2] + 1))
    if numbers(current) > numbers(line):
        return [f"main is already on line {current}; nothing to do"]
    if current != line:
        raise UnsafeInventory(f"main is on line {current}, older than {line}")
    core = core_versions(line, getter, fetch)
    if not core:
        if dry_run:
            return [f"would move main from {line} to {following} after Core {line} publishes"]
        raise UnsafeInventory(f"no Core release on line {line} yet; main stays on {line}")
    new_text = bumped(text)
    if properties_line(new_text) != following:
        raise UnsafeInventory("unexpected Version.properties after bump")
    if dry_run:
        return [f"would move main from {line} to {following}"]
    commit = writes.version_file(new_text, blob, f"Start {following} after Core {core[-1]}")
    if main_version(getter)[0] != following:
        raise UnsafeInventory("main did not move to the next line")
    return [f"moved main from {line} to {following} ({(commit or '')[:10]})"]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("step", choices=("branches", "bump"))
    parser.add_argument("--line", required=True, help="the line to cut, for example 0.51.02")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    try:
        writes = None if args.dry_run else CutWrites()
        step = branches if args.step == "branches" else bump
        for line in step(args.line, args.dry_run, writes=writes):
            print(line)
        return 0
    except (UnsafeInventory, ValueError, OSError, KeyError, TypeError) as error:
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
