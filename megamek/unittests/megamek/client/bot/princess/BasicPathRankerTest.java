/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.bot.princess;

import megamek.client.bot.princess.BotGeometry.HexLine;
import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.MovePath.Key;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/5/13 10:19 AM
 */
public class BasicPathRankerTest {
    private final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00");
    private final NumberFormat LOG_INT = NumberFormat.getIntegerInstance();
    private final NumberFormat LOG_PERCENT = NumberFormat.getPercentInstance();

    private final double TOLERANCE = 0.001;

    private static Princess mockPrincess;
    private static FireControl mockFireControl;

    @BeforeAll
    public static void beforeAll() {
        final BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getFallShameValue()).thenReturn(BehaviorSettings.FALL_SHAME_VALUES[5]);
        when(mockBehavior.getBraveryValue()).thenReturn(BehaviorSettings.BRAVERY[5]);
        when(mockBehavior.getHyperAggressionValue()).thenReturn(BehaviorSettings.HYPER_AGGRESSION_VALUES[5]);
        when(mockBehavior.getHerdMentalityValue()).thenReturn(BehaviorSettings.HERD_MENTALITY_VALUES[5]);
        when(mockBehavior.getSelfPreservationValue()).thenReturn(BehaviorSettings.SELF_PRESERVATION_VALUES[5]);

        mockFireControl = mock(FireControl.class);

        final IHonorUtil mockHonorUtil = mock(IHonorUtil.class);
        when(mockHonorUtil.isEnemyBroken(anyInt(), anyInt(), anyBoolean())).thenReturn(false);

        final List<Targetable> testAdditionalTargets = new ArrayList<>();
        FireControlState mockFireControlState = mock(FireControlState.class);
        when(mockFireControlState.getAdditionalTargets()).thenReturn(testAdditionalTargets);


        final Map<Key, Double> testSuccessProbabilities = new HashMap<>();
        PathRankerState mockPathRankerState = mock(PathRankerState.class);
        when(mockPathRankerState.getPathSuccessProbabilities()).thenReturn(testSuccessProbabilities);

        final UnitBehavior mockBehaviorTracker = mock(UnitBehavior.class);
        when(mockBehaviorTracker.getBehaviorType(any(Entity.class), any(Princess.class)))
            .thenReturn(BehaviorType.Engaged);

        mockPrincess = mock(Princess.class);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);
        when(mockPrincess.getFireControl(FireControlType.Basic)).thenReturn(mockFireControl);
        when(mockPrincess.getFireControl(any(Entity.class))).thenReturn(mockFireControl);
        when(mockPrincess.getHomeEdge(any(Entity.class))).thenReturn(CardinalEdge.NORTH);
        when(mockPrincess.getHonorUtil()).thenReturn(mockHonorUtil);
        when(mockPrincess.getFireControlState()).thenReturn(mockFireControlState);
        when(mockPrincess.getPathRankerState()).thenReturn(mockPathRankerState);
        when(mockPrincess.getMaxWeaponRange(any(Entity.class), anyBoolean())).thenReturn(21);
        when(mockPrincess.getUnitBehaviorTracker()).thenReturn(mockBehaviorTracker);
    }

    private void assertRankedPathEquals(final RankedPath expected, final RankedPath actual) {
        assertNotNull(actual, "Actual path is null.");
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

        if (!StringUtility.isNullOrBlank(failure.toString())) {
            fail(failure.toString());
        }
    }

    @Test
    public void testGetMovePathSuccessProbability() {
        final Entity mockMech = mock(BipedMech.class);
        when(mockMech.getMASCTarget()).thenReturn(3);

        final Crew mockCrew = mock(Crew.class);
        when(mockMech.getCrew()).thenReturn(mockCrew);

        final PilotOptions mockOptions = mock(PilotOptions.class);
        when(mockCrew.getOptions()).thenReturn(mockOptions);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        final MovePath mockPath = mock(MovePath.class);
        when(mockPath.hasActiveMASC()).thenReturn(false);
        when(mockPath.clone()).thenReturn(mockPath);
        when(mockPath.getEntity()).thenReturn(mockMech);

        final TargetRoll mockTargetRoll = mock(TargetRoll.class);
        when(mockTargetRoll.getValue()).thenReturn(8);
        when(mockTargetRoll.getDesc()).thenReturn("mock");

        final TargetRoll mockTargetRollTwo = mock(TargetRoll.class);
        when(mockTargetRollTwo.getValue()).thenReturn(5);
        when(mockTargetRollTwo.getDesc()).thenReturn("mock");

        final List<TargetRoll> testRollList = new ArrayList<>(2);
        testRollList.add(mockTargetRoll);
        testRollList.add(mockTargetRollTwo);

        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(testRollList).when(testRanker).getPSRList(eq(mockPath));

        double actual = testRanker.getMovePathSuccessProbability(mockPath, new StringBuilder());
        assertEquals(0.346, actual, TOLERANCE);

        // Add in a MASC roll.
        when(mockPath.hasActiveMASC()).thenReturn(true);
        actual = testRanker.getMovePathSuccessProbability(mockPath, new StringBuilder());
        assertEquals(0.346, actual, TOLERANCE);
    }

    @Test
    public void testEvaluateUnmovedEnemy() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockPrincess).when(testRanker).getOwner();

        final Coords testCoords = new Coords(10, 10);

        final Entity mockMyUnit = mock(BipedMech.class);
        when(mockMyUnit.canChangeSecondaryFacing()).thenReturn(true);
        doReturn(10.0).when(testRanker).getMaxDamageAtRange(nullable(FireControl.class),
                eq(mockMyUnit), anyInt(), anyBoolean(), anyBoolean());

        final MovePath mockPath = mock(MovePath.class);
        when(mockPath.getFinalCoords()).thenReturn(testCoords);
        when(mockPath.getFinalFacing()).thenReturn(3);
        when(mockPath.getEntity()).thenReturn(mockMyUnit);

        // Test an aero unit (doesn't really do anything at this point).
        final Entity mockAero = mock(Aero.class);
        when(mockAero.getId()).thenReturn(2);
        when(mockAero.isAero()).thenReturn(true);
        when(mockAero.isAirborne()).thenReturn(true);
        when(mockAero.isAirborneAeroOnGroundMap()).thenReturn(true);
        EntityEvaluationResponse expected = new EntityEvaluationResponse();
        EntityEvaluationResponse actual = testRanker.evaluateUnmovedEnemy(mockAero, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away, in my LoS and unable to kick my flank.
        Coords enemyCoords = new Coords(10, 15);
        int enemyMechId = 1;
        Entity mockEnemyMech = mock(BipedMech.class);
        when(mockEnemyMech.getWeight()).thenReturn(50.0);
        when(mockEnemyMech.getId()).thenReturn(enemyMechId);
        doReturn(enemyCoords)
               .when(testRanker)
               .getClosestCoordsTo(eq(enemyMechId), eq(testCoords));
        doReturn(true)
               .when(testRanker)
               .isInMyLoS(eq(mockEnemyMech), any(HexLine.class), any(HexLine.class));
        doReturn(8.5)
               .when(testRanker)
               .getMaxDamageAtRange(nullable(FireControl.class), eq(mockEnemyMech), anyInt(), anyBoolean(), anyBoolean());
        doReturn(false)
               .when(testRanker)
               .canFlankAndKick(eq(mockEnemyMech), any(Coords.class), any(Coords.class), any(Coords.class), anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(2.125);
        expected.setMyEstimatedDamage(2.5);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away but not in my LoS.
        enemyCoords = new Coords(10, 15);
        mockEnemyMech = mock(BipedMech.class);
        when(mockEnemyMech.getWeight()).thenReturn(50.0);
        when(mockEnemyMech.getId()).thenReturn(enemyMechId);
        doReturn(enemyCoords)
               .when(testRanker)
               .getClosestCoordsTo(eq(enemyMechId), eq(testCoords));
        doReturn(false)
               .when(testRanker)
               .isInMyLoS(eq(mockEnemyMech), any(HexLine.class), any(HexLine.class));
        doReturn(8.5)
               .when(testRanker)
               .getMaxDamageAtRange(nullable(FireControl.class), eq(mockEnemyMech), anyInt(), anyBoolean(), anyBoolean());
        doReturn(false)
               .when(testRanker)
               .canFlankAndKick(eq(mockEnemyMech), any(Coords.class), any(Coords.class), any(Coords.class), anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(2.125);
        expected.setMyEstimatedDamage(0.0);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);

        // Test an enemy mech 5 hexes away, not in my LoS and able to kick me.
        enemyCoords = new Coords(10, 15);
        mockEnemyMech = mock(BipedMech.class);
        when(mockEnemyMech.getWeight()).thenReturn(50.0);
        when(mockEnemyMech.getId()).thenReturn(enemyMechId);
        doReturn(enemyCoords)
               .when(testRanker)
               .getClosestCoordsTo(eq(enemyMechId), eq(testCoords));
        doReturn(false)
               .when(testRanker)
               .isInMyLoS(eq(mockEnemyMech), any(HexLine.class), any(HexLine.class));
        doReturn(8.5)
               .when(testRanker)
               .getMaxDamageAtRange(nullable(FireControl.class), eq(mockEnemyMech), anyInt(), anyBoolean(), anyBoolean());
        doReturn(true)
               .when(testRanker)
               .canFlankAndKick(eq(mockEnemyMech), any(Coords.class), any(Coords.class), any(Coords.class), anyInt());
        expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(4.625);
        expected.setMyEstimatedDamage(0.0);
        expected.setMyEstimatedPhysicalDamage(0.0);
        actual = testRanker.evaluateUnmovedEnemy(mockEnemyMech, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    @Test
    public void testEvaluateMovedEnemy() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockPrincess).when(testRanker).getOwner();

        final MovePath mockPath = mock(MovePath.class);
        final Entity mockMyUnit = mock(BipedMech.class);
        final Crew mockCrew = mock(Crew.class);
        final PilotOptions mockOptions = mock(PilotOptions.class);

        // we need to initialize the unit's crew and options
        when(mockPath.getEntity()).thenReturn(mockMyUnit);
        when(mockMyUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getOptions()).thenReturn(mockOptions);
        when(mockOptions.booleanOption(any(String.class))).thenReturn(false);
        when(mockPath.getFinalCoords()).thenReturn(new Coords(0, 0));

        final Game mockGame = mock(Game.class);

        final int mockEnemyMechId = 1;
        final Entity mockEnemyMech = mock(BipedMech.class);
        when(mockEnemyMech.getId()).thenReturn(mockEnemyMechId);
        when(mockEnemyMech.getPosition()).thenReturn(new Coords(1, 0));
        when(mockEnemyMech.getCrew()).thenReturn(mockCrew);

        doReturn(15.0)
               .when(testRanker)
               .calculateDamagePotential(eq(mockEnemyMech), any(EntityState.class),
                       any(MovePath.class), any(EntityState.class), anyInt(), any(Game.class));
        doReturn(10.0)
               .when(testRanker)
               .calculateKickDamagePotential(eq(mockEnemyMech), any(MovePath.class), any(Game.class));
        doReturn(14.5)
               .when(testRanker)
               .calculateMyDamagePotential(any(MovePath.class), eq(mockEnemyMech), anyInt(), any(Game.class));
        doReturn(8.0)
               .when(testRanker)
               .calculateMyKickDamagePotential(any(MovePath.class), eq(mockEnemyMech), any(Game.class));
        final Map<Integer, Double> testBestDamageByEnemies = new TreeMap<>();
        testBestDamageByEnemies.put(mockEnemyMechId, 0.0);
        doReturn(testBestDamageByEnemies)
               .when(testRanker)
               .getBestDamageByEnemies();
        final EntityEvaluationResponse expected = new EntityEvaluationResponse();
        expected.setMyEstimatedDamage(14.5);
        expected.setMyEstimatedPhysicalDamage(8.0);
        expected.setEstimatedEnemyDamage(25.0);
        EntityEvaluationResponse actual = testRanker.evaluateMovedEnemy(mockEnemyMech, mockPath, mockGame);
        assertEntityEvaluationResponseEquals(expected, actual);

        // test for distance.
        when(mockEnemyMech.getPosition()).thenReturn(new Coords(10, 0));
        expected.setMyEstimatedPhysicalDamage(0);
        expected.setEstimatedEnemyDamage(15);
        actual = testRanker.evaluateMovedEnemy(mockEnemyMech, mockPath, mockGame);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    private void assertEntityEvaluationResponseEquals(final EntityEvaluationResponse expected,
                                                      final EntityEvaluationResponse actual) {
        assertNotNull(actual);
        assertEquals(expected.getMyEstimatedDamage(), actual.getMyEstimatedDamage(), TOLERANCE);
        assertEquals(expected.getMyEstimatedPhysicalDamage(), actual.getMyEstimatedPhysicalDamage(), TOLERANCE);
        assertEquals(expected.getEstimatedEnemyDamage(), actual.getEstimatedEnemyDamage(), TOLERANCE);
    }

    @Test
    public void testRankPath() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(1.0)
               .when(testRanker)
               .getMovePathSuccessProbability(any(MovePath.class), any(StringBuilder.class));
        doReturn(5)
               .when(testRanker)
               .distanceToClosestEdge(any(Coords.class), any(Game.class));
        doReturn(20)
               .when(testRanker)
               .distanceToHomeEdge(any(Coords.class), any(CardinalEdge.class), any(Game.class));
        doReturn(12.0)
               .when(testRanker)
               .distanceToClosestEnemy(any(Entity.class), any(Coords.class), any(Game.class));
        doReturn(0.0)
               .when(testRanker)
               .checkPathForHazards(any(MovePath.class), any(Entity.class), any(Game.class));

        final Entity mockMover = mock(BipedMech.class);
        when(mockMover.isClan()).thenReturn(false);
        when(mockPrincess.wantsToFallBack(eq(mockMover))).thenReturn(false);

        final Coords finalCoords = new Coords(0, 0);

        final MoveStep mockLastStep = mock(MoveStep.class);
        when(mockLastStep.getFacing()).thenReturn(0);

        final MovePath mockPath = mock(MovePath.class);
        when(mockPath.getEntity()).thenReturn(mockMover);
        when(mockPath.getFinalCoords()).thenReturn(finalCoords);
        when(mockPath.toString()).thenReturn("F F F");
        when(mockPath.clone()).thenReturn(mockPath);
        when(mockPath.getLastStep()).thenReturn(mockLastStep);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());

        final Board mockBoard = mock(Board.class);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);
        final Coords boardCenter = spy(new Coords(8, 8));
        when(mockBoard.getCenter()).thenReturn(boardCenter);
        doReturn(3)
               .when(boardCenter)
               .direction(nullable(Coords.class));

        final GameOptions mockGameOptions = mock(GameOptions.class);
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL))).thenReturn(false);

        final Game mockGame = mock(Game.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getOptions()).thenReturn(mockGameOptions);
        when(mockGame.getArtilleryAttacks()).thenReturn(Collections.emptyEnumeration());
        when(mockPrincess.getGame()).thenReturn(mockGame);

        final List<Entity> testEnemies = new ArrayList<>();

        final Map<Integer, Double> bestDamageByEnemies = new TreeMap<>();
        when(testRanker.getBestDamageByEnemies()).thenReturn(bestDamageByEnemies);

        final Coords enemyMech1Position = spy(new Coords(10, 10));
        doReturn(3)
               .when(enemyMech1Position)
               .direction(nullable(Coords.class));
        final Entity mockEnemyMech1 = mock(BipedMech.class);
        when(mockEnemyMech1.isOffBoard()).thenReturn(false);
        when(mockEnemyMech1.getPosition()).thenReturn(enemyMech1Position);
        when(mockEnemyMech1.isSelectableThisTurn()).thenReturn(false);
        when(mockEnemyMech1.isImmobile()).thenReturn(false);
        when(mockEnemyMech1.getId()).thenReturn(1);
        EntityEvaluationResponse evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(eq(mockEnemyMech1), any(MovePath.class), any(Game.class));
        testEnemies.add(mockEnemyMech1);
        doReturn(mockEnemyMech1)
               .when(testRanker)
               .findClosestEnemy(eq(mockMover), nullable(Coords.class), any(Game.class));

        final Entity mockEnemyMech2 = mock(BipedMech.class);
        when(mockEnemyMech2.isOffBoard()).thenReturn(false);
        when(mockEnemyMech2.getPosition()).thenReturn(new Coords(10, 10));
        when(mockEnemyMech2.isSelectableThisTurn()).thenReturn(true);
        when(mockEnemyMech2.isImmobile()).thenReturn(false);
        when(mockEnemyMech2.getId()).thenReturn(2);
        final EntityEvaluationResponse evalForMockEnemyMech2 = new EntityEvaluationResponse();
        evalForMockEnemyMech2.setMyEstimatedDamage(8.0);
        evalForMockEnemyMech2.setMyEstimatedPhysicalDamage(0.0);
        evalForMockEnemyMech2.setEstimatedEnemyDamage(15.0);
        doReturn(evalForMockEnemyMech2)
               .when(testRanker)
               .evaluateUnmovedEnemy(eq(mockEnemyMech2), any(MovePath.class), anyBoolean(), anyBoolean());
        testEnemies.add(mockEnemyMech2);

        Coords friendsCoords = new Coords(10, 10);

        final double baseRank = -51.25; // The rank I expect to get with the above settings.

        RankedPath expected = new RankedPath(baseRank, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0)
                + " * " + LOG_DECIMAL.format(100) + "] + braveryMod ["
                + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT.format(1) + " * (("
                + LOG_DECIMAL.format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                + LOG_DECIMAL.format(40) + "] - aggressionMod ["
                + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL.format(12) + " * "
                + LOG_DECIMAL.format(2.5) + "] - herdingMod ["
                + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL.format(15) + " * "
                + LOG_DECIMAL.format(1) + "] - facingMod ["
                + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0) + ", "
                + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - "
                + LOG_INT.format(1) + "})]");
        RankedPath actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);

        // Change the move path success probability.
        doReturn(0.5)
               .when(testRanker)
               .getMovePathSuccessProbability(any(MovePath.class), any(StringBuilder.class));
        expected = new RankedPath(-98.125, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(50) + " = " + LOG_DECIMAL.format(0.5) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod ["
                + LOG_DECIMAL.format(-3.12) + " = " + LOG_PERCENT.format(0.5)
                + " * ((" + LOG_DECIMAL.format(22.5) + " * " + LOG_DECIMAL.format(1.5)
                + ") - " + LOG_DECIMAL.format(40) + "] - aggressionMod ["
                + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL.format(12) + " * "
                + LOG_DECIMAL.format(2.5) + "] - herdingMod ["
                + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL.format(15) + " * "
                + LOG_DECIMAL.format(1) + "] - facingMod ["
                + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0) + ", "
                + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - "
                + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("Higher chance to fall should mean lower rank.");
        }
        doReturn(0.75)
               .when(testRanker)
               .getMovePathSuccessProbability(any(MovePath.class), any(StringBuilder.class));
        expected = new RankedPath(-74.6875, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(25) + " = " + LOG_DECIMAL.format(0.25) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod ["
                + LOG_DECIMAL.format(-4.69) + " = " + LOG_PERCENT.format(0.75)
                + " * ((" + LOG_DECIMAL.format(22.5) + " * " + LOG_DECIMAL.format(1.5)
                + ") - " + LOG_DECIMAL.format(40) + "] - aggressionMod ["
                + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL.format(12) + " * "
                + LOG_DECIMAL.format(2.5) + "] - herdingMod ["
                + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL.format(15) + " * "
                + LOG_DECIMAL.format(1) + "] - facingMod ["
                + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0) + ", "
                + LOG_INT.format(50) + " * {" + LOG_INT.format(0)
                + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("Higher chance to fall should mean lower rank.");
        }
        doReturn(1.0)
               .when(testRanker)
               .getMovePathSuccessProbability(any(MovePath.class), any(StringBuilder.class));

        // Change the damage to enemy mech 1.
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(eq(mockEnemyMech1), any(MovePath.class), any(Game.class));
        expected = new RankedPath(-51.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod ["
                + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT.format(1) + " * (("
                + LOG_DECIMAL.format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                + LOG_DECIMAL.format(40) + "] - aggressionMod ["
                + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL.format(12) + " * "
                + LOG_DECIMAL.format(2.5) + "] - herdingMod ["
                + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL.format(15) + " * "
                + LOG_DECIMAL.format(1) + "] - facingMod [" + LOG_DECIMAL.format(0)
                + " = max(" + LOG_INT.format(0) + ", " + LOG_INT.format(50)
                + " * {" + LOG_INT.format(0) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            fail("The more damage I do, the higher the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(4.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(eq(mockEnemyMech1), any(MovePath.class), any(Game.class));
        expected = new RankedPath(-61.0, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-16)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(16)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1) + "] - facingMod ["
                + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0) + ", "
                + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - "
                + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("The less damage I do, the lower the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(eq(mockEnemyMech1), any(MovePath.class), any(Game.class));

        // Change the damage done by enemy mech 1.
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(35.0);
        doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(eq(mockEnemyMech1), any(MovePath.class), any(Game.class));
        expected = new RankedPath(-61.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-16.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(50)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max("
                + LOG_INT.format(0) + ", " + LOG_INT.format(50) + " * {"
                + LOG_INT.format(0) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        if (baseRank < actual.getRank()) {
            fail("The more damage they do, the lower the path rank should be.");
        }
        assertRankedPathEquals(expected, actual);
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(15.0);
        doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(eq(mockEnemyMech1), any(MovePath.class), any(Game.class));
        expected = new RankedPath(-41.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(3.75)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(30)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max("
                + LOG_INT.format(0) + ", " + LOG_INT.format(50) + " * {"
                + LOG_INT.format(0) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            fail("The less damage they do, the higher the path rank should be.");
        }
        evalForMockEnemyMech = new EntityEvaluationResponse();
        evalForMockEnemyMech.setMyEstimatedDamage(14.5);
        evalForMockEnemyMech.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMech.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMech)
               .when(testRanker)
               .evaluateMovedEnemy(eq(mockEnemyMech1), any(MovePath.class), any(Game.class));

        // Change the distance to the enemy.
        doReturn(2.0)
               .when(testRanker)
               .distanceToClosestEnemy(any(Entity.class), any(Coords.class), any(Game.class));
        expected = new RankedPath(-26.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + " + "braveryMod ["
                + LOG_DECIMAL.format(-6.25) + " = " + LOG_PERCENT.format(1) + " * (("
                + LOG_DECIMAL.format(22.5) + " * " + LOG_DECIMAL.format(1.5) + ") - "
                + LOG_DECIMAL.format(40) + "] - " +"aggressionMod [" + LOG_DECIMAL.format(5)
                + " = " + LOG_DECIMAL.format(2) + " * " + LOG_DECIMAL.format(2.5)
                + "] - " + "herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0)
                + ", " + "" + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - "
                + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            fail("The closer I am to the enemy, the higher the path rank should be.");
        }
        doReturn(22.0)
               .when(testRanker)
               .distanceToClosestEnemy(any(Entity.class), any(Coords.class), any(Game.class));
        expected = new RankedPath(-76.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(55) + " = "
                + LOG_DECIMAL.format(22) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0)
                + ", " + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - "
                + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("The further I am from the enemy, the lower the path rank should be.");
        }
        doReturn(12.0)
               .when(testRanker)
               .distanceToClosestEnemy(any(Entity.class), any(Coords.class), any(Game.class));

        // Change the distance to my friends.
        friendsCoords = new Coords(0, 10);
        expected = new RankedPath(-46.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(10) + " = "
                + LOG_DECIMAL.format(10) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max("
                + LOG_INT.format(0) + ", " + LOG_INT.format(50) + " * {"
                + LOG_INT.format(0) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            fail("The closer I am to my friends, the higher the path rank should be.");
        }
        friendsCoords = new Coords(20, 10);
        expected = new RankedPath(-56.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(20) + " = "
                + LOG_DECIMAL.format(20) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0)
                + ", " + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - "
                + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("The further I am from my friends, the lower the path rank should be.");
        }
        expected = new RankedPath(-36.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL .format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [0 no friends] - facingMod ["
                + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0) + ", "
                + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - "
                + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, null);
        assertRankedPathEquals(expected, actual);
        friendsCoords = new Coords(10, 10);

        // Set myself up to run away.
        final double baseFleeingRank = -51.25;
        when(mockMover.isCrippled()).thenReturn(true);
        expected = new RankedPath(baseFleeingRank, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max("
                + LOG_INT.format(0) + ", " + LOG_INT.format(50) + " * {"
                + LOG_INT.format(0) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        doReturn(10)
               .when(testRanker)
               .distanceToHomeEdge(any(Coords.class), any(CardinalEdge.class), any(Game.class));
        expected = new RankedPath(-51.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max(" + LOG_INT.format(0)
                + ", " + LOG_INT.format(50) + " * {" + LOG_INT.format(0) + " - "
                + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseFleeingRank > actual.getRank()) {
            fail("The closer I am to my home edge when fleeing, the higher the path rank should be.");
        }
        doReturn(30)
               .when(testRanker)
               .distanceToHomeEdge(any(Coords.class), any(CardinalEdge.class), any(Game.class));
        expected = new RankedPath(-51.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - "+ LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = " + LOG_DECIMAL.format(12)
                + " * " + LOG_DECIMAL.format(2.5) + "] - herdingMod ["
                + LOG_DECIMAL.format(15) + " = " + LOG_DECIMAL.format(15) + " * "
                + LOG_DECIMAL.format(1) + "] - facingMod [" + LOG_DECIMAL.format(0)
                + " = max(" + LOG_INT.format(0) + ", " + LOG_INT.format(50) + " * {"
                + LOG_INT.format(0) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseFleeingRank < actual.getRank()) {
            fail("The further I am from my home edge when fleeing, the lower the path rank should be.");
        }
        doReturn(20)
               .when(testRanker)
               .distanceToHomeEdge(nullable(Coords.class), any(CardinalEdge.class), any(Game.class));
        when(mockPrincess.wantsToFallBack(eq(mockMover))).thenReturn(false);
        when(mockMover.isCrippled()).thenReturn(false);

        // Change my facing.
        when(mockPath.getFinalFacing()).thenReturn(1);
        expected = new RankedPath(baseRank, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max("
                + LOG_INT.format(0) + ", " + LOG_INT.format(50) + " * {"
                + LOG_INT.format(1) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank != actual.getRank()) {
            fail("Being 1 hex off facing should make no difference in rank.");
        }
        when(mockPath.getFinalFacing()).thenReturn(4);
        expected = new RankedPath(-101.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(50) + " = max("
                + LOG_INT.format(0) + ", " + LOG_INT.format(50) + " * {"
                + LOG_INT.format(2) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("Being 2 or more hexes off facing should lower the path rank.");
        }
        when(mockPath.getFinalFacing()).thenReturn(3);
        expected = new RankedPath(-151.25, mockPath, "Calculation:{fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(100) + " = max("
                + LOG_INT.format(0) + ", " + "" + LOG_INT.format(50) + " * {"
                + LOG_INT.format(3) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("Being 2 or more hexes off facing should lower the path rank.");
        }
        when(mockPath.getFinalFacing()).thenReturn(0);

        // Test not being able to find an enemy.
        doReturn(null)
               .when(testRanker)
               .findClosestEnemy(eq(mockMover), nullable(Coords.class), any(Game.class));
        expected = new RankedPath(-51.25, mockPath, "Calculation: {fall mod ["
                + LOG_DECIMAL.format(0) + " = " + LOG_DECIMAL.format(0) + " * "
                + LOG_DECIMAL.format(100) + "] + braveryMod [" + LOG_DECIMAL.format(-6.25)
                + " = " + LOG_PERCENT.format(1) + " * ((" + LOG_DECIMAL.format(22.5)
                + " * " + LOG_DECIMAL.format(1.5) + ") - " + LOG_DECIMAL.format(40)
                + "] - aggressionMod [" + LOG_DECIMAL.format(30) + " = "
                + LOG_DECIMAL.format(12) + " * " + LOG_DECIMAL.format(2.5)
                + "] - herdingMod [" + LOG_DECIMAL.format(15) + " = "
                + LOG_DECIMAL.format(15) + " * " + LOG_DECIMAL.format(1)
                + "] - facingMod [" + LOG_DECIMAL.format(0) + " = max("
                + LOG_INT.format(0) + ", " + LOG_INT.format(50) + " * {"
                + LOG_INT.format(0) + " - " + LOG_INT.format(1) + "})]");
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        doReturn(mockEnemyMech1)
               .when(testRanker)
               .findClosestEnemy(eq(mockMover), nullable(Coords.class), any(Game.class));
    }

    @Test
    public void testFindClosestEnemy() {
        final List<Entity> enemyList = new ArrayList<>(3);

        final Entity enemyMech = mock(BipedMech.class);
        when(enemyMech.getPosition()).thenReturn(new Coords(10, 10));
        when(enemyMech.isSelectableThisTurn()).thenReturn(false);
        when(enemyMech.isImmobile()).thenReturn(false);
        enemyList.add(enemyMech);

        final Entity enemyTank = mock(Tank.class);
        when(enemyTank.getPosition()).thenReturn(new Coords(10, 15));
        when(enemyTank.isSelectableThisTurn()).thenReturn(false);
        when(enemyTank.isImmobile()).thenReturn(false);
        enemyList.add(enemyTank);

        final Entity enemyBA = mock(BattleArmor.class);
        when(enemyBA.getPosition()).thenReturn(new Coords(15, 15));
        when(enemyBA.isSelectableThisTurn()).thenReturn(false);
        when(enemyBA.isImmobile()).thenReturn(false);
        enemyList.add(enemyBA);

        final Coords position = new Coords(0, 0);
        final Entity me = mock(BipedMech.class);
        final Game mockGame = mock(Game.class);

        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(enemyList).when(mockPrincess).getEnemyEntities();

        assertEquals(enemyMech, testRanker.findClosestEnemy(me, position, mockGame, false));

        // Add in an unmoved mech.
        final Entity unmovedMech = mock(BipedMech.class);
        // Now the closest by position.
        when(unmovedMech.getPosition()).thenReturn(new Coords(9, 9));
        when(unmovedMech.isSelectableThisTurn()).thenReturn(true);
        when(unmovedMech.isImmobile()).thenReturn(false);
        // Movement should cause it to be further away.
        when(unmovedMech.getWalkMP(anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(6);
        enemyList.add(unmovedMech);
        assertEquals(enemyMech, testRanker.findClosestEnemy(me, position, mockGame));

        // Add in an aero unit right on top of me.
        final Entity mockAero = mock(ConvFighter.class);
        when(mockAero.isAero()).thenReturn(true);
        when(mockAero.isAirborne()).thenReturn(true);
        when(mockAero.isAirborneAeroOnGroundMap()).thenReturn(true);
        // Right on top of me, but being an aero, it shouldn't count
        when(mockAero.getPosition()).thenReturn(new Coords(1, 1));
        when(mockAero.isSelectableThisTurn()).thenReturn(false);
        when(mockAero.isImmobile()).thenReturn(false);
        enemyList.add(mockAero);
        assertEquals(enemyMech, testRanker.findClosestEnemy(me, position, mockGame));
    }

    @Test
    public void testCalcAllyCenter() {
        final BasicPathRanker testRanker = new BasicPathRanker(mockPrincess);

        final int myId = 1;

        final List<Entity> friends = new ArrayList<>();

        final Board mockBoard = mock(Board.class);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);

        final Game mockGame = mock(Game.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);

        final Entity mockFriend1 = mock(BipedMech.class);
        when(mockFriend1.getId()).thenReturn(myId);
        when(mockFriend1.isOffBoard()).thenReturn(false);
        final Coords friendPosition1 = new Coords(0, 0);
        when(mockFriend1.getPosition()).thenReturn(friendPosition1);
        friends.add(mockFriend1);

        final Entity mockFriend2 = mock(BipedMech.class);
        when(mockFriend2.getId()).thenReturn(2);
        when(mockFriend2.isOffBoard()).thenReturn(false);
        final Coords friendPosition2 = new Coords(10, 0);
        when(mockFriend2.getPosition()).thenReturn(friendPosition2);
        friends.add(mockFriend2);

        final Entity mockFriend3 = mock(BipedMech.class);
        when(mockFriend3.getId()).thenReturn(3);
        when(mockFriend3.isOffBoard()).thenReturn(false);
        final Coords friendPosition3 = new Coords(0, 10);
        when(mockFriend3.getPosition()).thenReturn(friendPosition3);
        friends.add(mockFriend3);

        final Entity mockFriend4 = mock(BipedMech.class);
        when(mockFriend4.getId()).thenReturn(4);
        when(mockFriend4.isOffBoard()).thenReturn(false);
        final Coords friendPosition4 = new Coords(10, 10);
        when(mockFriend4.getPosition()).thenReturn(friendPosition4);
        friends.add(mockFriend4);

        // Test the default conditions.
        Coords expected = new Coords(6, 6);
        Coords actual = testRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);

        // Move one of my friends off-board.
        when(mockFriend2.isOffBoard()).thenReturn(true);
        expected = new Coords(5, 10);
        actual = testRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        when(mockFriend2.isOffBoard()).thenReturn(false);

        // Give one of my friends a null position.
        when(mockFriend3.getPosition()).thenReturn(null);
        expected = new Coords(10, 5);
        actual = testRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        when(mockFriend3.getPosition()).thenReturn(friendPosition3);

        // Give one of my friends an invalid position.
        when(mockBoard.contains(eq(friendPosition4))).thenReturn(false);
        expected = new Coords(5, 5);
        actual = testRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        when(mockBoard.contains(eq(friendPosition4))).thenReturn(true);

        // Test having no friends.
        actual = testRanker.calcAllyCenter(myId, new ArrayList<>(0), mockGame);
        assertNull(actual);
        actual = testRanker.calcAllyCenter(myId, null, mockGame);
        assertNull(actual);
        final List<Entity> solo = new ArrayList<>(1);
        solo.add(mockFriend1);
        actual = testRanker.calcAllyCenter(myId, solo, mockGame);
        assertNull(actual);
    }

    private void assertCoordsEqual(final Coords expected, final Coords actual) {
        assertNotNull(actual);
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testCalculateDamagePotential() {
        final Entity mockMe = generateMockEntity(10, 10);

        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockFireControl).when(testRanker).getFireControl(mockMe);

        final Board mockBoard = generateMockBoard();
        final Entity mockEnemy = generateMockEntity(10, 5);
        final MovePath mockPath = generateMockPath(10, 5, mockEnemy);
        final List<Entity> entities = new ArrayList<>();
        entities.add(mockMe);
        entities.add(mockEnemy);

        final Game mockGame = generateMockGame(entities, mockBoard);

        final FiringPlan mockFiringPlan = mock(FiringPlan.class);
        when(mockFiringPlan.getUtility()).thenReturn(12.5);
        when(mockFireControl.determineBestFiringPlan(any(FiringPlanCalculationParameters.class)))
               .thenReturn(mockFiringPlan);

        final EntityState mockShooterState = mock(EntityState.class);
        final Coords mockEnemyPosition = mockEnemy.getPosition();
        when(mockShooterState.getPosition()).thenReturn(mockEnemyPosition);
        final EntityState mockTargetState = mock(EntityState.class);
        final Coords mockTargetPosition = mockMe.getPosition();
        when(mockTargetState.getPosition()).thenReturn(mockTargetPosition);

        // test an enemy that is out of range
        int testDistance = 30;
        assertEquals(0.0, testRanker.calculateDamagePotential(mockEnemy, mockShooterState,
                mockPath, mockTargetState, testDistance, mockGame), TOLERANCE);

        // Test an enemy that's in range and in Line of Sight.
        testDistance = 10;
        assertEquals(12.5, testRanker.calculateDamagePotential(mockEnemy, mockShooterState,
                mockPath, mockTargetState, testDistance, mockGame), TOLERANCE);

        // Test an enemy both in range but out of LoS.
        when(mockEnemy.getPosition()).thenReturn(null);
        when(mockTargetState.getPosition()).thenReturn(null);
        assertEquals(0.0, testRanker.calculateDamagePotential(mockEnemy, mockShooterState,
                mockPath, mockTargetState, testDistance, mockGame), TOLERANCE);
    }

    @Test
    public void testCalculateMyDamagePotential() {
        final Entity mockMe = generateMockEntity(10, 10);

        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockFireControl).when(testRanker).getFireControl(mockMe);

        final Board mockBoard = generateMockBoard();
        final MovePath mockPath = generateMockPath(10, 10, mockMe);
        final Entity mockEnemy = generateMockEntity(10, 15);
        final List<Entity> entities = new ArrayList<>();
        entities.add(mockMe);
        entities.add(mockEnemy);

        int testDistance = 10;
        final Game mockGame = generateMockGame(entities, mockBoard);

        final FiringPlan mockFiringPlan = mock(FiringPlan.class);
        when(mockFiringPlan.getUtility()).thenReturn(25.2);
        when(mockFireControl.determineBestFiringPlan(any(FiringPlanCalculationParameters.class)))
               .thenReturn(mockFiringPlan);

        // Test being in range and LoS.
        double expected = 25.2;
        double actual = testRanker.calculateMyDamagePotential(mockPath, mockEnemy, testDistance, mockGame);
        assertEquals(expected, actual, TOLERANCE);

        // Test being out of range.
        testDistance = 30;
        expected = 0;
        actual = testRanker.calculateMyDamagePotential(mockPath, mockEnemy, testDistance, mockGame);
        assertEquals(expected, actual, TOLERANCE);

        // Test being in range but out of LoS.
        // Take the enemy off the board
        testDistance = 10;
        when(mockEnemy.getPosition()).thenReturn(null);
        expected = 0;
        actual = testRanker.calculateMyDamagePotential(mockPath, mockEnemy, testDistance, mockGame);
        assertEquals(expected, actual, TOLERANCE);
    }

    private Board generateMockBoard() {
        // we'll be on a nice, empty, 20x20 board, not in space.
        final Board mockBoard = mock(Board.class);
        final Hex mockHex = new Hex();
        when(mockBoard.getHex(any(Coords.class))).thenReturn(mockHex);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);
        when(mockBoard.inSpace()).thenReturn(false);

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
        final Entity mockEntity = mock(BipedMech.class);
        when(mockEntity.getMaxWeaponRange()).thenReturn(21);

        final Crew mockCrew = mock(Crew.class);
        when(mockEntity.getCrew()).thenReturn(mockCrew);

        final PilotOptions mockOptions = mock(PilotOptions.class);
        when(mockCrew.getOptions()).thenReturn(mockOptions);
        when(mockOptions.booleanOption(anyString())).thenReturn(false);

        final Coords mockMyCoords = new Coords(x, y);
        when(mockEntity.getPosition()).thenReturn(mockMyCoords);

        when(mockEntity.getHeatCapacity()).thenReturn(20);
        when(mockEntity.getHeat()).thenReturn(0);
        when(mockEntity.isAirborne()).thenReturn(false);

        return mockEntity;
    }

    private MovePath generateMockPath(int x, int y, Entity mockEntity) {
        final MovePath mockPath = mock(MovePath.class);
        when(mockPath.getEntity()).thenReturn(mockEntity);

        final Coords mockMyCoords = new Coords(x, y);
        when(mockPath.getFinalCoords()).thenReturn(mockMyCoords);
        when(mockPath.getFinalFacing()).thenReturn(0);

        return mockPath;
    }

    /**
     * Generates a mock game object.
     * Sets up some values for the passed-in entities as well (game IDs, and the game object itself)
     * @param entities
     * @return
     */
    private Game generateMockGame(List<Entity> entities, Board mockBoard) {

        final Game mockGame = mock(Game.class);

        when(mockGame.getBoard()).thenReturn(mockBoard);
        final GameOptions mockGameOptions = mock(GameOptions.class);
        when(mockGame.getOptions()).thenReturn(mockGameOptions);
        when(mockGameOptions.booleanOption(anyString())).thenReturn(false);

        for (int x = 0; x < entities.size(); x++) {
            when(mockGame.getEntity(x + 1)).thenReturn(entities.get(x));
            when(entities.get(x).getGame()).thenReturn(mockGame);
            when(entities.get(x).getId()).thenReturn(x + 1);
        }

        return mockGame;
    }

    @Test
    public void testCheckPathForHazards() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));

        final Coords testCoordsOne = new Coords(10, 7);
        final Coords testCoordsTwo = new Coords(10, 8);
        final Coords testCoordsThree = new Coords(10, 9);
        final Coords testFinalCoords = new Coords(10, 10);

        final Hex mockHexOne = mock(Hex.class);
        final Hex mockHexTwo = mock(Hex.class);
        final Hex mockHexThree = mock(Hex.class);
        final Hex mockFinalHex = mock(Hex.class);
        when(mockHexOne.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexTwo.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexOne.getCoords()).thenReturn(testCoordsOne);
        when(mockHexTwo.getCoords()).thenReturn(testCoordsTwo);
        when(mockHexThree.getCoords()).thenReturn(testCoordsThree);
        when(mockFinalHex.getCoords()).thenReturn(testFinalCoords);

        final MoveStep mockStepOne = mock(MoveStep.class);
        final MoveStep mockStepTwo = mock(MoveStep.class);
        final MoveStep mockStepThree = mock(MoveStep.class);
        final MoveStep mockFinalStep = mock(MoveStep.class);
        when(mockStepOne.getPosition()).thenReturn(testCoordsOne);
        when(mockStepTwo.getPosition()).thenReturn(testCoordsTwo);
        when(mockStepThree.getPosition()).thenReturn(testCoordsThree);
        when(mockFinalStep.getPosition()).thenReturn(testFinalCoords);
        final Vector<MoveStep> stepVector = new Vector<>();
        stepVector.add(mockStepOne);
        stepVector.add(mockStepTwo);
        stepVector.add(mockStepThree);
        stepVector.add(mockFinalStep);

        final MovePath mockPath = mock(MovePath.class);
        when(mockPath.getLastStep()).thenReturn(mockFinalStep);
        when(mockPath.getFinalCoords()).thenReturn(testFinalCoords);
        when(mockPath.getStepVector()).thenReturn(stepVector);

        final Entity mockUnit = mock(BipedMech.class);
        when(mockUnit.locations()).thenReturn(8);
        when(mockUnit.getArmor(anyInt())).thenReturn(10);

        final Game mockGame = mock(Game.class);

        final Board mockBoard = mock(Board.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockBoard.getHex(eq(testFinalCoords))).thenReturn(mockFinalHex);
        when(mockBoard.getHex(eq(testCoordsOne))).thenReturn(mockHexOne);
        when(mockBoard.getHex(eq(testCoordsTwo))).thenReturn(mockHexTwo);
        when(mockBoard.getHex(eq(testCoordsThree))).thenReturn(mockHexThree);

        final Crew mockCrew = mock(Crew.class);
        when(mockUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);

        final Building mockBuilding = mock(Building.class);
        when(mockBoard.getBuildingAt(eq(testCoordsThree))).thenReturn(mockBuilding);
        when(mockBuilding.getCurrentCF(eq(testCoordsThree))).thenReturn(77);

        // Test waking fire-resistant BA through a burning building.
        final BattleArmor mockBA = mock(BattleArmor.class);
        when(mockBA.locations()).thenReturn(5);
        when(mockBA.getArmor(anyInt())).thenReturn(5);
        when(mockBA.getCrew()).thenReturn(mockCrew);
        when(mockBA.getHeatCapacity()).thenReturn(999);
        when(mockBA.isFireResistant()).thenReturn(true);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.BUILDING, Terrains.FIRE});
        assertEquals(0, testRanker.checkPathForHazards(mockPath, mockBA, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking a ProtoMek over magma crust
        final Entity mockProto = mock(Protomech.class);
        when(mockProto.locations()).thenReturn(6);
        when(mockProto.getArmor(anyInt())).thenReturn(5);
        when(mockProto.getCrew()).thenReturn(mockCrew);
        when(mockProto.getHeatCapacity()).thenReturn(999);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        assertEquals(166.7, testRanker.checkPathForHazards(mockPath, mockProto, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test waking a ProtoMek through a fire.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.FIRE, Terrains.WOODS});
        assertEquals(50.0, testRanker.checkPathForHazards(mockPath, mockProto, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking infantry over ice.
        final Entity mockInfantry = mock(Infantry.class);
        when(mockInfantry.locations()).thenReturn(2);
        when(mockInfantry.getArmor(anyInt())).thenReturn(0);
        when(mockInfantry.getCrew()).thenReturn(mockCrew);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        when(mockHexThree.depth()).thenReturn(1);
        assertEquals(166.7, testRanker.checkPathForHazards(mockPath, mockInfantry, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexThree.depth()).thenReturn(0);

        // Test driving a tank through a burning building.
        final Entity mockTank = mock(Tank.class);
        when(mockTank.locations()).thenReturn(5);
        when(mockTank.getArmor(anyInt())).thenReturn(10);
        when(mockTank.getCrew()).thenReturn(mockCrew);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.BUILDING, Terrains.FIRE});
        assertEquals(26.2859, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking through a building.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.BUILDING});
        assertEquals(1.285, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking over 3 hexes of ice.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexTwo.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        when(mockHexTwo.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockHexThree.terrainLevel(Terrains.WATER)).thenReturn(1);
        when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
        when(mockHexTwo.depth()).thenReturn(0);
        when(mockHexThree.depth()).thenReturn(1);
        when(mockFinalHex.depth()).thenReturn(2);
        when(mockUnit.getArmor(Mech.LOC_CT)).thenReturn(0);
        assertEquals(166.7, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockUnit.getArmor(Mech.LOC_CT)).thenReturn(10);
        when(mockUnit.getArmor(Mech.LOC_RARM)).thenReturn(0);
        assertEquals(8.334, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockUnit.getArmor(Mech.LOC_RARM)).thenReturn(10);
        when(mockHexTwo.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexTwo.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockHexThree.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockHexTwo.depth()).thenReturn(0);
        when(mockHexThree.depth()).thenReturn(0);
        when(mockFinalHex.depth()).thenReturn(0);

        // Test walking over 3 hexes of magma crust.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexTwo.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        when(mockHexTwo.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        assertEquals(17.8351, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockHexTwo.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexTwo.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test the stupidity of going prone in lava.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockFinalStep.isProne()).thenReturn(true);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        assertEquals(66.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalStep.isProne()).thenReturn(false);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[0]);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test walking through 2 hexes of fire.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexTwo.getTerrainTypes()).thenReturn(new int[]{Terrains.WOODS, Terrains.FIRE});
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[]{Terrains.WOODS, Terrains.FIRE});
        assertEquals(4.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockHexTwo.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test jumping.
        when(mockPath.isJumping()).thenReturn(true);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.ICE, Terrains.WATER});
        when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
        when(mockFinalHex.depth()).thenReturn(2);
        when(mockUnit.getArmor(eq(Mech.LOC_LLEG))).thenReturn(0);
        assertEquals(25.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockUnit.getArmor(eq(Mech.LOC_LLEG))).thenReturn(10);
        when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockFinalHex.depth()).thenReturn(0);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.MAGMA});
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        assertEquals(14.5, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.WOODS, Terrains.FIRE});
        assertEquals(5.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[]{Terrains.WOODS});
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Test a movement type that doesn't worry about ground terrain.
        when(mockPath.getLastStepMovementType()).thenReturn(EntityMovementType.MOVE_FLYING);
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
    }
}
