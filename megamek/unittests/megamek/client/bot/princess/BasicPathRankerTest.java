/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.bot.princess;

import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.options.GameOptions;
import megamek.common.options.PilotOptions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/5/13 10:19 AM
 */
@RunWith(JUnit4.class)
public class BasicPathRankerTest {
    private final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00");
    private final NumberFormat LOG_INT = NumberFormat.getIntegerInstance();
    private final NumberFormat LOG_PERCENT = NumberFormat.getPercentInstance();

    private final double TOLERANCE = 0.001;

    private Princess mockPrincess;
    private FireControl mockFireControl;
    private FireControlState mockFireControlState;
    private PathRankerState mockPathRankerState;

    @Before
    public void setUp() {
        final BehaviorSettings mockBehavior = Mockito.mock(BehaviorSettings.class);
        Mockito.when(mockBehavior.getFallShameValue()).thenReturn(BehaviorSettings.FALL_SHAME_VALUES[5]);
        Mockito.when(mockBehavior.getBraveryValue()).thenReturn(BehaviorSettings.BRAVERY[5]);
        Mockito.when(mockBehavior.getHyperAggressionValue()).thenReturn(BehaviorSettings.HYPER_AGGRESSION_VALUES[5]);
        Mockito.when(mockBehavior.getHerdMentalityValue()).thenReturn(BehaviorSettings.HERD_MENTALITY_VALUES[5]);
        Mockito.when(mockBehavior.getSelfPreservationValue()).thenReturn(BehaviorSettings.SELF_PRESERVATION_VALUES[5]);

        mockFireControl = Mockito.mock(FireControl.class);

        final IHonorUtil mockHonorUtil = Mockito.mock(IHonorUtil.class);
        Mockito.when(mockHonorUtil.isEnemyBroken(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyBoolean()))
               .thenReturn(false);
        
        final List<Targetable> testAdditionalTargets = new ArrayList<>();
        mockFireControlState = Mockito.mock(FireControlState.class);
        Mockito.when(mockFireControlState.getAdditionalTargets()).thenReturn(testAdditionalTargets);

        
        final Map<MovePath.Key, Double> testSuccessProbabilities = new HashMap<>();
        mockPathRankerState = Mockito.mock(PathRankerState.class);
        Mockito.when(mockPathRankerState.getPathSuccessProbabilities()).thenReturn(testSuccessProbabilities);
        
        final UnitBehavior mockBehaviorTracker = Mockito.mock(UnitBehavior.class);
        Mockito.when(mockBehaviorTracker.getBehaviorType(Mockito.any(Entity.class), Mockito.any(Princess.class)))
            .thenReturn(BehaviorType.Engaged);
        
        mockPrincess = Mockito.mock(Princess.class);
        Mockito.when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);
        Mockito.when(mockPrincess.getFireControl(FireControlType.Basic)).thenReturn(mockFireControl);
        Mockito.when(mockPrincess.getFireControl(Mockito.any(Entity.class))).thenReturn(mockFireControl);
        Mockito.when(mockPrincess.getHomeEdge(Mockito.any(Entity.class))).thenReturn(CardinalEdge.NORTH);
        Mockito.when(mockPrincess.getHonorUtil()).thenReturn(mockHonorUtil);
        Mockito.when(mockPrincess.getFireControlState()).thenReturn(mockFireControlState);
        Mockito.when(mockPrincess.getPathRankerState()).thenReturn(mockPathRankerState);
        Mockito.when(mockPrincess.getMaxWeaponRange(Mockito.any(Entity.class), Mockito.anyBoolean())).thenReturn(21);
        Mockito.when(mockPrincess.getUnitBehaviorTracker()).thenReturn(mockBehaviorTracker);
    }

    private void assertRankedPathEquals(final RankedPath expected, final RankedPath actual) {
        Assert.assertNotNull("Actual path is null.", actual);
        final StringBuilder failure = new StringBuilder();
        if (!expected.getReason().equals(actual.getReason())) {
            failure.append("\nExpected :").append(expected.getReason());
            failure.append("\nActual   :").append(actual.getReason());
        }
        if (!expected.getPath().equals(actual.getPath())) {
            failure.append("\nExpected :").append(expected);
            failure.append("\nActual   :").append(actual);
        }
        final int expectedRank = (int) (expected.getRank() * (1 / TOLERANCE));
        final int actualRank = (int) (actual.getRank() * (1 / TOLERANCE));
        if (expectedRank != actualRank) {
            failure.append("\nExpected :").append(expected.getRank());
            failure.append("\nActual   :").append(actual.getRank());
        }
        if (!StringUtility.isNullOrEmpty(failure.toString())) {
            Assert.fail(failure.toString());
        }
    }   

    @Test
    public void testGetMovePathSuccessProbability() {

        final Entity mockMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockMech.getMASCTarget()).thenReturn(3);

        final Crew mockCrew = Mockito.mock(Crew.class);
        Mockito.when(mockMech.getCrew()).thenReturn(mockCrew);

        final PilotOptions mockOptions = Mockito.mock(PilotOptions.class);
        Mockito.when(mockCrew.getOptions()).thenReturn(mockOptions);
        Mockito.when(mockOptions.booleanOption(Mockito.anyString())).thenReturn(false);

        final MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.hasActiveMASC()).thenReturn(false);
        Mockito.when(mockPath.clone()).thenReturn(mockPath);
        Mockito.when(mockPath.getEntity()).thenReturn(mockMech);

        final TargetRoll mockTargetRoll = Mockito.mock(TargetRoll.class);
        Mockito.when(mockTargetRoll.getValue()).thenReturn(8);
        Mockito.when(mockTargetRoll.getDesc()).thenReturn("mock");

        final TargetRoll mockTargetRollTwo = Mockito.mock(TargetRoll.class);
        Mockito.when(mockTargetRollTwo.getValue()).thenReturn(5);
        Mockito.when(mockTargetRollTwo.getDesc()).thenReturn("mock");

        final List<TargetRoll> testRollList = new ArrayList<>(2);
        testRollList.add(mockTargetRoll);
        testRollList.add(mockTargetRollTwo);

        final BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(testRollList).when(testRanker).getPSRList(Mockito.eq(mockPath));

        double expected = 0.346;
        double actual = testRanker.getMovePathSuccessProbability(mockPath, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Add in a MASC roll.
        Mockito.when(mockPath.hasActiveMASC()).thenReturn(true);
        expected = 0.346;
        actual = testRanker.getMovePathSuccessProbability(mockPath, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);
    }

    @Test
    public void testEvaluateUnmovedEnemy() {
        final BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(mockPrincess).when(testRanker).getOwner();

        final Coords testCoords = new Coords(10, 10);

        final Entity mockMyUnit = Mockito.mock(BipedMech.class);
        Mockito.when(mockMyUnit.canChangeSecondaryFacing()).thenReturn(true);
        Mockito.doReturn(10.0).when(testRanker).getMaxDamageAtRange(Mockito.nullable(FireControl.class),
                                                                    Mockito.eq(mockMyUnit), Mockito.anyInt(),
                                                                    Mockito.anyBoolean(), Mockito.anyBoolean());

        final MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(testCoords);
        Mockito.when(mockPath.getFinalFacing()).thenReturn(3);
        Mockito.when(mockPath.getEntity()).thenReturn(mockMyUnit);

        // Test an aero unit (doesn't really do anything at this point).
        final Entity mockAero = Mockito.mock(Aero.class);
        Mockito.when(mockAero.getId()).thenReturn(2);
        Mockito.when(mockAero.isAero()).thenReturn(true);
        Mockito.when(mockAero.isAirborne()).thenReturn(true);
        Mockito.when(mockAero.isAirborneAeroOnGroundMap()).thenReturn(true);
        EntityEvaluationResponse expected = new EntityEvaluationResponse();
        EntityEvaluationResponse actual = testRanker.evaluateUnmovedEnemy(mockAero, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away, in my LoS and unable to kick my flank.
        Coords enemyCoords = new Coords(10, 15);
        int enemyMechId = 1;
        Entity mockEnemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech.getWeight()).thenReturn(50.0);
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
               .getMaxDamageAtRange(Mockito.nullable(FireControl.class), Mockito.eq(mockEnemyMech), Mockito.anyInt(),
                                    Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(false)
               .when(testRanker)
               .canFlankAndKick(Mockito.eq(mockEnemyMech), Mockito.any(Coords.class), Mockito.any(Coords.class),
                                Mockito.any(Coords.class), Mockito.anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(2.125);
        expected.setMyEstimatedDamage(2.5);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away but not in my LoS.
        enemyCoords = new Coords(10, 15);
        enemyMechId = 1;
        mockEnemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech.getWeight()).thenReturn(50.0);
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
               .getMaxDamageAtRange(Mockito.nullable(FireControl.class), Mockito.eq(mockEnemyMech), Mockito.anyInt(),
                                    Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(false)
               .when(testRanker)
               .canFlankAndKick(Mockito.eq(mockEnemyMech), Mockito.any(Coords.class), Mockito.any(Coords.class),
                                Mockito.any(Coords.class), Mockito.anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(2.125);
        expected.setMyEstimatedDamage(0.0);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away, not in my LoS and able to kick me.
        enemyCoords = new Coords(10, 15);
        enemyMechId = 1;
        mockEnemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech.getWeight()).thenReturn(50.0);
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
               .getMaxDamageAtRange(Mockito.nullable(FireControl.class), Mockito.eq(mockEnemyMech), Mockito.anyInt(),
                                    Mockito.anyBoolean(), Mockito.anyBoolean());
        Mockito.doReturn(true)
               .when(testRanker)
               .canFlankAndKick(Mockito.eq(mockEnemyMech), Mockito.any(Coords.class), Mockito.any(Coords.class),
                                Mockito.any(Coords.class), Mockito.anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(4.625);
        expected.setMyEstimatedDamage(0.0);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    @Test
    public void testEvaluateMovedEnemy() {
        final BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(mockPrincess).when(testRanker).getOwner();

        final MovePath mockPath = Mockito.mock(MovePath.class);
        final Entity mockMyUnit = Mockito.mock(BipedMech.class);
        final Crew mockCrew = Mockito.mock(Crew.class);
        final PilotOptions mockOptions = Mockito.mock(PilotOptions.class);
        
        
        // we need to initialize the unit's crew and options
        Mockito.when(mockPath.getEntity()).thenReturn(mockMyUnit);
        Mockito.when(mockMyUnit.getCrew()).thenReturn(mockCrew);
        Mockito.when(mockCrew.getOptions()).thenReturn(mockOptions);
        Mockito.when(mockOptions.booleanOption(Mockito.any(String.class))).thenReturn(false);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(new Coords(0, 0));

        final Game mockGame = Mockito.mock(Game.class);

        //
        final int mockEnemyMechId = 1;
        final Entity mockEnemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech.getId()).thenReturn(mockEnemyMechId);
        Mockito.when(mockEnemyMech.getPosition()).thenReturn(new Coords(1, 0));
        Mockito.when(mockEnemyMech.getCrew()).thenReturn(mockCrew);
        
        Mockito.doReturn(15.0)
               .when(testRanker)
               .calculateDamagePotential(Mockito.eq(mockEnemyMech), Mockito.any(EntityState.class),
                                         Mockito.any(MovePath.class), Mockito.any(EntityState.class), Mockito.anyInt(),
                                         Mockito.any(Game.class));
        Mockito.doReturn(10.0)
               .when(testRanker)
               .calculateKickDamagePotential(Mockito.eq(mockEnemyMech), Mockito.any(MovePath.class),
                                             Mockito.any(Game.class));
        Mockito.doReturn(14.5)
               .when(testRanker)
               .calculateMyDamagePotential(Mockito.any(MovePath.class), Mockito.eq(mockEnemyMech),
                                           Mockito.anyInt(), Mockito.any(Game.class));
        Mockito.doReturn(8.0)
               .when(testRanker)
               .calculateMyKickDamagePotential(Mockito.any(MovePath.class), Mockito.eq(mockEnemyMech),
                                               Mockito.any(Game.class));
        final Map<Integer, Double> testBestDamageByEnemies = new TreeMap<>();
        testBestDamageByEnemies.put(mockEnemyMechId, 0.0);
        Mockito.doReturn(testBestDamageByEnemies)
               .when(testRanker)
               .getBestDamageByEnemies();
        final EntityEvaluationResponse expected = new EntityEvaluationResponse();
        expected.setMyEstimatedDamage(14.5);
        expected.setMyEstimatedPhysicalDamage(8.0);
        expected.setEstimatedEnemyDamage(25.0);
        EntityEvaluationResponse actual = testRanker.evaluateMovedEnemy(mockEnemyMech, mockPath, mockGame);
        assertEntityEvaluationResponseEquals(expected, actual);

        // test for distance.
        Mockito.when(mockEnemyMech.getPosition()).thenReturn(new Coords(10, 0));
        expected.setMyEstimatedPhysicalDamage(0);
        expected.setEstimatedEnemyDamage(15);
        actual = testRanker.evaluateMovedEnemy(mockEnemyMech, mockPath, mockGame);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    private void assertEntityEvaluationResponseEquals(final EntityEvaluationResponse expected,
                                                      final EntityEvaluationResponse actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getMyEstimatedDamage(), actual.getMyEstimatedDamage(), TOLERANCE);
        Assert.assertEquals(expected.getMyEstimatedPhysicalDamage(), actual.getMyEstimatedPhysicalDamage(), TOLERANCE);
        Assert.assertEquals(expected.getEstimatedEnemyDamage(), actual.getEstimatedEnemyDamage(), TOLERANCE);
    }

    @Test
    public void testRankPath() {
        final BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(1.0)
               .when(testRanker)
               .getMovePathSuccessProbability(Mockito.any(MovePath.class), Mockito.any(StringBuilder.class));
        Mockito.doReturn(5)
               .when(testRanker)
               .distanceToClosestEdge(Mockito.any(Coords.class), Mockito.any(Game.class));
        Mockito.doReturn(20)
               .when(testRanker)
               .distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(CardinalEdge.class), Mockito.any(Game.class));
        Mockito.doReturn(12.0)
               .when(testRanker)
               .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class), Mockito.any(Game.class));
        Mockito.doReturn(0.0)
               .when(testRanker)
               .checkPathForHazards(Mockito.any(MovePath.class), Mockito.any(Entity.class), Mockito.any(Game.class));

        final Entity mockMover = Mockito.mock(BipedMech.class);
        Mockito.when(mockMover.isClan()).thenReturn(false);
        Mockito.when(mockPrincess.wantsToFallBack(Mockito.eq(mockMover))).thenReturn(false);

        final Coords finalCoords = new Coords(0, 0);

        final MoveStep mockLastStep = Mockito.mock(MoveStep.class);
        Mockito.when(mockLastStep.getFacing()).thenReturn(0);

        final MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getEntity()).thenReturn(mockMover);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(finalCoords);
        Mockito.when(mockPath.toString()).thenReturn("F F F");
        Mockito.when(mockPath.clone()).thenReturn(mockPath);
        Mockito.when(mockPath.getLastStep()).thenReturn(mockLastStep);
        Mockito.when(mockPath.getStepVector()).thenReturn(new Vector<>());

        final Board mockBoard = Mockito.mock(Board.class);
        Mockito.when(mockBoard.contains(Mockito.any(Coords.class))).thenReturn(true);
        final Coords boardCenter = Mockito.spy(new Coords(8, 8));
        Mockito.when(mockBoard.getCenter()).thenReturn(boardCenter);
        Mockito.doReturn(3)
               .when(boardCenter)
               .direction(Mockito.nullable(Coords.class));

        final GameOptions mockGameOptions = Mockito.mock(GameOptions.class);
        Mockito.when(mockGameOptions.booleanOption(Mockito.eq("no_clan_physical"))).thenReturn(false);

        final Game mockGame = Mockito.mock(Game.class);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);
        Mockito.when(mockGame.getOptions()).thenReturn(mockGameOptions);
        Mockito.when(mockGame.getArtilleryAttacks()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(mockPrincess.getGame()).thenReturn(mockGame);

        final List<Entity> testEnemies = new ArrayList<>();

        final Map<Integer, Double> bestDamageByEnemies = new TreeMap<>();
        Mockito.when(testRanker.getBestDamageByEnemies()).thenReturn(bestDamageByEnemies);

        final Coords enemyMech1Position = Mockito.spy(new Coords(10, 10));
        Mockito.doReturn(3)
               .when(enemyMech1Position)
               .direction(Mockito.nullable(Coords.class));
        final Entity mockEnemyMech1 = Mockito.mock(BipedMech.class);
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
               .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class),
                                   Mockito.any(Game.class));
        testEnemies.add(mockEnemyMech1);
        Mockito.doReturn(mockEnemyMech1)
               .when(testRanker)
               .findClosestEnemy(Mockito.eq(mockMover), Mockito.nullable(Coords.class), Mockito.any(Game.class));

        final Entity mockEnemyMech2 = Mockito.mock(BipedMech.class);
        Mockito.when(mockEnemyMech2.isOffBoard()).thenReturn(false);
        Mockito.when(mockEnemyMech2.getPosition()).thenReturn(new Coords(10, 10));
        Mockito.when(mockEnemyMech2.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockEnemyMech2.isImmobile()).thenReturn(false);
        Mockito.when(mockEnemyMech2.getId()).thenReturn(2);
        final EntityEvaluationResponse evalForMockEnemyMech2 = new EntityEvaluationResponse();
        evalForMockEnemyMech2.setMyEstimatedDamage(8.0);
        evalForMockEnemyMech2.setMyEstimatedPhysicalDamage(0.0);
        evalForMockEnemyMech2.setEstimatedEnemyDamage(15.0);
        Mockito.doReturn(evalForMockEnemyMech2)
               .when(testRanker)
               .evaluateUnmovedEnemy(Mockito.eq(mockEnemyMech2), Mockito.any(MovePath.class), Mockito.anyBoolean(), Mockito.anyBoolean());
        testEnemies.add(mockEnemyMech2);

        Coords friendsCoords = new Coords(10, 10);

        final double baseRank = -51.25; // The rank I expect to get with the above settings.

        RankedPath expected = new RankedPath(baseRank, mockPath, "Calculation: {" +
                                                                 "fall mod [" + LOG_DECIMAL.format(0) + " = " +
                                                                 LOG_DECIMAL.format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                                 "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " +
                                                                 LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40) + "] - " +
                                                                 "aggressionMod [" + LOG_DECIMAL.format(30) + " = " +
                                                                 LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                                 "herdingMod [" + LOG_DECIMAL.format(15) + " = " +
                                                                 LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                                 "facingMod [" + LOG_DECIMAL.format(0) + " = max(" +
                                                                 LOG_INT.format(0) + ", " +
                                                                 "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0)
                                                                 + " - " + LOG_INT.format(1) + "})]");
        RankedPath actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);

        // Change the move path success probability.
        Mockito.doReturn(0.5)
               .when(testRanker)
               .getMovePathSuccessProbability(Mockito.any(MovePath.class), Mockito.any(StringBuilder.class));
        expected = new RankedPath(-98.125, mockPath, "Calculation: {" +
                                                     "fall mod [" + LOG_DECIMAL.format(50) + " = " + LOG_DECIMAL
                .format(0.5) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                     "braveryMod [" + LOG_DECIMAL.format(-3.12) + " = " + LOG_PERCENT
                .format(0.5) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                     + LOG_DECIMAL.format(40) + "] - " +
                                                     "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                     "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                     "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                     "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                     LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            Assert.fail("Higher chance to fall should mean lower rank.");
        }
        Mockito.doReturn(0.75)
               .when(testRanker)
               .getMovePathSuccessProbability(Mockito.any(MovePath.class), Mockito.any(StringBuilder.class));
        expected = new RankedPath(-74.6875, mockPath, "Calculation: {" +
                                                      "fall mod [" + LOG_DECIMAL.format(25) + " = " + LOG_DECIMAL
                .format(0.25) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                      "braveryMod [" + LOG_DECIMAL.format(-4.69) + " = " +
                                                      LOG_PERCENT.format(0.75) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40) + "] - " +
                                                      "aggressionMod [" + LOG_DECIMAL.format(30) + " = " +
                                                      LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                      "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                      "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                      "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                      LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            Assert.fail("Higher chance to fall should mean lower rank.");
        }
        Mockito.doReturn(1.0)
               .when(testRanker)
               .getMovePathSuccessProbability(Mockito.any(MovePath.class), Mockito.any(StringBuilder.class));

        // Change the damage to enemy mech 1.
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class),
                                   Mockito.any(Game.class));
        expected = new RankedPath(-51.25, mockPath, "Calculation: {" +
                                                    "fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format
                (0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(40) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            Assert.fail("The more damage I do, the higher the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(4.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class),
                                   Mockito.any(Game.class));
        expected = new RankedPath(-61.0, mockPath, "Calculation: {" +
                                                   "fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format
                (0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                   "braveryMod [" + LOG_DECIMAL.format(-16) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(16) + " * " + LOG_DECIMAL.format(1.5) + ") - " +
                                                   LOG_DECIMAL.format(40) + "] - " +
                                                   "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                   "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                   "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format
                (0) + ", " +
                                                   "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                   LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            Assert.fail("The less damage I do, the lower the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class),
                                   Mockito.any(Game.class));

        // Change the damage done by enemy mech 1.
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(35.0);
        Mockito.doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class),
                                   Mockito.any(Game.class));
        expected = new RankedPath(-61.25, mockPath, "Calculation: {" +
                                                    "fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format
                (0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(-16.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(50) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        if (baseRank < actual.getRank()) {
            Assert.fail("The more damage they do, the lower the path rank should be.");
        }
        assertRankedPathEquals(expected, actual);
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(15.0);
        Mockito.doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class),
                                   Mockito.any(Game.class));
        expected = new RankedPath(-41.25, mockPath, "Calculation: {" +
                                                    "fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format
                (0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(3.75) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(30) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            Assert.fail("The less damage they do, the higher the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        Mockito.doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(Mockito.eq(mockEnemyMech1), Mockito.any(MovePath.class),
                                   Mockito.any(Game.class));

        // Change the distance to the enemy.
        Mockito.doReturn(2.0)
               .when(testRanker)
               .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class), Mockito.any(Game.class));
        expected = new RankedPath(-26.25, mockPath, "Calculation: {" +
                                                    "fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format
                (0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(40) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(5) + " = " + LOG_DECIMAL
                .format(2) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            Assert.fail("The closer I am to the enemy, the higher the path rank should be.");
        }
        Mockito.doReturn(22.0)
               .when(testRanker)
               .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class), Mockito.any(Game.class));
        expected = new RankedPath(-76.25, mockPath, "Calculation: " +
                                                    "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(40) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(55) + " = " + LOG_DECIMAL
                .format(22) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            Assert.fail("The further I am from the enemy, the lower the path rank should be.");
        }
        Mockito.doReturn(12.0)
               .when(testRanker)
               .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class), Mockito.any(Game.class));

        // Change the distance to my friends.
        friendsCoords = new Coords(0, 10);
        expected = new RankedPath(-46.25, mockPath, "Calculation: " +
                                                    "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(40) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [" + LOG_DECIMAL.format(10) + " = " + LOG_DECIMAL
                .format(10) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            Assert.fail("The closer I am to my friends, the higher the path rank should be.");
        }
        friendsCoords = new Coords(20, 10);
        expected = new RankedPath(-56.25, mockPath, "Calculation: " +
                                                    "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(40) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [" + LOG_DECIMAL.format(20) + " = " + LOG_DECIMAL
                .format(20) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            Assert.fail("The further I am from my friends, the lower the path rank should be.");
        }
        expected = new RankedPath(-36.25, mockPath, "Calculation: " +
                                                    "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(40) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [0 no friends] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, null);
        assertRankedPathEquals(expected, actual);
        friendsCoords = new Coords(10, 10);

        // Set myself up to run away.
        final double baseFleeingRank = -51.25;
        Mockito.when(mockMover.isCrippled()).thenReturn(true);
        expected = new RankedPath(baseFleeingRank, mockPath, "Calculation: " +
                                                             "{fall mod [" + LOG_DECIMAL.format(0) + " = " +
                                                             LOG_DECIMAL.format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                             "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " +
                                                             LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40) + "] - " +
                                                             "aggressionMod [" + LOG_DECIMAL.format(30) + " = " +
                                                             LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                             "herdingMod [" + LOG_DECIMAL.format(15) + " = " +
                                                             LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                             "facingMod [" + LOG_DECIMAL.format(0) + " = max(" +
                                                             LOG_INT.format(0) + ", " +
                                                             "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + "" +
                                                             " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        Mockito.doReturn(10)
               .when(testRanker)
               .distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(CardinalEdge.class), Mockito.any(Game.class));
        expected = new RankedPath(-51.25, mockPath, "Calculation: " +
                                                     "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                     "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                     + LOG_DECIMAL.format(40) + "] - " +
                                                     "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                     "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                     "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                     "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                     LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseFleeingRank > actual.getRank()) {
            Assert.fail("The closer I am to my home edge when fleeing, the higher the path rank should be.");
        }
        Mockito.doReturn(30)
               .when(testRanker)
               .distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(CardinalEdge.class), Mockito.any(Game.class));
        expected = new RankedPath(-51.25, mockPath, "Calculation: " +
                                                     "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                     "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                     + LOG_DECIMAL.format(40) + "] - " +
                                                     "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                     "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                     "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                     "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                     LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseFleeingRank < actual.getRank()) {
            Assert.fail("The further I am from my home edge when fleeing, the lower the path rank should be.");
        }
        Mockito.doReturn(20)
               .when(testRanker)
               .distanceToHomeEdge(Mockito.nullable(Coords.class), Mockito.any(CardinalEdge.class), Mockito.any(Game.class));
        Mockito.when(mockPrincess.wantsToFallBack(Mockito.eq(mockMover))).thenReturn(false);
        Mockito.when(mockMover.isCrippled()).thenReturn(false);

        // Change my facing.
        Mockito.when(mockPath.getFinalFacing()).thenReturn(1);
        expected = new RankedPath(baseRank, mockPath, "Calculation: " +
                                                      "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                      "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " +
                                                      LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40) + "] - " +
                                                      "aggressionMod [" + LOG_DECIMAL.format(30) + " = " +
                                                      LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                      "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                      "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                      "" + LOG_INT.format(50) + " * {" + LOG_INT.format(1) + " - " +
                                                      LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank != actual.getRank()) {
            Assert.fail("Being 1 hex off facing should make no difference in rank.");
        }
        Mockito.when(mockPath.getFinalFacing()).thenReturn(4);
        expected = new RankedPath(-101.25, mockPath, "Calculation: " +
                                                     "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                     "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                     + LOG_DECIMAL.format(40) + "] - " +
                                                     "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                     "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                     "facingMod [" + LOG_DECIMAL.format(50) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                     "" + LOG_INT.format(50) + " * {" + LOG_INT.format(2) + " - " +
                                                     LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            Assert.fail("Being 2 or more hexes off facing should lower the path rank.");
        }
        Mockito.when(mockPath.getFinalFacing()).thenReturn(3);
        expected = new RankedPath(-151.25, mockPath, "Calculation: " +
                                                     "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                     "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                     + LOG_DECIMAL.format(40) + "] - " +
                                                     "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                     "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                     "facingMod [" + LOG_DECIMAL.format(100) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                     "" + LOG_INT.format(50) + " * {" + LOG_INT.format(3) + " - " +
                                                     LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            Assert.fail("Being 2 or more hexes off facing should lower the path rank.");
        }
        Mockito.when(mockPath.getFinalFacing()).thenReturn(0);

        // Test not being able to find an enemy.
        Mockito.doReturn(null)
               .when(testRanker)
               .findClosestEnemy(Mockito.eq(mockMover), Mockito.nullable(Coords.class), Mockito.any(Game.class));
        expected = new RankedPath(-51.25, mockPath, "Calculation: " +
                                                    "{fall mod [" + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL
                .format(0) + " * " + LOG_DECIMAL.format
                (100) + "] + " +
                                                    "braveryMod [" + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT
                .format(1) + " * ((" + LOG_DECIMAL
                .format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                                                    + LOG_DECIMAL.format(40) + "] - " +
                                                    "aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL
                .format(12) + " * " + LOG_DECIMAL
                .format(2.5) + "] - " +
                                                    "herdingMod [" + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL
                .format(15) + " * " + LOG_DECIMAL
                .format(1) + "] - " +
                                                    "facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT
                .format(0) + ", " +
                                                    "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - " +
                                                    LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        Mockito.doReturn(mockEnemyMech1)
               .when(testRanker)
               .findClosestEnemy(Mockito.eq(mockMover), Mockito.nullable(Coords.class), Mockito.any(Game.class));
    }

    @Test
    public void testFindClosestEnemy() {
        final List<Entity> enemyList = new ArrayList<>(3);

        final Entity enemyMech = Mockito.mock(BipedMech.class);
        Mockito.when(enemyMech.getPosition()).thenReturn(new Coords(10, 10));
        Mockito.when(enemyMech.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(enemyMech.isImmobile()).thenReturn(false);
        enemyList.add(enemyMech);

        final Entity enemyTank = Mockito.mock(Tank.class);
        Mockito.when(enemyTank.getPosition()).thenReturn(new Coords(10, 15));
        Mockito.when(enemyTank.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(enemyTank.isImmobile()).thenReturn(false);
        enemyList.add(enemyTank);

        final Entity enemyBA = Mockito.mock(BattleArmor.class);
        Mockito.when(enemyBA.getPosition()).thenReturn(new Coords(15, 15));
        Mockito.when(enemyBA.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(enemyBA.isImmobile()).thenReturn(false);
        enemyList.add(enemyBA);

        final Coords position = new Coords(0, 0);
        final Entity me = Mockito.mock(BipedMech.class);
        final Game mockGame = Mockito.mock(Game.class);

        final BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(enemyList).when(mockPrincess).getEnemyEntities();

        Entity expected = enemyMech;
        Targetable actual = testRanker.findClosestEnemy(me, position, mockGame, false);
        Assert.assertEquals(expected, actual);

        // Add in an unmoved mech.
        final Entity unmovedMech = Mockito.mock(BipedMech.class);
        Mockito.when(unmovedMech.getPosition()).thenReturn(new Coords(9, 9)); // Now the closest by position.
        Mockito.when(unmovedMech.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(unmovedMech.isImmobile()).thenReturn(false);
        Mockito.when(unmovedMech.getWalkMP(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean()))
               .thenReturn(6); // Movement should cause it to be further away.
        enemyList.add(unmovedMech);
        expected = enemyMech;
        actual = testRanker.findClosestEnemy(me, position, mockGame);
        Assert.assertEquals(expected, actual);

        // Add in an aero unit right on top of me.
        final Entity mockAero = Mockito.mock(ConvFighter.class);
        Mockito.when(mockAero.isAero()).thenReturn(true);
        Mockito.when(mockAero.isAirborne()).thenReturn(true);
        Mockito.when(mockAero.isAirborneAeroOnGroundMap()).thenReturn(true);
        Mockito.when(mockAero.getPosition()).thenReturn(new Coords(1, 1)); // Right on top of me, but being an aero, it
        // shouldn't count.
        Mockito.when(mockAero.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockAero.isImmobile()).thenReturn(false);
        enemyList.add(mockAero);
        expected = enemyMech;
        actual = testRanker.findClosestEnemy(me, position, mockGame);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCalcAllyCenter() {
        final BasicPathRanker testRanker = new BasicPathRanker(mockPrincess);

        final int myId = 1;

        final List<Entity> friends = new ArrayList<>();

        final Board mockBoard = Mockito.mock(Board.class);
        Mockito.when(mockBoard.contains(Mockito.any(Coords.class))).thenReturn(true);

        final Game mockGame = Mockito.mock(Game.class);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);

        final Entity mockFriend1 = Mockito.mock(BipedMech.class);
        Mockito.when(mockFriend1.getId()).thenReturn(myId);
        Mockito.when(mockFriend1.isOffBoard()).thenReturn(false);
        final Coords friendPosition1 = new Coords(0, 0);
        Mockito.when(mockFriend1.getPosition()).thenReturn(friendPosition1);
        friends.add(mockFriend1);

        final Entity mockFriend2 = Mockito.mock(BipedMech.class);
        Mockito.when(mockFriend2.getId()).thenReturn(2);
        Mockito.when(mockFriend2.isOffBoard()).thenReturn(false);
        final Coords friendPosition2 = new Coords(10, 0);
        Mockito.when(mockFriend2.getPosition()).thenReturn(friendPosition2);
        friends.add(mockFriend2);

        final Entity mockFriend3 = Mockito.mock(BipedMech.class);
        Mockito.when(mockFriend3.getId()).thenReturn(3);
        Mockito.when(mockFriend3.isOffBoard()).thenReturn(false);
        final Coords friendPosition3 = new Coords(0, 10);
        Mockito.when(mockFriend3.getPosition()).thenReturn(friendPosition3);
        friends.add(mockFriend3);

        final Entity mockFriend4 = Mockito.mock(BipedMech.class);
        Mockito.when(mockFriend4.getId()).thenReturn(4);
        Mockito.when(mockFriend4.isOffBoard()).thenReturn(false);
        final Coords friendPosition4 = new Coords(10, 10);
        Mockito.when(mockFriend4.getPosition()).thenReturn(friendPosition4);
        friends.add(mockFriend4);

        // Test the default conditions.
        Coords expected = new Coords(6, 6);
        Coords actual = testRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);

        // Move one of my friends off-board.
        Mockito.when(mockFriend2.isOffBoard()).thenReturn(true);
        expected = new Coords(5, 10);
        actual = testRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        Mockito.when(mockFriend2.isOffBoard()).thenReturn(false);

        // Give one of my friends a null position.
        Mockito.when(mockFriend3.getPosition()).thenReturn(null);
        expected = new Coords(10, 5);
        actual = testRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        Mockito.when(mockFriend3.getPosition()).thenReturn(friendPosition3);

        // Give one of my friends an invalid position.
        Mockito.when(mockBoard.contains(Mockito.eq(friendPosition4))).thenReturn(false);
        expected = new Coords(5, 5);
        actual = testRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        Mockito.when(mockBoard.contains(Mockito.eq(friendPosition4))).thenReturn(true);

        // Test having no friends.
        actual = testRanker.calcAllyCenter(myId, new ArrayList<>(0), mockGame);
        Assert.assertNull(actual);
        actual = testRanker.calcAllyCenter(myId, null, mockGame);
        Assert.assertNull(actual);
        final List<Entity> solo = new ArrayList<>(1);
        solo.add(mockFriend1);
        actual = testRanker.calcAllyCenter(myId, solo, mockGame);
        Assert.assertNull(actual);
    }

    private void assertCoordsEqual(final Coords expected,
                                   final Coords actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testCalculateDamagePotential() {
        final Entity mockMe = generateMockEntity(10, 10);
        
        final BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(mockFireControl).when(testRanker).getFireControl(mockMe);

        final Board mockBoard = generateMockBoard();
        final Entity mockEnemy = generateMockEntity(10, 5);
        final MovePath mockPath = generateMockPath(10, 5, mockEnemy);
        final List<Entity> entities = new ArrayList<>();
        entities.add(mockMe);
        entities.add(mockEnemy);
        
        final Game mockGame = generateMockGame(entities, mockBoard);

        final FiringPlan mockFiringPlan = Mockito.mock(FiringPlan.class);
        Mockito.when(mockFiringPlan.getUtility()).thenReturn(12.5);
        Mockito.when(mockFireControl.determineBestFiringPlan(
                Mockito.any(FiringPlanCalculationParameters.class)))
               .thenReturn(mockFiringPlan);
        
        final EntityState mockShooterState = Mockito.mock(EntityState.class);
        final Coords mockEnemyPosition = mockEnemy.getPosition();
        Mockito.when(mockShooterState.getPosition()).thenReturn(mockEnemyPosition);
        final EntityState mockTargetState = Mockito.mock(EntityState.class);
        final Coords mockTargetPosition = mockMe.getPosition();
        Mockito.when(mockTargetState.getPosition()).thenReturn(mockTargetPosition);
        
        // test an enemy that is out of range
        int testDistance = 30;
        Assert.assertEquals(0.0, testRanker.calculateDamagePotential(mockEnemy, mockShooterState, mockPath,
                                                                   mockTargetState, testDistance, mockGame),
                            TOLERANCE);

        // Test an enemy that's in range and in Line of Sight.
        testDistance = 10;
        Assert.assertEquals(12.5,
                testRanker.calculateDamagePotential(mockEnemy,
                                                    mockShooterState,
                                                    mockPath,
                                                    mockTargetState,
                                                    testDistance,
                                                    mockGame),
                TOLERANCE);
        
        // Test an enemy both in range but out of LoS.
        Mockito.when(mockEnemy.getPosition()).thenReturn(null);
        Mockito.when(mockTargetState.getPosition()).thenReturn(null);
        Assert.assertEquals(0.0, testRanker.calculateDamagePotential(mockEnemy, mockShooterState, mockPath,
                mockTargetState, testDistance, mockGame),
                TOLERANCE);
    }

    @Test
    public void testCalculateMyDamagePotential() {
        final Entity mockMe = generateMockEntity(10, 10);
        
        final BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));
        Mockito.doReturn(mockFireControl).when(testRanker).getFireControl(mockMe);
       
        final Board mockBoard = generateMockBoard();
        final MovePath mockPath = generateMockPath(10, 10, mockMe);
        final Entity mockEnemy = generateMockEntity(10, 15);
        final List<Entity> entities = new ArrayList<>();
        entities.add(mockMe);
        entities.add(mockEnemy);

        int testDistance = 10;
        final Game mockGame = generateMockGame(entities, mockBoard);
        
        final FiringPlan mockFiringPlan = Mockito.mock(FiringPlan.class);
        Mockito.when(mockFiringPlan.getUtility()).thenReturn(25.2);
        Mockito.when(mockFireControl.determineBestFiringPlan(
                Mockito.any(FiringPlanCalculationParameters.class)))
               .thenReturn(mockFiringPlan);

        // Test being in range and LoS.
        double expected = 25.2;
        double actual = testRanker.calculateMyDamagePotential(mockPath, mockEnemy, testDistance, mockGame);
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Test being out of range.
        testDistance = 30;
        expected = 0;
        actual = testRanker.calculateMyDamagePotential(mockPath, mockEnemy, testDistance, mockGame);
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Test being in range but out of LoS.
        // Take the enemy off the board
        testDistance = 10;
        Mockito.when(mockEnemy.getPosition()).thenReturn(null);
        expected = 0;
        actual = testRanker.calculateMyDamagePotential(mockPath, mockEnemy, testDistance, mockGame);
        Assert.assertEquals(expected, actual, TOLERANCE);
    }

    private Board generateMockBoard() {
        // we'll be on a nice, empty, 20x20 board, not in space.
        final Board mockBoard = Mockito.mock(Board.class);
        final Hex mockHex = new Hex();
        Mockito.when(mockBoard.getHex(Mockito.any(Coords.class))).thenReturn(mockHex);
        Mockito.when(mockBoard.contains(Mockito.any(Coords.class))).thenReturn(true);
        Mockito.when(mockBoard.inSpace()).thenReturn(false);
        
        return mockBoard;
    }
    
    /**
     * Generates an entity at specific coordinates
     * Vital statistics:
     * ID: 1
     * Max weapon range: 21 (LRMs, obviously)
     * Final path coordinates: (10, 10)
     * Final path facing: straight north
     * No SPAs
     * Default crew
     * @return
     */
    private Entity generateMockEntity(int x, int y) {
        final Entity mockEntity = Mockito.mock(BipedMech.class);
        Mockito.when(mockEntity.getMaxWeaponRange()).thenReturn(21);
        
        final Crew mockCrew = Mockito.mock(Crew.class);
        Mockito.when(mockEntity.getCrew()).thenReturn(mockCrew);

        final PilotOptions mockOptions = Mockito.mock(PilotOptions.class);
        Mockito.when(mockCrew.getOptions()).thenReturn(mockOptions);
        Mockito.when(mockOptions.booleanOption(Mockito.anyString())).thenReturn(false);
        
        final Coords mockMyCoords = new Coords(x, y);
        Mockito.when(mockEntity.getPosition()).thenReturn(mockMyCoords);
        
        Mockito.when(mockEntity.getHeatCapacity()).thenReturn(20);
        Mockito.when(mockEntity.getHeat()).thenReturn(0);
        Mockito.when(mockEntity.isAirborne()).thenReturn(false);
        
        return mockEntity;
    }
    
    private MovePath generateMockPath(int x, int y, Entity mockEntity) {
        final MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getEntity()).thenReturn(mockEntity);
        
        final Coords mockMyCoords = new Coords(x, y);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(mockMyCoords);
        Mockito.when(mockPath.getFinalFacing()).thenReturn(0);
        
        return mockPath;
    }
   
    /** 
     * Generates a mock game object.
     * Sets up some values for the passed-in entities as well (game IDs, and the game object itself)
     * @param entities
     * @return
     */ 
    private Game generateMockGame(List<Entity> entities, Board mockBoard) {
       
        final Game mockGame = Mockito.mock(Game.class);
        
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);
        final GameOptions mockGameOptions = Mockito.mock(GameOptions.class);
        Mockito.when(mockGame.getOptions()).thenReturn(mockGameOptions);
        Mockito.when(mockGameOptions.booleanOption(Mockito.anyString())).thenReturn(false);
         
        for (int x = 0; x < entities.size(); x++) {
            Mockito.when(mockGame.getEntity(x + 1)).thenReturn(entities.get(x));
            Mockito.when(entities.get(x).getGame()).thenReturn(mockGame);
            Mockito.when(entities.get(x).getId()).thenReturn(x + 1);
        }
        
        return mockGame;
    }
    
    @Test
    public void testCheckPathForHazards() {
        final BasicPathRanker testRanker = Mockito.spy(new BasicPathRanker(mockPrincess));

        final Coords testCoordsOne = new Coords(10, 7);
        final Coords testCoordsTwo = new Coords(10, 8);
        final Coords testCoordsThree = new Coords(10, 9);
        final Coords testFinalCoords = new Coords(10, 10);

        final Hex mockHexOne = Mockito.mock(Hex.class);
        final Hex mockHexTwo = Mockito.mock(Hex.class);
        final Hex mockHexThree = Mockito.mock(Hex.class);
        final Hex mockFinalHex = Mockito.mock(Hex.class);
        Mockito.when(mockHexOne.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexTwo.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexOne.getCoords()).thenReturn(testCoordsOne);
        Mockito.when(mockHexTwo.getCoords()).thenReturn(testCoordsTwo);
        Mockito.when(mockHexThree.getCoords()).thenReturn(testCoordsThree);
        Mockito.when(mockFinalHex.getCoords()).thenReturn(testFinalCoords);

        final MoveStep mockStepOne = Mockito.mock(MoveStep.class);
        final MoveStep mockStepTwo = Mockito.mock(MoveStep.class);
        final MoveStep mockStepThree = Mockito.mock(MoveStep.class);
        final MoveStep mockFinalStep = Mockito.mock(MoveStep.class);
        Mockito.when(mockStepOne.getPosition()).thenReturn(testCoordsOne);
        Mockito.when(mockStepTwo.getPosition()).thenReturn(testCoordsTwo);
        Mockito.when(mockStepThree.getPosition()).thenReturn(testCoordsThree);
        Mockito.when(mockFinalStep.getPosition()).thenReturn(testFinalCoords);
        final Vector<MoveStep> stepVector = new Vector<>();
        stepVector.add(mockStepOne);
        stepVector.add(mockStepTwo);
        stepVector.add(mockStepThree);
        stepVector.add(mockFinalStep);

        final MovePath mockPath = Mockito.mock(MovePath.class);
        Mockito.when(mockPath.getLastStep()).thenReturn(mockFinalStep);
        Mockito.when(mockPath.getFinalCoords()).thenReturn(testFinalCoords);
        Mockito.when(mockPath.getStepVector()).thenReturn(stepVector);

        final Entity mockUnit = Mockito.mock(BipedMech.class);
        Mockito.when(mockUnit.locations()).thenReturn(8);
        Mockito.when(mockUnit.getArmor(Mockito.anyInt())).thenReturn(10);

        final Game mockGame = Mockito.mock(Game.class);

        final Board mockBoard = Mockito.mock(Board.class);
        Mockito.when(mockGame.getBoard()).thenReturn(mockBoard);
        Mockito.when(mockBoard.getHex(Mockito.eq(testFinalCoords))).thenReturn(mockFinalHex);
        Mockito.when(mockBoard.getHex(Mockito.eq(testCoordsOne))).thenReturn(mockHexOne);
        Mockito.when(mockBoard.getHex(Mockito.eq(testCoordsTwo))).thenReturn(mockHexTwo);
        Mockito.when(mockBoard.getHex(Mockito.eq(testCoordsThree))).thenReturn(mockHexThree);

        final Crew mockCrew = Mockito.mock(Crew.class);
        Mockito.when(mockUnit.getCrew()).thenReturn(mockCrew);
        Mockito.when(mockCrew.getPiloting()).thenReturn(5);

        final Building mockBuilding = Mockito.mock(Building.class);
        Mockito.when(mockBoard.getBuildingAt(Mockito.eq(testCoordsThree))).thenReturn(mockBuilding);
        Mockito.when(mockBuilding.getCurrentCF(Mockito.eq(testCoordsThree))).thenReturn(77);

        // Test waking fire-resistant BA through a burning building.
        final BattleArmor mockBA = Mockito.mock(BattleArmor.class);
        Mockito.when(mockBA.locations()).thenReturn(5);
        Mockito.when(mockBA.getArmor(Mockito.anyInt())).thenReturn(5);
        Mockito.when(mockBA.getCrew()).thenReturn(mockCrew);
        Mockito.when(mockBA.getHeatCapacity()).thenReturn(999);
        Mockito.when(mockBA.isFireResistant()).thenReturn(true);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.BUILDING, Terrains.FIRE});
        Assert.assertEquals(0, testRanker.checkPathForHazards(mockPath, mockBA, mockGame), TOLERANCE);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking a protomech over magma crust
        final Entity mockProto = Mockito.mock(Protomech.class);
        Mockito.when(mockProto.locations()).thenReturn(6);
        Mockito.when(mockProto.getArmor(Mockito.anyInt())).thenReturn(5);
        Mockito.when(mockProto.getCrew()).thenReturn(mockCrew);
        Mockito.when(mockProto.getHeatCapacity()).thenReturn(999);
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        Mockito.when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        Assert.assertEquals(166.7, testRanker.checkPathForHazards(mockPath, mockProto, mockGame), TOLERANCE);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test waking a protomech through a fire.
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.FIRE, Terrains.WOODS});
        Assert.assertEquals(50.0, testRanker.checkPathForHazards(mockPath, mockProto, mockGame), TOLERANCE);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking infantry over ice.
        final Entity mockInfantry = Mockito.mock(Infantry.class);
        Mockito.when(mockInfantry.locations()).thenReturn(2);
        Mockito.when(mockInfantry.getArmor(Mockito.anyInt())).thenReturn(0);
        Mockito.when(mockInfantry.getCrew()).thenReturn(mockCrew);
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        Mockito.when(mockHexThree.depth()).thenReturn(1);
        Assert.assertEquals(166.7, testRanker.checkPathForHazards(mockPath, mockInfantry, mockGame), TOLERANCE);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexThree.depth()).thenReturn(0);

        // Test driving a tank through a burning building.
        final Entity mockTank = Mockito.mock(Tank.class);
        Mockito.when(mockTank.locations()).thenReturn(5);
        Mockito.when(mockTank.getArmor(Mockito.anyInt())).thenReturn(10);
        Mockito.when(mockTank.getCrew()).thenReturn(mockCrew);
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.BUILDING, Terrains.FIRE});
        Assert.assertEquals(26.2859, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking through a building.
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.BUILDING});
        Assert.assertEquals(1.285, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking over 3 hexes of ice.
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockHexTwo.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        Mockito.when(mockHexTwo.terrainLevel(Terrains.WATER)).thenReturn(0);
        Mockito.when(mockHexThree.terrainLevel(Terrains.WATER)).thenReturn(1);
        Mockito.when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
        Mockito.when(mockHexTwo.depth()).thenReturn(0);
        Mockito.when(mockHexThree.depth()).thenReturn(1);
        Mockito.when(mockFinalHex.depth()).thenReturn(2);
        Mockito.when(mockUnit.getArmor(Mech.LOC_CT)).thenReturn(0);
        Assert.assertEquals(166.7, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockUnit.getArmor(Mech.LOC_CT)).thenReturn(10);
        Mockito.when(mockUnit.getArmor(Mech.LOC_RARM)).thenReturn(0);
        Assert.assertEquals(8.334, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockUnit.getArmor(Mech.LOC_RARM)).thenReturn(10);
        Mockito.when(mockHexTwo.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexTwo.terrainLevel(Terrains.WATER)).thenReturn(0);
        Mockito.when(mockHexThree.terrainLevel(Terrains.WATER)).thenReturn(0);
        Mockito.when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(0);
        Mockito.when(mockHexTwo.depth()).thenReturn(0);
        Mockito.when(mockHexThree.depth()).thenReturn(0);
        Mockito.when(mockFinalHex.depth()).thenReturn(0);

        // Test walking over 3 hexes of magma crust.
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockHexTwo.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        Mockito.when(mockHexTwo.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        Mockito.when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        Mockito.when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        Assert.assertEquals(17.8351, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockHexTwo.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexTwo.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        Mockito.when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        Mockito.when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test the stupidity of going prone in lava.
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockFinalStep.isProne()).thenReturn(true);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        Mockito.when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        Assert.assertEquals(66.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockFinalStep.isProne()).thenReturn(false);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test walking through 2 hexes of fire.
        Mockito.when(mockPath.isJumping()).thenReturn(false);
        Mockito.when(mockHexTwo.getTerrainTypes()).thenReturn(new int[]{Terrains.WOODS, Terrains.FIRE});
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.WOODS, Terrains.FIRE});
        Assert.assertEquals(4.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockHexTwo.getTerrainTypes()).thenReturn(new int[0]);
        Mockito.when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test jumping.
        Mockito.when(mockPath.isJumping()).thenReturn(true);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        Mockito.when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
        Mockito.when(mockFinalHex.depth()).thenReturn(2);
        Mockito.when(mockUnit.getArmor(Mockito.eq(Mech.LOC_LLEG))).thenReturn(0);
        Assert.assertEquals(25.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockUnit.getArmor(Mockito.eq(Mech.LOC_LLEG))).thenReturn(10);
        Mockito.when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(0);
        Mockito.when(mockFinalHex.depth()).thenReturn(0);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        Mockito.when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        Assert.assertEquals(14.5, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.WOODS, Terrains.FIRE});
        Assert.assertEquals(5.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        Mockito.when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.WOODS});
        Assert.assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Test a movement type that doesn't worry about ground terrain.
        Mockito.when(mockPath.getLastStepMovementType()).thenReturn(EntityMovementType.MOVE_FLYING);
        Assert.assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
    }
}
