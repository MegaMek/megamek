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

import megamek.common.CriticalSlot;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeECM;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Sensor;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesEquipment;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class TWRulesEquipment extends RulesEquipment {
    /**
     * Does AMS allow for multiple shots.
     * AMS can shoot once.
     *
     * @return true if AMS allows multiple shots
     */
    @Override
    public boolean getAMSMultiShot() { return false;}

    /**
     * Can AMS reduce the value to 0.
     * Can AMS can reduce to 0?
     *
     * @param toAdvancedAMS true if using advanced AMS
     * @return true if AMS can reduce to 0
     */
    @Override
    public boolean getAMSReduction(boolean toAdvancedAMS) {
        if (toAdvancedAMS) { return true; }
        return false;
    }

    /**
     * Does AMS shoot down a single missile/pod.
     * AMS shoots down the missile on 1-3.
     *
     * @param roll the dice roll
     * @return true if AMS shoots down a single missile
     */
    @Override
    public boolean checkAMSSingleMissile(int roll) {
        return roll <= 3 ? true : false;
    }

    /**
     * How many hits destroy the gyro.
     *
     * @param gyroType the type of gyro
     * @return the number of hits needed to destroy the gyro
     */
    @Override
    public int hitsToDestroyGyro(int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY) {
            return 3;
        }
        return 2;
    }

    /**
     * What is the number for masc failure (also used for supercharger).
     *
     * @param nLevel the MASC level
     * @return the failure target number
     */
    @Override
    public int getMascFailure(int nLevel) {
        int[] MASC_FAILURE = { 3, 5, 7, 11, 13, 13, 13 };
        return MASC_FAILURE[nLevel];
    }

    /**
     * What is the target number for Blue shield.
     * Blue shield is 3+ number of rounds over 6.
     *
     * @param blueShieldRounds the number of blue shield rounds
     * @return the target number
     */
    @Override
    public int getBlueShieldTarget(int blueShieldRounds) {
        return (3 + blueShieldRounds - 6);
    }

    /**
     * What is the target number for radical heat sink.
     * Returns the target number to avoid Radical Heat Sink Failure for the given number of rounds of consecutive use,
     * IO p.89. The first round of use means consecutiveRounds = 1; this is the minimum as 0 rounds of use would not
     * trigger a roll.
     *
     * @param consecutiveRounds the number of consecutive rounds
     * @return the target number for success
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

    /**
     * What hexes are affected by ECM.
     * ECM affects things where LoS goes through the bubble.
     *
     * @param a the first coordinate
     * @param b the second coordinate
     * @return list of coordinates affected by ECM
     */
    @Override
    public ArrayList<Coords> getECMCoordsAffected(Coords a, Coords b) {
        return Coords.intervening(a, b);
    }
    
    /**
     * What are the ECM ranges other than angel.
     * ECM ranges.
     *
     * @param type the miscellaneous equipment type
     * @return the ECM range
     */
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

    /**
     * What are the Sensor ranges for probes.
     * Sensor ranges for probes.
     *
     * @param type the probe type
     * @return the sensor range
     */
    @Override
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

    /**
     * Is the probe impacted by ECM?
     * Active probes not affected by things other than angel.
     *
     * @param checkECM whether to check for ECM
     * @param type the miscellaneous equipment type
     * @param entity the entity with the probe
     * @param position the position to check
     * @return true if BAP is active
     */
    @Override
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
    
    /**
     * Is there an init bonus for the command console or tripod tech officer?
     * Command console and tech officer return bonus to init.
     *
     * @return the initiative bonus
     */
    @Override
    public int getCommandConsoleBonus() {
        return 2;
    }

    /**
     * Check for how many hits and how we deal with them.
     * What is the masc or supercharger failure hits.
     *
     * @param entityId the entity ID
     * @param vDesc vector of reports describing the failure
     * @param isSupercharger true if this is a supercharger failure
     * @return the number of hits
     */
    @Override
    public int getMascSuperChargerFailureHits(int entityId, Vector<Report> vDesc, boolean isSupercharger) {
        int hits = 0;
        int reportId = 6310;
        Roll diceRoll2 = Compute.rollD6(2);
        Report r = new Report(reportId);
        r.subject = entityId;
        r.add(diceRoll2);
        r.newlines = 0;
        vDesc.addElement(r);
        if (diceRoll2.getIntValue() <= 7) {
            // no effect
            reportId = 6005;
        } else if ((diceRoll2.getIntValue() == 8) || (diceRoll2.getIntValue() == 9)) {
            hits = 1;
            reportId = 6315;
        } else if ((diceRoll2.getIntValue() == 10) || (diceRoll2.getIntValue() == 11)) {
            hits = 2;
            reportId = 6320;
        } else if (diceRoll2.getIntValue() == 12) {
            hits = 3;
            reportId = 6325;
        }

        r = new Report(reportId);
        r.subject = entityId;
        r.newlines = 0;
        vDesc.addElement(r);

        return hits;
    }

    /**
     * Do masc critical hits.
     * Masc crits: do the damage. random critical slot on each leg, but MASC is not destroyed.
     *
     * @param entity the entity experiencing MASC failure
     * @param vCriticalSlots map of critical slots being damaged
     * @param hits the number of hits
     */
    @Override
    public void doMascFailureCrits(Entity entity, HashMap<Integer, List<CriticalSlot>> vCriticalSlots, int hits) {
        // do the damage. random critical slot on each leg, but MASC is not destroyed
        for (int loc = 0; loc < entity.locations(); loc++) {
            if (entity.locationIsLeg(loc) && (entity.getHittableCriticalSlots(loc) > 0)) {
                CriticalSlot slot;
                do {
                    int slotIndex = Compute.randomInt(entity.getNumberOfCriticalSlots(loc));
                    slot = entity.getCritical(loc, slotIndex);
                } while ((slot == null) || !slot.isHittable());
                vCriticalSlots.put(loc, new LinkedList<>());
                vCriticalSlots.get(loc).add(slot);
            }
        }
    }

    /**
     * This always returns false.
     * @param activeBlueShield Is the blue shield system active
     * @return false;
     */
    @Override
    public boolean blueShieldStealth(boolean activeBlueShield) { return false; }
}
