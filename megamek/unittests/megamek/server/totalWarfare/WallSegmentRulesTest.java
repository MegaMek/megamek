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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Player;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.Compute;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.units.*;
import megamek.common.util.SerializationHelper;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Rules-level probes through deployment, movement, attacks and serialization; TO:AR pp.113-116. */
class WallSegmentRulesTest {
    private static final Coords ORIGIN = new Coords(4, 4);

    private static class LocalManager extends TWGameManager {
        @Override public void send(Packet packet) { }
        @Override public void send(int id, Packet packet) { }
    }

    @BeforeAll
    static void equipment() { EquipmentType.initializeTypes(); }

    private TWGameManager manager(int classification, int height, int armor, int sides) {
        var manager = spy(new LocalManager());
        doNothing().when(manager).entityUpdate(anyInt());
        doNothing().when(manager).sendChangedHex(any(Coords.class), anyInt());
        var game = manager.getGame();
        game.setBoard(BoardLoader.initializeBoard("size 10 10\nend\n"));
        game.addPlayer(0, new Player(0, "Owner"));
        BuildingType type = classification == IBuilding.FENCE ? BuildingType.LIGHT : BuildingType.MEDIUM;
        var wall = new BuildingEntity(type, classification);
        wall.setChassis("Wall review");
        wall.setModel("Segments");
        wall.setId(1);
        wall.setOwner(game.getPlayer(0));
        wall.setEngine(new Engine(0, Engine.NONE, 0));
        wall.configureConstruction(type, classification, height, classification == IBuilding.FENCE ? 1 : 40,
              armor, List.of(CubeCoords.ZERO));
        wall.getDesign().getWallSides().put(CubeCoords.ZERO, sides);
        game.addEntity(wall);
        wall.setPosition(ORIGIN);
        wall.setDeployed(true);
        wall.updateBuildingEntityHexes(0, manager);
        game.setPhase(GamePhase.MOVEMENT);
        return manager;
    }

    private BipedMek mek(Game game, Coords position) {
        var unit = new BipedMek();
        unit.setId(game.getEntitiesVector().size() + 1);
        unit.setOwner(game.getPlayer(0));
        unit.setWeight(20);
        unit.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        unit.setOriginalWalkMP(10);
        unit.setPosition(position);
        unit.setDeployed(true);
        unit.setClimbMode(false);
        for (int location = 0; location < unit.locations(); location++) {
            unit.initializeInternal(10, location);
            unit.initializeArmor(20, location);
        }
        game.addEntity(unit);
        return unit;
    }

    private WallRules.Segment north(Game game) { return WallRules.at(game, 0, ORIGIN, 0).getFirst(); }

    @Test
    void publicationLeavesGroundAndAdjacentVolumesUntouched() {
        var manager = manager(IBuilding.WALL, 2, 0, 9);
        var game = manager.getGame();
        assertNull(game.getBoard(0).getBuildingAt(ORIGIN));
        assertFalse(game.getBoard(0).getHex(ORIGIN).containsTerrain(Terrains.BUILDING));
        assertFalse(game.getBoard(0).getHex(ORIGIN).containsTerrain(Terrains.BLDG_ELEV));
        assertEquals(2, WallRules.beside(game, 0, ORIGIN).size());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5 })
    void movementPaysOnlyForTheSideActuallyCrossed(int side) {
        var manager = manager(IBuilding.WALL, 2, 0, 1 << side);
        var game = manager.getGame();
        var unit = mek(game, ORIGIN.translated(side));
        unit.setFacing((side + 3) % 6);
        var through = new MovePath(game, unit).addStep(MoveStepType.FORWARDS);
        assertEquals(3, through.getMpUsed());
        assertNotEquals(EntityMovementType.MOVE_ILLEGAL, through.getLastStep().getMovementType(false));
        unit.setPosition(ORIGIN.translated((side + 2) % 6));
        unit.setFacing((side + 5) % 6);
        var open = new MovePath(game, unit).addStep(MoveStepType.FORWARDS);
        assertEquals(1, open.getMpUsed());
    }

    @Test
    void crossingDamagesOnlyOneSideAndDoesNotDamageInfantryBesideIt() {
        var manager = manager(IBuilding.WALL, 2, 0, 9);
        var game = manager.getGame();
        var unit = mek(game, ORIGIN.translated(0));
        doReturn(0).when(manager).doSkillCheckWhileMoving(any(), anyInt(), any(), any(), any(), anyBoolean(), any());
        manager.passWallSegments(unit, unit.getPosition(), ORIGIN, 0, 0, 1, false, EntityMovementType.MOVE_WALK);
        assertEquals(38, north(game).cf());
        assertEquals(40, WallRules.at(game, 0, ORIGIN, 3).getFirst().cf());
        assertTrue(manager.damageInfantryIn(north(game).building(), 20, ORIGIN).isEmpty());
    }

    @Test
    void armorAndDestroyedSegmentsAreIndependent() {
        var manager = manager(IBuilding.WALL, 2, 5, 9);
        var game = manager.getGame();
        var north = north(game);
        var south = WallRules.at(game, 0, ORIGIN, 3).getFirst();
        manager.damageWall(north, 8, false);
        assertEquals(37, north.cf());
        assertEquals(0, north.armor());
        assertEquals(40, south.cf());
        assertEquals(5, south.armor());
        manager.damageWall(north, 37, false);
        assertEquals(1, WallRules.segments(game, 0).size());
        var unit = mek(game, ORIGIN.translated(0));
        assertEquals(0, WallRules.movementCost(unit, unit.getPosition(), ORIGIN, 0, 0, false));
        assertEquals(40, south.cf());
    }

    @Test
    void fencesAffectOnlyConventionalGroundInfantryAndNeverSupportOccupants() {
        var manager = manager(IBuilding.FENCE, 3, 0, 1);
        var game = manager.getGame();
        var unit = mek(game, ORIGIN.translated(0));
        assertEquals(0, WallRules.movementCost(unit, unit.getPosition(), ORIGIN, 0, 0, false));
        assertFalse(WallRules.canClimbStep(unit, north(game).target(null), ORIGIN, 1));
        var infantry = new ConvInfantry();
        infantry.setGame(game);
        infantry.setBoardId(0);
        infantry.setPosition(ORIGIN.translated(0));
        infantry.setMovementMode(EntityMovementMode.INF_LEG);
        assertFalse(WallRules.canCross(infantry, infantry.getPosition(), ORIGIN, 0, 0, false));
        assertFalse(WallRules.canCross(infantry, infantry.getPosition(), ORIGIN, 0, 0, true),
              "climb mode alone does not replace the required individual climbing actions");
        assertTrue(WallRules.canClimbStep(infantry, north(game).target(null), ORIGIN, 1));
        assertFalse(WallRules.canStandOn(infantry, north(game).target(null), ORIGIN, 3));
        infantry.setMovementMode(EntityMovementMode.INF_MOTORIZED);
        assertEquals(1, WallRules.movementCost(infantry, infantry.getPosition(), ORIGIN, 0, 0, false));
        infantry.setMovementMode(EntityMovementMode.INF_JUMP);
        assertEquals(0, WallRules.movementCost(infantry, infantry.getPosition(), ORIGIN, 0, 0, false));
    }

    @Test
    void sightLineTestsTheEdgeInsteadOfTheWholeHex() {
        var manager = manager(IBuilding.WALL, 2, 0, 1);
        var game = manager.getGame();
        var attacker = mek(game, ORIGIN.translated(0, 2));
        var target = mek(game, ORIGIN);
        assertFalse(LosEffects.calculateLOS(game, attacker, target).canSee());
        attacker.setPosition(ORIGIN.translated(2, 2));
        assertTrue(LosEffects.calculateLOS(game, attacker, target).canSee());
        attacker.setPosition(ORIGIN.translated(0, 2));
        target.setElevation(3);
        attacker.setElevation(3);
        assertTrue(LosEffects.calculateLOS(game, attacker, target).canSee());
    }

    @Test
    void segmentTargetsRoundTripWithoutMergingDirectionsAndUseNearestBorderHex() throws Exception {
        var manager = manager(IBuilding.WALL, 2, 0, 9);
        var game = manager.getGame();
        var attacker = mek(game, ORIGIN.translated(0, 2));
        attacker.setFacing(3);
        attacker.setSecondaryFacing(3);
        var north = north(game).target(attacker.getPosition());
        var south = WallRules.at(game, 0, ORIGIN, 3).getFirst().target(attacker.getPosition());
        assertNotEquals(north.getTargetType(), south.getTargetType());
        assertEquals(1, Compute.effectiveDistance(game, attacker, north));
        assertEquals(2, Compute.effectiveDistance(game, attacker, south));
        var laser = attacker.addEquipment(new ISLaserMedium(), 0);
        var action = new WeaponAttackAction(attacker.getId(), north.getTargetType(), north.getId(), attacker.getEquipmentNum(laser));
        var decoded = (WallTarget) copy(action).getTarget(game);
        assertEquals(ORIGIN.translated(0), decoded.getPosition());
        assertEquals(north.side(), decoded.side());
        assertTrue(LosEffects.calculateLOS(game, attacker, decoded).canSee(), "the targeted wall must not block itself");
        assertNotEquals(Integer.MAX_VALUE, action.toHit(game).getValue(), action.toHit(game).getDesc());
    }

    @Test
    void segmentDamageSurvivesJavaAndXmlSaveAndCopiesWithoutAliasing() throws Exception {
        var manager = manager(IBuilding.WALL, 2, 5, 9);
        var game = manager.getGame();
        manager.damageWall(north(game), 8, false);
        var wall = north(game).building();
        var received = copy(wall);
        game.getBoard(0).updateBuilding(received);
        received.getWallSegmentState().setCF(CubeCoords.ZERO, 0, 1);
        assertEquals(37, north(game).cf());
        var serialized = SerializationHelper.getSaveGameXStream().toXML(wall);
        var restored = (BuildingEntity) SerializationHelper.getLoadSaveGameXStream().fromXML(serialized);
        assertEquals(37, restored.getWallSegmentState().getCF(CubeCoords.ZERO, 0));
        assertEquals(5, restored.getWallSegmentState().getArmor(CubeCoords.ZERO, 3));
    }

    @Test
    void namedWallMountCostsWalkingMPAndSurvivesSave() throws Exception {
        var manager = manager(IBuilding.WALL, 2, 0, 9);
        var game = manager.getGame();
        var unit = mek(game, ORIGIN);
        var wall = north(game).target(ORIGIN);
        var path = new MovePath(game, unit).addStep(MoveStepType.WALL_ASCEND, wall)
              .addStep(MoveStepType.WALL_ASCEND, wall);
        assertEquals(4, path.getMpUsed());
        assertEquals(2, path.getFinalElevation());
        assertNotEquals(EntityMovementType.MOVE_ILLEGAL, path.getLastStep().getMovementType(false));
        unit.setOccupiedWall(path.getLastStep().getOccupiedWall());
        unit.setElevation(2);
        var saved = copy(unit);
        assertEquals(wall.getId(), saved.getOccupiedWall().getId());
        assertEquals(wall.getTargetType(), saved.getOccupiedWall().getTargetType());
    }

    @Test
    void jumpingCanSelectTheNamedWallTopButNeverAFence() throws Exception {
        var manager = manager(IBuilding.WALL, 2, 0, 1);
        var game = manager.getGame();
        var unit = mek(game, ORIGIN.translated(0, 2));
        unit.setOriginalJumpMP(3);
        for (int n = 0; n < 3; n++) {
            unit.addEquipment(EquipmentType.get(megamek.common.equipment.EquipmentTypeLookup.JUMP_JET), BipedMek.LOC_CENTER_TORSO);
        }
        unit.setFacing(3);
        var wall = north(game).target(unit.getPosition());
        var path = new MovePath(game, unit).addStep(MoveStepType.START_JUMP)
              .addStep(MoveStepType.FORWARDS).addStep(MoveStepType.WALL_LAND, wall);
        assertEquals(2, path.getFinalElevation());
        assertEquals(1, path.getMpUsed());
        assertEquals(EntityMovementType.MOVE_JUMP, path.getLastStepMovementType());
        assertNotNull(path.getLastStep().getOccupiedWall());
        unit.setPosition(path.getFinalCoords());
        unit.setElevation(2);
        unit.setOccupiedWall(path.getLastStep().getOccupiedWall());
        assertTrue(manager.climbWallSegment(unit, path.getLastStep(), null));
        assertEquals(40, north(game).cf(), "a jump landing checks support but does not inflict climbing damage");
        assertTrue(unit.isElevationValid(2, game.getBoard().getHex(unit.getPosition())));
    }

    @Test
    void wallClimbUsesTheUnmodifiedClimbingRollAndCollapsesOnlyItsOwnSupport() {
        var manager = manager(IBuilding.WALL, 3, 0, 9);
        var game = manager.getGame();
        var unit = mek(game, ORIGIN);
        var target = north(game).target(ORIGIN);
        var step = new MovePath(game, unit).addStep(MoveStepType.WALL_ASCEND, target).getLastStep();
        doReturn(0).when(manager).doSkillCheckWhileMoving(any(), anyInt(), any(), any(), any(), anyBoolean(), any());
        unit.setElevation(1);
        unit.setOccupiedWall(target);
        assertTrue(manager.climbWallSegment(unit, step, null));
        var roll = org.mockito.ArgumentCaptor.forClass(megamek.common.rolls.PilotingRollData.class);
        verify(manager).doSkillCheckWhileMoving(eq(unit), eq(0), eq(ORIGIN), eq(ORIGIN), roll.capture(), eq(false), any());
        assertEquals(unit.getBasePilotingRoll(EntityMovementType.MOVE_WALK).getValue(), roll.getValue().getValue(),
              "AR p20 +1 is for other rolls while climbing, not the climbing roll itself");
        var bystander = mek(game, ORIGIN);
        manager.damageWall(north(game), 40, true);
        assertNull(unit.getOccupiedWall());
        assertFalse(bystander.isProne());
        assertEquals(40, WallRules.at(game, 0, ORIGIN, 3).getFirst().cf());
    }

    @SuppressWarnings("unchecked")
    private <T> T copy(T value) throws Exception {
        var buffer = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(buffer)) { output.writeObject(value); }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) { return (T) input.readObject(); }
    }

    @Test void oppositeHexCannotPublishADuplicatePhysicalSegment() {
        var manager = manager(IBuilding.WALL, 2, 0, 1);
        var wall = new BuildingEntity(BuildingType.MEDIUM, IBuilding.WALL);
        wall.setId(8);
        wall.setOwner(manager.getGame().getPlayer(0));
        wall.configureConstruction(BuildingType.MEDIUM, IBuilding.WALL, 2, 40, 0, List.of(CubeCoords.ZERO));
        wall.getDesign().getWallSides().put(CubeCoords.ZERO, 1 << 3);
        manager.getGame().addEntity(wall);
        assertTrue(WallRules.hasOverlappingSegment(wall, ORIGIN.translated(0), 0, 0));
        assertFalse(wall.isPositionAndFacingValid(ORIGIN.translated(0), 0, 0, 0));
        wall.getDesign().getWallSides().put(CubeCoords.ZERO, 1 << 1);
        assertFalse(WallRules.hasOverlappingSegment(wall, ORIGIN.translated(0), 0, 0));
    }

    @Test void bridgeRemnantsRemainSupportedByEitherOriginalBankButAnIsolatedMiddleCollapses() {
        var manager = manager(IBuilding.BRIDGE, 1, 0, 0);
        var bridge = (BuildingEntity) manager.getGame().getEntity(1);
        var cells = java.util.stream.IntStream.range(0, 5).mapToObj(q -> new CubeCoords(q, 0, -q)).toList();
        bridge.configureConstruction(BuildingType.MEDIUM, IBuilding.BRIDGE, 1, 40, 0, cells);
        cells.forEach(cell -> bridge.getDesign().getBridgeDecks().put(cell, 3));
        bridge.updateBuildingEntityHexes(0, manager);
        var positions = cells.stream().map(bridge::relativeToBoard).toList();
        manager.collapseBuilding(bridge, manager.getGame().getPositionMapMulti(), positions.get(1), new Vector<>());
        assertTrue(bridge.isIn(positions.get(0)));
        assertTrue(bridge.isIn(positions.get(2)), "the far bank still supports this span");
        assertTrue(bridge.isIn(positions.get(3)));
        assertTrue(bridge.isIn(positions.get(4)));
        manager.collapseBuilding(bridge, manager.getGame().getPositionMapMulti(), positions.get(3), new Vector<>());
        assertFalse(bridge.isIn(positions.get(2)), "both paths to the original banks were severed");
        assertTrue(bridge.isIn(positions.get(0)));
        assertTrue(bridge.isIn(positions.get(4)));
        assertFalse(manager.getGame().getBoard().getHex(positions.get(2)).containsTerrain(Terrains.BRIDGE));
    }
}
