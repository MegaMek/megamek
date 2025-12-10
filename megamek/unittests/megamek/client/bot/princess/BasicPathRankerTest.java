/*
 * Copyright (c) 2000-2011 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;

import megamek.client.bot.princess.FireControl.FireControlType;
import megamek.client.bot.princess.UnitBehavior.BehaviorType;
import megamek.client.bot.princess.geometry.HexLine;
import megamek.codeUtilities.StringUtility;
import megamek.common.Hex;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.ArmorType;
import megamek.common.game.Game;
import megamek.common.moves.Key;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.planetaryConditions.Weather;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.utils.MockGenerators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 12/5/13 10:19 AM
 */
class BasicPathRankerTest {
    private final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
    private final NumberFormat LOG_PERCENT = NumberFormat.getPercentInstance();

    private final double TOLERANCE = 0.01;

    private Princess mockPrincess;
    private FireControl mockFireControl;

    @BeforeEach
    void beforeEach() {

        // We now need to make sure all armor types are initialized or mockito will
        // complain.
        if (!ArmorType.getAllTypes().hasMoreElements()) {
            ArmorType.initializeTypes();
        }

        final BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getFallShameValue()).thenReturn(BehaviorSettings.FALL_SHAME_VALUES[5]);
        when(mockBehavior.getBraveryValue()).thenReturn(BehaviorSettings.BRAVERY[5]);
        when(mockBehavior.getHyperAggressionValue()).thenReturn(BehaviorSettings.HYPER_AGGRESSION_VALUES[5]);
        when(mockBehavior.getHerdMentalityValue()).thenReturn(BehaviorSettings.HERD_MENTALITY_VALUES[5]);
        when(mockBehavior.getSelfPreservationValue()).thenReturn(BehaviorSettings.SELF_PRESERVATION_VALUES[5]);
        when(mockBehavior.getFavorHigherTMM()).thenReturn(0);
        when(mockBehavior.getAntiCrowding()).thenReturn(0);
        when(mockBehavior.getAllowFacingTolerance()).thenReturn(1);
        when(mockBehavior.getNumberOfEnemiesToConsiderFacing()).thenReturn(4);


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
        when(mockBehaviorTracker.getBehaviorType(any(Entity.class),
              any(Princess.class))).thenReturn(BehaviorType.Engaged);

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
    void testGetMovePathSuccessProbability() {
        final Entity mockMek = MockGenerators.generateMockBipedMek(0, 0);
        final MovePath mockPath = MockGenerators.generateMockPath(0, 0, mockMek);
        when(mockPath.hasActiveMASC()).thenReturn(false);

        final TargetRoll mockTargetRoll = MockGenerators.mockTargetRoll(8);
        final TargetRoll mockTargetRollTwo = MockGenerators.mockTargetRoll(5);
        final List<TargetRoll> testRollList = List.of(mockTargetRoll, mockTargetRollTwo);

        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(testRollList).when(testRanker).getPSRList(eq(mockPath));

        double actual = testRanker.getMovePathSuccessProbability(mockPath);
        assertEquals(0.346, actual, TOLERANCE);
    }

    @Test
    void testGetMovePathSuccessProbabilityWithMASC() {
        final Entity mockMek = MockGenerators.generateMockBipedMek(0, 0);
        when(mockMek.getMASCTarget()).thenReturn(3);

        final MovePath mockPath = MockGenerators.generateMockPath(0, 0, mockMek);
        when(mockPath.hasActiveMASC()).thenReturn(true);

        final TargetRoll mockTargetRoll = MockGenerators.mockTargetRoll(8);
        final TargetRoll mockTargetRollTwo = MockGenerators.mockTargetRoll(5);
        final List<TargetRoll> testRollList = List.of(mockTargetRoll, mockTargetRollTwo);

        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(testRollList).when(testRanker).getPSRList(eq(mockPath));

        double actual = testRanker.getMovePathSuccessProbability(mockPath);
        assertEquals(0.346, actual, TOLERANCE);
    }

    @Test
    void testEvaluateUnmovedAeroEnemy() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockPrincess).when(testRanker).getOwner();

        final Coords testCoords = new Coords(10, 10);
        final Entity mockMyUnit = MockGenerators.generateMockBipedMek(0, 0);
        when(mockMyUnit.canChangeSecondaryFacing()).thenReturn(true);

        doReturn(10.0).when(testRanker).getMaxDamageAtRange(eq(mockMyUnit), anyInt(), anyBoolean(), anyBoolean());

        final MovePath mockPath = MockGenerators.generateMockPath(testCoords, mockMyUnit);
        when(mockPath.getFinalFacing()).thenReturn(3);

        // Test an aero unit (doesn't really do anything at this point).
        final Entity mockAero = MockGenerators.generateMockAerospace(0, 0);
        when(mockAero.getId()).thenReturn(2);

        EntityEvaluationResponse expected = new EntityEvaluationResponse();
        EntityEvaluationResponse actual = testRanker.evaluateUnmovedEnemy(mockAero, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    @Test
    void testEvaluateUnmovedEnemyInLOSAndUnableToKick() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockPrincess).when(testRanker).getOwner();

        final Coords testCoords = new Coords(10, 10);
        final Entity mockMyUnit = MockGenerators.generateMockBipedMek(0, 0);
        when(mockMyUnit.canChangeSecondaryFacing()).thenReturn(true);

        doReturn(10.0).when(testRanker).getMaxDamageAtRange(eq(mockMyUnit), anyInt(), anyBoolean(), anyBoolean());

        final MovePath mockPath = MockGenerators.generateMockPath(testCoords, mockMyUnit);
        when(mockPath.getFinalFacing()).thenReturn(3);

        // Test an enemy mek 5 hexes away, in my LoS and unable to kick my flank.
        Coords enemyCoords = new Coords(10, 15);
        int enemyMekId = 1;
        Entity mockEnemyMek = MockGenerators.generateMockBipedMek(0, 0);
        when(mockEnemyMek.getId()).thenReturn(enemyMekId);
        doReturn(enemyCoords).when(testRanker).getClosestCoordsTo(eq(enemyMekId), eq(testCoords));
        doReturn(true).when(testRanker).isInMyLoS(eq(mockEnemyMek), any(HexLine.class), any(HexLine.class));
        doReturn(8.5).when(testRanker).getMaxDamageAtRange(eq(mockEnemyMek), anyInt(), anyBoolean(), anyBoolean());
        doReturn(false).when(testRanker)
              .canFlankAndKick(eq(mockEnemyMek), any(Coords.class), any(Coords.class), any(Coords.class), anyInt());

        EntityEvaluationResponse expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(2.125);
        expected.setMyEstimatedDamage(2.5);
        expected.setMyEstimatedPhysicalDamage(0.0);

        EntityEvaluationResponse actual = testRanker.evaluateUnmovedEnemy(mockEnemyMek, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    @Test
    void testEvaluateUnmovedEnemyNotInLOS() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockPrincess).when(testRanker).getOwner();

        final Coords testCoords = new Coords(10, 10);
        final Entity mockMyUnit = MockGenerators.generateMockBipedMek(0, 0);
        when(mockMyUnit.canChangeSecondaryFacing()).thenReturn(true);

        doReturn(10.0).when(testRanker).getMaxDamageAtRange(eq(mockMyUnit), anyInt(), anyBoolean(), anyBoolean());

        final MovePath mockPath = MockGenerators.generateMockPath(testCoords, mockMyUnit);
        when(mockPath.getFinalFacing()).thenReturn(3);

        // Test an enemy mek 5 hexes away but not in my LoS.
        Coords enemyCoords = new Coords(10, 15);
        int enemyMekId = 1;
        Entity mockEnemyMek = MockGenerators.generateMockBipedMek(0, 0);
        when(mockEnemyMek.getId()).thenReturn(enemyMekId);
        doReturn(enemyCoords).when(testRanker).getClosestCoordsTo(eq(enemyMekId), eq(testCoords));
        doReturn(false).when(testRanker).isInMyLoS(eq(mockEnemyMek), any(HexLine.class), any(HexLine.class));
        doReturn(8.5).when(testRanker).getMaxDamageAtRange(eq(mockEnemyMek), anyInt(), anyBoolean(), anyBoolean());
        doReturn(false).when(testRanker)
              .canFlankAndKick(eq(mockEnemyMek), any(Coords.class), any(Coords.class), any(Coords.class), anyInt());
        EntityEvaluationResponse expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(2.125);
        expected.setMyEstimatedDamage(0.0);
        expected.setMyEstimatedPhysicalDamage(0.0);
        EntityEvaluationResponse actual = testRanker.evaluateUnmovedEnemy(mockEnemyMek, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    @Test
    void testEvaluateUnmovedEnemyNotInLOSAndAbleToKick() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockPrincess).when(testRanker).getOwner();

        final Coords testCoords = new Coords(10, 10);
        final Entity mockMyUnit = MockGenerators.generateMockBipedMek(0, 0);
        when(mockMyUnit.canChangeSecondaryFacing()).thenReturn(true);

        doReturn(10.0).when(testRanker).getMaxDamageAtRange(eq(mockMyUnit), anyInt(), anyBoolean(), anyBoolean());

        final MovePath mockPath = MockGenerators.generateMockPath(testCoords, mockMyUnit);
        when(mockPath.getFinalFacing()).thenReturn(3);

        // Test an enemy mek 5 hexes away, not in my LoS and able to kick me.
        Coords enemyCoords = new Coords(10, 15);
        int enemyMekId = 1;
        Entity mockEnemyMek = MockGenerators.generateMockBipedMek(0, 0);
        when(mockEnemyMek.getId()).thenReturn(enemyMekId);
        doReturn(enemyCoords).when(testRanker).getClosestCoordsTo(eq(enemyMekId), eq(testCoords));
        doReturn(false).when(testRanker).isInMyLoS(eq(mockEnemyMek), any(HexLine.class), any(HexLine.class));
        doReturn(8.5).when(testRanker).getMaxDamageAtRange(eq(mockEnemyMek), anyInt(), anyBoolean(), anyBoolean());
        doReturn(true).when(testRanker)
              .canFlankAndKick(eq(mockEnemyMek), any(Coords.class), any(Coords.class), any(Coords.class), anyInt());

        EntityEvaluationResponse expected = new EntityEvaluationResponse();
        expected.setEstimatedEnemyDamage(4.625);
        expected.setMyEstimatedDamage(0.0);
        expected.setMyEstimatedPhysicalDamage(0.0);

        EntityEvaluationResponse actual = testRanker.evaluateUnmovedEnemy(mockEnemyMek, mockPath, false, false);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    @Test
    void testEvaluateMovedEnemy() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(mockPrincess).when(testRanker).getOwner();

        final MovePath mockPath = mock(MovePath.class);
        final Entity mockMyUnit = mock(BipedMek.class);
        final Crew mockCrew = mock(Crew.class);
        final PilotOptions mockOptions = mock(PilotOptions.class);

        // we need to initialize the unit's crew and options
        when(mockPath.getEntity()).thenReturn(mockMyUnit);
        when(mockMyUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getOptions()).thenReturn(mockOptions);
        when(mockOptions.booleanOption(any(String.class))).thenReturn(false);
        when(mockPath.getFinalCoords()).thenReturn(new Coords(0, 0));

        final Game mockGame = mock(Game.class);

        final int mockEnemyMekId = 1;
        final Entity mockEnemyMek = mock(BipedMek.class);
        when(mockEnemyMek.getId()).thenReturn(mockEnemyMekId);
        when(mockEnemyMek.getPosition()).thenReturn(new Coords(1, 0));
        when(mockEnemyMek.getCrew()).thenReturn(mockCrew);

        doReturn(15.0).when(testRanker)
              .calculateDamagePotential(eq(mockEnemyMek),
                    any(EntityState.class),
                    any(MovePath.class),
                    any(EntityState.class),
                    anyInt(),
                    any(Game.class));
        doReturn(10.0).when(testRanker)
              .calculateKickDamagePotential(eq(mockEnemyMek), any(MovePath.class), any(Game.class));
        doReturn(14.5).when(testRanker)
              .calculateMyDamagePotential(any(MovePath.class), eq(mockEnemyMek), anyInt(), any(Game.class));
        doReturn(8.0).when(testRanker)
              .calculateMyKickDamagePotential(any(MovePath.class), eq(mockEnemyMek), any(Game.class));
        final Map<Integer, Double> testBestDamageByEnemies = new TreeMap<>();
        testBestDamageByEnemies.put(mockEnemyMekId, 0.0);
        doReturn(testBestDamageByEnemies).when(testRanker).getBestDamageByEnemies();
        final EntityEvaluationResponse expected = new EntityEvaluationResponse();
        expected.setMyEstimatedDamage(14.5);
        expected.setMyEstimatedPhysicalDamage(8.0);
        expected.setEstimatedEnemyDamage(25.0);
        EntityEvaluationResponse actual = testRanker.evaluateMovedEnemy(mockEnemyMek, mockPath, mockGame);
        assertEntityEvaluationResponseEquals(expected, actual);

        // test for distance.
        when(mockEnemyMek.getPosition()).thenReturn(new Coords(10, 0));
        expected.setMyEstimatedPhysicalDamage(0);
        expected.setEstimatedEnemyDamage(15);
        actual = testRanker.evaluateMovedEnemy(mockEnemyMek, mockPath, mockGame);
        assertEntityEvaluationResponseEquals(expected, actual);
    }

    private void assertEntityEvaluationResponseEquals(final EntityEvaluationResponse expected,
          final EntityEvaluationResponse actual) {
        assertNotNull(actual);
        assertEquals(expected.getMyEstimatedDamage(), actual.getMyEstimatedDamage(), TOLERANCE);
        assertEquals(expected.getMyEstimatedPhysicalDamage(), actual.getMyEstimatedPhysicalDamage(), TOLERANCE);
        assertEquals(expected.getEstimatedEnemyDamage(), actual.getEstimatedEnemyDamage(), TOLERANCE);
    }

    /**
     * Helper class for building the expected result string in testRankPath. This allows for much easier maintenance of
     * the expected string format.
     */
    private static class RankPathResultBuilder {
        private final DecimalFormat decimalFormat;
        private final NumberFormat percentFormat;

        private double fallModValue;
        private double fallProbability;
        private double fallShameValue;

        private double braveryModValue;
        private double braveryProbability;
        private double damageEstimate;
        private double damageMultiplier;
        private double enemyDamage;

        private double aggressionModValue;
        private double distanceToEnemy;
        private double hyperAggressionValue;

        private double herdingModValue;
        private double distanceToAllies;
        private double herdMentalityValue;

        private int facingModValue;
        private int facingModConstant;
        private int facingDiff;

        private boolean noFriends = false;

        public RankPathResultBuilder(DecimalFormat decimalFormat, NumberFormat percentFormat) {
            this.decimalFormat = decimalFormat;
            this.percentFormat = percentFormat;
        }

        public RankPathResultBuilder withFallMod(double fallModValue, double fallProbability, double fallShameValue) {
            this.fallModValue = fallModValue;
            this.fallProbability = fallProbability;
            this.fallShameValue = fallShameValue;
            return this;
        }

        public RankPathResultBuilder withBraveryMod(double braveryModValue, double braveryProbability,
              double damageEstimate, double damageMultiplier, double enemyDamage) {
            this.braveryModValue = braveryModValue;
            this.braveryProbability = braveryProbability;
            this.damageEstimate = damageEstimate;
            this.damageMultiplier = damageMultiplier;
            this.enemyDamage = enemyDamage;
            return this;
        }

        public RankPathResultBuilder withAggressionMod(double aggressionModValue, double distanceToEnemy,
              double hyperAggressionValue) {
            this.aggressionModValue = aggressionModValue;
            this.distanceToEnemy = distanceToEnemy;
            this.hyperAggressionValue = hyperAggressionValue;
            return this;
        }

        public RankPathResultBuilder withHerdingMod(double herdingModValue, double distanceToAllies,
              double herdMentalityValue) {
            this.herdingModValue = herdingModValue;
            this.distanceToAllies = distanceToAllies;
            this.herdMentalityValue = herdMentalityValue;
            return this;
        }

        public RankPathResultBuilder noFriends() {
            this.noFriends = true;
            return this;
        }

        public void withFriends() {
            this.noFriends = false;
        }

        public RankPathResultBuilder withFacingMod(int facingModValue, int facingModConstant, int facingDiff) {
            this.facingModValue = facingModValue;
            this.facingModConstant = facingModConstant;
            this.facingDiff = facingDiff;
            return this;
        }

        public String build() {
            StringBuilder result = new StringBuilder("Calculation: {fall mod [");
            result.append(decimalFormat.format(fallModValue))
                  .append(" = ")
                  .append(decimalFormat.format(fallProbability))
                  .append(" * ")
                  .append(decimalFormat.format(fallShameValue))
                  .append("] + braveryMod [")
                  .append(decimalFormat.format(braveryModValue))
                  .append(" = ")
                  .append(percentFormat.format(braveryProbability))
                  .append(" * ((")
                  .append(decimalFormat.format(damageEstimate))
                  .append(" * ")
                  .append(decimalFormat.format(damageMultiplier))
                  .append(") - ")
                  .append(decimalFormat.format(enemyDamage))
                  .append(")] - aggressionMod [")
                  .append(decimalFormat.format(aggressionModValue))
                  .append(" = ")
                  .append(decimalFormat.format(distanceToEnemy))
                  .append(" * ")
                  .append(decimalFormat.format(hyperAggressionValue))
                  .append("] - herdingMod [");

            if (noFriends) {
                result.append("0 no friends");
            } else {
                result.append(decimalFormat.format(herdingModValue))
                      .append(" = ")
                      .append(decimalFormat.format(distanceToAllies))
                      .append(" * ")
                      .append(decimalFormat.format(herdMentalityValue));
            }

            result.append("] - facingMod [")
                  .append(facingModValue)
                  .append(" = ")
                  .append(facingModConstant)
                  .append(" * ")
                  .append(facingDiff)
                  .append("]");

            return result.toString();
        }
    }


    @Test
    void testRankPath() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(1.0).when(testRanker).getMovePathSuccessProbability(any(MovePath.class));
        doReturn(20).when(testRanker).distanceToHomeEdge(any(Coords.class), anyInt(), any(CardinalEdge.class),
              any(Game.class));
        doReturn(12.0).when(testRanker).distanceToClosestEnemy(any(Entity.class), any(Coords.class), any(Game.class));
        doReturn(0.0).when(testRanker).checkPathForHazards(any(MovePath.class), any(Entity.class), any(Game.class));

        final Entity mockMover = mock(BipedMek.class);
        when(mockMover.isClan()).thenReturn(false);
        when(mockPrincess.wantsToFallBack(eq(mockMover))).thenReturn(false);

        final Coords finalCoords = new Coords(0, 0);

        final MoveStep mockLastStep = mock(MoveStep.class);
        when(mockLastStep.getFacing()).thenReturn(0);

        final MovePath mockPath = mock(MovePath.class);
        when(mockPath.getEntity()).thenReturn(mockMover);
        when(mockPath.getFinalCoords()).thenReturn(finalCoords);
        when(mockPath.toString()).thenReturn("F F F R R");
        when(mockPath.clone()).thenReturn(mockPath);
        when(mockPath.getLastStep()).thenReturn(mockLastStep);
        when(mockPath.getStepVector()).thenReturn(new Vector<>());
        when(mockPath.getFinalFacing()).thenReturn(2);
        final TargetRoll mockTargetRoll = MockGenerators.mockTargetRoll(8);
        final TargetRoll mockTargetRollTwo = MockGenerators.mockTargetRoll(5);
        final List<TargetRoll> testRollList = List.of(mockTargetRoll, mockTargetRollTwo);

        doReturn(testRollList).when(testRanker).getPSRList(eq(mockPath));

        final Board mockBoard = mock(Board.class);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);
        final Coords boardCenter = spy(new Coords(8, 8));
        when(mockBoard.getCenter()).thenReturn(boardCenter);
        doReturn(3).when(boardCenter).direction(nullable(Coords.class));

        final GameOptions mockGameOptions = mock(GameOptions.class);
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL))).thenReturn(false);
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ADVANCED_BLACK_ICE))).thenReturn(false);

        final PlanetaryConditions mockPC = new PlanetaryConditions();
        mockPC.setTemperature(25);
        mockPC.setWeather(Weather.CLEAR);

        final Game mockGame = mock(Game.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getBoard(any(Targetable.class))).thenReturn(mockBoard);
        when(mockGame.getBoard(anyInt())).thenReturn(mockBoard);
        when(mockGame.getOptions()).thenReturn(mockGameOptions);
        when(mockGame.getArtilleryAttacks()).thenReturn(Collections.emptyEnumeration());
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPC);
        when(mockPrincess.getGame()).thenReturn(mockGame);
        when(mockMover.getGame()).thenReturn(mockGame);
        when(mockGame.onTheSameBoard(any(Targetable.class), any(Targetable.class))).thenCallRealMethod();

        final List<Entity> testEnemies = new ArrayList<>();

        final Map<Integer, Double> bestDamageByEnemies = new TreeMap<>();
        when(testRanker.getBestDamageByEnemies()).thenReturn(bestDamageByEnemies);

        final Coords enemyMek1Position = spy(new Coords(10, 10));
        doReturn(3).when(enemyMek1Position).direction(nullable(Coords.class));
        final Entity mockEnemyMek1 = mock(BipedMek.class);
        when(mockEnemyMek1.isOffBoard()).thenReturn(false);
        when(mockEnemyMek1.getPosition()).thenReturn(enemyMek1Position);
        when(mockEnemyMek1.isSelectableThisTurn()).thenReturn(false);
        when(mockEnemyMek1.isImmobile()).thenReturn(false);
        when(mockEnemyMek1.getId()).thenReturn(1);
        EntityEvaluationResponse evalForMockEnemyMek = new EntityEvaluationResponse();
        evalForMockEnemyMek.setMyEstimatedDamage(14.5);
        evalForMockEnemyMek.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMek.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMek).when(testRanker)
              .evaluateMovedEnemy(eq(mockEnemyMek1), any(MovePath.class), any(Game.class));
        testEnemies.add(mockEnemyMek1);
        doReturn(mockEnemyMek1).when(testRanker)
              .findClosestEnemy(eq(mockMover), nullable(Coords.class), any(Game.class));

        final Entity mockEnemyMek2 = mock(BipedMek.class);
        when(mockEnemyMek2.isOffBoard()).thenReturn(false);
        when(mockEnemyMek2.getPosition()).thenReturn(new Coords(10, 10));
        when(mockEnemyMek2.isSelectableThisTurn()).thenReturn(true);
        when(mockEnemyMek2.isImmobile()).thenReturn(false);
        when(mockEnemyMek2.getId()).thenReturn(2);
        final EntityEvaluationResponse evalForMockEnemyMek2 = new EntityEvaluationResponse();
        evalForMockEnemyMek2.setMyEstimatedDamage(8.0);
        evalForMockEnemyMek2.setMyEstimatedPhysicalDamage(0.0);
        evalForMockEnemyMek2.setEstimatedEnemyDamage(15.0);
        doReturn(evalForMockEnemyMek2).when(testRanker)
              .evaluateUnmovedEnemy(eq(mockEnemyMek2), any(MovePath.class), anyBoolean(), anyBoolean());
        testEnemies.add(mockEnemyMek2);

        Coords friendsCoords = new Coords(10, 10);

        // Create a builder for the results - we'll reuse this for all test cases
        RankPathResultBuilder builder = new RankPathResultBuilder(LOG_DECIMAL, LOG_PERCENT);

        // TEST CASE 1: Base case - default conditions
        final double baseRank = -51.25; // The rank I expect to get with the above settings.
        RankedPath expected = new RankedPath(baseRank,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());

        RankedPath actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);

        // TEST CASE 2: Change the move path success probability to 0.5
        doReturn(0.5).when(testRanker).getMovePathSuccessProbability(any(MovePath.class));
        expected = new RankedPath(-318.125,
              mockPath,
              builder.withFallMod(250, 0.5, 500)
                    .withBraveryMod(-23.12, 0.5, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("Higher chance to fall should mean lower rank.");
        }

        // TEST CASE 3: Change move path success probability to 0.75
        doReturn(0.75).when(testRanker).getMovePathSuccessProbability(any(MovePath.class));
        expected = new RankedPath(-184.6875,
              mockPath,
              builder.withFallMod(125, 0.25, 500)
                    .withBraveryMod(-14.69, 0.75, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("Higher chance to fall should mean lower rank.");
        }

        // Reset the move path success probability to 1.0 for later tests
        doReturn(1.0).when(testRanker).getMovePathSuccessProbability(any(MovePath.class));

        // TEST CASE 4: Change the damage to enemy mek 1 (no change to set up values)
        evalForMockEnemyMek = new EntityEvaluationResponse();
        evalForMockEnemyMek.setMyEstimatedDamage(14.5);
        evalForMockEnemyMek.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMek.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMek).when(testRanker)
              .evaluateMovedEnemy(eq(mockEnemyMek1), any(MovePath.class), any(Game.class));
        expected = new RankedPath(-51.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            fail("The more damage I do, the higher the path rank should be.");
        }

        // TEST CASE 5: Reduce the damage dealt to the enemy
        evalForMockEnemyMek = new EntityEvaluationResponse();
        evalForMockEnemyMek.setMyEstimatedDamage(4.5);
        evalForMockEnemyMek.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMek.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMek).when(testRanker)
              .evaluateMovedEnemy(eq(mockEnemyMek1), any(MovePath.class), any(Game.class));
        expected = new RankedPath(-61.0,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-16, 1, 16, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("The less damage I do, the lower the path rank should be.");
        }

        // Reset damage settings for later tests
        evalForMockEnemyMek = new EntityEvaluationResponse();
        evalForMockEnemyMek.setMyEstimatedDamage(14.5);
        evalForMockEnemyMek.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMek.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMek).when(testRanker)
              .evaluateMovedEnemy(eq(mockEnemyMek1), any(MovePath.class), any(Game.class));

        // TEST CASE 6: Increase the damage done by enemy mek 1
        evalForMockEnemyMek = new EntityEvaluationResponse();
        evalForMockEnemyMek.setMyEstimatedDamage(14.5);
        evalForMockEnemyMek.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMek.setEstimatedEnemyDamage(35.0);
        doReturn(evalForMockEnemyMek).when(testRanker)
              .evaluateMovedEnemy(eq(mockEnemyMek1), any(MovePath.class), any(Game.class));
        expected = new RankedPath(-61.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-16.25, 1, 22.5, 1.5, 50)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        if (baseRank < actual.getRank()) {
            fail("The more damage they do, the lower the path rank should be.");
        }
        assertRankedPathEquals(expected, actual);

        // TEST CASE 7: Decrease the damage done by enemy mek 1
        evalForMockEnemyMek = new EntityEvaluationResponse();
        evalForMockEnemyMek.setMyEstimatedDamage(14.5);
        evalForMockEnemyMek.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMek.setEstimatedEnemyDamage(15.0);
        doReturn(evalForMockEnemyMek).when(testRanker)
              .evaluateMovedEnemy(eq(mockEnemyMek1), any(MovePath.class), any(Game.class));
        expected = new RankedPath(-41.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(3.75, 1, 22.5, 1.5, 30)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            fail("The less damage they do, the higher the path rank should be.");
        }

        // Reset damage settings for an enemy
        evalForMockEnemyMek = new EntityEvaluationResponse();
        evalForMockEnemyMek.setMyEstimatedDamage(14.5);
        evalForMockEnemyMek.setMyEstimatedPhysicalDamage(8.0);
        evalForMockEnemyMek.setEstimatedEnemyDamage(25.0);
        doReturn(evalForMockEnemyMek).when(testRanker)
              .evaluateMovedEnemy(eq(mockEnemyMek1), any(MovePath.class), any(Game.class));

        // TEST CASE 8: Change the distance to the enemy (closer)
        doReturn(2.0).when(testRanker).distanceToClosestEnemy(any(Entity.class), any(Coords.class), any(Game.class));
        expected = new RankedPath(-26.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(5, 2, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            fail("The closer I am to the enemy, the higher the path rank should be.");
        }

        // TEST CASE 9: Change the distance to the enemy (farther)
        doReturn(22.0).when(testRanker).distanceToClosestEnemy(any(Entity.class), any(Coords.class), any(Game.class));
        expected = new RankedPath(-76.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(55, 22, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("The further I am from the enemy, the lower the path rank should be.");
        }

        // Reset distance to an enemy
        doReturn(12.0).when(testRanker).distanceToClosestEnemy(any(Entity.class), any(Coords.class), any(Game.class));

        // TEST CASE 10: Change the distance to my friends (closer)
        friendsCoords = new Coords(0, 10);
        expected = new RankedPath(-46.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(10, 10, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank > actual.getRank()) {
            fail("The closer I am to my friends, the higher the path rank should be.");
        }

        // TEST CASE 11: Change the distance to my friends (farther)
        friendsCoords = new Coords(20, 10);
        expected = new RankedPath(-56.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(20, 20, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("The further I am from my friends, the lower the path rank should be.");
        }

        // TEST CASE 12: Set friends to null (no friends) - we'll check this in test case 14

        // Reset friends coordinates
        friendsCoords = new Coords(10, 10);

        // TEST CASE 13: Set friends to null (no friends)
        expected = new RankedPath(-36.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .noFriends()
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, null);
        assertRankedPathEquals(expected, actual);

        // Reset friends coordinates
        friendsCoords = new Coords(10, 10);
        builder.withFriends();

        // TEST CASE 14: Set up to run away (crippled mek)
        final double baseFleeingRank = -51.25;
        when(mockMover.isCrippled()).thenReturn(true);
        expected = new RankedPath(baseFleeingRank,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);

        // TEST CASE 15: Fleeing - closer to home edge
        doReturn(10).when(testRanker)
              .distanceToHomeEdge(any(Coords.class), anyInt(), any(CardinalEdge.class), any(Game.class));
        expected = new RankedPath(-51.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseFleeingRank > actual.getRank()) {
            fail("The closer I am to my home edge when fleeing, the higher the path rank should be.");
        }

        // TEST CASE 16: Fleeing - farther from home edge
        doReturn(30).when(testRanker)
              .distanceToHomeEdge(any(Coords.class), anyInt(), any(CardinalEdge.class), any(Game.class));
        expected = new RankedPath(-51.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseFleeingRank < actual.getRank()) {
            fail("The further I am from my home edge when fleeing, the lower the path rank should be.");
        }

        // Reset fleeing settings
        doReturn(20).when(testRanker)
              .distanceToHomeEdge(nullable(Coords.class), anyInt(), any(CardinalEdge.class), any(Game.class));
        when(mockPrincess.wantsToFallBack(eq(mockMover))).thenReturn(false);
        when(mockMover.isCrippled()).thenReturn(false);

        // TEST CASE 17: Change final facing (1 off from optimal)
        when(mockPath.getFinalFacing()).thenReturn(1);
        expected = new RankedPath(baseRank,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        // Being 1 off facing makes no difference in rank

        // TEST CASE 18: Change final facing (2 off from optimal)
        when(mockPath.getFinalFacing()).thenReturn(4);
        expected = new RankedPath(-101.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(50, 50, 1)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("Being 2 or more facings off should lower the path rank.");
        }

        // TEST CASE 19: Change final facing (1 more off from optimal)
        when(mockPath.getFinalFacing()).thenReturn(3);
        expected = new RankedPath(-51.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(0, 50, 0)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        if (baseRank < actual.getRank()) {
            fail("Being 1 or more hexes off facing should lower the path rank.");
        }
        when(mockPath.getFinalFacing()).thenReturn(0);

        // TEST CASE 20: Not being able to find an enemy
        doReturn(null).when(testRanker).findClosestEnemy(eq(mockMover), nullable(Coords.class), any(Game.class));
        expected = new RankedPath(-101.25,
              mockPath,
              builder.withFallMod(0, 0, 500)
                    .withBraveryMod(-6.25, 1, 22.5, 1.5, 40)
                    .withAggressionMod(30, 12, 2.5)
                    .withHerdingMod(15, 15, 1)
                    .withFacingMod(50, 50, 1)
                    .build());
        actual = testRanker.rankPath(mockPath, mockGame, 18, 0.5, testEnemies, friendsCoords);
        assertRankedPathEquals(expected, actual);
        doReturn(mockEnemyMek1).when(testRanker)
              .findClosestEnemy(eq(mockMover), nullable(Coords.class), any(Game.class));
    }

    @Test
    void testFindClosestEnemy() {
        final List<Entity> enemyList = new ArrayList<>(3);

        final Entity enemyMek = mock(BipedMek.class);
        when(enemyMek.getPosition()).thenReturn(new Coords(10, 10));
        when(enemyMek.isSelectableThisTurn()).thenReturn(false);
        when(enemyMek.isImmobile()).thenReturn(false);
        enemyList.add(enemyMek);

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
        final Entity me = mock(BipedMek.class);
        final Game mockGame = mock(Game.class);
        when(mockGame.onTheSameBoard(any(Targetable.class), any(Targetable.class))).thenCallRealMethod();

        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        doReturn(enemyList).when(mockPrincess).getEnemyEntities();

        assertEquals(enemyMek, testRanker.findClosestEnemy(me, position, mockGame, false));

        // Add in an unmoved mek.
        final Entity unmovedMek = mock(BipedMek.class);
        // Now the closest by position.
        when(unmovedMek.getPosition()).thenReturn(new Coords(9, 9));
        when(unmovedMek.isSelectableThisTurn()).thenReturn(true);
        when(unmovedMek.isImmobile()).thenReturn(false);
        // Movement should cause it to be further away.
        when(unmovedMek.getWalkMP()).thenReturn(6);
        enemyList.add(unmovedMek);
        assertEquals(enemyMek, testRanker.findClosestEnemy(me, position, mockGame));

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
        assertEquals(enemyMek, testRanker.findClosestEnemy(me, position, mockGame));
    }

    @Test
    void testCalcAllyCenter() {
        final int myId = 1;

        final List<Entity> friends = new ArrayList<>();

        final Board mockBoard = mock(Board.class);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);

        final Game mockGame = mock(Game.class);
        when(mockGame.getBoard(anyInt())).thenReturn(mockBoard);
        when(mockGame.getBoard(any(Targetable.class))).thenReturn(mockBoard);
        when(mockGame.onTheSameBoard(any(Targetable.class), any(Targetable.class))).thenCallRealMethod();

        final Entity mockFriend1 = mock(BipedMek.class);
        when(mockFriend1.getId()).thenReturn(myId);
        when(mockFriend1.isOffBoard()).thenReturn(false);
        final Coords friendPosition1 = new Coords(0, 0);
        when(mockFriend1.getPosition()).thenReturn(friendPosition1);
        friends.add(mockFriend1);
        when(mockGame.getEntity(myId)).thenReturn(mockFriend1);

        final Entity mockFriend2 = mock(BipedMek.class);
        when(mockFriend2.getId()).thenReturn(2);
        when(mockFriend2.isOffBoard()).thenReturn(false);
        final Coords friendPosition2 = new Coords(10, 0);
        when(mockFriend2.getPosition()).thenReturn(friendPosition2);
        friends.add(mockFriend2);

        final Entity mockFriend3 = mock(BipedMek.class);
        when(mockFriend3.getId()).thenReturn(3);
        when(mockFriend3.isOffBoard()).thenReturn(false);
        final Coords friendPosition3 = new Coords(0, 10);
        when(mockFriend3.getPosition()).thenReturn(friendPosition3);
        friends.add(mockFriend3);

        final Entity mockFriend4 = mock(BipedMek.class);
        when(mockFriend4.getId()).thenReturn(4);
        when(mockFriend4.isOffBoard()).thenReturn(false);
        final Coords friendPosition4 = new Coords(10, 10);
        when(mockFriend4.getPosition()).thenReturn(friendPosition4);
        friends.add(mockFriend4);

        // Test the default conditions.
        Coords expected = new Coords(7, 7);
        Coords actual = BasicPathRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);

        // Move one of my friends off-board.
        when(mockFriend2.isOffBoard()).thenReturn(true);
        expected = new Coords(5, 10);
        actual = BasicPathRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        when(mockFriend2.isOffBoard()).thenReturn(false);

        // Give one of my friends a null position.
        when(mockFriend3.getPosition()).thenReturn(null);
        expected = new Coords(10, 5);
        actual = BasicPathRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        when(mockFriend3.getPosition()).thenReturn(friendPosition3);

        // Give one of my friends an invalid position.
        when(mockBoard.contains(eq(friendPosition4))).thenReturn(false);
        expected = new Coords(5, 5);
        actual = BasicPathRanker.calcAllyCenter(myId, friends, mockGame);
        assertCoordsEqual(expected, actual);
        when(mockBoard.contains(eq(friendPosition4))).thenReturn(true);

        // Test having no friends.
        actual = BasicPathRanker.calcAllyCenter(myId, new ArrayList<>(0), mockGame);
        assertNull(actual);
        actual = BasicPathRanker.calcAllyCenter(myId, null, mockGame);
        assertNull(actual);
        // I'm my own best friend
        final List<Entity> solo = new ArrayList<>(1);
        solo.add(mockFriend1);
        actual = BasicPathRanker.calcAllyCenter(myId, solo, mockGame);
        // You are just coping, you have no friends.
        assertNull(actual);
    }

    private void assertCoordsEqual(final Coords expected, final Coords actual) {
        assertNotNull(actual);
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    void testCalculateDamagePotential() {
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
        when(mockFireControl.determineBestFiringPlan(any(FiringPlanCalculationParameters.class))).thenReturn(
              mockFiringPlan);

        final EntityState mockShooterState = mock(EntityState.class);
        final Coords mockEnemyPosition = mockEnemy.getPosition();
        when(mockShooterState.getPosition()).thenReturn(mockEnemyPosition);
        final EntityState mockTargetState = mock(EntityState.class);
        final Coords mockTargetPosition = mockMe.getPosition();
        when(mockTargetState.getPosition()).thenReturn(mockTargetPosition);

        // test an enemy out of range
        int testDistance = 30;
        assertEquals(0.0,
              testRanker.calculateDamagePotential(mockEnemy,
                    mockShooterState,
                    mockPath,
                    mockTargetState,
                    testDistance,
                    mockGame),
              TOLERANCE);

        // Test an enemy in range and in Line of Sight.
        testDistance = 10;
        assertEquals(12.5,
              testRanker.calculateDamagePotential(mockEnemy,
                    mockShooterState,
                    mockPath,
                    mockTargetState,
                    testDistance,
                    mockGame),
              TOLERANCE);

        // Test an enemy both in range but out of LoS.
        when(mockEnemy.getPosition()).thenReturn(null);
        when(mockTargetState.getPosition()).thenReturn(null);
        assertEquals(0.0,
              testRanker.calculateDamagePotential(mockEnemy,
                    mockShooterState,
                    mockPath,
                    mockTargetState,
                    testDistance,
                    mockGame),
              TOLERANCE);
    }

    @Test
    void testCalculateMyDamagePotential() {
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
        when(mockFireControl.determineBestFiringPlan(any(FiringPlanCalculationParameters.class))).thenReturn(
              mockFiringPlan);

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
        when(mockBoard.isSpace()).thenReturn(false);
        when(mockBoard.isGround()).thenReturn(true);

        return mockBoard;
    }

    /**
     * Generates an entity at specific coordinates Vital statistics: ID: 1 Max weapon range: 21 (LRMs, obviously) Final
     * path coordinates: (10, 10) Final path facing: straight north No SPAs Default crew
     *
     * @return a Mock Entity
     */
    private Entity generateMockEntity(int x, int y) {
        final Entity mockEntity = mock(BipedMek.class);
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
        when(mockPath.getFinalBoardId()).thenReturn(0);

        return mockPath;
    }

    /**
     * Generates a mock game object. Sets up some values for the passed-in entities as well (game IDs, and the game
     * object itself)
     *
     * @param entities A List of {@link Entity} Objects
     *
     * @return A Mock {@link Game} object
     */
    private Game generateMockGame(List<Entity> entities, Board mockBoard) {

        final Game mockGame = mock(Game.class);

        when(mockGame.getBoard()).thenReturn(mockBoard);
        final GameOptions mockGameOptions = mock(GameOptions.class);
        when(mockGame.getOptions()).thenReturn(mockGameOptions);
        when(mockGameOptions.booleanOption(anyString())).thenReturn(false);
        when(mockGame.getBoard(anyInt())).thenReturn(mockBoard);
        when(mockGame.hasBoard(0)).thenReturn(true);
        when(mockGame.hasBoardLocation(any(Coords.class), anyInt())).thenReturn(true);
        when(mockGame.getHex(any(Coords.class), anyInt())).thenCallRealMethod();

        for (int x = 0; x < entities.size(); x++) {
            when(mockGame.getEntity(x + 1)).thenReturn(entities.get(x));
            when(entities.get(x).getGame()).thenReturn(mockGame);
            when(entities.get(x).getId()).thenReturn(x + 1);
        }

        return mockGame;
    }

    List<Coords> setupCoords(String... pairs) {
        List<Coords> coords = new ArrayList<>();
        for (String pair : pairs) {
            String[] xyPair = pair.split(",");
            int x = Integer.parseInt(xyPair[0].strip());
            int y = Integer.parseInt(xyPair[1].strip());
            coords.add(new Coords(x, y));
        }
        return coords;
    }

    List<Hex> setupHexes(List<Coords> coords) {
        List<Hex> hexes = new ArrayList<>();
        for (Coords c : coords) {
            Hex mockHex = mock(Hex.class);
            when(mockHex.getTerrainTypes()).thenReturn(new int[0]);
            when(mockHex.getCoords()).thenReturn(c);
            hexes.add(mockHex);
        }
        return hexes;
    }

    Vector<MoveStep> setupMoveStepVector(List<Coords> coords) {
        Vector<MoveStep> moves = new Vector<>();
        for (Coords c : coords) {
            MoveStep mockStep = mock(MoveStep.class);
            when(mockStep.getPosition()).thenReturn(c);
            moves.add(mockStep);
        }
        return moves;
    }

    MovePath setupPath(Vector<MoveStep> steps) {
        Coords finalCoords = steps.lastElement().getPosition();
        MovePath mockPath = mock(MovePath.class);
        when(mockPath.getLastStep()).thenReturn(steps.lastElement());
        when(mockPath.getFinalCoords()).thenReturn(finalCoords);
        when(mockPath.getStepVector()).thenReturn(steps);

        return mockPath;
    }

    Game setupGame(List<Coords> coords, List<Hex> hexes) {
        Game mockGame = mock(Game.class);
        Board mockBoard = mock(Board.class);
        when(mockGame.getBoard()).thenReturn(mockBoard);
        when(mockGame.getBoard(anyInt())).thenReturn(mockBoard);
        for (Coords c : coords) {
            when(mockBoard.getHex(eq(c))).thenReturn(hexes.get(coords.indexOf(c)));
        }
        return mockGame;
    }

    @Test
    void testCheckPathForHazards() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));

        final List<Coords> testCoords = setupCoords("10,7", "10,8", "10,9", "10,10");
        final Coords testCoordsThree = testCoords.get(2);

        final List<Hex> testHexes = setupHexes(testCoords);
        final Hex mockHexTwo = testHexes.get(1);
        final Hex mockHexThree = testHexes.get(2);
        final Hex mockFinalHex = testHexes.get(3);

        final Vector<MoveStep> stepVector = setupMoveStepVector(testCoords);
        final MoveStep mockFinalStep = stepVector.lastElement();

        final MovePath mockPath = setupPath(stepVector);

        final Entity mockUnit = mock(BipedMek.class);
        when(mockUnit.locations()).thenReturn(8);
        when(mockUnit.getArmor(anyInt())).thenReturn(10);

        final Game mockGame = setupGame(testCoords, testHexes);

        final Crew mockCrew = mock(Crew.class);
        when(mockUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);

        final IBuilding mockBuilding = mock(IBuilding.class);
        when(mockGame.getBoard().getBuildingAt(eq(testCoordsThree))).thenReturn(mockBuilding);
        when(mockBuilding.getCurrentCF(eq(testCoordsThree))).thenReturn(77);

        // Test walking fire-resistant BA through a burning building.
        final BattleArmor mockBA = mock(BattleArmor.class);
        when(mockBA.locations()).thenReturn(5);
        when(mockBA.getArmor(anyInt())).thenReturn(5);
        when(mockBA.getCrew()).thenReturn(mockCrew);
        when(mockBA.getHeatCapacity()).thenReturn(999);
        when(mockBA.isFireResistant()).thenReturn(true);
        when(mockBA.isBattleArmor()).thenReturn(true);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[] { Terrains.BUILDING, Terrains.FIRE });
        assertEquals(0, testRanker.checkPathForHazards(mockPath, mockBA, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking a ProtoMek over magma crust
        final Entity mockProto = mock(ProtoMek.class);
        when(mockProto.locations()).thenReturn(6);
        when(mockProto.getArmor(anyInt())).thenReturn(5);
        when(mockProto.getCrew()).thenReturn(mockCrew);
        when(mockProto.isProtoMek()).thenReturn(true);
        when(mockProto.getHeatCapacity()).thenReturn(999);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.MAGMA)));
        when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        assertEquals(167.0, testRanker.checkPathForHazards(mockPath, mockProto, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test waking a ProtoMek through a fire.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.WOODS, Terrains.FIRE)));
        assertEquals(50.0, testRanker.checkPathForHazards(mockPath, mockProto, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking infantry over ice.
        final Entity mockInfantry = mock(Infantry.class);
        when(mockInfantry.locations()).thenReturn(2);
        when(mockInfantry.getArmor(anyInt())).thenReturn(0);
        when(mockInfantry.getCrew()).thenReturn(mockCrew);
        when(mockInfantry.isConventionalInfantry()).thenReturn(true);
        when(mockInfantry.isInfantry()).thenReturn(true);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ICE, Terrains.WATER)));
        when(mockHexThree.depth()).thenReturn(1);
        assertEquals(1000, testRanker.checkPathForHazards(mockPath, mockInfantry, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);
        when(mockHexThree.depth()).thenReturn(0);

        // Test-driving a tank through a burning building.
        final Entity mockTank = mock(Tank.class);
        when(mockTank.locations()).thenReturn(5);
        when(mockTank.getArmor(anyInt())).thenReturn(10);
        when(mockTank.getCrew()).thenReturn(mockCrew);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.BUILDING, Terrains.FIRE)));
        assertEquals(26.2859, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking through a building.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.BUILDING)));
        assertEquals(1.285, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[0]);

        // Test walking over 3 hexes of ice.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexTwo.getTerrainTypes()).thenReturn(new int[] { Terrains.ICE, Terrains.WATER });
        when(mockHexThree.getTerrainTypes()).thenReturn(new int[] { Terrains.ICE, Terrains.WATER });
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[] { Terrains.ICE, Terrains.WATER });
        when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ICE, Terrains.WATER)));
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ICE, Terrains.WATER)));
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ICE, Terrains.WATER)));

        when(mockHexTwo.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockHexThree.terrainLevel(Terrains.WATER)).thenReturn(1);
        when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
        when(mockHexTwo.depth()).thenReturn(0);
        when(mockHexThree.depth()).thenReturn(1);
        when(mockFinalHex.depth()).thenReturn(2);
        when(mockUnit.getArmor(Mek.LOC_CENTER_TORSO)).thenReturn(0);
        assertEquals(2000, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockUnit.getArmor(Mek.LOC_CENTER_TORSO)).thenReturn(10);
        when(mockUnit.getArmor(Mek.LOC_RIGHT_ARM)).thenReturn(0);
        assertEquals(2000, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockUnit.getArmor(Mek.LOC_RIGHT_ARM)).thenReturn(10);
        when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));
        when(mockHexTwo.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockHexThree.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockHexTwo.depth()).thenReturn(0);
        when(mockHexThree.depth()).thenReturn(0);
        when(mockFinalHex.depth()).thenReturn(0);

        // Test walking over 3 hexes of magma crust.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.MAGMA)));
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.MAGMA)));
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.MAGMA)));
        when(mockHexTwo.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        assertEquals(361.500, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));
        when(mockHexTwo.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        when(mockHexThree.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test the stupidity of going prone in lava.
        // Now that hazard is inversely related to remaining armor, this is a _BIG_
        // number
        when(mockPath.isJumping()).thenReturn(false);
        when(mockFinalStep.isProne()).thenReturn(true);
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.MAGMA)));
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        assertEquals(56010.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalStep.isProne()).thenReturn(false);
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);

        // Test walking through 2 hexes of fire.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.WOODS, Terrains.FIRE)));
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.WOODS, Terrains.FIRE)));
        assertEquals(4.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));
        when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(0)));

        // Test jumping.
        when(mockPath.isJumping()).thenReturn(true);

        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ICE, Terrains.WATER)));
        when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
        when(mockFinalHex.depth()).thenReturn(2);
        when(mockUnit.getArmor(eq(Mek.LOC_LEFT_LEG))).thenReturn(0);
        assertEquals(1000.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockUnit.getArmor(eq(Mek.LOC_LEFT_LEG))).thenReturn(10);
        when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(0);
        when(mockFinalHex.depth()).thenReturn(0);
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.MAGMA)));
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        assertEquals(3134.5, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        assertEquals(6264.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(0);
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.WOODS, Terrains.FIRE)));
        assertEquals(5.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.WOODS)));


        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Test a movement type that doesn't worry about ground terrain.
        when(mockPath.getLastStepMovementType()).thenReturn(EntityMovementType.MOVE_FLYING);
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
    }

    @Test
    void testMagmaHazard() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));

        final List<Coords> testCoords = setupCoords("10,7", "10,8", "10,9", "10,10");
        final Coords testCoordsThree = testCoords.get(2);

        final List<Hex> testHexes = setupHexes(testCoords);
        final Hex mockFinalHex = testHexes.get(3);

        final Vector<MoveStep> stepVector = setupMoveStepVector(testCoords);

        final MovePath mockPath = setupPath(stepVector);

        final Entity mockUnit = mock(BipedMek.class);
        when(mockUnit.locations()).thenReturn(8);
        when(mockUnit.getArmor(anyInt())).thenReturn(10);

        final Game mockGame = setupGame(testCoords, testHexes);

        final Crew mockCrew = mock(Crew.class);
        when(mockUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);

        final IBuilding mockBuilding = mock(IBuilding.class);
        when(mockGame.getBoard().getBuildingAt(eq(testCoordsThree))).thenReturn(mockBuilding);
        when(mockBuilding.getCurrentCF(eq(testCoordsThree))).thenReturn(77);

        // Test jumping onto Magma Crust.
        when(mockPath.isJumping()).thenReturn(true);
        when(mockUnit.getArmor(eq(Mek.LOC_LEFT_LEG))).thenReturn(24);
        when(mockUnit.getArmor(eq(Mek.LOC_RIGHT_LEG))).thenReturn(24);
        when(mockFinalHex.depth()).thenReturn(0);
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.MAGMA)));
        // Only 50% chance to break through Crust, but must make PSR to avoid getting
        // bogged down.
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        assertEquals(1333.5, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        // 100% chance to take damage when Magma is Liquid (aka Lava) and a PSR chance to
        // get stuck.
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        when(mockFinalHex.depth()).thenReturn(1);
        assertEquals(2661.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Test jumping with worse piloting score (hazard should increase quickly)
        when(mockPath.isJumping()).thenReturn(true);
        when(mockFinalHex.depth()).thenReturn(0);
        when(mockFinalHex.getTerrainTypes()).thenReturn(new int[] { Terrains.MAGMA });
        // Only 50% chance to break through Crust
        when(mockCrew.getPiloting()).thenReturn(6);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        assertEquals(2192.5, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        // 100% chance to take damage when Magma is Liquid (aka Lava)
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        when(mockFinalHex.depth()).thenReturn(1);
        assertEquals(4380.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        // Only 50% chance to break through Crust
        when(mockCrew.getPiloting()).thenReturn(7);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        when(mockFinalHex.depth()).thenReturn(0);
        assertEquals(4300.5, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        // 100% chance to take damage when Magma is Liquid (aka Lava)
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        when(mockFinalHex.depth()).thenReturn(1);
        assertEquals(8595.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Test damaged `mek walking hazard (should increase hazard as damage level
        // increases)
        when(mockCrew.getPiloting()).thenReturn(5);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockUnit.getArmor(eq(Mek.LOC_LEFT_LEG))).thenReturn(2);
        when(mockUnit.getArmor(eq(Mek.LOC_RIGHT_LEG))).thenReturn(2);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        when(mockFinalHex.depth()).thenReturn(0);
        // Moderate damage means moderate hazard
        when(mockUnit.getDamageLevel()).thenReturn(Entity.DMG_MODERATE);
        assertEquals(589.1665, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        when(mockFinalHex.depth()).thenReturn(1);
        assertEquals(3510.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        // Crippled should be a very high hazard
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        when(mockFinalHex.depth()).thenReturn(0);
        when(mockUnit.getDamageLevel()).thenReturn(Entity.DMG_CRIPPLED);
        assertEquals(589.1665, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        when(mockFinalHex.depth()).thenReturn(1);
        assertEquals(3510.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Check damaged Hover ending on Liquid Magma
        // Ramps up quickly with the damage state!
        final Entity mockTank = mock(Tank.class);
        when(mockTank.locations()).thenReturn(5);
        when(mockTank.getArmor(anyInt())).thenReturn(10);
        when(mockTank.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockTank.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
        when(mockTank.getHeatCapacity()).thenReturn(Entity.DOES_NOT_TRACK_HEAT);

        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(2);
        when(mockFinalHex.depth()).thenReturn(1);
        when(mockTank.getDamageLevel()).thenReturn(0);
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        when(mockTank.getDamageLevel()).thenReturn(1);
        assertEquals(250.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        when(mockTank.getDamageLevel()).thenReturn(2);
        assertEquals(500.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        // Not as severe over Crust
        when(mockFinalHex.terrainLevel(Terrains.MAGMA)).thenReturn(1);
        when(mockTank.getDamageLevel()).thenReturn(0);
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        when(mockTank.getDamageLevel()).thenReturn(1);
        assertEquals(42.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        when(mockTank.getDamageLevel()).thenReturn(2);
        assertEquals(83.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
    }

    /**
     * The tests from most the hazard-testing-related-tests could probably be broken down into individual tests - and
     * put here.
     */
    @Nested
    class hazardTests {
        BasicPathRanker testRanker;
        List<Coords> testCoords;
        Coords testCoordsThree;
        List<Hex> testHexes;
        Hex mockFinalHex;
        Vector<MoveStep> stepVector;
        MovePath mockPath;

        Game mockGame;

        Entity mockUnit;
        Crew mockCrew;

        IBuilding mockBuilding;

        @BeforeEach
        void init() {
            testRanker = spy(new BasicPathRanker(mockPrincess));

            testCoords = setupCoords("10,7", "10,8", "10,9", "10,10");
            testCoordsThree = testCoords.get(2);

            testHexes = setupHexes(testCoords);
            mockFinalHex = testHexes.get(3);

            stepVector = setupMoveStepVector(testCoords);

            mockPath = setupPath(stepVector);
            mockGame = setupGame(testCoords, testHexes);

            mockUnit = mock(BipedMek.class);

            mockCrew = mock(Crew.class);
            when(mockUnit.getCrew()).thenReturn(mockCrew);
            when(mockCrew.getPiloting()).thenReturn(5);

            mockBuilding = mock(IBuilding.class);

            when(mockGame.getBoard().getBuildingAt(eq(testCoordsThree))).thenReturn(mockBuilding);
            when(mockBuilding.getCurrentCF(eq(testCoordsThree))).thenReturn(77);
        }

        // START - Hazardous Liquid Pools
        @Test
        void testThreeHexShallowHazardousLiquid() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            final Hex mockHexTwo = testHexes.get(1);
            final Hex mockHexThree = testHexes.get(2);
            final Hex mockFinalHex = testHexes.get(3);

            // Test walking through 3 hexes of shallow hazardous liquid.
            when(mockHexTwo.depth()).thenReturn(1);
            when(mockHexThree.depth()).thenReturn(1);
            when(mockFinalHex.depth()).thenReturn(1);
            when(mockPath.isJumping()).thenReturn(false);
            when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockHexTwo.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockHexThree.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockHexTwo.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockHexThree.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockHexTwo.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockHexThree.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            assertEquals(450.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testThreeHexDeepHazardousLiquid() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            final Hex mockHexTwo = testHexes.get(1);
            final Hex mockHexThree = testHexes.get(2);
            final Hex mockFinalHex = testHexes.get(3);

            // Test walking through 3 hexes of deep hazardous liquid - this should be extremely dangerous for our test unit!.
            when(mockPath.isJumping()).thenReturn(false);
            when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockHexThree.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockHexTwo.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockHexThree.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockHexTwo.terrainLevel(Terrains.WATER)).thenReturn(2);
            when(mockHexThree.terrainLevel(Terrains.WATER)).thenReturn(2);
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
            when(mockHexTwo.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockHexThree.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            assertEquals(9000.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testProneHazardousLiquid() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            final Hex mockFinalHex = testHexes.get(3);

            final MoveStep mockFinalStep = stepVector.lastElement();

            // Test the stupidity of going prone in shallow hazardous liquid.
            // Now that hazard is inversely related to remaining armor, this is a _BIG_
            // number
            when(mockPath.isJumping()).thenReturn(false);
            when(mockFinalHex.depth()).thenReturn(1);
            when(mockFinalStep.isProne()).thenReturn(true);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            assertEquals(3000.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testHazardousLiquidHazard() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            // Test jumping into Hazardous Liquid.
            when(mockPath.isJumping()).thenReturn(true);
            when(mockUnit.getArmor(eq(Mek.LOC_LEFT_LEG))).thenReturn(24);
            when(mockUnit.getArmor(eq(Mek.LOC_RIGHT_LEG))).thenReturn(24);
            when(mockFinalHex.depth()).thenReturn(1);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            assertEquals(63.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testHazardousLiquidWalkingHazard() {
            // Test damaged `mek walking hazard (more dangerous in deeper liquid)
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockCrew.getPiloting()).thenReturn(5);
            when(mockPath.isJumping()).thenReturn(false);
            when(mockFinalHex.depth()).thenReturn(1);

            assertEquals(150.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testDeepHazardousLiquidWalkingHazard() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
            when(mockFinalHex.depth()).thenReturn(2);

            assertEquals(3000., testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testCrippledHazardousLiquidWalkingHazard() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);
            when(mockUnit.getArmor(eq(Mek.LOC_LEFT_LEG))).thenReturn(2);
            when(mockUnit.getArmor(eq(Mek.LOC_RIGHT_LEG))).thenReturn(2);

            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.depth()).thenReturn(1);

            when(mockUnit.getDamageLevel()).thenReturn(Entity.DMG_CRIPPLED);
            assertEquals(750.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testCrippledDeepHazardousLiquidWalkingHazard() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(2);

            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(2);
            when(mockFinalHex.depth()).thenReturn(2);

            assertEquals(3000.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }


        @Test
        void testHazardousLiquidUnsealedIndustrialMek() {
            // If this is an industrial Mek, this is twice as dangerous!
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);
            when(mockFinalHex.depth()).thenReturn(1);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);

            when(mockUnit.isIndustrialMek()).thenReturn(true);
            when(mockUnit.hasEnvironmentalSealing()).thenReturn(false);
            assertEquals(300.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }


        @Test
        void testHazardousLiquidSealedIndustrialMek() {
            //If it has Environmental Sealing, though, it should be normal
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);
            when(mockFinalHex.depth()).thenReturn(1);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);


            when(mockUnit.hasEnvironmentalSealing()).thenReturn(true);
            assertEquals(150.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
            when(mockUnit.isIndustrialMek()).thenReturn(false);
            when(mockUnit.hasEnvironmentalSealing()).thenReturn(false);
        }


        @Test
        void testHoverCraft() {
            when(mockFinalHex.depth()).thenReturn(1);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);

            // Check damaged Hover ending on Hazardous Liquid
            // Ramps up quickly with the damage state!
            final Entity mockTank = mock(Tank.class);
            when(mockTank.locations()).thenReturn(5);
            when(mockTank.getArmor(anyInt())).thenReturn(10);
            when(mockTank.getCrew()).thenReturn(mockCrew);
            when(mockCrew.getPiloting()).thenReturn(5);
            when(mockPath.isJumping()).thenReturn(false);
            when(mockTank.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
            when(mockTank.getHeatCapacity()).thenReturn(Entity.DOES_NOT_TRACK_HEAT);

            when(mockTank.getDamageLevel()).thenReturn(Entity.DMG_NONE);
            assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        }


        @Test
        void testLightDamageHoverCraft() {
            when(mockFinalHex.depth()).thenReturn(1);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);

            // Check damaged Hover ending on Hazardous Liquid
            // Ramps up quickly with the damage state!
            final Entity mockTank = mock(Tank.class);
            when(mockTank.locations()).thenReturn(5);
            when(mockTank.getArmor(anyInt())).thenReturn(10);
            when(mockTank.getCrew()).thenReturn(mockCrew);
            when(mockCrew.getPiloting()).thenReturn(5);
            when(mockPath.isJumping()).thenReturn(false);
            when(mockTank.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
            when(mockTank.getHeatCapacity()).thenReturn(Entity.DOES_NOT_TRACK_HEAT);

            when(mockTank.getDamageLevel()).thenReturn(Entity.DMG_LIGHT);
            assertEquals(250.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        }


        @Test
        void testHeavyDamageHoverCraft() {
            when(mockFinalHex.depth()).thenReturn(1);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.HAZARDOUS_LIQUID,
                  Terrains.WATER)));
            when(mockFinalHex.terrainLevel(Terrains.WATER)).thenReturn(1);
            when(mockFinalHex.containsTerrain(Terrains.WATER)).thenReturn(true);
            when(mockFinalHex.terrainLevel(Terrains.HAZARDOUS_LIQUID)).thenReturn(1);

            // Check damaged Hover ending on Hazardous Liquid
            // Ramps up quickly with the damage state!
            final Entity mockTank = mock(Tank.class);
            when(mockTank.locations()).thenReturn(5);
            when(mockTank.getArmor(anyInt())).thenReturn(10);
            when(mockTank.getCrew()).thenReturn(mockCrew);
            when(mockCrew.getPiloting()).thenReturn(5);
            when(mockPath.isJumping()).thenReturn(false);
            when(mockTank.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
            when(mockTank.getHeatCapacity()).thenReturn(Entity.DOES_NOT_TRACK_HEAT);

            when(mockTank.getDamageLevel()).thenReturn(Entity.DMG_MODERATE);
            assertEquals(500.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
        }
        // END - Hazardous Liquid Pools

        // START - Ultra Sublevel
        @Test
        void testWalkingThroughUltraSublevel() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            final Hex mockHexTwo = testHexes.get(1);

            // Test walking through hexes, one of which is an ultra sublevel
            when(mockPath.isJumping()).thenReturn(false);
            when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ULTRA_SUBLEVEL)));
            when(mockHexTwo.terrainLevel(Terrains.ULTRA_SUBLEVEL)).thenReturn(0);
            assertEquals(1000.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testWalkingEndingOnUltraSublevel() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            // Test walking through hexes, the last one is an ultra sublevel
            when(mockPath.isJumping()).thenReturn(false);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ULTRA_SUBLEVEL)));
            when(mockFinalHex.terrainLevel(Terrains.ULTRA_SUBLEVEL)).thenReturn(0);
            assertEquals(1000.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testJumpingOverUltraSublevel() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            final Hex mockHexTwo = testHexes.get(1);

            // Test jumping over hexes, one of which is an ultra sublevel
            when(mockPath.isJumping()).thenReturn(true);
            when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ULTRA_SUBLEVEL)));
            when(mockHexTwo.terrainLevel(Terrains.ULTRA_SUBLEVEL)).thenReturn(0);
            assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testJumpingEndingOnUltraSublevel() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            // Test jumping over hexes, the last one is an ultra sublevel
            when(mockPath.isJumping()).thenReturn(true);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ULTRA_SUBLEVEL)));
            when(mockFinalHex.terrainLevel(Terrains.ULTRA_SUBLEVEL)).thenReturn(0);
            assertEquals(1000.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testVTOLThroughUltraSublevel() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            when(mockUnit.getMovementMode()).thenReturn(EntityMovementMode.VTOL);
            when(mockPath.getLastStepMovementType()).thenReturn(EntityMovementType.MOVE_VTOL_RUN);

            final Hex mockHexTwo = testHexes.get(1);

            // Test flying over hexes, one of which is an ultra sublevel
            when(mockPath.isJumping()).thenReturn(false);
            when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ULTRA_SUBLEVEL)));
            when(mockHexTwo.terrainLevel(Terrains.ULTRA_SUBLEVEL)).thenReturn(0);
            assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testVTOLEndingOnUltraSublevel() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            when(mockUnit.getMovementMode()).thenReturn(EntityMovementMode.VTOL);
            when(mockUnit.getDamageLevel()).thenReturn(1);
            when(mockUnit.getElevation()).thenReturn(1);
            when(mockPath.getLastStepMovementType()).thenReturn(EntityMovementType.MOVE_VTOL_RUN);

            // Test flying over hexes, the last one is an ultra sublevel
            when(mockPath.isJumping()).thenReturn(false);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ULTRA_SUBLEVEL)));
            when(mockFinalHex.terrainLevel(Terrains.ULTRA_SUBLEVEL)).thenReturn(0);

            // TODO: Fix the entire BasicPathRanker so damaged VTOLs can properly consider the safety of ending a turn over a hazard
            //assertEquals(250.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
            assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testFlyingOverUltraSublevel() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            when(mockUnit.getMovementMode()).thenReturn(EntityMovementMode.AERODYNE);
            when(mockPath.getLastStepMovementType()).thenReturn(EntityMovementType.MOVE_FLYING);

            final Hex mockHexTwo = testHexes.get(1);

            // Test flying over hexes, one of which is an ultra sublevel
            when(mockPath.isJumping()).thenReturn(false);
            when(mockHexTwo.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ULTRA_SUBLEVEL)));
            when(mockHexTwo.terrainLevel(Terrains.ULTRA_SUBLEVEL)).thenReturn(0);
            assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        @Test
        void testFlyingEndingOnUltraSublevel() {
            when(mockUnit.locations()).thenReturn(8);
            when(mockUnit.getArmor(anyInt())).thenReturn(10);

            when(mockUnit.getMovementMode()).thenReturn(EntityMovementMode.AERODYNE);
            when(mockPath.getLastStepMovementType()).thenReturn(EntityMovementType.MOVE_FLYING);

            // Test flying over hexes, the last one is an ultra sublevel
            when(mockPath.isJumping()).thenReturn(false);
            when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.ULTRA_SUBLEVEL)));
            when(mockFinalHex.terrainLevel(Terrains.ULTRA_SUBLEVEL)).thenReturn(0);

            assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        }

        // END - Ultra Sublevel
    }

    @Test
    void testSwampHazard() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));

        final List<Coords> testCoords = setupCoords("10,7", "10,8", "10,9", "10,10");
        final Coords testCoordsThree = testCoords.get(2);

        final List<Hex> testHexes = setupHexes(testCoords);
        final Hex mockFinalHex = testHexes.get(3);

        final Vector<MoveStep> stepVector = setupMoveStepVector(testCoords);

        final MovePath mockPath = setupPath(stepVector);

        final Entity mockUnit = mock(BipedMek.class);
        when(mockUnit.locations()).thenReturn(8);
        when(mockUnit.getArmor(anyInt())).thenReturn(10);
        when(mockUnit.getHeight()).thenReturn(2);

        final Game mockGame = setupGame(testCoords, testHexes);

        final Crew mockCrew = mock(Crew.class);
        when(mockUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);

        final IBuilding mockBuilding = mock(IBuilding.class);
        when(mockGame.getBoard().getBuildingAt(eq(testCoordsThree))).thenReturn(mockBuilding);
        when(mockBuilding.getCurrentCF(eq(testCoordsThree))).thenReturn(77);

        // Test jumping onto Swamp, Swamp-turned-Quicksand, and Quicksand.
        // Hazard for Quicksand is _very_ high due to PSR mod of +3 and height+1 turns
        // to total destruction.
        when(mockPath.isJumping()).thenReturn(true);
        when(mockFinalHex.depth()).thenReturn(0);
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.SWAMP)));
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(1);
        assertEquals(35.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(2);
        assertEquals(2094.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(3);
        assertEquals(2094.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Test walking into Swamp, Swamp-turned-Quicksand, and Quicksand.
        // Hazard is lower due to a better chance to escape getting bogged down initially,
        // but still high.
        when(mockPath.isJumping()).thenReturn(false);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(1);
        assertEquals(28.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(2);
        assertEquals(1955.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(3);
        assertEquals(1955.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Test non-hover vehicle hazard
        // It takes one less round to destroy a 1-height tank _and_ the initial PSR is harder!
        final Entity mockTank = mock(Tank.class);
        when(mockTank.locations()).thenReturn(5);
        when(mockTank.getArmor(anyInt())).thenReturn(10);
        when(mockTank.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockTank.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
        when(mockUnit.getHeight()).thenReturn(1);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(1);
        assertEquals(112.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(2);
        assertEquals(5865.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(3);
        assertEquals(5865.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        // Confirm hovers are immune
        when(mockTank.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(1);
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(2);
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
        when(mockFinalHex.terrainLevel(Terrains.SWAMP)).thenReturn(3);
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
    }

    @Test
    void testMudHazard() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));

        final List<Coords> testCoords = setupCoords("10,7", "10,8", "10,9", "10,10");
        final Coords testCoordsThree = testCoords.get(2);

        final List<Hex> testHexes = setupHexes(testCoords);
        final Hex mockFinalHex = testHexes.get(3);

        final Vector<MoveStep> stepVector = setupMoveStepVector(testCoords);

        final MovePath mockPath = setupPath(stepVector);

        final Entity mockUnit = mock(BipedMek.class);
        when(mockUnit.locations()).thenReturn(8);
        when(mockUnit.getArmor(anyInt())).thenReturn(10);
        when(mockUnit.getHeight()).thenReturn(2);
        when(mockUnit.isMek()).thenReturn(true);

        final Game mockGame = setupGame(testCoords, testHexes);

        final Crew mockCrew = mock(Crew.class);
        when(mockUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);

        final IBuilding mockBuilding = mock(IBuilding.class);
        when(mockGame.getBoard().getBuildingAt(eq(testCoordsThree))).thenReturn(mockBuilding);

        when(mockBuilding.getCurrentCF(eq(testCoordsThree))).thenReturn(77);

        // Test walking onto mud; jumping doesn't change danger because Meks can't bog
        // down here. Small hazard to Meks due to PSR malus
        when(mockPath.isJumping()).thenReturn(false);
        when(mockFinalHex.depth()).thenReturn(0);
        when(mockFinalHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.MUD)));
        assertEquals(2.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);

        // Test non-hover vehicle hazard
        // PSR malus and a chance to bog down makes this slightly hazardous for vehicles
        final Entity mockTank = mock(Tank.class);
        when(mockTank.locations()).thenReturn(5);
        when(mockTank.getArmor(anyInt())).thenReturn(10);
        when(mockTank.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);
        when(mockPath.isJumping()).thenReturn(false);
        when(mockTank.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
        when(mockUnit.getHeight()).thenReturn(1);
        when(mockFinalHex.terrainLevel(Terrains.MUD)).thenReturn(1);
        assertEquals(25.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);

        // Confirm hovers are immune
        when(mockTank.getMovementMode()).thenReturn(EntityMovementMode.HOVER);
        when(mockFinalHex.terrainLevel(Terrains.MUD)).thenReturn(1);
        assertEquals(0.0, testRanker.checkPathForHazards(mockPath, mockTank, mockGame), TOLERANCE);
    }

    @Test
    void testBlackIceHazard() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        testRanker.blackIce = 1;

        final List<Coords> testCoords = setupCoords("10,7", "10,8", "10,9", "10,10");

        final List<Hex> testHexes = setupHexes(testCoords);
        final Hex mockPenultimateHex = testHexes.get(2);

        final Vector<MoveStep> stepVector = setupMoveStepVector(testCoords);

        final MovePath mockPath = setupPath(stepVector);

        final Entity mockUnit = mock(BipedMek.class);
        when(mockUnit.getWeight()).thenReturn(70.0);
        when(mockUnit.locations()).thenReturn(8);
        when(mockUnit.getArmor(anyInt())).thenReturn(10);
        when(mockUnit.getHeight()).thenReturn(2);
        when(mockPath.isJumping()).thenReturn(false);

        final Game mockGame = setupGame(testCoords, testHexes);

        final Crew mockCrew = mock(Crew.class);
        when(mockUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);

        // Test visible black ice hazard value
        when(mockPenultimateHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.BLACK_ICE)));
        assertEquals(12.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
    }

    @Test
    void testPossibleBlackIceHazard() {
        final BasicPathRanker testRanker = spy(new BasicPathRanker(mockPrincess));
        testRanker.blackIce = 1;

        final List<Coords> testCoords = setupCoords("10,7", "10,8", "10,9", "10,10");

        final List<Hex> testHexes = setupHexes(testCoords);
        final Hex mockPenultimateHex = testHexes.get(2);

        final Vector<MoveStep> stepVector = setupMoveStepVector(testCoords);

        final MovePath mockPath = setupPath(stepVector);

        final Entity mockUnit = mock(BipedMek.class);
        when(mockUnit.getWeight()).thenReturn(70.0);
        when(mockUnit.locations()).thenReturn(8);
        when(mockUnit.getArmor(anyInt())).thenReturn(10);
        when(mockUnit.getHeight()).thenReturn(2);
        when(mockPath.isJumping()).thenReturn(false);

        final Game mockGame = setupGame(testCoords, testHexes);

        final Crew mockCrew = mock(Crew.class);
        when(mockUnit.getCrew()).thenReturn(mockCrew);
        when(mockCrew.getPiloting()).thenReturn(5);
        // hex.getTerrainTypesSet()
        // Test _possible_ black ice hazard value (1/3 lower)
        when(mockPenultimateHex.getTerrainTypesSet()).thenReturn(new HashSet<>(Set.of(Terrains.PAVEMENT)));
        assertEquals(4.0, testRanker.checkPathForHazards(mockPath, mockUnit, mockGame), TOLERANCE);
    }

}
