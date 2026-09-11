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
 * Catalyst Game Labs and the Catalyst Games Labs logo are trademarks of
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
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.game.Game;
import megamek.common.moves.MobileStructureGeometry;
import megamek.common.moves.MobileStructureLinkage;
import megamek.common.moves.MobileStructureMovement;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.game.GameTurn;
import megamek.common.units.*;
import megamek.common.util.SerializationHelper;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MobileStructureLinkageTest {
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);

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
        Game game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 24 24\nend\n"));
        game.addPlayer(0, new Player(0, "Test"));
        return manager;
    }

    private MobileStructure module(TWGameManager manager, int id, CubeCoords offset, List<CubeCoords> hexes) throws Exception {
        var module = new MobileStructure(BuildingType.HEAVY, IBuilding.FORTRESS);
        module.setChassis("Module " + id);
        module.setModel("Linkage test");
        module.configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 1, 90, 0, hexes);
        module.setOwner(manager.getGame().getPlayer(0));
        module.setId(id);
        module.setDeployed(true);
        manager.getGame().addEntity(module);
        module.setPosition(new Coords(7, 7).toCube().add(offset).toOffset());
        for (int n = 0; n < hexes.size(); n++) {
            module.addEquipment(EquipmentType.get("Modular Structure Linkage"), n);
        }
        module.updateBuildingEntityHexes(0, manager);
        return module;
    }

    private MobileStructure[] pair(TWGameManager manager) throws Exception {
        var a = module(manager, 1, CubeCoords.ZERO, List.of(CubeCoords.ZERO, EAST));
        var b = module(manager, 2, new CubeCoords(2, 0, -2), List.of(CubeCoords.ZERO, EAST));
        return new MobileStructure[] { a, b };
    }

    @Test void nativeMovementPacketLinksWithoutEndingActivationAndCanThenMoveJoinedFootprint() throws Exception {
        var manager = manager();
        var pair = pair(manager);
        var a = pair[0];
        var b = pair[1];
        var game = manager.getGame();
        game.setPhase(GamePhase.MOVEMENT);
        GameTurn turn = mock(GameTurn.class);
        when(turn.isValid(0, a, game)).thenReturn(true);
        game.setTurnVector(List.of(turn));
        game.setTurnIndex(0, 0);
        MovePath action = new MovePath(game, a).addStep(MoveStepType.MODULE_LINK, b);
        assertNotEquals(EntityMovementType.MOVE_ILLEGAL, action.getLastStepMovementType());
        var receive = TWGameManager.class.getDeclaredMethod("receiveMovement", Packet.class, int.class);
        receive.setAccessible(true);
        receive.invoke(manager, new Packet(PacketCommand.ENTITY_MOVE, a.getId(), action), 0);
        assertEquals(List.of(a, b), MobileStructureLinkage.group(a));
        assertFalse(a.isDone());
        assertFalse(b.isDone());
        assertEquals(0, a.mpUsed);
        assertEquals(0, game.getTurnIndex());
        Coords oldA = a.getPosition();
        Coords oldB = b.getPosition();
        new MobileStructureMovementHandler(manager).process(a, new MovePath(game, a).addStep(MoveStepType.FORWARDS));
        assertEquals(oldA.translated(0), a.getPosition());
        assertEquals(oldB.translated(0), b.getPosition());
        assertTrue(a.isDone());
        assertTrue(b.isDone());
        assertEquals(a.mpUsed, b.mpUsed);
        assertEquals(List.of(a, b), MobileStructureLinkage.group(a));
    }

    @Test void linkagePacketCannotHijackAnEnemyOrActOutsideCurrentTurn() throws Exception {
        var manager = manager();
        var pair = pair(manager);
        var game = manager.getGame();
        game.setPhase(GamePhase.MOVEMENT);
        GameTurn turn = mock(GameTurn.class);
        game.setTurnVector(List.of(turn));
        game.setTurnIndex(0, 0);
        var action = new MovePath(game, pair[0]).addStep(MoveStepType.MODULE_LINK, pair[1]);
        var receive = TWGameManager.class.getDeclaredMethod("receiveMovement", Packet.class, int.class);
        receive.setAccessible(true);
        receive.invoke(manager, new Packet(PacketCommand.ENTITY_MOVE, pair[0].getId(), action), 99);
        assertEquals(1, MobileStructureLinkage.group(pair[0]).size());
        pair[1].setOwner(new Player(1, "Enemy"));
        assertFalse(new MobileStructureMovementHandler(manager).processLinkAction(pair[0], action));
    }

    @Test void incompatibleMotivePowerSpeedAndElevationCannotMate() throws Exception {
        var pair = pair(manager());
        var a = pair[0];
        var b = pair[1];
        b.setMaximumMP(1.25);
        assertFalse(MobileStructureLinkage.canLink(a, b));
        b.setMaximumMP(1);
        b.setPowerSystem(StructureEngine.FISSION);
        assertFalse(MobileStructureLinkage.canLink(a, b));
        b.setPowerSystem(StructureEngine.FUSION);
        b.setMovementMode(EntityMovementMode.VTOL);
        assertFalse(MobileStructureLinkage.canLink(a, b));
        b.setMovementMode(EntityMovementMode.TRACKED);
        b.setElevation(1);
        assertFalse(MobileStructureLinkage.canLink(a, b));
        b.setElevation(0);
        assertTrue(MobileStructureLinkage.canLink(a, b));
    }

    @Test void oneDisabledModuleCripplesGroupUntilImmediateUnlink() throws Exception {
        var manager = manager();
        var pair = pair(manager);
        var a = pair[0];
        var b = pair[1];
        assertTrue(MobileStructureLinkage.link(a, b));
        for (int n = 0; n < 6; n++) { b.addMobileCrewHit(); }
        assertTrue(a.isImmobile());
        assertEquals(0, a.getWalkMP());
        assertTrue(a.isEligibleForMovement(), "the crew must be able to break the links");
        var action = new MovePath(manager.getGame(), a).addStep(MoveStepType.MODULE_UNLINK, b);
        assertNotEquals(EntityMovementType.MOVE_ILLEGAL, action.getLastStepMovementType());
        assertTrue(new MobileStructureMovementHandler(manager).processLinkAction(a, action));
        assertFalse(a.isImmobile());
        assertTrue(b.isImmobile());
        assertFalse(a.isDone());
    }

    @Test void destroyingOneOfTwoRedundantLinkageHexesKeepsTheOtherConnection() throws Exception {
        var manager = manager();
        var vertical = List.of(CubeCoords.ZERO, new CubeCoords(0, 1, -1));
        var a = module(manager, 1, CubeCoords.ZERO, vertical);
        var b = module(manager, 2, EAST, vertical);
        assertTrue(MobileStructureLinkage.link(a, b));
        assertEquals(2, a.getModuleLinks().size());
        a.removeHex(a.relativeToBoard(CubeCoords.ZERO));
        assertEquals(1, a.getModuleLinks().size());
        assertEquals(2, MobileStructureLinkage.group(b).size());
        a.removeHex(a.relativeToBoard(new CubeCoords(0, 1, -1)));
        assertTrue(a.getModuleLinks().isEmpty());
        assertEquals(1, MobileStructureLinkage.group(b).size());
    }

    @Test void sharedPivotIsSelectedFromCombinedFootprintAndRotatesModulesRigidly() throws Exception {
        var manager = manager();
        var pair = pair(manager);
        var a = pair[0];
        var b = pair[1];
        assertTrue(MobileStructureLinkage.link(a, b));
        CubeCoords pivot = new CubeCoords(2, 0, -2);
        assertTrue(MobileStructureGeometry.pivots(a).contains(pivot), "the pivot can be in another module");
        Coords fixed = b.getPosition();
        var turn = new MovePath(manager.getGame(), a).addStep(MoveStepType.TURN_RIGHT,
              Map.of(MoveStep.MOBILE_PIVOT_Q_KEY, 2, MoveStep.MOBILE_PIVOT_R_KEY, 0));
        assertNotEquals(EntityMovementType.MOVE_ILLEGAL, turn.getLastStepMovementType());
        new MobileStructureMovementHandler(manager).process(a, turn);
        assertEquals(fixed, b.getPosition());
        assertEquals(1, a.getFacing());
        assertEquals(1, b.getFacing());
        assertEquals(2, MobileStructureLinkage.group(a).size());
    }

    @Test void illegalTerrainUnderOnlyTheSecondModulePreventsEitherModuleMoving() throws Exception {
        var manager = manager();
        var pair = pair(manager);
        var a = pair[0];
        var b = pair[1];
        assertTrue(MobileStructureLinkage.link(a, b));
        Coords oldA = a.getPosition();
        Coords oldB = b.getPosition();
        manager.getGame().getBoard().getHex(oldB.translated(0)).addTerrain(new Terrain(Terrains.IMPASSABLE, 1));
        var path = new MovePath(manager.getGame(), a).addStep(MoveStepType.FORWARDS);
        assertEquals(EntityMovementType.MOVE_ILLEGAL, path.getLastStepMovementType());
        new MobileStructureMovementHandler(manager).process(a, path);
        assertEquals(oldA, a.getPosition());
        assertEquals(oldB, b.getPosition());
        assertEquals(0, a.mpUsed);
        assertEquals(0, b.mpUsed);
    }

    @Test void allPosesChangeBeforeAnyRiderIsPublished() throws Exception {
        var manager = manager();
        var pair = pair(manager);
        var a = pair[0];
        var b = pair[1];
        assertTrue(MobileStructureLinkage.link(a, b));
        var rider = new BipedMek();
        rider.setId(10);
        rider.setOwner(a.getOwner());
        rider.setDeployed(true);
        manager.getGame().addEntity(rider);
        rider.setPosition(a.getPosition());
        rider.setElevation(a.getBaseElevation(a.getPosition()));
        assertTrue(manager.getGame().getEntitiesVector(a.getPosition(), a.getBoardId()).contains(rider));
        Coords targetA = a.getPosition().translated(0);
        Coords targetB = b.getPosition().translated(0);
        doAnswer(invocation -> {
            assertEquals(targetA, a.getPosition());
            assertEquals(targetB, b.getPosition());
            return null;
        }).when(manager).entityUpdate(rider.getId());
        new MobileStructureMovementHandler(manager).relocate(a, targetA, a.getFacing(), a.getElevation());
        assertEquals(targetA, rider.getPosition());
        verify(manager).entityUpdate(rider.getId());
    }

    @Test void slowModulesCommitSameQuarterPointsAndCannotUseSeparateActivations() throws Exception {
        var manager = manager();
        var pair = pair(manager);
        var a = pair[0];
        var b = pair[1];
        a.setMaximumMP(.5);
        b.setMaximumMP(.5);
        assertTrue(MobileStructureLinkage.link(a, b));
        Coords startA = a.getPosition();
        Coords startB = b.getPosition();
        new MobileStructureMovementHandler(manager).process(a, new MovePath(manager.getGame(), a).addStep(MoveStepType.FORWARDS));
        assertEquals(startA, a.getPosition());
        assertEquals(startB, b.getPosition());
        assertEquals(2, a.getMovementProgress().quarters());
        assertEquals(2, b.getMovementProgress().quarters());
        new MobileStructureMovementHandler(manager).process(b, new MovePath(manager.getGame(), b).addStep(MoveStepType.FORWARDS));
        assertEquals(startB, b.getPosition(), "the other module has already used the shared activation");
        a.newRound(2);
        b.newRound(2);
        a.setDone(false);
        b.setDone(false);
        new MobileStructureMovementHandler(manager).process(b, new MovePath(manager.getGame(), b).addStep(MoveStepType.FORWARDS));
        assertEquals(startA.translated(0), a.getPosition());
        assertEquals(startB.translated(0), b.getPosition());
    }

    @Test void slowestFuelRangeIsRetainedWithoutPoolingFuel() throws Exception {
        var pair = pair(manager());
        for (var unit : pair) { unit.setPowerSystem(StructureEngine.COMBUSTION_LIQUID); }
        pair[0].setOperatingRange(1000);
        pair[1].setOperatingRange(300);
        double fuelA = pair[0].getFuelWeight();
        double fuelB = pair[1].getFuelWeight();
        assertTrue(MobileStructureLinkage.link(pair[0], pair[1]));
        assertEquals(300, MobileStructureLinkage.operatingRange(pair[0]));
        assertEquals(fuelA, pair[0].getFuelWeight());
        assertEquals(fuelB, pair[1].getFuelWeight());
    }

    @Test void completeSaveGameRestoresLinkedAirModulesAndContinuesPaidTakeoff() throws Exception {
        var manager = manager();
        var pair = pair(manager);
        for (var module : pair) {
            module.configureConstruction(BuildingType.HARDENED, IBuilding.HANGAR, 2, 75, 0,
                  List.of(CubeCoords.ZERO, EAST));
            module.setMovementMode(EntityMovementMode.VTOL);
            module.setMaximumMP(.25);
            module.getCrew().setPiloting(0, 0);
            module.updateBuildingEntityHexes(0, manager);
        }
        assertTrue(MobileStructureLinkage.link(pair[0], pair[1]));
        pair[1].setAirLandingGearDamaged(true);
        manager.getGame().setPhase(GamePhase.MOVEMENT);
        for (int round = 1; round <= 3; round++) {
            manager.getGame().setRoundCount(round);
            for (var module : pair) { module.newRound(round); }
            new MobileStructureMovementHandler(manager).process(pair[0],
                  new MovePath(manager.getGame(), pair[0]).addStep(MoveStepType.VERTICAL_TAKE_OFF));
        }
        assertEquals(3, pair[0].getMovementProgress().quarters());
        String xml = SerializationHelper.getSaveGameXStream().toXML(manager.getGame());
        Game loaded = (Game) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
        var restoredManager = manager();
        restoredManager.setGame(loaded);
        var a = (MobileStructure) loaded.getEntity(pair[0].getId());
        var b = (MobileStructure) loaded.getEntity(pair[1].getId());
        assertEquals(List.of(a, b), MobileStructureLinkage.group(a));
        assertEquals(pair[0].getModuleLinks(), a.getModuleLinks());
        assertEquals(3, a.getMovementProgress().quarters());
        assertEquals(a.getMovementProgress().quarters(), b.getMovementProgress().quarters());
        assertTrue(b.isAirLandingGearDamaged());
        assertFalse(a.isAirborneVTOLorWIGE());
        for (int round = 4; round <= 8; round++) {
            loaded.setRoundCount(round);
            a.newRound(round);
            b.newRound(round);
            var path = new MovePath(loaded, b).addStep(MoveStepType.VERTICAL_TAKE_OFF);
            assertTrue(path.isMoveLegal());
            new MobileStructureMovementHandler(restoredManager).process(b, path);
        }
        assertNull(a.getMovementProgress());
        assertNull(b.getMovementProgress());
        assertEquals(1, a.getElevation());
        assertEquals(1, b.getElevation());
        assertEquals(List.of(a, b), MobileStructureLinkage.group(b));
        assertTrue(a.isAirborneVTOLorWIGE());
        assertTrue(b.isAirborneVTOLorWIGE());
    }
}
