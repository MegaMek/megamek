#!/usr/bin/env python3
# Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
#
# This file is part of MegaMek.
#
# MegaMek is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
# License (GPL), version 3 or (at your option) any later version, as published by the Free Software Foundation.
#
# MegaMek is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
# warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
#
# A copy of the GPL should have been included with this project; if not, see <https://www.gnu.org/licenses/>.
"""
Run bot orders test scenarios headless and render every game.

For each scenario: runs AIMatchRunner (N games, round limit, timeout) with java directly, copies the results CSV
and the per-game orders traces into OUTDIR/<scenario>/, then renders them with render_bot_orders.py.

Usage (from the megamek project folder, after ./gradlew :megamek:stageDataFiles :megamek:classes):

    py docs/bot-orders-tests/run_bot_orders_tests.py --out OUTDIR --label fix --games 3 --rounds 14 \
        --timeout 25 docs/bot-orders-tests/9038-champion-waypoints.mms [more.mms ...]

The classpath is read from `gradlew :megamek:printAiMatchClasspath` unless --classpath-file is given.
"""

import argparse
import glob
import os
import shutil
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.normpath(os.path.join(HERE, "..", ".."))
ROOT = os.path.normpath(os.path.join(PROJECT, ".."))
JVM_OPTIONS = ["-Xmx4096m", "--add-opens", "java.base/java.util=ALL-UNNAMED", "--add-opens",
               "java.base/java.util.concurrent=ALL-UNNAMED", "-Dsun.awt.disablegrab=true"]


def read_classpath(classpath_file):
    if classpath_file:
        with open(classpath_file, encoding="utf-8") as handle:
            return handle.read().strip()
    gradlew = os.path.join(ROOT, "gradlew.bat" if os.name == "nt" else "gradlew")
    output = subprocess.run([gradlew, ":megamek:printAiMatchClasspath", "--console=plain", "-q"], cwd=ROOT,
                            capture_output=True, text=True, check=True).stdout
    lines = [line for line in output.splitlines() if line.strip()]
    return lines[-1].strip()


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", required=True)
    parser.add_argument("--label", default="")
    parser.add_argument("--games", type=int, default=3)
    parser.add_argument("--rounds", type=int, default=14)
    parser.add_argument("--timeout", type=int, default=25, help="minutes per game")
    parser.add_argument("--classpath-file", default="")
    parser.add_argument("scenarios", nargs="+")
    arguments = parser.parse_args()

    classpath = read_classpath(arguments.classpath_file)
    logs_dir = os.path.join(PROJECT, "logs")
    os.makedirs(arguments.out, exist_ok=True)
    all_traces = []
    for scenario in arguments.scenarios:
        scenario_name = os.path.splitext(os.path.basename(scenario))[0]
        before = set(glob.glob(os.path.join(logs_dir, "ai_match_results_*")))
        started = time.time()
        command = ["java"] + JVM_OPTIONS + ["-cp", classpath, "megamek.utilities.AIMatchRunner",
                                            os.path.relpath(os.path.abspath(scenario), PROJECT),
                                            str(arguments.games), str(arguments.rounds), str(arguments.timeout)]
        print("running %s: %d games, %d rounds" % (scenario_name, arguments.games, arguments.rounds), flush=True)
        with open(os.path.join(arguments.out, scenario_name + "_console.log"), "w", encoding="utf-8") as console:
            subprocess.run(command, cwd=PROJECT, stdout=console, stderr=subprocess.STDOUT, check=False)
        print("  finished in %d s" % (time.time() - started), flush=True)
        new_files = sorted(set(glob.glob(os.path.join(logs_dir, "ai_match_results_*"))) - before)
        target_dir = os.path.join(arguments.out, scenario_name)
        os.makedirs(target_dir, exist_ok=True)
        for new_file in new_files:
            destination = os.path.join(target_dir, scenario_name + "_" + os.path.basename(new_file))
            shutil.copy2(new_file, destination)
            if destination.endswith("_orders.tsv"):
                all_traces.append(destination)
        print("  %d files copied to %s" % (len(new_files), target_dir), flush=True)
    if not all_traces:
        print("no traces produced", file=sys.stderr)
        sys.exit(1)
    renderer = os.path.join(HERE, "render_bot_orders.py")
    subprocess.run([sys.executable, renderer, "--out", arguments.out, "--label", arguments.label] + all_traces,
                   check=True)


if __name__ == "__main__":
    main()
