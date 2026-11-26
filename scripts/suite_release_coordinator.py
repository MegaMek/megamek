#!/usr/bin/env python3
"""Suite release driver. Prepare with a read token; finish with the scoped App token."""

import argparse
import hashlib
import json
import os
import sys
from pathlib import Path

from suite_release_plan import CHANNELS, REPOS, UnsafeInventory, discovered_inventory, parse_json, plan
from suite_pinned_build import build
from suite_release_publish import GitHubWrites, publish_approved


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


def prepare(channel, branch, rehearsal, sources, state, *, discover=discovered_inventory,
            builder=build):
    """Freeze, plan and (unless nothing changed) build. Never writes remotely."""
    if state.exists():
        raise UnsafeInventory("state directory already exists")
    state.mkdir()
    inventory = discover(channel, branch, rehearsal)
    proposal = plan(inventory)
    save(state / "inventory.json", inventory)
    if proposal["noop"]:
        save(state / "seal.json", {"inventory": digest(state / "inventory.json")})
        return proposal
    result = builder(inventory, sources, state / "archives")
    save(state / "build_result.json", result)
    # Bind both immutable inputs and resulting archive bytes for finish.
    save(state / "seal.json", {
        "inventory": digest(state / "inventory.json"),
        "build": digest(state / "build_result.json"),
        "archives": {p: digest(result["archives"][p]) for p in REPOS},
    })
    return proposal


def step_summary(lines):
    """Append Markdown to the GitHub job summary; silent outside Actions."""
    target = os.environ.get("GITHUB_STEP_SUMMARY")
    if target:
        with Path(target).open("a", encoding="utf-8") as output:
            output.write("\n".join(lines) + "\n\n")


def step_outputs(values):
    """Expose values to later workflow steps; silent outside Actions."""
    target = os.environ.get("GITHUB_OUTPUT")
    if target:
        with Path(target).open("a", encoding="utf-8") as output:
            for key, value in values.items():
                output.write(f"{key}={value}\n")


def prepared_summary(proposal, state):
    title = "Core" if proposal["channel"] == "core" else "Experimental"
    if proposal["rehearsal"]:
        title += " cut rehearsal"
    commits = proposal["frozenCommits"]
    lines = [f"## {title} {proposal['versionCandidate']} from `{proposal['branch']}`", ""]
    if proposal["noop"]:
        lines.append(f"Nothing changed since {proposal['previousTag']}; nothing was built "
                     "and nothing will be published.")
    else:
        seal = parse_json((state / "seal.json").read_bytes())
        lines += ["| Product | SHA-256 |", "| --- | --- |"]
        lines += [f"| {product} | `{seal['archives'][product]}` |" for product in REPOS]
        lines += ["", "Previous release in this channel: "
                  + (proposal["previousTag"] or "none")]
    lines += ["", "Frozen sources: " + ", ".join(
        f"[{repo}@{sha[:10]}](https://github.com/MegaMek/{repo}/commit/{sha})"
        for repo, sha in commits.items())]
    return lines


def finish(state, sources, publish, *, publisher=publish_approved):
    inventory_path = state / "inventory.json"
    seal = parse_json((state / "seal.json").read_bytes())
    if digest(inventory_path) != seal["inventory"]:
        raise UnsafeInventory("sealed inventory changed")
    inventory = parse_json(inventory_path.read_bytes())
    proposal = plan(inventory)
    if proposal["noop"]:
        return f"NOOP (nothing changed since {proposal['previousTag']}; nothing published)"
    build_path = state / "build_result.json"
    if digest(build_path) != seal["build"]:
        raise UnsafeInventory("sealed build result changed")
    result = parse_json(build_path.read_bytes())
    if set(result.get("archives", {})) != set(REPOS) or set(seal["archives"]) != set(REPOS):
        raise UnsafeInventory("missing sealed archives")
    for product in REPOS:
        path = Path(result["archives"][product])
        if not path.is_file() or digest(path) != seal["archives"][product]:
            raise UnsafeInventory("sealed archive changed")
    if not publish:
        # A dry run never calls the publisher, even if a write token is present.
        return "DRY_RUN_ONLY (archives built and verified; nothing published)"
    if proposal["rehearsal"]:
        raise UnsafeInventory("a cut rehearsal never publishes")
    publisher(inventory, result, sources, writes=GitHubWrites())
    return f"PUBLISHED {proposal['channel']} {proposal['versionCandidate']}"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("phase", choices=("prepare", "finish"))
    parser.add_argument("--channel", choices=CHANNELS)
    parser.add_argument("--branch", help="Core only: build this releases/<line> branch "
                        "instead of the newest one")
    parser.add_argument("--rehearsal", action="store_true",
                        help="Core cut dry run: build main as if it were the new Core line")
    parser.add_argument("--sources", type=Path, required=True)
    parser.add_argument("--state", type=Path, required=True)
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    try:
        if args.phase == "prepare":
            if args.publish or args.channel is None:
                raise UnsafeInventory("prepare requires a channel and forbids publish")
            if args.channel == "experimental" and (args.branch or args.rehearsal):
                raise UnsafeInventory("Experimental always builds main")
            proposal = prepare(args.channel, args.branch or None, args.rehearsal,
                               args.sources, args.state)
            print("PREPARED: " + json.dumps(proposal))
            step_outputs({"noop": str(proposal["noop"]).lower(),
                          "version": proposal["versionCandidate"],
                          "branch": proposal["branch"]})
            step_summary(prepared_summary(proposal, args.state))
        else:
            if args.channel or args.branch or args.rehearsal:
                raise UnsafeInventory("finish reads the sealed channel and branch")
            outcome = finish(args.state, args.sources, args.publish)
            print(outcome)
            step_summary([f"**Result:** `{outcome}`"])
        return 0
    except (UnsafeInventory, ValueError, OSError, KeyError, TypeError) as error:
        print(f"BLOCKED: {error}", file=sys.stderr)
        step_summary([f"**BLOCKED during {args.phase}:**", "", "```", str(error), "```"])
        return 2


if __name__ == "__main__":
    sys.exit(main())
