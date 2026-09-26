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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.pathfinder.BoardClusterTracker;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WaypointDistanceField} on a real board: a unit behind a cliff wall is further from the waypoint by
 * the real route than a unit further away in the straight line but on the open side (issue #7615).
 */
class WaypointDistanceFieldTest {

    private static final int WIDTH = 12;
    private static final int HEIGHT = 12;
    private static final int WALL_ROW = 5;
    private static final int GAP_COLUMN = 10;
    private static final int CLIFF_LEVEL = 5;
    private static final Coords WAYPOINT = new Coords(2, 2);

    private BipedMek mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /** Clear ground with a cliff wall across {@link #WALL_ROW}, open only from {@link #GAP_COLUMN} eastward. */
    private static Board boardWithWall() {
        Hex[] hexes = new Hex[WIDTH * HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Hex hex = new Hex();
                if ((y == WALL_ROW) && (x < GAP_COLUMN)) {
                    hex.setLevel(CLIFF_LEVEL);
                }
                hexes[(y * WIDTH) + x] = hex;
            }
        }
        return new Board(WIDTH, HEIGHT, hexes);
    }

    @BeforeEach
    void setUp() {
        Game game = new Game();
        game.setBoard(boardWithWall());
        mek = new BipedMek();
        mek.setGame(game);
    }

    @Test
    void theWaypointItselfCostsNothing() {
        WaypointDistanceField field = WaypointDistanceField.build(mek, WAYPOINT);

        assertEquals(0, field.costFrom(WAYPOINT));
    }

    @Test
    void behindTheWallIsFurtherThanTheOpenSide() {
        WaypointDistanceField field = WaypointDistanceField.build(mek, WAYPOINT);
        Coords behindWall = new Coords(2, 7);
        Coords openSide = new Coords(GAP_COLUMN, 6);

        // In a straight line the hex behind the wall is closer...
        assertTrue(behindWall.distance(WAYPOINT) < openSide.distance(WAYPOINT));
        // ...but by the route round the wall it is further.
        assertTrue(field.costFrom(behindWall) > field.costFrom(openSide),
              "behind wall " + field.costFrom(behindWall) + " vs open side " + field.costFrom(openSide));
    }

    @Test
    void theClifftopCannotBeReached() {
        WaypointDistanceField field = WaypointDistanceField.build(mek, WAYPOINT);

        assertEquals(WaypointDistanceField.UNREACHABLE, field.costFrom(new Coords(4, WALL_ROW)));
    }

    @Test
    void aWaypointTheQuickCheckRefusesIsKeptWhenARouteExists() {
        // In test games the cluster check refused waypoints the unit could walk to, and the unit parked.
        Princess princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        BoardClusterTracker refusingTracker = mock(BoardClusterTracker.class);
        when(refusingTracker.getDestinationCoords(any(Entity.class), any(Coords.class), anyBoolean()))
              .thenReturn(Collections.emptySet());
        doReturn(refusingTracker).when(princess).getClusterTracker();
        doReturn(mek.getGame()).when(princess).getGame();
        mek.setPosition(new Coords(2, 7));

        assertTrue(princess.getUnitOrdersFollower().canReach(mek, WAYPOINT));
        assertFalse(princess.getUnitOrdersFollower().canReach(mek, new Coords(4, WALL_ROW)));
    }

    @Test
    void roughTerrainCostsMore() {
        // heavy woods down column 3, from the north edge to the wall, so there is no way round them
        Board board = boardWithWall();
        for (int y = 0; y < WALL_ROW; y++) {
            board.getHex(3, y).addTerrain(new Terrain(Terrains.WOODS, 2));
        }
        Game game = new Game();
        game.setBoard(board);
        mek.setGame(game);

        WaypointDistanceField field = WaypointDistanceField.build(mek, new Coords(4, 2));

        // stepping into heavy woods costs more than stepping into clear ground the same distance away
        assertTrue(field.costFrom(new Coords(2, 2)) > field.costFrom(new Coords(6, 2)));
    }
}
