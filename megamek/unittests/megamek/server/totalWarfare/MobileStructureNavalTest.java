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

import java.io.*;
import java.util.List;
import java.util.Vector;

import megamek.common.Player;
import megamek.utils.BoardLoader;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.units.*;
import megamek.common.util.SerializationHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** TO:AUE pp.27–29, 40–41: exercise actual phase processing and persisted naval state. */
class MobileStructureNavalTest {
    private static final Coords ORIGIN = new Coords(6, 6);
    private static class LocalManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }
    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private TWGameManager manager(int depth, int height, EntityMovementMode mode) {
        var manager = spy(new LocalManager());
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        var game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 16 16\nend\n"));
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
            if (depth > 0) game.getBoard().getHex(new Coords(x,y)).addTerrain(new Terrain(Terrains.WATER, depth));
        }
        game.addPlayer(0, new Player(0, "Owner"));
        game.setRoundCount(1);
        var mobile = new MobileStructure(BuildingType.HARDENED, IBuilding.FORTRESS);
        mobile.setId(1);
        mobile.setOwner(game.getPlayer(0));
        mobile.setChassis("Naval review");
        mobile.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, height, 150, 0, List.of(CubeCoords.ZERO));
        mobile.setMovementMode(mode);
        game.addEntity(mobile);
        mobile.setPosition(ORIGIN);
        mobile.setDeployed(true);
        mobile.updateBuildingEntityHexes(0, manager);
        return manager;
    }
    private MobileStructure mobile(TWGameManager manager) { return (MobileStructure) manager.getGame().getEntity(1); }
    private Tank vessel(TWGameManager manager, int elevation) {
        var ship = spy(new Tank());
        doReturn(new megamek.common.HitData(Tank.LOC_FRONT)).when(ship).rollHitLocation(anyInt(), anyInt());
        // Exercise collision damage and stranding without the independent random surface-hull breach roll.
        doReturn(new Vector<>()).when(manager).breachCheck(eq(ship), anyInt(), isNull(), anyBoolean());
        ship.setId(2);
        ship.setOwner(manager.getGame().getPlayer(0));
        ship.setMovementMode(EntityMovementMode.NAVAL);
        ship.setWeight(50);
        for(int loc = 0; loc < ship.locations(); loc++) { ship.initializeInternal(100,loc); ship.initializeArmor(500,loc); }
        ship.setPosition(ORIGIN);
        ship.setElevation(elevation);
        ship.setDone(true);
        ship.setDeployed(true);
        manager.getGame().addEntity(ship);
        return ship;
    }
    @Test void destructionPreservesHullUntilNextTurnsMovementThenRemovesOnlyAtTheBottom() {
        var manager = manager(10, 6, EntityMovementMode.NAVAL);
        var mobile = mobile(manager);
        var handler = new MobileStructureNavalHandler(manager);
        manager.destroyEntity(mobile, "test destruction", true);
        assertTrue(mobile.getNavalState().isSinking());
        assertFalse(mobile.isDestroyed());
        assertEquals(3, BuildingElevation.roof(mobile, ORIGIN));
        handler.endMovement();
        assertEquals(0, mobile.getElevation(), "destruction turn has no sinking movement");
        manager.getGame().setRoundCount(2);
        handler.endMovement();
        assertEquals(-5, mobile.getElevation());
        assertEquals(-2, BuildingElevation.roof(mobile, ORIGIN));
        assertEquals(1, mobile.getCoordsList().size());
        handler.endMovement();
        assertEquals(-5, mobile.getElevation(), "repeated phase processing cannot sink twice");
        manager.getGame().setRoundCount(3);
        handler.endMovement();
        assertTrue(mobile.getNavalState().isSettled());
        assertTrue(mobile.isDoomed() || mobile.isDestroyed());
        assertTrue(mobile.getCoordsList().isEmpty());
        assertNull(manager.getGame().getBoard().getBuildingAt(ORIGIN));
    }
    @Test void aGroundStructureAlreadyTouchingTheBedIsRemovedAtDestruction() {
        var manager = manager(5, 6, EntityMovementMode.TRACKED);
        var mobile = mobile(manager);
        assertEquals(-3, mobile.getBaseElevation(ORIGIN), "two-level undercarriage stands on the bed");
        manager.destroyEntity(mobile, "ground wreck", true);
        assertTrue(mobile.getNavalState().isSettled());
        assertTrue(mobile.isDoomed() || mobile.isDestroyed());
    }
    @Test void dryStructureUsesNormalImmediateDestruction() {
        var manager = manager(0, 3, EntityMovementMode.TRACKED);
        manager.destroyEntity(mobile(manager), "dry wreck", true);
        assertTrue(mobile(manager).isDoomed() || mobile(manager).isDestroyed());
        assertFalse(mobile(manager).getNavalState().isSinking());
    }
    @Test void strandedVesselOnlyFloatsFreeAtMovementEndWithRoofAtDepthOne() {
        var manager = manager(10, 4, EntityMovementMode.SUBMARINE);
        var mobile = mobile(manager);
        var ship = vessel(manager, 2);
        ship.setStuck(true);
        mobile.getNavalState().getStrandedVessels().put(ship.getId(), CubeCoords.ZERO);
        var movement = new MobileStructureMovementHandler(manager);
        var naval = new MobileStructureNavalHandler(manager);
        movement.relocate(mobile, ORIGIN, 0, -2);
        naval.endMovement();
        assertTrue(ship.isStuck(), "a surface-level roof has not reached depth one");
        movement.relocate(mobile, ORIGIN, 0, -3);
        assertTrue(ship.isStuck());
        naval.endMovement();
        assertFalse(ship.isStuck());
        assertEquals(0, ship.getElevation());
        assertTrue(mobile.getNavalState().getStrandedVessels().isEmpty());
        assertFalse(ship.isDestroyed());
    }
    @Test void destroyedSupportingHexReleasesVesselWithoutDamageInWaterButDestroysItOnLand() {
        var manager = manager(10, 4, EntityMovementMode.SUBMARINE);
        var mobile = mobile(manager);
        var ship = vessel(manager, 2);
        mobile.getNavalState().getStrandedVessels().put(2, CubeCoords.ZERO);
        ship.setStuck(true);
        var armor = ship.getTotalArmor();
        assertEquals(java.util.Set.of(2), new MobileStructureNavalHandler(manager).releaseVesselsInHex(mobile, ORIGIN));
        assertEquals(armor, ship.getTotalArmor());
        assertEquals(0, ship.getElevation());
        manager.getGame().getBoard().getHex(ORIGIN).removeTerrain(Terrains.WATER);
        mobile.getNavalState().getStrandedVessels().put(2, CubeCoords.ZERO);
        new MobileStructureNavalHandler(manager).releaseVesselsInHex(mobile, ORIGIN);
        assertTrue(ship.isDoomed() || ship.isDestroyed());
    }
    @Test void actualUpwardCollisionStrandsSmallSurfaceVessel() {
        var manager = manager(10, 4, EntityMovementMode.SUBMARINE);
        var mobile = mobile(manager);
        mobile.setElevation(-3);
        mobile.updateBuildingEntityHexes(0, manager);
        var ship = vessel(manager, 0);
        var step = new MovePath(manager.getGame(), mobile).addStep(MoveStepType.UP).getLastStep();
        assertTrue(new MobileStructureCollisionHandler(manager).resolve(mobile, step));
        assertFalse(ship.isDoomed() || ship.isDestroyed(), "the armored vessel survives the collision before stranding");
        assertEquals(450, ship.getArmor(Tank.LOC_FRONT));
        new MobileStructureMovementHandler(manager).relocate(mobile, ORIGIN, 0, -2);
        assertTrue(ship.isStuck());
        assertEquals(CubeCoords.ZERO, mobile.getNavalState().getStrandedVessels().get(2));
        assertEquals(0, ship.getElevation());
    }
    @Test void navalStateSurvivesNetworkAndXmlSavesAndResumesOnlyOnTheNextRound() throws Exception {
        var manager = manager(10, 6, EntityMovementMode.NAVAL);
        var mobile = mobile(manager);
        mobile.getNavalState().getStrandedVessels().put(2, CubeCoords.ZERO);
        manager.destroyEntity(mobile, "test", true);
        var bytes = new ByteArrayOutputStream();
        try(var out = new ObjectOutputStream(bytes)) { out.writeObject(mobile); }
        MobileStructure restored;
        try(var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) { restored = (MobileStructure) in.readObject(); }
        assertTrue(restored.getNavalState().isSinking());
        assertEquals(CubeCoords.ZERO, restored.getNavalState().getStrandedVessels().get(2));
        assertFalse(restored.getNavalState().beginSinkingTurn(1));
        assertTrue(restored.getNavalState().beginSinkingTurn(2));
        var xml = SerializationHelper.getSaveGameXStream().toXML(mobile);
        restored = (MobileStructure) SerializationHelper.getLoadSaveGameXStream().fromXML(xml);
        assertTrue(restored.getNavalState().isSinking());
        assertEquals(-3, restored.getNavalState().getBaseOffsets().get(CubeCoords.ZERO));
        assertEquals(CubeCoords.ZERO, restored.getNavalState().getStrandedVessels().get(2));
    }
    @Test void templateEquivalentRateAndUnderwaterBoundaryFollowThePrintedExample() {
        var manager = manager(10, 6, EntityMovementMode.NAVAL);
        var mobile = mobile(manager);
        assertEquals(5, MobileStructureNavalHandler.sinkingRate(mobile));
        mobile.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 6, 150, 0,
              java.util.stream.IntStream.range(0,9).mapToObj(q -> new CubeCoords(q, 0, -q)).toList());
        assertEquals(1, MobileStructureNavalHandler.sinkingRate(mobile));
        mobile.setElevation(-3);
        assertFalse(MobileStructureCollisionHandler.fullyUnderwater(mobile), "AUE p28: three descents do not fully submerge a six-level vessel");
        mobile.setElevation(-4);
        assertTrue(MobileStructureCollisionHandler.fullyUnderwater(mobile));
    }
    @Test void deckEscapeUsesJumpRollAndStopsFailedJumpAtItsDestination() throws Exception {
        var manager = manager(10, 6, EntityMovementMode.NAVAL);
        var carrier = mobile(manager);
        var rider = new BipedMek();
        rider.setId(3);
        rider.setOwner(manager.getGame().getPlayer(0));
        rider.setWeight(20);
        rider.setOriginalWalkMP(4);
        rider.setOriginalJumpMP(5);
        for (int loc = 0; loc < rider.locations(); loc++) { rider.initializeInternal(30, loc); rider.initializeArmor(100,loc); }
        for(int n = 0; n < 5; n++) rider.addEquipment(EquipmentType.get(
              megamek.common.equipment.EquipmentTypeLookup.JUMP_JET), BipedMek.LOC_CENTER_TORSO);
        manager.getGame().addEntity(rider);
        rider.setPosition(ORIGIN);
        rider.setElevation(3);
        rider.setDeployed(true);
        manager.getGame().getBoard().getHex(ORIGIN.translated(0)).removeTerrain(Terrains.WATER);
        manager.destroyEntity(carrier, "test", true);
        assertFalse(MobileStructureNavalRules.canDepart(rider, ORIGIN, 3, ORIGIN.translated(0), 0, false));
        assertTrue(MobileStructureNavalRules.canDepart(rider, ORIGIN, 3, ORIGIN.translated(0), 0, true));
        var path = new MovePath(manager.getGame(), rider).addStep(MoveStepType.START_JUMP).addStep(MoveStepType.FORWARDS);
        doReturn(1).when(manager).doSkillCheckWhileMoving(eq(rider), anyInt(), any(), any(), any(), eq(false), any());
        doReturn(new Vector<megamek.common.Report>()).when(manager).doEntityFallsInto(eq(rider), anyInt(), any(), any(), any(), anyBoolean(), anyInt());
        assertFalse(new MobileStructureNavalHandler(manager).departDeck(rider,path));
        assertEquals(ORIGIN.translated(0), rider.getPosition());
        assertTrue(rider.isDone());
        verify(manager).doEntityFallsInto(eq(rider), anyInt(), eq(rider.getPosition()), eq(rider.getPosition()), any(), eq(true), eq(0));
    }

    @Test void delayedDescentCarriesRoofRidersAndFootingRunsOnlyOncePerTurn() {
        var manager = manager(20, 12, EntityMovementMode.NAVAL);
        var carrier = mobile(manager);
        var rider = new BipedMek();
        rider.setId(3);
        rider.setOwner(manager.getGame().getPlayer(0));
        rider.setWeight(20);
        rider.setPosition(ORIGIN);
        rider.setElevation(6);
        rider.setDeployed(true);
        manager.getGame().addEntity(rider);
        manager.destroyEntity(carrier, "test", true);
        doReturn(0).when(manager).doSkillCheckWhileMoving(eq(rider), anyInt(), any(), any(), any(), eq(false), any());
        manager.getGame().setRoundCount(2);
        var handler = new MobileStructureNavalHandler(manager);
        handler.endMovement();
        assertEquals(1, rider.getElevation());
        handler.endTurn();
        handler.endTurn();
        verify(manager, times(1)).doSkillCheckWhileMoving(eq(rider), eq(1), eq(ORIGIN), eq(ORIGIN), any(), eq(false), any());
    }

    @Test void intactLinkedModulesSinkTogetherOnlyOnceAndKeepTheirLink() throws Exception {
        var manager = manager(20, 6, EntityMovementMode.NAVAL);
        var first = mobile(manager);
        var second = new MobileStructure(BuildingType.HARDENED, IBuilding.FORTRESS);
        second.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 6, 150, 0, List.of(CubeCoords.ZERO));
        second.setId(4);
        second.setOwner(first.getOwner());
        second.setMovementMode(EntityMovementMode.NAVAL);
        manager.getGame().addEntity(second);
        second.setPosition(ORIGIN.translated(2));
        second.setDeployed(true);
        first.addEquipment(EquipmentType.get("Modular Structure Linkage"), 0);
        second.addEquipment(EquipmentType.get("Modular Structure Linkage"), 0);
        second.updateBuildingEntityHexes(0, manager);
        assertTrue(megamek.common.moves.MobileStructureLinkage.link(first, second));
        manager.destroyEntity(first, "joined casualty", true);
        assertTrue(first.getNavalState().isSinking());
        assertTrue(second.getNavalState().isSinking());
        assertFalse(MobileStructureNavalRules.leavingDeck(first, second.getPosition(),
              BuildingElevation.roof(second, second.getPosition())), "linked roofs belong to one sinking unit");
        manager.getGame().setRoundCount(2);
        var handler = new MobileStructureNavalHandler(manager);
        handler.endMovement();
        assertEquals(-4, first.getElevation());
        assertEquals(-4, second.getElevation(), "the second module must not descend twice in the same phase");
        assertEquals(2, megamek.common.moves.MobileStructureLinkage.group(first).size());
        handler.endMovement();
        assertEquals(-4, first.getElevation());
        assertEquals(-4, second.getElevation());
    }

    @Test void sinkingDisablesAndRemovesOnlyOwnedElevators() {
        var manager = manager(10, 6, EntityMovementMode.NAVAL);
        var mobile = mobile(manager);
        var location = megamek.common.board.BoardLocation.of(ORIGIN, 0);
        var own = new megamek.common.IndustrialElevator(location, -3, 2, 50);
        own.setBuildingId(mobile.getId());
        var other = new megamek.common.IndustrialElevator(location, -3, 2, 50);
        other.setBuildingId(999);
        manager.getGame().addIndustrialElevator(own);
        manager.getGame().addIndustrialElevator(other);
        manager.destroyEntity(mobile, "lift casualty", true);
        assertFalse(own.isFunctional());
        assertTrue(other.isFunctional());
        manager.getGame().setRoundCount(2);
        new MobileStructureNavalHandler(manager).endMovement();
        assertEquals(-3, other.getShaftBottom(), "another volume's shaft must not descend with this hull");
        manager.getGame().setRoundCount(3);
        new MobileStructureNavalHandler(manager).endMovement();
        assertEquals(List.of(other), List.copyOf(manager.getGame().getIndustrialElevators()));
    }

    private LargeSupportTank largeVessel(TWGameManager manager, double tons) {
        var vessel = spy(new LargeSupportTank());
        vessel.setId(2);
        vessel.setOwner(manager.getGame().getPlayer(0));
        vessel.setWeight(tons);
        vessel.setBARRating(10);
        vessel.setMovementMode(EntityMovementMode.NAVAL);
        vessel.setOriginalWalkMP(4);
        vessel.setPosition(ORIGIN);
        vessel.setDone(true);
        vessel.setDeployed(true);
        doReturn(new megamek.common.HitData(Tank.LOC_FRONT)).when(vessel).rollHitLocation(anyInt(), anyInt());
        // A random hull breach can legitimately sink even an armored ship before displacement is considered.
        doReturn(new Vector<>()).when(manager).breachCheck(eq(vessel), anyInt(), isNull(), anyBoolean());
        for (int loc = 0; loc < vessel.locations(); loc++) {
            vessel.initializeArmor(10000, loc);
            vessel.initializeInternal(1000, loc);
        }
        manager.getGame().addEntity(vessel);
        return vessel;
    }

    @Test void printedNavalTemplatesIncludeFlankHexesAndRotateAroundTheMarkedPivot() {
        var manager = manager(20, 6, EntityMovementMode.NAVAL);
        var ship = largeVessel(manager, 400);
        int[] expected = { 1, 3, 9, 19, 25 };
        double[] weights = { 400, 1000, 10000, 20000, 40000 };
        for (int index = 0; index < weights.length; index++) {
            ship.setWeight(weights[index]);
            for (int facing = 0; facing < 6; facing++) {
                var footprint = LargeNavalVesselRules.footprint(ship, ORIGIN, facing);
                assertEquals(expected[index], footprint.size());
                assertTrue(footprint.contains(ORIGIN));
                assertTrue(footprint.contains(ORIGIN.translated(facing, index)));
                assertTrue(footprint.contains(ORIGIN.translated((facing + 3) % 6, index)));
                assertEquals(expected[index], new java.util.HashSet<>(footprint).size());
            }
        }
    }

    @Test void surfacingBelowLargeVesselChecksCapsizeAndDisplacesItsEntireFootprint() {
        var manager = manager(20, 6, EntityMovementMode.SUBMARINE);
        var mobile = mobile(manager);
        mobile.setElevation(-4);
        mobile.updateBuildingEntityHexes(0, manager);
        var ship = largeVessel(manager, 1000);
        doReturn(1).when(manager).doSkillCheckWhileMoving(eq(ship), anyInt(), any(), any(), any(), eq(false), any());
        doReturn(new Vector<>()).when(manager).damageCrew(eq(ship), eq(1));
        doReturn(new Vector<>()).when(manager).vehicleMotiveDamage(eq(ship), eq(0));
        doReturn(new Vector<>()).when(manager).criticalEntity(eq(ship), eq(Tank.LOC_FRONT), eq(false), eq(0), eq(false), eq(false), eq(0));
        var step = new MovePath(manager.getGame(), mobile).addStep(MoveStepType.UP).getLastStep();
        assertTrue(new MobileStructureCollisionHandler(manager).resolve(mobile, step));
        var roll = org.mockito.ArgumentCaptor.forClass(megamek.common.rolls.PilotingRollData.class);
        verify(manager).doSkillCheckWhileMoving(eq(ship), eq(0), eq(ORIGIN), eq(ORIGIN), roll.capture(), eq(false), any());
        assertEquals(ship.getBasePilotingRoll(EntityMovementType.MOVE_WALK).getValue() + 4, roll.getValue().getValue());
        verify(manager).damageCrew(ship, 1);
        verify(manager, times(2)).vehicleMotiveDamage(ship, 0);
        verify(manager, times(3)).criticalEntity(ship, Tank.LOC_FRONT, false, 0, false, false, 0);
        assertFalse(LargeNavalVesselRules.footprint(ship).contains(ORIGIN));
        assertFalse(ship.isStuck());
    }

    @Test void submersibleLargeVesselReceivesOnlyOneMotiveAndOneCriticalRoll() throws Exception {
        var manager = manager(20, 6, EntityMovementMode.SUBMARINE);
        var mobile = mobile(manager);
        var ship = largeVessel(manager, 1000);
        ship.addEquipment(EquipmentType.get("SubmersibleChassisMod"), Tank.LOC_BODY);
        doReturn(1).when(manager).doSkillCheckWhileMoving(eq(ship), anyInt(), any(), any(), any(), eq(false), any());
        doReturn(new Vector<>()).when(manager).damageCrew(eq(ship), eq(1));
        doReturn(new Vector<>()).when(manager).vehicleMotiveDamage(eq(ship), eq(0));
        doReturn(new Vector<>()).when(manager).criticalEntity(eq(ship), eq(Tank.LOC_FRONT), eq(false), eq(0), eq(false), eq(false), eq(0));
        var step = new MovePath(manager.getGame(), mobile).addStep(MoveStepType.UP).getLastStep();
        new LargeNavalVesselCollisionHandler(manager).afterMobileSurfacing(ship, mobile, step);
        verify(manager).vehicleMotiveDamage(ship, 0);
        verify(manager).criticalEntity(ship, Tank.LOC_FRONT, false, 0, false, false, 0);
    }

    @Test void movingLargeVesselDisplacesWaterOnlyMobileInsteadOfGenericImmunity() {
        var manager = manager(20, 6, EntityMovementMode.NAVAL);
        var mobile = mobile(manager);
        var ship = largeVessel(manager, 400);
        ship.setPosition(ORIGIN.translated(3, 2));
        ship.setDone(false);
        var step = new MovePath(manager.getGame(), ship).addStep(MoveStepType.FORWARDS)
              .addStep(MoveStepType.FORWARDS).getLastStep();
        assertEquals(0, step.getElevation());
        assertEquals(1, step.getMp());
        assertNotEquals(EntityMovementType.MOVE_ILLEGAL, step.getMovementType(false));
        assertTrue(new MobileStructureCollisionHandler(manager).allowSurfacing(ship, ship.getPosition(), 0, step));
        assertFalse(ship.isDoomed() || ship.isDestroyed());
        assertEquals(9960, ship.getArmor(Tank.LOC_FRONT), "all forty points of return damage still reach the hull");
        assertFalse(mobile.isIn(ORIGIN));
        assertTrue(mobile.getCurrentCF(mobile.getPosition()) < 150);
    }

    @Test void aMovementPhaseCasualtyDescendsAtThatPhasesEnd() {
        var manager = manager(20, 6, EntityMovementMode.NAVAL);
        manager.getGame().setPhase(megamek.common.enums.GamePhase.MOVEMENT);
        manager.destroyEntity(mobile(manager), "movement casualty", true);
        new MobileStructureNavalHandler(manager).endMovement();
        assertEquals(-5, mobile(manager).getElevation());
        new MobileStructureNavalHandler(manager).endMovement();
        assertEquals(-5, mobile(manager).getElevation());
    }

    @Test void independentlySinkingHullsDescendSimultaneouslyInEitherEntityOrder() {
        for (boolean reverse : new boolean[] { false, true }) {
            var manager = manager(30, 2, EntityMovementMode.SUBMARINE);
            var first = mobile(manager);
            var second = new MobileStructure(BuildingType.HARDENED, IBuilding.FORTRESS);
            second.setId(3);
            second.setOwner(first.getOwner());
            second.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 2, 150, 0, List.of(CubeCoords.ZERO));
            second.setMovementMode(EntityMovementMode.SUBMARINE);
            second.setPosition(ORIGIN);
            second.setElevation(reverse ? 0 : -2);
            first.setElevation(reverse ? -2 : 0);
            manager.getGame().addEntity(second);
            second.setDeployed(true);
            first.updateBuildingEntityHexes(0, manager);
            second.updateBuildingEntityHexes(0, manager);
            manager.getGame().setPhase(megamek.common.enums.GamePhase.MOVEMENT);
            manager.destroyEntity(first, "simultaneous upper/lower wreck", true);
            manager.destroyEntity(second, "simultaneous upper/lower wreck", true);
            new MobileStructureNavalHandler(manager).endMovement();
            assertEquals(reverse ? -7 : -5, first.getElevation());
            assertEquals(reverse ? -5 : -7, second.getElevation());
            assertEquals(150, first.getCurrentCF(ORIGIN), "another sinking hull's previous depth is not a collision");
            assertEquals(150, second.getCurrentCF(ORIGIN));
            assertEquals(ORIGIN, first.getPosition());
            assertEquals(ORIGIN, second.getPosition());
        }
    }

    @Test void navalCollisionIncludesVisibleHexOfAPartlyOffMapMobile() {
        var manager = manager(20, 6, EntityMovementMode.NAVAL);
        var mobile = mobile(manager);
        mobile.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 6, 150, 0,
              List.of(CubeCoords.ZERO, new CubeCoords(-1, 0, 1)));
        var edge = new Coords(0, 6);
        mobile.setPosition(edge);
        mobile.updateBuildingEntityHexes(0, manager);
        assertTrue(mobile.getCoordsList().stream().anyMatch(coords -> !manager.getGame().getBoard().contains(coords)));
        var ship = largeVessel(manager, 400);
        var from = edge.translated(3, 2);
        ship.setPosition(from);
        assertEquals(List.of(mobile), LargeNavalVesselRules.obstacles(ship, from, 0, 0, edge, 0, 0));
    }

    @Test void onlyTheMobileHexWhoseHeightActuallyMeetsTheVesselTakesChargeDamage() {
        var manager = manager(20, 6, EntityMovementMode.NAVAL);
        var mobile = mobile(manager);
        var north = new CubeCoords(0, -1, 1);
        mobile.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 6, 150, 0, List.of(CubeCoords.ZERO, north));
        mobile.getInternalBuilding().setHeight(1, CubeCoords.ZERO);
        mobile.updateBuildingEntityHexes(0, manager);
        var ship = largeVessel(manager, 1000);
        ship.setMovementMode(EntityMovementMode.SUBMARINE);
        ship.setElevation(-1);
        ship.setPosition(ORIGIN.translated(3, 3));
        var expected = mobile.relativeToBoard(north);
        assertEquals(List.of(expected), LargeNavalVesselRules.contactHexes(ship, mobile,
              ship.getPosition(), 0, -1, ORIGIN, 0, -1));
        var step = new MovePath(manager.getGame(), ship).addStep(MoveStepType.FORWARDS)
              .addStep(MoveStepType.FORWARDS).addStep(MoveStepType.FORWARDS).getLastStep();
        new LargeNavalVesselCollisionHandler(manager).entering(ship, ship.getPosition(), -1, step);
        assertEquals(150, mobile.getInternalBuilding().getCurrentCF(CubeCoords.ZERO));
        assertTrue(mobile.getInternalBuilding().getCurrentCF(north) < 150);
    }

    @Test void upwardVesselCollisionUsesTheImpactWaterlineForDamage() {
        var manager = manager(20, 2, EntityMovementMode.SUBMARINE);
        var mobile = mobile(manager);
        mobile.setElevation(-2);
        mobile.updateBuildingEntityHexes(0, manager);
        var ship = largeVessel(manager, 1000);
        ship.setMovementMode(EntityMovementMode.SUBMARINE);
        ship.setElevation(-2);
        ship.setLocationStatus(1, megamek.common.interfaces.ILocationExposureStatus.WET);
        doReturn(new Vector<>()).when(manager).damageEntity(eq(ship), any(), anyInt());
        var step = new MovePath(manager.getGame(), ship).addStep(MoveStepType.UP).getLastStep();
        assertEquals(-1, step.getElevation());
        assertTrue(LargeNavalVesselCollisionHandler.fullyUnderwater(ship, -2));
        assertFalse(LargeNavalVesselCollisionHandler.fullyUnderwater(ship, step.getElevation()));
        new LargeNavalVesselCollisionHandler(manager).entering(ship, ORIGIN, -2, step);
        var damage = org.mockito.ArgumentCaptor.forClass(Integer.class);
        verify(manager, atLeastOnce()).damageEntity(eq(ship), any(), damage.capture());
        assertEquals(100, damage.getAllValues().stream().mapToInt(Integer::intValue).sum());
    }

    @Test void bottomedMobileWreckIsUltraRubbleAndPreservesTheWaterDepth() {
        var manager = manager(5, 6, EntityMovementMode.TRACKED);
        manager.getGame().getOptions().getOption(megamek.common.options.OptionsConstants.ADVANCED_TAC_OPS_BATTLE_WRECK).setValue(true);
        manager.destroyEntity(mobile(manager), "bottomed wreck", true);
        var hex = manager.getGame().getBoard().getHex(ORIGIN);
        assertEquals(6, hex.terrainLevel(Terrains.RUBBLE));
        assertEquals(5, hex.depth());
        assertFalse(hex.containsTerrain(Terrains.ROUGH));
    }

    @Test void underwaterCollisionRoundsOddReturnDamageDownExactlyOnceDespiteWetSource() {
        var manager = manager(20, 2, EntityMovementMode.SUBMARINE);
        var mobile = mobile(manager);
        mobile.setElevation(-3);
        mobile.updateBuildingEntityHexes(0, manager);
        var ship = largeVessel(manager, 1010);
        ship.setMovementMode(EntityMovementMode.SUBMARINE);
        ship.setElevation(-3);
        ship.setLocationStatus(1, megamek.common.interfaces.ILocationExposureStatus.WET);
        doReturn(new Vector<>()).when(manager).damageEntity(eq(ship), any(), anyInt());
        var step = new MovePath(manager.getGame(), ship).addStep(MoveStepType.UP).getLastStep();
        assertEquals(-2, step.getElevation());
        new LargeNavalVesselCollisionHandler(manager).entering(ship, ORIGIN, -3, step);
        var damage = org.mockito.ArgumentCaptor.forClass(Integer.class);
        verify(manager, atLeastOnce()).damageEntity(eq(ship), any(), damage.capture());
        assertEquals(50, damage.getAllValues().stream().mapToInt(Integer::intValue).sum(), "101 standard / 2, rounded down once");
    }

    @Test void verticalFighterTakeoffFromSinkingRoofChecksEvenWhenPathCoordinatesDoNotChange() {
        var manager = manager(20, 6, EntityMovementMode.NAVAL);
        var mobile = mobile(manager);
        var fighter = new megamek.common.units.AeroSpaceFighter();
        fighter.setId(5);
        fighter.setOwner(mobile.getOwner());
        fighter.setPosition(ORIGIN);
        fighter.setAltitude(0);
        fighter.setElevation(BuildingElevation.roof(mobile, ORIGIN));
        manager.getGame().addEntity(fighter);
        fighter.setDeployed(true);
        manager.destroyEntity(mobile, "sinking under fighter", true);
        var path = new MovePath(manager.getGame(), fighter).addStep(MoveStepType.VERTICAL_TAKE_OFF);
        assertEquals(ORIGIN, path.getFinalCoords());
        assertEquals(mobile, MobileStructureNavalRules.departureCarrier(fighter, path));
        doReturn(1).when(manager).doSkillCheckWhileMoving(eq(fighter), anyInt(), any(), any(), any(), eq(false), any());
        doReturn(new Vector<>()).when(manager).processCrash(fighter, 1, ORIGIN);
        assertFalse(new MobileStructureNavalHandler(manager).departDeck(fighter, path));
        verify(manager).processCrash(fighter, 1, ORIGIN);
        assertTrue(fighter.isDone());
    }

}
