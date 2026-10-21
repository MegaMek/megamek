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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.HitData;
import megamek.common.MMRandom;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import megamek.server.totalWarfare.ObjectiveDestructionHandler.FragileRisk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Carrying Mobile objectives and destroying objectives, on a real game and board with fixed dice: a carrier that dies
 * or falls drops its objective intact unless it is Fragile; the End Phase and the forced-drop roll take it from a
 * carrier; one objective fits in a hex; a collapsing building, fire, heat weapons and area-effect damage destroy what
 * they should and nothing more.
 */
class MobileAndFragileObjectiveTest {

    private static final Coords CARRIER_HEX = new Coords(5, 5);

    /** Dice that always land on the same face. */
    private static final class FixedDice extends MMRandom {
        private final int face;

        FixedDice(int face) {
            this.face = face;
        }

        @Override
        public int randomInt(int maxValue) {
            return Math.min(face, maxValue) - 1;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    private Game game;
    private TWGameManager gameManager;
    private Player alice;
    private Player bob;
    private int nextEntityId = 1;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        alice = new Player(0, "Alice");
        alice.setTeam(1);
        bob = new Player(1, "Bob");
        bob.setTeam(2);
        game.addPlayer(0, alice);
        game.addPlayer(1, bob);
        Board board = new Board(16, 17);
        for (int column = 0; column < board.getWidth(); column++) {
            for (int row = 0; row < board.getHeight(); row++) {
                board.setHex(column, row, new Hex());
            }
        }
        game.setBoard(board);
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        gameManager.setGame(game);
        rollEveryDieAs(6);
    }

    @AfterEach
    void restoreDice() {
        Compute.setRNG(MMRandom.R_DEFAULT);
    }

    // --- a carrier that dies ---

    @Test
    void testADestroyedCarrierDropsItsObjectiveIntactInItsHex() {
        // a 1 would wreck ordinary cargo; an objective is never wrecked by a drop
        rollEveryDieAs(1);
        ObjectiveMarker flag = mobileFlag();
        BipedMek carrier = carrying(flag);

        gameManager.destroyEntity(carrier, "test");

        assertTrue(game.getGroundObjects(CARRIER_HEX).contains(flag), "the objective lands where its carrier died");
        assertFalse(flag.isDestroyed());
        assertTrue(carrier.getDistinctCarriedObjects().isEmpty());
    }

    @Test
    void testAHitOnTheCarrierLeavesTheObjectiveInItsHand() {
        // RAW (Mobile Objectives): damage to the carrier calls for a forced-drop roll, it never wrecks the objective
        rollEveryDieAs(1);
        ObjectiveMarker flag = mobileFlag();
        BipedMek carrier = carrying(flag);

        gameManager.damageEntity(carrier, new HitData(Mek.LOC_CENTER_TORSO), 5);
        gameManager.damageEntity(carrier, new HitData(Mek.LOC_RIGHT_ARM), 5);

        assertFalse(flag.isDestroyed());
        assertEquals(List.of(flag), carrier.getDistinctCarriedObjects());
    }

    @Test
    void testAHitOnTheCarrierIsNoFragileTrigger() {
        rollEveryDieAs(1);
        ObjectiveMarker flag = mobileFlag();
        flag.setFragile(true);
        BipedMek carrier = carrying(flag);

        gameManager.damageEntity(carrier, new HitData(Mek.LOC_CENTER_TORSO), 5);

        assertFalse(flag.isDestroyed());
        assertEquals(List.of(flag), carrier.getDistinctCarriedObjects());
    }

    @Test
    void testAFragileObjectiveDroppedByADestroyedCarrierIsDestroyedOnAFour() {
        rollEveryDieAs(4);
        ObjectiveMarker flag = mobileFlag();
        flag.setFragile(true);
        BipedMek carrier = carrying(flag);

        gameManager.destroyEntity(carrier, "test");

        assertTrue(flag.isDestroyed());
        assertTrue(game.getGroundObjects(CARRIER_HEX).contains(flag), "the wreck stays on the board");
        assertTrue(reportIds().contains(ObjectiveDestructionHandler.REPORT_FRAGILE_ROLL));
        assertTrue(reportIds().contains(ObjectiveDestructionHandler.REPORT_OBJECTIVE_DESTROYED));
    }

    @Test
    void testAFragileObjectiveSurvivesTheDropOnAFive() {
        rollEveryDieAs(5);
        ObjectiveMarker flag = mobileFlag();
        flag.setFragile(true);
        BipedMek carrier = carrying(flag);

        gameManager.destroyEntity(carrier, "test");

        assertFalse(flag.isDestroyed());
    }

    // --- End Phase drops ---

    @Test
    void testAnImmobileCarrierDropsItsObjectiveInTheEndPhase() {
        ObjectiveMarker flag = mobileFlag();
        BipedMek carrier = carrying(flag);
        carrier.setShutDown(true);

        new ObjectiveCarryHandler(gameManager).resolveEndPhaseDrops();

        assertTrue(game.getGroundObjects(CARRIER_HEX).contains(flag));
        assertTrue(carrier.getDistinctCarriedObjects().isEmpty());
        assertTrue(reportIds().contains(ObjectiveCarryHandler.REPORT_OBJECTIVE_DROPPED));
    }

    @Test
    void testAProneCarrierSetsItsObjectiveDownWithoutAFragileRoll() {
        rollEveryDieAs(1);
        ObjectiveMarker flag = mobileFlag();
        flag.setFragile(true);
        BipedMek carrier = carrying(flag);
        carrier.setProne(true);

        new ObjectiveCarryHandler(gameManager).resolveEndPhaseDrops();

        assertTrue(game.getGroundObjects(CARRIER_HEX).contains(flag));
        assertFalse(flag.isDestroyed(), "going prone by choice puts nothing at risk");
        assertFalse(reportIds().contains(ObjectiveDestructionHandler.REPORT_FRAGILE_ROLL));
    }

    @Test
    void testAStandingMobileCarrierKeepsItsObjective() {
        ObjectiveMarker flag = mobileFlag();
        BipedMek carrier = carrying(flag);

        new ObjectiveCarryHandler(gameManager).resolveEndPhaseDrops();

        assertEquals(List.of(flag), carrier.getDistinctCarriedObjects());
    }

    // --- the forced-drop roll ---

    @Test
    void testADamagedCarrierThatFailsTheRollDropsItsObjective() {
        rollEveryDieAs(1);
        ObjectiveMarker flag = mobileFlag();
        BipedMek carrier = carrying(flag);
        carrier.damageThisPhase = 5;

        new ObjectiveCarryHandler(gameManager).resolveForcedObjectiveDrops();

        assertTrue(carrier.getDistinctCarriedObjects().isEmpty());
        assertTrue(game.getGroundObjects(CARRIER_HEX).contains(flag));
        assertTrue(reportIds().contains(ObjectiveCarryHandler.REPORT_FORCED_DROP_CHECK));
    }

    @Test
    void testADamagedCarrierThatPassesTheRollKeepsHold() {
        ObjectiveMarker flag = mobileFlag();
        BipedMek carrier = carrying(flag);
        carrier.damageThisPhase = 5;

        new ObjectiveCarryHandler(gameManager).resolveForcedObjectiveDrops();

        assertEquals(List.of(flag), carrier.getDistinctCarriedObjects());
        assertTrue(reportIds().contains(ObjectiveCarryHandler.REPORT_FORCED_DROP_CHECK));
    }

    @Test
    void testAnUndamagedCarrierMakesNoRoll() {
        rollEveryDieAs(1);
        ObjectiveMarker flag = mobileFlag();
        BipedMek carrier = carrying(flag);

        new ObjectiveCarryHandler(gameManager).resolveForcedObjectiveDrops();

        assertEquals(List.of(flag), carrier.getDistinctCarriedObjects());
        assertFalse(reportIds().contains(ObjectiveCarryHandler.REPORT_FORCED_DROP_CHECK));
    }

    // --- one objective per hex ---

    @Test
    void testDroppingOntoAnotherObjectivePushesItTheWayTheCarrierFaces() {
        ObjectiveMarker lying = mobileFlag();
        game.placeGroundObject(CARRIER_HEX, lying);
        ObjectiveMarker dropped = mobileFlag();
        BipedMek carrier = mekAt(alice, CARRIER_HEX);
        carrier.setFacing(2);

        new ObjectiveCarryHandler(gameManager).placeDroppedObjective(carrier, CARRIER_HEX, dropped);

        assertEquals(List.of(dropped), game.getGroundObjects(CARRIER_HEX));
        assertTrue(game.getGroundObjects(CARRIER_HEX.translated(2)).contains(lying));
        assertTrue(reportIds().contains(ObjectiveCarryHandler.REPORT_OBJECTIVE_DISPLACED));
    }

    @Test
    void testAnObjectivePushedTowardTheBoardEdgeStaysOnTheBoard() {
        Coords edgeHex = new Coords(5, 0);
        ObjectiveMarker lying = mobileFlag();
        game.placeGroundObject(edgeHex, lying);
        BipedMek carrier = mekAt(alice, edgeHex);
        carrier.setFacing(0);

        new ObjectiveCarryHandler(gameManager).placeDroppedObjective(carrier, edgeHex, mobileFlag());

        assertTrue(game.getGroundObjects(edgeHex).contains(lying), "pushed off the board, it stays in its last hex");
    }

    @Test
    void testADestroyedObjectiveIsNotPushed() {
        ObjectiveMarker wreck = mobileFlag();
        wreck.setDestroyed(true);
        game.placeGroundObject(CARRIER_HEX, wreck);

        new ObjectiveCarryHandler(gameManager).placeDroppedObjective(mekAt(alice, CARRIER_HEX), CARRIER_HEX,
              mobileFlag());

        assertTrue(game.getGroundObjects(CARRIER_HEX).contains(wreck));
    }

    // --- picking one up ---

    @Test
    void testOnlyAMekWithNothingInHandCanPickUpAnObjective() {
        ObjectiveMarker flag = mobileFlag();
        BipedMek emptyHanded = mekAt(alice, CARRIER_HEX);
        BipedMek alreadyCarrying = carrying(mobileFlag());

        assertTrue(emptyHanded.canPickupCarryableObject(flag));
        assertFalse(alreadyCarrying.canPickupCarryableObject(flag), "one objective per Mek");
    }

    // --- destruction ---

    @Test
    void testACollapsedBuildingDestroysTheObjectiveInside() {
        ObjectiveMarker point = groundPoint(CARRIER_HEX);

        new ObjectiveDestructionHandler(gameManager).destroyObjectivesInCollapsedHex(CARRIER_HEX, 0);

        assertTrue(point.isDestroyed());
        assertTrue(reportIds().contains(ObjectiveDestructionHandler.REPORT_OBJECTIVE_DESTROYED));
    }

    @Test
    void testAnIndestructibleObjectiveSurvivesItsBuilding() {
        ObjectiveMarker point = groundPoint(CARRIER_HEX);
        point.setInvulnerable(true);

        new ObjectiveDestructionHandler(gameManager).destroyObjectivesInCollapsedHex(CARRIER_HEX, 0);

        assertFalse(point.isDestroyed());
    }

    @Test
    void testABuildingCollapsingOnAnotherBoardLeavesTheObjectiveAlone() {
        ObjectiveMarker point = groundPoint(CARRIER_HEX);

        new ObjectiveDestructionHandler(gameManager).destroyObjectivesInCollapsedHex(CARRIER_HEX, 7);

        assertFalse(point.isDestroyed());
    }

    @Test
    void testAFragileObjectiveInABurningHexRollsInTheEndPhase() {
        rollEveryDieAs(4);
        ObjectiveMarker fragilePoint = groundPoint(CARRIER_HEX);
        fragilePoint.setFragile(true);
        Coords sturdyHex = new Coords(8, 8);
        ObjectiveMarker sturdyPoint = groundPoint(sturdyHex);
        setOnFire(CARRIER_HEX);
        setOnFire(sturdyHex);

        new ObjectiveDestructionHandler(gameManager).checkFragileObjectivesOnFire();

        assertTrue(fragilePoint.isDestroyed());
        assertFalse(sturdyPoint.isDestroyed(), "only a Fragile objective is at risk from fire");
    }

    @Test
    void testAFragileObjectiveCarriedThroughFireRolls() {
        rollEveryDieAs(3);
        ObjectiveMarker flag = mobileFlag();
        flag.setFragile(true);
        carrying(flag);
        setOnFire(CARRIER_HEX);

        new ObjectiveDestructionHandler(gameManager).checkFragileObjectivesOnFire();

        assertTrue(flag.isDestroyed());
    }

    @Test
    void testAHeatWeaponHittingTheHexRisksAFragileObjective() {
        rollEveryDieAs(2);
        ObjectiveMarker fragilePoint = groundPoint(CARRIER_HEX);
        fragilePoint.setFragile(true);

        gameManager.tryIgniteHex(CARRIER_HEX, 0, Entity.NONE, true, false, new TargetRoll(0, "test"), 0,
              new Vector<>());

        assertTrue(fragilePoint.isDestroyed());
    }

    @Test
    void testAnOrdinaryShotThatMightStartAFireDoesNotRiskIt() {
        rollEveryDieAs(1);
        ObjectiveMarker fragilePoint = groundPoint(CARRIER_HEX);
        fragilePoint.setFragile(true);

        gameManager.tryIgniteHex(CARRIER_HEX, 0, Entity.NONE, false, false, new TargetRoll(0, "test"), 0,
              new Vector<>());

        assertFalse(fragilePoint.isDestroyed());
    }

    @Test
    void testAreaEffectDamageInTheHexRisksAFragileObjective() {
        rollEveryDieAs(1);
        ObjectiveMarker fragilePoint = groundPoint(CARRIER_HEX);
        fragilePoint.setFragile(true);

        new ObjectiveDestructionHandler(gameManager).checkFragileObjectivesInHex(CARRIER_HEX, 0,
              FragileRisk.AREA_EFFECT);

        assertTrue(fragilePoint.isDestroyed());
    }

    @Test
    void testAnIndestructibleFragileObjectiveMakesNoRoll() {
        rollEveryDieAs(1);
        ObjectiveMarker fragilePoint = groundPoint(CARRIER_HEX);
        fragilePoint.setFragile(true);
        fragilePoint.setInvulnerable(true);

        new ObjectiveDestructionHandler(gameManager).checkFragileObjectivesInHex(CARRIER_HEX, 0,
              FragileRisk.AREA_EFFECT);

        assertFalse(fragilePoint.isDestroyed());
        assertFalse(reportIds().contains(ObjectiveDestructionHandler.REPORT_FRAGILE_ROLL));
    }

    @Test
    void testADestroyedObjectiveIsReportedOnce() {
        ObjectiveMarker point = groundPoint(CARRIER_HEX);
        ObjectiveDestructionHandler handler = new ObjectiveDestructionHandler(gameManager);

        handler.destroyObjectivesInCollapsedHex(CARRIER_HEX, 0);
        handler.destroyObjectivesInCollapsedHex(CARRIER_HEX, 0);

        assertTrue(point.isDestroyed());
        assertEquals(1, reportIds().stream()
              .filter(reportId -> reportId == ObjectiveDestructionHandler.REPORT_OBJECTIVE_DESTROYED)
              .count());
    }

    // --- helpers ---

    private void rollEveryDieAs(int face) {
        Compute.setRNG(new FixedDice(face));
    }

    private ObjectiveMarker mobileFlag() {
        ObjectiveMarker flag = new ObjectiveMarker();
        flag.setName("Flag " + nextEntityId++);
        flag.setOwnerId(bob.getId());
        flag.setScoringScheme(ObjectiveScoringScheme.raid());
        flag.setMobile(true);
        return flag;
    }

    private ObjectiveMarker groundPoint(Coords position) {
        ObjectiveMarker point = new ObjectiveMarker();
        point.setName("Point " + nextEntityId++);
        point.setOwnerId(bob.getId());
        game.placeGroundObject(position, point);
        return point;
    }

    private BipedMek mekAt(Player owner, Coords position) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(nextEntityId++);
        mek.setChassis("Carrier");
        mek.setModel("C-" + mek.getId());
        mek.setWeight(50);
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(10, location);
        }
        mek.setOwner(owner);
        mek.setPosition(position);
        mek.setDeployed(true);
        game.addEntity(mek);
        return mek;
    }

    private BipedMek carrying(ObjectiveMarker flag) {
        BipedMek carrier = mekAt(alice, CARRIER_HEX);
        carrier.pickupCarryableObject(flag, Mek.LOC_RIGHT_ARM);
        return carrier;
    }

    private void setOnFire(Coords position) {
        game.getBoard().getHex(position).addTerrain(new Terrain(Terrains.FIRE, 1));
    }

    private List<Integer> reportIds() {
        ArgumentCaptor<Report> reports = ArgumentCaptor.forClass(Report.class);
        Mockito.verify(gameManager, Mockito.atLeast(0)).addReport(reports.capture());
        return reports.getAllValues().stream().map(report -> report.messageId).toList();
    }
}
