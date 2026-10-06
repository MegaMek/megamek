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

import megamek.client.ui.Messages;
import megamek.common.Hex;
import megamek.common.Report;
import megamek.common.board.Coords;
import megamek.common.event.GameToastEvent;
import megamek.common.units.BulldozerRules;
import megamek.common.units.Entity;
import megamek.common.units.RubbleClearer;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.logging.MMLogger;

/**
 * Resolves the END-phase work of vehicles clearing a rubble hex with a bulldozer (TacOps). Each round a clearing
 * vehicle banks one turn of work; once it has banked the required number of turns (2/4/8/16 by the original structure
 * type, capped at 16) the rubble is removed so units may move through the hex as clear terrain. A vehicle that is
 * destroyed, displaced out of the hex, or loses its bulldozer abandons the work. Extracted from {@link TWGameManager}
 * so that large class does not also carry the bulldozer rules; {@link TWGameManager#checkClearRubble()} delegates here
 * once per END phase.
 */
class RubbleClearingHandler extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(RubbleClearingHandler.class);

    RubbleClearingHandler(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * The number of full turns a bulldozer needs to clear rubble of the given terrain level. Delegates to
     * {@link BulldozerRules#clearingTurnsFor(int)} so the client (prompt) and server share one formula.
     *
     * @param rubbleLevel the {@link Terrains#RUBBLE} terrain level (1 = light .. 4 = hardened, 5+ = wall/ultra)
     *
     * @return the number of turns of clearing required, between 2 and 16
     */
    static int clearingTurnsFor(int rubbleLevel) {
        return BulldozerRules.clearingTurnsFor(rubbleLevel);
    }

    /**
     * The {@link Terrains#FLUFF} level used for the cosmetic cleared-rubble path overlay, derived from the destroyed
     * structure type so the tileset draws the matching "_path" tile. Ultra rubble (level 6) and any other level reuse
     * the heavy artwork, mirroring the saxarba tileset's rubble image mapping.
     *
     * @param rubbleLevel the {@link Terrains#RUBBLE} level that was cleared (1 light .. 4 hardened, 5 wall, 6+ ultra)
     *
     * @return the fluff terrain level for the cleared-rubble path overlay
     */
    private static int clearedRubbleFluffLevel(int rubbleLevel) {
        int structureType = switch (rubbleLevel) {
            case 1, 2, 4, 5 -> rubbleLevel;
            default -> 3;
        };
        return Terrains.CLEARED_RUBBLE_FLUFF_BASE + structureType;
    }

    /**
     * End-phase check for every vehicle clearing rubble: abandons work whose vehicle was destroyed, displaced, or lost
     * its bulldozer; otherwise banks a turn of clearing and removes the rubble once the work is done. TacOps.
     */
    void checkClearRubble() {
        for (Entity entity : getGame().getEntitiesVector()) {
            if (!(entity instanceof RubbleClearer clearer) || !clearer.isClearingRubble()) {
                continue;
            }
            // A unit destroyed before this check does not finish clearing; clear the state so no stale work remains.
            if (entity.isDestroyed() || entity.isDoomed()) {
                LOGGER.info("[Bulldozer] {} was destroyed before finishing clearing rubble at {}; abandoned",
                      entity.getShortName(), clearer.getRubbleClearTarget());
                clearer.cancelClearingRubble();
                continue;
            }
            // The unit must remain in the rubble hex with working clearing equipment (bulldozer, or backhoe under the
            // unofficial rule); otherwise the work is abandoned.
            if (!BulldozerRules.canClearRubble(entity, getGame())) {
                LOGGER.info("[Bulldozer] {} abandons clearing rubble at {}: clearing equipment no longer working",
                      entity.getShortName(), clearer.getRubbleClearTarget());
                abandonClearing(entity);
                continue;
            }
            Coords target = clearer.getRubbleClearTarget();
            if ((target == null) || !target.equals(entity.getPosition())) {
                LOGGER.info("[Bulldozer] {} abandons clearing rubble at {}: no longer in the hex (position {})",
                      entity.getShortName(), target, entity.getPosition());
                abandonClearing(entity);
                continue;
            }
            progressClearing(entity);
        }
    }

    /**
     * Banks one turn of clearing for a unit and completes the hex once the work is done. TacOps.
     *
     * @param entity the unit clearing rubble (a {@link RubbleClearer})
     */
    private void progressClearing(Entity entity) {
        if (!(entity instanceof RubbleClearer clearer)) {
            return;
        }
        int turnsWorked = clearer.bankRubbleClearTurn();
        int turnsRequired = clearer.getRubbleClearTurnsRequired();
        LOGGER.debug("[Bulldozer] {} rubble clearing progress: turn {} of {} at {}",
              entity.getShortName(), turnsWorked, turnsRequired, clearer.getRubbleClearTarget());
        if (turnsWorked >= turnsRequired) {
            completeClearing(entity);
        } else {
            Report report = new Report(5307);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(BulldozerRules.clearingToolName(entity));
            report.add(turnsWorked);
            report.add(turnsRequired);
            addReport(report);
        }
    }

    /**
     * Completes a rubble clear: removes the RUBBLE terrain so the hex becomes genuinely clear (passable, no rubble
     * rules), and adds a cosmetic FLUFF terrain so the tileset draws the "_path" tiles marking that the hex was once
     * rubble. Updates the clients and clears the vehicle's clearing state. TacOps. If the hex no longer holds rubble
     * (already cleared by other means) the work is simply ended.
     *
     * @param entity the unit finishing the clear (a {@link RubbleClearer})
     */
    private void completeClearing(Entity entity) {
        if (!(entity instanceof RubbleClearer clearer)) {
            return;
        }
        Coords target = clearer.getRubbleClearTarget();
        int boardId = entity.getBoardId();
        Hex hex = getGame().getBoard(boardId).getHex(target);
        Terrain rubble = (hex == null) ? null : hex.getTerrain(Terrains.RUBBLE);
        if (rubble != null) {
            // The rubble is bulldozed away: the hex is clear terrain now, with only cosmetic overlays left. Two
            // stacked super layers (both gameplay-free): a GROUND_FLUFF "scraped ground" layer (quicksand_0) under a
            // FLUFF "_path" layer (the debris pushed into the wedges). Tilesets without these entries just draw clear.
            hex.removeTerrain(Terrains.RUBBLE);
            hex.addTerrain(new Terrain(Terrains.GROUND_FLUFF, Terrains.CLEARED_RUBBLE_FLUFF_BASE));
            hex.addTerrain(new Terrain(Terrains.FLUFF, clearedRubbleFluffLevel(rubble.getLevel())));
            gameManager.sendChangedHex(target, boardId);
            LOGGER.info("[Bulldozer] {} finished clearing rubble (level {}) at {}; the hex is now clear",
                  entity.getShortName(), rubble.getLevel(), target);
            Report report = new Report(5308);
            report.subject = entity.getId();
            report.addDesc(entity);
            report.add(target.getBoardNum());
            addReport(report);
            gameManager.sendToast(GameToastEvent.Level.SUCCESS,
                  Messages.getString("Bulldozer.completeClearToast", entity.getShortName(), target.getBoardNum()),
                  entity);
        } else {
            LOGGER.info("[Bulldozer] {} finished clearing at {} but the hex no longer holds rubble",
                  entity.getShortName(), target);
        }
        clearer.cancelClearingRubble();
    }

    /**
     * Abandons a unit's rubble-clearing work, clearing its state and reporting the loss of progress. TacOps.
     *
     * @param entity the unit abandoning the clear (a {@link RubbleClearer})
     */
    private void abandonClearing(Entity entity) {
        if (!(entity instanceof RubbleClearer clearer)) {
            return;
        }
        Report report = new Report(5309);
        report.subject = entity.getId();
        report.addDesc(entity);
        addReport(report);
        clearer.cancelClearingRubble();
    }
}
