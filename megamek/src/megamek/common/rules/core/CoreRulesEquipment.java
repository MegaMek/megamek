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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import megamek.common.CriticalSlot;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.Sensor;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.rules.RulesEquipment;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

public class CoreRulesEquipment extends RulesEquipment {

    /**
     * {@inheritDoc}
     * An Arrow IV missile "is unaffected by anti-missile systems of any kind" (Core p.182), whatever its munition
     * (Core p.190).
     */
    @Override
    public boolean antiMissileSystemsEngageArrowIV() {
        return false;
    }

    /**
     * {@inheritDoc}
     * The controller rolls on the Escalating Failure table with no modifier (Core pp.180, 204).
     */
    @Override
    public int getSuperchargerActivationModifier(Entity entity) {
        return 0;
    }
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
     * Get the masc failure roll from the Escalating Failure Table (Core pp.180, 204).
     * The level sent starts at 0, not 1. need to adjust
     */
    @Override
    public int getMascFailure(int nLevel) {
        nLevel += 1;
        return Game.rulesManager.getRulesCharts().escalatingFailure(nLevel);
    }

    /**
     * {@inheritDoc}
     * Blue Shield uses escalating failure for rounds starting at round 6. Core p.207
     */
    @Override
    public int getBlueShieldTarget(int blueShieldRounds) {
        return Game.rulesManager.getRulesCharts().escalatingFailure(blueShieldRounds - 5);
    }

    /**
     * {@inheritDoc}
     * Get the radical heat sink roll from the Escalating Failure Table (Core pp.180, 208).
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

    @Override
    public int getSensorRanges(int type) {
        return type == Sensor.TYPE_NOVA ? 15 : getCommonSensorRange(type);
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

    /** Core pp.197, 201 give the Watchdog the standard Clan active probe. */
    @Override
    public boolean watchdogUsesLightProbe() {
        return false;
    }
}
