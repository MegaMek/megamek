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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Vector;

import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.client.ui.panels.phaseDisplay.commands.MoveCommand;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.moves.MobileStructureAirMovement;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.Weather;
import megamek.common.rolls.Roll;
import megamek.common.units.*;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MobileStructureAirMovementTest {
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);
    private static final Coords ORIGIN = new Coords(8, 8);

    private static class TestManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private TWGameManager manager() {
        var manager = spy(new TestManager());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        manager.getGame().setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        manager.getGame().addPlayer(0, new Player(0, "Test"));
        manager.getGame().setPhase(GamePhase.MOVEMENT);
        return manager;
    }

    private MobileStructure air(TWGameManager manager, Coords position, int elevation) {
        var unit = new MobileStructure(BuildingType.HARDENED, IBuilding.HANGAR);
        unit.setChassis("Air mobile");
        unit.setModel("Test");
        unit.configureConstruction(BuildingType.HARDENED, IBuilding.HANGAR, 2, 75, 0,
              List.of(CubeCoords.ZERO, EAST));
        unit.setMovementMode(EntityMovementMode.VTOL);
        unit.setOwner(manager.getGame().getPlayer(0));
        unit.setId(1);
        unit.setDeployed(true);
        unit.getCrew().setPiloting(0, 0);
        manager.getGame().addEntity(unit);
        unit.setPosition(position);
        unit.setElevation(elevation);
        unit.updateBuildingEntityHexes(0, manager);
        return unit;
    }

    private void move(TWGameManager manager, MobileStructure unit, MoveStepType type) {
        var path = new MovePath(manager.getGame(), unit).addStep(type);
        assertTrue(path.isMoveLegal(), type + " must be playable through the ordinary path compiler");
        new MobileStructureMovementHandler(manager).process(unit, path);
    }

    @Test void quarterMpTakeoffCompletesOnlyAfterEightTurnsAndGroundedUnitsCannotTaxi() {
        var manager = manager();
        var unit = air(manager, ORIGIN, 0);
        unit.setMaximumMP(.25);
        assertFalse(unit.canGoUp(unit.getElevation(), unit.getPosition(), unit.getBoardId()));
        assertFalse(new MovePath(manager.getGame(), unit).addStep(MoveStepType.FORWARDS).isMoveLegal());
        for (int round = 1; round <= 8; round++) {
            unit.newRound(round);
            move(manager, unit, MoveStepType.VERTICAL_TAKE_OFF);
            assertEquals(1, unit.mpUsed);
            assertEquals(round == 8 ? 1 : 0, unit.getElevation());
            assertTrue(unit.isDone());
            if (round < 8) {
                assertEquals(round, unit.getMovementProgress().quarters());
            }
        }
        assertNull(unit.getMovementProgress());
        assertTrue(unit.isAirborneVTOLorWIGE());
    }

    @Test void altitudeControlsIgnoreOwnPublishedHexesAndKeepOneClearLevelForLanding() {
        var manager = manager();
        var unit = air(manager, ORIGIN, 3);
        assertTrue(unit.canGoDown());
        move(manager, unit, MoveStepType.DOWN);
        assertEquals(2, unit.getElevation());
        unit.newRound(2);
        move(manager, unit, MoveStepType.DOWN);
        assertEquals(1, unit.getElevation());
        assertFalse(unit.canGoDown());
        assertTrue(MobileStructureAirMovement.canAttempt(unit, MoveStepType.VERTICAL_LAND));
        unit.newRound(3);
        move(manager, unit, MoveStepType.UP);
        assertEquals(2, unit.getElevation());
    }

    @Test void horizontalMovementMaintainsAbsoluteHeightOverDifferentGroundLevels() {
        var manager = manager();
        var unit = air(manager, ORIGIN, 5);
        Coords destination = ORIGIN.translated(0);
        manager.getGame().getBoard().getHex(destination).setLevel(2);
        var path = new MovePath(manager.getGame(), unit).addStep(MoveStepType.FORWARDS);
        assertEquals(3, path.getFinalElevation());
        assertTrue(path.isMoveLegal());
        new MobileStructureMovementHandler(manager).process(unit, path);
        assertEquals(destination, unit.getPosition());
        for (Coords coords : unit.getCoordsList()) {
            assertEquals(5, manager.getGame().getBoard().getHex(coords).getLevel() + unit.getBaseElevation(coords));
        }
    }

    @Test void landingSupportsAFlatUndersideAtHighestGroundAndDamagesOnlyContactedTerrain() {
        var manager = manager();
        var unit = air(manager, ORIGIN, 3);
        Coords high = unit.relativeToBoard(EAST);
        manager.getGame().getBoard().getHex(high).setLevel(2);
        assertTrue(MobileStructureAirMovement.canAttempt(unit, MoveStepType.VERTICAL_LAND));
        move(manager, unit, MoveStepType.VERTICAL_LAND);
        assertEquals(2, unit.getElevation());
        assertEquals(0, unit.getBaseElevation(high));
        assertFalse(unit.isAirborneVTOLorWIGE());
        assertFalse(unit.canGoUp(unit.getElevation(), unit.getPosition(), unit.getBoardId()));
        assertFalse(unit.isAirLandingGearDamaged(), "New rough created during landing must not damage the gear");
        assertTrue(manager.getGame().getBoard().getHex(high).containsTerrain(Terrains.ROUGH));
        assertFalse(manager.getGame().getBoard().getHex(ORIGIN).containsTerrain(Terrains.ROUGH));
        unit.newRound(2);
        unit.setMaximumMP(2);
        move(manager, unit, MoveStepType.VERTICAL_TAKE_OFF);
        assertEquals(3, unit.getElevation());
        assertTrue(unit.isAirborneVTOLorWIGE());
    }

    @Test void partialOffMapOriginStillAllowsLiftoffButAnEntirelyOffMapTemplateDoesNot() {
        var manager = manager();
        var unit = air(manager, new Coords(-1, 5), 0);
        unit.setMaximumMP(2);
        assertTrue(MobileStructureAirMovement.canAttempt(unit, MoveStepType.VERTICAL_TAKE_OFF));
        move(manager, unit, MoveStepType.VERTICAL_TAKE_OFF);
        assertEquals(1, unit.getElevation());
        unit.setPosition(new Coords(-4, 5));
        unit.setElevation(0);
        assertFalse(MobileStructureAirMovement.canAttempt(unit, MoveStepType.VERTICAL_TAKE_OFF));
        assertFalse(new MovePath(manager.getGame(), unit).addStep(MoveStepType.VERTICAL_TAKE_OFF).isMoveLegal());
    }

    @ParameterizedTest
    @CsvSource({ "1,0", "1,1", "2,2", "3,6" })
    void airProhibitedTerrainPreventsLanding(int kind, int level) {
        var manager = manager();
        var unit = air(manager, ORIGIN, 1);
        int terrain = kind == 1 ? Terrains.WATER : kind == 2 ? Terrains.ROUGH : Terrains.RUBBLE;
        manager.getGame().getBoard().getHex(ORIGIN).addTerrain(new Terrain(terrain, level));
        assertFalse(MobileStructureAirMovement.canAttempt(unit, MoveStepType.VERTICAL_LAND));
        assertFalse(new MovePath(manager.getGame(), unit).addStep(MoveStepType.VERTICAL_LAND).isMoveLegal());
    }

    @Test void controlRollUsesDowngradedWeatherFullLightingAndHighestHalvedTerrainModifier() {
        var manager = manager();
        var unit = air(manager, ORIGIN, 1);
        unit.getCrew().setPiloting(4, 0);
        var conditions = manager.getGame().getPlanetaryConditions();
        conditions.setWeather(Weather.DOWNPOUR);
        conditions.setLight(Light.PITCH_BLACK);
        manager.getGame().getBoard().getHex(ORIGIN).addTerrain(new Terrain(Terrains.WOODS, 2));
        manager.getGame().getBoard().getHex(unit.relativeToBoard(EAST)).addTerrain(new Terrain(Terrains.ROUGH, 1));
        assertEquals(9, MobileStructureAirMovement.controlRoll(unit, MoveStepType.VERTICAL_LAND).getValue());
    }

    @Test void failedLandingAppliesOnlyTotalMarginDamageToActualAftBottomHexes() {
        var manager = manager();
        var unit = air(manager, ORIGIN, 1);
        unit.getCrew().setPiloting(4, 0); // Clear vertical landing is TN 5, so a roll of 2 fails by 3.
        Roll dice = mock(Roll.class);
        when(dice.getIntValue()).thenReturn(2);
        try (var compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(dice);
            move(manager, unit, MoveStepType.VERTICAL_LAND);
        }
        assertEquals(75, unit.getCurrentCF(ORIGIN));
        assertEquals(45, unit.getCurrentCF(unit.relativeToBoard(EAST)));
        assertEquals(0, unit.getElevation());
    }

    @ParameterizedTest
    @CsvSource({ "3,2,1,0", "4,2,1,0", "5,2,0,20", "6,2,0,20", "7,2,0,50", "8,2,0,100", "5,8,1,0" })
    void failedTakeoffUsesMarginTableAndSecondUnmodifiedRoll(int piloting, int second, int finalElevation, int damage) {
        var manager = manager();
        var unit = air(manager, ORIGIN, 0);
        unit.setMaximumMP(2);
        unit.getCrew().setPiloting(piloting, 0);
        var total = new java.util.concurrent.atomic.AtomicInteger();
        doAnswer(call -> { total.addAndGet(call.getArgument(1)); return new Vector<>(); }).when(manager)
              .damageBuilding(eq(unit), anyInt(), anyString(), any(Coords.class), eq(0), isNull(), eq(false));
        Roll first = mock(Roll.class), next = mock(Roll.class);
        when(first.getIntValue()).thenReturn(2);
        when(next.getIntValue()).thenReturn(second);
        try (var compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.rollD6(2)).thenReturn(first, next);
            move(manager, unit, MoveStepType.VERTICAL_TAKE_OFF);
        }
        assertEquals(damage, total.get());
        assertEquals(finalElevation, unit.getElevation());
        assertTrue(unit.isAirLandingGearDamaged());
    }

    @Test void hoveringUsesQuarterCommitmentsAndDestroyedFlyingCarriersRemainPhysicallyAirborne() {
        var manager = manager();
        var unit = air(manager, ORIGIN, 3);
        unit.setMaximumMP(.25);
        for (int round = 1; round <= 4; round++) {
            unit.newRound(round);
            new MobileStructureMovementHandler(manager).process(unit, new MovePath(manager.getGame(), unit));
            assertEquals(1, unit.mpUsed);
            assertEquals(ORIGIN, unit.getPosition());
            if (round < 4) {
                assertEquals(round, unit.getMovementProgress().quarters());
            }
        }
        assertNull(unit.getMovementProgress());
        unit.setDestroyed(true);
        assertTrue(unit.isAirborneVTOLorWIGE());
    }

    @Test void mobileCommandSetExposesVerticalFlightControlsWithoutAeroVelocityControls() {
        var commands = List.of(MoveCommand.values(MovementDisplay.CMD_TANK | MovementDisplay.CMD_MOBILE,
              new megamek.common.options.GameOptions(), false));
        assertTrue(commands.contains(MoveCommand.MOVE_VERT_TAKE_OFF));
        assertTrue(commands.contains(MoveCommand.MOVE_VERT_LAND));
        assertTrue(commands.contains(MoveCommand.MOVE_HOVER));
        assertFalse(commands.contains(MoveCommand.MOVE_ACC));
    }
}
