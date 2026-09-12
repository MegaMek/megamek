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
import java.util.Vector;

import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.bays.Bay;
import megamek.common.bays.CargoBay;
import megamek.common.bays.LiquidCargoBay;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.units.*;
import megamek.common.util.SerializationHelper;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Actual gameplay regressions for TO:AR pp.117–118,134–138. */
class BuildingInteriorEnvironmentTest {
    private static final Coords ORIGIN = new Coords(8, 8);
    private static final CubeCoords NORTH = new CubeCoords(0, -1, 1);

    @ParameterizedTest
    @CsvSource({"punch,false", "punch,true", "kick,false", "kick,true", "jumpJet,false", "jumpJet,true",
          "proto,false", "proto,true", "club,false", "club,true", "charge,false", "charge,true"})
    void physicalHandlersScaleOccupantDamageAfterAbsorptionAndDamageOnlyTheAbsorbedAmount(String attack, boolean fortress) throws Exception {
        var structure = building(fortress ? IBuilding.FORTRESS : IBuilding.STANDARD,3,List.of(CubeCoords.ZERO));
        var manager = manager(structure);
        structure.setCurrentCF(5,ORIGIN);
        structure.setPhaseCF(5,ORIGIN);
        manager.getGame().setPhase(GamePhase.PHYSICAL);
        var target = mek(manager,ORIGIN,0);
        megamek.common.units.Entity attacker;
        if (attack.equals("proto")) {
            var proto = new megamek.common.units.ProtoMek();
            proto.setId(20);
            proto.setOwner(target.getOwner());
            proto.setWeight(15);
            manager.getGame().addEntity(proto);
            proto.setPosition(ORIGIN.translated(3));
            attacker = proto;
        } else {
            attacker = mek(manager,ORIGIN.translated(3),0);
            attacker.setWeight(50);
        }
        doReturn(new Vector<Report>()).when(manager).damageEntity(any(),any(),anyInt(),anyBoolean(),
              any(megamek.common.weapons.DamageType.class),anyBoolean(),anyBoolean(),anyBoolean());
        var result = new megamek.common.PhysicalResult();
        result.damage = attack.equals("proto") ? 3 : 20;
        result.toHit = new megamek.common.ToHitData(megamek.common.rolls.TargetRoll.AUTOMATIC_SUCCESS,"test hit");
        result.roll = Compute.rollD6(2);
        String method;
        switch (attack) {
            case "punch" -> {
                result.aaa = new megamek.common.actions.PunchAttackAction(attacker.getId(),target.getId(),
                      megamek.common.actions.PunchAttackAction.LEFT);
                method = "resolvePunchAttack";
            }
            case "kick" -> {
                result.aaa = new megamek.common.actions.KickAttackAction(attacker.getId(),target.getId(),0);
                method = "resolveKickAttack";
            }
            case "jumpJet" -> {
                result.aaa = new megamek.common.actions.JumpJetAttackAction(attacker.getId(),target.getId(),0);
                method = "resolveJumpJetAttack";
            }
            case "proto" -> {
                result.aaa = new megamek.common.actions.ProtoMekPhysicalAttackAction(attacker.getId(),target.getId());
                method = "resolveProtoAttack";
            }
            case "club" -> {
                var club = (megamek.common.equipment.MiscMounted) attacker.addEquipment(
                      megamek.common.equipment.MiscType.createHatchet(),megamek.common.units.Mek.LOC_RIGHT_ARM);
                result.aaa = new megamek.common.actions.ClubAttackAction(attacker.getId(),target.getId(),club,0);
                method = "resolveClubAttack";
            }
            default -> method = "resolveChargeDamage";
        }
        if (attack.equals("charge")) {
            attacker.delta_distance = 4;
            var endpoint = TWGameManager.class.getDeclaredMethod(method,megamek.common.units.Entity.class,
                  megamek.common.units.Entity.class,megamek.common.ToHitData.class,int.class);
            endpoint.setAccessible(true);
            endpoint.invoke(manager,attacker,target,result.toHit,0);
        } else {
            var endpoint = TWGameManager.class.getDeclaredMethod(method,megamek.common.PhysicalResult.class,int.class);
            endpoint.setAccessible(true);
            endpoint.invoke(manager,result,Entity.NONE);
        }
        int actual = mockingDetails(manager).getInvocations().stream().filter(invocation ->
              invocation.getMethod().getName().equals("damageEntity") && invocation.getArguments().length == 8
                    && invocation.getArgument(0) == target).mapToInt(invocation -> (Integer) invocation.getArgument(2)).sum();
        int expected = attack.equals("charge") ? (fortress ? 8 : 16)
              : attack.equals("proto") ? (fortress ? 1 : 2) : fortress ? 9 : 19;
        assertEquals(expected,actual);
        int absorbed = mockingDetails(manager).getInvocations().stream().filter(invocation ->
              invocation.getMethod().getName().equals("damageBuilding") && invocation.getArguments().length == 7
                    && " absorbs ".equals(invocation.getArgument(2)))
              .mapToInt(invocation -> (Integer) invocation.getArgument(1)).sum();
        assertEquals(attack.equals("charge") ? 4 : 1, absorbed);
        // A successful charge also displaces its 50-ton attacker through the wall: another 5 CF before scaling.
        assertEquals(attack.equals("charge") ? (fortress ? 3 : 0) : fortress ? 5 : 4, structure.getCurrentCF(ORIGIN));
    }

    @Test void foundationCascadeRecomputesDamageAfterTheSurfaceFloorsHaveDamagedThemselves() {
        var surface = building(IBuilding.STANDARD,3,List.of(CubeCoords.ZERO));
        surface.configureConstruction(BuildingType.HEAVY,IBuilding.STANDARD,3,60,0,List.of(CubeCoords.ZERO));
        var manager = manager(surface);
        var foundation = building(IBuilding.STANDARD,2,List.of(CubeCoords.ZERO));
        foundation.configureConstruction(BuildingType.HEAVY,IBuilding.STANDARD,2,60,0,List.of(CubeCoords.ZERO));
        foundation.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        foundation.getDesign().setBaseLevel(-2);
        deploy(manager,foundation,2,ORIGIN);
        surface.enableExpandedCF();
        foundation.enableExpandedCF();
        surface.setCurrentCF(20,ORIGIN,0);
        surface.setCurrentCF(0,ORIGIN,1);
        new BuildingCollapseHandler(manager).resolveExpandedCollapse(surface,ORIGIN,new Vector<>());
        // The falling top floor first loses 20 of its 60 CF, then crosses G for floor(40 / 3 / 2) = 6 damage.
        assertEquals(54,foundation.getCurrentCF(ORIGIN,1));
    }

    private static class SilentManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int player, Packet packet) { }
    }

    @BeforeAll
    static void equipment() { EquipmentType.initializeTypes(); }

    private BuildingEntity building(int classification, int height, List<CubeCoords> footprint) {
        var result = new BuildingEntity(BuildingType.HEAVY, classification);
        result.configureConstruction(BuildingType.HEAVY, classification, height, 45, 0, footprint);
        result.setEngine(new Engine(0, Engine.NONE, 0));
        result.setChassis("Interior rules");
        result.setModel("Probe");
        return result;
    }

    @Test void floodingUsesOverlappingDoorHeightsAndTheCorrectColocatedUndergroundVolume() {
        var source = building(IBuilding.STANDARD,2,List.of(CubeCoords.ZERO));
        source.getDesign().setSite(BuildingDesign.Site.UNDERWATER);
        source.getDesign().setBaseLevel(-4);
        var sourceDoor = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO,0),0,2);
        source.getDesign().getDoors().add(sourceDoor);
        var manager = manager(source);
        var underground = building(IBuilding.STANDARD,3,List.of(CubeCoords.ZERO));
        underground.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        underground.getDesign().setBaseLevel(-4);
        var targetDoor = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO,1),3,2);
        underground.getDesign().getDoors().add(targetDoor);
        deploy(manager,underground,2,ORIGIN.translated(0));
        var surface = building(IBuilding.STANDARD,2,List.of(CubeCoords.ZERO));
        deploy(manager,surface,3,ORIGIN.translated(0));
        manager.getGame().setPhase(GamePhase.END);
        assertTrue(source.getBuildingRuntimeState().changeDoor(source,sourceDoor,source.getOwner(),true));
        assertTrue(underground.getBuildingRuntimeState().changeDoor(underground,targetDoor,underground.getOwner(),true));
        source.getBuildingRuntimeState().breach(CubeCoords.ZERO);
        source.getBuildingRuntimeState().flood(CubeCoords.ZERO,1,1,manager.getGame().getRoundCount());
        new BuildingEnvironmentHandler(manager).doorChanged(source,sourceDoor,new Vector<>());
        assertTrue(underground.getBuildingRuntimeState().isFlooded(CubeCoords.ZERO,0));
        assertFalse(surface.getBuildingRuntimeState().isBreached(CubeCoords.ZERO));
    }

    @Test void largeDoorPassageControlsAndFloodingFollowTheSurvivingOriginalFloors() {
        var structure = building(IBuilding.STANDARD, 4, List.of(CubeCoords.ZERO));
        structure.getDesign().setSite(BuildingDesign.Site.UNDERWATER);
        structure.getDesign().setBaseLevel(-4);
        var surviving = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 2), 0, 2);
        var destroyed = new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 1), 3, 1);
        structure.getDesign().getDoors().addAll(List.of(surviving, destroyed));
        var manager = manager(structure);
        manager.getGame().setPhase(GamePhase.END);
        var state = structure.getBuildingRuntimeState();
        assertTrue(state.changeDoor(structure, surviving, structure.getOwner(), true));
        assertTrue(state.changeDoor(structure, destroyed, structure.getOwner(), true));
        structure.enableExpandedCF();
        structure.setCurrentCF(0, ORIGIN, 1);
        assertTrue(new BuildingCollapseHandler(manager).resolveExpandedCollapse(structure, ORIGIN, new Vector<>()));
        assertEquals(1, BuildingElevation.currentFloor(structure, CubeCoords.ZERO, 2));
        assertEquals(-1, BuildingElevation.currentFloor(structure, CubeCoords.ZERO, 1));
        assertEquals(2, BuildingElevation.doorwayHeight(structure, surviving));
        var unit = mek(manager, ORIGIN.translated(0), -3);
        assertTrue(state.openPassage(structure, unit, ORIGIN.translated(0), ORIGIN, -3));
        assertFalse(state.openPassage(structure, unit, ORIGIN.translated(0), ORIGIN, -2));
        assertFalse(state.openPassage(structure, unit, ORIGIN.translated(3), ORIGIN, -3));
        manager.getGame().setRoundCount(2);
        assertTrue(state.canChangeDoor(structure, surviving, structure.getOwner()));
        assertFalse(state.canChangeDoor(structure, destroyed, structure.getOwner()));
        new BuildingEnvironmentHandler(manager).doorChanged(structure, surviving, new Vector<>());
        assertTrue(state.isFlooded(CubeCoords.ZERO, 1));
        assertFalse(state.isFlooded(CubeCoords.ZERO, 2));
        assertEquals(2, surviving.position().level(), "saved floor identity remains stable");
    }

    @Test void settledEquipmentUsesTheFloodedPhysicalFloorWithoutChangingItsDesignPlacement() throws Exception {
        var structure = building(IBuilding.STANDARD, 4, List.of(CubeCoords.ZERO));
        var lower = structure.addEquipment(new ISLaserMedium(), 2);
        var upper = structure.addEquipment(new ISLaserMedium(), 3);
        var lowerPosition = new BuildingDesign.Position(CubeCoords.ZERO, 2);
        var upperPosition = new BuildingDesign.Position(CubeCoords.ZERO, 3);
        structure.getDesign().getEquipmentSpace().put(lower, List.of(lowerPosition));
        structure.getDesign().getEquipmentSpace().put(upper, List.of(upperPosition));
        var manager = manager(structure);
        structure.enableExpandedCF();
        structure.setCurrentCF(0, ORIGIN, 1);
        assertTrue(new BuildingCollapseHandler(manager).resolveExpandedCollapse(structure, ORIGIN, new Vector<>()));
        assertEquals(2, BuildingElevation.currentFloor(structure, CubeCoords.ZERO, 3));
        structure.getBuildingRuntimeState().breach(CubeCoords.ZERO);
        structure.getBuildingRuntimeState().flood(CubeCoords.ZERO, 2, -1, manager.getGame().getRoundCount());
        new BuildingEnvironmentHandler(manager).endPhase(new Vector<>());
        assertTrue(upper.isBreached(), "original floor 3 settled into flooded physical level 2");
        assertFalse(lower.isBreached(), "original floor 2 settled into dry physical level 1");
        assertEquals(List.of(upperPosition), BuildingConstruction.equipmentPositions(structure, upper));
        assertEquals(List.of(lowerPosition), BuildingConstruction.equipmentPositions(structure, lower));
    }

    private TWGameManager manager(AbstractBuildingEntity building) {
        var manager = spy(new SilentManager());
        manager.getGame().getOptions().initialize();
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        doNothing().when(manager).sendChangedBuildings(any());
        var game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 20 20\nend\n"));
        var player = new Player(0, "Builder");
        game.addPlayer(0, player);
        deploy(manager, building, 1, ORIGIN);
        game.setRoundCount(1);
        return manager;
    }

    private void deploy(TWGameManager manager, AbstractBuildingEntity building, int id, Coords coords) {
        building.setOwner(manager.getGame().getPlayer(0));
        building.setId(id);
        building.setDeployed(true);
        manager.getGame().addEntity(building);
        building.setPosition(coords);
        building.updateBuildingEntityHexes(0, manager);
    }

    private BipedMek mek(TWGameManager manager, Coords coords, int elevation) {
        var mek = new BipedMek();
        mek.setOwner(manager.getGame().getPlayer(0));
        mek.setId(manager.getGame().getEntitiesVector().size() + 10);
        mek.setWeight(70);
        mek.setDeployed(true);
        mek.setOriginalWalkMP(6);
        mek.setEngine(new Engine(280, Engine.NORMAL_ENGINE, 0));
        mek.setPosition(coords);
        mek.setElevation(elevation);
        mek.setClimbMode(false);
        mek.getCrew().setPiloting(4, 0);
        manager.getGame().addEntity(mek);
        return mek;
    }

    private static int location(BuildingEntity building, CubeCoords hex, int floor) {
        return BuildingConstruction.location(building, new BuildingDesign.Position(hex, floor));
    }

    @ParameterizedTest
    @CsvSource({"unspecified,0,0,1", "specified,1,1,2", "weapon,2,2,2"})
    void equipmentUsesContentsTableAndOnlyItsPlacedFloor(String kind, int mp, int psr, int shooting) throws Exception {
        var building = building(IBuilding.STANDARD, 3, List.of(CubeCoords.ZERO));
        var type = kind.equals("unspecified") ? EquipmentType.get("Unspecified Building Equipment")
              : kind.equals("specified") ? MiscType.createHeatSink() : new ISLaserMedium();
        building.addEquipment(type, location(building, CubeCoords.ZERO, 1));
        manager(building);
        var feature = BuildingInteriorRules.features(building, ORIGIN, 1);
        assertEquals(mp, feature.movement());
        assertEquals(psr, feature.piloting());
        assertEquals(shooting, feature.shooting());
        assertTrue(BuildingInteriorRules.features(building, ORIGIN, 0).empty());
    }

    @ParameterizedTest
    @CsvSource({"false,false,-1,-3", "false,true,0,-2", "true,false,-1,-3", "true,true,1,2"})
    void cargoDistinguishesEmptyDryAndLiquid(boolean liquid, boolean loaded, int mp, int psr) {
        var building = building(IBuilding.STANDARD, 3, List.of(CubeCoords.ZERO));
        Bay bay = liquid ? new LiquidCargoBay(20, 0, 1) : new CargoBay(20, 0, 1);
        if (loaded) {
            bay.setCurrentSpace(15); // Bay API removes this many tons from unused capacity.
        }
        building.addTransporter(bay);
        building.getDesign().getBaySpace().put(bay,
              List.of(new BuildingDesign.Space(new BuildingDesign.Position(CubeCoords.ZERO, 0), 20)));
        manager(building);
        assertEquals(mp, BuildingInteriorRules.features(building, ORIGIN, 0).movement());
        assertEquals(psr, BuildingInteriorRules.features(building, ORIGIN, 0).piloting());
    }

    @Test
    void turretAndRoofEquipmentDoNotClutterTheRoom() throws Exception {
        var building = building(IBuilding.HANGAR, 3, List.of(CubeCoords.ZERO));
        WeaponMounted turret = (WeaponMounted) building.addEquipment(new ISLaserMedium(), 0);
        turret.setSponsonTurretMounted(true);
        var roof = building.addEquipment(MiscType.createHeatSink(), 1);
        building.getDesign().getEquipmentSpace().put(roof, List.of(new BuildingDesign.Position(CubeCoords.ZERO, 3)));
        manager(building);
        for (int floor = 0; floor < 3; floor++) {
            assertTrue(BuildingInteriorRules.features(building, ORIGIN, floor).empty());
        }
    }

    @Test
    void bookHangarExampleUsesTwoMpForEachHexAndDifferentPilotingRolls() throws Exception {
        var building = building(IBuilding.HANGAR, 2, List.of(CubeCoords.ZERO, NORTH));
        building.addEquipment(EquipmentType.get("Unspecified Building Equipment"), 0);
        building.addEquipment(new ISLaserMedium(), location(building, NORTH, 0));
        var manager = manager(building);
        var mek = mek(manager, ORIGIN.translated(3), 0);
        var path = new MovePath(manager.getGame(), mek);
        path.addStep(MoveStepType.FORWARDS);
        assertEquals(2, path.getMpUsed(), "outside wall: 1 + 2 - 1 = 2 MP");
        path.addStep(MoveStepType.FORWARDS);
        assertEquals(4, path.getMpUsed(), "inside machinery: 1 - 1 + 2 = 2 MP");
        assertEquals(2, mek.rollMovementInBuilding(building, 1, "", EntityMovementType.MOVE_WALK,
              ORIGIN.translated(3), ORIGIN, 0).getValue());
        assertEquals(3, mek.rollMovementInBuilding(building, 2, "", EntityMovementType.MOVE_WALK,
              ORIGIN, ORIGIN.translated(0), 0).getValue());
    }

    @Test
    void emptyHallInteriorIsPavedAndHasNoBuildingCheck() {
        var building = building(IBuilding.HANGAR, 2, List.of(CubeCoords.ZERO, NORTH));
        var manager = manager(building);
        var mek = mek(manager, ORIGIN, 0);
        var path = new MovePath(manager.getGame(), mek);
        path.addStep(MoveStepType.FORWARDS);
        assertEquals(1, path.getMpUsed());
        assertEquals(0, mek.checkMovementInBuilding(path.getLastStep(), null, ORIGIN.translated(0), ORIGIN));
        assertTrue(path.getLastStep().isPavementStep());
    }

    @ParameterizedTest
    @ValueSource(ints = { -21, -6, -3 })
    void undergroundUnitsKeepPhysicalAltitudeWhenMovingAndAiming(int base) throws Exception {
        var building = building(IBuilding.STANDARD, 3, List.of(CubeCoords.ZERO, NORTH));
        building.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        building.getDesign().setBaseLevel(base);
        var weapon = (WeaponMounted) building.addEquipment(new ISLaserMedium(), 1);
        var manager = manager(building);
        var mek = mek(manager, ORIGIN, base);
        assertTrue(Compute.isInBuilding(manager.getGame(), mek));
        assertFalse(manager.getGame().getHex(ORIGIN, 0).containsTerrain(Terrains.BLDG_ELEV));
        assertEquals(base + 1, new BuildingTarget(building, 1).getElevation());
        assertEquals(base + 1, building.getWeaponFiringHeight(weapon));
        var path = new MovePath(manager.getGame(), mek);
        path.addStep(MoveStepType.FORWARDS);
        assertEquals(base, path.getFinalElevation());
        assertTrue(path.isMoveLegal(), "a built passage must not become impassable earth");
        path.addStep(MoveStepType.FORWARDS);
        assertFalse(path.isMoveLegal(), "the next unbuilt hex is solid rock");
    }

    @Test
    void nestedAndStackedVolumesSurvivePublicationAndTargetedCollapse() {
        var underground = building(IBuilding.STANDARD, 3, List.of(CubeCoords.ZERO));
        underground.configureConstruction(BuildingType.HARDENED, IBuilding.FORTRESS, 3, 100, 0, List.of(CubeCoords.ZERO));
        underground.getDesign().setBaseLevel(-3);
        var manager = manager(underground);
        var surface = building(IBuilding.STANDARD, 2, List.of(CubeCoords.ZERO));
        deploy(manager, surface, 2, ORIGIN);
        var dome = building(IBuilding.CASTLE_BRIAN, 8, List.of(CubeCoords.ZERO, NORTH));
        dome.getDesign().setOpenSpace(true);
        deploy(manager, dome, 3, ORIGIN);
        var board = manager.getGame().getBoard(0);
        assertEquals(3, board.getBuildingsAt(ORIGIN).size());
        assertSame(underground, board.getBuildingAt(ORIGIN, -2));
        assertSame(surface, board.getBuildingAt(ORIGIN, 0));
        assertSame(dome, board.getBuildingAt(ORIGIN, 4));
        assertEquals(8, board.getHex(ORIGIN).terrainLevel(Terrains.BLDG_ELEV));
        Packet packet = new BuildingCollapseHandler(manager).createCollapseBuildingPacket(ORIGIN, dome);
        assertEquals(3, packet.getObject(2));
        board.collapseBuilding(dome, ORIGIN);
        assertTrue(surface.isIn(ORIGIN));
        assertTrue(underground.isIn(ORIGIN));
        assertEquals(2, board.getHex(ORIGIN).terrainLevel(Terrains.BLDG_ELEV));
    }

    @Test
    void openSpaceAllowsLongInteriorLosWithoutOrdinaryBuildingObstruction() {
        var footprint = java.util.stream.IntStream.range(0, 6).mapToObj(index -> new CubeCoords(0, -index, index)).toList();
        var dome = building(IBuilding.CASTLE_BRIAN, 8, footprint);
        dome.getDesign().setOpenSpace(true);
        var manager = manager(dome);
        var attacker = mek(manager, ORIGIN, 0);
        var target = mek(manager, ORIGIN.translated(0, 5), 0);
        assertTrue(LosEffects.calculateLOS(manager.getGame(), attacker, target).canSee());
        dome.getDesign().setOpenSpace(false);
        assertFalse(LosEffects.calculateLOS(manager.getGame(), attacker, target).canSee());
    }

    @ParameterizedTest
    @CsvSource({"10,5,45", "3,0,43", "0,0,40"})
    void fortressScalesAnAttackOnceBeforeArmorAbsorbsIt(int armor, int expectedArmor, int expectedCf) {
        var fortress = building(IBuilding.FORTRESS, 3, List.of(CubeCoords.ZERO));
        var manager = manager(fortress);
        fortress.setArmor(armor, ORIGIN);
        manager.damageBuilding(fortress, 10, ORIGIN);
        assertEquals(expectedArmor, fortress.getArmor(ORIGIN));
        assertEquals(expectedCf, fortress.getCurrentCF(ORIGIN));
    }
    @Test
    void sealedWallRequiresSuccessfulBreachBeforePassage() {
        var building = building(IBuilding.STANDARD, 3, List.of(CubeCoords.ZERO));
        building.getDesign().setEnvironmentalSealing(true);
        var manager = manager(building);
        var mek = mek(manager, ORIGIN.translated(3), 0);
        var handler = new BuildingEnvironmentHandler(manager);
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(2)).thenReturn(11);
            assertFalse(handler.canCross(mek, mek.getPosition(), 0, ORIGIN, 0, new Vector<>()));
            dice.when(() -> Compute.d6(2)).thenReturn(12);
            assertTrue(handler.canCross(mek, mek.getPosition(), 0, ORIGIN, 0, new Vector<>()));
        }
        assertTrue(building.getBuildingRuntimeState().isBreached(CubeCoords.ZERO));
    }

    @Test
    void breachKillsTheExposedBuildingsOwnGunners() {
        var building = building(IBuilding.STANDARD, 3, List.of(CubeCoords.ZERO));
        building.getDesign().setEnvironmentalSealing(true);
        var manager = manager(building);
        manager.getGame().getPlanetaryConditions().setAtmosphere(Atmosphere.VACUUM);
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(2)).thenReturn(12);
            new BuildingEnvironmentHandler(manager).attemptBreach(building, ORIGIN, 0, new Vector<>());
        }
        assertTrue(building.hasDeadGunners(0));
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void openSpaceAtmosphereUsesFollowingTurnEndRegardlessOfOptionalCollapseSettings(boolean floors, boolean collapse) {
        var dome = building(IBuilding.CASTLE_BRIAN, 8, List.of(CubeCoords.ZERO, NORTH));
        dome.getDesign().setOpenSpace(true);
        var manager = manager(dome);
        var game = manager.getGame();
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_CF).setValue(floors);
        game.getOptions().getOption(OptionsConstants.ADVANCED_BUILDING_EXPANDED_COLLAPSE).setValue(collapse);
        game.getPlanetaryConditions().setAtmosphere(Atmosphere.VACUUM);
        var infantry = new ConvInfantry();
        infantry.setId(10);
        infantry.setDeployed(true);
        infantry.setOwner(dome.getOwner());
        infantry.setPosition(ORIGIN.translated(0));
        game.addEntity(infantry);
        doReturn(new Vector<Report>()).when(manager).destroyEntity(eq(infantry), anyString());
        var handler = new BuildingEnvironmentHandler(manager);
        handler.collapsed(dome, ORIGIN, 8, new Vector<>());
        handler.endPhase(new Vector<>());
        verify(manager, never()).destroyEntity(eq(infantry), anyString());
        assertTrue(BuildingRuntimeState.protectsFromEnvironment(game, infantry));
        game.setRoundCount(2);
        game.setPhase(GamePhase.END);
        handler.endPhase(new Vector<>());
        verify(manager).destroyEntity(eq(infantry), eq("breached building"));
    }

    @ParameterizedTest
    @CsvSource({"1,1", "5,1", "6,2", "16,4"})
    void underwaterOpenSpaceFloodsAtPressureRateAndPersists(int cover, int rate) {
        var dome = building(IBuilding.CASTLE_BRIAN, 12, List.of(CubeCoords.ZERO, NORTH));
        dome.getDesign().setOpenSpace(true);
        dome.getDesign().setSite(BuildingDesign.Site.UNDERWATER);
        dome.getDesign().setDepth(cover);
        var manager = manager(dome);
        var handler = new BuildingEnvironmentHandler(manager);
        handler.collapsed(dome, ORIGIN, 12, new Vector<>());
        handler.endPhase(new Vector<>());
        assertEquals(0, dome.getBuildingRuntimeState().getOpenFloodDepth());
        manager.getGame().setRoundCount(2);
        handler.endPhase(new Vector<>());
        assertEquals(rate, dome.getBuildingRuntimeState().getOpenFloodDepth());
        assertTrue(dome.getBuildingRuntimeState().isFlooded(NORTH, rate - 1));
        assertFalse(dome.getBuildingRuntimeState().isFlooded(NORTH, rate));
        var xstream = SerializationHelper.getLoadSaveGameXStream();
        var loaded = (BuildingEntity) xstream.fromXML(SerializationHelper.getSaveGameXStream().toXML(dome));
        assertEquals(rate, loaded.getBuildingRuntimeState().getOpenFloodDepth());
        assertTrue(loaded.getBuildingRuntimeState().advanceOpenSpaceFlood(loaded, 3));
        assertEquals(rate * 2, loaded.getBuildingRuntimeState().getOpenFloodDepth());
    }

    @Test
    void caveInUsesOriginalCapitalCfAndProtectsOtherBuildings() {
        assertEquals(900, OpenSpaceCollapseHandler.debrisDamage(100, 10, 7, 7), "TO:AR p.137 worked example");
        var footprint = new java.util.ArrayList<CubeCoords>();
        for (int q = -2; q <= 2; q++) {
            for (int r = -2; r <= 2; r++) {
                if (Math.abs(q + r) <= 2) { footprint.add(new CubeCoords(q, r, -q-r)); }
            }
        }
        var dome = building(IBuilding.CASTLE_BRIAN, 7, footprint);
        dome.configureConstruction(BuildingType.HARDENED, IBuilding.CASTLE_BRIAN, 7, 100, 0, footprint);
        dome.getDesign().setOpenSpace(true);
        dome.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        dome.getDesign().setDepth(7);
        var manager = manager(dome);
        var inner = building(IBuilding.STANDARD, 3, List.of(CubeCoords.ZERO));
        inner.getDesign().setBaseLevel(-14);
        deploy(manager, inner, 2, ORIGIN.translated(0));
        var victim = mek(manager, ORIGIN, -14);
        doReturn(new Vector<Report>()).when(manager).damageEntity(eq(victim), any(), anyInt());
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(Compute::d6).thenReturn(2);
            new BuildingCollapseHandler(manager).collapseBuilding(dome, manager.getGame().getPositionMapMulti(),
                  ORIGIN, false, new Vector<>());
        }
        verify(manager, times(180)).damageEntity(eq(victim), any(), eq(5));
        assertTrue(inner.isIn(ORIGIN.translated(0)));
        assertTrue(dome.isIn(ORIGIN.translated(0)), "the nested building's hex is exempt from adjacent cave-in");
        assertTrue(dome.isIn(ORIGIN.translated(0, 2)), "outer walls are excluded");
    }

    @ParameterizedTest
    @CsvSource({"2,6", "3,8", "4,8", "5,10", "6,10", "7,12"})
    void underLakeCaveInUsesGroundCoverBreachThreshold(int cover, int target) {
        var dome = building(IBuilding.CASTLE_BRIAN, 7, List.of(CubeCoords.ZERO));
        dome.getDesign().setOpenSpace(true);
        dome.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        dome.getDesign().setBaseLevel(-7 - cover - 4);
        var manager = manager(dome);
        manager.getGame().getHex(ORIGIN, 0).addTerrain(new Terrain(Terrains.WATER, 4));
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(2)).thenReturn(target);
            new BuildingEnvironmentHandler(manager).collapsed(dome, ORIGIN, 7, new Vector<>());
        }
        assertTrue(dome.getBuildingRuntimeState().advanceOpenSpaceFlood(dome, 2));
        assertTrue(dome.getBuildingRuntimeState().getOpenFloodDepth() > 0);
    }
    private MobileStructure mobile(int height, List<CubeCoords> footprint) {
        var mobile = new MobileStructure(BuildingType.HEAVY, IBuilding.HANGAR);
        mobile.configureConstruction(BuildingType.HEAVY, IBuilding.HANGAR, height, 45, 0, footprint);
        return mobile;
    }

    @ParameterizedTest
    @CsvSource({"1,false", "2,false", "3,false", "1,true", "2,true", "3,true"})
    void mobileUndercarriageUsesLightWoodsLosRatherThanSolidBuilding(int hexes, boolean diagram) {
        var footprint = java.util.stream.IntStream.range(0, hexes).mapToObj(i -> new CubeCoords(0, -i, i)).toList();
        var mobile = mobile(3, footprint);
        var manager = manager(mobile);
        manager.getGame().getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1).setValue(diagram);
        var attacker = mek(manager, ORIGIN.translated(3), 0);
        var target = mek(manager, ORIGIN.translated(0, hexes), 0);
        var los = LosEffects.calculateLOS(manager.getGame(), attacker, target);
        assertEquals(hexes, los.getLightWoods());
        assertEquals(hexes < 3, los.canSee());
    }

    @Test
    void mobileWeaponsAndOccupantsUseTheLocalWaterBed() throws Exception {
        var mobile = mobile(3, List.of(CubeCoords.ZERO, NORTH));
        var weapon = (WeaponMounted) mobile.addEquipment(new ISLaserMedium(), 1);
        var manager = manager(mobile);
        manager.getGame().getHex(ORIGIN, 0).addTerrain(new Terrain(Terrains.WATER, 4));
        assertEquals(-2, BuildingElevation.base(mobile, ORIGIN));
        assertEquals(-1, mobile.getWeaponFiringHeight(weapon));
        assertEquals(-1, new BuildingTarget(mobile, 1).getElevation());
        assertEquals(2, BuildingElevation.base(mobile, ORIGIN.translated(0)));
        weapon.setSponsonTurretMounted(true);
        assertEquals(0, mobile.getWeaponFiringHeight(weapon), "base -2 + uppermost floor2");
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 8})
    void storedSurvivalGearTravelsInTheEditorPacketAndUsesEightPlus(int roll) throws Exception {
        var dome = building(IBuilding.CASTLE_BRIAN, 8, List.of(CubeCoords.ZERO, NORTH));
        dome.getDesign().setOpenSpace(true);
        var manager = manager(dome);
        var game = manager.getGame();
        game.getPlanetaryConditions().setAtmosphere(Atmosphere.VACUUM);
        var infantry = new ConvInfantry();
        infantry.setId(10);
        infantry.setDeployed(true);
        infantry.setOwner(dome.getOwner());
        infantry.setPosition(ORIGIN);
        infantry.setSpaceSuit(true);
        game.addEntity(infantry);
        DamageEditSpec edits = new DamageEditSpec();
        edits.entityId = infantry.getId();
        edits.survivalGearStored = true;
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new java.io.ObjectOutputStream(bytes)) { out.writeObject(edits); }
        try (var in = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            new DamageEditApplier(infantry, (DamageEditSpec) in.readObject()).applyToEntity();
        }
        assertTrue(infantry.isSurvivalGearStored());
        doReturn(new Vector<Report>()).when(manager).destroyEntity(eq(infantry), anyString());
        var handler = new BuildingEnvironmentHandler(manager);
        handler.collapsed(dome, ORIGIN, 8, new Vector<>());
        game.setRoundCount(2);
        try (var dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.d6(2)).thenReturn(roll);
            handler.endPhase(new Vector<>());
        }
        assertFalse(infantry.isSurvivalGearStored());
        verify(manager, times(roll < 8 ? 1 : 0)).destroyEntity(eq(infantry), anyString());
    }

    @Test
    void largePortalRequiresPreviousTurnOpeningAndPermitsActualTunnelMovement() {
        var tunnel = building(IBuilding.CASTLE_BRIAN, 7,
              List.of(CubeCoords.ZERO, new CubeCoords(0, 1, -1), new CubeCoords(0, 2, -2),
                    new CubeCoords(1, 0, -1), new CubeCoords(1, 1, -2), new CubeCoords(1, 2, -3)));
        tunnel.getDesign().setOpenSpace(true);
        tunnel.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        tunnel.getDesign().setBaseLevel(-8);
        var manager = manager(tunnel);
        tunnel.getCoordsList().forEach(c -> manager.getGame().getHex(c, 0).setLevel(8));
        Coords gate = ORIGIN.translated(0);
        var portal = mobile(3, List.of(CubeCoords.ZERO, new CubeCoords(1, 0, -1)));
        portal.getDesign().setPortalHex2(CubeCoords.ZERO);
        portal.getDesign().setPortalHex3(new CubeCoords(1, 0, -1));
        portal.getDesign().setOpenSpace(true);
        deploy(manager, portal, 2, gate);
        assertEquals(tunnel.getId(), portal.getBuildingRuntimeState().getPortalConnection().complexId());
        var movement = new MobileStructureMovementHandler(manager);
        movement.relocate(portal, gate.translated(1, 2), 0, 0);
        var mover = mobile(3, List.of(CubeCoords.ZERO));
        mover.setFacing(3);
        deploy(manager, mover, 3, gate);
        assertFalse(MobileStructurePortalRules.canEnter(mover, tunnel, List.of(ORIGIN), 3));
        manager.getGame().setRoundCount(2);
        assertTrue(MobileStructurePortalRules.canEnter(mover, tunnel, List.of(ORIGIN), 3));
        var path = new MovePath(manager.getGame(), mover);
        path.addStep(MoveStepType.FORWARDS);
        movement.process(mover, path);
        assertEquals(ORIGIN, mover.getPosition());
        assertEquals(-6, BuildingElevation.base(mover, ORIGIN));
        assertTrue(tunnel.isIn(ORIGIN), "entering a permitted tunnel must preserve its structure");
        assertEquals(45, tunnel.getCurrentCF(ORIGIN));
        assertEquals(2, manager.getGame().getBoard(0).getBuildingsAt(ORIGIN).size());
        var xstream = SerializationHelper.getLoadSaveGameXStream();
        var loaded = (MobileStructure) xstream.fromXML(SerializationHelper.getSaveGameXStream().toXML(portal));
        assertEquals(portal.getBuildingRuntimeState().getPortalConnection(), loaded.getBuildingRuntimeState().getPortalConnection());
    }
    @Test
    void realMekBayRespectsLinkedDoorsAndDamageAndRestoresUsabilityAfterUnlink() throws Exception {
        var first = mobile(3, List.of(CubeCoords.ZERO));
        first.addEquipment(EquipmentType.get("Modular Structure Linkage"), 0);
        var bay = new megamek.common.bays.MekBay(4, 1, 7);
        first.addTransporter(bay);
        first.getDesign().getBayDoors().add(new BuildingDesign.BayDoor(7, new BuildingDesign.Position(CubeCoords.ZERO, 0), 0));
        var manager = manager(first);
        var second = mobile(3, List.of(CubeCoords.ZERO));
        second.addEquipment(EquipmentType.get("Modular Structure Linkage"), 0);
        deploy(manager, second, 2, ORIGIN.translated(1));
        var passenger = mek(manager, ORIGIN.translated(3), 0);
        assertTrue(bay.canLoad(passenger));
        assertEquals(2, bay.getSafeLaunchRate());
        assertTrue(megamek.common.moves.MobileStructureLinkage.link(first, second));
        assertFalse(bay.canLoad(passenger));
        assertFalse(bay.canUnloadUnits());
        assertEquals(0, bay.getSafeLaunchRate());
        assertEquals(1, bay.getCurrentDoors(), "linkage is an obstruction, not damage");
        assertTrue(megamek.common.moves.MobileStructureLinkage.unlink(first, second));
        assertTrue(bay.canLoad(passenger));
        bay.setCurrentDoors(0);
        assertFalse(bay.canLoad(passenger));
        assertEquals(0, bay.getSafeLaunchRate());
    }

    @Test
    void bayDoorsAndPortalTemplatesRoundTripAndRemapWithoutInventingPlacements() throws Exception {
        var mobile = mobile(3, List.of(CubeCoords.ZERO, NORTH));
        mobile.addTransporter(new megamek.common.bays.MekBay(2, 1, 7));
        mobile.getDesign().getBayDoors().add(new BuildingDesign.BayDoor(7, new BuildingDesign.Position(NORTH, 1), 0));
        mobile.getDesign().setPortalHex2(CubeCoords.ZERO);
        mobile.getDesign().setPortalHex3(NORTH);
        var block = new megamek.common.util.BuildingBlock();
        megamek.common.loaders.BuildingDesignCodec.write(block, mobile);
        var loaded = mobile(3, List.of(CubeCoords.ZERO, NORTH));
        loaded.addTransporter(new megamek.common.bays.MekBay(2, 1, 7));
        megamek.common.loaders.BuildingDesignCodec.read(block, loaded);
        assertEquals(mobile.getDesign().getBayDoors(), loaded.getDesign().getBayDoors());
        assertEquals(NORTH, loaded.getDesign().getPortalHex3());
        loaded.getDesign().remap(position -> position.hex().equals(NORTH) ? null : position, side -> side);
        assertTrue(loaded.getDesign().getBayDoors().isEmpty());
        assertNull(loaded.getDesign().getPortalHex3());
        var legacy = mobile(3, List.of(CubeCoords.ZERO, NORTH));
        megamek.common.loaders.BuildingDesignCodec.read(new megamek.common.util.BuildingBlock(), legacy);
        assertTrue(legacy.getDesign().getBayDoors().isEmpty());
        assertNull(legacy.getDesign().getPortalHex2());
    }

    @Test
    void movingElevatorDoesNotMoveTheSeparateUndergroundShaft() {
        var mobile = mobile(3, List.of(CubeCoords.ZERO));
        mobile.getDesign().getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 20, Map.of(0, 1, 1, 1, 2, 1)));
        var manager = manager(mobile);
        var basement = building(IBuilding.STANDARD, 2, List.of(CubeCoords.ZERO));
        basement.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        basement.getDesign().setBaseLevel(-3);
        basement.getDesign().getElevators().add(new BuildingDesign.Elevator(CubeCoords.ZERO, 20, Map.of(0, 1, 1, 1)));
        deploy(manager, basement, 2, ORIGIN);
        assertEquals(2, manager.getGame().getIndustrialElevators().size());
        new MobileStructureMovementHandler(manager).relocate(mobile, ORIGIN.translated(0), 0, 0);
        var lifts = manager.getGame().getIndustrialElevators();
        assertEquals(2, lifts.size());
        assertEquals(ORIGIN, lifts.stream().filter(lift -> lift.getBuildingId() == basement.getId()).findFirst().orElseThrow().getCoords());
        assertEquals(ORIGIN.translated(0), lifts.stream().filter(lift -> lift.getBuildingId() == mobile.getId()).findFirst().orElseThrow().getCoords());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void portalTunnelEquipmentUsesRotatedTemplateRolesAndChecksEveryFiveHexes(int facing) throws Exception {
        var portal = mobile(3, List.of(CubeCoords.ZERO, new CubeCoords(1, 0, -1)));
        portal.getDesign().setOpenSpace(true);
        portal.getDesign().setPortalHex2(CubeCoords.ZERO);
        portal.getDesign().setPortalHex3(new CubeCoords(1, 0, -1));
        portal.addEquipment(MiscType.createHeatSink(), 0);
        var manager = manager(portal);
        portal.setFacing(facing);
        int rear = (facing + 3) % 6;
        var tunnelHexes = java.util.stream.IntStream.rangeClosed(1, 10)
              .mapToObj(distance -> ORIGIN.translated(rear, distance).toCube().subtract(ORIGIN.toCube())).toList();
        var tunnel = building(IBuilding.CASTLE_BRIAN, 7, tunnelHexes);
        tunnel.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        tunnel.getDesign().setOpenSpace(true);
        deploy(manager, tunnel, 2, ORIGIN);
        assertFalse(MobileStructurePortalRules.tunnelEquipmentMatches(portal, tunnel, List.of(ORIGIN), facing));
        for (int distance : new int[] {4, 9}) {
            var hex = ORIGIN.translated(rear, distance).toCube().subtract(ORIGIN.toCube());
            tunnel.addEquipment(MiscType.createHeatSink(), location(tunnel, hex, 0));
        }
        assertTrue(MobileStructurePortalRules.tunnelEquipmentMatches(portal, tunnel, List.of(ORIGIN), facing));
        assertEquals(1, portal.getEquipment().size(), "validation never creates free equipment");
    }
    @Test
    void partialUndergroundCollapseFallsOnePhysicalLevelRatherThanTheDepthBelowGround() {
        var basement = building(IBuilding.STANDARD, 3, List.of(CubeCoords.ZERO, NORTH));
        basement.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        basement.getDesign().setBaseLevel(-4);
        var manager = manager(basement);
        basement.enableExpandedCF();
        var occupant = mek(manager, ORIGIN, -2);
        occupant.setWeight(10);
        doReturn(new Vector<Report>()).when(manager).doEntityFallsInto(eq(occupant), anyInt(), any(), any(), any(), anyBoolean(), anyInt());
        basement.setCurrentCF(0, ORIGIN, 1);
        assertTrue(new BuildingCollapseHandler(manager).resolveExpandedCollapse(basement, ORIGIN, new Vector<>()));
        assertEquals(-3, occupant.getElevation());
        verify(manager).doEntityFallsInto(eq(occupant), eq(-2), eq(ORIGIN), eq(ORIGIN), any(), eq(true), eq(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {50, 100})
    void strongerUndergroundFoundationUsesItsOwnCfInsteadOfTheSurfaceCf(int tons) {
        var surface = building(IBuilding.STANDARD, 4, List.of(CubeCoords.ZERO, NORTH));
        surface.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 4, 25, 0, List.of(CubeCoords.ZERO, NORTH));
        var manager = manager(surface);
        var foundation = building(IBuilding.STANDARD, 2, List.of(CubeCoords.ZERO, NORTH));
        foundation.configureConstruction(BuildingType.HEAVY, IBuilding.STANDARD, 2, 90, 0, List.of(CubeCoords.ZERO, NORTH));
        foundation.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        foundation.getDesign().setBaseLevel(-2);
        deploy(manager, foundation, 2, ORIGIN);
        var occupant = mek(manager, ORIGIN, 0);
        occupant.setWeight(tons);
        doReturn(new Vector<Report>()).when(manager).damageEntity(eq(occupant), any(), anyInt());
        doReturn(new Vector<Report>()).when(manager).doEntityFallsInto(eq(occupant), anyInt(), any(), any(), any(), anyBoolean(), anyInt());
        assertTrue(BuildingFoundationRules.stronger(foundation, surface, ORIGIN));
        new BuildingCollapseHandler(manager).checkForCollapse(surface, ORIGIN, false, new Vector<>());
        assertEquals(tons > 90 ? -2 : 0, occupant.getElevation());
        assertEquals(tons <= 90, foundation.isIn(ORIGIN));
    }

    @Test
    void surfaceTopDownCollapseHalvesFoundationAndOccupantDamageOnceBelowGround() {
        var surface = building(IBuilding.STANDARD, 4, List.of(CubeCoords.ZERO, NORTH));
        surface.configureConstruction(BuildingType.HEAVY, IBuilding.STANDARD, 4, 60, 0, List.of(CubeCoords.ZERO, NORTH));
        var manager = manager(surface);
        var foundation = building(IBuilding.STANDARD, 2, List.of(CubeCoords.ZERO, NORTH));
        foundation.configureConstruction(BuildingType.HEAVY, IBuilding.STANDARD, 2, 90, 0, List.of(CubeCoords.ZERO, NORTH));
        foundation.getDesign().setSite(BuildingDesign.Site.UNDERGROUND);
        foundation.getDesign().setBaseLevel(-2);
        deploy(manager, foundation, 2, ORIGIN);
        surface.enableExpandedCF();
        foundation.enableExpandedCF();
        var occupant = mek(manager, ORIGIN, -1);
        occupant.setWeight(10);
        doReturn(new Vector<Report>()).when(manager).damageEntity(eq(occupant), any(), anyInt());
        surface.setCurrentCF(0, ORIGIN, 0);
        new BuildingCollapseHandler(manager).resolveExpandedCollapse(surface, ORIGIN, new Vector<>());
        assertEquals(60, foundation.getCurrentCF(ORIGIN, 1), "three falling 60-CF levels /3 /2 =30");
        verify(manager).damageEntity(eq(occupant), any(), eq(5));
        verify(manager).damageEntity(eq(occupant), any(), eq(4));
        assertEquals(-1, occupant.getElevation());
    }

    @Test
    void anOpenSpaceEnclosureCannotContainAnInnerBuildingAboveItsRoof() {
        var enclosure = building(IBuilding.CASTLE_BRIAN, 3, List.of(CubeCoords.ZERO, NORTH));
        enclosure.getDesign().setOpenSpace(true);
        manager(enclosure);
        var inner = building(IBuilding.STANDARD, 4, List.of(CubeCoords.ZERO));
        inner.setPosition(ORIGIN);
        assertFalse(BuildingElevation.canCoexist(inner, enclosure));
    }
}
