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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.MekFileParser;
import megamek.common.net.packets.Packet;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.Entity;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * The flash fires of a Davy Crockett (IO:AE pp. 165-166): blast radius 20, secondary radius 40, 100 damage at Ground
 * Zero less 5 a hex.
 */
class NuclearFiresTest {
    private static final String UNIT_FILES = "testresources/megamek/common/units/";
    private static final int BOARD_SIZE = 81;
    private static final Coords GROUND_ZERO = new Coords(40, 40);
    /** About 60 hexes from Ground Zero, well past the secondary radius of 40. */
    private static final Coords BOARD_CORNER = new Coords(0, 0);

    private TWGameManager gameManager;
    private Game game;
    private Board board;
    private Player player;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        game = gameManager.getGame();
        game.setOptions(new GameOptions());
        setFiresAllowed(true);
        player = new Player(0, "Test");
        game.addPlayer(0, player);

        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        board = new Board();
        board.newData(BOARD_SIZE, BOARD_SIZE, hexes, null);
        game.setBoard(board);
    }

    private void setFiresAllowed(boolean allowed) {
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_START_FIRE).setValue(allowed);
    }

    /** A hex straight north of Ground Zero, so its distance is exactly {@code distance}. */
    private static Coords hexNorth(int distance) {
        return new Coords(GROUND_ZERO.getX(), GROUND_ZERO.getY() - distance);
    }

    private void addWoods(int distance) {
        board.getHex(hexNorth(distance)).addTerrain(new Terrain(Terrains.WOODS, 1));
    }

    private boolean isBurning(int distance) {
        return board.getHex(hexNorth(distance)).containsTerrain(Terrains.FIRE);
    }

    private Entity placeUnit(String fileName, Coords position) throws Exception {
        Entity entity = new MekFileParser(new File(UNIT_FILES + fileName)).getEntity();
        entity.setId(game.getNextEntityId());
        game.addEntity(entity);
        entity.setOwner(player);
        entity.setPosition(position);
        entity.setDeployed(true);
        return entity;
    }

    /** Starts the fires of a Davy Crockett with every 2d6 roll forced to {@code value}. */
    private Set<Coords> startFiresWithRoll(int value) {
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(value);
        when(roll.getReport()).thenReturn(String.valueOf(value));
        Set<Coords> changedHexes = new LinkedHashSet<>();
        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class, Mockito.CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(roll);
            new NuclearFires(gameManager).startFires(GROUND_ZERO,
                  NuclearBlastProfile.groundBurst(NuclearWeaponType.DAVY_CROCKETT_M), changedHexes,
                  new Vector<Report>());
        }
        return changedHexes;
    }

    @Test
    void woodsAtTheEdgeOfTheBlastNeedFive() {
        // 19 hexes out the blast deals 5, which adds 0 to the roll
        addWoods(19);

        startFiresWithRoll(4);
        assertFalse(isBurning(19), "4 is not enough");

        Set<Coords> changedHexes = startFiresWithRoll(5);
        assertTrue(isBurning(19), "5 sets it alight");
        assertTrue(changedHexes.contains(hexNorth(19)), "the fire goes to the clients with the other changes");
    }

    @Test
    void blastDamageAddsToTheRoll() {
        // 10 hexes out the blast deals 50: 2 + 5 = 7, enough
        addWoods(10);

        startFiresWithRoll(2);

        assertTrue(isBurning(10));
    }

    @Test
    void woodsBeyondTheBlastNeedNine() {
        // 30 hexes out is past the blast radius of 20 but inside the secondary radius of 40
        addWoods(30);

        startFiresWithRoll(8);
        assertFalse(isBurning(30), "8 is not enough");

        startFiresWithRoll(9);
        assertTrue(isBurning(30), "9 sets it alight");
    }

    @Test
    void nothingBurnsPastTheSecondaryRadius() {
        addWoods(40);
        board.getHex(BOARD_CORNER).addTerrain(new Terrain(Terrains.WOODS, 1));

        startFiresWithRoll(12);

        assertTrue(isBurning(40), "40 hexes is the edge of the secondary radius");
        assertFalse(board.getHex(BOARD_CORNER).containsTerrain(Terrains.FIRE),
              "the board corner is far outside it");
    }

    @Test
    void noFiresWhenTheOptionIsOff() {
        setFiresAllowed(false);
        addWoods(10);

        startFiresWithRoll(12);

        assertFalse(isBurning(10));
    }

    @Test
    void hardenedBuildingsDoNotBurn() {
        Hex hardened = new Hex(0);
        hardened.addTerrain(new Terrain(Terrains.BUILDING, 4));
        Hex medium = new Hex(0);
        medium.addTerrain(new Terrain(Terrains.BUILDING, 2));

        assertFalse(NuclearFires.hasFlammableTerrain(hardened), "hardened (IO:AE p.165)");
        assertTrue(NuclearFires.hasFlammableTerrain(medium), "medium");
        assertFalse(NuclearFires.hasFlammableTerrain(new Hex(0)), "clear ground has nothing to burn");
    }

    @Test
    void buildingDestroyedByTheBlastDoesNotBurn() {
        // A building at CF 0 waits on the map until the detonation resolves; it no longer burns (HammerGS, 2026-10-29)
        Hex[] hexes = new Hex[BOARD_SIZE * BOARD_SIZE];
        for (int index = 0; index < hexes.length; index++) {
            hexes[index] = new Hex(0);
        }
        Coords standing = hexNorth(10);
        Coords destroyed = hexNorth(12);
        for (Coords coords : new Coords[] { standing, destroyed }) {
            Hex buildingHex = hexes[(coords.getY() * BOARD_SIZE) + coords.getX()];
            buildingHex.addTerrain(new Terrain(Terrains.BUILDING, 2));
            buildingHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 2));
            buildingHex.addTerrain(new Terrain(Terrains.BLDG_CF, 40));
        }
        board = new Board();
        board.newData(BOARD_SIZE, BOARD_SIZE, hexes, null);
        game.setBoard(board);
        board.getBuildingAt(destroyed).setCurrentCF(0, destroyed);

        startFiresWithRoll(12);

        assertTrue(isBurning(10), "the standing building burns");
        assertFalse(isBurning(12), "the destroyed one does not");
    }

    @Test
    void unitInTheBlastAreaIsSetAflame() throws Exception {
        // A clear hex with a unit in it rolls too (modifier 0); the unit burns, the clear hex does not
        Entity atlas = placeUnit("Atlas AS7-D.mtf", hexNorth(15));

        startFiresWithRoll(12);

        assertTrue(atlas.infernos.isStillBurning(), "set aflame as if hit by Inferno");
        assertFalse(isBurning(15), "clear ground has nothing to burn");
    }

    @Test
    void unitBeyondTheBlastIsNotSetAflame() throws Exception {
        Entity atlas = placeUnit("Atlas AS7-D.mtf", hexNorth(30));

        startFiresWithRoll(12);

        assertFalse(atlas.infernos.isStillBurning(), "only the blast area sets units aflame");
    }
}
