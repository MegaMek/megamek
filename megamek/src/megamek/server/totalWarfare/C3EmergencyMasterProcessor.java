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
package megamek.server.totalWarfare;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.compute.ComputeECM;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * End Phase processing for the C3 Emergency Master (TO:AUE p.110): a C3EM duplicates a C3 Slave until its lance's
 * C3 Master is destroyed or jammed by hostile ECM, then takes over as lance master for up to
 * {@link Entity#C3EM_MAX_OPERATING_TURNS} operating turns before overloading for the scenario. Turns the C3EM
 * itself spends in hostile ECM do not count (standby). Per the official ruling (Xotl, forum topic 40600), the
 * takeover is strictly lance-level: the C3EM adopts only its dead master's slave dependents and reconnects
 * upward to a surviving company master, and it never substitutes for a company master. When the original master
 * was only jammed and becomes operable again, it resumes and the C3EM returns to slave mode with its used
 * operating turns still spent.
 *
 * <p>Static and game-only so the rules are testable without a server; {@link TWGameManager} calls
 * {@link #processEndPhase(Game)} through a one-line delegator.</p>
 */
public final class C3EmergencyMasterProcessor {

    private static final MMLogger logger = MMLogger.create(C3EmergencyMasterProcessor.class);

    private C3EmergencyMasterProcessor() {
    }

    /**
     * Runs the End Phase C3EM sequence: tick or standby active units, overload those out of turns, hand lances
     * back to recovered masters, then activate emergency masters whose lance master died or got jammed this turn.
     *
     * @param game the game to process
     *
     * @return the reports to append to the End Phase report
     */
    public static Vector<Report> processEndPhase(Game game) {
        Vector<Report> reports = new Vector<>();
        if (!game.getOptions().booleanOption(OptionsConstants.ADVANCED_TAC_OPS_C3_EMERGENCY_MASTER)) {
            return reports;
        }
        for (Entity entity : new ArrayList<>(game.getEntitiesVector())) {
            if (entity.isC3EmergencyMasterActive()) {
                processActiveEmergencyMaster(game, entity, reports);
            }
        }
        for (Entity entity : new ArrayList<>(game.getEntitiesVector())) {
            if (isEligibleForActivation(entity)) {
                tryActivate(game, entity, reports);
            }
        }
        return reports;
    }

    /** Ticks, overloads or yields back one active emergency master. */
    private static void processActiveEmergencyMaster(Game game, Entity emergencyMaster, Vector<Report> reports) {
        if (isInHostileEcm(emergencyMaster)) {
            // Standby: the turn is not counted and the system just waits out the interference (TO:AUE p.110)
            reports.add(unitReport(7602, emergencyMaster));
            return;
        }
        int operatingTurns = emergencyMaster.getC3EmergencyMasterOperatingTurns() + 1;
        emergencyMaster.setC3EmergencyMasterOperatingTurns(operatingTurns);
        if (emergencyMaster.isC3EmergencyMasterOverloaded()) {
            overload(game, emergencyMaster, reports);
            return;
        }
        Report tickReport = unitReport(7603, emergencyMaster);
        tickReport.add(operatingTurns);
        tickReport.add(Entity.C3EM_MAX_OPERATING_TURNS);
        reports.add(tickReport);

        yieldBackIfMasterRecovered(game, emergencyMaster, reports);
    }

    /** After the final operating turn the system shuts down from overload and is dead for the scenario. */
    private static void overload(Game game, Entity emergencyMaster, Vector<Report> reports) {
        for (Entity dependent : dependentsOf(game, emergencyMaster)) {
            dependent.setC3Master(Entity.NONE, true);
        }
        emergencyMaster.setC3EmergencyMasterActive(false);
        emergencyMaster.setC3Master(Entity.NONE, true);
        emergencyMaster.setC3EmergencyOriginalMasterId(Entity.NONE);
        reports.add(unitReport(7604, emergencyMaster));
        logger.info("[C3EM] {} overloaded after {} operating turns - network dissolved",
              emergencyMaster.getShortName(), Entity.C3EM_MAX_OPERATING_TURNS);
    }

    /** An ECM-jammed original master that is operable and clear again takes its lance back (Dave's D2 ruling). */
    private static void yieldBackIfMasterRecovered(Game game, Entity emergencyMaster, Vector<Report> reports) {
        Entity originalMaster = game.getEntity(emergencyMaster.getC3EmergencyOriginalMasterId());
        if ((originalMaster == null) || originalMaster.isDestroyed() || originalMaster.isDoomed()
              || !originalMaster.hasC3M() || isInHostileEcm(originalMaster)) {
            return;
        }
        for (Entity dependent : dependentsOf(game, emergencyMaster)) {
            dependent.setC3Master(originalMaster, true);
        }
        emergencyMaster.setC3EmergencyMasterActive(false);
        emergencyMaster.setC3Master(originalMaster, true);
        emergencyMaster.setC3EmergencyOriginalMasterId(Entity.NONE);
        Report yieldReport = unitReport(7605, emergencyMaster);
        yieldReport.addDesc(originalMaster);
        reports.add(yieldReport);
    }

    /** True for an operable, inactive, non-overloaded C3EM unit that is (or was) slaved to a master. */
    private static boolean isEligibleForActivation(Entity entity) {
        return entity.hasC3EmergencyMaster() && !entity.isC3EmergencyMasterActive()
              && !entity.isDestroyed() && !entity.isDoomed();
    }

    /** Activates the C3EM when its lance master is destroyed, has lost its computer, or sits in hostile ECM. */
    private static void tryActivate(Game game, Entity emergencyMaster, Vector<Report> reports) {
        int lanceMasterId = (emergencyMaster.getC3MasterId() != Entity.NONE)
              ? emergencyMaster.getC3MasterId() : emergencyMaster.getC3MasterLostId();
        if ((lanceMasterId == Entity.NONE) || (lanceMasterId == emergencyMaster.getId())) {
            return;
        }
        Entity lanceMaster = game.getEntity(lanceMasterId);
        boolean masterGone = (lanceMaster == null) || lanceMaster.isDestroyed() || lanceMaster.isDoomed()
              || !lanceMaster.hasC3M();
        boolean masterJammed = !masterGone && isInHostileEcm(lanceMaster);
        if (!masterGone && !masterJammed) {
            return;
        }

        // Find the dead/jammed master's superior first (ruling: the C3EM reconnects to the company master)
        Entity companyMaster = findSuperiorOf(game, lanceMaster, lanceMasterId);

        emergencyMaster.setC3EmergencyMasterActive(true);
        emergencyMaster.setC3EmergencyOriginalMasterId(masterJammed ? lanceMasterId : Entity.NONE);
        if ((companyMaster != null) && !companyMaster.equals(emergencyMaster)) {
            emergencyMaster.setC3Master(companyMaster, true);
        } else {
            emergencyMaster.setC3Master(Entity.NONE, true);
        }

        // Adopt up to 3 fellow SLAVE dependents of the lost master (strictly lance-level per the ruling)
        int adopted = 0;
        for (Entity formerDependent : formerDependentsOf(game, lanceMasterId, emergencyMaster)) {
            if (adopted >= Entity.MAX_C3M_SUBORDINATES) {
                break;
            }
            formerDependent.setC3Master(emergencyMaster, true);
            adopted++;
        }

        Report activationReport = unitReport(masterJammed ? 7606 : 7601, emergencyMaster);
        activationReport.add(adopted);
        reports.add(activationReport);
        logger.info("[C3EM] {} activated ({}), adopted {} slaves, company link {}",
              emergencyMaster.getShortName(), masterJammed ? "master jammed" : "master destroyed", adopted,
              (companyMaster != null) ? companyMaster.getShortName() : "none");
    }

    /** The lost master's superior when it is alive and still a working master, {@code null} otherwise. */
    @Nullable
    private static Entity findSuperiorOf(Game game, @Nullable Entity lanceMaster, int lanceMasterId) {
        int superiorId = Entity.NONE;
        if (lanceMaster != null) {
            superiorId = (lanceMaster.getC3MasterId() != Entity.NONE)
                  ? lanceMaster.getC3MasterId() : lanceMaster.getC3MasterLostId();
        }
        if ((superiorId == Entity.NONE) || (superiorId == lanceMasterId)) {
            return null;
        }
        Entity superior = game.getEntity(superiorId);
        if ((superior == null) || superior.isDestroyed() || superior.isDoomed() || !superior.hasC3M()) {
            return null;
        }
        return superior;
    }

    /** Live slave units that pointed at the lost master - by live pointer or by recorded loss. */
    private static List<Entity> formerDependentsOf(Game game, int lostMasterId, Entity emergencyMaster) {
        List<Entity> dependents = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            if (entity.equals(emergencyMaster) || entity.isDestroyed() || entity.isDoomed()
                  || emergencyMaster.isEnemyOf(entity) || !entity.hasC3S()) {
                continue;
            }
            boolean pointsAtLostMaster = (entity.getC3MasterId() == lostMasterId)
                  || (entity.getC3MasterLostId() == lostMasterId);
            if (pointsAtLostMaster) {
                dependents.add(entity);
            }
        }
        dependents.sort(java.util.Comparator.comparingInt(Entity::getId));
        return dependents;
    }

    /** Live units currently slaved to the given emergency master. */
    private static List<Entity> dependentsOf(Game game, Entity emergencyMaster) {
        List<Entity> dependents = new ArrayList<>();
        for (Entity entity : game.getEntitiesVector()) {
            if (!entity.equals(emergencyMaster) && entity.C3MasterIs(emergencyMaster)) {
                dependents.add(entity);
            }
        }
        return dependents;
    }

    /** True when the unit's own hex sits inside hostile ECM (positions may be null in the lobby - then false). */
    private static boolean isInHostileEcm(Entity entity) {
        return (entity.getPosition() != null)
              && ComputeECM.isAffectedByECM(entity, entity.getPosition(), entity.getPosition());
    }

    /** A public unit report with the unit's description as the first argument. */
    private static Report unitReport(int messageId, Entity entity) {
        Report report = new Report(messageId);
        report.subject = entity.getId();
        report.addDesc(entity);
        return report;
    }
}
