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

import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Keeps the turn order sound after a gamemaster act has taken a unit out of action outside the phase flow: a
 * {@code /kill}, a fatal damage edit, or an equipment explosion that destroyed the unit or ejected its crew.
 * <p>
 * In play a unit that dies mid-move ends its own turn, and the phase machinery never hands a turn to a unit that
 * cannot take it. A gamemaster's act has no such hook: it lands between packets, so the unit keeps the turn it
 * held and, being no longer selectable (a Mek whose crew has ejected counts as abandoned until its destruction
 * is finalised at the end of the phase), it can never use it. Its player's movement is refused as invalid and the
 * game waits on a dead unit for ever. This is what happened to an Akuma blown up by its own ammo in the
 * movement phase.
 * </p>
 * <p>
 * So after such an act the unit's own pending turns are dropped, the way the phase flow drops them for a unit
 * removed in the movement phase, and if the current turn is left with no unit that can take it, the turn is
 * skipped exactly as the {@code /skip} command would.
 * </p>
 */
class GamemasterTurnUpkeep extends AbstractTWRuleHandler {

    private static final MMLogger LOGGER = MMLogger.create(GamemasterTurnUpkeep.class);

    GamemasterTurnUpkeep(TWGameManager gameManager) {
        super(gameManager);
    }

    /**
     * Drops the pending turns of a unit a gamemaster act has just taken out of action, and skips the current turn
     * if nobody can take it any more. Does nothing for a unit that can still act, or outside the phases that use
     * turns.
     *
     * @param entity the unit the gamemaster acted on
     *
     * @return {@code true} if a turn was dropped or skipped
     */
    boolean settleTurnsAfter(Entity entity) {
        Game game = getGame();
        if (!game.getPhase().usesTurns()) {
            return false;
        }
        boolean canStillAct = entity.isSelectableThisTurn() && !entity.isDoomed() && !entity.isDestroyed();
        if (canStillAct) {
            return false;
        }

        // Nobody is left to take the current turn: skip it as /skip would, which also moves the game on to the
        // next turn or the next phase. This consumes the dead unit's own turn where it held the current one.
        GameTurn currentTurn = game.getTurn();
        if ((currentTurn != null) && (game.getFirstEntity() == null)) {
            LOGGER.info("[GMTurn] {} can no longer act and no unit can take the current turn ({}); skipping it",
                  entity.getDisplayName(), currentTurn);
            gameManager.skipCurrentTurn();
            return true;
        }

        // The current turn is usable by another unit, so only the dead unit's own share of the turns goes: any
        // turn naming it specifically, or else - each unit standing for one of its owner's turns in the phase -
        // the last of the owner's later turns, so the owner is not asked to act with a unit they no longer have.
        // Game.removeTurnFor cannot do this: it only drops a turn the unit is still valid for, and an ejected or
        // doomed unit is valid for none.
        int specificTurnsRemoved = game.removeSpecificEntityTurnsFor(entity);
        boolean turnsChanged = specificTurnsRemoved > 0;
        if (!entity.isDone() && (specificTurnsRemoved == 0)) {
            turnsChanged = dropOwnersLastPlainTurn(game, entity.getOwnerId());
        }
        if (turnsChanged) {
            LOGGER.info("[GMTurn] {} can no longer act; its pending turn was dropped", entity.getDisplayName());
            gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
        }
        return turnsChanged;
    }

    /**
     * Drops the last turn after the current one that belongs to the given player and names no unit or class in
     * particular, which is the turn the dead unit would have taken.
     *
     * @return {@code true} if a turn was dropped
     */
    private boolean dropOwnersLastPlainTurn(Game game, int ownerId) {
        List<GameTurn> turns = new ArrayList<>(game.getTurnsList());
        for (int index = turns.size() - 1; index > game.getTurnIndex(); index--) {
            GameTurn turn = turns.get(index);
            boolean isPlainPlayerTurn = (turn.getClass() == GameTurn.class) && (turn.playerId() == ownerId);
            if (isPlainPlayerTurn) {
                turns.remove(index);
                game.setTurnVector(turns);
                return true;
            }
        }
        return false;
    }
}
