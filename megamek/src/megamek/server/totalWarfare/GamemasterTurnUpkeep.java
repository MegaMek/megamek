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
import megamek.common.turns.SpecificEntityTurn;
import megamek.common.units.Entity;
import megamek.common.units.EntityClassTurn;
import megamek.common.units.UnitNumberTurn;
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
 * So after such an act the unit's own pending turns are dropped, the way the charge, DFA and ram resolution
 * drops them for a target it destroys ("Dead entities don't take turns" in {@link TWGameManager}), and if the
 * current turn is left with no unit that can take it, the turn is skipped exactly as the {@code /skip} command
 * would. The one departure from that resolution is how the turn is found: it uses {@code Game.removeTurnFor},
 * which only drops a turn the unit is still valid for, and a unit whose crew has ejected is valid for none.
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
        // the last of the owner's later turns it could have taken, so the owner is not asked to act with a unit
        // they no longer have. Game.removeTurnFor cannot do this: it only drops a turn the unit is still valid
        // for, and an ejected or doomed unit is valid for none.
        int specificTurnsRemoved = game.removeSpecificEntityTurnsFor(entity);
        boolean turnsChanged = specificTurnsRemoved > 0;
        if (!entity.isDone() && (specificTurnsRemoved == 0)) {
            turnsChanged = dropOwnersLastTurnFor(game, entity);
        }
        if (turnsChanged) {
            LOGGER.info("[GMTurn] {} can no longer act; its pending turn was dropped", entity.getDisplayName());
            gameManager.send(gameManager.getPacketHelper().createTurnListPacket());
        }
        // The owner's client may still have the dead unit selected for the current turn, and nothing else tells
        // it to move on: a fresh turn packet makes every client start the turn over and pick a unit that can act.
        if (currentTurn.playerId() == entity.getOwnerId()) {
            LOGGER.info("[GMTurn] {} can no longer act during its owner's turn; resending the turn so the owner's"
                  + " client selects another unit", entity.getDisplayName());
            gameManager.send(gameManager.getPacketHelper().createTurnIndexPacket(entity.getOwnerId()));
        }
        return turnsChanged;
    }

    /**
     * Drops the last turn after the current one that the given unit could have taken: one of its owner's turns
     * that names no particular unit and, where the turn is restricted to a class of unit (the movement phase
     * hands out Mek turns, vehicle turns and so on), admits the unit's class.
     *
     * @return {@code true} if a turn was dropped
     */
    private boolean dropOwnersLastTurnFor(Game game, Entity entity) {
        List<GameTurn> turns = new ArrayList<>(game.getTurnsList());
        for (int index = turns.size() - 1; index > game.getTurnIndex(); index--) {
            if (couldHaveTaken(entity, turns.get(index))) {
                turns.remove(index);
                game.setTurnVector(turns);
                return true;
            }
        }
        return false;
    }

    /** Whether the unit, were it still able to act, would be one of the units this turn is for. */
    private static boolean couldHaveTaken(Entity entity, GameTurn turn) {
        if (turn.playerId() != entity.getOwnerId()) {
            return false;
        }
        if ((turn instanceof SpecificEntityTurn) || (turn instanceof UnitNumberTurn)) {
            return false;
        }
        if (turn instanceof EntityClassTurn classTurn) {
            return classTurn.isValidClass(EntityClassTurn.getClassCode(entity));
        }
        return true;
    }
}
