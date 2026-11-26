/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.rules.core;

import jakarta.annotation.Nullable;
import megamek.common.Messages;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BFSSpecialType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.EquipmentActivation;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesScanning;
import megamek.common.units.Entity;
import megamek.common.units.ProtoMek;
import megamek.common.units.Targetable;

/**
 * Scanning under the Core Rules (p.233 Scanning, p.113 Sensor Checks). A scan is a sensor check: a Piloting Skill
 * Roll that ignores every ordinary Piloting modifier, at +3 for scanning, +2 with one sensor critical hit (two
 * hits prevent sensor checks altogether), minus the level of a working active probe that is not inside hostile
 * ECM, and +2 against a target with an active stealth system. The range is 2 hexes, or the probe's range. A probe
 * is negated when either the probe or the target sits inside hostile ECM (p.197); a Bloodhound ignores ordinary ECM
 * and is negated only by Angel ECM. Infantry follow the infantry Asset rules: they scan the hex they stand in
 * automatically and anything else at TN 8 plus the probe modifier. Units without Piloting Skill scan with Gunnery. Aerospace units - fighters,
 * small craft and DropShips - cannot scan; a VTOL is a vehicle and scans like one, airborne or not.
 */
public class CoreRulesScanning extends RulesScanning {

    /** Every scan is a sensor check at this modifier (Core Rules p.233). */
    public static final int SCAN_MODIFIER = 3;
    /** One critical hit to the sensors adds this to every sensor check (Core Rules p.113). */
    public static final int SENSOR_HIT_MODIFIER = 2;
    /** This many sensor critical hits prevent sensor checks altogether. */
    public static final int BLOCKING_SENSOR_HITS = 2;
    /** Scanning a target whose stealth system is active (Core Rules p.233). */
    public static final int STEALTH_MODIFIER = 2;
    /** Ground Assets, and non-Asset infantry, scan at this TN regardless of skill (Core Rules p.233). */
    public static final int GROUND_ASSET_SCAN_TARGET = 8;
    /** Scanning range without a working active probe. */
    public static final int DEFAULT_SCANNING_RANGE = 2;
    /** "Assume an Asset with the Probe Special has a probe range of 4" (Core p.150). */
    public static final int PROBE_SPECIAL_RANGE = 4;

    @Override
    public int scanningRange(Entity scanner, @Nullable Targetable target) {
        if (refusalReason(scanner) != null) {
            return 0;
        }
        int probeRange = !hasWorkingProbeAgainst(scanner, target) ? 0
              : hasProbeSpecial(scanner) ? PROBE_SPECIAL_RANGE : scanner.getBAPRange();
        return Math.max(DEFAULT_SCANNING_RANGE, probeRange);
    }

    /**
     * Whether the unit's active probe works against this target. {@code hasBAP(true)} already answers for the probe's
     * own hex and for the Bloodhound's immunity to ordinary ECM; this adds the target's end of the line (Core Rules
     * p.197): a standard probe is negated by any hostile ECM covering the target, a Bloodhound only by Angel ECM.
     *
     * @param scanner the scanning unit
     * @param target  the target, or {@code null} to ask only about the probe's own hex
     *
     * @return {@code true} when the probe gives its range and its target number bonus against this target
     */
    boolean hasWorkingProbeAgainst(Entity scanner, @Nullable Targetable target) {
        // An Asset's Probe Special is a standard probe, negated by hostile ECM like any other (Core pp.150, 233)
        boolean probeWorks = hasProbeSpecial(scanner)
              ? !ComputeECM.isAffectedByECM(scanner, scanner.getPosition(), scanner.getPosition())
              : scanner.hasBAP(true);
        if (!probeWorks) {
            return false;
        }
        if ((target == null) || (target.getPosition() == null)) {
            return true;
        }
        if (hasBloodhound(scanner)) {
            return !isTargetInsideHostileAngelEcm(scanner, target);
        }
        return !isTargetInsideHostileEcm(scanner, target);
    }

    private static boolean hasProbeSpecial(Entity scanner) {
        return (scanner instanceof BattlefieldSupportAsset asset) && asset.hasSpecial(BFSSpecialType.PROBE);
    }

    /**
     * @param scanner the scanning unit
     *
     * @return {@code true} when the unit's working probe is a Bloodhound
     */
    private static boolean hasBloodhound(Entity scanner) {
        MiscMounted probe = EquipmentActivation.activeProbe(scanner);
        return probe != null && probe.getType().hasFlag(MiscType.F_BLOODHOUND);
    }

    /** Seam for tests: whether ECM hostile to the scanner covers the target's hex. */
    protected boolean isTargetInsideHostileEcm(Entity scanner, Targetable target) {
        return ComputeECM.isAffectedByECM(scanner, target.getPosition(), target.getPosition());
    }

    /** Seam for tests: whether Angel ECM hostile to the scanner covers the target's hex. */
    protected boolean isTargetInsideHostileAngelEcm(Entity scanner, Targetable target) {
        return ComputeECM.isAffectedByAngelECM(scanner, target.getPosition(), target.getPosition());
    }

    @Override
    public TargetRoll scanTargetRoll(Entity scanner, Targetable target) {
        String refusal = refusalReason(scanner);
        if (refusal != null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, refusal);
        }
        TargetRoll roll;
        if (scanner.isInfantry() || scanner.isBattlefieldSupportAsset()) {
            // Ground Assets, and non-Asset infantry, scan at TN 8 regardless of skill, modified only by a probe or the
            // PRB Special; infantry automatically scan "any building and/or hex they occupy" (Core pp.150, 233)
            if (isInfantryScanner(scanner) && isOwnHexOrBuilding(scanner, target)) {
                return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("RulesScanning.infantryOwnHex"));
            }
            roll = new TargetRoll(GROUND_ASSET_SCAN_TARGET, Messages.getString("RulesScanning.infantryScan"));
        } else {
            // ProtoMeks have no Piloting Skill (TW p.187); such units scan with Gunnery (Core p.233)
            roll = (scanner instanceof ProtoMek)
                  ? new TargetRoll(scanner.getCrew().getGunnery(), Messages.getString("RulesScanning.gunnerySkill"))
                  : new TargetRoll(scanner.getCrew().getPiloting(), Messages.getString("RulesScanning.pilotingSkill"));
            roll.addModifier(SCAN_MODIFIER, Messages.getString("RulesScanning.scanning"));
            if (sensorCriticalHits(scanner) > 0) {
                roll.addModifier(SENSOR_HIT_MODIFIER, Messages.getString("RulesScanning.sensorHit"));
            }
        }
        int probeLevel = !hasWorkingProbeAgainst(scanner, target) ? PROBE_LEVEL_NONE
              : hasProbeSpecial(scanner) ? PROBE_LEVEL_STANDARD : activeProbeLevel(scanner);
        if (probeLevel > PROBE_LEVEL_NONE) {
            roll.addModifier(-probeLevel, Messages.getString("RulesScanning.activeProbe", probeLevel));
        }
        boolean isStealthyTarget = (target instanceof Entity targetEntity) && hasActiveStealthSystem(targetEntity);
        if (isStealthyTarget) {
            roll.addModifier(STEALTH_MODIFIER, Messages.getString("RulesScanning.stealth"));
        }
        return roll;
    }

    /**
     * @param target the unit being scanned
     *
     * @return {@code true} when the target is running any stealth system, which the Core Rulebook (p. 209) takes to
     *       mean stealth armour, a Null Signature System, a Void Signature System or a Chameleon Light Polarisation
     *       Shield, rather than stealth armour alone
     */
    private static boolean isInfantryScanner(Entity scanner) {
        if (scanner instanceof BattlefieldSupportAsset asset) {
            return (asset.getAssetType() == BFSAssetType.CONV_INFANTRY)
                  || (asset.getAssetType() == BFSAssetType.BATTLE_ARMOR);
        }
        return scanner.isInfantry();
    }

    private static boolean hasActiveStealthSystem(Entity target) {
        return target.isStealthActive()
              || target.isNullSigActive()
              || target.isVoidSigActive()
              || target.isChameleonShieldActive();
    }

    /**
     * @param scanner the unit that wants to scan
     *
     * @return why the unit cannot make a sensor check at all, or {@code null} when it can
     */
    private static @Nullable String refusalReason(Entity scanner) {
        if (scanner.isAero()) {
            return Messages.getString("RulesScanning.aerospace");
        }
        if (scanner.getCrew() == null) {
            return Messages.getString("RulesScanning.noCrew");
        }
        // "A scan is a sensor check" (Core p.233), and shutdown Meks "cannot make sensor checks" (Core p.105)
        if (scanner.isShutDown()) {
            return Messages.getString("RulesScanning.shutdown");
        }
        if (sensorCriticalHits(scanner) >= BLOCKING_SENSOR_HITS) {
            return Messages.getString("RulesScanning.sensorsOut");
        }
        return null;
    }
}
