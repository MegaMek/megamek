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
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.StructureEngine;
import megamek.common.game.Game;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKStructureFile;
import megamek.common.moves.MobileStructureGeometry;
import megamek.common.moves.MobileStructureMovement;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.net.packets.Packet;
import megamek.common.units.*;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MobileStructureGameplayTest {
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);
    private static final Coords ORIGIN = new Coords(8, 8);

    private static class TestManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private MobileStructure unit(int levels, List<CubeCoords> coords) {
        var unit = new MobileStructure(BuildingType.HARDENED, IBuilding.FORTRESS);
        unit.setChassis("Mobile gameplay");
        unit.setModel("Test");
        unit.setYear(3145);
        unit.setTechLevel(TechConstants.T_IS_ADVANCED);
        unit.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, levels, 150, 0, coords);
        return unit;
    }

    private TWGameManager manager(MobileStructure unit, int waterDepth) {
        var manager = spy(new TestManager());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        Game game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        if (waterDepth > 0) {
            for (int x = 0; x < 20; x++) {
                for (int y = 0; y < 20; y++) {
                    game.getBoard().getHex(new Coords(x, y)).addTerrain(new Terrain(Terrains.WATER, waterDepth));
                }
            }
        }
        Player owner = new Player(0, "Test");
        game.addPlayer(0, owner);
        unit.setOwner(owner);
        unit.setId(1);
        game.addEntity(unit);
        unit.setPosition(ORIGIN);
        unit.updateBuildingEntityHexes(0, manager);
        return manager;
    }

    @Test void printedFreighterFuelDoesNotGainATonFromFloatingPointNoise() {
        var coords = java.util.stream.IntStream.range(0, 20).mapToObj(q -> new CubeCoords(q, 0, -q)).toList();
        var mobile = unit(14, coords);
        mobile.setMovementMode(EntityMovementMode.NAVAL);
        mobile.setPowerSystem(StructureEngine.COMBUSTION_LIQUID);
        mobile.setMaximumMP(1.25);
        mobile.setOperatingRange(7000);
        assertEquals(1050, mobile.getPowerSystemWeight());
        assertEquals(1470, mobile.getFuelWeight(), "TO:AUE printed freighter example");
    }

    @Test void battleValueCountsAllHexesAndUsesPrintedFractionalMP() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        mobile.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 3, 150, 20,
              List.of(CubeCoords.ZERO, EAST));
        mobile.setMaximumMP(1.25);
        // TO:AUE p.191: (40 armor * 2.5 + 300 CF * 1.5) * .5 + (2 hexes * 50) * .57.
        assertEquals(332, mobile.calculateBattleValue(true, true));
        mobile.setMaximumMP(2);
        assertEquals(340, mobile.calculateBattleValue(true, true));
    }

    @Test void fuelConcentrationSurvivesBlkAndCountsOnlyWhereStored() throws Exception {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        mobile.setPowerSystem(StructureEngine.COMBUSTION_LIQUID);
        mobile.setOperatingRange(100);
        mobile.setFuelLocations(Map.of(EAST, mobile.getFuelWeight()));
        var restored = (MobileStructure) new BLKStructureFile(BLKFile.getBlock(mobile)).getEntity();
        assertEquals(mobile.getFuelLocations(), restored.getFuelLocations());
        assertEquals(0, restored.fuelWeightInHex(CubeCoords.ZERO));
        assertEquals(restored.getFuelWeight(), restored.fuelWeightInHex(EAST));
        assertEquals(restored.getFuelWeight(), restored.systemWeightInHex(EAST)
              - restored.systemWeightInHex(CubeCoords.ZERO));
    }

    @Test void genuineMekBayExportLoadsFuelAndInteriorFeatures() throws Exception {
        try (var stream = java.nio.file.Files.newInputStream(java.nio.file.Path.of(
              "testresources/megamek/common/units/MekBayMobileStructure.blk"))) {
            var mobile = (MobileStructure) new BLKStructureFile(
                  new megamek.common.util.BuildingBlock(stream)).getEntity();
            assertEquals(StructureEngine.FUEL_CELL, mobile.getPowerSystem());
            assertEquals(1.25, mobile.getMaximumMP());
            assertEquals(3, mobile.getOriginalHexCount());
            assertEquals(1, mobile.getInternalBuilding().getHeight(EAST));
            assertEquals(Map.of(EAST, 9.0), mobile.getFuelLocations());
            assertEquals(1, mobile.getDesign().getDoors().size());
            assertEquals(1, mobile.getDesign().getElevators().size());
            var door = mobile.getDesign().getBayDoors().getFirst();
            assertEquals(7, door.bayNumber());
            assertEquals(new CubeCoords(0, 1, -1), door.position().hex());
            assertEquals(0, door.position().level());
            assertEquals(3, door.facing());
            assertTrue(mobile.getTransportBays().stream().anyMatch(bay -> bay.getBayNumber() == door.bayNumber()));
            assertEquals("InfantryStandardSRM", mobile.getWeaponList().getFirst().getType().getInternalName());
        }
    }

    @Test void genuineMekBayPortalExportPreservesExplicitEquipmentTemplateReferences() throws Exception {
        try (var stream = java.nio.file.Files.newInputStream(java.nio.file.Path.of(
              "testresources/megamek/common/units/MekBayMobilePortal.blk"))) {
            var mobile = (MobileStructure) new BLKStructureFile(
                  new megamek.common.util.BuildingBlock(stream)).getEntity();
            assertEquals(IBuilding.HANGAR, mobile.getBldgClass());
            assertTrue(mobile.getDesign().isOpenSpace());
            assertEquals(CubeCoords.ZERO, mobile.getDesign().getPortalHex2());
            assertEquals(EAST, mobile.getDesign().getPortalHex3());
        }
    }

    @Test void waterSpeedAcceleratesByOneMPAndDisabledVesselCoastsEveryOtherTurn() {
        var mobile = unit(4, List.of(CubeCoords.ZERO, EAST));
        mobile.setMovementMode(EntityMovementMode.NAVAL);
        mobile.setMaximumMP(3);
        assertEquals(4, mobile.declareWaterSpeed(12, 1));
        assertEquals(4, mobile.declareWaterSpeed(12, 1), "a second packet cannot accelerate again");
        assertEquals(8, mobile.declareWaterSpeed(12, 2));
        assertEquals(12, mobile.declareWaterSpeed(12, 3));
        for (int n = 0; n < 6; n++) { mobile.addMobileCrewHit(); }
        assertEquals(12, mobile.declareWaterSpeed(0, 4));
        assertEquals(8, mobile.declareWaterSpeed(0, 5));
        assertEquals(8, mobile.declareWaterSpeed(0, 6));
        assertEquals(4, mobile.declareWaterSpeed(0, 7));
        assertEquals(4, mobile.declareWaterSpeed(0, 8));
        assertEquals(0, mobile.declareWaterSpeed(0, 9));
    }

    @Test void submarineCannotChangeDepthTwiceDespiteEnoughMP() {
        var mobile = unit(4, List.of(CubeCoords.ZERO, EAST));
        mobile.setMovementMode(EntityMovementMode.SUBMARINE);
        mobile.setMaximumMP(4);
        var manager = manager(mobile, 20);
        mobile.declareWaterSpeed(16, -3);
        mobile.declareWaterSpeed(16, -2);
        var path = new MovePath(manager.getGame(), mobile).addStep(MoveStepType.DOWN).addStep(MoveStepType.DOWN);
        path.setMobileSpeedQuarters(12);
        new MobileStructureMovementHandler(manager).process(mobile, path);
        assertEquals(-1, mobile.getElevation());
        assertEquals(1, mobile.getDepthChangesThisTurn());
    }

    @Test void shallowWaterEntryIsAllowedThenGroundsAndDamagesOnlyShallowHex() {
        var mobile = unit(4, List.of(CubeCoords.ZERO, EAST));
        mobile.setMovementMode(EntityMovementMode.NAVAL);
        var manager = manager(mobile, 5);
        Coords shallow = ORIGIN.translated(0);
        manager.getGame().getBoard().getHex(shallow).addTerrain(new Terrain(Terrains.WATER, 1));
        var path = new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS);
        new MobileStructureMovementHandler(manager).process(mobile, path);
        assertEquals(shallow, mobile.getPosition());
        assertTrue(mobile.isGrounded());
        assertEquals(0, mobile.getWaterSpeedQuarters());
        assertEquals(125, mobile.getCurrentCF(shallow), "50 points scaled for Fortress");
        assertEquals(150, mobile.getCurrentCF(mobile.relativeToBoard(EAST)));
    }

    @ParameterizedTest @CsvSource({"0,1", "1,0"})
    void eitherTiedCentralHexCanBeChosenAsPivot(int selected, int other) {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        var pivots = MobileStructureGeometry.pivots(mobile);
        assertEquals(2, pivots.size());
        var pivot = pivots.get(selected);
        Coords stationary = mobile.relativeToBoard(pivot);
        var path = new MovePath(manager.getGame(), mobile).addStep(MoveStepType.TURN_RIGHT,
              Map.of(MoveStep.MOBILE_PIVOT_Q_KEY, (int) pivot.q(), MoveStep.MOBILE_PIVOT_R_KEY, (int) pivot.r()));
        new MobileStructureMovementHandler(manager).process(mobile, path);
        assertEquals(1, mobile.getFacing());
        assertEquals(stationary, mobile.relativeToBoard(pivot));
        assertNotEquals(mobile.relativeToBoard(pivots.get(other)), stationary);
    }

    @Test void pivotSweepIncludesCellsNotInEitherEndpoint() {
        var mobile = unit(3, List.of(EAST.add(EAST), EAST, CubeCoords.ZERO,
              new CubeCoords(-1, 0, 1), new CubeCoords(-2, 0, 2)));
        manager(mobile, 0);
        var pivot = MobileStructureGeometry.pivots(mobile).getFirst();
        Coords end = MobileStructureGeometry.pivotOrigin(mobile, ORIGIN, 0, 1, pivot);
        var endpoints = new java.util.HashSet<>(mobile.getCoordsList());
        endpoints.addAll(mobile.computeBuildingCoordsForPositionAndFacing(end, 1));
        assertTrue(MobileStructureGeometry.contacts(mobile, ORIGIN, 0, end, 1).stream()
              .anyMatch(contact -> !endpoints.contains(contact.boardHex())));
    }

    @Test void offBoardPortionsRemainMovableAndCannotReceiveDamage() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST, EAST.add(EAST)));
        var manager = manager(mobile, 0);
        new MobileStructureMovementHandler(manager).relocate(mobile, new Coords(-1, 5), 0, 0);
        var path = new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS);
        assertEquals(new Coords(-1, 4), path.getFinalCoords());
        assertEquals(4, path.getLastStep().getMp());
        int cf = mobile.getCurrentCF(mobile.getPosition());
        manager.damageBuilding(mobile, 100, mobile.getPosition());
        assertEquals(cf, mobile.getCurrentCF(mobile.getPosition()));
        new MobileStructureMovementHandler(manager).process(mobile, path);
        assertEquals(new Coords(-1, 4), mobile.getPosition());
    }

    @Test void aOneHexCanyonWithinAThreeHexWidthHasAConsistentSupportedUnderside() {
        var mobile = unit(3, List.of(new CubeCoords(-1, 0, 1), CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        manager.getGame().getBoard().getHex(ORIGIN).setLevel(-8);
        assertEquals(10, mobile.getBaseElevation(ORIGIN));
        assertEquals(2, mobile.getBaseElevation(mobile.relativeToBoard(EAST)));
        assertEquals(5, BuildingElevation.altitude(manager.getGame(), mobile, ORIGIN, 3));
        assertEquals(4, new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS).getMpUsed());
    }

    @Test void physicalUndercarriageHeightIncludesTheUnitsOccupiedTopLevel() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        var mek = new BipedMek();
        mek.setWeight(50);
        assertEquals(1, mek.height());
        assertSame(mobile, MobileStructureMovement.undercarriage(manager.getGame(), 0, ORIGIN, 0, mek.height()));
        mek.setWeight(150);
        assertEquals(2, mek.height());
        assertNull(MobileStructureMovement.undercarriage(manager.getGame(), 0, ORIGIN, 0, mek.height()));
    }

    @Test void weatherIsReducedOnlyForMobileStructuresAndPreservesLighting() {
        var conditions = new megamek.common.planetaryConditions.PlanetaryConditions();
        conditions.setWeather(megamek.common.planetaryConditions.Weather.HEAVY_RAIN);
        conditions.setWind(megamek.common.planetaryConditions.Wind.STORM);
        conditions.setLight(megamek.common.planetaryConditions.Light.PITCH_BLACK);
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var applied = conditions.forEntity(mobile);
        assertEquals(megamek.common.planetaryConditions.Weather.LIGHT_RAIN, applied.getWeather());
        assertEquals(megamek.common.planetaryConditions.Wind.MOD_GALE, applied.getWind());
        assertEquals(conditions.getLight(), applied.getLight());
        assertEquals(megamek.common.planetaryConditions.Weather.HEAVY_RAIN, conditions.getWeather());
        assertSame(conditions, conditions.forEntity(new BipedMek()));
    }

    @Test void ordinaryDisplacementCannotMoveTheStructureOrPushATallUnitIntoItsHull() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        manager.doEntityDisplacement(mobile, ORIGIN, ORIGIN.translated(0), null);
        assertEquals(ORIGIN, mobile.getPosition());
        assertFalse(megamek.common.compute.Compute.isValidDisplacement(manager.getGame(), mobile.getId(),
              ORIGIN, ORIGIN.translated(0)));
        var tall = new BipedMek();
        tall.setWeight(150);
        tall.setId(2);
        tall.setOwner(mobile.getOwner());
        manager.getGame().addEntity(tall);
        tall.setPosition(ORIGIN.translated(0));
        assertSame(mobile, MobileStructureMovement.displacementObstacle(tall, tall.getPosition(), ORIGIN));
        assertFalse(megamek.common.compute.Compute.isValidDisplacement(manager.getGame(), tall.getId(), tall.getPosition(), ORIGIN));
    }

    @ParameterizedTest
    @CsvSource({ "TRACKED,true", "WHEELED,false", "HOVER,false" })
    void forcedDisplacementBelowTheUndercarriageUsesActualClearance(EntityMovementMode mode, boolean allowed) {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        var tank = new Tank();
        tank.setId(2);
        tank.setOwner(mobile.getOwner());
        tank.setWeight(50);
        tank.setMovementMode(mode);
        tank.setOriginalWalkMP(4);
        tank.setDeployed(true);
        manager.getGame().addEntity(tank);
        tank.setPosition(ORIGIN.translated(0));
        assertEquals(allowed, MobileStructureMovement.displacementObstacle(tank, tank.getPosition(), ORIGIN) == null);
        manager.doEntityDisplacement(tank, tank.getPosition(), ORIGIN, null);
        assertEquals(allowed, ORIGIN.equals(tank.getPosition()));
        assertEquals(150, mobile.getCurrentCF(ORIGIN), "a unit below the body never passes through its walls");
        if (allowed) {
            assertEquals(0, tank.getElevation());
            verify(manager, never()).damageEntity(eq(tank), any(megamek.common.HitData.class), anyInt());
        }
    }

    @Test void mobileAccidentalFallsDamageEveryHexRatherThanTheDummyLocation() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        manager.doEntityFall(mobile, ORIGIN, 3, 0, mobile.getBasePilotingRoll(), false, false);
        assertEquals(135, mobile.getCurrentCF(ORIGIN), "30 standard damage uses fortress scaling");
        assertEquals(135, mobile.getCurrentCF(mobile.relativeToBoard(EAST)));
        assertFalse(mobile.isProne());
    }

    private BipedMek cargo(TWGameManager manager, MobileStructure mobile) {
        var mek = new BipedMek();
        mek.setChassis("Cargo");
        mek.setModel("Test");
        mek.setId(2);
        mek.setWeight(50);
        mek.setOriginalWalkMP(4);
        mek.setOwner(mobile.getOwner());
        for (int loc = 0; loc < mek.locations(); loc++) {
            mek.initializeInternal(100, loc);
            mek.initializeArmor(500, loc);
        }
        manager.getGame().addEntity(mek);
        mobile.addTransporter(new megamek.common.bays.MekBay(2, 2, 1));
        mobile.load(mek, false, 1);
        mek.setTransportId(mobile.getId());
        mek.setLoadedThisTurn(false);
        return mek;
    }

    @Test void placedBayDoorsAllowUnderbodyExitAndRejectRemoteOrDamagedDoorRequests() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        var mek = cargo(manager, mobile);
        mobile.getDesign().getBayDoors().add(new BuildingDesign.BayDoor(1, new BuildingDesign.Position(EAST, 0), 1));
        Coords inside = mobile.relativeToBoard(EAST);
        assertNotNull(MobileStructureCargoRules.exit(mobile, mek, inside));
        assertNull(MobileStructureCargoRules.exit(mobile, mek, ORIGIN.translated(0, 5)));
        assertFalse(manager.unloadUnit(mobile, mek, ORIGIN.translated(0, 5), 0, 0));
        assertEquals(mobile.getId(), mek.getTransportId());
        mobile.getBay(mek).setCurrentDoors(0);
        assertTrue(MobileStructureCargoRules.exits(mobile, mek).isEmpty());
        assertEquals(mobile.getId(), mek.getTransportId());
    }

    @Test void cargoExitsUsePredictedMovedFootprintAndAnOffMapOriginIsHarmless() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST, EAST.add(EAST)));
        var manager = manager(mobile, 0);
        var mek = cargo(manager, mobile);
        var pose = new megamek.common.moves.MobileStructureLinkage.Pose(new Coords(-1, 5), 0, 0);
        var exits = MobileStructureCargoRules.exits(mobile, mek, pose);
        assertFalse(exits.isEmpty());
        assertTrue(exits.stream().allMatch(exit -> exit.position().getX() >= 0 && exit.position().getX() <= 2));
        assertEquals(ORIGIN, mobile.getPosition(), "preview must not mutate deployed state");
        new MobileStructureMovementHandler(manager).relocate(mobile, pose.position(), pose.facing(), pose.elevation());
        assertEquals(exits, MobileStructureCargoRules.exits(mobile, mek));
    }

    @Test void movementHandlerActuallyUnloadsCargoAfterMoving() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        var mek = cargo(manager, mobile);
        var destination = ORIGIN.translated(0);
        var path = new MovePath(manager.getGame(), mobile).addStep(MoveStepType.FORWARDS)
              .addStep(MoveStepType.UNLOAD, mek, destination);
        assertTrue(path.getLastStep().isLegal(path));
        new MobileStructureMovementHandler(manager).process(mobile, path);
        assertEquals(destination, mobile.getPosition());
        assertEquals(destination, mek.getPosition());
        assertEquals(Entity.NONE, mek.getTransportId());
        assertEquals(0, mek.getElevation());
        assertEquals(4, mobile.mpUsed, "unloading consumes the passenger's half movement, not another carrier MP");
        assertEquals(2, mek.mpUsed);
    }

    @Test void sinkingCargoUsesDeckDepthAndInactiveUnitsCannotEscape() {
        var mobile = unit(4, List.of(CubeCoords.ZERO, EAST));
        mobile.setMovementMode(EntityMovementMode.NAVAL);
        var manager = manager(mobile, 10);
        var mek = cargo(manager, mobile);
        mobile.getNavalState().beginSinking(0, 1);
        mobile.setElevation(-3);
        var exits = MobileStructureCargoRules.exits(mobile, mek);
        assertFalse(exits.isEmpty());
        assertTrue(exits.stream().allMatch(exit -> exit.deckElevation() == -1 && exit.elevation() == -10 && exit.fall()));
        mek.setManualShutdown(true);
        assertTrue(MobileStructureCargoRules.exits(mobile, mek).isEmpty());
        mek.setManualShutdown(false);
        mobile.getBay(mek).setCurrentDoors(0);
        assertTrue(MobileStructureCargoRules.exits(mobile, mek).isEmpty());
    }

    @Test void sinkingPreservesCargoUntilLastHexAndOriginalEvacuationFootprintSurvives() {
        var mobile = unit(4, List.of(CubeCoords.ZERO, EAST));
        mobile.setMovementMode(EntityMovementMode.NAVAL);
        var manager = manager(mobile, 20);
        var mek = cargo(manager, mobile);
        var before = MobileStructureCargoRules.evacuationPositions(mobile);
        manager.destroyEntity(mobile, "sinking cargo test", true);
        assertEquals(mobile.getId(), mek.getTransportId());
        assertFalse(mek.isDestroyed());
        for (Coords hex : List.copyOf(mobile.getCoordsList())) { mobile.removeHex(hex); }
        assertEquals(before, MobileStructureCargoRules.evacuationPositions(mobile));
        mobile.getNavalState().finishSinking();
        manager.destroyEntity(mobile, "sunk", true);
        assertTrue(mek.isDestroyed(), "non-infantry cargo follows destroyed DropShip rules after final sinking");
    }

    @Test void sinkingGroundCargoUsesCapturedPhysicalDoorHeightsAndTheCarriersBoard() {
        var mobile = unit(4, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 10);
        var mek = cargo(manager, mobile);
        mobile.getNavalState().beginSinking(0, 1);
        mobile.getNavalState().getBaseOffsets().put(CubeCoords.ZERO, 5);
        mobile.getNavalState().getBaseOffsets().put(EAST, 6);
        mobile.setElevation(-7);
        mobile.getDesign().getBayDoors().add(new BuildingDesign.BayDoor(1, new BuildingDesign.Position(EAST, 0), 1));
        mek.setBoardId(99);
        var exits = MobileStructureCargoRules.exits(mobile, mek);
        assertFalse(exits.isEmpty(), "carried unit's previous board is irrelevant");
        assertTrue(exits.stream().allMatch(exit -> exit.deckElevation() == 3));
    }

    @Test void collisionSubmersionUsesImpactElevationAndIgnoresOffMapHexes() {
        var mobile = unit(4, List.of(CubeCoords.ZERO, EAST, EAST.add(EAST)));
        mobile.setMovementMode(EntityMovementMode.SUBMARINE);
        var manager = manager(mobile, 10);
        mobile.setElevation(-3);
        assertTrue(MobileStructureCollisionHandler.fullyUnderwater(mobile));
        assertFalse(MobileStructureCollisionHandler.fullyUnderwater(mobile, ORIGIN, 0, -2),
              "a roof at the water surface is not fully underwater");
        assertTrue(MobileStructureCollisionHandler.fullyUnderwater(mobile, new Coords(-1, 5), 0, -3));
    }

    @Test void alreadyWalkingCargoDoesNotInvokeTheUnrelatedZiplinePath() {
        var mobile = unit(3, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(mobile, 0);
        var mek = cargo(manager, mobile);
        mek.setOriginalWalkMP(5);
        mek.moved = EntityMovementType.MOVE_WALK;
        assertTrue(manager.unloadUnit(mobile, mek, ORIGIN, 0, 0));
        assertEquals(Entity.NONE, mek.getTransportId());
        assertEquals(ORIGIN, mek.getPosition());
        assertEquals(3, mek.mpUsed, "TW p.89 rounds the half-MP dismount cost up");
    }

    @Test void navalJumpDismountOffersOnlyTheCheapestReachableLand() throws Exception {
        var mobile = unit(4, List.of(CubeCoords.ZERO, EAST));
        mobile.setMovementMode(EntityMovementMode.NAVAL);
        var manager = manager(mobile, 10);
        var mek = cargo(manager, mobile);
        mek.setOriginalJumpMP(4);
        for (int jet = 0; jet < 4; jet++) {
            mek.addEquipment(EquipmentType.get("Jump Jet"), Mek.LOC_RIGHT_TORSO);
        }
        assertEquals(4, mek.getJumpMP(), "dismounting requires working jump jets, not just the design's MP field");
        mobile.getDesign().getBayDoors().add(new BuildingDesign.BayDoor(1, new BuildingDesign.Position(EAST, 2), 1));
        Coords near = mobile.relativeToBoard(EAST).translated(1, 2);
        Coords far = mobile.relativeToBoard(EAST).translated(1, 3);
        manager.getGame().getBoard().getHex(near).removeTerrain(Terrains.WATER);
        manager.getGame().getBoard().getHex(far).removeTerrain(Terrains.WATER);
        var exits = MobileStructureCargoRules.exits(mobile, mek);
        assertNotNull(MobileStructureCargoRules.exit(mobile, mek, near));
        assertNull(MobileStructureCargoRules.exit(mobile, mek, far));
        assertTrue(exits.stream().allMatch(exit -> exit.movement() == EntityMovementType.MOVE_JUMP));
        assertTrue(manager.unloadUnit(mobile, mek, near, 0, 0));
        assertEquals(EntityMovementType.MOVE_JUMP, mek.moved);
        assertEquals(0, mek.delta_distance);
    }
}
