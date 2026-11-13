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
package megamek.common.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.board.Coords;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * A bot keeps two copies of the game: its own, and the one its background planner (Precognition) works on. Both are
 * handed the very same unit objects from each server packet, and the planner's copy is filled second, so every unit
 * ends up pointing at the planner's copy. Moving a unit then only updates the planner's record of which unit stands
 * in which hex, never the bot's own.
 *
 * <p>These tests walk through a bot's first walk-on unit: the bot stands it on each hex it scores, the bot's own
 * record of positions is built while the unit is standing on one of them, and the server then reports the unit
 * somewhere else entirely. The bot's own copy must not keep believing the unit is on the scored hex.</p>
 */
class SharedEntityPositionLookupTest {

    private static final int UNIT_ID = 5;
    private static final Coords SCORED_HEX = new Coords(8, 0);
    private static final Coords WALK_ON_HEX = new Coords(23, 0);
    private static final Coords HEX_AFTER_MOVING = new Coords(21, 9);

    @Test
    void aUnitScoredOnAHexIsNotLeftThereWhenThePlannerSharesTheUnit() {
        Game botGame = new Game();
        Game plannerGame = new Game();

        walkOnAfterScoring(botGame, plannerGame);

        assertTrue(botGame.getEntitiesVector(SCORED_HEX, true).isEmpty(),
              "The bot's own game still lists the unit on a hex it was only scored on");
    }

    @Test
    void aUnitScoredOnAHexIsNotLeftThereWithoutAPlanner() {
        Game botGame = new Game();

        walkOnAfterScoring(botGame, botGame);

        assertTrue(botGame.getEntitiesVector(SCORED_HEX, true).isEmpty());
    }

    @Test
    void theUnitIsFoundWhereTheServerSaysItIs() {
        Game botGame = new Game();
        Game plannerGame = new Game();

        walkOnAfterScoring(botGame, plannerGame);

        List<Entity> unitsAfterMoving = botGame.getEntitiesVector(HEX_AFTER_MOVING, true);
        assertEquals(1, unitsAfterMoving.size());
        assertEquals(UNIT_ID, unitsAfterMoving.getFirst().getId());
    }

    /**
     * Plays the bot side of one walk-on. The server sends the undeployed unit to both copies of the game, the bot's
     * own first. The bot stands it on a hex it is scoring, and its own game builds its record of positions at that
     * moment, because it is still empty while nothing is on the board. The bot then puts the unit on the hex it
     * walks on from, and the server reports where the unit ended its move.
     *
     * @param botGame     the bot's own game
     * @param plannerGame the planner's game, which is filled second; the same game as {@code botGame} for a client
     *                    with no planner
     */
    private static void walkOnAfterScoring(Game botGame, Game plannerGame) {
        Entity undeployedUnit = createUnit(null);
        botGame.setEntitiesVector(new ArrayList<>(List.of(undeployedUnit)));
        if (plannerGame != botGame) {
            plannerGame.setEntitiesVector(new ArrayList<>(List.of(undeployedUnit)));
        }

        undeployedUnit.setPosition(SCORED_HEX);
        botGame.getEntitiesVector(SCORED_HEX, true);
        undeployedUnit.setPosition(WALK_ON_HEX);

        botGame.setEntity(UNIT_ID, createUnit(HEX_AFTER_MOVING));
    }

    private static Entity createUnit(@Nullable Coords position) {
        Entity unit = new BipedMek();
        unit.setId(UNIT_ID);
        unit.setPosition(position, false);
        return unit;
    }
}
