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
 * RatGen: a JavaScript copy of MegaMek's RAT Generator table-building steps, for the RAT Probability Explorer on the
 * MegaMek website (megamek.client.ratgenerator: RATGenerator.generateTable, adjustForRating,
 * ChassisRecord.totalModelWeight, AvailabilityRating, AbstractUnitRecord.calcAvailability,
 * MissionRole.adjustAvailabilityByRole and fitsUnitType, UnitTable, ForceGenerationOptionsPanel's filter lists).
 *
 * Works from inputs.json.gz written by megamek.utilities.ratdata.RatInputExporter. Kept deliberately close to the
 * Java, including its quirks, so results can be checked against MegaMek table by table.
 *
 * CHANGE THE JAVA AND THIS FILE TOGETHER. ./gradlew :megamek:ratParity compares this engine with UnitTable.findTable
 * on thousands of random tables (parity.js beside this file) and fails when they drift.
 *
 * Usage:
 *   const engine = RatGen.create(inputs);
 *   const table = engine.findTable({ faction: 'FS', unitType: 0, year: 3039, rating: 'C', weightClasses: [1, 2],
 *                                    movementModes: [], networkMask: 0, roles: ['recon'], roleStrictness: 2 });
 *   engine.setCode('model', 3039, modelIndex, 'FS', '8');   // test a weight; null removes the code
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.RatGen = factory();
  }
}(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  // ---------------------------------------------------------------- constants (MegaMek values)
  const UT = {
    MEK: 0, TANK: 1, BATTLE_ARMOR: 2, INFANTRY: 3, PROTOMEK: 4, VTOL: 5, NAVAL: 6, GUN_EMPLACEMENT: 7,
    CONV_FIGHTER: 8, AEROSPACE_FIGHTER: 9, SMALL_CRAFT: 10, DROPSHIP: 11, JUMPSHIP: 12, WARSHIP: 13,
    SPACE_STATION: 14, HANDHELD_WEAPON: 15, MOBILE_STRUCTURE: 16, ADVANCED_BUILDING: 17, AERO: 18,
  };
  const WC = { ULTRA_LIGHT: 0, LIGHT: 1, MEDIUM: 2, HEAVY: 3, ASSAULT: 4 };
  /** MissionRole.FAST_HEAVY_RECON_MIN_WALK: walking MP an untagged heavy Mek needs for a recon slot. */
  const FAST_HEAVY_RECON_MIN_WALK = 5;
  const NETWORK_C3_MASTER = 1 << 1;
  const NETWORK_COMPANY_COMMAND = 1 << 5;
  const MIN_OMNI_DIFFERENCE = 2.5;
  const MIN_CLAN_DIFFERENCE = 2.5;
  const MIN_SL_DIFFERENCE = 2.5;
  const TECH = ['OMNI', 'CLAN', 'IS_ADVANCED', 'OMNI_AERO', 'CLAN_AERO', 'IS_ADVANCED_AERO', 'CLAN_VEE',
    'IS_ADVANCED_VEE'];
  const LOG2 = Math.log(2);

  const calcWeight = (av) => Math.pow(2, av / 2.0);
  const calcAvRating = (weight) => 2.0 * Math.log(weight) / LOG2;
  /**
   * Java's (int) cast: truncates toward zero, turns NaN into 0, and clamps to the int range. The clamp matters: a
   * malformed code such as MERC:3112 (an availability of 3112) gives an infinite weight, which Java holds as
   * Integer.MAX_VALUE and carries on with, where Math.trunc would keep Infinity and later produce NaN.
   */
  function javaInt(value) {
    if (Number.isNaN(value)) return 0;
    if (value >= 2147483647) return 2147483647;
    if (value <= -2147483648) return -2147483648;
    return Math.trunc(value);
  }

  /**
   * (int) AvailabilityRating.calcAvRating(weight), as Java computes it. Java's Math.log and the browser's can differ in
   * the last bit, and the merge then truncates, so a value whose true result is a whole number (one parent, or parents
   * that agree) can land either side of it: code 5 comes back as 5.0 in Java and 4.999999999999999 here. Snapping to
   * the whole number matches Java for every code from -2 to 14; at -1 Java's own result is -0.9999999999999999,
   * which truncates to 0.
   */
  function mergedAvailability(avRating) {
    const nearest = Math.round(avRating);
    if (Math.abs(avRating - nearest) < 1e-9) {
      return nearest === -1 ? 0 : nearest;
    }
    return javaInt(avRating);
  }

  /** RATGenerator.interpolate: nulls count as 0 unless both are null. */
  function interpolate(av1, av2, year1, year2, now) {
    if ((av1 === null || av1 === undefined) && (av2 === null || av2 === undefined)) return null;
    const a = (av1 === null || av1 === undefined) ? 0.0 : av1;
    const b = (av2 === null || av2 === undefined) ? 0.0 : av2;
    if (year1 === year2) return a;
    return a + (b - a) * (now - year1) / (year2 - year1);
  }

  // ---------------------------------------------------------------- AvailabilityRating
  class AvailabilityRating {
    /** Mirrors the Java constructor: code is FKEY[!RATING:AV...] or FKEY:AV[+/-][:YEAR]. */
    constructor(era, code) {
      this.era = era;
      this.startYear = era;
      this.faction = 'General';
      this.availability = 0;
      this.ratingAdjustment = 0;
      this.ratingByLevel = new Map();
      this.ratingByNumericLevel = new Map();
      if (!code.includes('!')) {
        const fields = code.split(':');
        if (fields.length < 2 || fields.length > 3) return;
        this.faction = fields[0];
        let value = fields[1];
        if (value.endsWith('+')) { this.ratingAdjustment++; value = value.replace(/\+/g, ''); }
        if (value.endsWith('-')) { this.ratingAdjustment--; value = value.replace(/-/g, ''); }
        this.availability = parseIntOr(value, 0);
        if (fields.length > 2) {
          this.startYear = parseIntOr(fields[2], -1);
          if (this.startYear < 0) throw new Error('Invalid year value.');
        }
      } else {
        const fields = code.split('!');
        this.faction = fields[0];
        for (let i = 1; i < fields.length; i++) {
          const sub = fields[i].split(':');
          if (sub.length !== 2) return;
          this.ratingByLevel.set(sub[0], parseIntOr(sub[1], 0));
        }
        this.availability = Math.max(...this.ratingByLevel.values());
      }
    }

    hasMultipleRatings() {
      if (this.ratingByLevel.size === 0) return false;
      for (const value of this.ratingByLevel.values()) if (value > 0) return true;
      return false;
    }

    getAvailabilityAt(equipmentLevelIndex) {
      if (!this.hasMultipleRatings() || equipmentLevelIndex < 0) return this.availability;
      return this.ratingByNumericLevel.has(equipmentLevelIndex) ? this.ratingByNumericLevel.get(equipmentLevelIndex) : 0;
    }

    adjustForRating(equipRating, numLevels) {
      if (this.ratingAdjustment === 0 || equipRating < 0) return this.availability;
      if (this.ratingAdjustment > 0) return this.availability - (numLevels - 1 - equipRating);
      return this.availability - equipRating;
    }

    /** setRatingByNumericLevel(FactionRecord): note ratingLevel carries over between levels, as in Java. */
    setRatingByNumericLevel(factionRecord) {
      if (!factionRecord || !this.hasMultipleRatings()) return;
      let ratingLevel = -1;
      const factionRatings = factionRecord.ratingSystem;
      const numRatingLevels = factionRatings.length;
      for (const [curLevel, value] of this.ratingByLevel) {
        if ((curLevel === null) && factionRecord.ratingLevels.length === 1) {
          ratingLevel = factionRatings.indexOf(factionRecord.ratingLevels[0]);
        }
        if (curLevel !== null && numRatingLevels > 1) {
          ratingLevel = factionRatings.indexOf(curLevel);
        }
        this.ratingByNumericLevel.set(ratingLevel, value);
      }
    }

    getAvailabilityCode() {
      if (!this.hasMultipleRatings()) {
        if (this.ratingAdjustment === 0) return String(this.availability);
        return this.availability + (this.ratingAdjustment < 0 ? '-' : '+');
      }
      let code = '';
      for (const [level, value] of this.ratingByLevel) code += `!${level}:${value}`;
      return code;
    }

    makeCopy(newFaction) {
      const copy = new AvailabilityRating(this.era,
        newFaction + (this.hasMultipleRatings() ? '' : ':') + this.getAvailabilityCode());
      copy.startYear = this.startYear;
      return copy;
    }
  }

  function parseIntOr(text, fallback) {
    if (!/^\s*[+-]?\d+\s*$/.test(text)) return fallback;
    return parseInt(text, 10);
  }

  // ---------------------------------------------------------------- MissionRole.adjustAvailabilityByRole
  function adjustAvailabilityByRole(avRating, desiredRoles, model, strictness) {
    let roleApplied = false;
    const desired = desiredRoles || [];
    const has = (role) => model.roleSet.has(role);
    const wants = (role) => desired.includes(role);
    const adj = [1, 2, 3, 4, 5].map((i) => i * strictness / 3.0);
    const [minAdjust, lightAdjust, mediumAdjust, strongAdjust, maxAdjust] = adj;
    const type = model.unitType;
    const mode = model.movementMode;
    const weightClass = model.weightClass;
    const speed = model.speed;
    // MissionRole.isFastHeavyMek: an untagged heavy Mek with Walk 5+ (CO p. 65) can fill a recon slot
    const isFastHeavyMek = type === UT.MEK && weightClass === WC.HEAVY && (model.walkMp || 0) >= FAST_HEAVY_RECON_MIN_WALK;

    const isSpecialized = () => {
      if (model.roleSet.size === 0) return false;
      if (has('support') && !wants('support')) return true;
      if (has('civilian') && !wants('civilian')) return true;
      return (type !== UT.DROPSHIP) && (model.roleSet.size === 1) && (has('artillery') || has('missile_artillery'));
    };

    if (desired.length > 0) {
      roleApplied = true;
      for (const role of desired) {
        switch (role) {
          case 'recon':
            if (isSpecialized()) return null;
            if (has('recon')) avRating += mediumAdjust;
            else if (has('ew_support') || has('specops')) avRating += lightAdjust;
            else if (has('spotter')) avRating += minAdjust;
            else if ((type === UT.MEK || type === UT.PROTOMEK) && (weightClass === WC.LIGHT || weightClass === WC.MEDIUM) && speed >= 6) avRating = Math.max(avRating - strongAdjust, 1.0);
            else if (isFastHeavyMek) avRating = Math.max(avRating - strongAdjust, 1.0);
            else if (type === UT.VTOL || mode === 'HOVER') avRating = Math.max(avRating - strongAdjust, 1.0);
            else return null;
            break;
          case 'ew_support':
            if (!has('ew_support') || isSpecialized()) return null;
            break;
          case 'spotter':
            if (isSpecialized()) return null;
            if (has('spotter')) avRating += strongAdjust;
            else if (type === UT.INFANTRY || type === UT.BATTLE_ARMOR) avRating -= mediumAdjust;
            else if (has('recon')) avRating -= mediumAdjust;
            else avRating -= strongAdjust;
            break;
          case 'command':
            if (has('civilian') && !wants('civilian')) return null;
            if (has('command')) avRating += maxAdjust; else avRating -= strongAdjust;
            if (type === UT.MEK && has('support') && !wants('support')) return null;
            if ((NETWORK_COMPANY_COMMAND & model.networkMask) !== 0) avRating += lightAdjust;
            else if ((NETWORK_C3_MASTER & model.networkMask) !== 0) avRating += minAdjust;
            break;
          case 'fire_support':
            if (has('support') || has('civilian')) return null;
            if (has('fire_support') || model.longRange > 0.75) avRating += mediumAdjust;
            else if (model.longRange > 0.5) avRating += lightAdjust;
            else if (model.longRange > 0.2) avRating += minAdjust;
            else if (has('artillery') || has('missile_artillery') || has('mixed_artillery')) avRating += minAdjust;
            else return null;
            break;
          case 'sr_fire_support':
            if (isSpecialized()) return null;
            if (has('sr_fire_support')) avRating += mediumAdjust;
            else if (!has('fire_support') && model.longRange <= 0.2) avRating += lightAdjust;
            else if (model.longRange >= 0.5) return null;
            break;
          case 'artillery':
            if (has('civilian') && !wants('civilian')) return null;
            if (!has('artillery') && !has('missile_artillery') && !has('mixed_artillery')) return null;
            break;
          case 'mixed_artillery':
            if (has('civilian') && !wants('civilian')) return null;
            if (!has('mixed_artillery')) return null;
            break;
          case 'missile_artillery':
            if (!has('missile_artillery')) return null;
            break;
          case 'urban':
            if (isSpecialized()) return null;
            if (has('urban')) avRating += strongAdjust;
            else if (has('anti_infantry') || has('sr_fire_support') || model.longRange <= 0.2) avRating += lightAdjust;
            else if (has('inf_support')) avRating += minAdjust;
            else if (has('fire_support') || model.longRange >= 0.5) avRating -= minAdjust;
            else avRating -= mediumAdjust;
            if (mode === 'WHEELED') avRating += mediumAdjust;
            else if (mode === 'TRACKED' || mode === 'HOVER') avRating -= mediumAdjust;
            break;
          case 'anti_infantry':
            if (isSpecialized()) return null;
            if (has('anti_infantry')) avRating += mediumAdjust;
            else if (model.apWeapons) avRating += lightAdjust;
            else avRating -= strongAdjust;
            break;
          case 'inf_support':
            if (has('support') || has('civilian')) return null;
            else if (has('inf_support')) avRating += mediumAdjust;
            else if (has('fire_support') || has('anti_aircraft') || has('mixed_artillery')) avRating -= minAdjust;
            else if (has('apc') || has('anti_aircraft') || has('artillery')) avRating -= lightAdjust;
            else avRating -= maxAdjust;
            break;
          case 'apc':
            if (has('support') && !wants('support')) return null;
            if (has('civilian') && !wants('civilian')) return null;
            if (!has('apc')) return null;
            break;
          case 'mechanized_ba':
            if (has('support') && !wants('support')) return null;
            if (has('civilian') && !wants('civilian')) return null;
            if (!model.mechanizedBA && !has('mag_clamp')) return null;
            break;
          case 'mag_clamp':
            if (has('civilian') && !wants('civilian')) return null;
            if (!model.magClamp && !has('mag_clamp')) return null;
            break;
          case 'marine':
          case 'mountaineer':
          case 'field_gun':
            if (isSpecialized()) return null;
            if (!has(role)) return null;
            break;
          case 'paratrooper':
            if (isSpecialized()) return null;
            if (has('paratrooper')) avRating += strongAdjust;
            else if (mode === 'INF_JUMP') avRating = Math.max(avRating - mediumAdjust, 1.0);
            else return null;
            break;
          case 'anti_mek':
            if (isSpecialized()) return null;
            if (has('anti_mek')) avRating += strongAdjust;
            else if (mode === 'INF_LEG' || mode === 'INF_JUMP' || mode === 'INF_MOTORIZED') avRating -= mediumAdjust;
            else return null;
            break;
          case 'xct':
            if (isSpecialized()) return null;
            if (has('xct')) avRating += mediumAdjust;
            else if (has('marine')) avRating = Math.max(avRating - lightAdjust, 1.0);
            else return null;
            break;
          case 'cavalry':
            if (isSpecialized()) return null;
            if (has('cavalry')) avRating += mediumAdjust;
            else if (type === UT.MEK || type === UT.PROTOMEK) {
              if (weightClass === WC.MEDIUM && speed >= 6) avRating = Math.max(avRating - strongAdjust, 1.0);
              else if (weightClass === WC.HEAVY && speed >= 5) avRating = Math.max(avRating - strongAdjust, 1.0);
              else if (weightClass === WC.LIGHT && speed >= 5 && speed <= 7) avRating = Math.max(avRating - strongAdjust, 1.0);
              else return null;
            } else if (mode === 'HOVER' && weightClass === WC.MEDIUM) avRating = Math.max(avRating - strongAdjust, 1.0);
            else if (mode === 'TRACKED' && speed >= 5) {
              if (weightClass === WC.LIGHT && speed <= 6) avRating = Math.max(avRating - strongAdjust, 1.0);
              else if (weightClass === WC.MEDIUM) avRating = Math.max(avRating - strongAdjust, 1.0);
              else return null;
            } else return null;
            break;
          case 'raider':
            if (isSpecialized()) return null;
            if (has('raider')) avRating += mediumAdjust;
            else if (type === UT.MEK || type === UT.PROTOMEK) {
              if ((weightClass === WC.LIGHT && speed >= 5) || (weightClass === WC.MEDIUM && speed >= 5)
                || (weightClass === WC.HEAVY && speed >= 4)) {
                if (model.ammoRequirement < 0.2) avRating = Math.max(avRating - mediumAdjust, 1.0);
                else if (model.ammoRequirement < 0.5) avRating = Math.max(avRating - strongAdjust, 1.0);
              } else return null;
            } else return null;
            break;
          case 'anti_aircraft':
            if (has('support') || has('civilian')) return null;
            if (has('anti_aircraft') || model.flak > 0.75) avRating += mediumAdjust;
            else if (model.flak > 0.5) avRating += lightAdjust;
            else if (model.flak > 0.2) avRating += minAdjust;
            else if (has('artillery') || has('missile_artillery')) avRating = Math.max(avRating - mediumAdjust, 1.0);
            else if (type !== UT.INFANTRY && has('fire_support')) avRating = Math.max(avRating - strongAdjust, 1.0);
            else return null;
            break;
          case 'incendiary':
            if (isSpecialized()) return null;
            if (has('incendiary')) avRating += mediumAdjust;
            else if (model.incendiary) avRating -= lightAdjust;
            else return null;
            break;
          case 'specops':
            if (isSpecialized()) return null;
            if (has('specops')) avRating += strongAdjust;
            else if (has('recon') || has('raider')) avRating -= lightAdjust;
            else if (type !== UT.INFANTRY) avRating -= mediumAdjust;
            else avRating -= strongAdjust;
            break;
          case 'omni':
            if (has('support') && !wants('support')) return null;
            if (has('civilian') && !wants('civilian')) return null;
            if (isSpecialized() || !model.omni) return null;
            break;
          case 'training':
            if (has('support') && !wants('support')) return null;
            if (has('civilian') && !wants('civilian')) return null;
            if (has('training')) avRating += strongAdjust; else avRating -= mediumAdjust;
            break;
          case 'ground_support':
            if (isSpecialized()) return null;
            if (has('ground_support')) avRating += mediumAdjust;
            else if (has('bomber')) avRating -= lightAdjust;
            else avRating -= mediumAdjust;
            break;
          case 'interceptor':
          case 'bomber':
          case 'escort':
            if (isSpecialized()) return null;
            if (has(role)) avRating += mediumAdjust; else avRating -= mediumAdjust;
            break;
          case 'assault':
            if (isSpecialized()) return null;
            if (has('assault')) avRating += mediumAdjust;
            else if (has('pocket_warship')) avRating -= minAdjust;
            else return null;
            break;
          case 'vee_carrier':
          case 'infantry_carrier':
          case 'mek_carrier':
            if (isSpecialized()) return null;
            if (has(role)) avRating += mediumAdjust;
            else if (has('troop_carrier')) avRating -= minAdjust;
            else return null;
            break;
          case 'ba_carrier':
          case 'protomek_carrier':
          case 'tug':
          case 'asf_carrier':
          case 'troop_carrier':
            if (isSpecialized()) return null;
            if (!has(role)) return null;
            break;
          case 'pocket_warship':
            if (isSpecialized()) return null;
            if (has('pocket_warship')) avRating += mediumAdjust;
            else if (has('assault')) avRating -= minAdjust;
            else return null;
            break;
          case 'corvette':
          case 'destroyer':
          case 'frigate':
          case 'cruiser':
          case 'battleship':
          case 'generator':
          case 'control':
            if (!has(role)) return null;
            break;
          case 'engineer':
            if (has('engineer')) {
              avRating += mediumAdjust;
              if ((wants('cargo') && has('cargo')) || (wants('minesweeper') && has('minesweeper'))
                || (wants('minelayer') && has('minelayer'))) avRating += lightAdjust;
            } else if ((has('apc') && (mode === 'WHEELED')) || mode === 'TRACKED') { // Java operator precedence
              avRating -= lightAdjust;
            } else if ((has('minesweeper') && !wants('minesweeper')) || (has('minelayer') && !wants('minelayer'))) {
              avRating -= minAdjust;
            } else if (has('support')) {
              if (has('cargo') && (mode === 'WHEELED' || mode === 'TRACKED' || mode === 'VTOL')) avRating -= minAdjust;
              else if (!wants('support')) avRating -= lightAdjust;
              else return null;
            } else return null;
            break;
          case 'minesweeper':
          case 'minelayer':
          case 'bridge_layer':
          case 'demolition':
          case 'firefighter':
          case 'fieldworks':
          case 'cargo':
          case 'recovery':
            if (has('civilian') && !wants('civilian')) return null;
            if (!has(role)) return null;
            break;
          case 'support':
            if (has('support')) {
              avRating += mediumAdjust;
              if (has('engineer') || has('cargo')) avRating += minAdjust;
            } else if (has('apc') && (mode === 'WHEELED' || mode === 'TRACKED' || mode === 'VTOL')) avRating -= lightAdjust;
            else if (has('engineer') || (has('cargo') && !has('civilian') && (mode === 'WHEELED' || mode === 'TRACKED' || mode === 'VTOL'))) avRating -= minAdjust;
            else if (has('civilian') && has('cargo')) avRating -= lightAdjust;
            else return null;
            break;
          case 'civilian':
            if (!has('civilian') || (has('civilian') && has('support'))) return null;
            break;
          default:
            roleApplied = false;
        }
      }
    }

    if (!roleApplied) {
      if (type !== UT.DROPSHIP && type !== UT.JUMPSHIP) {
        if (has('support') || has('civilian')) return null;
        if (model.roleSet.size === 1 && (has('artillery') || has('missile_artillery'))) return null;
      }
    }
    return avRating;
  }

  // ---------------------------------------------------------------- filter options (RATGenUnitTypeCard)
  /** MissionRole.fitsUnitType */
  function roleFitsUnitType(role, unitType) {
    const t = unitType;
    switch (role) {
      case 'recon': return t !== UT.GUN_EMPLACEMENT && t !== UT.JUMPSHIP && t !== UT.SPACE_STATION && t !== UT.AERO
        && t !== UT.ADVANCED_BUILDING && t !== UT.MOBILE_STRUCTURE && t !== UT.HANDHELD_WEAPON;
      case 'ew_support': return [UT.MEK, UT.TANK, UT.PROTOMEK, UT.VTOL, UT.NAVAL, UT.GUN_EMPLACEMENT, UT.SMALL_CRAFT,
        UT.ADVANCED_BUILDING, UT.HANDHELD_WEAPON].includes(t);
      case 'spotter': return t <= UT.AEROSPACE_FIGHTER || t === UT.ADVANCED_BUILDING || t === UT.HANDHELD_WEAPON;
      case 'command': return [UT.MEK, UT.TANK, UT.VTOL, UT.NAVAL, UT.CONV_FIGHTER, UT.SMALL_CRAFT, UT.ADVANCED_BUILDING,
        UT.MOBILE_STRUCTURE].includes(t);
      case 'fire_support': case 'sr_fire_support':
        return t <= UT.GUN_EMPLACEMENT || t === UT.ADVANCED_BUILDING || t === UT.HANDHELD_WEAPON;
      case 'artillery': case 'missile_artillery': case 'mixed_artillery':
        return t <= UT.INFANTRY || [UT.VTOL, UT.NAVAL, UT.GUN_EMPLACEMENT, UT.CONV_FIGHTER, UT.SMALL_CRAFT, UT.DROPSHIP,
          UT.ADVANCED_BUILDING, UT.MOBILE_STRUCTURE].includes(t);
      case 'urban': return t <= UT.PROTOMEK;
      case 'anti_infantry': case 'inf_support': return t <= UT.PROTOMEK || t === UT.VTOL;
      case 'apc': return t === UT.TANK || t === UT.VTOL;
      case 'mechanized_ba': return t === UT.BATTLE_ARMOR;
      case 'mag_clamp': return t === UT.BATTLE_ARMOR || t === UT.PROTOMEK;
      case 'marine': return t === UT.BATTLE_ARMOR || t === UT.INFANTRY;
      case 'mountaineer': case 'paratrooper': case 'anti_mek': case 'field_gun': case 'xct': return t === UT.INFANTRY;
      case 'cavalry': return t === UT.MEK || t === UT.TANK || t === UT.PROTOMEK;
      case 'raider': return t === UT.MEK || t === UT.TANK || t === UT.PROTOMEK || t === UT.VTOL;
      case 'anti_aircraft': return [UT.MEK, UT.TANK, UT.INFANTRY, UT.NAVAL, UT.GUN_EMPLACEMENT, UT.ADVANCED_BUILDING,
        UT.MOBILE_STRUCTURE].includes(t);
      case 'incendiary': return t <= UT.PROTOMEK;
      case 'specops': return [UT.MEK, UT.TANK, UT.INFANTRY, UT.BATTLE_ARMOR, UT.VTOL].includes(t);
      case 'omni': return t === UT.MEK || t === UT.TANK || t === UT.AEROSPACE_FIGHTER;
      case 'bomber': case 'interceptor': return t === UT.CONV_FIGHTER || t === UT.AEROSPACE_FIGHTER;
      case 'ground_support': case 'escort': return t === UT.CONV_FIGHTER || t === UT.AEROSPACE_FIGHTER || t === UT.SMALL_CRAFT;
      case 'assault': case 'infantry_carrier': case 'ba_carrier': return t === UT.DROPSHIP || t === UT.SMALL_CRAFT;
      case 'vee_carrier': case 'mek_carrier': case 'tug': case 'pocket_warship': case 'protomek_carrier': return t === UT.DROPSHIP;
      case 'troop_carrier': case 'asf_carrier': return t === UT.DROPSHIP || t === UT.WARSHIP;
      case 'corvette': case 'destroyer': case 'frigate': case 'cruiser': case 'battleship': return t === UT.WARSHIP;
      case 'training': return [UT.MEK, UT.TANK, UT.VTOL, UT.NAVAL, UT.CONV_FIGHTER].includes(t);
      case 'engineer': case 'bridge_layer': case 'demolition': case 'firefighter': case 'fieldworks':
        return t === UT.MEK || t === UT.TANK || t === UT.INFANTRY;
      case 'minesweeper': case 'minelayer': return t === UT.TANK || t === UT.BATTLE_ARMOR || t === UT.INFANTRY;
      case 'support': return t !== UT.PROTOMEK && t !== UT.GUN_EMPLACEMENT && t !== UT.WARSHIP && t !== UT.HANDHELD_WEAPON;
      case 'civilian': return t !== UT.PROTOMEK && t !== UT.GUN_EMPLACEMENT && t !== UT.AEROSPACE_FIGHTER
        && t !== UT.WARSHIP && t !== UT.HANDHELD_WEAPON;
      case 'cargo': return [UT.TANK, UT.VTOL, UT.NAVAL, UT.CONV_FIGHTER].includes(t)
        || (t >= UT.SMALL_CRAFT && t <= UT.SPACE_STATION) || t === UT.ADVANCED_BUILDING || t === UT.MOBILE_STRUCTURE;
      case 'recovery': return t === UT.TANK;
      case 'generator': case 'control': return t === UT.ADVANCED_BUILDING;
      default: return false;
    }
  }

  const WEIGHT_CLASS_NAMES = ['Ultra light', 'Light', 'Medium', 'Heavy', 'Assault', 'Super heavy / colossal',
    'Small craft', 'Small', 'Medium', 'Large', 'Small', 'Large'];

  /**
   * The RAT Generator panel's options for a unit type: weight class check boxes (with which start ticked), motive
   * check boxes and C3 network choices. Mirrors ForceGenerationOptionsPanel.RATGenUnitTypeCard.
   */
  function filterOptions(unitType) {
    const range = (start, end) => { const list = []; for (let w = start; w <= end; w++) list.push(w); return list; };
    let weights = [];
    let allTicked = false;
    switch (unitType) {
      case UT.MEK: weights = range(0, 5); break;
      case UT.NAVAL: case UT.PROTOMEK: weights = range(1, 5); allTicked = true; break;
      case UT.TANK: weights = range(1, 5); break;
      case UT.BATTLE_ARMOR: weights = range(0, 4); allTicked = true; break;
      case UT.AEROSPACE_FIGHTER: weights = range(1, 3); break;
      case UT.DROPSHIP: weights = range(7, 9); allTicked = true; break;
      case UT.WARSHIP: weights = range(10, 11); allTicked = true; break;
      default: weights = [];
    }
    let defaultWeights = allTicked ? weights.slice() : [];
    if (!allTicked && weights.length > 0) defaultWeights = [weights[Math.min(weights[0] > 0 ? 0 : 1, weights.length - 1)]];

    const motive = (label, mode, ticked) => ({ label, mode, ticked });
    let motives = [];
    switch (unitType) {
      case UT.TANK: motives = [motive('Hover', 'HOVER', true), motive('Tracked', 'TRACKED', true),
        motive('Wheeled', 'WHEELED', true), motive('WiGE', 'WIGE', true), motive('VTOL', 'VTOL', false)]; break;
      case UT.INFANTRY: motives = [motive('Leg', 'INF_LEG', true), motive('Jump', 'INF_JUMP', true),
        motive('Motorized', 'INF_MOTORIZED', true), motive('Mechanized hover', 'HOVER', true),
        motive('Mechanized tracked', 'TRACKED', true), motive('Mechanized wheeled', 'WHEELED', true)]; break;
      case UT.BATTLE_ARMOR: motives = [motive('Leg', 'INF_LEG', true), motive('Jump', 'INF_JUMP', true),
        motive('UMU', 'INF_UMU', true), motive('VTOL', 'VTOL', true)]; break;
      case UT.NAVAL: motives = [motive('Naval', 'NAVAL', true), motive('Hydrofoil', 'HYDROFOIL', true),
        motive('Submarine', 'SUBMARINE', true)]; break;
      case UT.DROPSHIP: motives = [motive('Aerodyne', 'AERODYNE', true), motive('Spheroid', 'SPHEROID', true)]; break;
      default: motives = [];
    }

    const network = (label, mask) => ({ label, mask });
    let networks = [];
    switch (unitType) {
      case UT.MEK: case UT.TANK: case UT.VTOL: case UT.NAVAL: case UT.CONV_FIGHTER: case UT.AEROSPACE_FIGHTER:
        networks = [network('None', 0), network('C3 slave', 1), network('C3 master', 2), network('C3i', 4),
          network('Boosted C3 slave', 17), network('Boosted C3 master', 18), network('C3 company command', 32),
          network('Boosted C3 company command', 48), network('Nova CEWS', 8)];
        break;
      case UT.BATTLE_ARMOR:
        networks = [network('None', 0), network('BA C3', 1), network('Boosted BA C3', 17), network('C3i', 4)];
        break;
      case UT.DROPSHIP: case UT.JUMPSHIP: case UT.WARSHIP: case UT.SPACE_STATION:
        networks = [network('None', 0), network('Naval C3', 4)];
        break;
      default: networks = [];
    }
    return {
      weightClasses: weights.map((w) => ({ value: w, label: WEIGHT_CLASS_NAMES[w] || String(w) })),
      defaultWeightClasses: defaultWeights, motives, networks,
    };
  }

  // ---------------------------------------------------------------- engine
  function create(inputs) {
    const eras = inputs.eras.slice().sort((a, b) => a - b);
    const eraSet = new Set(eras);
    const floorEra = (year) => { let r = null; for (const e of eras) if (e <= year) r = e; return r; };
    const ceilingEra = (year) => { for (const e of eras) if (e >= year) return e; return null; };

    const factions = new Map();
    for (const faction of inputs.factions) {
      const aliasYears = Object.keys(faction.aliasesByYear || {}).map(Number).sort((a, b) => a - b);
      factions.set(faction.key, {
        ...faction,
        aliasYears,
        activeYears: faction.activeYears,
      });
    }
    const factionAliases = inputs.factionAliases || {};
    const getFaction = (key) => factions.get(key) || (factionAliases[key] ? factions.get(factionAliases[key]) : undefined) || null;

    const models = inputs.models.map((model, index) => ({ ...model, index, roleSet: new Set(model.roles) }));
    const chassis = inputs.chassis.map((record, index) => ({ ...record, index }));

    // era -> { chassis: Map<chassisIndex, Map<factionCode, AvailabilityRating>>, models: same }
    const eraIndex = new Map();
    function eraData(era) {
      if (!eraIndex.has(era)) {
        const raw = inputs.codes[String(era)] || { chassis: {}, models: {} };
        const parsed = { chassis: new Map(), models: new Map() };
        for (const level of ['chassis', 'models']) {
          for (const [index, codes] of Object.entries(raw[level])) {
            const ratings = new Map();
            for (const encoded of codes) {
              const rating = decodeRating(era, encoded);
              if (rating) ratings.set(rating.faction, rating);
            }
            parsed[level].set(Number(index), ratings);
          }
        }
        eraIndex.set(era, parsed);
      }
      return eraIndex.get(era);
    }

    /** "FS=7+@3055" or "LA=!A:5!B:3" -> AvailabilityRating, as RATGenerator.parseChassisNode builds it. */
    function decodeRating(era, encoded) {
      const at = encoded.lastIndexOf('@');
      const body = at >= 0 ? encoded.slice(0, at) : encoded;
      const startYear = at >= 0 ? Number(encoded.slice(at + 1)) : era;
      const eq = body.indexOf('=');
      const factionCode = body.slice(0, eq);
      const code = body.slice(eq + 1);
      return buildRating(era, factionCode, code, startYear);
    }

    function buildRating(era, factionCode, code, startYear) {
      const rating = new AvailabilityRating(era, code.startsWith('!') ? factionCode + code : `${factionCode}:${code}`);
      if (startYear !== undefined && startYear !== null) rating.startYear = startYear;
      const record = getFaction(rating.faction);
      if (!record && !(`${factionCode}`).startsWith('General')) return null;
      if (rating.hasMultipleRatings()) rating.setRatingByNumericLevel(record);
      return rating;
    }

    function lineageCodes(faction, year) {
      if (faction.aliasYears.length === 0) return [faction.key];
      const codes = [];
      let active = null;
      for (const aliasYear of faction.aliasYears) if (aliasYear <= year) active = faction.aliasesByYear[aliasYear];
      codes.push(active !== null ? active : faction.key);
      if (!codes.includes(faction.key)) codes.push(faction.key);
      for (const aliasYear of faction.aliasYears) {
        const code = faction.aliasesByYear[aliasYear];
        if (!codes.includes(code)) codes.push(code);
      }
      return codes;
    }

    function findLineage(unitAvailability, faction, year) {
      for (const code of lineageCodes(faction, year)) {
        if (unitAvailability.has(code)) return unitAvailability.get(code);
      }
      return null;
    }

    function mergeFactionAvailability(factionKey, list) {
      if (list.length === 0) return null;
      let totalWeight = 0;
      let totalAdjustment = 0;
      for (const rating of list) {
        totalWeight += calcWeight(rating.availability);
        totalAdjustment += rating.ratingAdjustment;
      }
      const merged = list[0].makeCopy(factionKey);
      merged.availability = mergedAvailability(calcAvRating(totalWeight / list.length));
      if (totalAdjustment !== 0) merged.ratingAdjustment = totalAdjustment > 0 ? 1 : -1;
      if (merged.hasMultipleRatings()) merged.setRatingByNumericLevel(factions.get(factionKey) || null);
      return merged;
    }

    function findChassisByKey(era, chassisIndex, factionKey, year) {
      const faction = getFaction(factionKey);
      if (faction) return findChassis(era, chassisIndex, faction, year);
      const unitAvailability = eraData(era).chassis.get(chassisIndex);
      if (unitAvailability) {
        const general = unitAvailability.get('General');
        if (general && year >= general.startYear) return general;
      }
      return null;
    }

    function findChassis(era, chassisIndex, faction, year) {
      if (!faction) return null;
      let result = null;
      const unitAvailability = eraData(era).chassis.get(chassisIndex);
      if (unitAvailability) {
        const lineage = findLineage(unitAvailability, faction, year);
        if (lineage) result = lineage;
        else if (faction.parents.length === 1) result = findChassisByKey(era, chassisIndex, faction.parents[0], year);
        else if (faction.parents.length > 0) {
          const list = [];
          for (const parent of faction.parents) {
            const found = findChassisByKey(era, chassisIndex, parent, year);
            if (found) list.push(found);
          }
          result = mergeFactionAvailability(faction.key, list);
        } else result = unitAvailability.get('General') || null;
      }
      return (result && year >= result.startYear) ? result : null;
    }

    function findModelByKey(era, modelIndex, factionKey) {
      const faction = getFaction(factionKey);
      if (faction) return findModel(era, modelIndex, faction);
      const unitAvailability = eraData(era).models.get(modelIndex);
      return unitAvailability ? (unitAvailability.get('General') || null) : null;
    }

    function findModel(era, modelIndex, faction) {
      const model = models[modelIndex];
      if (!faction || model.excludedFactions.includes(faction.key)) return null;
      const unitAvailability = eraData(era).models.get(modelIndex);
      if (!unitAvailability) return null;
      const lineage = findLineage(unitAvailability, faction, era);
      if (lineage) return lineage;
      if (faction.parents.length === 1) return findModelByKey(era, modelIndex, faction.parents[0]);
      if (faction.parents.length > 0) {
        const list = [];
        for (const parent of faction.parents) {
          const found = findModelByKey(era, modelIndex, parent);
          if (found) list.push(found);
        }
        return mergeFactionAvailability(faction.key, list);
      }
      return unitAvailability.get('General') || null;
    }

    function findModelForYear(era, modelIndex, faction, year) {
      const rating = findModel(era, modelIndex, faction);
      return (rating && year >= rating.startYear) ? rating : null;
    }

    /** AbstractUnitRecord.calcAvailability */
    function calcAvailability(rating, equipRating, ratingLevels, year, introYear) {
      let value = rating.hasMultipleRatings() ? rating.getAvailabilityAt(equipRating)
        : rating.adjustForRating(equipRating, ratingLevels);
      if (year === introYear - 1) value -= 2;
      if (year === introYear) value -= 1;
      return Math.max(value, 0);
    }

    const clamp = (value, min, max) => {
      if (min > max) throw new Error(`${min} > ${max}`);
      return Math.min(max, Math.max(min, value));
    };

    function isActiveInYear(faction, year) {
      for (const [first, last] of faction.activeYears) if (year >= first && year <= last) return true;
      return false;
    }

    function eraSettings(faction, era) {
      return faction.eras[String(era)] || {};
    }

    function findPctTech(faction, category, era, rating) {
      const list = (eraSettings(faction, era).pctTech || {})[category];
      if (!list || rating < 0 || rating >= list.length) return null;
      const value = list[rating];
      return value === undefined ? null : value;
    }

    // ---------------------------------------------------------------- generateTable
    /**
     * RATGenerator.generateTable. Returns { units: [[modelIndex, weight]], salvage: [[factionKey, weight]] }, or
     * { error } where MegaMek itself would throw.
     */
    function generateTable(params) {
      const fRec = getFaction(params.faction);
      if (!fRec) return { units: [], salvage: [] };
      const user = params.user ? getFaction(params.user) : fRec;
      const unitType = params.unitType;
      const year = params.year;
      const weightClasses = params.weightClasses || [];
      const movementModes = params.movementModes || [];
      const networkMask = params.networkMask || 0;
      const roles = params.roles || [];
      const roleStrictness = params.roleStrictness === undefined ? 1 : params.roleStrictness;
      const rating = params.rating === undefined ? null : params.rating;

      let currentEra = floorEra(year);
      if (currentEra === null) currentEra = eras[0];
      let nextEra = null;
      if (!eraSet.has(year)) nextEra = ceilingEra(year);
      if (nextEra === null) nextEra = currentEra;

      let ratingLevel = -1;
      const factionRatings = fRec.ratingSystem;
      const numRatingLevels = factionRatings.length;
      if (rating === null && fRec.ratingLevels.length === 1) ratingLevel = factionRatings.indexOf(fRec.ratingLevels[0]);
      if (rating !== null && numRatingLevels > 1) ratingLevel = factionRatings.indexOf(rating);

      const unitWeights = new Map(); // modelIndex -> weight
      let chassisWeightTotal = 0.0;
      const currentData = eraData(currentEra);

      for (const chassisIndex of currentData.chassis.keys()) {
        const curChassis = chassis[chassisIndex];
        if (curChassis.introYear > year + 1) continue;
        let chassisType = curChassis.unitType;
        if (chassisType === UT.AERO || chassisType === UT.AEROSPACE_FIGHTER) chassisType = UT.AEROSPACE_FIGHTER;
        if (chassisType !== unitType
          && !(unitType === UT.TANK && chassisType === UT.VTOL && movementModes.includes('VTOL'))) continue;
        if (weightClasses.length > 0) {
          let validChassis = false;
          for (const modelIndex of curChassis.models) {
            if (weightClasses.includes(models[modelIndex].weightClass)) { validChassis = true; break; }
          }
          if (!validChassis) continue;
        }

        const chassisAv = findChassis(currentEra, chassisIndex, fRec, year);
        if (!chassisAv) continue;
        if (chassisAv.availability <= 0) continue;

        let chassisAdjRating;
        if (year !== currentEra && year !== nextEra) {
          const chassisNextAv = findChassis(nextEra, chassisIndex, fRec, nextEra);
          const interpolationStart = clamp(curChassis.introYear, currentEra, year);
          chassisAdjRating = calcAvailability(chassisAv, ratingLevel, numRatingLevels, interpolationStart, curChassis.introYear);
          let chassisNextAdj = 0.0;
          if (chassisNextAv) chassisNextAdj = calcAvailability(chassisNextAv, ratingLevel, numRatingLevels, nextEra, curChassis.introYear);
          if (chassisAdjRating !== chassisNextAdj) {
            chassisAdjRating = interpolate(chassisAdjRating, chassisNextAdj, interpolationStart, nextEra, year);
          }
        } else {
          chassisAdjRating = calcAvailability(chassisAv, ratingLevel, numRatingLevels, year, curChassis.introYear);
        }

        if (chassisAdjRating > 0) {
          const validModels = [];
          for (const modelIndex of curChassis.models) {
            const model = models[modelIndex];
            if (model.introYear > year + 1) continue;
            if (weightClasses.length > 0 && !weightClasses.includes(model.weightClass)) continue;
            if (movementModes.length > 0 && !movementModes.includes(model.movementMode)) continue;
            if ((networkMask & model.networkMask) !== networkMask) continue;
            validModels.push(model);
          }
          const modelWeights = new Map();
          const totalWeight = totalModelWeight(validModels, currentEra, year, nextEra, curChassis.omni ? user : fRec,
            roles, roleStrictness, ratingLevel, numRatingLevels, modelWeights);
          if (totalWeight > 0 && modelWeights.size > 0) {
            const chassisWeight = calcWeight(chassisAdjRating);
            let hasModels = false;
            for (const model of validModels) {
              if (!modelWeights.has(model.index)) continue;
              const curWeight = chassisWeight * modelWeights.get(model.index) / totalWeight;
              if (curWeight > 0) { unitWeights.set(model.index, curWeight); hasModels = true; }
            }
            if (hasModels) chassisWeightTotal += chassisWeight;
          }
        }
      }

      if (unitWeights.size === 0 || chassisWeightTotal === 0.0) return { units: [], salvage: [] };
      for (const [modelIndex, weight] of unitWeights) unitWeights.set(modelIndex, 100.0 * weight / chassisWeightTotal);

      // Weight class rebalance
      if (weightClasses.length > 1
        && (unitType === UT.MEK || unitType === UT.TANK || unitType === UT.AEROSPACE_FIGHTER) && roles.length === 0) {
        const settings = eraSettings(fRec, currentEra);
        if ((settings.weightDistributionFails || []).includes(unitType)) {
          return { error: 'MegaMek fails here: this faction\'s weight distribution cannot be read for these eras (parent factions disagree)' };
        }
        const distribution = (settings.weightDistribution || {})[String(unitType)];
        if (distribution && distribution.length > 0) {
          const wcdIndex = [0, 0, 1, 2, 3, 3];
          let totalTableWeight = 0;
          for (const weight of unitWeights.values()) totalTableWeight += weight;
          let totalWCDWeights = 0;
          for (const weightClass of weightClasses) {
            if (wcdIndex[weightClass] === undefined) return { error: 'MegaMek fails here: weight class outside the distribution' };
            if (wcdIndex[weightClass] < distribution.length) totalWCDWeights += distribution[wcdIndex[weightClass]];
          }
          if (totalWCDWeights > 0) {
            const groups = new Map();
            for (const modelIndex of unitWeights.keys()) {
              const group = wcdIndex[models[modelIndex].weightClass];
              if (group === undefined) return { error: 'MegaMek fails here: unit weight class outside the distribution' };
              if (!groups.has(group)) groups.set(group, []);
              groups.get(group).push(modelIndex);
            }
            for (const [group, members] of groups) {
              let groupWeight = 0;
              for (const modelIndex of members) groupWeight += unitWeights.get(modelIndex);
              if (groupWeight > 0) {
                if (group >= distribution.length) return { error: 'MegaMek fails here: weight distribution too short' };
                const adjust = totalTableWeight * distribution[group] / (groupWeight * totalWCDWeights);
                for (const modelIndex of members) unitWeights.set(modelIndex, unitWeights.get(modelIndex) * adjust);
              }
            }
          }
        }
      }

      // Salvage
      const salvageWeights = new Map(); // factionKey -> weight
      const pctSalvage = eraSettings(fRec, currentEra).pctSalvage;
      if (pctSalvage !== null && pctSalvage !== undefined) {
        const salvageEntries = new Map();
        const current = eraSettings(fRec, currentEra).salvage || {};
        const next = eraSettings(fRec, nextEra).salvage || {};
        for (const [key, value] of Object.entries(current)) {
          salvageEntries.set(key, currentEra === year ? value
            : interpolate(value, next[key] === undefined ? null : next[key], currentEra, nextEra, year));
        }
        if (nextEra !== currentEra) {
          for (const [key, value] of Object.entries(next)) {
            if (!salvageEntries.has(key)) salvageEntries.set(key, interpolate(0.0, value, currentEra, nextEra, year));
          }
        }
        let totalTableWeight = 0;
        for (const weight of unitWeights.values()) totalTableWeight += weight;
        let overallSalvage = pctSalvage;
        if (overallSalvage >= 100) {
          overallSalvage = totalTableWeight;
          unitWeights.clear();
        } else {
          overallSalvage = totalTableWeight * overallSalvage / 100.0;
        }
        let totalFactionWeight = 0;
        for (const value of salvageEntries.values()) totalFactionWeight += value;
        for (const [key, value] of salvageEntries) {
          if (factions.has(key)) salvageWeights.set(key, overallSalvage * value / totalFactionWeight);
        }
      }

      if (ratingLevel >= 0
        && (unitType === UT.MEK || unitType === UT.AEROSPACE_FIGHTER || unitType === UT.TANK || unitType === UT.VTOL)
        && roles.length === 0) {
        adjustForRating(fRec, unitType, year, ratingLevel, unitWeights, salvageWeights, currentEra, nextEra);
      }

      // Scale to keep weights between 0.5 and 1000, then round as Java does
      let min = Infinity;
      let max = -Infinity;
      for (const weight of [...salvageWeights.values(), ...unitWeights.values()]) {
        if (weight > 0) { min = Math.min(min, weight); max = Math.max(max, weight); }
      }
      let adj = 1.0;
      if (min < 0.5 || max > 1000) {
        adj = 0.5 / min;
        if (max * adj > 1000.0) adj = 1000.0 / max;
      }
      const result = { units: [], salvage: [] };
      for (const [key, weight] of salvageWeights) {
        const rounded = javaInt(weight * adj + 0.5);
        if (rounded > 0) result.salvage.push([key, rounded]);
      }
      for (const [modelIndex, weight] of unitWeights) {
        const rounded = javaInt(weight * adj + 0.5);
        if (rounded > 0) result.units.push([modelIndex, rounded]);
      }
      return result;
    }

    function totalModelWeight(validModels, currentEra, exactYear, nextEra, fRec, roles, roleStrictness, equipRating,
      numRatingLevels, weightData) {
      let total = 0;
      weightData.clear();
      for (const model of validModels) {
        if (fRec && model.excludedFactions.includes(fRec.key)) continue;
        const rating = findModelForYear(currentEra, model.index, fRec, exactYear);
        if (!rating || rating.availability <= 0) continue;
        let adjRating;
        if (exactYear > currentEra && currentEra !== nextEra) {
          const nextRating = findModelForYear(nextEra, model.index, fRec, nextEra);
          const interpolationStart = clamp(model.introYear, currentEra, exactYear);
          adjRating = calcAvailability(rating, equipRating, numRatingLevels, interpolationStart, model.introYear);
          let next = 0.0;
          if (nextRating) next = calcAvailability(nextRating, equipRating, numRatingLevels, nextEra, model.introYear);
          if (adjRating !== next) {
            adjRating = adjRating + (next - adjRating) * (exactYear - interpolationStart) / (nextEra - interpolationStart);
          }
        } else {
          adjRating = calcAvailability(rating, equipRating, numRatingLevels, exactYear, model.introYear);
        }
        if (adjRating <= 0) continue;
        const roleRating = adjustAvailabilityByRole(adjRating, roles, model, roleStrictness);
        if (roleRating === null || roleRating <= 0) continue;
        const weight = calcWeight(roleRating);
        total += weight;
        weightData.set(model.index, weight);
      }
      return total;
    }

    function adjustForRating(fRec, unitType, year, rating, unitWeights, salvageWeights, currentEra, nextEra) {
      const model = (index) => models[index];
      let totalWeight = 0.0, totalOmniWeight = 0.0, totalClanWeight = 0.0, totalSLWeight = 0.0, totalOtherWeight = 0.0;
      for (const [index, weight] of unitWeights) {
        totalWeight += weight;
        if (model(index).omni) totalOmniWeight += weight;
        if (model(index).starLeague) totalSLWeight += weight;
        else if (!model(index).mixedOrClanTech) totalOtherWeight += weight;
      }
      let pctOmni = null, pctSL = null, pctClan = null, pctOther = null;
      const pct = (category) => interpolate(findPctTech(fRec, category, currentEra, rating),
        findPctTech(fRec, category, nextEra, rating), currentEra, nextEra, year);
      if (unitType === UT.MEK) { pctOmni = pct('OMNI'); pctClan = pct('CLAN'); pctSL = pct('IS_ADVANCED'); }
      if (unitType === UT.AEROSPACE_FIGHTER) { pctOmni = pct('OMNI_AERO'); pctClan = pct('CLAN_AERO'); pctSL = pct('IS_ADVANCED_AERO'); }
      if (unitType === UT.TANK || unitType === UT.VTOL) { pctClan = pct('CLAN_VEE'); pctSL = pct('IS_ADVANCED_VEE'); }

      const margin = (name) => interpolate(eraSettings(fRec, currentEra)[name] || 0, eraSettings(fRec, nextEra)[name] || 0,
        currentEra, nextEra, year);

      if (pctOmni !== null) {
        const omniMargin = margin('omniMargin');
        if (omniMargin !== null && omniMargin > 0) {
          const current = 100.0 * totalOmniWeight / totalWeight;
          if (current < pctOmni - omniMargin) pctOmni -= omniMargin;
          else if (current > pctOmni + omniMargin) pctOmni += omniMargin;
        }
      }

      let totalWeightPostMod = 0.0;
      if ((unitType === UT.MEK || unitType === UT.AEROSPACE_FIGHTER) && pctOmni !== null) {
        const omniPctDifference = pctOmni - (100.0 * totalOmniWeight / totalWeight);
        if (Math.abs(omniPctDifference) > MIN_OMNI_DIFFERENCE && totalOmniWeight > 0.0 && pctOmni >= 0.0) {
          const totalNonOmniWeight = totalOmniWeight - totalWeight;
          totalClanWeight = 0.0; totalSLWeight = 0.0; totalOtherWeight = 0.0;
          let totalOmniWeightPostMod = 0.0;
          for (const index of unitWeights.keys()) {
            let curWeight = unitWeights.get(index);
            if (model(index).omni) curWeight = pctOmni > 0.0 ? curWeight + curWeight * omniPctDifference / totalOmniWeight : 0.0;
            else curWeight = pctOmni < 100.0 ? curWeight + curWeight * omniPctDifference / totalNonOmniWeight : 0.0;
            unitWeights.set(index, curWeight);
            totalWeightPostMod += curWeight;
            if (model(index).mixedOrClanTech) totalClanWeight += curWeight;
            else if (model(index).starLeague) totalSLWeight += curWeight;
            else totalOtherWeight += curWeight;
            if (model(index).omni) totalOmniWeightPostMod += curWeight;
          }
          if (totalOmniWeightPostMod > 0.0) totalOmniWeight = totalOmniWeightPostMod;
        }
        if (totalWeightPostMod > 0.0) totalWeight = totalWeightPostMod;
      }

      let techMargin = null;
      if (pctSL !== null || pctClan !== null) {
        pctOther = 100.0;
        if (pctSL !== null) pctOther -= pctSL;
        if (pctClan !== null) pctOther -= pctClan;
        techMargin = margin('techMargin');
        if (techMargin !== null && techMargin > 0) {
          if (pctClan !== null) {
            const current = 100.0 * totalClanWeight / totalWeight;
            if (current < pctClan - techMargin) pctClan -= techMargin;
            else if (current > pctClan + techMargin) pctClan += techMargin;
          }
          if (pctSL !== null) {
            const current = 100.0 * totalSLWeight / totalWeight;
            if (current < pctSL - techMargin) pctSL -= techMargin;
            else if (current > pctSL + techMargin) pctSL += techMargin;
          }
        }
        const upgradeMargin = margin('upgradeMargin');
        if (upgradeMargin !== null && upgradeMargin > 0) {
          const current = 100.0 * (totalWeight - totalClanWeight - totalSLWeight) / totalWeight;
          if (current < pctOther - upgradeMargin) pctOther -= upgradeMargin;
          else if (current > pctOther + upgradeMargin) pctOther += upgradeMargin;
          if (techMargin !== null && upgradeMargin <= techMargin) {
            if (pctClan === null || pctClan === 0) pctSL = 100.0 - pctOther;
            else if (pctSL === null || pctSL === 0) pctClan = 100.0 - pctOther;
            else { pctSL = (100.0 - pctOther) * pctSL / (pctSL + pctClan); pctClan = 100.0 - pctOther - pctSL; }
          }
        }
      }

      if (pctSL !== null) {
        const slPctDifference = pctSL - (100.0 * totalSLWeight / totalWeight);
        if (Math.abs(slPctDifference) > MIN_SL_DIFFERENCE && totalSLWeight > 0.0) {
          const totalNonSLWeight = totalSLWeight - totalWeight;
          totalClanWeight = 0.0; totalOtherWeight = 0.0; totalOmniWeight = 0.0; totalWeightPostMod = 0.0;
          let totalSLWeightPostMod = 0.0;
          for (const index of unitWeights.keys()) {
            let curWeight = unitWeights.get(index);
            if (model(index).starLeague) curWeight = pctSL > 0.0 ? curWeight + curWeight * slPctDifference / totalSLWeight : 0.0;
            else curWeight = pctSL < 100.0 ? curWeight + curWeight * slPctDifference / totalNonSLWeight : 0.0;
            unitWeights.set(index, curWeight);
            totalWeightPostMod += curWeight;
            if (model(index).mixedOrClanTech) totalClanWeight += curWeight;
            else if (model(index).starLeague) totalSLWeightPostMod += curWeight;
            else totalOtherWeight += curWeight;
            if (model(index).omni) totalOmniWeight += curWeight;
          }
          if (totalWeightPostMod > 0.0) totalWeight = totalWeightPostMod;
          if (totalSLWeightPostMod > 0.0) totalSLWeight = totalSLWeightPostMod;
        }
      }

      let clanSalvageWeight = 0.0;
      if (pctClan !== null) {
        if (!fRec.clan) {
          for (const [key, weight] of salvageWeights) if (factions.get(key).clan) clanSalvageWeight += weight;
        }
        const clanPctDifference = pctClan - (100.0 * Math.min(totalWeight, totalClanWeight + clanSalvageWeight) / totalWeight);
        if (Math.abs(clanPctDifference) > MIN_CLAN_DIFFERENCE && totalClanWeight > 0.0) {
          const totalNonClanWeight = Math.min(totalWeight, totalClanWeight + clanSalvageWeight) - totalWeight;
          totalSLWeight = 0.0; totalOtherWeight = 0.0; totalOmniWeight = 0.0; totalWeightPostMod = 0.0;
          let totalClanWeightPostMod = 0.0;
          for (const index of unitWeights.keys()) {
            let curWeight = unitWeights.get(index);
            if (model(index).mixedOrClanTech) curWeight = pctClan > 0.0 ? curWeight + curWeight * clanPctDifference / totalClanWeight : 0.0;
            else curWeight = pctClan < 100.0 ? curWeight + curWeight * clanPctDifference / totalNonClanWeight : 0.0;
            unitWeights.set(index, curWeight);
            totalWeightPostMod += curWeight;
            if (model(index).mixedOrClanTech) totalClanWeightPostMod += curWeight;
            else if (model(index).starLeague) totalSLWeight += curWeight;
            else totalOtherWeight += curWeight;
            if (model(index).omni) totalOmniWeight += curWeight;
          }
          if (totalWeightPostMod > 0.0) totalWeight = totalWeightPostMod;
          if (totalClanWeightPostMod > 0.0) totalClanWeight = totalClanWeightPostMod;
        }
      }

      if (pctSL !== null && pctClan !== null && (pctOther === 0.0 || pctSL + pctClan >= 100.0)) {
        const pctOtherDifference = pctOther - 100.0 * totalOtherWeight / totalWeight;
        const totalAdvancedWeight = totalOtherWeight - totalWeight;
        for (const index of unitWeights.keys()) {
          let curWeight = unitWeights.get(index);
          if (!model(index).starLeague && !model(index).mixedOrClanTech && curWeight > 0.0) curWeight = 0.0;
          else curWeight = curWeight + curWeight * pctOtherDifference / totalAdvancedWeight;
          unitWeights.set(index, curWeight);
        }
      }
    }

    // ---------------------------------------------------------------- UnitTable
    const roleOrderIndex = new Map(inputs.roles.map((role, index) => [role, index]));
    const roleOrder = (role) => (roleOrderIndex.has(role) ? roleOrderIndex.get(role) : Number.MAX_SAFE_INTEGER);

    /** UnitTable: the faction must be active; salvagePct is a whole-number share. */
    function findTable(params) {
      const faction = getFaction(params.faction);
      const empty = { units: [], salvage: [], salvagePct: 0, unitTotal: 0, salvageTotal: 0 };
      if (!faction || !isActiveInYear(faction, params.year)) return empty;
      // UnitTable's Parameters keeps roles in an EnumSet, so MegaMek applies them in MissionRole declaration order
      // (the order of inputs.roles), not the order they were picked. Some role rules clamp, so the order matters.
      const roles = (params.roles || []).slice().sort((a, b) => roleOrder(a) - roleOrder(b));
      const table = generateTable({ ...params, roles });
      if (table.error) return { ...empty, error: table.error };
      let unitTotal = 0;
      let salvageTotal = 0;
      for (const [, weight] of table.units) unitTotal += weight;
      for (const [, weight] of table.salvage) salvageTotal += weight;
      const salvagePct = (salvageTotal + unitTotal > 0) ? javaInt(salvageTotal * 100 / (salvageTotal + unitTotal)) : 0;
      return { units: table.units, salvage: table.salvage, salvagePct, unitTotal, salvageTotal };
    }

    // ---------------------------------------------------------------- editing (test weights)
    /**
     * Sets or clears one availability code, as the RAT Editor does. level is 'chassis' or 'model'; code is the part
     * after the faction, such as "7", "7+", "5:3055" or "!A:5!B:3"; null removes it.
     */
    function setCode(level, era, index, factionCode, code) {
      const data = eraData(era)[level === 'chassis' ? 'chassis' : 'models'];
      if (!data.has(index)) data.set(index, new Map());
      const ratings = data.get(index);
      if (code === null || code === undefined || String(code).trim() === '') {
        ratings.delete(factionCode);
        return null;
      }
      const text = String(code).trim();
      let startYear = era;
      let value = text;
      const yearMatch = text.match(/^(\d+[+-]?):(\d+)$/);
      if (yearMatch) { value = yearMatch[1]; startYear = Number(yearMatch[2]); }
      const rating = buildRating(era, factionCode, value, startYear);
      if (!rating) throw new Error(`Unknown faction ${factionCode}`);
      ratings.set(factionCode, rating);
      return rating;
    }

    function getCodes(level, era, index) {
      const ratings = eraData(era)[level === 'chassis' ? 'chassis' : 'models'].get(index);
      const result = [];
      if (ratings) {
        for (const rating of ratings.values()) {
          let code = rating.getAvailabilityCode();
          if (rating.startYear !== rating.era) code += `:${rating.startYear}`;
          result.push({ faction: rating.faction, code });
        }
      }
      return result;
    }

    /** Throws away every edit and goes back to the loaded data. */
    function resetEdits() {
      eraIndex.clear();
    }

    /** Which faction code and era answered a lookup, for showing where a value comes from. */
    function explain(level, era, index, factionKey, year) {
      const faction = getFaction(factionKey);
      const rating = level === 'chassis' ? findChassis(era, index, faction, year) : findModelForYear(era, index, faction, year);
      return rating ? { faction: rating.faction, code: rating.getAvailabilityCode(), startYear: rating.startYear } : null;
    }

    return {
      eras, models, chassis, factions, getFaction, isActiveInYear,
      generateTable, findTable, setCode, getCodes, resetEdits, explain,
      chassisByKey: new Map(chassis.map((record) => [record.key, record])),
    };
  }

  return { create, AvailabilityRating, adjustAvailabilityByRole, roleFitsUnitType, filterOptions, UT };
}));
