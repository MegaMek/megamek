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
import java.util.TreeMap;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.bays.ASFBay;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.moves.MobileStructureAirMovement;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.units.*;
import megamek.common.util.SerializationHelper;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BuildingAircraftDeckTest {
    private static final Coords ORIGIN = new Coords(8, 8);
    private static final List<CubeCoords> HEXES = List.of(CubeCoords.ZERO, new CubeCoords(0,-1,1), new CubeCoords(0,-2,2));
    private static class SilentManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int player, Packet packet) { }
    }
    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }
    private TWGameManager manager() {
        var manager = spy(new SilentManager());
        manager.getGame().getOptions().initialize();
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        manager.getGame().setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        manager.getGame().addPlayer(0, new Player(0, "Carrier"));
        manager.getGame().setRoundCount(1);
        manager.getGame().setPhase(GamePhase.MOVEMENT);
        return manager;
    }
    private MobileStructure carrier(TWGameManager manager, boolean helipad) throws Exception {
        var carrier = new MobileStructure(BuildingType.HEAVY, IBuilding.HANGAR);
        carrier.configureConstruction(BuildingType.HEAVY, IBuilding.HANGAR, 3, 45, 10, HEXES);
        carrier.setId(1);
        carrier.setOwner(manager.getGame().getPlayer(0));
        carrier.setChassis("Carrier");
        carrier.setModel("Deck");
        var mount = carrier.addEquipment(EquipmentType.get(helipad ? "Building Helipad" : "Building Flight Deck"), 2);
        carrier.getDesign().getEquipmentSpace().put(mount, new java.util.ArrayList<>((helipad ? List.of(CubeCoords.ZERO) : HEXES)
              .stream().map(hex -> new BuildingDesign.Position(hex, 2)).toList()));
        carrier.addTransporter(new ASFBay(4, 1, 7));
        carrier.getDesign().getBayDoors().add(new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 0), 3));
        manager.getGame().addEntity(carrier);
        carrier.setPosition(ORIGIN);
        carrier.setDeployed(true);
        carrier.updateBuildingEntityHexes(carrier.getBoardId(), manager);
        return carrier;
    }
    private AeroSpaceFighter fighter(TWGameManager manager, int id) {
        var fighter = new AeroSpaceFighter();
        fighter.setId(id);
        fighter.setOwner(manager.getGame().getPlayer(0));
        fighter.setWeight(50);
        fighter.setOriginalWalkMP(6);
        fighter.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        fighter.setChassis("Fighter");
        fighter.setModel("Probe");
        fighter.getCrew().setPiloting(0, 0);
        fighter.setDeployed(true);
        manager.getGame().addEntity(fighter);
        fighter.setPosition(ORIGIN);
        return fighter;
    }
    private void load(MobileStructure carrier, AeroSpaceFighter fighter) {
        ((ASFBay) carrier.getTransportBays().getFirst()).load(fighter);
        fighter.setTransportId(carrier.getId());
        fighter.setPosition(null);
        fighter.newRound(carrier.getGame().getRoundCount());
        carrier.getBay(fighter).resetCounts();
    }

    @Test void landingUsesTheThreeHexDeckAndEnemyModifierInsteadOfTerrainRunway() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        var fighter = fighter(manager, 2);
        fighter.liftOff(1);
        assertNull(fighter.hasRoomForHorizontalLanding(0, ORIGIN));
        assertEquals(3, fighter.getLandingCoords(false, ORIGIN, 0).size());
        assertNull(BuildingFlightDeckRules.landingDeck(fighter, 0, ORIGIN, 1, false));
        int friendly = fighter.getLandingControlRoll(1, ORIGIN, 0, false).getValue();
        var enemy = new Player(2, "Enemy");
        manager.getGame().addPlayer(2, enemy);
        fighter.setOwner(enemy);
        assertEquals(friendly + 3, fighter.getLandingControlRoll(1, ORIGIN, 0, false).getValue());
        assertEquals(5, BuildingFlightDeckRules.decks(carrier).getFirst().elevation(ORIGIN));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void flightDeckAndHelipadRemainOnSurvivingSettledRoofs(boolean helipad) throws Exception {
        var manager = manager();
        var carrier = carrier(manager, helipad);
        carrier.enableExpandedCF();
        var handler = new BuildingCollapseHandler(manager);
        for (CubeCoords hex : HEXES) {
            Coords coords = carrier.relativeToBoard(hex);
            carrier.setCurrentCF(0, coords, 1);
            assertTrue(handler.resolveExpandedCollapse(carrier, coords, new Vector<>()));
        }
        var deck = BuildingFlightDeckRules.decks(carrier).getFirst();
        assertEquals(4, deck.elevation(ORIGIN));
        assertEquals(2, BuildingConstruction.equipmentPositions(carrier, deck.mount()).getFirst().level());
        carrier.setCurrentCF(0, ORIGIN, 1);
        assertTrue(handler.resolveExpandedCollapse(carrier, ORIGIN, new Vector<>()));
        assertTrue(BuildingFlightDeckRules.decks(carrier).isEmpty(), "destroying the original roof destroys its deck");
    }

    @Test void landingIsExposedForFiveTurnsAndDeckCannotOperateAgainUntilSixHaveElapsed() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        var fighter = fighter(manager, 2);
        fighter.land();
        fighter.setElevation(5);
        var deck = BuildingFlightDeckRules.decks(carrier).getFirst();
        BuildingFlightDeckRules.landed(deck, fighter, ORIGIN);
        assertFalse(MobileStructureCargoRules.canMount(carrier, fighter, ORIGIN, 5));
        manager.getGame().setRoundCount(5);
        assertFalse(BuildingFlightDeckRules.canEnterBay(fighter));
        manager.getGame().setRoundCount(6);
        assertTrue(MobileStructureCargoRules.canMount(carrier, fighter, ORIGIN, 5));
        assertTrue(Compute.getMountableUnits(fighter, ORIGIN, 0, 5, manager.getGame()).contains(carrier));
        assertFalse(BuildingFlightDeckRules.canTakeOff(fighter, false));
        manager.getGame().setRoundCount(7);
        assertTrue(BuildingFlightDeckRules.canTakeOff(fighter, false));
        var xstream = SerializationHelper.getLoadSaveGameXStream();
        var restored = (MobileStructure) xstream.fromXML(SerializationHelper.getSaveGameXStream().toXML(carrier));
        assertEquals(deck.state().occupants(), restored.getBuildingRuntimeState().getFlightDecks().get(deck.id()).occupants());
        assertEquals(carrier.getDesign().getBayDoors(), restored.getDesign().getBayDoors());
    }

    @Test void actualUnloadStagesAnAircraftOnDeckRatherThanUnderTheCarrier() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        var fighter = fighter(manager, 2);
        load(carrier, fighter);
        var exits = MobileStructureCargoRules.exits(carrier, fighter);
        assertEquals(3, exits.size());
        assertTrue(exits.stream().allMatch(exit -> exit.elevation() == 5));
        assertTrue(manager.unloadUnit(carrier, fighter, ORIGIN, 0, 0));
        assertEquals(5, fighter.getElevation());
        assertEquals(0, fighter.getAltitude());
        assertNotNull(BuildingFlightDeckRules.onDeck(fighter));
        assertTrue(BuildingFlightDeckRules.canEnterBay(fighter));
    }

    @Test void damageToCarrierHexAlsoHitsTheAircraftBeforeCarrierArmorAbsorbsIt() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        var fighter = fighter(manager, 2);
        fighter.land();
        fighter.setElevation(5);
        var attacker = fighter(manager, 3);
        attacker.setPosition(ORIGIN.translated(3));
        doReturn(new Vector<Report>()).when(manager).damageEntity(eq(fighter), any(), anyInt());
        manager.damageBuilding(carrier, 9, "shot", ORIGIN, 0, attacker, false);
        verify(manager).damageEntity(eq(fighter), any(), eq(9));
        assertEquals(1, carrier.getArmor(ORIGIN));
    }

    @ParameterizedTest @ValueSource(ints={200,201})
    void weightLimitAndWorkingDeckAreRequired(int tons) throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        var fighter = fighter(manager, 2);
        fighter.setWeight(tons);
        fighter.liftOff(1);
        assertEquals(tons <= 200, BuildingFlightDeckRules.landingDeck(fighter,0,ORIGIN,0,false) != null);
        carrier.getEquipment().getFirst().setDestroyed(true);
        assertTrue(BuildingFlightDeckRules.decks(carrier).isEmpty());
    }

    @Test void helipadDoesNotAllowHorizontalLandingOrLaunch() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, true);
        var fighter = fighter(manager, 2);
        fighter.liftOff(1);
        assertNull(BuildingFlightDeckRules.landingDeck(fighter,0,ORIGIN,0,false));
        fighter.land();
        fighter.setElevation(5);
        assertFalse(BuildingFlightDeckRules.canTakeOff(fighter, false));
        assertEquals(1, BuildingFlightDeckRules.decks(carrier).getFirst().hexes().size());
    }

    @ParameterizedTest @CsvSource({"3,1", "100,6"})
    void airCarrierLaunchUsesActualServerMovementAndRestoresCorrectBoard(int carrierElevation, int altitude) throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        carrier.setMovementMode(EntityMovementMode.VTOL);
        carrier.setElevation(carrierElevation);
        carrier.updateBuildingEntityHexes(carrier.getBoardId(), manager);
        var fighter = fighter(manager, 2);
        load(carrier, fighter);
        fighter.setBoardId(7);
        var launches = new TreeMap<Integer,Vector<Integer>>();
        launches.put(0, new Vector<>(List.of(fighter.getId())));
        var path = new MovePath(manager.getGame(), carrier).addStep(MoveStepType.LAUNCH, launches);
        assertTrue(path.isMoveLegal());
        new MobileStructureMovementHandler(manager).process(carrier, path);
        assertEquals(0, fighter.getBoardId());
        assertEquals(Entity.NONE, fighter.getTransportId());
        assertEquals(altitude, fighter.getAltitude());
        assertFalse(fighter.isDone());
        assertTrue(manager.getGame().getBoard().contains(fighter.getPosition()));
    }

    @Test void forgedAndDuplicateLaunchIdsAndGroundedCarrierAreRejected() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        var fighter = fighter(manager, 2);
        load(carrier, fighter);
        var launches = new TreeMap<Integer,Vector<Integer>>();
        launches.put(0,new Vector<>(List.of(2)));
        assertFalse(MobileStructureBayLaunch.valid(carrier,ORIGIN,0,0,launches));
        carrier.setMovementMode(EntityMovementMode.VTOL);
        carrier.setElevation(2);
        assertTrue(MobileStructureBayLaunch.valid(carrier,ORIGIN,0,2,launches));
        launches.get(0).add(2);
        assertFalse(MobileStructureBayLaunch.valid(carrier,ORIGIN,0,2,launches));
        launches.put(0,new Vector<>(List.of(42)));
        assertFalse(MobileStructureBayLaunch.valid(carrier,ORIGIN,0,2,launches));
        launches.clear();
        launches.put(4,new Vector<>(List.of(2)));
        assertFalse(MobileStructureBayLaunch.valid(carrier,ORIGIN,0,2,launches));
    }

    @Test void landingAndCatapultLaunchUseTheActualServerEndpointAndPreserveRoofHeight() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        var fighter = fighter(manager, 2);
        fighter.liftOff(1);
        doReturn(true).when(manager).attemptLandingCheck(eq(fighter), any(), any());
        var handler = new BuildingFlightDeckHandler(manager);
        assertTrue(handler.handleAircraft(fighter, new MovePath(manager.getGame(), fighter).addStep(MoveStepType.LAND), true, () -> { }));
        assertEquals(ORIGIN.translated(0,2), fighter.getPosition());
        assertEquals(5, fighter.getElevation());
        assertEquals(0, fighter.getAltitude());
        assertTrue(fighter.isDone());
        verify(manager).attemptLandingCheck(eq(fighter), any(), any());
        manager.getGame().setRoundCount(7);
        fighter.setFacing(3);
        assertTrue(handler.handleAircraft(fighter, new MovePath(manager.getGame(), fighter).addStep(MoveStepType.TAKEOFF), true, () -> { }));
        assertEquals(1, fighter.getAltitude());
        assertFalse(fighter.isDone());
        assertTrue(BuildingFlightDeckRules.decks(carrier).getFirst().state().occupants().isEmpty());
    }

    @Test void highDeckRejectsAPlaneFlyingUnderneathAndLaunchesIntoItsPhysicalAltitudeBand() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        carrier.setMovementMode(EntityMovementMode.VTOL);
        carrier.setElevation(100);
        carrier.updateBuildingEntityHexes(carrier.getBoardId(), manager);
        var fighter = fighter(manager, 2);
        fighter.liftOff(1);
        doReturn(true).when(manager).attemptLandingCheck(eq(fighter), any(), any());
        var handler = new BuildingFlightDeckHandler(manager);
        handler.handleAircraft(fighter, new MovePath(manager.getGame(), fighter).addStep(MoveStepType.LAND), true, () -> { });
        assertEquals(1, fighter.getAltitude());
        verify(manager, never()).attemptLandingCheck(eq(fighter), any(), any());
        fighter.liftOff(6);
        handler.handleAircraft(fighter, new MovePath(manager.getGame(), fighter).addStep(MoveStepType.LAND), true, () -> { });
        assertEquals(0, fighter.getAltitude());
        assertEquals(103, fighter.getElevation());
        manager.getGame().setRoundCount(7);
        fighter.setFacing(3);
        handler.handleAircraft(fighter, new MovePath(manager.getGame(), fighter).addStep(MoveStepType.TAKEOFF), true,
              () -> fighter.setPosition(new Coords(1,1)));
        assertEquals(6, fighter.getAltitude());
    }

    @Test void failedCatapultLaunchStrikesTheAftLocationAndStillLaunchesASurvivingFighter() throws Exception {
        var manager = manager();
        carrier(manager, false);
        var fighter = fighter(manager, 2);
        fighter.land();
        fighter.setElevation(5);
        fighter.getCrew().setPiloting(20, 0);
        doReturn(new Vector<Report>()).when(manager).damageEntity(eq(fighter), any(), anyInt());
        new BuildingFlightDeckHandler(manager).handleAircraft(fighter,
              new MovePath(manager.getGame(), fighter).addStep(MoveStepType.TAKEOFF), true, () -> { });
        verify(manager).damageEntity(eq(fighter), argThat(hit -> hit.getLocation() == Aero.LOC_AFT), intThat(damage -> damage >= 80));
        assertTrue(fighter.isAirborne());
    }

    @Test void partiallyOffBoardDeckAndIncapacitatedOrOversizedAircraftCannotLaunch() throws Exception {
        var manager = manager();
        var carrier = carrier(manager, false);
        var fighter = fighter(manager, 2);
        fighter.land();
        fighter.setElevation(5);
        fighter.setWeight(201);
        assertFalse(BuildingFlightDeckRules.canTakeOff(fighter, false));
        fighter.setWeight(50);
        fighter.setPosition(ORIGIN.translated(0));
        assertFalse(BuildingFlightDeckRules.canTakeOff(fighter, false));
        fighter.setPosition(ORIGIN);
        fighter.setShutDown(true);
        assertFalse(BuildingFlightDeckRules.canTakeOff(fighter, false));
        fighter.setShutDown(false);
        load(carrier, fighter);
        carrier.setMovementMode(EntityMovementMode.VTOL);
        carrier.setElevation(3);
        var launches = new TreeMap<Integer, Vector<Integer>>();
        launches.put(0, new Vector<>(List.of(fighter.getId())));
        fighter.setDestroyed(true);
        assertFalse(MobileStructureBayLaunch.valid(carrier, ORIGIN, 0, 3, launches));
        carrier.setPosition(new Coords(8,0));
        assertTrue(BuildingFlightDeckRules.decks(carrier).isEmpty());
    }

    private MobileStructure landingCarrier(TWGameManager manager, int radius) throws Exception {
        var hexes = ORIGIN.allAtDistanceOrLess(radius).stream().map(c -> c.toCube().subtract(ORIGIN.toCube())).toList();
        var carrier = new MobileStructure(BuildingType.HARDENED, IBuilding.FORTRESS);
        carrier.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 3, 150, 0, hexes);
        carrier.setId(1);
        carrier.setOwner(manager.getGame().getPlayer(0));
        var mount = carrier.addEquipment(EquipmentType.get("Building Landing Deck"), 2);
        carrier.getDesign().getEquipmentSpace().put(mount, new java.util.ArrayList<>(hexes.stream()
              .map(hex -> new BuildingDesign.Position(hex,2)).toList()));
        manager.getGame().addEntity(carrier);
        carrier.setPosition(ORIGIN);
        carrier.setDeployed(true);
        carrier.updateBuildingEntityHexes(carrier.getBoardId(), manager);
        return carrier;
    }

    @ParameterizedTest @CsvSource({"1,7000", "2,19000", "3,37000"})
    void landingDeckUsesAuthoredAreaAndRetainsCapacityUntilAThirdOfTheCarrierIsLost(int radius, int tons) throws Exception {
        var manager = manager();
        var carrier = landingCarrier(manager,radius);
        var deck = BuildingFlightDeckRules.decks(carrier).getFirst();
        assertEquals(tons,deck.capacity());
        var lost = new java.util.ArrayList<>(carrier.getCoordsList());
        int threshold = (carrier.getOriginalHexCount() + 2) / 3;
        for (int i = 0; i < threshold - 1; i++) { carrier.removeHex(lost.get(i)); }
        assertTrue(deck.available());
        assertEquals(tons,deck.capacity());
        carrier.removeHex(lost.get(threshold - 1));
        assertFalse(deck.available());
    }

    @Test void dropshipLandsAndLiftsOffFromAnActualSevenHexLandingDeck() throws Exception {
        var manager = manager();
        var carrier = landingCarrier(manager,1);
        var dropship = new Dropship();
        dropship.setId(2);
        dropship.setOwner(manager.getGame().getPlayer(0));
        dropship.setWeight(6000);
        dropship.setSpheroid(true);
        dropship.setOriginalWalkMP(3);
        dropship.setDeployed(true);
        dropship.setEngine(new Engine(400,Engine.NORMAL_ENGINE,0));
        manager.getGame().addEntity(dropship);
        dropship.setPosition(ORIGIN);
        dropship.liftOff(1);
        assertNull(dropship.hasRoomForVerticalLanding(0,ORIGIN));
        assertEquals(7,dropship.getLandingCoords(true,ORIGIN,0).size());
        doReturn(true).when(manager).attemptLandingCheck(eq(dropship),any(),any());
        doReturn(true).when(manager).doVerticalTakeOffCheck(eq(dropship),any());
        var handler = new BuildingFlightDeckHandler(manager);
        handler.handleAircraft(dropship,new MovePath(manager.getGame(),dropship).addStep(MoveStepType.VERTICAL_LAND),true,() -> { });
        assertEquals(0,dropship.getAltitude());
        assertEquals(5,dropship.getElevation());
        assertEquals(7,dropship.getOccupiedCoords().size());
        assertTrue(dropship.isTargetable());
        assertNotNull(BuildingFlightDeckRules.onDeck(dropship));
        assertTrue(BuildingFlightDeckRules.canTakeOff(dropship,true), () -> "thrust=" + dropship.getCurrentThrust()
              + ", canLift=" + dropship.canTakeOffVertically() + ", active=" + dropship.getCrew().isActive()
              + ", shutdown=" + dropship.isShutDown() + ", manualShutdown=" + dropship.isManualShutdown()
              + ", deck=" + BuildingFlightDeckRules.onDeck(dropship));
        handler.handleAircraft(dropship,new MovePath(manager.getGame(),dropship).addStep(MoveStepType.VERTICAL_TAKE_OFF),true,() -> { });
        assertTrue(dropship.isAirborne());
        assertTrue(dropship.isDone());
        assertEquals(1,dropship.getOccupiedCoords().size());
        assertEquals(7,carrier.getCoordsList().size());
    }

    @ParameterizedTest @CsvSource({"0,1", "8,1", "9,2", "16,2", "17,3", "25,3", "26,4", "83,5", "84,6", "3001,11"})
    void physicalHeightsUseThePublishedLowAltitudeBands(int level,int expected) {
        assertEquals(expected,MobileStructureAirMovement.aerospaceAltitude(level));
    }
}
