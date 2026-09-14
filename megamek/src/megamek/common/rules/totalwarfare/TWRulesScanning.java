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
package megamek.common.rules.totalwarfare;

import megamek.common.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.compute.ComputeECM;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesScanning;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;

/**
 * Scanning under Total Warfare (p.187 Scanning). Any Mek, vehicle, battle armour or grounded aerospace unit with
 * working sensors scans one target in line of sight per turn, at any distance, with no roll: the target's
 * controller simply answers. A target sitting inside ECM hostile to the scanner forces a 2D6 roll of 8 or better
 * first. A unit whose sensors have been hit, or conventional infantry, which has no sensors, is limited to visual
 * inspection: 3 hexes, automatic, and ECM has no effect on it. An airborne aerospace unit cannot scan.
 */
public class TWRulesScanning extends RulesScanning {

    /** Visual inspection reaches this far (Total Warfare p.187). */
    public static final int VISUAL_INSPECTION_RANGE = 3;
    /** A scan of a target inside hostile ECM needs this on 2D6 (Total Warfare p.187). */
    public static final int ECM_TARGET_NUMBER = 8;
    /** Standard sensors reach anything in line of sight; the caller bounds the search by the board. */
    public static final int SENSOR_RANGE = UNLIMITED_RANGE;

    @Override
    public int scanningRange(Entity scanner, @Nullable Targetable target) {
        if (refusalReason(scanner) != null) {
            return 0;
        }
        return usesVisualInspectionOnly(scanner) ? VISUAL_INSPECTION_RANGE : SENSOR_RANGE;
    }

    @Override
    public TargetRoll scanTargetRoll(Entity scanner, Targetable target) {
        String refusal = refusalReason(scanner);
        if (refusal != null) {
            return new TargetRoll(TargetRoll.IMPOSSIBLE, refusal);
        }
        if (usesVisualInspectionOnly(scanner)) {
            // no sensors in play, so nothing for ECM to jam
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("RulesScanning.visualInspection"));
        }
        if ((target instanceof Entity targetEntity) && isInsideHostileEcm(scanner, targetEntity)) {
            return new TargetRoll(ECM_TARGET_NUMBER, Messages.getString("RulesScanning.targetInEcm"));
        }
        return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, Messages.getString("RulesScanning.standardSensors"));
    }

    /**
     * @param scanner the unit that wants to scan
     *
     * @return {@code true} when the unit has no usable sensors and falls back to visual inspection: conventional
     *       infantry, or any unit with a sensor critical hit
     */
    public static boolean usesVisualInspectionOnly(Entity scanner) {
        return scanner.isConventionalInfantry() || (sensorCriticalHits(scanner) > 0);
    }

    /**
     * Seam for tests: whether ECM hostile to the scanner covers the target's hex.
     *
     * @param scanner the scanning unit
     * @param target  the unit being scanned
     *
     * @return {@code true} when the target sits inside ECM that is hostile to the scanner
     */
    protected boolean isInsideHostileEcm(Entity scanner, Entity target) {
        return (target.getPosition() != null)
              && ComputeECM.isAffectedByECM(scanner, target.getPosition(), target.getPosition());
    }

    /**
     * @param scanner the unit that wants to scan
     *
     * @return why the unit cannot scan at all, or {@code null} when it can
     */
    private static @Nullable String refusalReason(Entity scanner) {
        if (scanner.getCrew() == null) {
            return Messages.getString("RulesScanning.noCrew");
        }
        if (scanner.isAero() && scanner.isAirborne()) {
            return Messages.getString("RulesScanning.airborne");
        }
        return null;
    }
}
