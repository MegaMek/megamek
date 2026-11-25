package megamek.common.rules;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Sensor;
import megamek.common.units.Entity;

public abstract class RulesEquipment {
    /**
     * Does AMS allow for multiple shots.
     *
     * @return true if AMS allows multiple shots
     */
    public abstract boolean getAMSMultiShot();

    /**
     * Can AMS reduce the value to 0.
     *
     * @param toAdvancedAMS true if using advanced AMS
     * @return true if AMS can reduce to 0
     */
    public abstract boolean getAMSReduction(boolean toAdvancedAMS);

    /**
     * Does AMS shoot down a single missile/pod.
     *
     * @param roll the dice roll
     * @return true if AMS shoots down a single missile
     */
    public abstract boolean checkAMSSingleMissile(int roll);

    /**
     * How many hits destroy the gyro.
     *
     * @param gyroType the type of gyro
     * @return the number of hits needed to destroy the gyro
     */
    public abstract int hitsToDestroyGyro(int gyroType);

    /**
     * What is the number for masc failure (also used for supercharger).
     *
     * @param nLevel the MASC level
     * @return the failure target number
     */
    public abstract int getMascFailure(int nLevel);

    /**
     * What is the target number for Blue shield.
     *
     * @param blueShieldRounds the number of blue shield rounds
     * @return the target number
     */
    public abstract int getBlueShieldTarget(int blueShieldRounds);

    /**
     * What is the target number for radical heat sink.
     *
     * @param consecutiveRounds the number of consecutive rounds
     * @return the target number for success
     */
    public abstract int radicalHeatSinkSuccessTarget(int consecutiveRounds);

    /**
     * What hexes are affected by ECM.
     *
     * @param a the first coordinate
     * @param b the second coordinate
     * @return list of coordinates affected by ECM
     */
    public abstract ArrayList<Coords> getECMCoordsAffected(final Coords a, final Coords b);

    /**
     * What are the ECM ranges other than angel. ECM suites have a 6-hex radius, the Watchdog included (Core p.201;
     * TO:AUE p.90); EW equipment has 3 (TO:AUE p.123), as does the Nova CEWS (IO:AE pp.60, 212).
     *
     * @param type the miscellaneous equipment type
     * @return the ECM range
     */
    public int getECMRanges(MiscType type) {
        if (type.hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
            return 0;
        } else if (type.hasFlag(MiscType.F_EW_EQUIPMENT) || type.hasFlag(MiscType.F_NOVA)) {
            return 3;
        }
        return 6;
    }

    /**
     * Returns the range per bracket for Tactical Operations and Strategic Operations sensors.
     * The TO:AR sensor table (p. 190) omits the Nova CEWS, so each ruleset retains its existing bracket.
     *
     * @param type the probe type
     * @return the sensor range
     */
    public abstract int getSensorRanges(int type);

    /** Returns the range per bracket for sensor types shared by both rulesets. */
    protected static int getCommonSensorRange(int type) {
        return switch (type) {
            case Sensor.TYPE_BAP, Sensor.TYPE_BAPP -> 12;
            case Sensor.TYPE_BLOODHOUND -> 16;
            case Sensor.TYPE_CLAN_AP -> 15;
            // TO:AR p.190 lists "Clan Watchdog/Light Active Probe" together
            case Sensor.TYPE_WATCHDOG, Sensor.TYPE_LIGHT_AP, Sensor.TYPE_VEE_MAG_SCAN, Sensor.TYPE_VEE_IR,
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

    /**
     * Is the probe impacted by ECM?
     *
     * @param checkECM whether to check for ECM
     * @param type the miscellaneous equipment type
     * @param entity the entity with the probe
     * @param position the position to check
     * @return true if BAP is active
     */
    public boolean isBAPActive(boolean checkECM, MiscType type, Entity entity, Coords position) {
        // A Bloodhound is negated only by Angel ECM (Core p.200; TO:AUE pp.90-91)
        if (type.hasFlag(MiscType.F_BLOODHOUND)) {
            return !checkECM || !ComputeECM.isAffectedByAngelECM(entity, position, position);
        }
        return !checkECM || !ComputeECM.isAffectedByECM(entity, position, position);
    }

    /**
     * Is there an init bonus for the command console or tripod tech officer?
     *
     * @return the initiative bonus
     */
    public abstract int getCommandConsoleBonus();

    /**
     * @return true if anti-missile systems and point defenses can engage homing Arrow IV missiles
     */
    public abstract boolean antiMissileSystemsEngageArrowIV();

    /**
     * The modifier to a supercharger's activation roll result.
     *
     * @param entity the unit using the supercharger
     * @return the roll modifier
     */
    public abstract int getSuperchargerActivationModifier(Entity entity);

    /**
     * Check for how many hits and how we deal with them.
     *
     * @param entityId the entity ID
     * @param vDesc vector of reports describing the failure
     * @param isSupercharger true if this is a supercharger failure
     * @return the number of hits
     */
    public abstract int getMascSuperChargerFailureHits(int entityId, Vector<Report> vDesc, boolean isSupercharger);

    /**
     * Do masc critical hits.
     *
     * @param entity the entity experiencing MASC failure
     * @param vCriticalSlots map of critical slots being damaged
     * @param hits the number of hits
     */
    public abstract void doMascFailureCrits(Entity entity, HashMap<Integer, List<CriticalSlot>> vCriticalSlots,
          int hits);

    /**
     * Should blue shield limit stealth?
     * @param activeBlueShield Is the blue shield system active
     * @return true if it blocks stealth
     */
    public abstract boolean blueShieldStealth(boolean activeBlueShield);

    /**
     * Asks is blue shield blocks stealth when active. It assumes it is active
     * @return calls blueShieldStealth(true)
     */
    public boolean blueShieldStealth() {
        return blueShieldStealth(true);
    }

    /** Ground active-probe radius of a Watchdog CEWS, before crew and equipment bonuses. */
    public int getWatchdogProbeRange() {
        return watchdogUsesLightProbe() ? 3 : 5;
    }

    /** Whether the Watchdog carries the Clan light active probe instead of the standard Clan probe. */
    public abstract boolean watchdogUsesLightProbe();
}
