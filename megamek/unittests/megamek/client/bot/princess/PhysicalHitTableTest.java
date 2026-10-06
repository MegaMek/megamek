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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link PhysicalHitTable} resolves the same hit-location table the rules engine would:
 * elevation decides whether a kick reaches the legs or the head, and whether a punch lands on the punch
 * table, the kick table (a leg punch), or the full body of a height-zero target.
 */
class PhysicalHitTableTest {

    private static final Coords ATTACKER_POSITION = new Coords(0, 0);
    private static final Coords TARGET_POSITION = new Coords(1, 0);

    private Game mockGame;
    private Hex attackerHex;
    private Hex targetHex;
    private BipedMek attacker;
    private BipedMek targetMek;
    private EntityState attackerState;
    private EntityState targetState;

    @BeforeEach
    void setUp() {
        mockGame = mock(Game.class);
        Board mockBoard = mock(Board.class);
        doReturn(mockBoard).when(mockGame).getBoard(any(Targetable.class));

        attackerHex = mock(Hex.class);
        targetHex = mock(Hex.class);
        when(mockBoard.getHex(ATTACKER_POSITION)).thenReturn(attackerHex);
        when(mockBoard.getHex(TARGET_POSITION)).thenReturn(targetHex);

        attacker = mock(BipedMek.class);
        when(attacker.getHeight()).thenReturn(1);
        targetMek = mock(BipedMek.class);
        when(targetMek.getHeight()).thenReturn(1);

        attackerState = mock(EntityState.class);
        when(attackerState.getPosition()).thenReturn(ATTACKER_POSITION);
        when(attackerState.getElevation()).thenReturn(0);
        targetState = mock(EntityState.class);
        when(targetState.getPosition()).thenReturn(TARGET_POSITION);
        when(targetState.getElevation()).thenReturn(0);
    }

    private void setHexLevels(int attackerLevel, int targetLevel) {
        when(attackerHex.getLevel()).thenReturn(attackerLevel);
        when(targetHex.getLevel()).thenReturn(targetLevel);
    }

    @Test
    void testKickOnTheLevelHitsLegs() {
        setHexLevels(0, 0);
        assertEquals(ToHitData.HIT_KICK, PhysicalHitTable.resolve(PhysicalAttackType.RIGHT_KICK,
              attacker, attackerState, targetMek, targetState, mockGame));
    }

    @Test
    void testKickFromOneLevelAboveHitsPunchTable() {
        // The QA case: the kicker stands on a hill one level above a standing Mek, so its feet are level
        // with the target's torso and head - the kick resolves on the punch table.
        setHexLevels(1, 0);
        assertEquals(ToHitData.HIT_PUNCH, PhysicalHitTable.resolve(PhysicalAttackType.RIGHT_KICK,
              attacker, attackerState, targetMek, targetState, mockGame));
    }

    @Test
    void testKickFromAboveOnHeightZeroTargetHitsFullBody() {
        Tank targetTank = mock(Tank.class);
        when(targetTank.getHeight()).thenReturn(0);
        setHexLevels(1, 0);
        assertEquals(ToHitData.HIT_NORMAL, PhysicalHitTable.resolve(PhysicalAttackType.RIGHT_KICK,
              attacker, attackerState, targetTank, targetState, mockGame));
    }

    @Test
    void testKickAgainstProneMekHitsFullBody() {
        when(targetState.isProne()).thenReturn(true);
        setHexLevels(1, 0);
        assertEquals(ToHitData.HIT_NORMAL, PhysicalHitTable.resolve(PhysicalAttackType.RIGHT_KICK,
              attacker, attackerState, targetMek, targetState, mockGame));
    }

    @Test
    void testPunchOnTheLevelHitsPunchTable() {
        setHexLevels(0, 0);
        assertEquals(ToHitData.HIT_PUNCH, PhysicalHitTable.resolve(PhysicalAttackType.LEFT_PUNCH,
              attacker, attackerState, targetMek, targetState, mockGame));
    }

    @Test
    void testPunchAtTargetBaseHitsLegs() {
        // The target stands one level above the attacker, so the attacker's arms are level with its feet:
        // the punch resolves on the kick (leg) table.
        setHexLevels(0, 1);
        assertEquals(ToHitData.HIT_KICK, PhysicalHitTable.resolve(PhysicalAttackType.LEFT_PUNCH,
              attacker, attackerState, targetMek, targetState, mockGame));
    }

    @Test
    void testKickFromAboveOnStandingSuperHeavyStillHitsLegs() {
        // A currently-prone superheavy whose projected state stands it up is two levels tall, so a kick
        // from one level above still lands on its legs - not the punch table a normal Mek would offer.
        when(targetMek.isSuperHeavy()).thenReturn(true);
        when(targetMek.isProne()).thenReturn(true);
        when(targetMek.getHeight()).thenReturn(0);
        when(targetState.isProne()).thenReturn(false);
        setHexLevels(1, 0);
        assertEquals(ToHitData.HIT_KICK, PhysicalHitTable.resolve(PhysicalAttackType.RIGHT_KICK,
              attacker, attackerState, targetMek, targetState, mockGame));
    }

    @Test
    void testMissingBoardFallsBackToAttackTypeTable() {
        Game boardlessGame = mock(Game.class);
        assertEquals(ToHitData.HIT_KICK, PhysicalHitTable.resolve(PhysicalAttackType.RIGHT_KICK,
              attacker, attackerState, targetMek, targetState, boardlessGame));
        assertEquals(ToHitData.HIT_PUNCH, PhysicalHitTable.resolve(PhysicalAttackType.LEFT_PUNCH,
              attacker, attackerState, targetMek, targetState, boardlessGame));
    }
}
