"""In-memory GitHub for the suite release tests. Never touches the network."""

import base64
import hashlib
import json
import subprocess
from pathlib import Path

import suite_release_plan as release

SHA = {repo: (str(i) * 40) for i, repo in enumerate(release.SOURCES, start=1)}


def sha(seed):
    return hashlib.sha1(seed.encode()).hexdigest()


def properties(line):
    major, minor, patch = line.split(".")
    return (f"# MegaMek version\nmajor={int(major)}\nminor={int(minor)}\n"
            f"patch={patch}\nextraVersion=\n")


class FakeGitHub:
    """Branches, tags, releases and assets of the four repositories, plus their writes."""

    def __init__(self, line="0.51.02", commits=None):
        self.heads = {}
        self.props = {}
        self.tags = {repo: {} for repo in release.REPOS.values()}
        self.releases = {repo: [] for repo in release.REPOS.values()}
        self.assets = {}
        self.writes = []
        self.next_id = 1000
        self.get_log = []
        self.set_branch("main", commits or SHA, line)

    def new_id(self):
        self.next_id += 1
        return self.next_id

    def set_branch(self, name, commits, line=None):
        for repo, value in commits.items():
            self.heads[(repo, name)] = value
        if line is not None:
            self.props[commits["megamek"]] = properties(line)

    # Read side: every GET goes through the production whitelist.
    def get(self, endpoint):
        self.get_log.append(endpoint)
        return release.gh_get(endpoint, runner=self.runner)

    def runner(self, command, **kwargs):
        endpoint = command[2]
        try:
            value = self.answer(endpoint)
        except KeyError:
            raise subprocess.CalledProcessError(1, command, stderr=b"HTTP 404: Not Found") from None
        return subprocess.CompletedProcess(command, 0, stdout=json.dumps(value).encode())

    def answer(self, endpoint):
        parts = endpoint.split("/")
        repo = parts[2]
        path = "/".join(parts[3:])
        if path.startswith("git/ref/heads/"):
            branch = path.removeprefix("git/ref/heads/")
            return {"ref": f"refs/heads/{branch}",
                    "object": {"type": "commit", "sha": self.heads[(repo, branch)]}}
        if path.startswith("git/matching-refs/heads/releases/"):
            return [{"ref": f"refs/heads/{branch}"} for (owner, branch) in sorted(self.heads)
                    if owner == repo and branch.startswith("releases/")]
        if path.startswith("contents/"):
            ref = path.split("?ref=")[1]
            text = self.props[ref]
            return {"encoding": "base64", "sha": sha("blob" + text),
                    "content": base64.b64encode(text.encode()).decode()}
        if path.startswith("git/ref/tags/"):
            tag = path.removeprefix("git/ref/tags/")
            return {"ref": f"refs/tags/{tag}",
                    "object": {"type": "commit", "sha": self.tags[repo][tag]}}
        if path.startswith("commits/"):
            return {"sha": self.tags[repo][path.removeprefix("commits/")]}
        kind, query = path.split("?")
        page = int(query.split("&page=")[1])
        items = ([{"name": name} for name in self.tags[repo]] if kind == "tags"
                 else self.releases[repo])
        return items[(page - 1) * 100:page * 100]

    def fetch(self, repo, asset_id, destination):
        Path(destination).write_bytes(self.assets[asset_id])

    # Write side: the GitHubWrites and CutWrites interfaces.
    def notes(self, repo, tag, commit, previous_tag):
        self.writes.append(("notes", repo, tag, previous_tag))
        return f"## What's Changed\n* {repo} change since {previous_tag}\n"

    def tag(self, repo, tag, commit):
        self.writes.append(("tag", repo, tag))
        self.tags[repo][tag] = commit

    def release(self, repo, tag, commit, *, name, body, prerelease):
        self.writes.append(("release", repo, tag, prerelease))
        entry = {"id": self.new_id(), "tag_name": tag, "draft": False,
                 "prerelease": prerelease, "name": name, "body": body, "assets": []}
        self.releases[repo].insert(0, entry)
        return entry["id"]

    def upload(self, repo, release_id, path):
        path = Path(path)
        self.writes.append(("upload", repo, path.name))
        data = path.read_bytes()
        asset_id = self.new_id()
        self.assets[asset_id] = data
        entry = next(r for r in self.releases[repo] if r["id"] == release_id)
        entry["assets"].append({"id": asset_id, "name": path.name, "size": len(data),
                                "state": "uploaded"})
        return asset_id

    def branch(self, repo, name, commit):
        self.writes.append(("branch", repo, name))
        self.heads[(repo, name)] = commit

    def version_file(self, text, blob, message):
        self.writes.append(("version_file", message))
        head = sha(text)
        self.heads[("megamek", "main")] = head
        self.props[head] = text
        return head

    # Fixtures.
    def add_suite(self, channel, version, commits):
        """A complete, already-published suite of ``channel`` at ``version``."""
        tag = "v" + version
        record = {"schemaVersion": 2, "version": version, "channel": channel, "tag": tag,
                  "products": {}, "mmData": {"repository": "https://github.com/MegaMek/mm-data",
                                             "commit": commits["mm-data"]}}
        for product, repo in release.REPOS.items():
            data = f"{product} {version}".encode()
            asset_id = self.new_id()
            self.assets[asset_id] = data
            self.tags[repo][tag] = commits[repo]
            entry = {"id": self.new_id(), "tag_name": tag, "draft": False,
                     "prerelease": channel == "experimental", "assets": [{
                         "id": asset_id, "name": f"{product}-{version}.tar.gz",
                         "size": len(data), "state": "uploaded"}]}
            self.releases[repo].insert(0, entry)
            record["products"][product] = {
                "repository": f"https://github.com/MegaMek/{repo}", "commit": commits[repo],
                "version": version, "tag": tag, "releaseId": entry["id"],
                "asset": {"assetId": asset_id, "name": f"{product}-{version}.tar.gz",
                          "sha256": hashlib.sha256(data).hexdigest(), "size": len(data)}}
        raw = json.dumps(record).encode()
        record_id = self.new_id()
        self.assets[record_id] = raw
        self.releases["megamek"][0]["assets"].append({
            "id": record_id, "name": f"suite-record-{version}.json", "size": len(raw),
            "state": "uploaded"})
        return record
