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

import megamek.common.Report;
import megamek.common.annotations.Nullable;
import megamek.common.bays.Bay;
import megamek.common.board.Coords;
import megamek.common.units.CraneOperation;
import megamek.common.units.CraneRules;
import megamek.common.units.Entity;
import megamek.common.units.SmallCraft;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

/**
 * Loads and unloads units with the cranes of a grounded Small Craft or DropShip (TW p.90-91). VTOLs, Small Craft and
 * fighters cannot mount or dismount under their own power. A declared loading starts in that turn's End Phase and puts
 * the unit aboard in the End Phase of the fourth turn after it; a declared unloading puts the unit in its chosen hex in
 * the End Phase of the third turn after it.
 * <p>
 * Loading is cancelled if the unit leaves the hex it waits in, moves, fires a weapon, the carrier lifts off, or either
 * unit is destroyed. An unloading whose chosen hex is blocked when the work is done waits and tries again each End
 * Phase. Extracted from {@link TWGameManager} so that large class does not also carry the crane rules;
 * {@link TWGameManager#checkCraneOperations()} delegates here once per End Phase.
 * </p>
 */
class CraneOperationHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(CraneOperationHandler.class);

    CraneOperationHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Records a unit's declaration to be loaded by crane. The step was checked on the client; this repeats the check
     * so a stale or tampered path cannot start an illegal operation.
     *
     * @param unit         the VTOL, fighter or Small Craft to be loaded
     * @param target       the carrier named by the step, may be {@code null}
     * @param isOnlyAction {@code true} if the server found no step before this one in the path; crane loading must be
     *                     the unit's only action, and this is worked out on the server rather than trusted from the
     *                     client's step
     */
    void declareLoad(Entity unit, @Nullable Targetable target, boolean isOnlyAction) {
        String illegalReason = CraneRules.loadByCraneIllegalReason(unit, target, isOnlyAction, getGame());
        if (illegalReason != null) {
            LOGGER.info("[Crane] {}: load by crane rejected - {}", unit.getDisplayName(), illegalReason);
            return;
        }
        if (!(target instanceof SmallCraft carrier)) {
            LOGGER.info("[Crane] {}: load by crane rejected - the target is not a small craft or DropShip",
                  unit.getDisplayName());
            return;
        }
        carrier.getCraneOperations().add(CraneOperation.load(unit.getId(), unit.getPosition()));
        LOGGER.info("[Crane] {} declares crane loading into {} from {}", unit.getDisplayName(),
              carrier.getDisplayName(), unit.getPosition());
        gameManager.entityUpdate(carrier.getId());
    }

    /**
     * Records a carrier's declaration to unload a carried unit by crane, with the hex and facing chosen for it.
     *
     * @param carrier      the Small Craft or DropShip declaring the unloading
     * @param target       the carried unit named by the step, may be {@code null}
     * @param position     the hex chosen for the unit, may be {@code null}
     * @param facing       the facing chosen for the unit, may be {@code null}
     * @param isOnlyAction {@code true} if the server found no step before this one in the path; crane unloading must
     *                     be the carrier's only action, and this is worked out on the server rather than trusted from
     *                     the client's step
     */
    void declareUnload(Entity carrier, @Nullable Targetable target, @Nullable Coords position,
          @Nullable Integer facing, boolean isOnlyAction) {
        String illegalReason = CraneRules.unloadByCraneIllegalReason(carrier, target, position, isOnlyAction,
              getGame());
        if (illegalReason != null) {
            LOGGER.info("[Crane] {}: unload by crane rejected - {}", carrier.getDisplayName(), illegalReason);
            return;
        }
        if (!(carrier instanceof SmallCraft smallCraft) || !(target instanceof Entity unit)) {
            LOGGER.info("[Crane] {}: unload by crane rejected - not a carrier and carried unit",
                  carrier.getDisplayName());
            return;
        }
        int unloadFacing = (facing == null) ? carrier.getFacing() : facing;
        smallCraft.getCraneOperations().add(CraneOperation.unload(unit.getId(), position, unloadFacing));
        LOGGER.info("[Crane] {} declares crane unloading of {} into {} facing {}", carrier.getDisplayName(),
              unit.getDisplayName(), position, unloadFacing);
        gameManager.entityUpdate(carrier.getId());
    }

    /**
     * End Phase work for every crane operation in the game: confirms newly declared operations, banks a turn of work on
     * the others, cancels any that no longer qualify, and loads or unloads the units whose work is done.
     */
    void checkCraneOperations() {
        // Copies: loading and unloading change transport state while we walk the units and their operations
        List<Entity> units = new ArrayList<>(getGame().getEntitiesVector());
        for (Entity entity : units) {
            if (!(entity instanceof SmallCraft carrier) || carrier.getCraneOperations().isEmpty()) {
                continue;
            }
            List<CraneOperation> operations = new ArrayList<>(carrier.getCraneOperations().getOperations());
            for (CraneOperation operation : operations) {
                if (operation.isLoading()) {
                    progressLoading(carrier, operation);
                } else {
                    progressUnloading(carrier, operation);
                }
            }
        }
    }

    private void progressLoading(SmallCraft carrier, CraneOperation operation) {
        Entity unit = getGame().getEntity(operation.getUnitId());
        if (unit == null) {
            LOGGER.info("[Crane] {}: unit {} waiting to be loaded no longer exists; operation dropped",
                  carrier.getDisplayName(), operation.getUnitId());
            endOperation(carrier, operation.getUnitId());
            return;
        }
        if (unit.isDestroyed() || unit.isDoomed() || carrier.isDestroyed() || carrier.isDoomed()) {
            cancelLoading(carrier, unit, 5357, false, "a unit involved was destroyed");
            return;
        }
        if (!CraneRules.isGroundedCarrier(carrier)) {
            cancelLoading(carrier, unit, 5356, true, "the carrier is no longer grounded");
            return;
        }
        boolean hasLeftWaitingHex = !operation.getUnitPosition().equals(unit.getPosition());
        if (hasLeftWaitingHex || (unit.delta_distance > 0)
              || !CraneRules.isInReach(unit, unit.getPosition(), carrier, getGame())) {
            cancelLoading(carrier, unit, 5354, true, "the unit moved or is out of the cranes' reach");
            return;
        }
        if (CraneRules.hasFiredWeapons(unit)) {
            cancelLoading(carrier, unit, 5355, false, "the unit fired a weapon");
            return;
        }

        if (!operation.isStarted()) {
            operation.start();
            LOGGER.info("[Crane] {} starts loading {} by crane; aboard in {} turns", carrier.getDisplayName(),
                  unit.getDisplayName(), operation.getTurnsRequired());
            Report report = unitReport(5351, unit);
            report.add(carrier.getDisplayName());
            report.add(operation.getTurnsRequired());
            addReport(report);
            gameManager.entityUpdate(carrier.getId());
            return;
        }

        int turnsCompleted = operation.bankTurn();
        if (!operation.isComplete()) {
            LOGGER.debug("[Crane] {} loading {}: turn {} of {}", carrier.getDisplayName(), unit.getDisplayName(),
                  turnsCompleted, operation.getTurnsRequired());
            Report report = unitReport(5352, unit);
            report.add(carrier.getDisplayName());
            report.add(turnsCompleted);
            report.add(operation.getTurnsRequired());
            addReport(report);
            gameManager.entityUpdate(carrier.getId());
            return;
        }

        Bay bay = CraneRules.findBayFor(carrier, unit);
        if (bay == null) {
            cancelLoading(carrier, unit, 5358, true, "the carrier has no suitable bay space left");
            return;
        }
        carrier.getCraneOperations().remove(unit.getId());
        gameManager.loadUnitByCrane(carrier, unit, bay.getBayNumber());
        LOGGER.info("[Crane] {} loads {} by crane into bay {}", carrier.getDisplayName(), unit.getDisplayName(),
              bay.getBayNumber());
        Report report = unitReport(5353, unit);
        report.add(carrier.getDisplayName());
        addReport(report);
    }

    private void progressUnloading(SmallCraft carrier, CraneOperation operation) {
        Entity unit = getGame().getEntity(operation.getUnitId());
        if ((unit == null) || (unit.getTransportId() != carrier.getId())) {
            LOGGER.info("[Crane] {}: unit {} to be unloaded is no longer aboard; operation dropped",
                  carrier.getDisplayName(), operation.getUnitId());
            endOperation(carrier, operation.getUnitId());
            return;
        }
        if (!CraneRules.isGroundedCarrier(carrier)) {
            LOGGER.info("[Crane] {}: crane unloading of {} cancelled - the carrier is no longer grounded or was "
                  + "destroyed", carrier.getDisplayName(), unit.getDisplayName());
            endOperation(carrier, unit.getId());
            Report report = unitReport(5363, unit);
            report.add(carrier.getDisplayName());
            addReport(report);
            return;
        }

        Coords unloadPosition = operation.getUnitPosition();
        if (!operation.isStarted()) {
            operation.start();
            LOGGER.info("[Crane] {} starts unloading {} by crane into {}; out in {} turns",
                  carrier.getDisplayName(), unit.getDisplayName(), unloadPosition, operation.getTurnsRequired());
            Report report = carrierReport(5359, carrier);
            report.addDesc(unit);
            report.add(unloadPosition.getBoardNum());
            report.add(operation.getTurnsRequired());
            addReport(report);
            gameManager.entityUpdate(carrier.getId());
            return;
        }

        int turnsCompleted = operation.bankTurn();
        if (turnsCompleted < operation.getTurnsRequired()) {
            LOGGER.debug("[Crane] {} unloading {}: turn {} of {}", carrier.getDisplayName(), unit.getDisplayName(),
                  turnsCompleted, operation.getTurnsRequired());
            Report report = carrierReport(5360, carrier);
            report.addDesc(unit);
            report.add(turnsCompleted);
            report.add(operation.getTurnsRequired());
            addReport(report);
            gameManager.entityUpdate(carrier.getId());
            return;
        }

        if (!CraneRules.unloadPositions(carrier, unit, getGame()).contains(unloadPosition)) {
            LOGGER.info("[Crane] {} cannot unload {} into {} this turn (hex blocked or illegal); waiting",
                  carrier.getDisplayName(), unit.getDisplayName(), unloadPosition);
            Report report = carrierReport(5362, carrier);
            report.addDesc(unit);
            report.add(unloadPosition.getBoardNum());
            addReport(report);
            return;
        }

        endOperation(carrier, unit.getId());
        if (!gameManager.unloadUnit(carrier, unit, unloadPosition, operation.getUnloadFacing(), 0)) {
            LOGGER.error("[Crane] {} failed to unload {} into {}", carrier.getDisplayName(), unit.getDisplayName(),
                  unloadPosition);
            return;
        }
        LOGGER.info("[Crane] {} unloads {} by crane into {} facing {}", carrier.getDisplayName(),
              unit.getDisplayName(), unloadPosition, operation.getUnloadFacing());
        Report report = carrierReport(5361, carrier);
        report.addDesc(unit);
        report.add(unloadPosition.getBoardNum());
        addReport(report);
        gameManager.entityUpdate(unit.getId());
    }

    private void cancelLoading(SmallCraft carrier, Entity unit, int reportId, boolean namesCarrier, String reason) {
        LOGGER.info("[Crane] crane loading of {} into {} cancelled - {}", unit.getDisplayName(),
              carrier.getDisplayName(), reason);
        endOperation(carrier, unit.getId());
        Report report = unitReport(reportId, unit);
        if (namesCarrier) {
            report.add(carrier.getDisplayName());
        }
        addReport(report);
    }

    private void endOperation(SmallCraft carrier, int unitId) {
        carrier.getCraneOperations().remove(unitId);
        gameManager.entityUpdate(carrier.getId());
    }

    private static Report unitReport(int reportId, Entity unit) {
        Report report = new Report(reportId);
        report.subject = unit.getId();
        report.addDesc(unit);
        return report;
    }

    private static Report carrierReport(int reportId, Entity carrier) {
        Report report = new Report(reportId);
        report.subject = carrier.getId();
        report.add(carrier.getDisplayName());
        return report;
    }
}
