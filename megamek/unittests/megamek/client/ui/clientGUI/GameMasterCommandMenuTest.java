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

package megamek.client.ui.clientGUI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.server.commands.ClientServerCommand;
import megamek.server.commands.arguments.CoordXArgument;
import megamek.server.commands.arguments.CoordYArgument;
import megamek.server.commands.arguments.UnitArgument;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the rule that decides which Game Master special commands each menu offers.
 *
 * <p>Right-clicking a hex means "do something to this hex", so that menu offers only the commands that act on one
 * hex. The commands that act on the whole map have nothing to do with the hex under the cursor and belong on the game
 * commands strip, which implies no hex. The two menus therefore offer different commands, not overlapping ones.</p>
 *
 * <p>Commands that act on a single unit belong in that unit's Edit Damage dialog and must appear in neither menu.</p>
 */
class GameMasterCommandMenuTest {

    /**
     * A board holding one hex of each kind the menu cares about, so the tests can ask what is offered on a hex that
     * has a building, one that has terrain worth modifying, and one that has neither.
     */
    private static final String BOARD_DATA = """
          size 4 4
          hex 0101 0 "" ""
          hex 0102 0 "woods:1" ""
          hex 0103 0 "bldg_elev:2;building:2;bldg_class:1;bldg_cf:40" ""
          hex 0104 0 "" ""
          end""";

    private static final Coords BARE_HEX = new Coords(0, 0);
    private static final Coords WOODS_HEX = new Coords(1, 0);
    private static final Coords BUILDING_HEX = new Coords(2, 0);

    private Board board;

    @BeforeEach
    void beforeEach() {
        board = BoardLoader.initializeBoard(BOARD_DATA);
    }

    /** @return the names of the commands offered on the given hex */
    private List<String> offeredOn(Coords coords) {
        return GameMasterCommandMenu.commandsFor(coords, board)
              .stream()
              .map(ClientServerCommand::getName)
              .toList();
    }

    /** @return {@code true} if the command asks for a hex, which it can only get from a right-clicked hex */
    private static boolean takesAHex(ClientServerCommand command) {
        return command.defineArguments()
              .stream()
              .anyMatch(argument -> (argument instanceof CoordXArgument) || (argument instanceof CoordYArgument));
    }

    /** @return {@code true} if the command asks for a unit, which makes it a unit tool rather than a map tool */
    private static boolean takesAUnit(ClientServerCommand command) {
        return command.defineArguments().stream().anyMatch(UnitArgument.class::isInstance);
    }

    @Test
    void withoutAHexOffersOnlyCommandsThatNeedNone() {
        List<ClientServerCommand> offered = GameMasterCommandMenu.commandsFor(null, null);

        assertFalse(offered.isEmpty(), "the board-wide commands should be offered without a hex");
        for (ClientServerCommand command : offered) {
            assertFalse(takesAHex(command),
                  command.getName() + " takes a hex, so it must not be offered where there is none");
        }
    }

    @Test
    void withAHexOffersOnlyCommandsThatActOnThatHex() {
        List<ClientServerCommand> offered = GameMasterCommandMenu.commandsFor(BUILDING_HEX, board);

        assertFalse(offered.isEmpty(), "the hex commands should be offered when a hex was right-clicked");
        for (ClientServerCommand command : offered) {
            assertTrue(takesAHex(command),
                  command.getName() + " does not act on a hex, so it belongs on the game commands strip instead");
        }
    }

    @Test
    void theTwoMenusOfferDifferentCommands() {
        List<String> boardWide = GameMasterCommandMenu.commandsFor(null, null)
              .stream()
              .map(ClientServerCommand::getName)
              .toList();
        List<String> hexTargeted = GameMasterCommandMenu.commandsFor(BUILDING_HEX, board)
              .stream()
              .map(ClientServerCommand::getName)
              .toList();

        assertTrue(hexTargeted.stream().noneMatch(boardWide::contains),
              "a command should appear in one menu or the other, never both");
    }

    @Test
    void neitherMenuOffersAUnitCommand() {
        List<ClientServerCommand> everythingOffered = new ArrayList<>(GameMasterCommandMenu.commandsFor(null, null));
        everythingOffered.addAll(GameMasterCommandMenu.commandsFor(BUILDING_HEX, board));

        for (ClientServerCommand command : everythingOffered) {
            assertFalse(takesAUnit(command),
                  command.getName() + " acts on one unit, so it belongs in that unit's Edit Damage dialog");
        }
    }

    @Test
    void buildingsAreNotAmongTheseCommands() {
        assertFalse(offeredOn(BUILDING_HEX).contains("building"),
              "buildings have their own dialog, opened from the map menu, not a generated form");
    }

    @Test
    void theTerrainFactorIsNotAmongTheseCommands() {
        assertFalse(offeredOn(WOODS_HEX).contains("modifyterrain"),
              "the terrain factor is set in the Change Terrain dialog, beside the terrain it belongs to");
    }

    @Test
    void aCommandThatWorksAnywhereIsOfferedOnABareHex() {
        assertTrue(offeredOn(BARE_HEX).contains("firestarter"),
              "a hex can be set alight whatever is or is not in it");
    }

    @Test
    void changeTerrainIsNotAmongTheseCommands() {
        assertFalse(offeredOn(BARE_HEX).contains("changeterrain"),
              "changing what a hex is made of has its own dialog, opened from the map menu, not a generated form");
    }
}
