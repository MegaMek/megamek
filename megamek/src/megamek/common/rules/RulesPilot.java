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
import megamek.common.TargetRollModifier;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.Roll;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.Mek;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

public abstract class RulesPilot {

    /**
     * Handle pilot hits. Core p.117, same as TW.
     *
     * @param e the entity taking pilot hits
     * @param totalHits the total number of hits
     * @param damage the damage amount
     * @param crewPos the crew position
     * @param toughness true if the pilot has toughness
     * @return vector of reports describing the pilot hits
     */
    public Vector<Report> pilotHits(Entity e, int totalHits, int damage, int crewPos, boolean toughness,
          GamePhase phase) {
        Vector<Report> vDesc = new Vector<>();
        
        for (int hit = totalHits - damage + 1; hit <= totalHits; hit++) {
            int rollTarget = Game.rulesManager.getRulesCharts().escalatingFailure(hit);

            if (toughness) {
                rollTarget -= e.getCrew().getToughness(crewPos);
            }

            boolean rerollWithEdge = false;
            boolean edgeAlreadyUsed = false;

            do {
                if (rerollWithEdge) {
                    e.getCrew().decreaseEdge();
                    edgeAlreadyUsed = true;
                    rerollWithEdge = false;
                }
                Roll diceRoll = Compute.rollD6(2);
                int rollValue = diceRoll.getIntValue();
                String rollCalc = String.valueOf(rollValue);

                if (e.hasAbility(OptionsConstants.MISC_PAIN_RESISTANCE)) {
                    rollValue = Math.min(12, rollValue + 1);
                    rollCalc = rollValue + " [" + diceRoll.getIntValue() + " + 1] max 12";
                }

                Report r = new Report(6030);
                r.indent(2);
                r.subject = e.getId();
                r.add(e.getCrew().getCrewType().getRoleName(crewPos));
                r.addDesc(e);
                r.add(e.getCrew().getName(crewPos));
                r.add(rollTarget);
                r.addDataWithTooltip(rollCalc, diceRoll.getReport());

                if (rollValue >= rollTarget) {
                    e.getCrew().setKoThisRound(false, crewPos);
                    r.choose(true);
                } else {
                    e.getCrew().setKoThisRound(true, crewPos);
                    r.choose(false);
                    if (!edgeAlreadyUsed && (e.shouldUseEdge(OptionsConstants.EDGE_WHEN_KO) ||
                          e.shouldUseEdge(OptionsConstants.EDGE_WHEN_AERO_KO))) {
                        rerollWithEdge = true;
                        vDesc.add(r);
                        r = new Report(6520);
                        r.subject = e.getId();
                        r.addDesc(e);
                        r.add(e.getCrew().getName(crewPos));
                        r.add(e.getCrew().getOptions().intOption(OptionsConstants.EDGE));
                    } // if
                    // return true;
                } // else
                vDesc.add(r);
            } while (rerollWithEdge);
            // end of do-while
            if (e.getCrew().isKoThisRound(crewPos)) {
                boolean wasPilot = e.getCrew().getCurrentPilotIndex() == crewPos;
                boolean wasGunner = e.getCrew().getCurrentGunnerIndex() == crewPos;
                e.getCrew().setUnconscious(true, crewPos);
                Report r = createCrewTakeoverReport(e, crewPos, wasPilot, wasGunner);
                if (null != r) {
                    vDesc.add(r);
                }
                return vDesc;
            }
        }

        return vDesc;
    }
    
    /**
     * How many pilot hits for an explosion.
     *
     * @return the number of pilot hits caused by an explosion
     */
    public abstract int getExplosionPilotHits();

    /**
     * Crew takeover report. Required by damage.
     * Convenience method that fills in a report showing that a crew member of a multicrew cockpit has taken over for
     * another incapacitated crew member.
     *
     *
     * @param e the entity with the crew
     * @param slot the crew slot being taken over
     * @param wasPilot true if the crew member was the pilot
     * @param wasGunner true if the crew member was the gunner
     * @return a report of the crew takeover
     */
    public Report createCrewTakeoverReport(Entity e, int slot, boolean wasPilot, boolean wasGunner) {
        if (wasPilot && e.getCrew().getCurrentPilotIndex() != slot) {
            Report r = new Report(5560);
            r.subject = e.getId();
            r.indent(4);
            r.add(e.getCrew().getNameAndRole(e.getCrew().getCurrentPilotIndex()));
            r.add(e.getCrew().getCrewType().getRoleName(e.getCrew().getCrewType().getPilotPos()));
            r.addDesc(e);
            return r;
        }
        if (wasGunner && e.getCrew().getCurrentGunnerIndex() != slot) {
            Report r = new Report(5560);
            r.subject = e.getId();
            r.indent(4);
            r.add(e.getCrew().getNameAndRole(e.getCrew().getCurrentGunnerIndex()));
            r.add(e.getCrew().getCrewType().getRoleName(e.getCrew().getCrewType().getGunnerPos()));
            r.addDesc(e);
            return r;
        }
        return null;
    }
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

    /**
     * Seatbelt checks for potentially multiple crew
     * @param entity the entity falling
     * @param fallHeight how big was the fall
     * @param piloting piloting skill
     * @param modifiers list of modifiers
     * @param roll the current Piloting Roll
     * @return the new Piloting roll
     */
    public abstract PilotingRollData getSeatbeltRoll(Entity entity, int fallHeight, int piloting,
          List<TargetRollModifier> modifiers, PilotingRollData roll);

    /**
     * Rolls any pending con checks
     * @param entity The entity to roll
     * @return The reports
     */
    @Nullable
    public Vector<Report> rollConRolls(Entity entity, boolean toughness) { 
        return null;
    }
}
