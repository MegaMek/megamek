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

import megamek.common.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesScanning;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * Scanning under the Core Rules (p.233 Scanning, p.113 Sensor Checks). A scan is a sensor check: a Piloting Skill
 * Roll that ignores every ordinary Piloting modifier, at +3 for scanning, +2 with one sensor critical hit (two
 * hits prevent sensor checks altogether), minus the level of a working active probe that is not inside hostile
 * ECM, and +2 against a target with an active stealth system. The range is 2 hexes, or the probe's range. Infantry
 * scan the hex they stand in automatically. Aerospace units - fighters, small craft and DropShips - cannot scan; a
 * VTOL is a vehicle and scans like one, airborne or not.
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
    /** Scanning range without a working active probe. */
    public static final int DEFAULT_SCANNING_RANGE = 2;

    @Override
    public int scanningRange(Entity scanner) {
        if (refusalReason(scanner) != null) {
            return 0;
        }
        int probeRange = scanner.hasBAP(true) ? scanner.getBAPRange() : 0;
        return Math.max(DEFAULT_SCANNING_RANGE, probeRange);
    }

    @Override
    public TargetRoll scanTargetRoll(Entity scanner, Targetable target) {
        String refusal = refusalReason(scanner);
        if (refusal != null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, refusal);
        }
        if (scanner.isInfantry() && isInOwnHex(scanner, target)) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("RulesScanning.infantryOwnHex"));
        }
        TargetRoll roll = new TargetRoll(scanner.getCrew().getPiloting(),
              Messages.getString("RulesScanning.pilotingSkill"));
        roll.addModifier(SCAN_MODIFIER, Messages.getString("RulesScanning.scanning"));
        if (sensorCriticalHits(scanner) > 0) {
            roll.addModifier(SENSOR_HIT_MODIFIER, Messages.getString("RulesScanning.sensorHit"));
        }
        int probeLevel = activeProbeLevel(scanner);
        if (probeLevel > PROBE_LEVEL_NONE) {
            roll.addModifier(-probeLevel, Messages.getString("RulesScanning.activeProbe", probeLevel));
        }
        boolean isStealthyTarget = (target instanceof Entity targetEntity) && targetEntity.isStealthActive();
        if (isStealthyTarget) {
            roll.addModifier(STEALTH_MODIFIER, Messages.getString("RulesScanning.stealth"));
        }
        return roll;
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
        if (sensorCriticalHits(scanner) >= BLOCKING_SENSOR_HITS) {
            return Messages.getString("RulesScanning.sensorsOut");
        }
        return null;
    }
}
