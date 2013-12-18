/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.bot.princess;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.IBoard;
import megamek.common.IGame;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.options.GameOptions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Deric Page (deric.page@nisc.coop) (ext 2335)
 * @version %Id%
 * @since 12/5/13 10:19 AM
 */
@RunWith(JUnit4.class)
public class BasicPathRankerTest {
    private final double TOLERANCE = 0.001;

    private Princess mockPrincess;
    private BehaviorSettings mockBehavior;
    private FireControl mockFireControl;
    private List<Targetable> testAdditionalTargets;

    @Before
    public void setUp() {
        mockBehavior = Mockito.mock(BehaviorSettings.class);
        Mockito.when(mockBehavior.getFallShameValue()).thenReturn(10);
        Mockito.when(mockBehavior.getBraveryValue()).thenReturn(1.5);
        Mockito.when(mockBehavior.getHyperAggressionValue()).thenReturn(10);
        Mockito.when(mockBehavior.getHerdMentalityValue()).thenReturn(0.01);
        Mockito.when(mockBehavior.getSelfPreservationValue()).thenReturn(30);

        testAdditionalTargets = new ArrayList<Targetable>();

        mockFireControl = Mockito.mock(FireControl.class);
        Mockito.when(mockFireControl.getAdditionalTargets()).thenReturn(testAdditionalTargets);

        mockPrincess = Mockito.mock(Princess.class);
        Mockito.when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);
        Mockito.when(mockPrincess.getFireControl()).thenReturn(mockFireControl);
        Mockito.when(mockPrincess.getHomeEdge()).thenReturn(HomeEdge.NORTH);
    }

    private void assertRankedPathEquals(RankedPath expected, RankedPath actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.rank, actual.rank, TOLERANCE);
        Assert.assertEquals(expected.reason, actual.reason);
        Assert.assertEquals("\nexpected :" + expected.toString() + "\nactual   :" + actual.toString(),
                expected.path, actual.path);
    }

    @Test
    public void testDoAeroSpecificRanking() {

        BasicPathRanker testRanker = new BasicPathRanker(mockPrincess);

        // Test a normal flight.
        MoveStep mockLastStep = Mockito.mock(MoveStep.class);
        Mockito.when(mockLastStep.getType()).thenReturn(MovePath.MoveStepType.FORWARDS);
        MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getFinalVelocity()).thenReturn(10);
        Mockito.when(mockPath.getFinalAltitude()).thenReturn(10);
        Mockito.when(mockPath.getLastStep()).thenReturn(mockLastStep);
        Assert.assertNull(testRanker.doAeroSpecificRanking(mockPath));

        // Test a stall
        Mockito.when(mockLastStep.getType()).thenReturn(MovePath.MoveStepType.FORWARDS);
        Mockito.when(mockPath.getFinalVelocity()).thenReturn(0);
        Mockito.when(mockPath.getFinalAltitude()).thenReturn(10);
        RankedPath expected = new RankedPath(-1000d, mockPath, "stall");
        assertRankedPathEquals(expected, testRanker.doAeroSpecificRanking(mockPath));

        // Test a crash.
        Mockito.when(mockLastStep.getType()).thenReturn(MovePath.MoveStepType.FORWARDS);
        Mockito.when(mockPath.getFinalVelocity()).thenReturn(10);
        Mockito.when(mockPath.getFinalAltitude()).thenReturn(0);
        expected = new RankedPath(-10000d, mockPath, "crash");
        assertRankedPathEquals(expected, testRanker.doAeroSpecificRanking(mockPath));

        // Test flying off the board.
        Mockito.when(mockLastStep.getType()).thenReturn(MovePath.MoveStepType.RETURN);
        Mockito.when(mockPath.getFinalVelocity()).thenReturn(10);
        Mockito.when(mockPath.getFinalAltitude()).thenReturn(10);
        expected = new RankedPath(-5d, mockPath, "off-board");
        assertRankedPathEquals(expected, testRanker.doAeroSpecificRanking(mockPath));
    }

    @Test
    public void testGetMovePathSuccessProbability() {

        Entity mockMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockMech.getMASCTarget()).thenReturn(3);

        MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.hasActiveMASC()).thenReturn(false);
        Mockito.when(mockPath.clone()).thenReturn(mockPath);
        Mockito.when(mockPath.getEntity()).thenReturn(mockMech);

        TargetRoll mockTargetRoll = Mockito.mock(TargetRoll.class);
        Mockito.when(mockTargetRoll.getValue()).thenReturn(8);

        TargetRoll mockTargetRollTwo = Mockito.mock(TargetRoll.class);
        Mockito.when(mockTargetRollTwo.getValue()).thenReturn(5);

        List<TargetRoll> testRollList = new ArrayList<TargetRoll>(2);
        testRollList.add(mockTargetRoll);
        testRollList.add(mockTargetRollTwo);

        BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(testRollList).when(testRanker).getPSRList(Mockito.eq(mockPath));

        double expected = 0.346;
        double actual = testRanker.getMovePathSuccessProbability(mockPath);
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Add in a MASC roll.
        Mockito.when(mockPath.hasActiveMASC()).thenReturn(true);
        expected = 0.336;
        actual = testRanker.getMovePathSuccessProbability(mockPath);
        Assert.assertEquals(expected, actual, TOLERANCE);
    }

    @Test
    public void testEvaluateUnmovedEnemy() {
        BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(mockPrincess).when(testRanker).getOwner();

        Coords testCoords = new Coords(10, 10);

        Entity mockMyUnit = Mockito.mock(BipedMech.class);
        Mockito.when(mockMyUnit.canChangeSecondaryFacing()).thenReturn(true);
        Mockito.doReturn(10.0).when(testRanker).getMaxDamageAtRange(Mockito.any(FireControl.class),
                Mockito.eq(mockMyUnit), Mockito.anyInt());

        MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(testCoords);
        Mockito.when(mockPath.getFinalFacing()).thenReturn(3);
        Mockito.when(mockPath.getEntity()).thenReturn(mockMyUnit);

        // Test an aero unit (doesn't really do anything at this point).
        Entity mockAero = Mockito.mock(Aero.class);
        Mockito.when(mockAero.getId()).thenReturn(2);
        EntityEvaluationResponse expected = new EntityEvaluationResponse();
        EntityEvaluationResponse actual = testRanker.evaluateUnmovedEnemy(mockAero, mockPath);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away, in my LoS and unable to kick my flank.
        Coords enemyCoords = new Coords(10, 15);
        int enemyMechId = 1;
        Entity mockEnemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech.getWeight()).thenReturn(50.0f);
        Mockito.when(mockEnemyMech.getId()).thenReturn(enemyMechId);
        Mockito.doReturn(enemyCoords)
                .when(testRanker)
                .getClosestCoordsTo(Mockito.eq(enemyMechId), Mockito.eq(testCoords));
        Mockito.doReturn(true)
                .when(testRanker)
                .isInMyLoS(Mockito.eq(mockEnemyMech), Mockito.any(BotGeometry.HexLine.class),
                        Mockito.any(BotGeometry.HexLine.class));
        Mockito.doReturn(8.5)
                .when(testRanker)
                .getMaxDamageAtRange(Mockito.any(FireControl.class), Mockito.eq(mockEnemyMech), Mockito.anyInt());
        Mockito.doReturn(false)
                .when(testRanker)
                .canFlankAndKick(Mockito.eq(mockEnemyMech), Mockito.any(Coords.class), Mockito.any(Coords.class),
                        Mockito.any(Coords.class), Mockito.anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(2.125);
        expected.setMyEstimatedDamage(2.5);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away but not in my LoS.
        enemyCoords = new Coords(10, 15);
        enemyMechId = 1;
        mockEnemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech.getWeight()).thenReturn(50.0f);
        Mockito.when(mockEnemyMech.getId()).thenReturn(enemyMechId);
        Mockito.doReturn(enemyCoords)
                .when(testRanker)
                .getClosestCoordsTo(Mockito.eq(enemyMechId), Mockito.eq(testCoords));
        Mockito.doReturn(false)
                .when(testRanker)
                .isInMyLoS(Mockito.eq(mockEnemyMech), Mockito.any(BotGeometry.HexLine.class),
                        Mockito.any(BotGeometry.HexLine.class));
        Mockito.doReturn(8.5)
                .when(testRanker)
                .getMaxDamageAtRange(Mockito.any(FireControl.class), Mockito.eq(mockEnemyMech), Mockito.anyInt());
        Mockito.doReturn(false)
                .when(testRanker)
                .canFlankAndKick(Mockito.eq(mockEnemyMech), Mockito.any(Coords.class), Mockito.any(Coords.class),
                        Mockito.any(Coords.class), Mockito.anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(2.125);
        expected.setMyEstimatedDamage(0.0);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away, not in my LoS and able to kick me.
        enemyCoords = new Coords(10, 15);
        enemyMechId = 1;
        mockEnemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech.getWeight()).thenReturn(50.0f);
        Mockito.when(mockEnemyMech.getId()).thenReturn(enemyMechId);
        Mockito.doReturn(enemyCoords)
                .when(testRanker)
                .getClosestCoordsTo(Mockito.eq(enemyMechId), Mockito.eq(testCoords));
        Mockito.doReturn(false)
                .when(testRanker)
                .isInMyLoS(Mockito.eq(mockEnemyMech), Mockito.any(BotGeometry.HexLine.class),
                        Mockito.any(BotGeometry.HexLine.class));
        Mockito.doReturn(8.5)
                .when(testRanker)
                .getMaxDamageAtRange(Mockito.any(FireControl.class), Mockito.eq(mockEnemyMech), Mockito.anyInt());
        Mockito.doReturn(true)
                .when(testRanker)
                .canFlankAndKick(Mockito.eq(mockEnemyMech), Mockito.any(Coords.class), Mockito.any(Coords.class),
                        Mockito.any(Coords.class), Mockito.anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(4.625);
        expected.setMyEstimatedDamage(0.0);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    @Test
    public void testEvaluateMovedEnemy() {
        BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(mockPrincess).when(testRanker).getOwner();

        MovePath mockPath = Mockito.mock(MovePath.class);

        IGame mockGame = Mockito.mock(IGame.class);

        //
        int mockEnemyMechId = 1;
        Entity mockEnemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech.getId()).thenReturn(mockEnemyMechId);
        Mockito.doReturn(15.0)
                .when(testRanker)
                .calculateDamagePotential(Mockito.eq(mockEnemyMech), Mockito.any(EntityState.class),
                        Mockito.any(MovePath.class), Mockito.any(IGame.class));
        Mockito.doReturn(10.0)
                .when(testRanker)
                .calculateKickDamagePotential(Mockito.eq(mockEnemyMech), Mockito.any(MovePath.class),
                        Mockito.any(IGame.class));
        Mockito.doReturn(14.5)
                .when(testRanker)
                .calculateMyDamagePotential(Mockito.any(MovePath.class), Mockito.eq(mockEnemyMech),
                        Mockito.any(IGame.class));
        Mockito.doReturn(8.0)
                .when(testRanker)
                .calculateMyKickDamagePotential(Mockito.any(MovePath.class), Mockito.eq(mockEnemyMech),
                        Mockito.any(IGame.class));
        Map<Integer, Double> testBestDamageByEnemies = new TreeMap<Integer, Double>();
        testBestDamageByEnemies.put(mockEnemyMechId, 0.0);
        Mockito.doReturn(testBestDamageByEnemies)
                .when(testRanker)
                .getBestDamageByEnemies();
        EntityEvaluationResponse expected = new EntityEvaluationResponse();
        expected.setMyEstimatedDamage(14.5);
        expected.setMyEstimatedPhysicalDamage(8.0);
        expected.setEstimatedEnemyDamage(25.0);
        EntityEvaluationResponse actual = testRanker.evaluateMovedEnemy(mockEnemyMech, mockPath, mockGame);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    private void assertEntityEvaluationResponseEquals(EntityEvaluationResponse expected,
                                                      EntityEvaluationResponse actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getMyEstimatedDamage(), actual.getMyEstimatedDamage(), TOLERANCE);
        Assert.assertEquals(expected.getMyEstimatedPhysicalDamage(), actual.getMyEstimatedPhysicalDamage(), TOLERANCE);
        Assert.assertEquals(expected.getEstimatedEnemyDamage(), actual.getEstimatedEnemyDamage(), TOLERANCE);
    }

    @Test
    public void testRankPath() {
        BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(1.0)
                .when(testRanker)
                .getMovePathSuccessProbability(Mockito.any(MovePath.class));
        Mockito.doReturn(5)
                .when(testRanker)
                .distanceToClosestEdge(Mockito.any(Coords.class), Mockito.any(IGame.class));
        Mockito.doReturn(20)
                .when(testRanker)
                .distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(HomeEdge.class), Mockito.any(IGame.class));
        Mockito.doReturn(12.0)
                .when(testRanker)
                .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class), Mockito.any(IGame.class));

        Entity mockMover = Mockito.mock(BipedMech.class);
        Mockito.when(mockMover.isClan()).thenReturn(false);
        Mockito.when(mockPrincess.wantsToFlee(Mockito.eq(mockMover))).thenReturn(false);

        Coords finalCoords = new Coords(0, 0);

        MoveStep mockLastStep = Mockito.mock(MoveStep.class);
        Mockito.when(mockLastStep.getFacing()).thenReturn(0);

        MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getEntity()).thenReturn(mockMover);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(finalCoords);
        Mockito.when(mockPath.toString()).thenReturn("F F F");
        Mockito.when(mockPath.clone()).thenReturn(mockPath);
        Mockito.when(mockPath.getLastStep()).thenReturn(mockLastStep);

        IBoard mockBoard = Mockito.mock(IBoard.class);
        Mockito.when(mockBoard.contains(Mockito.any(Coords.class))).thenReturn(true);

        GameOptions mockGameOptions = Mockito.mock(GameOptions.class);
        Mockito.when(mockGameOptions.booleanOption(Mockito.eq("no_clan_physical"))).thenReturn(false);

        IGame mockGame = Mockito.mock(IGame.class);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);
        Mockito.when(mockGame.getOptions()).thenReturn(mockGameOptions);

        List<Entity> testEnemies = new ArrayList<Entity>();

        Map<Integer, Double> bestDamageByEnemies = new TreeMap<Integer, Double>();
        Mockito.when(testRanker.getBestDamageByEnemies()).thenReturn(bestDamageByEnemies);

        Coords enemyMech1Position = Mockito.spy(new Coords(10, 10));
        Mockito.doReturn(3)
                .when(enemyMech1Position)
                .direction(Mockito.any(Coords.class));
        Entity mockEnemyMech1 = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech1.isOffBoard()).thenReturn(false);
        Mockito.when(mockEnemyMech1.getPosition()).thenReturn(enemyMech1Position);
        Mockito.when(mockEnemyMech1.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockEnemyMech1.isImmobile()).thenReturn(false);
        Mockito.when(mockEnemyMech1.getId()).thenReturn(1);
        EntityEvaluationResponse evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
                .when(testRanker)
                .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class), Mockito.any(IGame.class));
        testEnemies.add(mockEnemyMech1);
        Mockito.doReturn(mockEnemyMech1)
                .when(testRanker)
                .findClosestEnemy(Mockito.eq(mockMover), Mockito.any(Coords.class), Mockito.any(IGame.class));

        Entity mockEnemyMech2 = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech2.isOffBoard()).thenReturn(false);
        Mockito.when(mockEnemyMech2.getPosition()).thenReturn(new Coords(10, 10));
        Mockito.when(mockEnemyMech2.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockEnemyMech2.isImmobile()).thenReturn(false);
        Mockito.when(mockEnemyMech2.getId()).thenReturn(2);
        EntityEvaluationResponse evalForMockEnemyMech2 = new EntityEvaluationResponse();
        evalForMockEnemyMech2.setMyEstimatedDamage(8.0);
        evalForMockEnemyMech2.setMyEstimatedPhysicalDamage(0.0);
        evalForMockEnemyMech2.setEstimatedEnemyDamage(15.0);
        Mockito.doReturn(evalForMockEnemyMech2)
                .when(testRanker)
                .evaluateUnmovedEnemy(Mockito.eq(mockEnemyMech2), Mockito.any(MovePath.class));
        testEnemies.add(mockEnemyMech2);

        Coords friendsCoords = new Coords(10, 10);

        double baseRank = -126.4; // The rank I expect to get with the above settings.

        RankedPath expected = new RankedPath(baseRank, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        RankedPath actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);

        // Change the move path success probability.
        Mockito.doReturn(0.5)
                .when(testRanker)
                .getMovePathSuccessProbability(Mockito.any(MovePath.class));
        expected = new RankedPath(-128.275, mockPath, "Calculation: fall mod [5.0 = 0.5 * 10.0] + " +
                "braveryMod [-3.12 = 50% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.rank) {
            Assert.fail("Higher chance to fall should mean lower rank.");
        }
        Mockito.doReturn(0.75)
                .when(testRanker)
                .getMovePathSuccessProbability(Mockito.any(MovePath.class));
        expected = new RankedPath(-127.337, mockPath, "Calculation: fall mod [2.5 = 0.25 * 10.0] + " +
                "braveryMod [-4.69 = 75% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.rank) {
            Assert.fail("Higher chance to fall should mean lower rank.");
        }
        Mockito.doReturn(1.0)
                .when(testRanker)
                .getMovePathSuccessProbability(Mockito.any(MovePath.class));

        // Change the damage to enemy mech 1.
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
                .when(testRanker)
                .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class), Mockito.any(IGame.class));
        expected = new RankedPath(-126.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.rank) {
            Assert.fail("The more damage I do, the higher the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(4.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
                .when(testRanker)
                .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class), Mockito.any(IGame.class));
        expected = new RankedPath(-136.15, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-16.00 = 100% * ((16.00 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.rank) {
            Assert.fail("The less damage I do, the lower the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
                .when(testRanker)
                .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class), Mockito.any(IGame.class));

        // Change the damage done by enemy mech 1.
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(35.0);
        Mockito.doReturn(evalForMockEnemyMech)
                .when(testRanker)
                .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class), Mockito.any(IGame.class));
        expected = new RankedPath(-136.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-16.25 = 100% * ((22.50 * 1.50) - 50.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        if (baseRank < actual.rank) {
            Assert.fail("The more damage they do, the lower the path rank should be.");
        }
        assertRankedPathEquals(expected, actual);
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(15.0);
        Mockito.doReturn(evalForMockEnemyMech)
                .when(testRanker)
                .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class), Mockito.any(IGame.class));
        expected = new RankedPath(-116.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [3.75 = 100% * ((22.50 * 1.50) - 30.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.rank) {
            Assert.fail("The less damage they do, the higher the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
                .when(testRanker)
                .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class), Mockito.any(IGame.class));

        // Change the distance to the enemy.
        Mockito.doReturn(2.0)
                .when(testRanker)
                .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class), Mockito.any(IGame.class));
        expected = new RankedPath(-26.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [20.00 = 2.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.rank) {
            Assert.fail("The closer I am to the enemy, the higher the path rank should be.");
        }
        Mockito.doReturn(22.0)
                .when(testRanker)
                .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class), Mockito.any(IGame.class));
        expected = new RankedPath(-226.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [220.00 = 22.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.rank) {
            Assert.fail("The further I am from the enemy, the lower the path rank should be.");
        }
        Mockito.doReturn(12.0)
                .when(testRanker)
                .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class), Mockito.any(IGame.class));

        // Change the distance to my friends.
        friendsCoords = new Coords(0, 10);
        expected = new RankedPath(-126.35, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.10 = 10.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.rank) {
            Assert.fail("The closer I am to my friends, the higher the path rank should be.");
        }
        friendsCoords = new Coords(20, 10);
        expected = new RankedPath(-126.45, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.20 = 20.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.rank) {
            Assert.fail("The further I am from my friends, the lower the path rank should be.");
        }
        friendsCoords = new Coords(10, 10);

        // Set myself up to run away.
        double baseFleeingRank = -726.4;
        Mockito.when(mockPrincess.wantsToFlee(Mockito.eq(mockMover))).thenReturn(true);
        expected = new RankedPath(baseFleeingRank, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})] - " +
                "selfPreservationMod [600.00 = 20.00 * 30.00]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        Mockito.doReturn(10)
                .when(testRanker)
                .distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(HomeEdge.class), Mockito.any(IGame.class));
        expected = new RankedPath(-426.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})] - " +
                "selfPreservationMod [300.00 = 10.00 * 30.00]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseFleeingRank > actual.rank) {
            Assert.fail("The closer I am to my home edge when fleeing, the higher the path rank should be.");
        }
        Mockito.doReturn(30)
                .when(testRanker)
                .distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(HomeEdge.class), Mockito.any(IGame.class));
        expected = new RankedPath(-1026.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {0 - 1})] - " +
                "selfPreservationMod [900.00 = 30.00 * 30.00]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseFleeingRank < actual.rank) {
            Assert.fail("The further I am from my home edge when fleeing, the lower the path rank should be.");
        }
        Mockito.doReturn(20)
                .when(testRanker)
                .distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(HomeEdge.class), Mockito.any(IGame.class));
        Mockito.when(mockPrincess.wantsToFlee(Mockito.eq(mockMover))).thenReturn(false);

        // Change my facing.
        Mockito.when(mockLastStep.getFacing()).thenReturn(1);
        expected = new RankedPath(baseRank, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [0.00 = max(0, 50 * {1 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank != actual.rank) {
            Assert.fail("Being 1 hex off facing should make no difference in rank.");
        }
        Mockito.when(mockLastStep.getFacing()).thenReturn(4);
        expected = new RankedPath(-176.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [50.00 = max(0, 50 * {2 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.rank) {
            Assert.fail("Being 2 or more hexes off facing should lower the path rank.");
        }
        Mockito.when(mockLastStep.getFacing()).thenReturn(3);
        expected = new RankedPath(-226.4, mockPath, "Calculation: fall mod [0.0 = 0.0 * 10.0] + " +
                "braveryMod [-6.25 = 100% * ((22.50 * 1.50) - 40.00] - aggressionMod [120.00 = 12.00 * 10.00] - " +
                "herdingMod [0.15 = 15.00 * 0.01] - facingMod [100.00 = max(0, 50 * {3 - 1})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, 20, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.rank) {
            Assert.fail("Being 2 or more hexes off facing should lower the path rank.");
        }

        Mockito.when(mockLastStep.getFacing()).thenReturn(0);
    }

    @Test
    public void testFindClosestEnemy() {
        List<Entity> enemyList = new ArrayList<Entity>(3);

        Entity enemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(enemyMech.getPosition()).thenReturn(new Coords(10, 10));
        enemyList.add(enemyMech);

        Entity enemyTank = Mockito.mock(Tank.class);
        Mockito.when(enemyTank.getPosition()).thenReturn(new Coords(10, 15));
        enemyList.add(enemyTank);

        Entity enemyBA = Mockito.mock(BattleArmor.class);
        Mockito.when(enemyBA.getPosition()).thenReturn(new Coords(15, 15));
        enemyList.add(enemyBA);

        Coords position = new Coords(0, 0);
        Entity me = Mockito.mock(BipedMech.class);
        IGame mockGame = Mockito.mock(IGame.class);

        BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(enemyList)
                .when(testRanker)
                .getEnemies(Mockito.any(Entity.class), Mockito.any(IGame.class));

        Entity expected = enemyMech;
        Entity actual = testRanker.findClosestEnemy(me, position, mockGame);
        Assert.assertEquals(expected, actual);
    }
}
