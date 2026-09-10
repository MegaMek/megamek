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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import megamek.common.board.AllowedDeploymentHelper;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.board.FacingOption;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.game.Game;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKStructureFile;
import megamek.common.units.BipedMek;
import megamek.common.units.BuildingEntity;
import megamek.common.units.BuildingDesign;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BuildingRotationTest {
    private static final CubeCoords NORTH = new CubeCoords(0, -1, 1);
    private static final List<CubeCoords> LAYOUT = List.of(CubeCoords.ZERO, NORTH,
          new CubeCoords(0, -2, 2), new CubeCoords(1, -1, 0));
    private Board board;
    private Game game;
    private BuildingEntity building;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        board = emptyBoard();
        game = new Game(board);
        Player player = new Player(0, "Builder");
        game.addPlayer(0, player);
        building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        building.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 2, 50, 10, LAYOUT);
        building.setId(1);
        building.setOwner(player);
        building.setStartingPos(Board.START_ANY);
        game.addEntity(building);
    }

    private Board emptyBoard() {
        return new Board(9, 9, Stream.generate(Hex::new).limit(81).toArray(Hex[]::new));
    }

    private Set<Coords> footprint(Coords origin, int facing) {
        return Set.of(origin, origin.translated(facing), origin.translated(facing, 2),
              origin.translated((facing + 1) % 6));
    }

    @ParameterizedTest
    @CsvSource({ "0, 4", "1, 4", "2, 4", "3, 4", "4, 4", "5, 4",
                 "0, 3", "1, 3", "2, 3", "3, 3", "4, 3", "5, 3" })
    void rotatesAsymmetricFootprintAndWeaponPositionsOnBothColumnParities(int facing, int x) throws Exception {
        Coords origin = new Coords(x, 4);
        WeaponMounted weapon = (WeaponMounted) building.addEquipment(new ISLaserMedium(), 3);
        building.setPosition(origin);
        building.setFacing(facing);

        assertEquals(footprint(origin, facing), building.getOccupiedCoords());
        assertEquals(footprint(origin, facing), game.getEntityPositions(building));
        assertEquals(origin, building.getSecondaryPositions().get(0));
        assertEquals(origin.translated(facing), building.getWeaponFiringPosition(weapon));
        assertEquals(origin.translated(facing), building.getLocationCoords(3));
        assertEquals("Level 1 " + origin.translated(facing).getBoardNum(), building.getLocationName(3));
        assertEquals(1, building.getWeaponFiringHeight(weapon));
        assertEquals(3, weapon.getLocation());
        assertEquals(LAYOUT, building.getInternalBuilding().getCoordsList());
        assertEquals(NORTH, building.boardToRelative(origin.translated(facing)));
        assertEquals(50, building.getCurrentCF(origin.translated(facing)));
        assertEquals(10, building.getArmor(origin.translated(facing)));
    }

    @Test
    void fiveCopiesKeepIndependentFacingsAndDoNotChangeTheSavedDesign() throws Exception {
        building.setChassis("Rotation template");
        building.setModel("");
        building.setYear(3145);
        building.setTechLevel(TechConstants.T_IS_ADVANCED);
        building.setEngine(new Engine(0, Engine.NONE, 0));
        WeaponMounted sourceWeapon = (WeaponMounted) building.addEquipment(new ISLaserMedium(), 3);
        sourceWeapon.setFacing(2);
        building.getDesign().getDoors().add(new BuildingDesign.Door(new BuildingDesign.Position(NORTH, 1), 5, 2));
        var savedDesign = BLKFile.getBlock(building);
        game.setBoard(0, new Board(40, 9, Stream.generate(Hex::new).limit(360).toArray(Hex[]::new)));
        List<BuildingEntity> copies = new ArrayList<>();

        for (int facing = 0; facing < 5; facing++) {
            BuildingEntity copy = (BuildingEntity) new BLKStructureFile(savedDesign).getEntity();
            copy.setOwner(building.getOwner());
            copy.setId(10 + facing);
            game.addEntity(copy);
            String[] designBeforePlacement = BLKFile.getBlock(copy).getAllDataAsString();
            Coords origin = new Coords(4 + 7 * facing, 4);
            copy.setPosition(origin);
            copy.setFacing(facing);
            copies.add(copy);

            assertEquals(footprint(origin, facing), copy.getOccupiedCoords());
            assertEquals(LAYOUT, copy.getInternalBuilding().getCoordsList());
            assertEquals(3, copy.getWeaponList().getFirst().getLocation());
            assertEquals(2, copy.getWeaponList().getFirst().getFacing());
            assertEquals(building.getDesign().getDoors(), copy.getDesign().getDoors());
            assertArrayEquals(designBeforePlacement, BLKFile.getBlock(copy).getAllDataAsString());
            BuildingEntity reloaded = (BuildingEntity) new BLKStructureFile(BLKFile.getBlock(copy)).getEntity();
            assertEquals(LAYOUT, reloaded.getInternalBuilding().getCoordsList());
            assertEquals(1, reloaded.getWeaponList().size());
            assertEquals(3, reloaded.getWeaponList().getFirst().getLocation());
            assertEquals(2, reloaded.getWeaponList().getFirst().getFacing());
        }

        copies.getFirst().setFacing(5);
        for (int index = 1; index < copies.size(); index++) {
            BuildingEntity copy = copies.get(index);
            assertEquals(index, copy.getFacing());
            assertEquals(footprint(copy.getPosition(), index), copy.getOccupiedCoords());
        }
        assertNull(building.getPosition());
        assertEquals(0, building.getFacing());
        assertEquals(LAYOUT, building.getInternalBuilding().getCoordsList());
        assertArrayEquals(savedDesign.getAllDataAsString(), BLKFile.getBlock(building).getAllDataAsString());
    }

    @Test
    void facingChangePublishesUpdatedFootprintAndLookup() {
        Coords origin = new Coords(4, 4);
        building.setPosition(origin);
        AtomicInteger events = new AtomicInteger();
        game.addGameListener(new GameListenerAdapter() {
            @Override
            public void gameEntityChange(GameEntityChangeEvent event) {
                assertEquals(footprint(origin, 2), building.getOccupiedCoords());
                assertEquals(footprint(origin, 2), game.getEntityPositions(building));
                assertFalse(game.getEntitiesVector(origin.translated(0), 0, true).contains(building));
                assertTrue(game.getEntitiesVector(origin.translated(2, 2), 0, true).contains(building));
                events.incrementAndGet();
            }
        });

        building.setFacing(2);
        assertEquals(1, events.get());
    }

    @Test
    void movingAndUnplacingClearOldOccupiedHexes() {
        building.setFacing(3);
        building.setPosition(new Coords(3, 3));
        Coords destination = new Coords(6, 6);
        building.setPosition(destination);
        assertEquals(footprint(destination, 3), game.getEntityPositions(building));

        building.setPosition(null);
        assertTrue(building.getSecondaryPositions().isEmpty());
        assertTrue(building.getOccupiedCoords().isEmpty());
        assertTrue(game.getEntityPositions(building).isEmpty());
        assertNull(building.relativeToBoard(NORTH));
    }

    @Test
    void positionWithoutGameUpdateChangesLayoutButLeavesLookupAlone() {
        Coords origin = new Coords(3, 3);
        building.setPosition(origin);
        Coords destination = new Coords(6, 6);
        building.setPosition(destination, false);
        assertEquals(footprint(destination, 0), building.getOccupiedCoords());
        assertEquals(footprint(origin, 0), game.getEntityPositions(building));
    }

    @Test
    void allowedFacingsAtBoardEdgeDoNotMutatePreview() {
        building.setPosition(new Coords(4, 4));
        Set<Coords> previous = new HashSet<>(building.getOccupiedCoords());
        Coords edge = new Coords(4, 0);
        FacingOption options = new AllowedDeploymentHelper(building, edge, board, board.getHex(edge), game)
              .findAllowedFacings(0);

        assertNotNull(options);
        assertFalse(options.getValidFacings().contains(0));
        assertTrue(options.getValidFacings().contains(3));
        assertEquals(0, building.getFacing());
        assertEquals(previous, building.getOccupiedCoords());
        assertEquals(previous, game.getEntityPositions(building));
    }

    @Test
    void everyRotatedHexMustFitInDeploymentZone() {
        Coords origin = new Coords(4, 4);
        building.setStartingAnyNWy(4);
        assertTrue(board.isLegalDeployment(origin, building));
        assertFalse(building.isDeploymentPositionAndFacingValid(origin, 0, 0, 0));
        assertTrue(building.isDeploymentPositionAndFacingValid(origin, 3, 0, 0));
        FacingOption options = new AllowedDeploymentHelper(building, origin, board, board.getHex(origin), game)
              .findAllowedFacings(0);
        assertNotNull(options);
        assertFalse(options.getValidFacings().contains(0));
        assertTrue(options.getValidFacings().contains(3));
    }

    @Test
    void rotationChecksTerrainBeyondTheOriginAndCannotBypassItWithElevation() {
        Coords origin = new Coords(4, 4);
        Hex wingHex = board.getHex(origin.translated(1, 2));
        wingHex.setLevel(1);
        assertFalse(building.isDeploymentPositionAndFacingValid(origin, 1, 0, 0));
        assertTrue(building.isDeploymentPositionAndFacingValid(origin, 0, 0, 0));
        wingHex.setLevel(0);
        wingHex.addTerrain(new Terrain(Terrains.IMPASSABLE, 1));
        assertFalse(building.isDeploymentPositionAndFacingValid(origin, 1, 0, 0));
        assertFalse(building.isPositionAndFacingValid(origin, 1, 1, 0));
        assertFalse(building.isPositionAndFacingValid(new Coords(4, 0), 0, 1, 0));
        assertTrue(building.isDeploymentPositionAndFacingValid(origin, 0, 1, 0));
    }

    @Test
    void rotationChecksOtherUnitsOnTheDestinationBoard() {
        Board destination = emptyBoard();
        destination.setBoardId(1);
        game.setBoard(1, destination);
        Coords origin = new Coords(4, 4);
        BipedMek blocker = new BipedMek();
        blocker.setId(2);
        blocker.setOwner(building.getOwner());
        blocker.setDeployed(true);
        blocker.setPosition(origin.translated(1, 2));
        game.addEntity(blocker);

        assertFalse(building.isDeploymentPositionAndFacingValid(origin, 1, 0, 0));
        assertTrue(building.isDeploymentPositionAndFacingValid(origin, 1, 0, 1));
        blocker.setBoardId(1);
        assertFalse(building.isDeploymentPositionAndFacingValid(origin, 1, 0, 1));
        assertTrue(building.isDeploymentPositionAndFacingValid(origin, 0, 0, 1));
    }
}
