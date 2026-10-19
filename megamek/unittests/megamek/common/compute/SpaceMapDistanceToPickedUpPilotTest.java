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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BoardType;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.Gender;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.MekWarrior;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for issue #9049: a space battle froze at the start of the Physical Attack phase.
 *
 * <p>An ejected pilot who is picked up by a friendly ship leaves the map (their position is cleared) but stays in the
 * game. When an enemy ejected pilot still floating on the map was checked for physical attacks, the distance to the
 * picked-up pilot was measured, and on a space map that measurement walked towards a hex that does not exist.</p>
 */
class SpaceMapDistanceToPickedUpPilotTest {

    private static final int BOARD_WIDTH = 30;
    private static final int BOARD_HEIGHT = 30;
    private static final int OUR_PLAYER_ID = 1;
    private static final int THEIR_PLAYER_ID = 2;

    private Game game;
    private Player ourPlayer;
    private Player theirPlayer;
    private MekWarrior floatingPilot;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.setBoard(spaceBoard());

        ourPlayer = new Player(OUR_PLAYER_ID, "Us");
        ourPlayer.setTeam(1);
        theirPlayer = new Player(THEIR_PLAYER_ID, "Them");
        theirPlayer.setTeam(2);
        game.addPlayer(OUR_PLAYER_ID, ourPlayer);
        game.addPlayer(THEIR_PLAYER_ID, theirPlayer);

        floatingPilot = pilot(1, ourPlayer, new Coords(10, 10));
    }

    private static Board spaceBoard() {
        Hex[] hexes = new Hex[BOARD_WIDTH * BOARD_HEIGHT];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex();
        }
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT, hexes);
        board.setBoardType(BoardType.FAR_SPACE);
        return board;
    }

    private MekWarrior pilot(int id, Player owner, Coords position) {
        Crew crew = new Crew(CrewType.SINGLE, "Pilot " + id, 1, 4, 5, Gender.FEMALE, false, null);
        MekWarrior pilot = new MekWarrior(crew, owner, game);
        pilot.setId(id);
        pilot.setOwner(owner);
        pilot.setPosition(position);
        pilot.setDeployed(true);
        game.addEntity(pilot, false);
        return pilot;
    }

    /** The same state the pick-up leaves behind: still in the game and deployed, but with no hex. */
    private MekWarrior pickedUpPilot(int id, Player owner) {
        MekWarrior pilot = pilot(id, owner, new Coords(11, 10));
        pilot.setPickedUpById(99);
        pilot.setPosition(null);
        return pilot;
    }

    @Test
    void distanceToAPickedUpPilotOnASpaceMapIsOutOfReach() {
        MekWarrior pickedUp = pickedUpPilot(2, theirPlayer);

        int distance = assertDoesNotThrow(() -> Compute.effectiveDistance(game, floatingPilot, pickedUp));

        assertTrue(distance > 1, "a unit with no hex must never count as adjacent, was " + distance);
    }

    @Test
    void physicalPhaseEligibilityIgnoresAPickedUpEnemyPilotOnASpaceMap() {
        pickedUpPilot(2, theirPlayer);

        assertDoesNotThrow(() -> floatingPilot.isEligibleFor(GamePhase.PHYSICAL));
    }

    /** The positive control: two pilots that are both on the map still measure the real distance between them. */
    @Test
    void distanceBetweenTwoPilotsOnASpaceMapIsStillMeasured() {
        MekWarrior adjacentPilot = pilot(2, theirPlayer, new Coords(11, 10));
        MekWarrior distantPilot = pilot(3, theirPlayer, new Coords(10, 14));

        assertEquals(1, Compute.effectiveDistance(game, floatingPilot, adjacentPilot));
        assertEquals(4, Compute.effectiveDistance(game, floatingPilot, distantPilot));
    }
}
