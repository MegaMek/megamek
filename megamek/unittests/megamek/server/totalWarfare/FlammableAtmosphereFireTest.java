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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.units.Terrains;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests the fire a flammable atmosphere spreads of its own accord, TO:AR p.54.
 * <p>
 * In flammable toxic air a fire started by an inferno round or by explosive ordnance does not creep outwards over
 * following turns as an ordinary fire does; it takes every adjacent hex at once. These tests cover which hexes are
 * caught and, as importantly, which are left alone.
 */
class FlammableAtmosphereFireTest {

    /** A patch of woods with a stretch of water along one side, so both the burnable and unburnable cases appear. */
    private static final String BOARD_DATA = """
          size 5 5
          hex 0101 0 "woods:1" ""
          hex 0102 0 "woods:1" ""
          hex 0103 0 "woods:1" ""
          hex 0104 0 "woods:1" ""
          hex 0105 0 "woods:1" ""
          hex 0201 0 "woods:1" ""
          hex 0202 0 "woods:1" ""
          hex 0203 0 "woods:1" ""
          hex 0204 0 "woods:1" ""
          hex 0205 0 "woods:1" ""
          hex 0301 0 "woods:1" ""
          hex 0302 0 "woods:1" ""
          hex 0303 0 "woods:1" ""
          hex 0304 0 "woods:1" ""
          hex 0305 0 "woods:1" ""
          hex 0401 0 "water:2" ""
          hex 0402 0 "water:2" ""
          hex 0403 0 "water:2" ""
          hex 0404 0 "water:2" ""
          hex 0405 0 "water:2" ""
          hex 0501 0 "" ""
          hex 0502 0 "" ""
          hex 0503 0 "" ""
          hex 0504 0 "" ""
          hex 0505 0 "" ""
          end""";

    private static final Coords CENTRE_OF_THE_WOODS = new Coords(1, 1);

    private TWGameManager gameManager;
    private Game game;
    private Board board;
    private TaintedAtmosphereHandler handler;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(anyString());
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.FIRING);
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE).setValue(true);
        board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);

        handler = new TaintedAtmosphereHandler(gameManager);
    }

    private boolean isOnFire(Coords coords) {
        return board.getHex(coords).containsTerrain(Terrains.FIRE);
    }

    private int burningNeighbourCount(Coords coords) {
        return (int) coords.allAdjacent().stream().filter(neighbour -> board.getHex(neighbour) != null)
              .filter(this::isOnFire).count();
    }

    @Test
    @DisplayName("In flammable toxic air an explosive fire takes every burnable hex around it at once")
    void anExplosiveFireSpreadsToEveryAdjacentHex() {
        game.getPlanetaryConditions().setAtmosphericTaint(AtmosphericTaint.FLAMMABLE_TOXIC);
        Vector<Report> reports = new Vector<>();

        handler.spreadExplosiveFire(CENTRE_OF_THE_WOODS, 0, 1, reports);

        assertEquals(6, burningNeighbourCount(CENTRE_OF_THE_WOODS),
              "every one of the six surrounding woods hexes should have caught");
        assertFalse(reports.isEmpty(), "the spread should be reported to the players");
    }

    @Test
    @DisplayName("Water beside the fire is left alone")
    void waterDoesNotCatch() {
        game.getPlanetaryConditions().setAtmosphericTaint(AtmosphericTaint.FLAMMABLE_TOXIC);
        Coords woodsBesideTheWater = new Coords(2, 2);

        handler.spreadExplosiveFire(woodsBesideTheWater, 0, 1, new Vector<>());

        for (Coords neighbour : woodsBesideTheWater.allAdjacent()) {
            if ((board.getHex(neighbour) != null) && board.getHex(neighbour).containsTerrain(Terrains.WATER)) {
                assertFalse(isOnFire(neighbour), neighbour + " is water and must not burn");
            }
        }
        assertTrue(burningNeighbourCount(woodsBesideTheWater) > 0,
              "the woods on the other side should still have caught");
    }

    @Test
    @DisplayName("Flammable tainted air spreads a fire no faster than usual")
    void taintedAirDoesNotSpreadExplosiveFires() {
        game.getPlanetaryConditions().setAtmosphericTaint(AtmosphericTaint.FLAMMABLE_TAINTED);
        Vector<Report> reports = new Vector<>();

        handler.spreadExplosiveFire(CENTRE_OF_THE_WOODS, 0, 1, reports);

        assertEquals(0, burningNeighbourCount(CENTRE_OF_THE_WOODS),
              "only toxic air carries a fire straight into the neighbouring hexes");
        assertTrue(reports.isEmpty(), "and nothing should be reported");
    }

    @Test
    @DisplayName("Air that is not flammable at all spreads nothing")
    void breathableAirSpreadsNothing() {
        game.getPlanetaryConditions().setAtmosphericTaint(AtmosphericTaint.BREATHABLE);

        handler.spreadExplosiveFire(CENTRE_OF_THE_WOODS, 0, 1, new Vector<>());

        assertEquals(0, burningNeighbourCount(CENTRE_OF_THE_WOODS), "breathable air does nothing of the sort");
    }
}
