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

import megamek.common.BattleArmor;
import megamek.common.BipedMech;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.GameTurn;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechWarrior;
import megamek.common.MoveStep;
import megamek.common.PilotingRollData;
import megamek.common.Tank;
import megamek.common.logging.LogLevel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version $Id$
 * @since 11/22/13 8:33 AM
 */
@RunWith(JUnit4.class)
public class PrincessTest {

    private Princess mockPrincess;
    private BasicPathRanker mockPathRanker;

    @Before
    public void setUp() {
        mockPathRanker = Mockito.mock(BasicPathRanker.class);

        MoralUtil mockMoralUtil = Mockito.mock(MoralUtil.class);

        mockPrincess = Mockito.mock(Princess.class);
        Mockito.doNothing().when(mockPrincess).log(Mockito.any(Class.class), Mockito.anyString(),
                                                   Mockito.any(LogLevel.class), Mockito.anyString());
        Mockito.when(mockPrincess.getPathRanker()).thenReturn(mockPathRanker);
        Mockito.when(mockPrincess.getMoralUtil()).thenReturn(mockMoralUtil);
        Mockito.when(mockPrincess.getMyFleeingEntities()).thenReturn(new HashSet<>(0));
    }

    @Test
    public void testCalculateAdjustment() {
        Mockito.when(mockPrincess.calculateAdjustment(Mockito.anyString())).thenCallRealMethod();

        // Test a +3 adjustment.
        String ticks = "+++";
        int expected = 3;
        int actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test a -2 adjustment.
        ticks = "--";
        expected = -2;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test an adjustment with some bad characters.
        ticks = "+4";
        expected = 1;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test an adjustment with nothing but bad characters.
        ticks = "5";
        expected = 0;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test an empty ticks argument.
        ticks = "";
        expected = 0;
        actual = mockPrincess.calculateAdjustment(ticks);
        Assert.assertEquals(expected, actual);

        // Test a null ticks argument.
        expected = 0;
        actual = mockPrincess.calculateAdjustment(null);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCalculateMoveIndex() {
        final double TOLERANCE = 0.001;
        Mockito.when(mockPrincess.calculateMoveIndex(Mockito.any(Entity.class), Mockito.any(StringBuilder.class)))
               .thenCallRealMethod();
        Mockito.when(mockPrincess.isFallingBack(Mockito.any(Entity.class))).thenReturn(false);

        Mockito.when(mockPathRanker
                             .distanceToClosestEnemy(Mockito.any(Entity.class), Mockito.any(Coords.class),
                                                     Mockito.any(IGame.class)))
               .thenReturn(10.0);

        // Test a 6/9/6 regular mech.
        Entity mockMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockMech.getRunMP(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean()))
               .thenReturn(9);
        Mockito.when(mockMech.getJumpMP(Mockito.anyBoolean())).thenReturn(6);
        Mockito.when(mockMech.isProne()).thenReturn(false);
        Mockito.when(mockMech.isCommander()).thenReturn(false);
        Mockito.when(mockMech.isMilitary()).thenReturn(true);
        Mockito.when(mockMech.isStealthActive()).thenReturn(false);
        Mockito.when(mockMech.isStealthOn()).thenReturn(false);
        Mockito.when(mockMech.isVoidSigActive()).thenReturn(false);
        Mockito.when(mockMech.isVoidSigOn()).thenReturn(false);
        double expected = 1.111;
        double actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Make the mech prone.
        Mockito.when(mockMech.isProne()).thenReturn(true);
        expected = 1.222;
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Make the mech flee.
        Mockito.when(mockMech.isProne()).thenReturn(false);
        Mockito.when(mockPrincess.isFallingBack(Mockito.eq(mockMech))).thenReturn(true);
        expected = 2.222;
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Make the mech a commander.
        Mockito.when(mockPrincess.isFallingBack(Mockito.eq(mockMech))).thenReturn(false);
        Mockito.when(mockMech.isCommander()).thenReturn(true);
        expected = 0.555;
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Make it a civillian mech.
        Mockito.when(mockMech.isCommander()).thenReturn(false);
        Mockito.when(mockMech.isMilitary()).thenReturn(false);
        expected = 5.555;
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Make it stealthy;
        Mockito.when(mockMech.isMilitary()).thenReturn(true);
        Mockito.when(mockMech.isStealthActive()).thenReturn(true);
        expected = 0.370;
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);
        Mockito.when(mockMech.isStealthActive()).thenReturn(false);
        Mockito.when(mockMech.isStealthOn()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);
        Mockito.when(mockMech.isStealthOn()).thenReturn(false);
        Mockito.when(mockMech.isVoidSigActive()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);
        Mockito.when(mockMech.isVoidSigActive()).thenReturn(false);
        Mockito.when(mockMech.isVoidSigOn()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMech, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Test a BA unit.
        Entity mockBA = Mockito.mock(BattleArmor.class);
        Mockito.when(mockBA.getRunMP(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean()))
               .thenReturn(1);
        Mockito.when(mockBA.getJumpMP(Mockito.anyBoolean())).thenReturn(3);
        Mockito.when(mockBA.isProne()).thenReturn(false);
        Mockito.when(mockBA.isCommander()).thenReturn(false);
        Mockito.when(mockBA.isMilitary()).thenReturn(true);
        Mockito.when(mockBA.isStealthActive()).thenReturn(false);
        Mockito.when(mockBA.isStealthOn()).thenReturn(false);
        Mockito.when(mockBA.isVoidSigActive()).thenReturn(false);
        Mockito.when(mockBA.isVoidSigOn()).thenReturn(false);
        expected = 6.666;
        actual = mockPrincess.calculateMoveIndex(mockBA, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Test an Inf unit.
        Entity mockInf = Mockito.mock(Infantry.class);
        Mockito.when(mockInf.getRunMP(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean()))
               .thenReturn(1);
        Mockito.when(mockInf.getJumpMP(Mockito.anyBoolean())).thenReturn(0);
        Mockito.when(mockInf.isProne()).thenReturn(false);
        Mockito.when(mockInf.isCommander()).thenReturn(false);
        Mockito.when(mockInf.isMilitary()).thenReturn(true);
        Mockito.when(mockInf.isStealthActive()).thenReturn(false);
        Mockito.when(mockInf.isStealthOn()).thenReturn(false);
        Mockito.when(mockInf.isVoidSigActive()).thenReturn(false);
        Mockito.when(mockInf.isVoidSigOn()).thenReturn(false);
        expected = 30.0;
        actual = mockPrincess.calculateMoveIndex(mockInf, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);

        // Test a Tank.
        Entity mockTank = Mockito.mock(Tank.class);
        Mockito.when(mockTank.getRunMP(Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean()))
               .thenReturn(6);
        Mockito.when(mockTank.getJumpMP(Mockito.anyBoolean())).thenReturn(0);
        Mockito.when(mockTank.isProne()).thenReturn(false);
        Mockito.when(mockTank.isCommander()).thenReturn(false);
        Mockito.when(mockTank.isMilitary()).thenReturn(true);
        Mockito.when(mockTank.isStealthActive()).thenReturn(false);
        Mockito.when(mockTank.isStealthOn()).thenReturn(false);
        Mockito.when(mockTank.isVoidSigActive()).thenReturn(false);
        Mockito.when(mockTank.isVoidSigOn()).thenReturn(false);
        expected = 2.5;
        actual = mockPrincess.calculateMoveIndex(mockTank, new StringBuilder());
        Assert.assertEquals(expected, actual, TOLERANCE);
    }

    @Test
    public void testGetEntityToMove() {
        Mockito.when(mockPrincess.getEntityToMove()).thenCallRealMethod();
        Mockito.when(mockPrincess.isImmobilized(Mockito.any(Entity.class))).thenCallRealMethod();

        Coords mockCoords = Mockito.mock(Coords.class);

        Entity mockMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockMech.getRunMP()).thenReturn(6);
        Mockito.when(mockMech.isOffBoard()).thenReturn(false);
        Mockito.when(mockMech.getPosition()).thenReturn(mockCoords);
        Mockito.when(mockMech.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockPrincess.calculateMoveIndex(Mockito.eq(mockMech), Mockito.any(StringBuilder.class)))
               .thenReturn(1.111);

        Entity mockBA = Mockito.mock(BattleArmor.class);
        Mockito.when(mockBA.getRunMP()).thenReturn(3);
        Mockito.when(mockBA.isOffBoard()).thenReturn(false);
        Mockito.when(mockBA.getPosition()).thenReturn(mockCoords);
        Mockito.when(mockBA.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockPrincess.calculateMoveIndex(Mockito.eq(mockBA), Mockito.any(StringBuilder.class)))
               .thenReturn(6.666);

        Entity mockTank = Mockito.mock(Tank.class);
        Mockito.when(mockTank.getRunMP()).thenReturn(6);
        Mockito.when(mockTank.isOffBoard()).thenReturn(false);
        Mockito.when(mockTank.getPosition()).thenReturn(mockCoords);
        Mockito.when(mockTank.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockPrincess.calculateMoveIndex(Mockito.eq(mockTank), Mockito.any(StringBuilder.class)))
               .thenReturn(2.5);

        Entity mockEjectedMechwarrior = Mockito.mock(MechWarrior.class);
        Mockito.when(mockEjectedMechwarrior.getRunMP()).thenReturn(1);
        Mockito.when(mockEjectedMechwarrior.isOffBoard()).thenReturn(false);
        Mockito.when(mockEjectedMechwarrior.getPosition()).thenReturn(mockCoords);
        Mockito.when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(true);

        Entity mockImmobileMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockImmobileMech.getRunMP()).thenReturn(0);
        Mockito.when(mockImmobileMech.isOffBoard()).thenReturn(false);
        Mockito.when(mockImmobileMech.getPosition()).thenReturn(mockCoords);
        Mockito.when(mockImmobileMech.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockImmobileMech.isImmobile()).thenReturn(true);

        Entity mockOffBoardArty = Mockito.mock(Tank.class);
        Mockito.when(mockOffBoardArty.getRunMP()).thenReturn(6);
        Mockito.when(mockOffBoardArty.getPosition()).thenReturn(mockCoords);
        Mockito.when(mockOffBoardArty.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockOffBoardArty.isOffBoard()).thenReturn(true);
        Mockito.when(mockPrincess.calculateMoveIndex(Mockito.eq(mockOffBoardArty), Mockito.any(StringBuilder.class)))
               .thenReturn(10.0);

        // Test a list of normal units.
        IGame mockGame = Mockito.mock(IGame.class);
        GameTurn mockTurn = Mockito.mock(GameTurn.class);
        Mockito.when(mockGame.getTurn()).thenReturn(mockTurn);
        Mockito.when(mockTurn.isValidEntity(Mockito.any(Entity.class), Mockito.any(IGame.class))).thenReturn(true);
        Mockito.when(mockPrincess.getGame()).thenReturn(mockGame);

        List<Entity> testEntityList = new ArrayList<>();
        testEntityList.add(mockMech);
        testEntityList.add(mockBA);
        testEntityList.add(mockTank);
        Mockito.when(mockPrincess.getEntitiesOwned()).thenReturn(testEntityList);
        Entity pickedEntity = mockPrincess.getEntityToMove();
        Assert.assertEquals(mockBA, pickedEntity);

        // Add the off-board artillery, which should be ignored.  Otherwise it would be picked as the next to move.
        testEntityList.add(mockOffBoardArty);
        pickedEntity = mockPrincess.getEntityToMove();
        Assert.assertEquals(mockBA, pickedEntity);

        // Mark the battle armor as having already been moved.
        Mockito.when(mockBA.isSelectableThisTurn()).thenReturn(false);
        pickedEntity = mockPrincess.getEntityToMove();
        Assert.assertEquals(mockTank, pickedEntity);

        // Add the immobilized mech, which should be picked as the next to move.
        testEntityList.add(mockImmobileMech);
        pickedEntity = mockPrincess.getEntityToMove();
        Assert.assertEquals(mockImmobileMech, pickedEntity);

        // Replace the immobilized mech with the ejected mechwarrior, which should now be the next to move.
        testEntityList.remove(mockImmobileMech);
        testEntityList.add(mockEjectedMechwarrior);
        pickedEntity = mockPrincess.getEntityToMove();
        Assert.assertEquals(mockEjectedMechwarrior, pickedEntity);

        // Test a list that contains a unit with a move index of 0.
        Mockito.when(mockBA.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockTank.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockImmobileMech.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockPrincess.calculateMoveIndex(mockMech, new StringBuilder())).thenReturn(0.0);
        pickedEntity = mockPrincess.getEntityToMove();
        Assert.assertEquals(mockMech, pickedEntity);
        Mockito.when(mockBA.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockTank.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockImmobileMech.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockPrincess.calculateMoveIndex(mockMech, new StringBuilder())).thenReturn(1.111);

        // Test a list where everyone has moved except one unit with the lowest possible move index.
        Mockito.when(mockBA.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockTank.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockImmobileMech.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(false);
        Mockito.when(mockPrincess.calculateMoveIndex(mockMech, new StringBuilder())).thenReturn(Double.MIN_VALUE);
        pickedEntity = mockPrincess.getEntityToMove();
        Assert.assertEquals(mockMech, pickedEntity);
        Mockito.when(mockBA.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockTank.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockImmobileMech.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockEjectedMechwarrior.isSelectableThisTurn()).thenReturn(true);
        Mockito.when(mockPrincess.calculateMoveIndex(mockMech, new StringBuilder())).thenReturn(1.111);
    }

    @Test
    public void testWantsToFallBack() {

        Entity mockMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockMech.isCrippled()).thenReturn(false);

        Mockito.when(mockPrincess.wantsToFallBack(Mockito.any(Entity.class))).thenCallRealMethod();
        //Mockito.when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);
        Mockito.when(mockPrincess.getForcedWithdrawal()).thenReturn(true);
        Mockito.when(mockPrincess.getFallBack()).thenReturn(false);
        Mockito.when(mockPrincess.getFleeBoard()).thenReturn(false);
        //Forced Withdrawal Enabled, Mech Undamaged, Fall Back disabled, Flee Board disabled
        //Should Not Fall Back
        Assert.assertFalse(mockPrincess.wantsToFallBack(mockMech));

        Mockito.when(mockPrincess.getFallBack()).thenReturn(true);
        //Fall Back Enabled
        //Should Fall Back
        Assert.assertTrue(mockPrincess.wantsToFallBack(mockMech));

        Mockito.when(mockPrincess.getFallBack()).thenReturn(false);
        Mockito.when(mockPrincess.getFleeBoard()).thenReturn(true);
        //Fall Back Disabled, Flee Board Enabled (Should Never Happen)
        //Should Not Fall Back
        Assert.assertFalse(mockPrincess.wantsToFallBack(mockMech));

        Mockito.when(mockPrincess.getFleeBoard()).thenReturn(false);
        Mockito.when(mockMech.isCrippled()).thenReturn(true);
        //Fall Back and Flee Board Disabled, Mech Crippled, Forced Withdrawal Enabled
        //Should Fall Back
        Assert.assertTrue(mockPrincess.wantsToFallBack(mockMech));

        //Mockito.when(mockBehavior.isForcedWithdrawal()).thenReturn(false);
        Mockito.when(mockPrincess.getForcedWithdrawal()).thenReturn(false);
        //Fall Back and Flee Board Disabled, Mech Crippled, Forced Withdrawal Disabled
        //Should Not Fall Back
        Assert.assertFalse(mockPrincess.wantsToFallBack(mockMech));
    }

    @Test
    public void testIsFallingBack() {
        Entity mockMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockMech.isImmobile()).thenReturn(false);
        Mockito.when(mockMech.getId()).thenReturn(1);

        Mockito.when(mockPrincess.wantsToFallBack(Mockito.any(Entity.class))).thenReturn(false);
        Mockito.when(mockPrincess.isFallingBack(Mockito.any(Entity.class))).thenCallRealMethod();

        Set<Integer> myFleeingEntities = new HashSet<>(1);
        Mockito.when(mockPrincess.getMyFleeingEntities()).thenReturn(myFleeingEntities);

        // A normal undamaged mech.
        Assert.assertFalse(mockPrincess.isFallingBack(mockMech));

        // A mobile mech that wants to fall back (for any reason).
        myFleeingEntities.add(mockMech.getId());
        Assert.assertTrue(mockPrincess.isFallingBack(mockMech));
    }

    @Test
    public void testMustFleeBoard() {
        Mockito.when(mockPrincess.mustFleeBoard(Mockito.any(Entity.class))).thenCallRealMethod();

        // Unit is not yet falling back
        Mockito.when(mockPrincess.isFallingBack(Mockito.any(Entity.class))).thenReturn(false);

        // Unit is capable of fleeing.
        Entity mockMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockMech.canFlee()).thenReturn(true);

        // Unit is on home edge.
        BasicPathRanker mockRanker = Mockito.mock(BasicPathRanker.class);
        Mockito.when(mockRanker.distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(HomeEdge.class),
                                                   Mockito.any(IGame.class))).thenReturn(0);
        Mockito.when(mockPrincess.getPathRanker()).thenReturn(mockRanker);

        // Mock objects so we don't have nulls.
        Coords mockCoords = Mockito.mock(Coords.class);
        Mockito.when(mockMech.getPosition()).thenReturn(mockCoords);
        Mockito.when(mockPrincess.getHomeEdge()).thenReturn(HomeEdge.NORTH);
        IGame mockGame = Mockito.mock(IGame.class);
        Mockito.when(mockPrincess.getGame()).thenReturn(mockGame);

        // In its current state, the entity does not need to flee the board.
        Assert.assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // Now the unit is falling back, but it should not flee the board unless fleeBoard is enabled
        // or the unit is crippled and forcedWithdrawal is enabled
        Mockito.when(mockPrincess.isFallingBack(Mockito.any(Entity.class))).thenReturn(true);
        Assert.assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // Even a crippled mech should not fall back unless fleeBoard or forcedWithdrawal is enabled
        Mockito.when(mockMech.isCrippled()).thenReturn(true);
        Assert.assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // Enabling forcedWithdrawal should cause fleeing, because mech is crippled
        Mockito.when(mockPrincess.getForcedWithdrawal()).thenReturn(true);
        Assert.assertTrue(mockPrincess.mustFleeBoard(mockMech));

        // But forcedWithdrawal without a crippled mech should not flee
        Mockito.when(mockMech.isCrippled()).thenReturn(false);
        Assert.assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // If fleeBoard is true, all units falling back should flee
        Mockito.when(mockPrincess.getFleeBoard()).thenReturn(true);
        Assert.assertTrue(mockPrincess.mustFleeBoard(mockMech));

        // Make the unit incapable of fleeing.
        Mockito.when(mockMech.canFlee()).thenReturn(false);
        Assert.assertFalse(mockPrincess.mustFleeBoard(mockMech));

        // The unit can flee, but is no longer on the board edge.
        Mockito.when(mockMech.canFlee()).thenReturn(true);
        Mockito.when(mockRanker.distanceToHomeEdge(Mockito.any(Coords.class), Mockito.any(HomeEdge.class),
                                                   Mockito.any(IGame.class))).thenReturn(1);
        Assert.assertFalse(mockPrincess.mustFleeBoard(mockMech));
    }

    @Test
    public void testIsImmobilized() {
        Mockito.when(mockPrincess.isImmobilized(Mockito.any(Entity.class))).thenCallRealMethod();
        Mockito.when(mockPrincess.getBooleanOption(Mockito.eq("tacops_careful_stand"))).thenReturn(false);

        IHex mockHex = Mockito.mock(IHex.class);
        Mockito.when(mockHex.getLevel()).thenReturn(0);
        Mockito.when(mockPrincess.getHex(Mockito.any(Coords.class))).thenReturn(mockHex);

        IGame mockGame = Mockito.mock(IGame.class);
        Mockito.doReturn(mockGame).when(mockPrincess).getGame();

        BehaviorSettings mockBehavior = Mockito.mock(BehaviorSettings.class);
        Mockito.when(mockBehavior.getFallShameIndex()).thenReturn(5);
        Mockito.when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        PilotingRollData mockPilotingRollData = Mockito.mock(PilotingRollData.class);
        Mockito.when(mockPilotingRollData.getValue()).thenReturn(7);

        Coords mockPosiiton = Mockito.mock(Coords.class);

        Coords mockPriorPosition = Mockito.mock(Coords.class);

        // Test a fully mobile mech.
        Mech mockMech = Mockito.mock(BipedMech.class);
        Mockito.when(mockMech.getRunMP()).thenReturn(6);
        Mockito.when(mockMech.isImmobile()).thenReturn(false);
        Mockito.when(mockMech.isShutDown()).thenReturn(false);
        Mockito.when(mockMech.isProne()).thenReturn(false);
        Mockito.when(mockMech.isStuck()).thenReturn(false);
        Mockito.when(mockMech.isStalled()).thenReturn(false);
        Mockito.when(mockMech.cannotStandUpFromHullDown()).thenReturn(false);
        Mockito.when(mockMech.checkGetUp(Mockito.any(MoveStep.class), Mockito.any(EntityMovementType.class))).thenReturn(mockPilotingRollData);
        Mockito.when(mockMech.getPosition()).thenReturn(mockPosiiton);
        Mockito.when(mockMech.getPriorPosition()).thenReturn(mockPriorPosition);
        Mockito.when(mockMech.checkBogDown(Mockito.any(MoveStep.class), Mockito.any(EntityMovementType.class), Mockito.eq(mockHex),
                                           Mockito.eq(mockPriorPosition), Mockito.eq(mockPosiiton), Mockito.anyInt(),
                                           Mockito.anyBoolean()))
               .thenReturn(mockPilotingRollData);
        Assert.assertFalse(mockPrincess.isImmobilized(mockMech));

        // Test a shut down mech.
        Mockito.when(mockMech.isImmobile()).thenReturn(true);
        Mockito.when(mockMech.isShutDown()).thenReturn(true);
        Assert.assertFalse(mockPrincess.isImmobilized(mockMech));

        // Test an immobile mech that is not shut down.
        Mockito.when(mockMech.isImmobile()).thenReturn(true);
        Mockito.when(mockMech.isShutDown()).thenReturn(false);
        Assert.assertTrue(mockPrincess.isImmobilized(mockMech));

        // Test a mech with move 0.
        Mockito.when(mockMech.isImmobile()).thenReturn(false);
        Mockito.when(mockMech.getRunMP()).thenReturn(0);
        Assert.assertTrue(mockPrincess.isImmobilized(mockMech));
        Mockito.when(mockMech.getRunMP()).thenReturn(6);

        // Test a tank that is not immobile.
        Tank mockTank = Mockito.mock(Tank.class);
        Mockito.when(mockTank.getRunMP()).thenReturn(6);
        Mockito.when(mockTank.isImmobile()).thenReturn(false);
        Mockito.when(mockTank.isShutDown()).thenReturn(false);
        Assert.assertFalse(mockPrincess.isImmobilized(mockTank));

        // Test a prone mech that cannot stand up.
        Mockito.when(mockMech.isImmobile()).thenReturn(false);
        Mockito.when(mockMech.isShutDown()).thenReturn(false);
        Mockito.when(mockMech.isProne()).thenReturn(true);
        Mockito.when(mockMech.cannotStandUpFromHullDown()).thenReturn(true);
        Assert.assertTrue(mockPrincess.isImmobilized(mockMech));

        // Test a prone mech whose chance to stand up is better than our fall tolerance threshold.
        Mockito.when(mockMech.isImmobile()).thenReturn(false);
        Mockito.when(mockMech.isShutDown()).thenReturn(false);
        Mockito.when(mockMech.isProne()).thenReturn(true);
        Mockito.when(mockMech.cannotStandUpFromHullDown()).thenReturn(false);
        Assert.assertFalse(mockPrincess.isImmobilized(mockMech));

        // Test a prone mech whose chance to stand up is worse than our fall tolerance threshold.
        Mockito.when(mockPilotingRollData.getValue()).thenReturn(12);
        Mockito.when(mockMech.isImmobile()).thenReturn(false);
        Mockito.when(mockMech.isShutDown()).thenReturn(false);
        Mockito.when(mockMech.isProne()).thenReturn(true);
        Mockito.when(mockMech.cannotStandUpFromHullDown()).thenReturn(false);
        Assert.assertTrue(mockPrincess.isImmobilized(mockMech));

        // Test a stuck mech whose chance to get unstuck is better than our fall tolerance threshold.
        Mockito.when(mockPilotingRollData.getValue()).thenReturn(7);
        Mockito.when(mockMech.isImmobile()).thenReturn(false);
        Mockito.when(mockMech.isShutDown()).thenReturn(false);
        Mockito.when(mockMech.isProne()).thenReturn(false);
        Mockito.when(mockMech.isStuck()).thenReturn(true);
        Assert.assertFalse(mockPrincess.isImmobilized(mockMech));

        // Test a stuck mech whose chance to get unstuck is worse than our fall tolerance threshold.
        Mockito.when(mockPilotingRollData.getValue()).thenReturn(12);
        Mockito.when(mockMech.isImmobile()).thenReturn(false);
        Mockito.when(mockMech.isShutDown()).thenReturn(false);
        Mockito.when(mockMech.isProne()).thenReturn(false);
        Mockito.when(mockMech.isStuck()).thenReturn(true);
        Assert.assertTrue(mockPrincess.isImmobilized(mockMech));
    }
}
