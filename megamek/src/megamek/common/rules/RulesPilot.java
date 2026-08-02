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

import megamek.common.CriticalSlot;
import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

import java.util.Vector;

public abstract class RulesPilot {

    /**
     * Handle pilot hits.
     *
     * @param e the entity taking pilot hits
     * @param totalHits the total number of hits
     * @param damage the damage amount
     * @param crewPos the crew position
     * @param toughness true if the pilot has toughness
     * @return vector of reports describing the pilot hits
     */
    public abstract Vector<Report> pilotHits(Entity e, int totalHits, int damage, int crewPos, boolean toughness);

    /**
     * How many pilot hits for an explosion.
     *
     * @return the number of pilot hits caused by an explosion
     */
    public abstract int getExplosionPilotHits();

    /**
     * Crew takeover report. Required by damage.
     *
     * @param e the entity with the crew
     * @param slot the crew slot being taken over
     * @param wasPilot true if the crew member was the pilot
     * @param wasGunner true if the crew member was the gunner
     * @return a report of the crew takeover
     */
    public abstract Report createCrewTakeoverReport(Entity e, int slot, boolean wasPilot, boolean wasGunner);

    /**
     * Is there a modifier for the gyro being destroyed.
     *
     * @param piloting the piloting skill
     * @return the seatbelt gyro modifier
     */
    public abstract int getSeatbeltGyroModifier(int piloting);

    /**
     * Do we modify seatbelt by legs destroyed.
     *
     * @param piloting the piloting skill
     * @param legsDestroyed the number of legs destroyed
     * @return the seatbelt leg modifier
     */
    public abstract int getSeatbeltLegModifier(int piloting, int legsDestroyed);

    /**
     * What is the seatbelt check on shutdown.
     *
     * @param piloting the piloting skill
     * @return the seatbelt shutdown target number
     */
    public abstract int getSeatbeltShutdown(int piloting);

    /**
     * Returns the result of a sensor roll, and adds to a report.
     *
     * @param entity the entity rolling
     * @param modifier any external modifiers
     * @param vDesc vector to add report to
     * @return the margin of success or failure. negative is a failure, 0 or higher is a success.
     */
    public int rollSensorCheck(Entity entity, int modifier, Vector<Report> vDesc) {
        int targetNumber = entity.getCrew().getPiloting();
        int sensorHits = 0;
        Roll diceRoll = Compute.rollD6(2);
        int rollValue = diceRoll.getIntValue();
        String rollCalc = String.valueOf(rollValue);
        if (entity instanceof Mek) {
            sensorHits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, Mek.LOC_HEAD);
            sensorHits += entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM,Mek.SYSTEM_SENSORS, Mek.LOC_CENTER_TORSO);
        }
        if (sensorHits > 1) {
            return TargetRoll.IMPOSSIBLE;
        } else if (sensorHits == 1) {
            targetNumber += 2;
        }
        targetNumber += modifier;

        Report r = new Report(2366);
        r.subject = entity.getId();
        r.addDesc(entity);
        r.add(targetNumber);
        r.add(rollValue);
        vDesc.addElement(r);

        // Return the MoS/Failure
        return targetNumber - rollValue;
    }

    /**
     * Get the height modifier for pilot hit checks
     *
     * @param fallHeight the height of the fall
     * @return return the fall height, unmodified
     */
    public int getSeatbeltHeightModifier(int fallHeight) { return fallHeight; }
}
