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
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.bays.ASFBay;
import megamek.common.bays.BattleArmorBay;
import megamek.common.bays.LightVehicleBay;
import megamek.common.bays.MekBay;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.moves.MobileStructureLinkage;
import megamek.common.moves.MovePath;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.*;
import megamek.common.util.SerializationHelper;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MobileStructureBayDoorDamageTest {
    private static final Coords ORIGIN = new Coords(8, 8);
    private static final BuildingDesign.BayDoor NORTH =
          new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 0), 0);
    private static final BuildingDesign.BayDoor SOUTH =
          new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 0), 3);

    private static class TestManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }

    private record Fixture(TWGameManager manager, MobileStructure mobile, MekBay bay, BipedMek first, BipedMek second) { }

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private TWGameManager manager() {
        var manager = spy(new TestManager());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        manager.getGame().setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        manager.getGame().addPlayer(0, new Player(0, "Test"));
        manager.getGame().setPhase(GamePhase.LOUNGE);
        return manager;
    }

    private Fixture fixture(int classification, int doors, List<BuildingDesign.BayDoor> placements) {
        var manager = manager();
        var mobile = new MobileStructure(BuildingType.HEAVY, classification);
        mobile.configureConstruction(BuildingType.HEAVY, classification, 3,
              classification == IBuilding.HANGAR ? 45 : 90, 0, List.of(CubeCoords.ZERO));
        mobile.setId(1);
        mobile.setOwner(manager.getGame().getPlayer(0));
        mobile.setDeployed(true);
        manager.getGame().addEntity(mobile);
        mobile.setPosition(ORIGIN);
        mobile.updateBuildingEntityHexes(0, manager);
        var bay = new MekBay(4, doors, 7);
        mobile.addTransporter(bay);
        mobile.getDesign().getBayDoors().addAll(placements);
        var first = cargo(manager, mobile, 2);
        var second = cargo(manager, mobile, 3);
        manager.getGame().setPhase(GamePhase.MOVEMENT);
        manager.getGame().setRoundCount(1);
        return new Fixture(manager, mobile, bay, first, second);
    }

    private BipedMek cargo(TWGameManager manager, MobileStructure mobile, int id) {
        var unit = new BipedMek();
        unit.setChassis("Cargo " + id);
        unit.setModel("Door test");
        unit.setId(id);
        unit.setWeight(50);
        unit.setOriginalWalkMP(5);
        unit.setOwner(mobile.getOwner());
        for (int loc = 0; loc < unit.locations(); loc++) {
            unit.initializeInternal(100, loc);
            unit.initializeArmor(500, loc);
        }
        manager.getGame().addEntity(unit);
        mobile.load(unit, false, 7);
        unit.setTransportId(mobile.getId());
        unit.setLoadedThisTurn(false);
        return unit;
    }

    @Test void unloadDamageClosesTheUsedDoorAndSurvivesCompleteGameSave() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        Coords north = ORIGIN.translated(0), south = ORIGIN.translated(3);
        assertEquals(NORTH, MobileStructureCargoRules.exit(f.mobile(), f.first(), north).door());
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(2)).thenReturn(2);
            assertTrue(f.manager().unloadUnit(f.mobile(), f.first(), north, 0, 0));
        }
        assertEquals(3, f.first().mpUsed, "half of five Walking MP rounds up");
        assertEquals(1, f.bay().getCurrentDoors());
        assertEquals(1, f.bay().getDoorsNext());
        assertEquals(Map.of(NORTH, 1), f.mobile().getBuildingRuntimeState().getDamagedBayDoors());
        f.bay().resetCounts();
        assertNull(MobileStructureCargoRules.exit(f.mobile(), f.second(), north));
        assertNotNull(MobileStructureCargoRules.exit(f.mobile(), f.second(), south));
        String xml = SerializationHelper.getSaveGameXStream().toXML(f.manager().getGame());
        var loaded = (Game) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
        var restoredManager = manager();
        restoredManager.setGame(loaded);
        var mobile = (MobileStructure) loaded.getEntity(1);
        var second = loaded.getEntity(3);
        assertEquals(Map.of(NORTH, 1), mobile.getBuildingRuntimeState().getDamagedBayDoors());
        assertEquals(1, mobile.getBay(second).getCurrentDoors());
        assertNull(MobileStructureCargoRules.exit(mobile, second, north));
        assertNotNull(MobileStructureCargoRules.exit(mobile, second, south));
    }

    @Test void twoHangarDoorsOnOneEdgeAreDamagedSeparately() {
        var f = fixture(IBuilding.HANGAR, 3, List.of(NORTH, NORTH, SOUTH));
        assertTrue(BuildingBayDoors.damage(f.mobile(), f.bay(), NORTH));
        assertEquals(2, f.bay().getCurrentDoors());
        assertEquals(List.of(NORTH, SOUTH), BuildingBayDoors.usablePlacements(f.mobile(), f.bay()));
        assertEquals(List.of(1, 2), BuildingBayDoors.usablePlacementIndices(f.mobile(), f.bay()));
        assertTrue(BuildingBayDoors.isUsable(f.mobile(), NORTH));
        assertTrue(BuildingBayDoors.damage(f.mobile(), f.bay(), NORTH));
        assertEquals(1, f.bay().getCurrentDoors());
        assertEquals(List.of(SOUTH), BuildingBayDoors.usablePlacements(f.mobile(), f.bay()));
        assertEquals(List.of(2), BuildingBayDoors.usablePlacementIndices(f.mobile(), f.bay()));
        assertFalse(BuildingBayDoors.isUsable(f.mobile(), NORTH));
        assertFalse(BuildingBayDoors.damage(f.mobile(), f.bay(), NORTH));
        assertEquals(1, f.bay().getCurrentDoors());
        assertEquals(2, f.mobile().getBuildingRuntimeState().getDamagedBayDoors().get(NORTH));
    }

    @Test void genericCriticalDamageUpdatesOnePhysicalDoorAndTheBayCountOnce() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        f.bay().destroyDoor();
        f.mobile().resetBayDoors();
        assertEquals(1, f.bay().getCurrentDoors());
        assertEquals(1, f.bay().getDoorsNext());
        assertEquals(1, f.mobile().getBuildingRuntimeState().getDamagedBayDoors().values().stream()
              .mapToInt(Integer::intValue).sum());
        assertEquals(1, BuildingBayDoors.usablePlacements(f.mobile(), f.bay()).size());
        f.bay().destroyDoor();
        assertEquals(0, f.bay().getCurrentDoors());
        assertTrue(BuildingBayDoors.usablePlacements(f.mobile(), f.bay()).isEmpty());
    }

    @Test void predictedOnBoardDoorCanUnloadAfterItsOffBoardStartingPoseMoves() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        f.mobile().configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 3, 90, 0,
              List.of(CubeCoords.ZERO, new CubeCoords(1, 0, -1)));
        f.mobile().updateBuildingEntityHexes(0, f.manager());
        new MobileStructureMovementHandler(f.manager()).relocate(f.mobile(), new Coords(-1, 8), 0, 0);
        assertEquals(0, f.bay().getUsableDoors());
        assertTrue(MobileStructureCargoRules.exits(f.mobile(), f.second()).isEmpty());
        var pose = new MobileStructureLinkage.Pose(ORIGIN, 0, 0);
        var predicted = MobileStructureCargoRules.exits(f.mobile(), f.second(), pose);
        assertFalse(predicted.isEmpty());
        new MobileStructureMovementHandler(f.manager()).relocate(f.mobile(), ORIGIN, 0, 0);
        assertEquals(predicted, MobileStructureCargoRules.exits(f.mobile(), f.second()));
    }

    @Test void predictedOnBoardDoorCanLoadWithoutBypassingBayTypeOrCapacity() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        f.mobile().configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 3, 90, 0,
              List.of(CubeCoords.ZERO, new CubeCoords(1, 0, -1)));
        f.mobile().updateBuildingEntityHexes(0, f.manager());
        assertTrue(f.mobile().unload(f.first()));
        f.first().setTransportId(Entity.NONE);
        f.first().setUnloaded(false);
        f.first().setDeployed(true);
        f.first().setDone(false);
        f.first().setPosition(ORIGIN.translated(0));
        f.bay().resetCounts();
        new MobileStructureMovementHandler(f.manager()).relocate(f.mobile(), new Coords(-1, 8), 0, 0);
        assertFalse(f.bay().canLoad(f.first()));
        var projected = new MobileStructureLinkage.Pose(ORIGIN, 0, 0);
        assertTrue(MobileStructureCargoRules.loadableUnits(f.mobile(), projected).contains(f.first()));
        assertFalse(f.bay().canLoad(new Tank(), 2), "proposed doors must not bypass the bay's native unit-type validator");
        var full = new MekBay(0, 2, 10);
        assertFalse(full.canLoad(f.first(), 2));
        new MobileStructureMovementHandler(f.manager()).relocate(f.mobile(), ORIGIN, 0, 0);
        assertTrue(MobileStructureCargoRules.loadableUnits(f.mobile(), projected).contains(f.first()));
    }

    @Test void countOnlyLegacyBayDamageDoesNotCreateAnAuthoredDoor() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of());
        assertTrue(BuildingBayDoors.damage(f.mobile(), f.bay(), null));
        assertEquals(1, f.bay().getCurrentDoors());
        assertEquals(1, f.bay().getUsableDoors());
        assertTrue(f.mobile().getDesign().getBayDoors().isEmpty());
        assertTrue(f.mobile().getBuildingRuntimeState().getDamagedBayDoors().isEmpty());
    }

    @Test void carrierLoadActionUsesItsActualDoorAndChargesOnlyPassengerMovement() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        var passenger = f.first();
        assertTrue(f.mobile().unload(passenger));
        passenger.setTransportId(Entity.NONE);
        passenger.setUnloaded(false);
        passenger.setDeployed(true);
        passenger.setDone(false);
        passenger.setPosition(ORIGIN.translated(0));
        passenger.setElevation(0);
        passenger.setTargetBay(7);
        f.bay().resetCounts();
        var path = new MovePath(f.manager().getGame(), f.mobile())
              .addStep(MoveStepType.LOAD, passenger, passenger.getPosition());
        assertTrue(path.isMoveLegal());
        assertEquals(0, path.getMpUsed());
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(2)).thenReturn(2);
            new MobileStructureMovementHandler(f.manager()).process(f.mobile(), path);
        }
        assertEquals(f.mobile().getId(), passenger.getTransportId());
        assertNull(passenger.getPosition());
        assertEquals(3, passenger.mpUsed);
        assertTrue(passenger.isDone());
        assertEquals(0, f.mobile().mpUsed);
        assertEquals(Map.of(NORTH, 1), f.mobile().getBuildingRuntimeState().getDamagedBayDoors());
    }

    @Test void carrierLoadActionRejectsAWaitingUnitAwayFromItsDoors() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        var passenger = f.first();
        assertTrue(f.mobile().unload(passenger));
        passenger.setTransportId(Entity.NONE);
        passenger.setUnloaded(false);
        passenger.setDeployed(true);
        passenger.setDone(false);
        passenger.setPosition(ORIGIN.translated(1, 4));
        f.bay().resetCounts();
        var path = new MovePath(f.manager().getGame(), f.mobile())
              .addStep(MoveStepType.LOAD, passenger, passenger.getPosition());
        assertFalse(path.isMoveLegal());
        new MobileStructureMovementHandler(f.manager()).process(f.mobile(), path);
        assertEquals(Entity.NONE, passenger.getTransportId());
        assertEquals(0, passenger.mpUsed);
        assertTrue(f.mobile().getBuildingRuntimeState().getDamagedBayDoors().isEmpty());
    }

    private void water(Fixture f) {
        for (int x = 0; x < 20; x++) {
            for (int y = 0; y < 20; y++) {
                f.manager().getGame().getBoard().getHex(new Coords(x, y)).addTerrain(new Terrain(Terrains.WATER, 10));
            }
        }
        f.mobile().setMovementMode(EntityMovementMode.SUBMARINE);
        f.mobile().updateBuildingEntityHexes(0, f.manager());
    }

    private Fixture jumpingCargo() throws Exception {
        var f = fixture(IBuilding.FORTRESS, 1, List.of(
              new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 2), 0)));
        water(f);
        f.first().setOriginalJumpMP(4);
        for (int n = 0; n < 4; n++) { f.first().addEquipment(EquipmentType.get("Jump Jet"), Mek.LOC_RIGHT_TORSO); }
        assertEquals(4, f.first().getJumpMP());
        return f;
    }

    @Test void jumpDismountCannotCrossAHighHillBlockingEveryShortestPath() throws Exception {
        var f = jumpingCargo();
        Coords destination = ORIGIN.translated(0, 3);
        f.manager().getGame().getBoard().getHex(destination).removeTerrain(Terrains.WATER);
        f.manager().getGame().getBoard().getHex(ORIGIN.translated(0, 2)).setLevel(5);
        assertNull(MobileStructureCargoRules.exit(f.mobile(), f.first(), destination));
        f.manager().getGame().getBoard().getHex(ORIGIN.translated(0, 2)).setLevel(4);
        assertNotNull(MobileStructureCargoRules.exit(f.mobile(), f.first(), destination));
    }

    @Test void jumpDismountCanChooseAnotherShortestPathAndIgnoresTrees() throws Exception {
        var f = jumpingCargo();
        Coords destination = ORIGIN.translated(0, 2).translated(1);
        var board = f.manager().getGame().getBoard();
        board.getHex(destination).removeTerrain(Terrains.WATER);
        Coords blocked = ORIGIN.translated(0, 2);
        Coords alternate = ORIGIN.translated(0).translated(1);
        board.getHex(blocked).setLevel(5);
        board.getHex(alternate).addTerrain(new Terrain(Terrains.WOODS, 3));
        assertNotNull(MobileStructureCargoRules.exit(f.mobile(), f.first(), destination));
        board.getHex(alternate).setLevel(5);
        assertNull(MobileStructureCargoRules.exit(f.mobile(), f.first(), destination));
    }

    @Test void jumpDismountCannotLeaveThroughTheOppositeSideOfTheHull() throws Exception {
        var f = jumpingCargo();
        Coords destination = ORIGIN.translated(3, 2);
        f.manager().getGame().getBoard().getHex(destination).removeTerrain(Terrains.WATER);
        assertNull(MobileStructureCargoRules.exit(f.mobile(), f.first(), destination));
        f.mobile().getDesign().getBayDoors().clear();
        f.mobile().getDesign().getBayDoors().add(new BuildingDesign.BayDoor(7,
              new BuildingDesign.Position(CubeCoords.ZERO, 2), 3));
        assertNotNull(MobileStructureCargoRules.exit(f.mobile(), f.first(), destination));
    }

    @Test void jumpDismountLandsOnBuildingRoof() throws Exception {
        var f = jumpingCargo();
        Coords destination = ORIGIN.translated(0, 3);
        f.manager().getGame().getBoard().getHex(destination).removeTerrain(Terrains.WATER);
        var building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        building.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 2, 40, 0, List.of(CubeCoords.ZERO));
        building.setId(4);
        building.setOwner(f.mobile().getOwner());
        building.setDeployed(true);
        f.manager().getGame().addEntity(building);
        building.setPosition(destination);
        building.updateBuildingEntityHexes(0, f.manager());
        var exit = MobileStructureCargoRules.exit(f.mobile(), f.first(), destination);
        assertNotNull(exit);
        assertEquals(2, exit.elevation());
        assertTrue(f.manager().unloadUnit(f.mobile(), f.first(), destination, 0, 2));
        assertEquals(2, f.first().getElevation());
    }

    @Test void predictedSubmersionDisablesHoverDismountOnlyOnceTheWholeGroupIsSubmerged() throws Exception {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        water(f);
        f.mobile().configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 3, 90, 0,
              List.of(CubeCoords.ZERO, new CubeCoords(1, 0, -1)));
        f.mobile().updateBuildingEntityHexes(0, f.manager());
        var hover = new Tank();
        hover.setMovementMode(EntityMovementMode.HOVER);
        hover.setWeight(20);
        hover.setOriginalWalkMP(5);
        hover.setOwner(f.mobile().getOwner());
        hover.setId(4);
        for (int loc = 0; loc < hover.locations(); loc++) { hover.initializeInternal(20, loc); }
        var bay = new LightVehicleBay(1, 1, 8);
        f.mobile().addTransporter(bay);
        f.mobile().getDesign().getBayDoors().add(new BuildingDesign.BayDoor(8,
              new BuildingDesign.Position(CubeCoords.ZERO, 2), 5));
        f.manager().getGame().addEntity(hover);
        f.mobile().load(hover, false, 8);
        hover.setTransportId(f.mobile().getId());
        hover.setLoadedThisTurn(false);
        assertFalse(MobileStructureCargoRules.exits(f.mobile(), hover).isEmpty());
        var submerged = new MobileStructureLinkage.Pose(ORIGIN, 0, -2);
        assertTrue(MobileStructureCargoRules.exits(f.mobile(), hover, submerged).isEmpty());
        var tall = new MobileStructure(BuildingType.HEAVY, IBuilding.FORTRESS);
        tall.configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 7, 90, 0, List.of(CubeCoords.ZERO));
        tall.setMovementMode(EntityMovementMode.SUBMARINE);
        tall.setOwner(f.mobile().getOwner());
        tall.setId(5);
        tall.setDeployed(true);
        f.manager().getGame().addEntity(tall);
        tall.setPosition(ORIGIN.toCube().add(new CubeCoords(2, 0, -2)).toOffset());
        tall.updateBuildingEntityHexes(0, f.manager());
        f.mobile().addEquipment(EquipmentType.get("Modular Structure Linkage"),
              f.mobile().getLocationsAt(f.mobile().relativeToBoard(new CubeCoords(1, 0, -1))).getFirst());
        tall.addEquipment(EquipmentType.get("Modular Structure Linkage"), 0);
        assertTrue(MobileStructureLinkage.link(f.mobile(), tall));
        assertFalse(MobileStructureCargoRules.exits(f.mobile(), hover, submerged).isEmpty(),
              "the connected taller module still projects above the surface");
    }

    @Test void carrierLoadUsesCurrentMovementAndRejectsCompletedOrExhaustedPassengers() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        var passenger = f.first();
        assertTrue(f.mobile().unload(passenger));
        passenger.setTransportId(Entity.NONE);
        passenger.setUnloaded(false);
        passenger.setDeployed(true);
        passenger.setPosition(ORIGIN.translated(0));
        passenger.setTargetBay(7);
        passenger.heat = 10;
        assertEquals(3, passenger.getWalkMP());
        f.bay().resetCounts();
        var pose = new MobileStructureLinkage.Pose(ORIGIN, 0, 0);
        passenger.setDone(true);
        assertFalse(MobileStructureCargoRules.loadableUnits(f.mobile(), pose).contains(passenger));
        passenger.setDone(false);
        passenger.mpUsed = 2;
        assertFalse(MobileStructureCargoRules.loadableUnits(f.mobile(), pose).contains(passenger));
        passenger.mpUsed = 0;
        assertTrue(MobileStructureCargoRules.loadableUnits(f.mobile(), pose).contains(passenger));
        var otherBay = new MekBay(1, 1, 8);
        f.mobile().addTransporter(otherBay);
        f.mobile().getDesign().getBayDoors().add(new BuildingDesign.BayDoor(8,
              new BuildingDesign.Position(CubeCoords.ZERO, 0), 5));
        passenger.setTargetBay(8);
        assertFalse(MobileStructureCargoRules.loadableUnits(f.mobile(), pose).contains(passenger),
              "an otherwise compatible bay cannot load through another bay's door");
        passenger.setTargetBay(7);
        new MobileStructureMovementHandler(f.manager()).process(f.mobile(), new MovePath(f.manager().getGame(), f.mobile())
              .addStep(MoveStepType.LOAD, passenger, passenger.getPosition()));
        assertEquals(f.mobile().getId(), passenger.getTransportId());
        assertEquals(2, passenger.mpUsed, "half current Walking MP, rounded up");
    }

    @Test void explicitNavalMissileEjectionPacketMakesBattleArmorJumpDismountAvailable() throws Exception {
        var f = jumpingCargo();
        var armor = new BattleArmor();
        armor.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
        armor.setChassisType(BattleArmor.CHASSIS_TYPE_BIPED);
        armor.setSquadSize(4);
        armor.autoSetInternal();
        armor.setOriginalJumpMP(3);
        armor.setMovementMode(EntityMovementMode.INF_JUMP);
        armor.setId(4);
        armor.setOwner(f.mobile().getOwner());
        var launcher = armor.addEquipment(EquipmentType.get("ISBASRM2"), BattleArmor.LOC_SQUAD);
        launcher.setBaMountLoc(BattleArmor.MOUNT_LOC_BODY);
        var ammo = armor.addEquipment(EquipmentType.get("BA-SRM2 Ammo"), BattleArmor.LOC_SQUAD);
        launcher.setLinked(ammo);
        f.manager().getGame().addEntity(armor);
        var bay = new BattleArmorBay(1, 1, 9, false, false);
        f.mobile().addTransporter(bay);
        f.mobile().getDesign().getBayDoors().add(new BuildingDesign.BayDoor(9,
              new BuildingDesign.Position(CubeCoords.ZERO, 2), 3));
        f.mobile().load(armor, false, 9);
        armor.setTransportId(f.mobile().getId());
        armor.setLoadedThisTurn(false);
        Coords destination = ORIGIN.translated(3, 2);
        f.manager().getGame().getBoard().getHex(destination).removeTerrain(Terrains.WATER);
        assertTrue(armor.isBurdened());
        assertEquals(0, armor.getJumpMP());
        assertNull(MobileStructureCargoRules.exit(f.mobile(), armor, destination));
        var receive = TWGameManager.class.getDeclaredMethod("receiveEntityModeChange", Packet.class, int.class);
        receive.setAccessible(true);
        var originalRules = Game.rulesManager;
        Game.rulesManager = new TWRulesManager();
        try {
            // A foreign client cannot eject the equipment; a cancellation is not an ejection announcement.
            var request = new Packet(PacketCommand.ENTITY_MODE_CHANGE, armor.getId(), armor.getEquipmentNum(launcher), -1);
            receive.invoke(f.manager(), request, 99);
            assertFalse(launcher.isMissing());
            receive.invoke(f.manager(), new Packet(PacketCommand.ENTITY_MODE_CHANGE,
                  armor.getId(), armor.getEquipmentNum(launcher), 0), 0);
            assertEquals(0, armor.getJumpMP());
            receive.invoke(f.manager(), request, 0);
        } finally {
            Game.rulesManager = originalRules;
        }
        assertTrue(launcher.isMissing());
        assertTrue(ammo.isMissing());
        assertFalse(launcher.isPendingDump());
        assertEquals(3, armor.getJumpMP());
        assertNotNull(MobileStructureCargoRules.exit(f.mobile(), armor, destination));
    }

    @Test void jumpDismountMustClearGroundedDropships() throws Exception {
        var f = jumpingCargo();
        Coords destination = ORIGIN.translated(0, 3);
        f.manager().getGame().getBoard().getHex(destination).removeTerrain(Terrains.WATER);
        var dropship = new Dropship();
        dropship.setSpheroid(true);
        dropship.setId(4);
        dropship.setOwner(f.mobile().getOwner());
        dropship.setDeployed(true);
        dropship.setAltitude(0);
        for (int loc = 0; loc < dropship.locations(); loc++) { dropship.initializeInternal(20, loc); }
        f.manager().getGame().addEntity(dropship);
        dropship.setPosition(ORIGIN.translated(0, 2));
        assertTrue(dropship.height() > f.first().getJumpMP());
        assertNull(MobileStructureCargoRules.exit(f.mobile(), f.first(), destination));
        dropship.setAltitude(1);
        assertNotNull(MobileStructureCargoRules.exit(f.mobile(), f.first(), destination));
    }

    @Test void passengerMountActionSelectsTheBayWhoseDoorItActuallyApproaches() {
        var f = fixture(IBuilding.FORTRESS, 1, List.of(SOUTH));
        var passenger = f.first();
        assertTrue(f.mobile().unload(passenger));
        passenger.setTransportId(Entity.NONE);
        passenger.setUnloaded(false);
        passenger.setDone(false);
        passenger.setDeployed(true);
        passenger.setPosition(ORIGIN.translated(0));
        passenger.setTargetBay(megamek.common.bays.Bay.UNSET_BAY);
        f.bay().resetCounts();
        var northBay = new MekBay(1, 1, 8);
        f.mobile().addTransporter(northBay);
        var northDoor = new BuildingDesign.BayDoor(8, new BuildingDesign.Position(CubeCoords.ZERO, 0), 0);
        f.mobile().getDesign().getBayDoors().add(northDoor);
        var path = new MovePath(f.manager().getGame(), passenger).addStep(MoveStepType.MOUNT, f.mobile());
        assertTrue(path.isMoveLegal());
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(2)).thenReturn(2);
            new MovePathHandler(f.manager(), passenger, path, null).processMovement();
        }
        assertEquals(f.mobile().getId(), passenger.getTransportId());
        assertSame(northBay, f.mobile().getBay(passenger));
        assertEquals(1, f.bay().getCurrentDoors());
        assertEquals(0, northBay.getCurrentDoors());
        assertEquals(Map.of(northDoor, 1), f.mobile().getBuildingRuntimeState().getDamagedBayDoors());
    }

    @Test void projectedAircraftLoadingKeepsEachPhysicalDoorsOwnRecoveryTimers() {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        var east = new CubeCoords(1, 0, -1);
        f.mobile().configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 3, 90, 0, List.of(CubeCoords.ZERO, east));
        f.mobile().updateBuildingEntityHexes(0, f.manager());
        var bay = new ASFBay(4, 2, 8);
        f.mobile().addTransporter(bay);
        f.mobile().getDesign().getBayDoors().addAll(List.of(
              new BuildingDesign.BayDoor(8, new BuildingDesign.Position(CubeCoords.ZERO, 0), 5),
              new BuildingDesign.BayDoor(8, new BuildingDesign.Position(east, 0), 3)));
        var fighters = new java.util.ArrayList<AeroSpaceFighter>();
        for (int id = 4; id <= 6; id++) {
            var fighter = new AeroSpaceFighter();
            fighter.setId(id);
            fighter.setOwner(f.mobile().getOwner());
            f.manager().getGame().addEntity(fighter);
            fighters.add(fighter);
        }
        bay.recover(fighters.get(0), 1);
        bay.recover(fighters.get(1), 1);
        var waiting = fighters.get(2);
        assertEquals(2, bay.availableRecoverySlots(0));
        assertEquals(0, bay.availableRecoverySlots(1));
        new MobileStructureMovementHandler(f.manager()).relocate(f.mobile(), new Coords(-1, 8), 0, 0);
        assertEquals(List.of(1), BuildingBayDoors.usablePlacementIndices(f.mobile(), bay));
        assertFalse(bay.canLoad(waiting), "the only on-board door is still busy");
        assertFalse(bay.canLoadAt(waiting, new Coords(-1, 8), 0));
        assertTrue(bay.canLoadAt(waiting, ORIGIN, 0), "the newly on-board door has its own two empty slots");
        assertFalse(bay.canLoadAt(f.first(), ORIGIN, 0), "a projected pose cannot bypass aircraft type restrictions");
        assertEquals(0, bay.availableRecoverySlots(1), "previewing does not change any recovery timer");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cargoUsesSettledBayDoorsAndCannotUseDestroyedFloorDoors(boolean expanded) {
        var f = fixture(IBuilding.FORTRESS, 2, List.of(NORTH, SOUTH));
        f.mobile().configureConstruction(BuildingType.HEAVY, IBuilding.FORTRESS, 4, 90, 0, List.of(CubeCoords.ZERO));
        var upperDoor = new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 3), 0);
        var destroyedDoor = new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 1), 3);
        var originalDoors = List.of(upperDoor, destroyedDoor);
        f.mobile().getDesign().getBayDoors().clear();
        f.mobile().getDesign().getBayDoors().addAll(originalDoors);
        f.mobile().updateBuildingEntityHexes(0, f.manager());
        Coords north = ORIGIN.translated(0), south = ORIGIN.translated(3);
        f.manager().getGame().getBoard().getHex(north).setLevel(2);
        f.manager().getGame().getBoard().getHex(south).setLevel(1);
        f.manager().getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(expanded);
        f.manager().resetEntityRound();
        assertEquals(expanded, f.mobile().usesExpandedCF());
        if (expanded) {
            var floors = f.mobile().getInternalBuilding().getFloorState(CubeCoords.ZERO);
            floors.setCF(1, 0);
            floors.setCF(2, 1);
            floors.setCF(3, 1);
            assertTrue(new BuildingCollapseHandler(f.manager()).resolveExpandedCollapse(f.mobile(), ORIGIN,
                  new java.util.Vector<>()));
        }
        assertEquals(expanded, MobileStructureCargoRules.exit(f.mobile(), f.first(), north) != null,
              "the upper door becomes low enough to reach the north terrain only after its floor settles");
        assertEquals(!expanded, MobileStructureCargoRules.exit(f.mobile(), f.first(), south) != null,
              "a door disappears with its destroyed floor even when another floor settles into that level");
        if (expanded) {
            try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
                dice.when(() -> Compute.d6(2)).thenReturn(12);
                assertTrue(f.manager().unloadUnit(f.mobile(), f.first(), north, 0, 0));
            }
            assertEquals(north, f.first().getPosition());
            assertEquals(0, f.first().getElevation());
        }
        assertEquals(originalDoors, f.mobile().getDesign().getBayDoors());
    }
}
