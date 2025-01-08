# MegaMek

## Table of Contents

1. About
2. Status
3. Compiling
4. Support
5. License

## 1. About

[MegaMek](http://megamek.org) is a networked Java clone of BattleTech, a turn-based sci-fi boardgame for 2+ players.
Fight using giant robots, tanks, and/or infantry on a hex-based map.
For more details, see
our [website](http://megamek.org/) and join our [Discord](https://discord.gg/XM54YH9396).

## 2. Status

| Type | MM Status | MML Status | MHQ Status |
| ---- | --------- | ---------- | ---------- |
| Latest Release | [![Release](https://img.shields.io/github/release/MegaMek/megamek.svg)](https://gitHub.com/MegaMek/megamek/releases/) | [![Release](https://img.shields.io/github/release/MegaMek/megameklab.svg)](https://gitHub.com/MegaMek/megameklab/releases/) | [![Release](https://img.shields.io/github/release/MegaMek/mekhq.svg)](https://gitHub.com/MegaMek/mekhq/releases/) |
| Javadocs | [![javadoc](https://javadoc.io/badge2/org.megamek/megamek/javadoc.svg?color=red)](https://javadoc.io/doc/org.megamek/megamek) | [![javadoc](https://javadoc.io/badge2/org.megamek/megameklab/javadoc.svg?color=red)](https://javadoc.io/doc/org.megamek/megameklab) | [![javadoc](https://javadoc.io/badge2/org.megamek/mekhq/javadoc.svg?color=red)](https://javadoc.io/doc/org.megamek/mekhq) |
| License | [![GPLv3 license](https://img.shields.io/badge/License-GPLv2-blue.svg)](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html) | [![GPLv3 license](https://img.shields.io/badge/License-GPLv2-blue.svg)](http://www.gnu.org/licenses/old-licenses/gpl-2.0.html) | [![GPLv3 license](https://img.shields.io/badge/License-GPLv3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0.html) |
| Build (CI) | [![MM Nightly CI](https://github.com/MegaMek/megamek/workflows/MegaMek%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/megamek/actions/workflows/nightly-ci.yml) | [![MML Nightly CI](https://github.com/MegaMek/megameklab/workflows/MegaMekLab%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/megameklab/actions/workflows/nightly-ci.yml) | [![MHQ Nightly CI](https://github.com/MegaMek/mekhq/workflows/MekHQ%20Nightly%20CI/badge.svg)](https://github.com/MegaMek/mekhq/actions/workflows/nightly-ci.yml) |
| Issues | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/megamek)](https://gitHub.com/MegaMek/megamek/issues/) | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/megameklab)](https://gitHub.com/MegaMek/megameklab/issues/) | [![GitHub Issues](https://badgen.net/github/open-issues/MegaMek/mekhq)](https://gitHub.com/MegaMek/mekhq/issues/) |
| PRs | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/megamek)](https://gitHub.com/MegaMek/megamek/pull/) | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/megameklab)](https://gitHub.com/MegaMek/megameklab/pull/) | [![GitHub Open Pull Requests](https://badgen.net/github/open-prs/MegaMek/mekhq)](https://gitHub.com/MegaMek/mekhq/pull/) |
| Code Coverage | [![MegaMek codecov.io](https://codecov.io/github/MegaMek/megamek/coverage.svg)](https://codecov.io/github/MegaMek/megamek) | [![MegaMekLab codecov.io](https://codecov.io/github/MegaMek/megameklab/coverage.svg)](https://codecov.io/github/MegaMek/megameklab) | [![MekHQ codecov.io](https://codecov.io/github/MegaMek/mekhq/coverage.svg)](https://codecov.io/github/MegaMek/mekhq) |

Note that not everything has been implemented across the suite at this time, which will lead to gaps.

## 3. Compiling

1) Install [Gradle](https://gradle.org/).

2) Follow the [instructions on the wiki](https://github.com/MegaMek/megamek/wiki/Working-With-Gradle) for using Gradle.

### 3.1 Style Guide

When contributing to this project, please enable the EditorConfig option within your IDE to ensure some basic compliance with our [style guide](https://github.com/MegaMek/megamek/wiki/MegaMek-Coding-Style-Guide) which includes some defaults for line length, tabs vs spaces, etc. When all else fails, we follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

To help with additional checks, you can run the following commands within a terminal to see what is in compliance. There will be a lot of output while we clean up our code, so please only focus on the files with which you are editing.

```shell
./gradlew editorconfigCheck
./gradlew check
```

The first ensures compliance with with the EditorConfig file, the other works with the Google Style Guide for most of the rest.

## 4. Support

For bugs, crashes, or other issues you can fill out a [GitHub issue request](https://github.com/MegaMek/MegaMek/issues).

## 5. License

```shell
MegaMek is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
MegaMek is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
```

Please see `license.txt` for more information.
