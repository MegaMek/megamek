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

import java.util.Vector;

import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.BridgeConstruction;
import megamek.common.board.Coords;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;

/**
 * Resolves the END-phase work of Bridge-Building Engineers, TO:AUE p.152: banks each platoon's turn of building or
 * dismantling, applies casualty extensions, abandons work whose platoon was destroyed or displaced, and places the
 * finished bridge (or repaired section) on the board. Extracted from {@link TWGameManager} so that large class does not
 * also carry the bridge rules; {@link TWGameManager#checkBuildBridges()} delegates here once per END phase.
 */
class BridgeBuildPhaseHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(BridgeBuildPhaseHandler.class);

    BridgeBuildPhaseHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * End-phase checks for every platoon with bridge work in progress: reports build progress, extends the build by a
     * turn when the platoon took casualties this turn, abandons builds whose platoon is destroyed or no longer adjacent
     * to the site, and completes bridges whose work is done. TO:AUE p.152.
     */
    void checkBuildBridges() {
        for (Entity entity : getGame().getEntitiesVector()) {
            if (!(entity instanceof ConvInfantry convInfantry) || !convInfantry.hasBridgeInProgress()) {
                continue;
            }
            // A platoon destroyed before this completion check does not finish its bridge, TO:AUE p.152 ("if destroyed
            // before completing its task, the bridge is destroyed as well"). This also covers a platoon wiped out
            // during the combat of its final turn that is still doomed in the entity list when the END phase runs, and
            // a platoon that was paused or dismantling - clearing the state so no stale reservation/indicator remains.
            if (entity.isDestroyed() || entity.isDoomed()) {
                LOGGER.info("[BuildBridge] {} was destroyed before finishing its bridge work at {}; abandoned",
                      convInfantry.getShortName(), convInfantry.getBridgeTargetCoords());
                convInfantry.cancelBridgeBuild();
                continue;
            }
            // A paused build does not advance and the platoon is free to move away, so the displacement check and the
            // per-round progression below apply only while actively building or dismantling.
            if (!convInfantry.isBusyWithBridge()) {
                continue;
            }
            if (!convInfantry.isAdjacentToBridgeSite()) {
                LOGGER.info("[BuildBridge] {} abandons its bridge work at {}: no longer adjacent (position {})",
                      convInfantry.getShortName(), convInfantry.getBridgeTargetCoords(),
                      convInfantry.getPosition());
                convInfantry.cancelBridgeBuild();
                Report report = new Report(4278);
                report.subject = convInfantry.getId();
                report.addDesc(convInfantry);
                addReport(report);
                continue;
            }
            if (convInfantry.isDismantlingBridge()) {
                progressBridgeDismantle(convInfantry);
            } else {
                progressBridgeBuild(convInfantry);
            }
        }
    }

    /**
     * Advances one turn of bridge building for a platoon during the END phase, applying any casualty extension and
     * completing the bridge once the work is done. TO:AUE p.152.
     *
     * @param convInfantry the platoon building a bridge
     */
    private void progressBridgeBuild(ConvInfantry convInfantry) {
        if (convInfantry.updateBridgeBuildCasualties()) {
            Report report = new Report(4279);
            report.subject = convInfantry.getId();
            report.addDesc(convInfantry);
            addReport(report);
        }
        // The platoon spent this whole round on the bridge (it could take no other action), so bank a turn of work now.
        int turnsWorked = convInfantry.bankBridgeBuildTurn();
        LOGGER.debug("[BuildBridge] {} bridge build progress: turn {} of {} at {}",
              convInfantry.getShortName(), turnsWorked, convInfantry.getBridgeBuildRequiredTurns(),
              convInfantry.getBridgeTargetCoords());
        if (turnsWorked >= convInfantry.getBridgeBuildRequiredTurns()) {
            finishBridgeBuild(convInfantry);
        } else {
            Report report = new Report(4276);
            report.subject = convInfantry.getId();
            report.addDesc(convInfantry);
            report.add(turnsWorked);
            report.add(convInfantry.getBridgeBuildRequiredTurns());
            addReport(report);
        }
    }

    /**
     * Advances one turn of bridge dismantling for a platoon during the END phase. Once the work is done the spent
     * bridge building budget is refunded and the platoon is freed. TO:AUE p.152.
     *
     * @param convInfantry the platoon dismantling its cancelled bridge
     */
    private void progressBridgeDismantle(ConvInfantry convInfantry) {
        // The platoon spent this whole round dismantling, so bank a turn of dismantling now.
        int turnsWorked = convInfantry.bankBridgeDismantleTurn();
        LOGGER.debug("[BuildBridge] {} bridge dismantling progress: turn {} of {} at {}",
              convInfantry.getShortName(), turnsWorked, convInfantry.getBridgeDismantleRequiredTurns(),
              convInfantry.getBridgeTargetCoords());
        if (turnsWorked >= convInfantry.getBridgeDismantleRequiredTurns()) {
            Coords target = convInfantry.getBridgeTargetCoords();
            convInfantry.refundBridgeBuildPoints();
            convInfantry.cancelBridgeBuild();
            LOGGER.info("[BuildBridge] {} finished dismantling its bridge at {}; budget refunded",
                  convInfantry.getShortName(), target);
            Report report = new Report(4283);
            report.subject = convInfantry.getId();
            report.addDesc(convInfantry);
            if (target != null) {
                report.add(target.getBoardNum());
            } else {
                report.add("?");
            }
            addReport(report);
        } else {
            // Report the standing structure counting back down on the build's scale (e.g. 3 of 6 remaining)
            Report report = new Report(4282);
            report.subject = convInfantry.getId();
            report.addDesc(convInfantry);
            report.add(convInfantry.getBridgeDismantleRemaining());
            report.add(convInfantry.getBridgeBuildRequiredTurns());
            addReport(report);
        }
    }

    /**
     * Completes a bridge build, TO:AUE p.152: places the new bridge (or repaired section) on the board, registers it as
     * a structure, and updates the clients with the changed hex and the new bridge. If the site became invalid while
     * building (e.g. another structure appeared there), the work is abandoned instead.
     *
     * @param convInfantry the platoon finishing its bridge
     */
    private void finishBridgeBuild(ConvInfantry convInfantry) {
        Coords target = convInfantry.getBridgeTargetCoords();
        int boardId = convInfantry.getBoardId();
        Board board = getGame().getBoard(boardId);
        int exits = convInfantry.getBridgeExits();
        boolean isRepair = convInfantry.isBridgeBuildRepair();
        String featureTag = isRepair ? "BridgeRepair" : "BuildBridge";
        // A repair must still be a repairable gap, and a fresh build must still be a valid site - either may have
        // become invalid while building (e.g. the surviving span collapsed, or a structure appeared in the hex).
        boolean siteStillValid = isRepair
              ? BridgeConstruction.isBridgeRepairSite(board, target, exits)
              : BridgeConstruction.isValidBridgeSite(board, target, exits);
        if (!siteStillValid) {
            LOGGER.info("[{}] {} abandons its bridge work: {} is no longer a valid {}", featureTag,
                  convInfantry.getShortName(), target, isRepair ? "repair gap" : "bridge site");
            convInfantry.cancelBridgeBuild();
            Report report = new Report(4278);
            report.subject = convInfantry.getId();
            report.addDesc(convInfantry);
            addReport(report);
            return;
        }

        boolean isOverWater = BridgeConstruction.isOverWater(board.getHex(target));
        int constructionFactor = ConvInfantry.getBuiltBridgeCF(convInfantry.getBridgeType(), isOverWater);
        IBuilding bridge = isRepair
              ? BridgeConstruction.placeRepairedBridge(board, target, exits, convInfantry.getBridgeType(),
              constructionFactor)
              : BridgeConstruction.placeBridge(board, target, exits, convInfantry.getBridgeType(), constructionFactor);
        LOGGER.info("[{}] {} completed a type-{} bridge {} at {} (CF {}, over water: {}, exits bitmask {})", featureTag,
              convInfantry.getShortName(), convInfantry.getBridgeType(), isRepair ? "repair" : "build", target,
              constructionFactor, isOverWater, exits);
        gameManager.sendChangedHex(target, boardId);
        Vector<IBuilding> newBridges = new Vector<>();
        newBridges.add(bridge);
        gameManager.sendNewBuildings(newBridges);

        Report report = new Report(isRepair ? 4289 : 4277);
        report.subject = convInfantry.getId();
        report.addDesc(convInfantry);
        report.add(constructionFactor);
        report.add(target.getBoardNum());
        addReport(report);
        convInfantry.cancelBridgeBuild();
    }
}
