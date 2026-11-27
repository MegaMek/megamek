#!/usr/bin/env node
/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License (GPL), version 3 or (at your option) any later version, as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project; if not, see <https://www.gnu.org/licenses/>.
 */

/*
 * Parity check: ratgen.js (the website's copy of the RAT Generator) against MegaMek's UnitTable.findTable.
 * Run it through Gradle: ./gradlew :megamek:ratParity [-PratParityCount=3000] [-PratParitySeed=7]
 *
 *   node parity.js make  <inputs.json.gz> <count> <seed> <cases.txt>
 *       Draws random tables: active faction, year, rating, unit type and the RAT Generator panel's filters (weight
 *       classes, motive types, C3 network, mission roles, strictness). One case per line, in the format
 *       megamek.utilities.ratdata.RatParityReference reads.
 *   node parity.js check <inputs.json.gz> <cases.txt> <reference.tsv>
 *       Compares the engine's table for every case with MegaMek's. A weight may differ by 1 (and the salvage share
 *       by 1 point), which is MegaMek's own run-to-run rounding wobble. Exits 1 on any larger difference.
 */
'use strict';
const fs = require('fs');
const zlib = require('zlib');
const path = require('path');
const RatGen = require(path.join(__dirname, 'ratgen.js'));

function loadInputs(file) {
  return JSON.parse(zlib.gunzipSync(fs.readFileSync(file)).toString('utf8'));
}

/** Small seeded generator (mulberry32) so a run can be repeated exactly. */
function rng(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6D2B79F5) >>> 0;
    let t = state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Weighted toward the unit types people use most
const TYPE_POOL = [0, 0, 0, 0, 1, 1, 1, 9, 9, 3, 3, 2, 2, 5, 8, 4, 6, 10, 11, 12, 13];

function makeCases(inputs, count, seed) {
  const random = rng(seed);
  const pick = (list) => list[Math.floor(random() * list.length)];
  const subset = (list, chance) => list.filter(() => random() < chance);
  const engine = RatGen.create(inputs);
  const first = inputs.eras[0];
  const last = inputs.eras[inputs.eras.length - 1];
  const cases = [];
  let guard = 0;
  while (cases.length < count && guard++ < count * 50) {
    const year = random() < 0.3 ? pick(inputs.eras) : first + Math.floor(random() * (last - first + 1));
    const active = inputs.factions.filter((faction) => engine.isActiveInYear(engine.getFaction(faction.key), year));
    if (active.length === 0) continue;
    const faction = pick(active);
    const unitType = pick(TYPE_POOL);
    // A faction pinned to one rating is called with no rating, as MekHQ does
    const rating = faction.ratingLevels.length === 1 ? null
      : (faction.ratingSystem.length > 1 ? pick(faction.ratingSystem) : null);
    const options = RatGen.filterOptions(unitType);
    const filtered = random() < 0.6;
    const weightClasses = filtered && options.weightClasses.length
      ? subset(options.weightClasses.map((w) => w.value), 0.45) : [];
    const movementModes = filtered && options.motives.length && random() < 0.5
      ? subset(options.motives.map((m) => m.mode), 0.6) : [];
    const networkMask = filtered && options.networks.length && random() < 0.15 ? pick(options.networks).mask : 0;
    const fitting = inputs.roles.filter((role) => RatGen.roleFitsUnitType(role, unitType));
    const roles = filtered && random() < 0.4 && fitting.length
      ? [...new Set([pick(fitting)].concat(random() < 0.25 ? [pick(fitting)] : []))] : [];
    const roleStrictness = 1 + Math.floor(random() * 3);
    cases.push({ faction: faction.key, year, rating, unitType, weightClasses, movementModes, networkMask, roles,
      roleStrictness });
  }
  return cases;
}

const join = (list) => (list.length ? list.join(',') : '-');
const underscore = (text) => text.replace(/ /g, '_');

/** FACTION YEAR RATING UNITTYPE ROLES STRICTNESS WEIGHTS MOTIVES NETWORK; "-" for none, "_" for a space. */
function caseLine(inputs, c) {
  return [c.faction, c.year, c.rating === null ? '-' : underscore(c.rating), underscore(inputs.unitTypes[c.unitType]),
    join(c.roles), c.roleStrictness, join(c.weightClasses), join(c.movementModes), c.networkMask].join(' ');
}

function parseCaseLine(inputs, line) {
  const [faction, year, rating, unitType, roles, strictness, weights, motives, network] = line.trim().split(/\s+/);
  const list = (text) => (text === '-' ? [] : text.split(','));
  return {
    faction, year: Number(year), rating: rating === '-' ? null : rating.replace(/_/g, ' '),
    unitType: inputs.unitTypes.indexOf(unitType.replace(/_/g, ' ')), roles: list(roles),
    roleStrictness: Number(strictness), weightClasses: list(weights).map(Number), movementModes: list(motives),
    networkMask: Number(network),
  };
}

function readReference(file) {
  const tables = new Map();
  const table = (index) => {
    if (!tables.has(index)) tables.set(index, { units: new Map(), salvage: new Map(), salvagePct: 0, error: null });
    return tables.get(index);
  };
  for (const line of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    if (!line || line.startsWith('case\t')) continue;
    const [index, kind, entry, value] = line.split('\t');
    const current = table(Number(index));
    if (kind === 'unit') current.units.set(entry, Number(value));
    else if (kind === 'salvage') current.salvage.set(entry, Number(value));
    else if (kind === 'salvagePct') current.salvagePct = Number(value);
    else if (kind === 'error') current.error = entry;
  }
  return tables;
}

function check(inputs, cases, reference) {
  const engine = RatGen.create(inputs);
  let exact = 0;
  let withinOne = 0;
  let empty = 0;
  let bothFailed = 0;
  const failures = [];
  cases.forEach((c, index) => {
    const expected = reference.get(index) || { units: new Map(), salvage: new Map(), salvagePct: 0, error: null };
    const got = engine.findTable({ ...c, user: c.faction });
    if (expected.error || got.error) {
      if (expected.error && got.error) {
        bothFailed++;
      } else {
        failures.push({ c, reason: `MegaMek ${expected.error ? `fails (${expected.error})` : 'gives a table'}, `
          + `engine ${got.error ? `fails (${got.error})` : 'gives a table'}` });
      }
      return;
    }
    const gotUnits = new Map(got.units.map(([modelIndex, weight]) => [engine.models[modelIndex].key, weight]));
    const gotSalvage = new Map(got.salvage);
    // The salvage share is worked out from the weights, so it is only compared when every weight agrees exactly: a
    // weight off by MegaMek's own rounding wobble can move the whole-number share by more than one point.
    let maxDifference = 0;
    const differences = [];
    for (const [expectedMap, gotMap, label] of [[expected.units, gotUnits, 'unit'], [expected.salvage, gotSalvage, 'salvage']]) {
      for (const name of new Set([...expectedMap.keys(), ...gotMap.keys()])) {
        const difference = Math.abs((expectedMap.get(name) || 0) - (gotMap.get(name) || 0));
        if (difference > 0) {
          differences.push(`${label} ${name}: MegaMek ${expectedMap.get(name) || 0}, engine ${gotMap.get(name) || 0}`);
        }
        maxDifference = Math.max(maxDifference, difference);
      }
    }
    if (maxDifference === 0 && expected.salvagePct !== got.salvagePct) {
      differences.push(`salvage share: MegaMek ${expected.salvagePct}%, engine ${got.salvagePct}%`);
      maxDifference = 2;
    }
    if (expected.units.size === 0 && gotUnits.size === 0) empty++;
    if (maxDifference === 0) exact++;
    else if (maxDifference <= 1) withinOne++;
    else {
      failures.push({ c, reason: differences.slice(0, 4).join('; ')
        + (differences.length > 4 ? ` (+${differences.length - 4} more)` : '') });
    }
  });
  console.log(`[RatParity] ${cases.length} tables: ${exact} identical, ${withinOne} within MegaMek's own rounding (1), `
    + `${bothFailed} where both fail, ${failures.length} DIFFERENT (${empty} empty tables)`);
  for (const failure of failures.slice(0, 20)) {
    console.log(`  DIFFERENT ${caseLine(inputs, failure.c)}\n            ${failure.reason}`);
  }
  return failures.length;
}

const [mode, ...args] = process.argv.slice(2);
if (mode === 'make') {
  const [inputsFile, count, seed, casesFile] = args;
  const inputs = loadInputs(inputsFile);
  const cases = makeCases(inputs, Number(count), Number(seed));
  fs.writeFileSync(casesFile, cases.map((c) => caseLine(inputs, c)).join('\n') + '\n');
  console.log(`[RatParity] Wrote ${cases.length} cases (seed ${seed})`);
} else if (mode === 'check') {
  const [inputsFile, casesFile, referenceFile] = args;
  const inputs = loadInputs(inputsFile);
  const cases = fs.readFileSync(casesFile, 'utf8').split(/\r?\n/).filter((line) => line.trim())
    .map((line) => parseCaseLine(inputs, line));
  process.exitCode = check(inputs, cases, readReference(referenceFile)) > 0 ? 1 : 0;
} else {
  console.error('usage: parity.js make <inputs> <count> <seed> <cases.txt> | check <inputs> <cases.txt> <reference.tsv>');
  process.exitCode = 2;
}
