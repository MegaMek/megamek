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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Vector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.BLKStructureFile;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.units.*;
import megamek.common.verifier.TestBuilding;
import megamek.common.verifier.TestXMLOption;
import megamek.common.util.SerializationHelper;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.common.weapons.handlers.DamageFalloff;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

/** Rules and persistence regressions found during the building release review. */
class BuildingRulesRegressionTest {
    private static final Coords ORIGIN = new Coords(8, 8);
    private static final CubeCoords EAST = new CubeCoords(1, 0, -1);

    private static class ReviewGameManager extends TWGameManager {
        @Override
        public void send(Packet packet) { }
        @Override
        public void send(int connectionId, Packet packet) { }
    }

    @BeforeAll
    static void equipment() { EquipmentType.initializeTypes(); }

    private AbstractBuildingEntity structure(boolean mobile, int height, List<CubeCoords> hexes) {
        AbstractBuildingEntity unit = mobile ? new MobileStructure(BuildingType.MEDIUM, IBuilding.FORTRESS)
              : new BuildingEntity(BuildingType.MEDIUM, IBuilding.FORTRESS);
        unit.setChassis("Release Review");
        unit.setModel("Probe");
        unit.setYear(3145);
        unit.setTechLevel(TechConstants.T_IS_ADVANCED);
        if (!mobile) { unit.setEngine(new Engine(0, Engine.NONE, 0)); }
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.FORTRESS, height, 40, 0, hexes);
        return unit;
    }

    private TWGameManager manager(AbstractBuildingEntity unit) {
        TWGameManager manager = spy(new ReviewGameManager());
        doNothing().when(manager).send(any(Packet.class));
        doNothing().when(manager).sendChangedHex(any(Coords.class), any(int.class));
        doNothing().when(manager).entityUpdate(any(int.class));
        doNothing().when(manager).sendChangedBuildings(any());
        Game game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        Player owner = new Player(0, "Review");
        game.addPlayer(0, owner);
        unit.setOwner(owner);
        unit.setId(1);
        game.addEntity(unit);
        unit.setPosition(ORIGIN);
        unit.updateBuildingEntityHexes(0, manager);
        return manager;
    }

    @Test
    void mobileStructureSurvivesBlkRoundTrip() throws Exception {
        var unit = (MobileStructure) structure(true, 3, List.of(CubeCoords.ZERO, EAST));
        unit.setMaximumMP(1.25);
        unit.setOperatingRange(432.5);
        unit.setCrewCount(100);
        unit.getInternalBuilding().setHeight(2, EAST);
        var loaded = new BLKStructureFile(BLKFile.getBlock(unit)).getEntity();
        assertInstanceOf(MobileStructure.class, loaded, "saving must preserve Mobile Structure identity");
        var mobile = (MobileStructure) loaded;
        assertEquals(1.25, mobile.getMaximumMP());
        assertEquals(432.5, mobile.getOperatingRange());
        assertEquals(unit.getMovementMode(), mobile.getMovementMode());
        assertEquals(unit.getPowerSystem(), mobile.getPowerSystem());
        assertEquals(2, mobile.getInternalBuilding().getHeight(EAST));
        assertEquals(100, mobile.getNCrew());
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> mobileDesigns() {
        return java.util.stream.Stream.of(IBuilding.STANDARD, IBuilding.HANGAR, IBuilding.FORTRESS)
              .flatMap(classification -> java.util.stream.Stream.of(BuildingType.LIGHT, BuildingType.MEDIUM,
                    BuildingType.HEAVY, BuildingType.HARDENED)
                    .filter(type -> TestBuilding.limits(type, classification) != null)
                    .flatMap(type -> java.util.stream.Stream.of(EntityMovementMode.TRACKED, EntityMovementMode.NAVAL,
                                EntityMovementMode.SUBMARINE, EntityMovementMode.VTOL)
                          .filter(mode -> classification != IBuilding.FORTRESS || mode != EntityMovementMode.VTOL)
                          .map(mode -> org.junit.jupiter.params.provider.Arguments.of(classification, type, mode))));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("mobileDesigns")
    void supportedMobileClassesAndMotivesReopenWithTheirOwnConstructionRules(int classification, BuildingType type,
          EntityMovementMode mode) throws Exception {
        var unit = (MobileStructure) structure(true, 3, List.of(CubeCoords.ZERO, EAST));
        int cf = TestBuilding.limits(unit, type, classification).maximumCF();
        unit.configureConstruction(type, classification, 3, cf, 0, List.of(CubeCoords.ZERO, EAST));
        unit.setMovementMode(mode);
        unit.setMaximumMP(.25);
        unit.getDesign().setEnvironmentalSealing(mode == EntityMovementMode.SUBMARINE);
        unit.getInternalBuilding().setHeight(2, EAST);
        var loaded = new BLKStructureFile(BLKFile.getBlock(unit)).getEntity();
        assertInstanceOf(MobileStructure.class, loaded);
        var mobile = (MobileStructure) loaded;
        assertEquals(type, mobile.getBuildingType());
        assertEquals(classification, mobile.getBldgClass());
        assertEquals(mode, mobile.getMovementMode());
        assertEquals(.25, mobile.getMaximumMP());
        assertEquals(2, mobile.getInternalBuilding().getHeight(EAST));
        assertEquals(List.of(), new TestBuilding(mobile, new TestXMLOption(), "").constructionIssues());
    }

    @Test
    void mobileMinimumFootprintIsEnforced() {
        var unit = structure(true, 1, List.of(CubeCoords.ZERO));
        assertFalse(new TestBuilding(unit, new TestXMLOption(), "").correctEntity(new StringBuffer(), unit.getTechLevel()),
              "TO:AUE requires at least two hexes; required crew quarters are supplied free");
    }

    @Test
    void mobileQuarterBudgetPaysFourQuartersForOneClearHex() {
        var unit = (MobileStructure) structure(true, 1, List.of(CubeCoords.ZERO, EAST));
        unit.setMaximumMP(1);
        var manager = manager(unit);
        var path = new MovePath(manager.getGame(), unit);
        path.addStep(MoveStepType.FORWARDS);
        assertEquals(unit.getWalkMP(), path.getMpUsed(), "one clear hex must exhaust a 1-MP structure's budget");
    }

    @ParameterizedTest
    @ValueSource(doubles = { .25, .5, .75, 1 })
    void mobileServerWaitsUntilAFullHexHasBeenPaid(double speed) throws Exception {
        var unit = (MobileStructure) structure(true, 1, List.of(CubeCoords.ZERO, EAST));
        unit.setMaximumMP(speed);
        var manager = manager(unit);
        int turns = (int) Math.ceil(1 / speed);
        for (int turn = 1; turn <= turns; turn++) {
            unit.newRound(turn);
            var path = new MovePath(manager.getGame(), unit).addStep(MoveStepType.FORWARDS);
            new MovePathHandler(manager, unit, path, null).processMovement();
            if (turn < turns) {
                assertEquals(ORIGIN, unit.getPosition(), "partial MP must not move the footprint");
                assertEquals(unit.getMovementProgress(), roundTrip(unit).getMovementProgress());
                assertEquals(turn * (int) (speed * 4), unit.getMovementProgress().quarters());
            }
        }
        assertEquals(ORIGIN.translated(0), unit.getPosition());
        assertNull(unit.getMovementProgress());
        assertEquals(EntityMovementType.MOVE_RUN, unit.moved);
        assertSame(unit, manager.getGame().getBuildingAt(unit.getPosition(), 0).orElseThrow());
    }

    @Test
    void mobilePreviewRetainsRemainingMPAfterCompletingAPreviousTurn() {
        var unit = (MobileStructure) structure(true, 1, List.of(CubeCoords.ZERO, EAST));
        unit.setMaximumMP(.75);
        var manager = manager(unit);
        new MovePathHandler(manager, unit, new MovePath(manager.getGame(), unit).addStep(MoveStepType.FORWARDS), null)
              .processMovement();
        unit.newRound(1);
        var path = new MovePath(manager.getGame(), unit).addStep(MoveStepType.FORWARDS).addStep(MoveStepType.FORWARDS);
        assertEquals(1, path.getStepVector().getFirst().getMp(), "only one quarter remains on the previous maneuver");
        assertNotEquals(EntityMovementType.MOVE_ILLEGAL, path.getLastStep().getMovementType(false));
        new MovePathHandler(manager, unit, path, null).processMovement();
        assertEquals(ORIGIN.translated(0), unit.getPosition());
        assertEquals(2, unit.getMovementProgress().quarters(), "the remaining half MP starts the following hex");
    }

    @Test
    void mobileCollisionDamagesAndDisplacesWithoutOverwritingEitherFootprint() {
        var unit = (MobileStructure) structure(true, 3, List.of(CubeCoords.ZERO, EAST, EAST.add(EAST)));
        unit.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 3, 150, 0,
              List.of(CubeCoords.ZERO, EAST, EAST.add(EAST)));
        unit.setMaximumMP(1);
        var manager = manager(unit);
        var game = manager.getGame();
        var other = (MobileStructure) structure(true, 3, List.of(CubeCoords.ZERO, EAST));
        other.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 3, 150, 0, List.of(CubeCoords.ZERO, EAST));
        other.setDone(true);
        other.setId(2);
        other.setOwner(unit.getOwner());
        game.addEntity(other);
        other.setPosition(ORIGIN.translated(0));
        other.updateBuildingEntityHexes(0, manager);
        new MovePathHandler(manager, unit, new MovePath(game, unit).addStep(MoveStepType.FORWARDS), null)
              .processMovement();
        assertEquals(ORIGIN.translated(0), unit.getPosition());
        assertNotEquals(ORIGIN.translated(0), other.getPosition());
        assertEquals(4, unit.mpUsed, "a collision is a paid movement attempt");
        assertTrue(unit.getCoordsList().stream().noneMatch(other.getCoordsList()::contains));
        assertTrue(other.getCoordsList().stream().anyMatch(hex -> other.getCurrentCF(hex) < 150));
        for (var structure : List.of(unit, other)) {
            for (Coords hex : structure.getCoordsList()) {
                assertSame(structure, game.getBuildingAt(hex, 0).orElseThrow());
            }
        }
    }

    private static <T extends Serializable> T roundTrip(T object) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(object);
        }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            @SuppressWarnings("unchecked") T copy = (T) input.readObject();
            return copy;
        }
    }

    @Test
    void actualSaveGameRestoresAuthoredRecordsAndUnfinishedMobileMovement() throws Exception {
        var mobile = (MobileStructure) structure(true, 3, List.of(CubeCoords.ZERO, EAST));
        mobile.getDesign().getDoors().add(new BuildingDesign.Door(new BuildingDesign.Position(EAST, 1), 2, 2));
        mobile.getDesign().getElevators().add(new BuildingDesign.Elevator(EAST, 20.5, Map.of(0, 4, 1, 4, 2, 4)));
        mobile.setMaximumMP(.25);
        mobile.advanceMovement(ORIGIN.translated(0), 0, 0, 4, 1);
        var weapon = mobile.addEquipment(new ISLaserMedium(), 0);
        mobile.getDesign().getEquipmentSpace().put(weapon, List.of(new BuildingDesign.Position(CubeCoords.ZERO, 0)));
        String xml = SerializationHelper.getSaveGameXStream().toXML(mobile);
        var restored = (MobileStructure) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
        assertEquals(mobile.getMovementProgress(), restored.getMovementProgress());
        assertEquals(mobile.getDesign().getDoors(), restored.getDesign().getDoors());
        assertEquals(mobile.getDesign().getElevators(), restored.getDesign().getElevators());
        assertEquals(mobile.getDesign().getEquipmentSpace().get(weapon),
              restored.getDesign().getEquipmentSpace().get(restored.getEquipment().getFirst()));
        assertEquals(.25, restored.getMaximumMP());
    }

    @Test
    void doorsRequireEndPhaseControlAndCanOnlyChangeOncePerTurn() throws Exception {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        var door = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 0, 2);
        unit.getDesign().getDoors().add(door);
        var manager = manager(unit);
        var game = manager.getGame();
        game.addPlayer(1, new Player(1, "Opponent"));
        game.setPhase(megamek.common.enums.GamePhase.MOVEMENT);
        assertFalse(manager.changeBuildingDoor(0, unit.getId(), 0, true));
        game.setPhase(megamek.common.enums.GamePhase.END);
        assertFalse(manager.changeBuildingDoor(1, unit.getId(), 0, true));
        assertFalse(manager.changeBuildingDoor(0, unit.getId(), -1, true));
        assertTrue(manager.changeBuildingDoor(0, unit.getId(), 0, true));
        assertFalse(manager.changeBuildingDoor(0, unit.getId(), 0, false));
        String saved = SerializationHelper.getSaveGameXStream().toXML(unit);
        var loaded = (BuildingEntity) SerializationHelper.getLoadSaveGameXStream().fromXML(saved);
        assertTrue(loaded.getBuildingRuntimeState().isDoorOpen(loaded.getDesign().getDoors().getFirst()));
        var received = roundTrip(unit);
        game.setEntity(received.getId(), received);
        assertSame(received, game.getBuildingAt(ORIGIN, 0).orElseThrow());
        assertSame(received, game.getBoard(0).getBuildingsVector().getFirst());
        game.setRoundCount(game.getRoundCount() + 1);
        assertTrue(manager.changeBuildingDoor(0, unit.getId(), 0, false));
    }

    @Test
    void capturingDoorControlsRequiresInfantryInsideForTheWholeTurn() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        var door = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 0, 2);
        unit.getDesign().getDoors().add(door);
        var manager = manager(unit);
        var game = manager.getGame();
        var enemy = new Player(1, "Opponent");
        game.addPlayer(1, enemy);
        var infantry = new ConvInfantry();
        infantry.setId(2);
        infantry.setOwner(enemy);
        infantry.setPosition(ORIGIN);
        infantry.setDeployed(true);
        game.addEntity(infantry);
        game.setPhase(megamek.common.enums.GamePhase.END);
        assertFalse(manager.changeBuildingDoor(1, unit.getId(), 0, true), "arriving this turn is insufficient");
        unit.newRound(1);
        infantry.delta_distance = 1;
        assertFalse(manager.changeBuildingDoor(1, unit.getId(), 0, true), "leaving and returning does not capture controls");
        infantry.delta_distance = 0;
        assertTrue(manager.changeBuildingDoor(1, unit.getId(), 0, true));
        game.setRoundCount(2);
        assertFalse(manager.changeBuildingDoor(0, unit.getId(), 0, false), "the former owner lost these controls");
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2 })
    void openDoorsMustFitTheUnitAndAvoidWallDamage(int height) {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        var door = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 0, height);
        unit.getDesign().getDoors().add(door);
        var manager = manager(unit);
        manager.getGame().setPhase(megamek.common.enums.GamePhase.END);
        assertTrue(manager.changeBuildingDoor(0, unit.getId(), 0, true));
        var mek = new BipedMek();
        mek.setWeight(50);
        mek.setOwner(unit.getOwner());
        mek.setId(2);
        mek.setPosition(ORIGIN.translated(0));
        manager.getGame().addEntity(mek);
        assertEquals(height >= 2, unit.getBuildingRuntimeState().openPassage(unit, mek, mek.getPosition(), ORIGIN, 0));
        assertTrue(unit.getBuildingRuntimeState().openPassage(unit, new Tank(), mek.getPosition(), ORIGIN, 0));
        if (height >= 2) {
            manager.passBuildingWall(mek, unit, mek.getPosition(), ORIGIN, 1, "", false,
                  EntityMovementType.MOVE_WALK, true, new Vector<>());
            assertEquals(40, unit.getCurrentCF(ORIGIN));
            verify(manager, never()).damageEntity(eq(mek), any(megamek.common.HitData.class), any(int.class));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 9, 10, 11 })
    void sealedBuildingChecksBreachAtTenActualCFPerHit(int damage) {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO, EAST));
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 3, 40, 0, List.of(CubeCoords.ZERO, EAST));
        unit.getDesign().setEnvironmentalSealing(true);
        var manager = manager(unit);
        try (var dice = mockStatic(megamek.common.compute.Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(10);
            manager.damageBuilding(unit, damage, ORIGIN, 1);
        }
        assertEquals(damage >= 10, unit.getBuildingRuntimeState().isBreached(CubeCoords.ZERO));
        assertEquals(damage >= 10, unit.getBuildingRuntimeState().isBreached(EAST));
    }

    @Test
    void armorAbsorbedHitsDoNotBreachTheEnvironmentalSeal() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.getDesign().setEnvironmentalSealing(true);
        var manager = manager(unit);
        unit.setArmor(20, ORIGIN);
        try (var dice = mockStatic(megamek.common.compute.Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(12);
            manager.damageBuilding(unit, 11, ORIGIN, 0);
        }
        assertFalse(unit.getBuildingRuntimeState().isBreached(CubeCoords.ZERO));
        assertEquals(40, unit.getCurrentCF(ORIGIN));
    }

    @Test
    void underwaterBreachDisablesElevatorAndFloodsDownOneFloorPerSubsequentTurn() throws Exception {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO, EAST));
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 3, 40, 0, List.of(CubeCoords.ZERO, EAST));
        unit.getDesign().setSite(BuildingDesign.Site.UNDERWATER);
        unit.getDesign().setEnvironmentalSealing(true);
        unit.getDesign().getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 20, Map.of(0, 4, 1, 4, 2, 4)));
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        try (var dice = mockStatic(megamek.common.compute.Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(10);
            manager.damageBuilding(unit, 10, ORIGIN, 2);
        }
        assertFalse(manager.getGame().getIndustrialElevator(ORIGIN, 0).isFunctional());
        var state = unit.getBuildingRuntimeState();
        assertTrue(state.isFlooded(CubeCoords.ZERO, 2));
        assertFalse(state.isFlooded(CubeCoords.ZERO, 1));
        new BuildingEnvironmentHandler(manager).endPhase(new Vector<>());
        assertFalse(state.isFlooded(CubeCoords.ZERO, 1), "no second flood advance on the breach turn");
        manager.getGame().setRoundCount(1);
        org.mockito.Mockito.clearInvocations(manager);
        new BuildingEnvironmentHandler(manager).endPhase(new Vector<>());
        verify(manager).entityUpdate(unit.getId());
        assertTrue(state.isFlooded(CubeCoords.ZERO, 1));
        assertFalse(state.isFlooded(CubeCoords.ZERO, 0));
        String xml = SerializationHelper.getSaveGameXStream().toXML(unit);
        var restored = (BuildingEntity) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
        assertTrue(restored.getBuildingRuntimeState().isFlooded(CubeCoords.ZERO, 1));
        manager.getGame().setRoundCount(2);
        new BuildingEnvironmentHandler(manager).endPhase(new Vector<>());
        assertTrue(state.isFlooded(CubeCoords.ZERO, 0));
    }

    @ParameterizedTest
    @CsvSource({ "false,0,false", "true,0,true", "true,2,false" })
    void floodingRequiresOpenConnectingDoorsAtTheSameAltitude(boolean secondOpen, int secondFloor, boolean floods) {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.getDesign().setSite(BuildingDesign.Site.UNDERWATER);
        unit.getDesign().setEnvironmentalSealing(true);
        var door = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 0, 1);
        unit.getDesign().getDoors().add(door);
        var manager = manager(unit);
        var game = manager.getGame();
        var neighbor = structure(false, 3, List.of(CubeCoords.ZERO));
        neighbor.setId(2);
        neighbor.setOwner(unit.getOwner());
        neighbor.getDesign().setSite(BuildingDesign.Site.UNDERWATER);
        var otherDoor = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, secondFloor), 3, 1);
        neighbor.getDesign().getDoors().add(otherDoor);
        game.addEntity(neighbor);
        neighbor.setPosition(ORIGIN.translated(0));
        neighbor.updateBuildingEntityHexes(0, manager);
        game.setPhase(megamek.common.enums.GamePhase.END);
        assertTrue(unit.getBuildingRuntimeState().changeDoor(unit, door, unit.getOwner(), true));
        if (secondOpen) {
            assertTrue(neighbor.getBuildingRuntimeState().changeDoor(neighbor, otherDoor, unit.getOwner(), true));
        }
        try (var dice = mockStatic(megamek.common.compute.Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(12);
            new BuildingEnvironmentHandler(manager).damage(unit, ORIGIN, 0, 10, new Vector<>());
        }
        assertEquals(floods, neighbor.getBuildingRuntimeState().isFlooded(CubeCoords.ZERO, 0));
    }

    @Test
    void losingAtmosphericSealingDoesNotBreachEquipmentAlreadyOutsideOnTheRoof() throws Exception {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.getDesign().setEnvironmentalSealing(true);
        var interior = unit.addEquipment(new ISLaserMedium(), 0);
        var roof = unit.addEquipment(new ISLaserMedium(), 0);
        unit.getDesign().getEquipmentSpace().put(roof, List.of(new BuildingDesign.Position(CubeCoords.ZERO, 3)));
        var manager = manager(unit);
        manager.getGame().getPlanetaryConditions().setAtmosphere(megamek.common.planetaryConditions.Atmosphere.VACUUM);
        try (var dice = mockStatic(megamek.common.compute.Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(12);
            new BuildingEnvironmentHandler(manager).damage(unit, ORIGIN, 0, 10, new Vector<>());
        }
        assertTrue(interior.isBreached());
        assertFalse(roof.isBreached());
    }

    @Test
    void castleBrianBreachesStayInTheStruckHex() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO, EAST));
        unit.configureConstruction(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN, 3, 40, 0, List.of(CubeCoords.ZERO, EAST));
        var manager = manager(unit);
        try (var dice = mockStatic(megamek.common.compute.Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(12);
            new BuildingEnvironmentHandler(manager).damage(unit, ORIGIN, 1, 10, new Vector<>());
        }
        assertTrue(unit.getBuildingRuntimeState().isBreached(CubeCoords.ZERO));
        assertFalse(unit.getBuildingRuntimeState().isBreached(EAST));
    }

    @Test
    void subsurfaceBreachUsesCumulativeDamageWithinOnePhase() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 3, 40, 0, List.of(CubeCoords.ZERO));
        unit.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        var manager = manager(unit);
        manager.getGame().setPhase(megamek.common.enums.GamePhase.FIRING);
        try (var dice = mockStatic(megamek.common.compute.Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.d6(2)).thenReturn(10);
            manager.damageBuilding(unit, 6, ORIGIN);
            assertFalse(unit.getBuildingRuntimeState().isBreached(CubeCoords.ZERO));
            manager.damageBuilding(unit, 4, ORIGIN);
        }
        assertTrue(unit.getBuildingRuntimeState().isBreached(CubeCoords.ZERO));
        assertEquals(0, unit.getCurrentCF(ORIGIN), "a breached underground hex collapses");
    }

    @Test
    void heavyMetalAffectsCrossingShotsProbesAndECMButNotItsOwnEquipment() throws Exception {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.getDesign().setHeavyMetal(true);
        var manager = manager(unit);
        var mek = new BipedMek();
        mek.setId(2);
        mek.setOwner(unit.getOwner());
        mek.setPosition(ORIGIN.translated(0, 4));
        manager.getGame().addEntity(mek);
        mek.addEquipment(megamek.common.equipment.MiscType.createBeagleActiveProbe(), Mek.LOC_CENTER_TORSO);
        mek.addEquipment(megamek.common.equipment.MiscType.createGECM(), Mek.LOC_CENTER_TORSO);
        assertTrue(mek.hasBAP(false));
        assertEquals(6, megamek.common.compute.ComputeECM.getECMInfo(mek).getRange());
        assertTrue(mek.isAffectedByEMI(ORIGIN.translated(3, 4)), "the line crosses a heavy-metal building");
        assertFalse(mek.isAffectedByEMI(ORIGIN.translated(0, 5)));
        assertFalse(unit.isAffectedByEMI(ORIGIN.translated(0, 4)), "the building's own equipment is exempt");
        mek.setPosition(ORIGIN.translated(0));
        assertFalse(mek.hasBAP(false));
        assertEquals(Entity.NONE, mek.getBAPRange());
        assertEquals(12, megamek.common.compute.ComputeECM.getECMInfo(mek).getRange());
    }

    @Test
    void intactSealedBuildingSheltersInfantryFromVacuum() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.getDesign().setEnvironmentalSealing(true);
        var manager = manager(unit);
        var infantry = new ConvInfantry();
        infantry.setId(2);
        infantry.setOwner(unit.getOwner());
        infantry.setPosition(ORIGIN);
        manager.getGame().addEntity(infantry);
        var conditions = manager.getGame().getPlanetaryConditions();
        conditions.setAtmosphere(megamek.common.planetaryConditions.Atmosphere.VACUUM);
        assertNull(conditions.whyDoomed(infantry, manager.getGame()));
        unit.getBuildingRuntimeState().breach(CubeCoords.ZERO);
        assertNotNull(conditions.whyDoomed(infantry, manager.getGame()));
    }

    @Test
    void openSpaceConstructionCannotIsolateDamageToOneFloor() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.configureConstruction(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN, 3, 40, 0, List.of(CubeCoords.ZERO));
        unit.getDesign().setOpenSpace(true);
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        manager.damageBuilding(unit, 20, ORIGIN, 2);
        assertFalse(unit.usesExpandedCF());
        assertTrue(unit.getCurrentCF(ORIGIN) < 40);
        assertEquals(unit.getCurrentCF(ORIGIN, 0), unit.getCurrentCF(ORIGIN, 2));
    }

    @Test
    void serverFloorDamageReachesClientBuilding() throws Exception {
        var clientUnit = structure(false, 3, List.of(CubeCoords.ZERO));
        var manager = manager(clientUnit);
        var serverUnit = structure(false, 3, List.of(CubeCoords.ZERO));
        serverUnit.setId(clientUnit.getId());
        serverUnit.setPosition(ORIGIN);
        serverUnit.getInternalBuilding().enableExpandedCF();
        serverUnit.setCurrentCF(36, ORIGIN, 1);
        serverUnit.getFloorState(ORIGIN).setPhaseCF(1, 38);
        serverUnit.getFloorState(ORIGIN).setArmor(2, 7);
        var received = roundTrip(serverUnit);
        manager.getGame().getBoard(0).updateBuilding(received);
        assertEquals(36, clientUnit.getCurrentCF(ORIGIN, 1), "BLDG_UPDATE must transmit the independently damaged floor");
        assertEquals(38, clientUnit.getFloorState(ORIGIN).getPhaseCF(1));
        assertEquals(7, clientUnit.getFloorState(ORIGIN).getArmor(2));
        received.setCurrentCF(1, ORIGIN, 1);
        assertEquals(36, clientUnit.getCurrentCF(ORIGIN, 1), "client snapshots must not alias the incoming packet");
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void onlyAMajorityOfHexesAtHalfHeightCollapsesTheWholeBuilding(int shortened) {
        var unit = structure(false, 4, List.of(CubeCoords.ZERO, EAST, new CubeCoords(0, 1, -1), new CubeCoords(1, -1, 0)));
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        unit.getInternalBuilding().enableExpandedCF();
        var coords = unit.getCoordsList();
        for (Coords hex : coords.subList(0, shortened)) {
            unit.setCurrentCF(0, hex, 2);
            unit.setCurrentCF(0, hex, 3);
        }
        manager.applyBuildingDamage();
        assertEquals(shortened > 2 ? 0 : 40, unit.getCurrentCF(coords.get(3)),
              "TO:AR requires more than half, so exactly two of four shortened hexes remain standing");
        manager.applyBuildingDamage();
        assertEquals(shortened > 2 ? 0 : 40, unit.getCurrentCF(coords.get(3)), "reprocessing must not repeat collapse effects");
        if (shortened > 2) {
            assertEquals(4, unit.getCollapsedHexCount());
            assertTrue(unit.getInternalBuilding().getOriginalCoordsList().stream()
                  .allMatch(hex -> unit.getInternalBuilding().getFloorState(hex).height() == 0));
        }
    }

    @Test
    void authoredElevatorIsAvailableAfterDeployment() {
        var unit = structure(false, 2, List.of(CubeCoords.ZERO, EAST));
        unit.getDesign().getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 20, Map.of(0, 4, 1, 4)));
        var manager = manager(unit);
        assertNotNull(manager.getGame().getIndustrialElevator(ORIGIN, 0),
              "TO:AR p.135: an authored elevator must be usable when its building is deployed");
    }

    @ParameterizedTest
    @CsvSource({ "false, false", "false, true", "true, false", "true, true" })
    void expandedCollapseIsIndependentOfExpandedCF(boolean collapse, boolean expandedCF) {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO, EAST, new CubeCoords(-1, 0, 1)));
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 3, 40, 0,
              unit.getInternalBuilding().getCoordsList());
        var manager = manager(unit);
        var game = manager.getGame();
        assertFalse(game.getOptions().booleanOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_COLLAPSE));
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_COLLAPSE).setValue(collapse);
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(expandedCF);
        if (expandedCF) {
            unit.getInternalBuilding().enableExpandedCF();
        }
        new BuildingCollapseHandler(manager).collapseBuilding(unit, game.getPositionMapMulti(),
              unit.relativeToBoard(EAST), false, new Vector<>());
        for (int level = 0; level < 3; level++) {
            assertEquals(collapse ? 20 : 40, unit.getCurrentCF(ORIGIN, level));
        }
    }

    @Test
    void connectedCollapseHalvesOnlyOncePerPhaseAndOnlyCorrespondingFloors() {
        var hexes = List.of(CubeCoords.ZERO, EAST, new CubeCoords(-1, 0, 1));
        var unit = structure(false, 3, hexes);
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 3, 40, 0, hexes);
        var manager = manager(unit);
        var game = manager.getGame();
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_COLLAPSE).setValue(true);
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        unit.getInternalBuilding().enableExpandedCF();
        var handler = new BuildingCollapseHandler(manager);
        for (CubeCoords hex : hexes.subList(1, 3)) {
            Coords coords = unit.relativeToBoard(hex);
            unit.setCurrentCF(0, coords, 2);
            handler.resolveExpandedCollapse(unit, coords, new Vector<>());
        }
        assertEquals(40, unit.getCurrentCF(ORIGIN, 0));
        assertEquals(40, unit.getCurrentCF(ORIGIN, 1));
        assertEquals(20, unit.getCurrentCF(ORIGIN, 2));
    }

    @ParameterizedTest
    @CsvSource({ "11, 0, 0", "12, 57, 0", "24, 114, 76" })
    void collapseDebrisUsesStartOfTurnCFAndFullTwelveLevelRadii(int height, int adjacent, int distant) {
        var unit = structure(false, height, List.of(CubeCoords.ZERO));
        unit.configureConstruction(BuildingType.HARDENED, IBuilding.STANDARD, height, 95, 0, List.of(CubeCoords.ZERO));
        var manager = manager(unit);
        var game = manager.getGame();
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_COLLAPSE).setValue(true);
        unit.getInternalBuilding().newRound();
        unit.setCurrentCF(0, ORIGIN);
        doReturn(new Vector<>()).when(manager).tryClearHex(any(Coords.class), any(int.class), any(int.class), any(int.class));
        new BuildingCollapseHandler(manager).checkForCollapse(unit, ORIGIN, true, new Vector<>());
        for (int distance = 1; distance <= 2; distance++) {
            Coords target = ORIGIN.translated(0, distance);
            int damage = distance == 1 ? adjacent : distant;
            if (damage == 0) {
                assertFalse(game.getBoard(0).getHex(target).containsTerrain(Terrains.RUBBLE));
                verify(manager, never()).tryClearHex(eq(target), eq(0), any(int.class), any(int.class));
            } else {
                assertTrue(game.getBoard(0).getHex(target).containsTerrain(Terrains.RUBBLE));
                verify(manager).tryClearHex(target, 0, damage, Entity.NONE);
            }
        }
    }

    @Test
    void tallHillBlocksCollapseDebris() {
        var unit = structure(false, 24, List.of(CubeCoords.ZERO));
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_COLLAPSE).setValue(true);
        manager.getGame().getBoard(0).getHex(ORIGIN.translated(0)).setLevel(12);
        doReturn(new Vector<>()).when(manager).tryClearHex(any(Coords.class), any(int.class), any(int.class), any(int.class));
        unit.setCurrentCF(0, ORIGIN);
        new BuildingCollapseHandler(manager).checkForCollapse(unit, ORIGIN, true, new Vector<>());
        assertFalse(manager.getGame().getBoard(0).getHex(ORIGIN.translated(0, 2)).containsTerrain(Terrains.RUBBLE));
        assertTrue(manager.getGame().getBoard(0).getHex(ORIGIN.translated(3, 2)).containsTerrain(Terrains.RUBBLE));
    }

    @Test
    void deployedElevatorsRetainFractionalCapacityAccessAndSeparateShaftsThroughSave() throws Exception {
        var unit = structure(false, 5, List.of(CubeCoords.ZERO));
        unit.getDesign().getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 20.5, Map.of(0, 4, 1, 4)));
        unit.getDesign().getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 40, Map.of(3, 8, 4, 8)));
        var manager = manager(unit);
        var location = megamek.common.board.BoardLocation.of(ORIGIN, 0);
        var first = manager.getGame().getIndustrialElevator(location, 0);
        first.setPlatformLevel(0);
        assertEquals(20.5, first.getCapacityTons());
        assertTrue(first.canAccess(0, 2));
        assertFalse(first.canAccess(0, 3));
        var restored = roundTrip(manager.getGame());
        assertEquals(2, restored.getIndustrialElevators().size());
        assertEquals(20.5, restored.getIndustrialElevator(location, 0).getCapacityTons());
        assertEquals(0, restored.getIndustrialElevator(location, 0).getPlatformLevel());
        assertEquals(40, restored.getIndustrialElevator(location, 4).getCapacityTons());
        assertNull(restored.getIndustrialElevator(location, 2));
        assertEquals(int.class, java.io.ObjectStreamClass.lookup(megamek.common.IndustrialElevator.class)
              .getField("capacityTons").getType(), "old Java saves retain their integer capacity field");
    }

    @Test
    void legacySingleElevatorRegistryMigratesWithoutResettingPlatform() throws Exception {
        Game legacy = new Game();
        var location = megamek.common.board.BoardLocation.of(ORIGIN, 0);
        var elevator = new megamek.common.IndustrialElevator(location, -2, 0, 50);
        elevator.setPlatformLevel(-1);
        var field = Game.class.getDeclaredField("industrialElevators");
        field.setAccessible(true);
        field.set(legacy, new java.util.concurrent.ConcurrentHashMap<>(Map.of(location, elevator)));
        Game restored = roundTrip(legacy);
        assertEquals(-1, restored.getIndustrialElevator(location).getPlatformLevel());
        assertEquals(50, restored.getIndustrialElevator(location).getCapacityTons());
    }

    @Test
    void automatedBuildingWeaponChoosesNearestEnemyAndUsesFixedGunnery() throws Exception {
        var unit = structure(false, 1, List.of(CubeCoords.ZERO));
        var weapon = (WeaponMounted) unit.addEquipment(new ISLaserMedium(), 0);
        unit.getDesign().getAutomatedWeapons().add(weapon);
        unit.getCrew().setGunnery(0, 0);
        var manager = manager(unit);
        manager.getGame().setPhase(megamek.common.enums.GamePhase.FIRING);
        Player enemy = new Player(1, "Enemy");
        manager.getGame().addPlayer(1, enemy);
        for (int distance : List.of(4, 2)) {
            var target = new BipedMek();
            target.setId(distance + 1);
            target.setOwner(enemy);
            target.setPosition(ORIGIN.translated(0, distance));
            target.setDeployed(true);
            manager.getGame().addEntity(target);
        }
        var action = manager.selectAutomatedBuildingAttack(unit, weapon);
        assertNotNull(action);
        assertEquals(3, action.getTargetId());
        assertTrue(action.toHit(manager.getGame()).getDesc().contains("Automated building weapon"));
        assertEquals(5, action.toHit(manager.getGame()).getValue());
        weapon.setJammed(true);
        assertNull(manager.selectAutomatedBuildingAttack(unit, weapon));
    }

    @Test
    void automatedWeaponIgnoresFriendsAndRandomizesEquallyNearEnemies() throws Exception {
        var unit = structure(false, 1, List.of(CubeCoords.ZERO));
        var weapon = (WeaponMounted) unit.addEquipment(new ISLaserMedium(), 0);
        unit.getDesign().getAutomatedWeapons().add(weapon);
        var manager = manager(unit);
        var game = manager.getGame();
        game.setPhase(megamek.common.enums.GamePhase.FIRING);
        Player enemy = new Player(1, "Enemy");
        game.addPlayer(1, enemy);
        for (int id : List.of(2, 3, 4)) {
            var target = new BipedMek();
            target.setId(id);
            target.setOwner(id == 2 ? unit.getOwner() : enemy);
            target.setPosition(ORIGIN.translated(id == 4 ? 1 : 0, id == 2 ? 1 : 2));
            target.setDeployed(true);
            game.addEntity(target);
        }
        try (var dice = mockStatic(megamek.common.compute.Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> megamek.common.compute.Compute.randomInt(2)).thenReturn(0, 1);
            var first = manager.selectAutomatedBuildingAttack(unit, weapon);
            var second = manager.selectAutomatedBuildingAttack(unit, weapon);
            assertNotNull(first);
            assertNotNull(second);
            assertEquals(java.util.Set.of(3, 4), java.util.Set.of(first.getTargetId(), second.getTargetId()));
        }
        game.getEntity(3).setDestroyed(true);
        game.getEntity(4).setDestroyed(true);
        assertNull(manager.selectAutomatedBuildingAttack(unit, weapon), "a nearby friendly is never a fallback target");
    }

    @Test
    void authoredBridgeDeckIsPublishedAsBridgeTerrain() {
        var unit = structure(false, 1, List.of(CubeCoords.ZERO, EAST));
        unit.configureConstruction(BuildingType.MEDIUM, IBuilding.BRIDGE, 1, 40, 0,
              List.of(CubeCoords.ZERO, EAST));
        unit.getDesign().getBridgeDecks().put(CubeCoords.ZERO, 3);
        unit.getDesign().getBridgeDecks().put(EAST, 3);
        var manager = manager(unit);
        assertEquals(3, manager.getGame().getBoard(0).getHex(ORIGIN).terrainLevel(Terrains.BRIDGE_ELEV),
              "an authored level-3 bridge must deploy a traversable level-3 deck");
    }

    @Test
    void mobileResultSixWithHighSubrollJamsTheTurretInsteadOfTheGun() throws Exception {
        var unit = (MobileStructure) structure(true, 1, List.of(CubeCoords.ZERO, EAST));
        WeaponMounted gun = new WeaponMounted(unit, new ISLaserMedium());
        gun.setSponsonTurretMounted(true);
        unit.addEquipment(gun, 0, false);
        var manager = manager(unit);
        new BuildingEntityCriticalHandler(manager).applyCriticalResult(unit, ORIGIN, 6, 6);
        assertTrue(unit.isTurretJammed(gun), "TO:AUE p.40: 6 with a 4-6 subroll is a turret jam");
        assertFalse(gun.isJammed());
    }

    @Test
    void artilleryAtGroundLevelOnAHillHitsTheLowestFloor() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        manager.getGame().getBoard(0).getHex(ORIGIN).setLevel(5);
        var killer = new BipedMek();
        killer.setId(2);
        manager.artilleryDamageHex(ORIGIN, 0, ORIGIN, 1, null, killer.getId(), killer, null,
              false, 5, 5, new Vector<>(), false, new Vector<>(), false, new DamageFalloff());
        assertEquals(39, unit.getCurrentCF(ORIGIN, 0),
              "absolute blast elevation 5 on level-5 terrain is relative building floor 0");
        assertEquals(40, unit.getCurrentCF(ORIGIN, 2));
    }

    @Test
    void expandedCastleBrianBlastDamagesEveryAffectedFloor() {
        var unit = structure(false, 3, List.of(CubeCoords.ZERO));
        unit.configureConstruction(BuildingType.HEAVY, IBuilding.CASTLE_BRIAN, 3, 40, 0,
              List.of(CubeCoords.ZERO));
        var manager = manager(unit);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(true);
        var killer = new BipedMek();
        killer.setId(2);
        var damagedHexes = new HashSet<megamek.common.board.BoardLocation>();
        for (int level = 0; level < 2; level++) {
            manager.artilleryDamageHex(ORIGIN, 0, ORIGIN, 20, null, killer.getId(), killer, null,
                  false, level, 0, new Vector<>(), false, new Vector<>(), false, new DamageFalloff(), damagedHexes);
        }
        assertTrue(unit.getCurrentCF(ORIGIN, 0) < 40, "the blast damages the first affected floor");
        assertTrue(unit.getCurrentCF(ORIGIN, 1) < 40,
              "Expanded CF tracks each affected Castle Brian floor separately; whole-hex deduplication drops upper hits");
    }
}
