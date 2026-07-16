package megamek.common.rules.totalwarfare;

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

import megamek.common.board.Coords;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Sensor;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.core.CoreRulesEquipment;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

import java.util.ArrayList;

public class TWRulesEquipment extends CoreRulesEquipment {
    // AMS can shoot once
    @Override
    public boolean getAMSMultiShot() { return false;}

    // Can AMS can reduce to 0?
    @Override
    public boolean getAMSReduction(boolean toAdvancedAMS) {
        if (toAdvancedAMS) { return true; }
        return false;
    }

    // AMS shoots down the missile on 1-3
    @Override
    public boolean checkAMSSingleMissile(int roll) {
        return roll <= 3 ? true : false;
    }

    @Override
    public int hitsToDestroyGyro(int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY) {
            return 3;
        }
        return 2;
    }

    @Override
    public int getMascFailure(int nLevel) {
        int[] MASC_FAILURE = { 3, 5, 7, 11, 13, 13, 13 };
        return MASC_FAILURE[nLevel];
    }

    // Blue shield is 3+ number of rounds over 6
    @Override
    public int getBlueShieldTarget(int blueShieldRounds) {
        return (3 + blueShieldRounds - 6);
    }

    /**
     * Returns the target number to avoid Radical Heat Sink Failure for the given number of rounds of consecutive use,
     * IO p.89. The first round of use means consecutiveRounds = 1; this is the minimum as 0 rounds of use would not
     * trigger a roll.
     *
     * @param consecutiveRounds The rounds the RHS has been used
     *
     * @return The roll target number to avoid failure
     */
    @Override
    public int radicalHeatSinkSuccessTarget(int consecutiveRounds) {
        return switch (consecutiveRounds) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            case 4 -> 10;
            case 5 -> 11;
            default -> TargetRoll.AUTOMATIC_FAIL;
        };
    }

    // ECM affects things where LoS goes through the bubble
    public ArrayList<Coords> getECMCoordsAffected(Coords a, Coords b) {
        return Coords.intervening(a, b);
    }
    
    // ECM ranges 
    @Override
    public int getECMRanges(MiscType type) {
        if (type.hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
            return 0;
        } else if (type.hasFlag(MiscType.F_EW_EQUIPMENT) ||
              type.hasFlag(MiscType.F_NOVA) ||
              type.hasFlag(MiscType.F_WATCHDOG)) {
            return 3;
        }
        return 6;
    }

    // Sensor ranges for probes.
    public int getSensorRanges(int type) {
        return switch (type) {
            case Sensor.TYPE_BAP, Sensor.TYPE_BAPP -> 12;
            case Sensor.TYPE_BLOODHOUND -> 16;
            case Sensor.TYPE_CLAN_AP -> 15;
            case Sensor.TYPE_WATCHDOG, Sensor.TYPE_NOVA, Sensor.TYPE_LIGHT_AP, Sensor.TYPE_VEE_MAG_SCAN,
                 Sensor.TYPE_VEE_IR,
                 Sensor.TYPE_BA_HEAT -> 9;
            //Under the current errata (3.0,Dec 2017), the rules only give aero sensor ranges against overflown ground units
            //No differences in range are mentioned for any sensor but active probe, so I'm assuming magscan range for standard sensors
            case Sensor.TYPE_MEK_MAG_SCAN, Sensor.TYPE_MEK_IR, Sensor.TYPE_AERO_SENSOR -> 10;
            case Sensor.TYPE_MEK_RADAR -> 8;
            case Sensor.TYPE_VEE_RADAR, Sensor.TYPE_BA_IMPROVED -> 6;
            case Sensor.TYPE_EW_EQUIPMENT -> 3;
            case Sensor.TYPE_MEK_SEISMIC -> 2;
            case Sensor.TYPE_VEE_SEISMIC, Sensor.TYPE_EI_PROBE -> 1;
            //The ranges listed for the various sensors in SO are so far beyond gameplay distances that I'm condensing
            //them into just the types that have different detection mechanics.
            case Sensor.TYPE_SPACECRAFT_RADAR, Sensor.TYPE_SPACECRAFT_ESM -> 5555;
            case Sensor.TYPE_SPACECRAFT_THERMAL -> 1388;
            case Sensor.TYPE_AERO_THERMAL -> 139;
            default -> 0;
        };
    }

    // Active probes not affected by things other than angel
    public boolean isBAPActive(boolean checkECM,
          final MiscType type,
          final Entity entity,
          final Coords position) {
        // Beagle Isn't affected by normal ECM
        if (type.getName().equals("Beagle Active Probe")) {
            return !checkECM ||
                  !ComputeECM.isAffectedByAngelECM(entity, position, position);
        }
        return !checkECM ||
              !ComputeECM.isAffectedByECM(entity, position, position);
    }
}
