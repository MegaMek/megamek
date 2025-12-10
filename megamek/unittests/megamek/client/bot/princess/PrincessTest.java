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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.client.bot.princess.PathRanker.PathRankerType;
import megamek.common.Facing;
import megamek.common.Hex;
import megamek.common.MPCalculationSetting;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.moves.MoveStep;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/22/13 8:33 AM
 */
class PrincessTest {

    static WeaponType mockAC5 = (WeaponType) EquipmentType.get("ISAC5");
    static AmmoType mockAC5AmmoType = (AmmoType) EquipmentType.get("ISAC5 Ammo");
    static WeaponType mockRL20 = (WeaponType) EquipmentType.get("RL20");
    private Princess mockPrincess;
    private BasicPathRanker mockPathRanker;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        mockPathRanker = mock(BasicPathRanker.class);

        MoraleUtil mockMoralUtil = mock(MoraleUtil.class);

        mockPrincess = mock(Princess.class);
        when(mockPrincess.getPathRanker(PathRankerType.Basic)).thenReturn(mockPathRanker);
        when(mockPrincess.getPathRanker(any(Entity.class))).thenReturn(mockPathRanker);
        when(mockPrincess.getMoraleUtil()).thenReturn(mockMoralUtil);
        when(mockPrincess.calcAmmoConservation(any(Entity.class))).thenCallRealMethod();
        when(mockPrincess.shouldAbandon(any(Entity.class))).thenCallRealMethod();
    }

    @Test
    void testCalculateAdjustment() {
        // Test a +3 adjustment.
        assertEquals(3, Princess.calculateAdjustment("+++"));
        assertEquals(2, Princess.calculateAdjustment("++3"));
        assertEquals(2, Princess.calculateAdjustment("3++"));

        // Test a -2 adjustment.
        assertEquals(-2, Princess.calculateAdjustment("--"));

        // Test an adjustment with some bad characters.
        assertEquals(1, Princess.calculateAdjustment("+p"));

        // Test an adjustment with a number should set the value to the number.
        assertEquals(5, Princess.calculateAdjustment("5"));

        // Test an adjustment with a number should set the value to the number.
        assertEquals(-5, Princess.calculateAdjustment("-5"));

        // Test an adjustment with a number should set the value to the number.
        assertEquals(22, Princess.calculateAdjustment("+22"));

        // Test an empty ticks argument.
        assertEquals(0, Princess.calculateAdjustment(""));

        // Test a null ticks argument.
        assertEquals(0, Princess.calculateAdjustment(null));
    }

    @Test
    void testCalculateMoveIndex() {
        final double TOLERANCE = 0.001;
        when(mockPrincess.calculateMoveIndex(any(Entity.class), any(StringBuilder.class))).thenCallRealMethod();
        when(mockPrincess.isFallingBack(any(Entity.class))).thenReturn(false);

        when(mockPathRanker.distanceToClosestEnemy(any(Entity.class),
              nullable(Coords.class),
              nullable(Game.class))).thenReturn(10.0);

        // Test a 6/9/6 regular mek.
        Entity mockMek = mock(BipedMek.class);
        when(mockMek.getRunMP(MPCalculationSetting.STANDARD)).thenReturn(9);
        when(mockMek.getJumpMP(MPCalculationSetting.STANDARD)).thenReturn(6);
        when(mockMek.getAnyTypeMaxJumpMP()).thenReturn(0);
        when(mockMek.isProne()).thenReturn(false);
        when(mockMek.isCommander()).thenReturn(false);
        when(mockMek.isMilitary()).thenReturn(true);
        when(mockMek.isStealthActive()).thenReturn(false);
        when(mockMek.isStealthOn()).thenReturn(false);
        when(mockMek.isVoidSigActive()).thenReturn(false);
        when(mockMek.isVoidSigOn()).thenReturn(false);
        double actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(1.111, actual, TOLERANCE);

        // Make the mek prone.
        when(mockMek.isProne()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(1.222, actual, TOLERANCE);

        // Make the mek flee.
        when(mockMek.isProne()).thenReturn(false);
        when(mockPrincess.isFallingBack(eq(mockMek))).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(2.222, actual, TOLERANCE);

        // Make the mek a commander.
        when(mockPrincess.isFallingBack(eq(mockMek))).thenReturn(false);
        when(mockMek.isCommander()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(0.555, actual, TOLERANCE);

        // Make it a civilian mek.
        when(mockMek.isCommander()).thenReturn(false);
        when(mockMek.isMilitary()).thenReturn(false);
        actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(5.555, actual, TOLERANCE);

        // Make it stealthy;
        when(mockMek.isMilitary()).thenReturn(true);
        when(mockMek.isStealthActive()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(0.37, actual, TOLERANCE);
        when(mockMek.isStealthActive()).thenReturn(false);
        when(mockMek.isStealthOn()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(0.37, actual, TOLERANCE);
        when(mockMek.isStealthOn()).thenReturn(false);
        when(mockMek.isVoidSigActive()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(0.37, actual, TOLERANCE);
        when(mockMek.isVoidSigActive()).thenReturn(false);
        when(mockMek.isVoidSigOn()).thenReturn(true);
        actual = mockPrincess.calculateMoveIndex(mockMek, new StringBuilder());
        assertEquals(0.37, actual, TOLERANCE);

        // Test a BA unit.
        Entity mockBA = mock(BattleArmor.class);
        when(mockBA.getRunMP()).thenReturn(1);
        when(mockBA.getAnyTypeMaxJumpMP()).thenReturn(3);
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
        when(mockInf.getAnyTypeMaxJumpMP()).thenReturn(0);
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
        when(mockInf.getAnyTypeMaxJumpMP()).thenReturn(0);
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
    void testGetEntityToMove() {
        when(mockPrincess.getEntityToMove()).thenCallRealMethod();
        when(mockPrincess.isImmobilized(any(Entity.class))).thenCallRealMethod();

        Coords mockCoords = mock(Coords.class);

        Entity mockMek = mock(BipedMek.class);
        when(mockMek.getRunMP()).thenReturn(6);
        when(mockMek.isOffBoard()).thenReturn(false);
        when(mockMek.getPosition()).thenReturn(mockCoords);
        when(mockMek.isSelectableThisTurn()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(eq(mockMek), any(StringBuilder.class))).thenReturn(1.111);

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

        Entity mockEjectedMekwarrior = mock(MekWarrior.class);
        when(mockEjectedMekwarrior.getRunMP()).thenReturn(1);
        when(mockEjectedMekwarrior.isOffBoard()).thenReturn(false);
        when(mockEjectedMekwarrior.getPosition()).thenReturn(mockCoords);
        when(mockEjectedMekwarrior.isSelectableThisTurn()).thenReturn(true);

        Entity mockImmobileMek = mock(BipedMek.class);
        when(mockImmobileMek.getRunMP()).thenReturn(0);
        when(mockImmobileMek.isOffBoard()).thenReturn(false);
        when(mockImmobileMek.getPosition()).thenReturn(mockCoords);
        when(mockImmobileMek.isSelectableThisTurn()).thenReturn(true);
        when(mockImmobileMek.isImmobile()).thenReturn(true);

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
        PlanetaryConditions mockPC = new PlanetaryConditions();
        mockPC.setGravity(1.0f);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPC);
        when(mockPrincess.getGame()).thenReturn(mockGame);

        List<Entity> testEntityList = new ArrayList<>();
        testEntityList.add(mockMek);
        testEntityList.add(mockBA);
        testEntityList.add(mockTank);
        when(mockPrincess.getEntitiesOwned()).thenReturn(testEntityList);
        Entity pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockBA, pickedEntity);

        // Add the off-board artillery, which should be ignored. Otherwise, it would be
        // picked as the next to move.
        testEntityList.add(mockOffBoardArty);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockBA, pickedEntity);

        // Mark the battle armor as having already been moved.
        when(mockBA.isSelectableThisTurn()).thenReturn(false);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockTank, pickedEntity);

        // Add the immobilized mek, which should be picked as the next to move.
        testEntityList.add(mockImmobileMek);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockImmobileMek, pickedEntity);

        // Replace the immobilized mek with the ejected mekwarrior, which should now be
        // the next to move.
        testEntityList.remove(mockImmobileMek);
        testEntityList.add(mockEjectedMekwarrior);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockEjectedMekwarrior, pickedEntity);

        // Test a list that contains a unit with a move index of 0.
        when(mockBA.isSelectableThisTurn()).thenReturn(false);
        when(mockTank.isSelectableThisTurn()).thenReturn(false);
        when(mockImmobileMek.isSelectableThisTurn()).thenReturn(false);
        when(mockEjectedMekwarrior.isSelectableThisTurn()).thenReturn(false);
        when(mockPrincess.calculateMoveIndex(mockMek, new StringBuilder())).thenReturn(0.0);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockMek, pickedEntity);
        when(mockBA.isSelectableThisTurn()).thenReturn(true);
        when(mockTank.isSelectableThisTurn()).thenReturn(true);
        when(mockImmobileMek.isSelectableThisTurn()).thenReturn(true);
        when(mockEjectedMekwarrior.isSelectableThisTurn()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(mockMek, new StringBuilder())).thenReturn(1.111);

        // Test a list where everyone has moved except one unit with the lowest possible
        // move index.
        when(mockBA.isSelectableThisTurn()).thenReturn(false);
        when(mockTank.isSelectableThisTurn()).thenReturn(false);
        when(mockImmobileMek.isSelectableThisTurn()).thenReturn(false);
        when(mockEjectedMekwarrior.isSelectableThisTurn()).thenReturn(false);
        when(mockPrincess.calculateMoveIndex(mockMek, new StringBuilder())).thenReturn(Double.MIN_VALUE);
        pickedEntity = mockPrincess.getEntityToMove();
        assertEquals(mockMek, pickedEntity);
        when(mockBA.isSelectableThisTurn()).thenReturn(true);
        when(mockTank.isSelectableThisTurn()).thenReturn(true);
        when(mockImmobileMek.isSelectableThisTurn()).thenReturn(true);
        when(mockEjectedMekwarrior.isSelectableThisTurn()).thenReturn(true);
        when(mockPrincess.calculateMoveIndex(mockMek, new StringBuilder())).thenReturn(1.111);
    }

    @Test
    void testWantsToFallBack() {
        Entity mockMek = mock(BipedMek.class);
        when(mockMek.isCrippled()).thenReturn(false);

        when(mockPrincess.wantsToFallBack(any(Entity.class))).thenCallRealMethod();
        when(mockPrincess.getForcedWithdrawal()).thenReturn(true);
        when(mockPrincess.getFallBack()).thenReturn(false);
        when(mockPrincess.getFleeBoard()).thenReturn(false);
        // Forced Withdrawal Enabled, Mek Undamaged, Fall Back disabled, Flee Board
        // disabled
        // Should Not Fall Back
        assertFalse(mockPrincess.wantsToFallBack(mockMek));

        when(mockPrincess.getFallBack()).thenReturn(true);
        // Fall Back Enabled
        // Should Fall Back
        assertTrue(mockPrincess.wantsToFallBack(mockMek));

        when(mockPrincess.getFallBack()).thenReturn(false);
        when(mockPrincess.getFleeBoard()).thenReturn(true);
        // Fall Back Disabled, Flee Board Enabled (Should Never Happen)
        // Should Not Fall Back
        assertFalse(mockPrincess.wantsToFallBack(mockMek));

        when(mockPrincess.getFleeBoard()).thenReturn(false);
        when(mockMek.isCrippled()).thenReturn(true);
        // Fall Back and Flee Board Disabled, Mek Crippled, Forced Withdrawal Enabled
        // Should Fall Back
        assertTrue(mockPrincess.wantsToFallBack(mockMek));

        when(mockPrincess.getForcedWithdrawal()).thenReturn(false);
        // Fall Back and Flee Board Disabled, Mek Crippled, Forced Withdrawal Disabled
        // Should Not Fall Back
        assertFalse(mockPrincess.wantsToFallBack(mockMek));
    }

    @Test
    void testIsFallingBack() {
        Entity mockMek = mock(BipedMek.class);
        when(mockMek.isImmobile()).thenReturn(false);
        when(mockMek.isCrippled(anyBoolean())).thenReturn(false);
        when(mockMek.getId()).thenReturn(1);

        when(mockPrincess.wantsToFallBack(any(Entity.class))).thenReturn(false);
        when(mockPrincess.isFallingBack(any(Entity.class))).thenCallRealMethod();

        BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getDestinationEdge()).thenReturn(CardinalEdge.NONE);
        when(mockBehavior.isForcedWithdrawal()).thenReturn(true);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        // A normal undamaged mek.
        assertFalse(mockPrincess.isFallingBack(mockMek));

        // A mobile mek that wants to fall back (for any reason).
        when(mockMek.isCrippled(anyBoolean())).thenReturn(true);
        assertTrue(mockPrincess.isFallingBack(mockMek));

        // A mek whose bot is set for a destination edge
        when(mockBehavior.getDestinationEdge()).thenReturn(CardinalEdge.NEAREST);
        assertTrue(mockPrincess.isFallingBack(mockMek));
    }

    @Test
    void testMustFleeBoard() {
        when(mockPrincess.mustFleeBoard(any(Entity.class))).thenCallRealMethod();

        // Unit is not yet falling back
        when(mockPrincess.isFallingBack(any(Entity.class))).thenReturn(false);

        // Unit is capable of fleeing.
        Entity mockMek = mock(BipedMek.class);

        // Unit is on home edge.
        BasicPathRanker mockRanker = mock(BasicPathRanker.class);
        when(mockRanker.distanceToHomeEdge(any(Coords.class),
              anyInt(),
              any(CardinalEdge.class),
              any(Game.class))).thenReturn(0);
        when(mockPrincess.getPathRanker(any(Entity.class))).thenReturn(mockRanker);

        // Mock objects so we don't have nulls.
        Coords mockCoords = mock(Coords.class);
        when(mockMek.getPosition()).thenReturn(mockCoords);
        when(mockPrincess.getHomeEdge(any(Entity.class))).thenReturn(CardinalEdge.NORTH);
        Game mockGame = mock(Game.class);
        when(mockPrincess.getGame()).thenReturn(mockGame);
        when(mockMek.canFlee(mockMek.getPosition())).thenReturn(true);

        // In its current state, the entity does not need to flee the board.
        assertFalse(mockPrincess.mustFleeBoard(mockMek));

        // Now the unit is falling back, but it should not flee the board unless
        // fleeBoard is enabled
        // or the unit is crippled and forcedWithdrawal is enabled
        when(mockPrincess.isFallingBack(any(Entity.class))).thenReturn(true);
        assertFalse(mockPrincess.mustFleeBoard(mockMek));

        // Even a crippled mek should not fall back unless fleeBoard or forcedWithdrawal
        // is enabled
        when(mockMek.isCrippled()).thenReturn(true);
        assertFalse(mockPrincess.mustFleeBoard(mockMek));

        // Enabling forcedWithdrawal should cause fleeing, because mek is crippled
        when(mockPrincess.getForcedWithdrawal()).thenReturn(true);
        assertTrue(mockPrincess.mustFleeBoard(mockMek));

        // But forcedWithdrawal without a crippled mek should not flee
        when(mockMek.isCrippled()).thenReturn(false);
        assertFalse(mockPrincess.mustFleeBoard(mockMek));

        // If fleeBoard is true, all units falling back should flee
        when(mockPrincess.getFleeBoard()).thenReturn(true);
        assertTrue(mockPrincess.mustFleeBoard(mockMek));

        // Make the unit incapable of fleeing.
        when(mockMek.canFlee(mockMek.getPosition())).thenReturn(false);
        assertFalse(mockPrincess.mustFleeBoard(mockMek));

        // The unit can flee, but is no longer on the board edge.
        when(mockMek.canFlee(mockMek.getPosition())).thenReturn(true);
        when(mockRanker.distanceToHomeEdge(any(Coords.class),
              anyInt(),
              any(CardinalEdge.class),
              any(Game.class))).thenReturn(1);
        assertFalse(mockPrincess.mustFleeBoard(mockMek));
    }

    @Test
    void testIsImmobilized() {
        when(mockPrincess.isImmobilized(any(Entity.class))).thenCallRealMethod();
        when(mockPrincess.getBooleanOption(eq("tacops_careful_stand"))).thenReturn(false);

        Hex mockHex = mock(Hex.class);
        when(mockHex.getLevel()).thenReturn(0);

        Game mockGame = mock(Game.class);
        PlanetaryConditions mockPC = new PlanetaryConditions();
        mockPC.setGravity(1.0f);
        when(mockGame.getPlanetaryConditions()).thenReturn(mockPC);
        doReturn(mockGame).when(mockPrincess).getGame();
        when(mockGame.getHexOf(any(Targetable.class))).thenReturn(mockHex);

        BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getFallShameIndex()).thenReturn(5);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        PilotingRollData mockPilotingRollData = mock(PilotingRollData.class);
        when(mockPilotingRollData.getValue()).thenReturn(7);

        Coords mockPosition = mock(Coords.class);

        Coords mockPriorPosition = mock(Coords.class);

        // Test a fully mobile mek.
        Mek mockMek = mock(BipedMek.class);
        when(mockMek.getRunMP()).thenReturn(6);
        when(mockMek.isImmobile()).thenReturn(false);
        when(mockMek.isShutDown()).thenReturn(false);
        when(mockMek.isProne()).thenReturn(false);
        when(mockMek.isStuck()).thenReturn(false);
        when(mockMek.isStalled()).thenReturn(false);
        when(mockMek.cannotStandUpFromHullDown()).thenReturn(false);
        when(mockMek.checkGetUp(any(MoveStep.class), any(EntityMovementType.class))).thenReturn(mockPilotingRollData);
        when(mockMek.getPosition()).thenReturn(mockPosition);
        when(mockMek.getPriorPosition()).thenReturn(mockPriorPosition);
        when(mockMek.checkBogDown(any(MoveStep.class),
              any(EntityMovementType.class),
              eq(mockHex),
              eq(mockPriorPosition),
              eq(mockPosition),
              anyInt(),
              anyBoolean())).thenReturn(mockPilotingRollData);
        assertFalse(mockPrincess.isImmobilized(mockMek));

        // Test a shutdown mek.
        when(mockMek.isImmobile()).thenReturn(true);
        when(mockMek.isShutDown()).thenReturn(true);
        assertFalse(mockPrincess.isImmobilized(mockMek));

        // Test an immobile mek that is not shut down.
        when(mockMek.isImmobile()).thenReturn(true);
        when(mockMek.isShutDown()).thenReturn(false);
        assertTrue(mockPrincess.isImmobilized(mockMek));

        // Test a mek with move 0.
        when(mockMek.isImmobile()).thenReturn(false);
        when(mockMek.getRunMP()).thenReturn(0);
        assertTrue(mockPrincess.isImmobilized(mockMek));
        when(mockMek.getRunMP()).thenReturn(6);

        // Test a tank that is not immobile.
        Tank mockTank = mock(Tank.class);
        when(mockTank.getRunMP()).thenReturn(6);
        when(mockTank.isImmobile()).thenReturn(false);
        when(mockTank.isShutDown()).thenReturn(false);
        assertFalse(mockPrincess.isImmobilized(mockTank));

        // Test a prone mek that cannot stand up.
        when(mockMek.isImmobile()).thenReturn(false);
        when(mockMek.isShutDown()).thenReturn(false);
        when(mockMek.isProne()).thenReturn(true);
        when(mockMek.cannotStandUpFromHullDown()).thenReturn(true);
        assertTrue(mockPrincess.isImmobilized(mockMek));

        // Test a prone mek whose chance to stand up is better than our fall tolerance
        // threshold.
        when(mockMek.isImmobile()).thenReturn(false);
        when(mockMek.isShutDown()).thenReturn(false);
        when(mockMek.isProne()).thenReturn(true);
        when(mockMek.cannotStandUpFromHullDown()).thenReturn(false);
        assertFalse(mockPrincess.isImmobilized(mockMek));

        // Test a prone mek whose chance to stand up is worse than our fall tolerance
        // threshold.
        when(mockPilotingRollData.getValue()).thenReturn(12);
        when(mockMek.isImmobile()).thenReturn(false);
        when(mockMek.isShutDown()).thenReturn(false);
        when(mockMek.isProne()).thenReturn(true);
        when(mockMek.cannotStandUpFromHullDown()).thenReturn(false);
        assertTrue(mockPrincess.isImmobilized(mockMek));

        // Test a stuck mek whose chance to get unstuck is better than our fall
        // tolerance threshold.
        when(mockPilotingRollData.getValue()).thenReturn(7);
        when(mockMek.isImmobile()).thenReturn(false);
        when(mockMek.isShutDown()).thenReturn(false);
        when(mockMek.isProne()).thenReturn(false);
        when(mockMek.isStuck()).thenReturn(true);
        assertFalse(mockPrincess.isImmobilized(mockMek));

        // Test a stuck mek whose chance to get unstuck is worse than our fall tolerance
        // threshold.
        when(mockPilotingRollData.getValue()).thenReturn(12);
        when(mockMek.isImmobile()).thenReturn(false);
        when(mockMek.isShutDown()).thenReturn(false);
        when(mockMek.isProne()).thenReturn(false);
        when(mockMek.isStuck()).thenReturn(true);
        assertTrue(mockPrincess.isImmobilized(mockMek));
    }

    @Test
    void testCalcAmmoForDefaultAggressionLevel() throws LocationFullException {
        // Expected toHitThresholds should equate to a TN of 12, 11, and 10 for ammo
        // values
        // of 7+, 3+, 1.

        // Set aggression to default level
        BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getHyperAggressionIndex()).thenReturn(5);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        // Set up unit
        Mek mek1 = new BipedMek();
        Mounted<?> bin1 = mek1.addEquipment(mockAC5AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> wpn1 = mek1.addEquipment(mockAC5, Mek.LOC_RIGHT_TORSO);

        // Check default toHitThresholds
        // Default toHitThreshold for 7+ rounds for this level should allow firing on
        // 12s
        double target = Compute.oddsAbove(12) / 100.0;
        bin1.setShotsLeft(7);
        Map<WeaponMounted, Double> conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get((WeaponMounted) wpn1) <= target);

        // Default toHitThreshold for 3+ rounds for this level should allow firing on
        // 11s
        target = Compute.oddsAbove(11) / 100.0;
        bin1.setShotsLeft(3);
        conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get(wpn1) <= target);

        // Default toHitThreshold for 1 rounds for this level should allow firing on 10s
        target = Compute.oddsAbove(10) / 100.0;
        bin1.setShotsLeft(1);
        conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get(wpn1) <= target);
    }

    @Test
    void testCalcAmmoForMaxAggressionLevel() throws LocationFullException {
        // Expected toHitThresholds should equate to a TN of 12, 12, and 10 for ammo
        // values
        // of 7+, 3+, 1.

        // Set aggression to default level
        BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getHyperAggressionIndex()).thenReturn(10);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        // Set up unit
        Mek mek1 = new BipedMek();
        Mounted<?> bin1 = mek1.addEquipment(mockAC5AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> wpn1 = mek1.addEquipment(mockAC5, Mek.LOC_RIGHT_TORSO);

        // Check default toHitThresholds
        // Default toHitThreshold for 7+ rounds for this level should allow firing on
        // 12s
        double target = Compute.oddsAbove(12) / 100.0;
        bin1.setShotsLeft(7);
        Map<WeaponMounted, Double> conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get((WeaponMounted) wpn1) <= target);

        // Default toHitThreshold for 3+ rounds for this level should allow firing on
        // 12s
        bin1.setShotsLeft(3);
        conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get(wpn1) <= target);

        // Default toHitThreshold for 1 rounds for this level should allow firing on 10s
        target = Compute.oddsAbove(10) / 100.0;
        bin1.setShotsLeft(1);
        conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get(wpn1) <= target);
    }

    @Test
    void testCalcAmmoForZeroAggressionLevel() throws LocationFullException {
        // Expected toHitThresholds should equate to a TN of 10, 9, and 7 for ammo
        // values
        // of 7+, 3+, 1.

        // Set aggression to default level
        BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getHyperAggressionIndex()).thenReturn(0);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        // Set up unit
        Mek mek1 = new BipedMek();
        Mounted<?> bin1 = mek1.addEquipment(mockAC5AmmoType, Mek.LOC_LEFT_TORSO);
        Mounted<?> wpn1 = mek1.addEquipment(mockAC5, Mek.LOC_RIGHT_TORSO);

        // Check default toHitThresholds
        // Default toHitThreshold for 7+ rounds for this level should allow firing on
        // 12s
        double target = Compute.oddsAbove(10) / 100.0;
        bin1.setShotsLeft(7);
        Map<WeaponMounted, Double> conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get((WeaponMounted) wpn1) <= target);

        // Default toHitThreshold for 3+ rounds for this level should allow firing on
        // 11s
        target = Compute.oddsAbove(9) / 100.0;
        bin1.setShotsLeft(3);
        conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get(wpn1) <= target);

        // Default toHitThreshold for 1 rounds for this level should allow firing on 10s
        target = Compute.oddsAbove(7) / 100.0;
        bin1.setShotsLeft(1);
        conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get(wpn1) <= target);
    }

    @Test
    void testCalcAmmoForOneShotWeapons() throws LocationFullException {
        // Set aggression to the lowest level first
        BehaviorSettings mockBehavior = mock(BehaviorSettings.class);
        when(mockBehavior.getHyperAggressionIndex()).thenReturn(0);
        when(mockPrincess.getBehaviorSettings()).thenReturn(mockBehavior);

        // Set up unit
        Mek mek1 = new BipedMek();
        Mounted<?> wpn1 = mek1.addEquipment(mockRL20, Mek.LOC_LEFT_TORSO);

        // Check default toHitThresholds
        // For max aggro, shoot OS weapons at TN 10 or better
        double target = Compute.oddsAbove(8) / 100.0;
        Map<WeaponMounted, Double> conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get((WeaponMounted) wpn1) <= target);

        // For default aggro, shoot OS weapons at TN 9 or better
        when(mockBehavior.getHyperAggressionIndex()).thenReturn(5);
        target = Compute.oddsAbove(9) / 100.0;
        conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get(wpn1) <= target);

        // For lowest aggro, shoot OS weapons at TN 8 or better
        when(mockBehavior.getHyperAggressionIndex()).thenReturn(10);
        target = Compute.oddsAbove(10) / 100.0;
        conserveMap = mockPrincess.calcAmmoConservation(mek1);
        assertTrue(conserveMap.get(wpn1) <= target);
    }

    @Test
    void testShouldAbandonShouldNotAbandon() {
        // Tank is working fine
        Tank tank = new Tank();
        assertFalse(tank.isPermanentlyImmobilized(true));
        assertFalse(tank.isCrippled());
        assertFalse(tank.isShutDown());
        assertFalse(tank.isDoomed());
        assertFalse(mockPrincess.shouldAbandon(tank));
    }

    @Test
    void testShouldAbandonShouldAbandonTankCrewDead() {
        // Tank is crew kill
        Tank tank = new Tank();
        Crew crew = new Crew(CrewType.CREW);
        tank.setCrew(crew);
        crew.setDead(true);
        assertTrue(tank.isPermanentlyImmobilized(true));
        // Per TW Errata v11.01 p.54, immobilized units are NOT automatically crippled
        assertFalse(tank.isCrippled());
        assertFalse(tank.isShutDown());
        assertFalse(tank.isDoomed());
        assertTrue(mockPrincess.shouldAbandon(tank));
    }

    @Test
    void testShouldAbandonShouldAbandonTankImmobilized() {
        // Tank is mobility killed
        Tank tank = new Tank();
        Crew crew = new Crew(CrewType.CREW);
        tank.setCrew(crew);
        tank.setOriginalWalkMP(4);
        tank.setMotiveDamage(4);
        assertTrue(tank.isPermanentlyImmobilized(true));
        // Per TW Errata v11.01 p.54, immobilized units are NOT automatically crippled
        assertFalse(tank.isCrippled());
        assertFalse(tank.isShutDown());
        assertFalse(tank.isDoomed());
        assertTrue(mockPrincess.shouldAbandon(tank));
    }

    @Test
    void testShouldAbandonShouldAbandonTankArmorCrippled() {
        // Tank is nearly dead so hop off
        Tank tank = new Tank();
        Crew crew = new Crew(CrewType.CREW);
        tank.setCrew(crew);
        tank.initializeArmor(10, Tank.LOC_FRONT);
        tank.setArmor(0, Tank.LOC_FRONT);
        assertFalse(tank.isPermanentlyImmobilized(true));
        assertTrue(tank.isCrippled());
        assertFalse(tank.isShutDown());
        assertFalse(tank.isDoomed());
        assertTrue(mockPrincess.shouldAbandon(tank));
    }

    @Test
    void testShouldAbandonShouldAbandonVTOLNoFlying() {
        // VTOL with rotor blown off
        VTOL vtol = new VTOL();
        // Non-turreted VTOL
        vtol.setOriginalWalkMP(5);
        vtol.setHasNoTurret(true);
        vtol.setHasNoDualTurret(true);
        for (int loc = 0; loc <= 6; loc++) {
            vtol.initializeArmor(5, loc);
        }
        vtol.setInternal(IArmorState.ARMOR_DESTROYED, VTOL.LOC_ROTOR);
        vtol.setElevation(0);
        Crew crew = new Crew(CrewType.CREW);
        vtol.setCrew(crew);

        assertTrue(vtol.isPermanentlyImmobilized(true));
        // Per TW Errata v11.01 p.54, immobilized units are NOT automatically crippled
        assertFalse(vtol.isCrippled());
        assertFalse(vtol.isShutDown());
        assertFalse(vtol.isDoomed());
        assertTrue(mockPrincess.shouldAbandon(vtol));
    }

    Board createLevelBoard(int width, int height, int level) {
        Board board = new Board(width, height);
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Coords c = new Coords(x, y);
                board.setHex(c, new Hex(level));
            }
        }
        return board;
    }

    @Test
    void testShouldAbandonShouldAbandonAeroSpaceFighterNotMoving() {
        // Aerospace carrier, likely cargo-bay-mounted Infantry transport
        Aero aero = new AeroSpaceFighter();
        Game game = new Game();
        aero.setGame(game);
        game.setBoard(createLevelBoard(32, 34, 0));
        aero.setPosition(new Coords(5, 5));
        aero.setFacing(Facing.SE.getIntValue());

        aero.setOriginalWalkMP(5);
        aero.setElevation(0);
        aero.setAltitude(0);
        Crew crew = new Crew(CrewType.CREW);
        aero.setCrew(crew);

        // Aero unit has been grounded two turns
        aero.moved = EntityMovementType.MOVE_NONE;
        aero.movedLastRound = EntityMovementType.MOVE_NONE;

        assertFalse(aero.isPermanentlyImmobilized(true));
        assertFalse(aero.isCrippled());
        assertFalse(aero.isShutDown());
        assertFalse(aero.isDoomed());
        assertTrue(mockPrincess.shouldAbandon(aero));
    }

    @Test
    void testShouldAbandonShouldAbandonDropShipEngineHits() {
        // Spheroid DS is crippled by engine hits
        Aero aero = new Dropship();
        aero.setSpheroid(true);
        Game game = new Game();
        aero.setGame(game);
        game.setBoard(createLevelBoard(32, 34, 0));
        aero.setPosition(new Coords(5, 5));
        aero.setFacing(Facing.SE.getIntValue());

        aero.setOriginalWalkMP(5);
        aero.setEngineHits(aero.getMaxEngineHits() - 2);
        aero.setElevation(0);
        aero.setAltitude(0);
        Crew crew = new Crew(CrewType.CREW);
        aero.setCrew(crew);

        assertFalse(aero.isPermanentlyImmobilized(true));
        assertTrue(aero.isCrippled());
        assertFalse(aero.isShutDown());
        assertFalse(aero.isDoomed());
        assertTrue(mockPrincess.shouldAbandon(aero));
    }

    @Test
    void testShouldAbandonDoNotAbandonDropShipCanFly() {
        // Aerodyne DS has maneuvered to clear its runway for liftoff
        Aero aero = new Dropship();
        aero.setSpheroid(false);
        Game game = new Game();
        aero.setGame(game);
        game.setBoard(createLevelBoard(32, 34, 0));
        aero.setPosition(new Coords(5, 5));
        aero.setFacing(Facing.SE.getIntValue());

        aero.setOriginalWalkMP(3);
        aero.setElevation(0);
        aero.setAltitude(0);
        Crew crew = new Crew(CrewType.CREW);
        aero.setCrew(crew);

        // Aero unit has been mobile for last two turns
        aero.moved = EntityMovementType.MOVE_WALK;
        aero.movedLastRound = EntityMovementType.MOVE_WALK;

        assertFalse(aero.isPermanentlyImmobilized(true));
        assertFalse(aero.isCrippled());
        assertFalse(aero.isShutDown());
        assertFalse(aero.isDoomed());
        assertFalse(mockPrincess.shouldAbandon(aero));
    }

    @Test
    void testShouldAbandonShouldAbandonDropShipCannotLiftOff() {
        // Aerodyne DS has a wall in the middle of its takeoff run: go ahead and abandon
        Aero aero = new Dropship();
        aero.setSpheroid(false);
        Game game = new Game();
        aero.setGame(game);
        game.setBoard(createLevelBoard(32, 34, 0));
        // Add blockage
        game.getBoard().setHex(12, 7, new Hex(4));
        game.getBoard().setHex(12, 8, new Hex(4));
        game.getBoard().setHex(12, 9, new Hex(4));
        aero.setPosition(new Coords(5, 5));
        aero.setFacing(Facing.SE.getIntValue());

        aero.setOriginalWalkMP(3);
        aero.setElevation(0);
        aero.setAltitude(0);
        Crew crew = new Crew(CrewType.CREW);
        aero.setCrew(crew);

        // Aero unit has been mobile for last two turns
        aero.moved = EntityMovementType.MOVE_WALK;
        aero.movedLastRound = EntityMovementType.MOVE_WALK;

        assertFalse(aero.isPermanentlyImmobilized(true));
        assertFalse(aero.isCrippled());
        assertFalse(aero.isShutDown());
        assertFalse(aero.isDoomed());
        assertTrue(mockPrincess.shouldAbandon(aero));
    }
}
