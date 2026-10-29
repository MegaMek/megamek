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
package megamek.server.nuclear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collection;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.net.packets.Packet;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Runs real detonations on an empty board and checks what they do to the terrain (IO:AE 3rd printing, pp. 164-165).
 */
class NuclearDetonationResolverTest {
    private static final int BOARD_SIZE = 81;
    private static final Coords GROUND_ZERO = new Coords(40, 40);

    private TWGameManager gameManager;
    private Board board;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        gameManager.getGame().addPlayer(0, new Player(0, "Test"));

        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        board = new Board(BOARD_SIZE, BOARD_SIZE, hexes);
        gameManager.getGame().setBoard(board);
    }

    private void detonate(NuclearWeaponType weaponType) {
        new NuclearDetonationResolver(gameManager).resolve(GROUND_ZERO,
              NuclearBlastProfile.groundBurst(weaponType),
              new Vector<Report>());
    }

    /** A hex straight "north" of Ground Zero, so its distance is exactly {@code distance}. */
    private Hex hexAt(int distance) {
        return board.getHex(coordsAt(distance));
    }

    private static Coords coordsAt(int distance) {
        return new Coords(GROUND_ZERO.getX(), GROUND_ZERO.getY() - distance);
    }

    /** Rebuilds the board with one building hex, so the board registers the building. */
    private void placeBuilding(Coords coords, int buildingType, int constructionFactor, int height) {
        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        Hex buildingHex = hexes[(coords.getY() * BOARD_SIZE) + coords.getX()];
        buildingHex.addTerrain(new Terrain(Terrains.BUILDING, buildingType));
        buildingHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, height));
        buildingHex.addTerrain(new Terrain(Terrains.BLDG_CF, constructionFactor));
        board = new Board();
        board.newData(BOARD_SIZE, BOARD_SIZE, hexes, null);
        gameManager.getGame().setBoard(board);
    }

    @Test
    void santaAnaCraterMatchesTheBookExample() {
        // IO:AE p.164: 3 levels at Ground Zero, 2 at 2 hexes, 1 at 4 hexes, no crater at 6 hexes
        detonate(NuclearWeaponType.SANTA_ANA);

        int[] expectedLevels = { -3, -3, -2, -2, -1, -1, 0 };
        for (int distance = 0; distance < expectedLevels.length; distance++) {
            assertEquals(expectedLevels[distance], hexAt(distance).getLevel(), "level at " + distance + " hexes");
        }
    }

    @Test
    void craterHexesBecomeOrdinaryRubble() {
        // IO:AE p.164: everything in the crater is annihilated and replaced by rubble. The Santa Ana crater ends
        // after 5 hexes.
        detonate(NuclearWeaponType.SANTA_ANA);

        assertEquals(NuclearTerrainEffects.ORDINARY_RUBBLE, hexAt(5).terrainLevel(Terrains.RUBBLE),
              "the crater's edge is ordinary rubble");
        assertFalse(hexAt(6).containsTerrain(Terrains.RUBBLE), "6 hexes is past the crater");
    }

    @Test
    void buildingInTheCraterLeavesItsOwnRubble() {
        // A Heavy building 3 hexes from a Santa Ana collapses into heavy rubble (HammerGS, 2026-10-28)
        placeBuilding(coordsAt(3), 3, 90, 2);

        detonate(NuclearWeaponType.SANTA_ANA);

        assertNull(board.getBuildingAt(coordsAt(3)), "the building is gone from the board");
        assertFalse(hexAt(3).containsTerrain(Terrains.BUILDING), "no building terrain is left");
        assertEquals(3, hexAt(3).terrainLevel(Terrains.RUBBLE), "heavy rubble");
    }

    @Test
    void terrainRingTakesTheDamageForItsOwnDistance() {
        // Alamo at 34 hexes: 1,000 - 34 x 23 = 218, which turns a clear hex rough (200 or more, IO:AE p.165).
        // At 35 hexes: 1,000 - 35 x 23 = 195, not enough.
        detonate(NuclearWeaponType.ALAMO);

        assertTrue(hexAt(34).containsTerrain(Terrains.ROUGH), "34 hexes takes 218 and becomes rough");
        assertTrue(!hexAt(35).containsTerrain(Terrains.ROUGH), "35 hexes takes 195 and stays clear");
    }

    @Test
    void changedHexesGoToTheClientsInOneUpdate() {
        detonate(NuclearWeaponType.SANTA_ANA);

        ArgumentCaptor<Collection<Coords>> changed = ArgumentCaptor.captor();
        Mockito.verify(gameManager, Mockito.times(1)).sendChangedHexes(changed.capture(), eq(0));
        Mockito.verify(gameManager, Mockito.never()).sendChangedHex(any(Coords.class), anyInt());
        assertTrue(changed.getValue().contains(GROUND_ZERO), "the crater at Ground Zero is in the update");
        assertTrue(changed.getValue().contains(new Coords(GROUND_ZERO.getX(), GROUND_ZERO.getY() - 34)),
              "a hex the blast turned rough is in the update");
    }
}
