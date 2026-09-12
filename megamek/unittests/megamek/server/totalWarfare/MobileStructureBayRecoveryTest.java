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
import java.util.Map;

import megamek.common.Player;
import megamek.common.bays.ASFBay;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.moves.MobileStructureBayRecovery;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.*;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MobileStructureBayRecoveryTest {
    private static final Coords ORIGIN = new Coords(8, 8);
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);
    private static class TestManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }
    private record Fixture(TWGameManager manager, MobileStructure carrier, ASFBay bay, AeroSpaceFighter fighter) { }

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private Fixture fixture() {
        var manager = spy(new TestManager());
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        var game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        game.addPlayer(0, new Player(0, "Carrier"));
        game.setPhase(GamePhase.MOVEMENT);
        game.setRoundCount(1);
        var carrier = new MobileStructure(BuildingType.HARDENED, IBuilding.HANGAR);
        carrier.configureConstruction(BuildingType.HARDENED, IBuilding.HANGAR, 2, 75, 0, List.of(CubeCoords.ZERO, EAST));
        carrier.setChassis("Air carrier");
        carrier.setModel("Recovery");
        carrier.setId(1);
        carrier.setOwner(game.getPlayer(0));
        carrier.setDeployed(true);
        carrier.setMovementMode(EntityMovementMode.VTOL);
        var bay = new ASFBay(5, 2, 7);
        carrier.addTransporter(bay);
        carrier.getDesign().getBayDoors().addAll(List.of(
              new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 0), 3),
              new BuildingDesign.BayDoor(7, new BuildingDesign.Position(EAST, 1), 1)));
        game.addEntity(carrier);
        carrier.setPosition(ORIGIN);
        carrier.setElevation(10);
        carrier.setDone(true);
        carrier.updateBuildingEntityHexes(0, manager);
        var fighter = new AeroSpaceFighter();
        fighter.setChassis("Fighter");
        fighter.setModel("Recovery");
        fighter.setId(2);
        fighter.setOwner(game.getPlayer(0));
        fighter.setDeployed(true);
        fighter.setWeight(50);
        fighter.setOriginalWalkMP(6);
        fighter.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        fighter.getCrew().setPiloting(2, 0);
        fighter.initializeSI(100);
        for (int loc = 0; loc < fighter.locations(); loc++) { fighter.initializeArmor(100, loc); }
        game.addEntity(fighter);
        fighter.setPosition(carrier.relativeToBoard(EAST));
        fighter.liftOff(2);
        fighter.setCurrentVelocity(0);
        fighter.setNextVelocity(0);
        return new Fixture(manager, carrier, bay, fighter);
    }

    private MovePath path(Fixture f) {
        return new MovePath(f.manager().getGame(), f.fighter()).addStep(MoveStepType.RECOVER, f.carrier().getId(), -1);
    }

    private void recover(Fixture f, int result) {
        var path = path(f);
        assertTrue(path.isMoveLegal(), "the ordinary fighter path must expose this recovery action");
        Roll roll = mock(Roll.class);
        when(roll.getIntValue()).thenReturn(result);
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.rollD6(2)).thenReturn(roll);
            new MovePathHandler(f.manager(), f.fighter(), path, Map.of()).processMovement();
        }
    }

    @Test void actualRecoveryUsesTheAuthoredDoorAwayFromOriginAndItsAltitude() {
        var f = fixture();
        assertNotEquals(f.carrier().getPosition(), f.fighter().getPosition());
        assertEquals(List.of(f.carrier()), MobileStructureBayRecovery.candidates(f.fighter(), path(f)));
        recover(f, 12);
        assertEquals(f.carrier().getId(), f.fighter().getTransportId());
        assertNull(f.fighter().getPosition());
        assertEquals(f.bay(), f.carrier().getBay(f.fighter()));
        assertEquals(5, f.fighter().getRecoveryTurn());
        assertEquals(2, f.bay().availableRecoverySlots(0));
        assertEquals(1, f.bay().availableRecoverySlots(1));
    }

    @Test void successfulSnakeEyesRecoveryLoadsBeforeBreakingTheUsedDoor() {
        var f = fixture();
        f.fighter().getCrew().setPiloting(1, 0);
        assertEquals(2, f.fighter().getBasePilotingRoll().getValue(), "atmospheric operations +2, fighter -1");
        recover(f, 2);
        assertEquals(f.carrier().getId(), f.fighter().getTransportId());
        assertEquals(1, f.bay().getCurrentDoors());
        assertEquals(0, f.bay().availableRecoverySlots(1));
        assertEquals(2, f.bay().availableRecoverySlots(0));
        assertEquals(Map.of(f.carrier().getDesign().getBayDoors().get(1), 1),
              f.carrier().getBuildingRuntimeState().getDamagedBayDoors());
    }

    @Test void failedRecoveryInflictsTwiceTheMarginAndDoesNotConsumeARecoverySlot() {
        var f = fixture();
        f.fighter().getCrew().setPiloting(3, 0);
        assertEquals(4, f.fighter().getBasePilotingRoll().getValue());
        recover(f, 3);
        assertEquals(Entity.NONE, f.fighter().getTransportId());
        assertTrue(f.bay().getTroops().isEmpty());
        assertEquals(2, f.bay().availableRecoverySlots(1));
        verify(f.manager()).damageEntity(eq(f.fighter()), any(), eq(2));
        assertTrue(f.manager().getGame().getControlRolls().hasMoreElements(), "a failed attempt can still stall");
    }

    @Test void successfulLowAltitudeRecoveryCannotStallAfterReachingTheBay() {
        var f = fixture();
        f.carrier().setElevation(1);
        f.carrier().updateBuildingEntityHexes(0, f.manager());
        f.fighter().setAltitude(1);
        recover(f, 12);
        assertEquals(f.carrier().getId(), f.fighter().getTransportId());
        assertFalse(f.fighter().isDestroyed() || f.fighter().isDoomed());
        assertFalse(f.manager().getGame().getControlRolls().hasMoreElements());
        verify(f.manager(), never()).processCrash(eq(f.fighter()), anyInt(), any());
    }

    @Test void carrierMustHaveFinishedMovementAndRecoveryMustMatchAltitudeHeadingAndVelocity() {
        var f = fixture();
        f.carrier().setDone(false);
        assertFalse(path(f).isMoveLegal());
        f.carrier().setDone(true);
        f.fighter().setAltitude(1);
        assertFalse(path(f).isMoveLegal());
        f.fighter().setAltitude(2);
        f.fighter().setFacing(1);
        assertFalse(path(f).isMoveLegal());
        f.fighter().setFacing(0);
        f.carrier().delta_distance = 16;
        assertFalse(path(f).isMoveLegal());
        f.fighter().setCurrentVelocity(1);
        assertFalse(MobileStructureBayRecovery.candidates(f.fighter(), path(f)).isEmpty());
    }

    @Test void unfinishedOrEnemyRecoveryCannotBeForgedPastTheServer() {
        var f = fixture();
        var path = path(f);
        assertTrue(path.isMoveLegal());
        var enemy = new Player(3, "Enemy");
        f.manager().getGame().addPlayer(3, enemy);
        f.fighter().setOwner(enemy);
        new MovePathHandler(f.manager(), f.fighter(), path, Map.of()).processMovement();
        assertEquals(Entity.NONE, f.fighter().getTransportId());
        assertTrue(f.bay().getTroops().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void recoveryUsesTheSurvivingDoorsCurrentFloorAfterOptionalCollapse(boolean expanded) {
        var f = fixture();
        f.carrier().configureConstruction(BuildingType.HARDENED, IBuilding.HANGAR, 4, 75, 0,
              List.of(CubeCoords.ZERO, EAST));
        var destroyedDoor = new BuildingDesign.BayDoor(7, new BuildingDesign.Position(EAST, 1), 0);
        var upperDoor = new BuildingDesign.BayDoor(7, new BuildingDesign.Position(EAST, 3), 1);
        var originalDoors = List.of(destroyedDoor, upperDoor);
        f.carrier().getDesign().getBayDoors().clear();
        f.carrier().getDesign().getBayDoors().addAll(originalDoors);
        f.carrier().setElevation(6);
        f.carrier().updateBuildingEntityHexes(0, f.manager());
        f.manager().getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(expanded);
        f.manager().resetEntityRound();
        assertEquals(expanded, f.carrier().usesExpandedCF());
        f.carrier().setDone(true);
        if (expanded) {
            var floors = f.carrier().getInternalBuilding().getFloorState(EAST);
            floors.setCF(1, 0);
            floors.setCF(2, 1);
            floors.setCF(3, 1);
            assertTrue(new BuildingCollapseHandler(f.manager()).resolveExpandedCollapse(f.carrier(),
                  f.carrier().relativeToBoard(EAST), new java.util.Vector<>()));
        }
        assertEquals(expanded ? -1 : 1, BuildingBayDoors.physicalLevel(f.carrier(), destroyedDoor));
        assertEquals(expanded ? 2 : 3, BuildingBayDoors.physicalLevel(f.carrier(), upperDoor));
        assertEquals(expanded ? List.of(1) : List.of(0, 1), BuildingBayDoors.usablePlacementIndices(f.carrier(), f.bay()));
        // Native level 8 is NOE; level 9 is altitude 2. Settling crosses that actual recovery boundary.
        f.fighter().setAltitude(expanded ? 1 : 2);
        recover(f, 12);
        assertEquals(f.carrier().getId(), f.fighter().getTransportId());
        assertEquals(1, f.bay().availableRecoverySlots(1));
        assertEquals(originalDoors, f.carrier().getDesign().getBayDoors());
        assertTrue(BuildingBayDoors.damage(f.carrier(), f.bay(), upperDoor));
        assertEquals(Map.of(upperDoor, 1), f.carrier().getBuildingRuntimeState().getDamagedBayDoors());
    }
}
