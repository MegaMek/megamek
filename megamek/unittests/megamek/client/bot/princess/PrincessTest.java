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

import megamek.client.bot.princess.PathRanker.PathRankerType;
import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/22/13 8:33 AM
 */
public class PrincessTest {

    private Princess mockPrincess;
    private BasicPathRanker mockPathRanker;

    @BeforeAll
    public static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    public void beforeEach() {
        mockPathRanker = mock(BasicPathRanker.class);

        MoraleUtil mockMoralUtil = mock(MoraleUtil.class);

        mockPrincess = mock(Princess.class);
        when(mockPrincess.getPathRanker(PathRankerType.Basic)).thenReturn(mockPathRanker);
        when(mockPrincess.getPathRanker(any(Entity.class))).thenReturn(mockPathRanker);
        when(mockPrincess.getMoraleUtil()).thenReturn(mockMoralUtil);
    }

    @Test
    public void testCalculateAdjustment() {
        when(mockPrincess.calculateAdjustment(anyString())).thenCallRealMethod();

        // Test a +3 adjustment.
        assertEquals(3, mockPrincess.calculateAdjustment("+++"));

        // Test a -2 adjustment.
        assertEquals(-2, mockPrincess.calculateAdjustment("--"));

        // Test an adjustment with some bad characters.
        assertEquals(1, mockPrincess.calculateAdjustment("+4"));

        // Test an adjustment with nothing but bad characters.
        assertEquals(0, mockPrincess.calculateAdjustment("5"));

        // Test an empty ticks argument.
        assertEquals(0, mockPrincess.calculateAdjustment(""));

        // Test a null ticks argument.
        assertEquals(0, mockPrincess.calculateAdjustment(null));
    }

    @Test
    public void testCalculateMoveIndex() {
        final double TOLERANCE = 0.001;
        when(mockPrincess.calculateMoveIndex(any(Entity.class), any(StringBuilder.class)))
               .thenCallRealMethod();
        when(mockPrincess.isFallingBack(any(Entity.class))).thenReturn(false);

        when(mockPathRanker.distanceToClosestEnemy(any(Entity.class), nullable(Coords.class),
                nullable(Game.class))).thenReturn(10.0);

        // Test a 6/9/6 regular mech.
        Entity mockMech = mock(BipedMech.class);
        when(mockMech.getRunMP(MPCalculationSetting.STANDARD)).thenReturn(9);
        when(mockMech.getJumpMP(MPCalculationSetting.STANDARD)).thenReturn(6);
        when(mockMech.isProne()).thenReturn(false);
        when(mockMech.isCommander()).thenReturn(false);
        when(mockMech.isMilitary()).thenReturn(true);
        when(mockMech.isStealthActive()).thenReturn(false);
        when(mockMech.isStealthOn()).thenReturn(false);
        when(mockMech.isVoidSigActive()).thenReturn(false);
        when(mockMech.isVoidSigOn()).thenReturn(false);
        double actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(1.111, actual, TOLERANCE);

        // Make the mech prone.
        when(mockMech.isProne()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(1.222, actual, TOLERANCE);

        // Make the mech flee.
        when(mockMech.isProne()).thenReturn(false);
        when(mockPrincess.isFallingBack(eq(mockMech))).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(2.222, actual, TOLERANCE);

        // Make the mech a commander.
        when(mockPrincess.isFallingBack(eq(mockMech))).thenReturn(false);
        when(mockMech.isCommander()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(0.555, actual, TOLERANCE);

        // Make it a civilian mech.
        when(mockMech.isCommander()).thenReturn(false);
        when(mockMech.isMilitary()).thenReturn(false);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(5.555, actual, TOLERANCE);

        // Make it stealthy;
        when(mockMech.isMilitary()).thenReturn(true);
        when(mockMech.isStealthActive()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(0.37, actual, TOLERANCE);
        when(mockMech.isStealthActive()).thenReturn(false);
        when(mockMech.isStealthOn()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(0.37, actual, TOLERANCE);
        when(mockMech.isStealthOn()).thenReturn(false);
        when(mockMech.isVoidSigActive()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(0.37, actual, TOLERANCE);
        when(mockMech.isVoidSigActive()).thenReturn(false);
        when(mockMech.isVoidSigOn()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        assertEquals(0.37, actual, TOLERANCE);

        // Test a BA unit.
        Entity mockBA = mock(BattleArmor.class);
        when(mockBA.getRunMP()).thenReturn(1);
        when(mockBA.getJumpMP()).thenReturn(3);
        when(mockBA.isProne()).thenReturn(false);
        when(mockBA.isCommander()).thenReturn(false);
        when(mockBA.isMilitary()).thenReturn(true);
        when(mockBA.isStealthActive()).thenReturn(false);
        when(mockBA.isStealthOn()).thenReturn(false);
        when(mockBA.isVoidSigActive()).thenReturn(false);
        when(mockBA.isVoidSigOn()).thenReturn(false);
        actual = mockPrincess.calculateMoveIndex(mockBA, new StringBuilder());
        assertEquals(6.666, actual, TOLERANCE);

        // Test an Inf unit.
        Entity mockInf = mock(Infantry.class);
        when(mockInf.getRunMP(MPCalculationSetting.STANDARD)).thenReturn(1);
        when(mockInf.getJumpMP(MPCalculationSetting.STANDARD)).thenReturn(0);
        when(mockInf.isProne()).thenReturn(false);
        when(mockInf.isCommander()).thenReturn(false);
        when(mockInf.isMilitary()).thenReturn(true);
        when(mockInf.isStealthActive()).thenReturn(false);
        when(mockInf.isStealthOn()).thenReturn(false);
        when(mockInf.isVoidSigActive()).thenReturn(false);
        when(mockInf.isVoidSigOn()).thenReturn(false);
        actual = mockPrincess.calculateMoveIndex(mockInf, new StringBuilder());
        assertEquals(30, actual, TOLERANCE);

        // Test a Tank.
        Entity mockTank = mock(Tank.class);
        when(mockTank.getRunMP(MPCalculationSetting.STANDARD)).thenReturn(6);
        when(mockTank.getJumpMP(MPCalculationSetting.STANDARD)).thenReturn(0);
        when(mockTank.isProne()).thenReturn(false);
        when(mockTank.isCommander()).thenReturn(false);
        when(mockTank.isMilitary()).thenReturn(true);
        when(mockTank.isStealthActive()).thenReturn(false);
        when(mockTank.isStealthOn()).thenReturn(false);
        when(mockTank.isVoidSigActive()).thenReturn(false);
        when(mockTank.isVoidSigOn()).thenReturn(false);
        actual = mockPrincess.calculateMoveIndex(mockTank, new StringBuilder());
        assertEquals(2.5, actual, TOLERANCE);
    }

    @Test
    public void testGetEntityToMove() {
        when(mockPrincess.getEntityToMove()).thenCallRealMethod();
        when(mockPrincess.isImmobilized(any(Entity.class))).thenCallRealMethod();

        Coords mockCoords = mock(Coords.class);

        Entity mockMech = mock(BipedMech.class);
        when(mockMech.getRunMP()).thenReturn(6);
        when(mockMech.isOffBoard()).thenReturn(false);
        when(mockMech.getPosition()).thenReturn(mockCoords);
        when(mockMech.isSelectableThisTurn()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(eq(mockMech), any(StringBuilder.class))).thenReturn(1.111);

        Entity mockBA = mock(BattleArmor.class);
        when(mockBA.getRunMP()).thenReturn(3);
        when(mockBA.isOffBoard()).thenReturn(false);
        when(mockBA.getPosition()).thenReturn(mockCoords);
        when(mockBA.isSelectableThisTurn()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(eq(mockBA), any(StringBuilder.class))).thenReturn(6.666);

        Entity mockTank = mock(Tank.class);
        when(mockTank.getRunMP()).thenReturn(6);
        when(mockTank.isOffBoard()).thenReturn(false);
        when(mockTank.getPosition()).thenReturn(mockCoords);
        when(mockTank.isSelectableThisTurn()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(eq(mockTank), any(StringBuilder.class))).thenReturn(2.5);

        Entity mockEjectedMechwarrior = mock(MechWarrior.class);
        when(mockEjectedMechwarrior.getRunMP()).thenReturn(1);
        when(mockEjectedMechwarrior.isOffBoard()).thenReturn(false);
        when(mockEjectedMechwarrior.getPosition()).thenReturn(mockCoords);
        when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(true);

        Entity mockImmobileMech = mock(BipedMech.class);
        when(mockImmobileMech.getRunMP()).thenReturn(0);
        when(mockImmobileMech.isOffBoard()).thenReturn(false);
        when(mockImmobileMech.getPosition()).thenReturn(mockCoords);
        when(mockImmobileMech.isSelectableThisTurn()).thenReturn(true);
        when(mockImmobileMech.isImmobile()).thenReturn(true);

        Entity mockOffBoardArty = mock(Tank.class);
        when(mockOffBoardArty.getRunMP()).thenReturn(6);
        when(mockOffBoardArty.getPosition()).thenReturn(mockCoords);
        when(mockOffBoardArty.isSelectableThisTurn()).thenReturn(true);
        when(mockOffBoardArty.isOffBoard()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(eq(mockOffBoardArty), any(StringBuilder.class))).thenReturn(10.0);

        // Test a list of normal units.
        Game mockGame = mock(Game.class);
        GameOptions mockOptions = mock(GameOptions.class);
        when(mockGame.getOptions()).thenReturn(mockOptions);
        when(mockOptions.booleanOption(OptionsConstants.INIT_SIMULTANEOUS_MOVEMENT)).thenReturn(false);
        when(mockGame.getPhase()).thenReturn(GamePhase.MOVEMENT);
        GameTurn mockTurn = mock(GameTurn.class);
        when(mockGame.getTurn()).thenReturn(mockTurn);
        when(mockTurn.isValidEntity(any(Entity.class), any(Game.class))).thenCallRealMethod();
        when(mockTurn.isValidEntity(any(Entity.class), any(Game.class), anyBoolean())).thenCallRealMethod();
        when(mockPrincess.getGame()).thenReturn(mockGame);

        List<Entity> testEntityList = new ArrayList<>();
        testEntityList.add(mockMech);
        testEntityList.add(mockBA);
        testEntityList.add(mockTank);
        when(mockPrincess.getEntitiesOwned()).thenReturn(testEntityList);
        Entity pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockBA, pickedEntity);

        // Add the off-board artillery, which should be ignored. Otherwise it would be picked as the next to move.
        testEntityList.add(mockOffBoardArty);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockBA, pickedEntity);

        // Mark the battle armor as having already been moved.
        when(mockBA.isSelectableThisTurn()).thenReturn(false);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockTank, pickedEntity);

        // Add the immobilized mech, which should be picked as the next to move.
        testEntityList.add(mockImmobileMech);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockImmobileMech, pickedEntity);

        // Replace the immobilized mech with the ejected mechwarrior, which should now be the next to move.
        testEntityList.remove(mockImmobileMech);
        testEntityList.add(mockEjectedMechwarrior);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockEjectedMechwarrior, pickedEntity);

        // Test a list that contains a unit with a move index of 0.
        when(mockBA.isSelectableThisTurn()).thenReturn(false);
        when(mockTank.isSelectableThisTurn()).thenReturn(false);
        when(mockImmobileMech.isSelectableThisTurn()).thenReturn(false);
        when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(false);
        when(mockPrincess.calculateMoveIndex(mockMech, new StringBuilder())).thenReturn(0.0);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockMech, pickedEntity);
        when(mockBA.isSelectableThisTurn()).thenReturn(true);
        when(mockTank.isSelectableThisTurn()).thenReturn(true);
        when(mockImmobileMech.isSelectableThisTurn()).thenReturn(true);
        when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(mockMech, new StringBuilder())).thenReturn(1.111);

        // Test a list where everyone has moved except one unit with the lowest possible move index.
        when(mockBA.isSelectableThisTurn()).thenReturn(false);
        when(mockTank.isSelectableThisTurn()).thenReturn(false);
        when(mockImmobileMech.isSelectableThisTurn()).thenReturn(false);
        when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(false);
        when(mockPrincess.calculateMoveIndex(mockMech, new StringBuilder())).thenReturn(Double.MIN_VALUE);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockMech, pickedEntity);
        when(mockBA.isSelectableThisTurn()).thenReturn(true);
        when(mockTank.isSelectableThisTurn()).thenReturn(true);
        when(mockImmobileMech.isSelectableThisTurn()).thenReturn(true);
        when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(mockMech, new StringBuilder())).thenReturn(1.111);
    }

    @Test
    public void testWantsToFallBack() {
        Entity mockMech = mock(BipedMech.class);
        when(mockMech.isCrippled()).thenReturn(false);

        when(mockPrincess.wantsToFallBack(any(Entity.class))).thenCallRealMethod();
        when(mockPrincess.getForcedWithdrawal()).thenReturn(true);
        when(mockPrincess.getFallBack()).thenReturn(false);
        when(mockPrincess.getFleeBoard()).thenReturn(false);
        // Forced Withdrawal Enabled, Mech Undamaged, Fall Back disabled, Flee Board disabled
        // Should Not Fall Back
        assertFalse(mockPrincess.wantsToFallBack(mockMech));

        when(mockPrincess.getFallBack()).thenReturn(true);
        // Fall Back Enabled
        // Should Fall Back
        assertTrue(mockPrincess.wantsToFallBack(mockMech));

        when(mockPrincess.getFallBack()).thenReturn(false);
        when(mockPrincess.getFleeBoard()).thenReturn(true);
        // Fall Back Disabled, Flee Board Enabled (Should Never Happen)
        // Should Not Fall Back
        assertFalse(mockPrincess.wantsToFallBack(mockMech));

        when(mockPrincess.getFleeBoard()).thenReturn(false);
        when(mockMech.isCrippled()).thenReturn(true);
        // Fall Back and Flee Board Disabled, Mech Crippled, Forced Withdrawal Enabled
        // Should Fall Back
        assertTrue(mockPrincess.wantsToFallBack(mockMech));

        when(mockPrincess.getForcedWithdrawal()).thenReturn(false);
        // Fall Back and Flee Board Disabled, Mech Crippled, Forced Withdrawal Disabled
        // Should Not Fall Back
        assertFalse(mockPrincess.wantsToFallBack(mockMech));
    }

    @Test
    public void testIsFallingBack() {
        Entity mockMech = mock(BipedMech.class);
        when(mockMech.isImmobile()).thenReturn(false);
        when(mockMech.isCrippled(anyBoolean())).thenReturn(false);
        when(mockMech.getId()).thenReturn(1);

        when(mockPrincess.wantsToFallBack(any(Entity.class))).thenReturn(false);
        when(mockPrincess.isFallingBack(any(Entity.class))).thenCallRealMethod();
       
        BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getDestinationEdge()).thenReturn(CardinalEdge.NONE);
        when(mockBehavior.isForcedWithdrawal()).thenReturn(true);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);
        
        // A normal undamaged mech.
        assertFalse(mockPrincess.isFallingBack(mockMech));

        // A mobile mech that wants to fall back (for any reason).
        when(mockMech.isCrippled(anyBoolean())).thenReturn(true);
        assertTrue(mockPrincess.isFallingBack(mockMech));
        
        // A mech whose bot is set for a destination edge
        when(mockBehavior.getDestinationEdge()).thenReturn(CardinalEdge.NEAREST);
        assertTrue(mockPrincess.isFallingBack(mockMech));
    }

    @Test
    public void testMustFleeBoard() {
        when(mockPrincess.mustFleeBoard(any(Entity.class))).thenCallRealMethod();

        // Unit is not yet falling back
        when(mockPrincess.isFallingBack(any(Entity.class))).thenReturn(false);

        // Unit is capable of fleeing.
        Entity mockMech = mock(BipedMech.class);
        when(mockMech.canFlee()).thenReturn(true);

        // Unit is on home edge.
        BasicPathRanker mockRanker = mock(BasicPathRanker.class);
        when(mockRanker.distanceToHomeEdge(any(Coords.class), any(CardinalEdge.class),
                any(Game.class))).thenReturn(0);
        when(mockPrincess.getPathRanker(any(Entity.class))).thenReturn(mockRanker);

        // Mock objects so we don't have nulls.
        Coords mockCoords = mock(Coords.class);
        when(mockMech.getPosition()).thenReturn(mockCoords);
        when(mockPrincess.getHomeEdge(any(Entity.class))).thenReturn(CardinalEdge.NORTH);
        Game mockGame = mock(Game.class);
        when(mockPrincess.getGame()).thenReturn(mockGame);

        // In its current state, the entity does not need to flee the board.
        assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // Now the unit is falling back, but it should not flee the board unless fleeBoard is enabled
        // or the unit is crippled and forcedWithdrawal is enabled
        when(mockPrincess.isFallingBack(any(Entity.class))).thenReturn(true);
        assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // Even a crippled mech should not fall back unless fleeBoard or forcedWithdrawal is enabled
        when(mockMech.isCrippled()).thenReturn(true);
        assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // Enabling forcedWithdrawal should cause fleeing, because mech is crippled
        when(mockPrincess.getForcedWithdrawal()).thenReturn(true);
        assertTrue(mockPrincess.mustFleeBoard(mockMech));

        // But forcedWithdrawal without a crippled mech should not flee
        when(mockMech.isCrippled()).thenReturn(false);
        assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // If fleeBoard is true, all units falling back should flee
        when(mockPrincess.getFleeBoard()).thenReturn(true);
        assertTrue(mockPrincess.mustFleeBoard(mockMech));

        // Make the unit incapable of fleeing.
        when(mockMech.canFlee()).thenReturn(false);
        assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // The unit can flee, but is no longer on the board edge.
        when(mockMech.canFlee()).thenReturn(true);
        when(mockRanker.distanceToHomeEdge(any(Coords.class), any(CardinalEdge.class),
                any(Game.class))).thenReturn(1);
        assertFalse(mockPrincess.mustFleeBoard(mockMech));
    }

    @Test
    public void testIsImmobilized() {
        when(mockPrincess.isImmobilized(any(Entity.class))).thenCallRealMethod();
        when(mockPrincess.getBooleanOption(eq("tacops_careful_stand"))).thenReturn(false);

        Hex mockHex = mock(Hex.class);
        when(mockHex.getLevel()).thenReturn(0);
        when(mockPrincess.getHex(any(Coords.class))).thenReturn(mockHex);

        Game mockGame = mock(Game.class);
        doReturn(mockGame).when(mockPrincess).getGame();

        BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getFallShameIndex()).thenReturn(5);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        PilotingRollData mockPilotingRollData = mock(PilotingRollData.class);
        when(mockPilotingRollData.getValue()).thenReturn(7);

        Coords mockPosition = mock(Coords.class);

        Coords mockPriorPosition = mock(Coords.class);

        // Test a fully mobile mech.
        Mech mockMech = mock(BipedMech.class);
        when(mockMech.getRunMP()).thenReturn(6);
        when(mockMech.isImmobile()).thenReturn(false);
        when(mockMech.isShutDown()).thenReturn(false);
        when(mockMech.isProne()).thenReturn(false);
        when(mockMech.isStuck()).thenReturn(false);
        when(mockMech.isStalled()).thenReturn(false);
        when(mockMech.cannotStandUpFromHullDown()).thenReturn(false);
        when(mockMech.checkGetUp(any(MoveStep.class), any(EntityMovementType.class))).thenReturn(mockPilotingRollData);
        when(mockMech.getPosition()).thenReturn(mockPosition);
        when(mockMech.getPriorPosition()).thenReturn(mockPriorPosition);
        when(mockMech.checkBogDown(any(MoveStep.class), any(EntityMovementType.class), eq(mockHex),
                eq(mockPriorPosition), eq(mockPosition), anyInt(), anyBoolean()))
               .thenReturn(mockPilotingRollData);
        assertFalse(mockPrincess.isImmobilized(mockMech));

        // Test a shutdown mech.
        when(mockMech.isImmobile()).thenReturn(true);
        when(mockMech.isShutDown()).thenReturn(true);
        assertFalse(mockPrincess.isImmobilized(mockMech));

        // Test an immobile mech that is not shut down.
        when(mockMech.isImmobile()).thenReturn(true);
        when(mockMech.isShutDown()).thenReturn(false);
        assertTrue(mockPrincess.isImmobilized(mockMech));

        // Test a mech with move 0.
        when(mockMech.isImmobile()).thenReturn(false);
        when(mockMech.getRunMP()).thenReturn(0);
        assertTrue(mockPrincess.isImmobilized(mockMech));
        when(mockMech.getRunMP()).thenReturn(6);

        // Test a tank that is not immobile.
        Tank mockTank = mock(Tank.class);
        when(mockTank.getRunMP()).thenReturn(6);
        when(mockTank.isImmobile()).thenReturn(false);
        when(mockTank.isShutDown()).thenReturn(false);
        assertFalse(mockPrincess.isImmobilized(mockTank));

        // Test a prone mech that cannot stand up.
        when(mockMech.isImmobile()).thenReturn(false);
        when(mockMech.isShutDown()).thenReturn(false);
        when(mockMech.isProne()).thenReturn(true);
        when(mockMech.cannotStandUpFromHullDown()).thenReturn(true);
        assertTrue(mockPrincess.isImmobilized(mockMech));

        // Test a prone mech whose chance to stand up is better than our fall tolerance threshold.
        when(mockMech.isImmobile()).thenReturn(false);
        when(mockMech.isShutDown()).thenReturn(false);
        when(mockMech.isProne()).thenReturn(true);
        when(mockMech.cannotStandUpFromHullDown()).thenReturn(false);
        assertFalse(mockPrincess.isImmobilized(mockMech));

        // Test a prone mech whose chance to stand up is worse than our fall tolerance threshold.
        when(mockPilotingRollData.getValue()).thenReturn(12);
        when(mockMech.isImmobile()).thenReturn(false);
        when(mockMech.isShutDown()).thenReturn(false);
        when(mockMech.isProne()).thenReturn(true);
        when(mockMech.cannotStandUpFromHullDown()).thenReturn(false);
        assertTrue(mockPrincess.isImmobilized(mockMech));

        // Test a stuck mech whose chance to get unstuck is better than our fall tolerance threshold.
        when(mockPilotingRollData.getValue()).thenReturn(7);
        when(mockMech.isImmobile()).thenReturn(false);
        when(mockMech.isShutDown()).thenReturn(false);
        when(mockMech.isProne()).thenReturn(false);
        when(mockMech.isStuck()).thenReturn(true);
        assertFalse(mockPrincess.isImmobilized(mockMech));

        // Test a stuck mech whose chance to get unstuck is worse than our fall tolerance threshold.
        when(mockPilotingRollData.getValue()).thenReturn(12);
        when(mockMech.isImmobile()).thenReturn(false);
        when(mockMech.isShutDown()).thenReturn(false);
        when(mockMech.isProne()).thenReturn(false);
        when(mockMech.isStuck()).thenReturn(true);
        assertTrue(mockPrincess.isImmobilized(mockMech));
    }
}
