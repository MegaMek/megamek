#!/usr/bin/env python3
"""Manual suite release driver. Prepare with read token; finish with scoped App token."""

import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

from suite_release_plan import (
    REPOS, UnsafeInventory, check_product_assets, discovered_inventory,
    download_asset, freeze_and_inventory, gh_get, parse_json, plan, release_details,
)
from suite_pinned_build import build
from suite_pinned_build import clean, stage
from suite_archive_attestation import attest_archives
from suite_release_publish import GitHubWrites, exact_asset, publish_approved


def digest(path):
    result = hashlib.sha256()
    with Path(path).open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            result.update(chunk)
    return result.hexdigest()


def save(path, value):
    with Path(path).open("x", encoding="utf-8") as output:
        json.dump(value, output, indent=2)
        output.write("\n")


def prepare(membership, bootstrap, floor, sources, state,
            *, discover=discovered_inventory, builder=build):
    if state.exists():
        raise UnsafeInventory("state directory already exists")
    state.mkdir()
    prior_dir = state / "prior"
    def attest_previous(record, paths):
        commits = {repo: record["products"][product]["commit"]
                   for product, repo in REPOS.items()}
        commits["mm-data"] = record["mmData"]["commit"]
        with tempfile.TemporaryDirectory(prefix="suite-prior-verifiers-") as temp:
            worktrees = stage(sources, commits, Path(temp), subprocess.run)
            attest_archives(record, paths, worktrees=worktrees)
            for repo in commits:
                clean(Path(temp) / repo, commits[repo], subprocess.run)

    inventory = discover(membership, bootstrap=bootstrap, bootstrap_floor=floor,
                         retain=prior_dir, attest=attest_previous)
    proposal = plan(inventory)
    save(state / "inventory.json", inventory)
    prior = ({product: prior_dir / inventory["previous"]["products"][product]["asset"]["name"]
              for product in REPOS} if inventory["previous"] else {})
    result = builder(inventory, sources, prior, state / "archives", bootstrap=bootstrap)
    save(state / "build_result.json", result)
    # Bind both immutable inputs and resulting archive bytes for finish.
    save(state / "seal.json", {
        "inventory": digest(state / "inventory.json"),
        "build": digest(state / "build_result.json"),
        "archives": {p: digest(result["archives"][p]) for p in REPOS},
    })
    return proposal


def verify_noop(inventory, paths, *, getter=gh_get, fetch=download_asset):
    previous = inventory["previous"]
    if not plan(inventory)["weeklyNoopCandidate"] or previous is None:
        raise UnsafeInventory("not a verified Weekly no-op candidate")
    fresh = freeze_and_inventory(inventory["membership"], inventory["floor"], getter)
    if any(fresh[key] != inventory[key] for key in ("commits", "tags", "releases")):
        raise UnsafeInventory("remote inventory moved after build")
    details = release_details(getter)
    from suite_release_plan import complete_record
    if complete_record(details["megamek"], fetch) != previous:
        raise UnsafeInventory("latest complete record moved after build")
    check_product_assets(previous, details, fetch)
    import tempfile
    with tempfile.TemporaryDirectory() as directory:
        for product, repo in REPOS.items():
            exact_asset(previous["products"][product], details[repo], repo,
                        fetch, Path(directory), getter)
    for product in REPOS:
        item = previous["products"][product]["asset"]
        if digest(paths[product]) != item["sha256"]:
            raise UnsafeInventory("local reused archive moved after build")
    return previous


def finish(state, sources, bootstrap, publish, *, publisher=publish_approved,
           noop=verify_noop):
    inventory_path = state / "inventory.json"
    build_path = state / "build_result.json"
    seal = parse_json((state / "seal.json").read_bytes())
    if digest(inventory_path) != seal["inventory"] or digest(build_path) != seal["build"]:
        raise UnsafeInventory("sealed inventory or build result changed")
    inventory = parse_json(inventory_path.read_bytes())
    result = parse_json(build_path.read_bytes())
    if bootstrap != (inventory["previous"] is None):
        raise UnsafeInventory("bootstrap intent differs from prepared inventory")
    if set(result.get("archives", {})) != set(REPOS) or set(seal["archives"]) != set(REPOS):
        raise UnsafeInventory("missing sealed archives")
    for product in REPOS:
        path = Path(result["archives"][product])
        if not path.is_file() or digest(path) != seal["archives"][product]:
            raise UnsafeInventory("sealed archive changed")
    proposal = plan(inventory)
    if proposal["weeklyNoopCandidate"]:
        noop(inventory, result["archives"])
        return ("DRY_RUN_VERIFIED_WEEKLY_NOOP" if not publish else
                "VERIFIED_WEEKLY_NOOP") + " (no new record or writes)"
    if not publish:
        # A dry run never calls the publisher, even if a write token is present.
        return "DRY_RUN_ONLY (pinned archives built; no release or record published)"
    publisher(inventory, result, sources, bootstrap=bootstrap, writes=GitHubWrites())
    return "PUBLISHED_COMPLETE_RECORD"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("prepare", "finish"))
    parser.add_argument("--membership", choices=("weekly", "milestone", "development"))
    parser.add_argument("--bootstrap", action="store_true")
    parser.add_argument("--bootstrap-floor")
    parser.add_argument("--sources", type=Path, required=True)
    parser.add_argument("--state", type=Path, required=True)
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    try:
        if args.phase == "prepare":
            if args.publish or args.membership is None:
                raise UnsafeInventory("prepare requires membership and forbids publish")
            proposal = prepare(args.membership, args.bootstrap, args.bootstrap_floor,
                               args.sources, args.state)
            print("PREPARED: " + json.dumps(proposal))
        else:
            if args.membership or args.bootstrap_floor:
                raise UnsafeInventory("finish reads sealed membership and floor")
            print(finish(args.state, args.sources, args.bootstrap, args.publish))
        return 0
    except (UnsafeInventory, ValueError, OSError, KeyError, TypeError) as error:
        print(f"BLOCKED: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
