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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.BuildingEditSpec;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.FuelTank;
import megamek.common.units.IBuilding;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests the gamemaster's edits to a building: that the construction factor is written to the building, that a hex
 * without a building is refused, and that setting the factor to zero brings the building down rather than leaving it
 * standing on nothing.
 */
class BuildingEditHandlerTest {

    /**
     * Rebuilt for every test, because each test changes the building on it and a shared board would carry one test's
     * change into the next.
     */
    private static final String BOARD_DATA = """
          size 3 3
          hex 0101 0 "bldg_elev:2;building:2;bldg_class:1;bldg_cf:40" ""
          hex 0102 0 "" ""
          hex 0103 0 "" ""
          end""";

    private static final String GAMEMASTER = "Referee";
    private static final Coords BUILDING_HEX = new Coords(0, 0);
    private static final Coords EMPTY_HEX = new Coords(1, 0);

    private TWGameManager gameManager;
    private Game game;
    private Board board;


    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendChangedHex(any(Coords.class), anyInt());
        Mockito.doNothing().when(gameManager).sendChangedBuildings(any());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);


    }


    /** @return the handler under test, reached the way the server reaches it */
    private BuildingEditHandler buildingEditHandler() {
        return gameManager.buildingEditHandler();
    }

    /** @return an edit that sets only the construction factor and leaves everything else alone */
    private static BuildingEditHandler.BuildingEdit cfOnly(int constructionFactor) {
        return new BuildingEditHandler.BuildingEdit(constructionFactor, null, null, null);
    }

    @Test
    void theBoardStartsWithABuildingToEdit() {
        IBuilding building = board.getBuildingAt(BUILDING_HEX);

        assertNotNull(building, "the test board should hold a building to edit");
        assertEquals(40, building.getCurrentCF(BUILDING_HEX), "the building should start at its board factor");
    }

    @Test
    void weakeningABuildingSetsItsConstructionFactor() {
        String refusal = buildingEditHandler().applyEdit(BUILDING_HEX, cfOnly(15), GAMEMASTER);

        assertNull(refusal, "weakening a building should be allowed");
        assertEquals(15, board.getBuildingAt(BUILDING_HEX).getCurrentCF(BUILDING_HEX),
              "the building should be left at the factor it was set to");
    }

    @Test
    void thePhaseFactorMovesWithIt() {
        buildingEditHandler().applyEdit(BUILDING_HEX, cfOnly(15), GAMEMASTER);

        assertEquals(15, board.getBuildingAt(BUILDING_HEX).getPhaseCF(BUILDING_HEX),
              "leaving the phase factor behind would measure this phase's damage against the old value");
    }

    @Test
    void aHexWithNoBuildingIsRefused() {
        String refusal = buildingEditHandler().applyEdit(EMPTY_HEX, cfOnly(20), GAMEMASTER);

        assertNotNull(refusal, "there is nothing to change in a hex with no building");
    }

    @Test
    void aFactorOfZeroBringsTheBuildingDown() {
        String refusal = buildingEditHandler().applyEdit(BUILDING_HEX,
              cfOnly(BuildingEditHandler.COLLAPSING_CONSTRUCTION_FACTOR),
              GAMEMASTER);

        assertNull(refusal, "collapsing a building should be allowed");
        assertNull(board.getBuildingAt(BUILDING_HEX),
              "a collapsed building is gone from the hex, not merely left standing at zero");
    }

    @Test
    void theHeightIsWrittenToTheBuildingAndTheHex() {
        String refusal = buildingEditHandler().applyEdit(BUILDING_HEX,
              new BuildingEditHandler.BuildingEdit(null, null, 5, null), GAMEMASTER);

        assertNull(refusal, "raising a building should be allowed");
        assertEquals(5, board.getBuildingAt(BUILDING_HEX).getHeight(BUILDING_HEX),
              "the building should stand at its new height");
        assertEquals(5, board.getHex(BUILDING_HEX).terrainLevel(Terrains.BLDG_ELEV),
              "the hex is what the board is drawn from, so it has to agree with the building");
    }

    @Test
    void theArmorIsWrittenToTheBuilding() {
        String refusal = buildingEditHandler().applyEdit(BUILDING_HEX,
              new BuildingEditHandler.BuildingEdit(null, 12, null, null), GAMEMASTER);

        assertNull(refusal, "armouring a building should be allowed");
        assertEquals(12, board.getBuildingAt(BUILDING_HEX).getArmor(BUILDING_HEX),
              "the building should carry the armor it was given");
    }

    @Test
    void valuesLeftAloneAreNotTouched() {
        buildingEditHandler().applyEdit(BUILDING_HEX,
              new BuildingEditHandler.BuildingEdit(null, 12, null, null), GAMEMASTER);

        assertEquals(40, board.getBuildingAt(BUILDING_HEX).getCurrentCF(BUILDING_HEX),
              "only the armor was set, so the construction factor should be untouched");
    }

    /** @return a spec describing a building of the given type in the given hex */
    private BuildingEditSpec specFor(Coords coords, BuildingType type) {
        BuildingEditSpec spec = new BuildingEditSpec(coords, board.getBoardId());
        spec.setBuildingType(type);
        spec.setConstructionFactor(type.getDefaultCF());
        spec.setHeight(2);
        return spec;
    }

    @Test
    void aBuildingCanBeRaisedInAnEmptyHex() {
        String refusal = buildingEditHandler().applyBuildingSpec(specFor(EMPTY_HEX, BuildingType.HEAVY), GAMEMASTER);

        assertNull(refusal, "putting a building in an empty hex should be allowed");
        IBuilding raised = board.getBuildingAt(EMPTY_HEX);
        assertNotNull(raised, "the board should now hold a building there");
        assertEquals(BuildingType.HEAVY, raised.getBuildingType(), "and it should be the type that was asked for");
    }

    @Test
    void aRaisedBuildingTakesTheConstructionFactorItWasGiven() {
        buildingEditHandler().applyBuildingSpec(specFor(EMPTY_HEX, BuildingType.LIGHT), GAMEMASTER);

        assertEquals(BuildingType.LIGHT.getDefaultCF(), board.getBuildingAt(EMPTY_HEX).getCurrentCF(EMPTY_HEX),
              "a new building should stand at the factor it was built with");
    }

    @Test
    void changingTheTypeRebuildsTheBuilding() {
        BuildingEditSpec spec = specFor(BUILDING_HEX, BuildingType.HARDENED);

        String refusal = buildingEditHandler().applyBuildingSpec(spec, GAMEMASTER);

        assertNull(refusal, "changing what a building is made of should be allowed");
        assertEquals(BuildingType.HARDENED, board.getBuildingAt(BUILDING_HEX).getBuildingType(),
              "what a building is made of is fixed when the board makes it, so it has to be rebuilt to change");
    }

    @Test
    void aBuildingCanBeRemoved() {
        BuildingEditSpec spec = new BuildingEditSpec(BUILDING_HEX, board.getBoardId());
        spec.setRemovingBuilding(true);

        String refusal = buildingEditHandler().applyBuildingSpec(spec, GAMEMASTER);

        assertNull(refusal, "removing a building should be allowed");
        assertNull(board.getBuildingAt(BUILDING_HEX), "and the hex should hold no building afterwards");
    }

    @Test
    void removingABuildingThatIsNotThereIsRefused() {
        BuildingEditSpec spec = new BuildingEditSpec(EMPTY_HEX, board.getBoardId());
        spec.setRemovingBuilding(true);

        assertNotNull(buildingEditHandler().applyBuildingSpec(spec, GAMEMASTER),
              "there is nothing to remove from an empty hex");
    }

    @Test
    void aBuildingCannotBeRaisedInWater() {
        board.getHex(EMPTY_HEX).addTerrain(new Terrain(Terrains.WATER, 2));

        assertNotNull(buildingEditHandler().applyBuildingSpec(specFor(EMPTY_HEX, BuildingType.MEDIUM), GAMEMASTER),
              "a building cannot stand in water, so raising one there should be refused");
    }

    /** Sends a building edit the way a client does, so the packet handler and its Game Master guard are exercised. */
    private void sendAsPacket(BuildingEditSpec spec, boolean senderIsGameMaster) {
        Player sender = game.getPlayer(0);
        sender.setGameMaster(senderIsGameMaster);
        gameManager.handlePacket(0, new Packet(PacketCommand.BUILDING_EDIT, spec));
    }

    @Test
    void aGameMasterCanRaiseABuildingThroughThePacket() {
        sendAsPacket(specFor(EMPTY_HEX, BuildingType.HEAVY), true);

        assertNotNull(board.getBuildingAt(EMPTY_HEX),
              "the packet path should reach the handler and put the building up");
    }

    @Test
    void aPlayerWhoIsNotGameMasterCannotChangeTheBoard() {
        sendAsPacket(specFor(EMPTY_HEX, BuildingType.HEAVY), false);

        assertNull(board.getBuildingAt(EMPTY_HEX),
              "only a gamemaster may change the board, whatever a client sends");
    }

    @Test
    void aPlayerWhoIsNotGameMasterCannotRemoveABuilding() {
        BuildingEditSpec spec = new BuildingEditSpec(BUILDING_HEX, board.getBoardId());
        spec.setRemovingBuilding(true);

        sendAsPacket(spec, false);

        assertNotNull(board.getBuildingAt(BUILDING_HEX), "the building should still be standing");
    }

    @Test
    void aRebuiltBuildingKeepsItsSpecialImage() {
        BuildingEditSpec spec = specFor(BUILDING_HEX, BuildingType.HARDENED);
        spec.setFluffImage(100);

        String refusal = buildingEditHandler().applyBuildingSpec(spec, GAMEMASTER);

        assertNull(refusal, "rebuilding with a fluff image should be allowed");
        assertEquals(100, board.getHex(BUILDING_HEX).terrainLevel(Terrains.BLDG_FLUFF),
              "the building should still be drawn with the image it was given");
    }

    @Test
    void aBuildingWithNoFluffImageIsDrawnTheOrdinaryWay() {
        buildingEditHandler().applyBuildingSpec(specFor(EMPTY_HEX, BuildingType.MEDIUM), GAMEMASTER);

        assertFalse(board.getHex(EMPTY_HEX).containsTerrain(Terrains.BLDG_FLUFF),
              "a building raised without a fluff image should carry none");
    }

    /** @return a spec describing a fuel tank of the given explosion magnitude in the given hex */
    private BuildingEditSpec fuelTankFor(Coords coords, int magnitude) {
        BuildingEditSpec spec = new BuildingEditSpec(coords, board.getBoardId());
        spec.setFuelTank(true);
        spec.setMagnitude(magnitude);
        spec.setConstructionFactor(20);
        spec.setHeight(1);
        return spec;
    }

    @Test
    void aFuelTankCanBeRaised() {
        String refusal = buildingEditHandler().applyBuildingSpec(fuelTankFor(EMPTY_HEX, 15), GAMEMASTER);

        assertNull(refusal, "putting a fuel tank in an empty hex should be allowed");
        assertInstanceOf(FuelTank.class, board.getBuildingAt(EMPTY_HEX),
              "a fuel tank is its own kind of structure, not an ordinary building");
        assertEquals(15, ((FuelTank) board.getBuildingAt(EMPTY_HEX)).getMagnitude(),
              "and it should carry the explosion size it was given");
    }

    @Test
    void changingTheMagnitudeRebuildsTheTank() {
        buildingEditHandler().applyBuildingSpec(fuelTankFor(EMPTY_HEX, 15), GAMEMASTER);

        String refusal = buildingEditHandler().applyBuildingSpec(fuelTankFor(EMPTY_HEX, 30), GAMEMASTER);

        assertNull(refusal, "changing how big the explosion is should be allowed");
        assertEquals(30, ((FuelTank) board.getBuildingAt(EMPTY_HEX)).getMagnitude(),
              "a fuel tank holds its magnitude as a final field, so it has to be rebuilt to change it");
    }

    @Test
    void aBuildingCanBeReplacedByAFuelTank() {
        String refusal = buildingEditHandler().applyBuildingSpec(fuelTankFor(BUILDING_HEX, 10), GAMEMASTER);

        assertNull(refusal, "replacing a building with a fuel tank should be allowed");
        assertInstanceOf(FuelTank.class, board.getBuildingAt(BUILDING_HEX),
              "the hex should now hold a fuel tank rather than the building that was there");
    }

    @Test
    void aHexCanBePutBackTheWayItWasBeforeEditing() {
        int originalFactor = board.getBuildingAt(BUILDING_HEX).getCurrentCF(BUILDING_HEX);
        buildingEditHandler().applyBuildingSpec(specFor(BUILDING_HEX, BuildingType.HARDENED), GAMEMASTER);
        assertEquals(BuildingType.HARDENED, board.getBuildingAt(BUILDING_HEX).getBuildingType());

        BuildingEditSpec restore = new BuildingEditSpec(BUILDING_HEX, board.getBoardId());
        restore.setRestoringOriginal(true);
        String refusal = buildingEditHandler().applyBuildingSpec(restore, GAMEMASTER);

        assertNull(refusal, "there was a change to put back");
        assertEquals(BuildingType.MEDIUM, board.getBuildingAt(BUILDING_HEX).getBuildingType(),
              "the building should be the kind it was before it was edited");
        assertEquals(originalFactor, board.getBuildingAt(BUILDING_HEX).getCurrentCF(BUILDING_HEX),
              "and should stand at the factor it had then");
    }

    @Test
    void restoringGoesBackPastSeveralEdits() {
        buildingEditHandler().applyBuildingSpec(specFor(BUILDING_HEX, BuildingType.HEAVY), GAMEMASTER);
        buildingEditHandler().applyBuildingSpec(specFor(BUILDING_HEX, BuildingType.HARDENED), GAMEMASTER);

        BuildingEditSpec restore = new BuildingEditSpec(BUILDING_HEX, board.getBoardId());
        restore.setRestoringOriginal(true);
        buildingEditHandler().applyBuildingSpec(restore, GAMEMASTER);

        assertEquals(BuildingType.MEDIUM, board.getBuildingAt(BUILDING_HEX).getBuildingType(),
              "restoring goes back to before the gamemaster started, not to the previous edit");
    }

    @Test
    void restoringAnUntouchedHexIsRefused() {
        BuildingEditSpec restore = new BuildingEditSpec(EMPTY_HEX, board.getBoardId());
        restore.setRestoringOriginal(true);

        assertNotNull(buildingEditHandler().applyBuildingSpec(restore, GAMEMASTER),
              "a hex no gamemaster has touched has nothing to put back");
    }

    @Test
    void aRaisedBuildingCanBeRestoredAwayAgain() {
        buildingEditHandler().applyBuildingSpec(specFor(EMPTY_HEX, BuildingType.HEAVY), GAMEMASTER);

        BuildingEditSpec restore = new BuildingEditSpec(EMPTY_HEX, board.getBoardId());
        restore.setRestoringOriginal(true);
        buildingEditHandler().applyBuildingSpec(restore, GAMEMASTER);

        assertNull(board.getBuildingAt(EMPTY_HEX),
              "the hex was empty before, so putting it back should leave it empty");
    }

    @Test
    void theImageIsWrittenWhenNothingElseAboutTheBuildingChanges() {
        // the picture a building is drawn with is hex terrain, not building state, so an edit that only changes the
        // picture goes down a path that never touches the building itself - and used to leave the hex alone with it
        BuildingEditSpec spec = specFor(BUILDING_HEX, BuildingType.MEDIUM);
        spec.setBuildingClass(IBuilding.HANGAR);
        spec.setFluffImage(7);

        String refusal = buildingEditHandler().applyBuildingSpec(spec, GAMEMASTER);

        assertNull(refusal, "changing only the picture should be allowed");
        assertEquals(7, board.getHex(BUILDING_HEX).terrainLevel(Terrains.BLDG_FLUFF),
              "the hex should hold the picture that was chosen");
    }

    @Test
    void theImageCanBeTakenBackOffAgain() {
        BuildingEditSpec spec = specFor(BUILDING_HEX, BuildingType.MEDIUM);
        spec.setBuildingClass(IBuilding.HANGAR);
        spec.setFluffImage(7);
        buildingEditHandler().applyBuildingSpec(spec, GAMEMASTER);

        BuildingEditSpec plain = specFor(BUILDING_HEX, BuildingType.MEDIUM);
        plain.setBuildingClass(IBuilding.HANGAR);
        plain.setFluffImage(0);
        buildingEditHandler().applyBuildingSpec(plain, GAMEMASTER);

        assertFalse(board.getHex(BUILDING_HEX).containsTerrain(Terrains.BLDG_FLUFF),
              "choosing the default picture should leave no picture terrain behind");
    }

    @Test
    void aRebuiltBuildingKeepsTheImageItWasGiven() {
        BuildingEditSpec spec = specFor(BUILDING_HEX, BuildingType.HEAVY);
        spec.setFluffImage(3);

        buildingEditHandler().applyBuildingSpec(spec, GAMEMASTER);

        assertEquals(3, board.getHex(BUILDING_HEX).terrainLevel(Terrains.BLDG_FLUFF),
              "a building rebuilt as another type should still be drawn with the picture that was chosen");
    }
}
