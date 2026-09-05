package megamek.common.rules.core;

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
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesEquipment;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class CoreRulesEquipment extends RulesEquipment {
    /**
     * {@inheritDoc}
     * AMS can shoot twice. Core P.206
     */
    @Override
    public boolean getAMSMultiShot() {return true;}

    /**
     * {@inheritDoc}
     * AMS can reduce to 0. Core P.206
     */
    @Override
    public boolean getAMSReduction(boolean toAdvancedAMS) { return true; }

    /**
     * {@inheritDoc}
     * AMS destroys a single missile on 4+. Core p.206
     */
    @Override
    public boolean checkAMSSingleMissile(int roll) {
        return roll >= 4 ? true : false;
    }

    /**
     * {@inheritDoc}
     * HD Gyros take 4 hits to destroy. Core p.98
     */
    @Override
    public int hitsToDestroyGyro(int gyroType) {
        if (gyroType == Mek.GYRO_HEAVY_DUTY) {
            return 4;
        }
        return 2;
    }

    /**
     * {@inheritDoc}
     * Get the masc failure roll from the escalating chart
     */
    @Override
    public int getMascFailure(int nLevel) {
        return Game.rulesManager.getRulesCharts().escalatingFailure(nLevel);
    }

    /**
     * {@inheritDoc}
     * Blue Shield uses escalating failure for rounds after 6. Core p.207
     */
    @Override
    public int getBlueShieldTarget(int blueShieldRounds) {
        return Game.rulesManager.getRulesCharts().escalatingFailure(blueShieldRounds - 6);
    }

    /**
     * {@inheritDoc}
     * Get the radical heat sink
     */
    @Override
    public int radicalHeatSinkSuccessTarget(int consecutiveRounds) {
        return Game.rulesManager.getRulesCharts().escalatingFailure(consecutiveRounds);
    }
    
    /**
     * {@inheritDoc}
     * ECM only affects if the target or source is under the bubble. Not intervening. Core p.200
     */
    @Override
    public ArrayList<Coords> getECMCoordsAffected(Coords a, Coords b) {
        ArrayList<Coords> coords = new ArrayList<>();
        coords.add(a);
        coords.add(b);
        
        return coords;
    }

    /**
     * {@inheritDoc}
     * ECM ranges. Watchdog is the same as clan ECM. Core p.197, 200, 201
     */
    @Override
    public int getECMRanges(MiscType type) {
        if (type.hasFlag(MiscType.F_SINGLE_HEX_ECM)) {
            return 0;
        } else if (type.hasFlag(MiscType.F_EW_EQUIPMENT)) {
            return 3;
        }
        // all ECMs other than these two are the standard 6 hex range
        return 6;
    }
    
    /**
     * {@inheritDoc}
     * Sensor ranges for probes. Core p.197 and 201 provide guidance due to how Watchdog is changed
     */
    @Override
    public int getSensorRanges(int type) {
        return switch (type) {
            case Sensor.TYPE_BAP, Sensor.TYPE_BAPP -> 12;
            case Sensor.TYPE_BLOODHOUND -> 16;
            case Sensor.TYPE_CLAN_AP, Sensor.TYPE_WATCHDOG, Sensor.TYPE_NOVA -> 15;
            case Sensor.TYPE_LIGHT_AP, Sensor.TYPE_VEE_MAG_SCAN, Sensor.TYPE_VEE_IR,
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
     * {@inheritDoc}
     * Active probes are affected by ECM other than bloodhound. Core p.200
     */
    @Override
    public boolean isBAPActive(boolean checkECM,
          final MiscType type,
          final Entity entity,
          final Coords position) {
        // Active probes are impacted by ECM unless they are Bloodhound
        if (type.hasFlag(MiscType.F_BLOODHOUND)) {
            return !checkECM ||
                  !ComputeECM.isAffectedByAngelECM(entity, position, position);
        }
        return !checkECM ||
              !ComputeECM.isAffectedByECM(entity, position, position);
    }
    
    /**
     * {@inheritDoc}
     * No init bonus for command console or tech officer Core p.203, 236
     */
    @Override
    public int getCommandConsoleBonus() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * What is the masc or supercharger failure hits. Core p.204
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
     * {@inheritDoc}
     * Masc failure critical. Core p.204
     */
    @Override
    public void doMascFailureCrits(Entity entity, HashMap<Integer, List<CriticalSlot>> vCriticalSlots, int hits) {
        // No hits, early return
        if (hits == 0) {
            return;
        }
        ArrayList<Integer> legLocations = new ArrayList<>();
        // We need a random hit location of a leg
        for (int loc = 0; loc < entity.locations(); loc++) {
            if (entity.locationIsLeg(loc)) {
                legLocations.add(loc);
            }
        }
        int randomLeg = Compute.randomInt(legLocations.size());
        int selectedLeg = legLocations.get(randomLeg);

        vCriticalSlots.put(selectedLeg, new LinkedList<>());
        int numCritSlots = entity.getNumberOfCriticalSlots(selectedLeg);

        ArrayList<Integer> potentialSlots = new ArrayList<>();

        // Populate the List with the potential critical slot numbers
        for (int critSlot = 0; critSlot < numCritSlots; critSlot++) {
            CriticalSlot slot = entity.getCritical(selectedLeg,critSlot);
            if (slot != null && slot.isHittable()) {
                potentialSlots.add(critSlot);
            }
        }
        // Make sure we have enough crits to hit
        if (hits > potentialSlots.size()) {
            hits = potentialSlots.size();
        }
        vCriticalSlots.put(selectedLeg, new LinkedList<>());
        for (int hit = 0; hit < hits; hit++) {
            CriticalSlot slot;
            int slotIndex = Compute.randomInt(potentialSlots.size());
            slot = entity.getCritical(selectedLeg,potentialSlots.get(slotIndex));
            vCriticalSlots.get(selectedLeg).add(slot);
            // Make sure we don't hit the same one again
            potentialSlots.remove(slotIndex);
        }
    }

    /**
     * {@inheritDoc}
     * Blue shield blocks stealth. Core p.207
     */
    @Override
    public boolean blueShieldStealth(boolean activeBlueShield) { return activeBlueShield; }
}
