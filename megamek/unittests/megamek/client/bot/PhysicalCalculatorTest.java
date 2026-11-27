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
package megamek.client.bot;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.HonorUtil;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Princess must not make physical attacks on an enemy unit that is crippled under Forced Withdrawal, just as she
 * holds her weapons fire (issue #9761). The honor check used to ask about her own attacking unit instead of the
 * target, so it never stopped a punch, kick or club.
 */
class PhysicalCalculatorTest {
    private static final int BOT_PLAYER_ID = 0;
    private static final int ENEMY_PLAYER_ID = 1;

    private MockedStatic<Compute> computeMock;
    private MockedStatic<PhysicalCalculator> calculatorMock;

    private Game game;
    private BehaviorSettings behaviorSettings;
    private HonorUtil honorUtil;
    private Mek attacker;
    private Mek enemy;

    @BeforeEach
    void setUp() {
        // Every unit counts as adjacent.
        computeMock = mockStatic(Compute.class);
        computeMock.when(() -> Compute.effectiveDistance(any(), any(), any())).thenReturn(1);

        // Stand in for the damage estimate of one physical attack, so any enemy considered gets a punch. Everything
        // else, including the target filtering under test, runs for real.
        calculatorMock = mockStatic(PhysicalCalculator.class, invocation -> {
            if (invocation.getMethod().getName().equals("getBestPhysicalAttack")) {
                Object[] arguments = invocation.getArguments();
                return new PhysicalOption((Entity) arguments[0], (Entity) arguments[1], 10.0,
                      PhysicalOption.PUNCH_LEFT, null);
            }
            return invocation.callRealMethod();
        });

        attacker = mock(Mek.class);
        when(attacker.getId()).thenReturn(1);
        when(attacker.getOwnerId()).thenReturn(BOT_PLAYER_ID);
        when(attacker.getSwarmAttackerId()).thenReturn(Entity.NONE);
        when(attacker.getPosition()).thenReturn(new Coords(5, 5));

        enemy = mock(Mek.class);
        when(enemy.getId()).thenReturn(2);
        when(enemy.getOwnerId()).thenReturn(ENEMY_PLAYER_ID);
        when(enemy.isEnemyOf(attacker)).thenReturn(true);
        when(enemy.getPosition()).thenReturn(new Coords(5, 6));
        when(enemy.getDisplayName()).thenReturn("BattleMaster BLR-1G");

        game = mock(Game.class);
        when(game.getEntitiesVector()).thenReturn(List.of(attacker, enemy));

        behaviorSettings = mock(BehaviorSettings.class);
        when(behaviorSettings.isForcedWithdrawal()).thenReturn(true);
        when(behaviorSettings.getIgnoredUnitTargets()).thenReturn(new HashSet<>());

        honorUtil = new HonorUtil();
    }

    @AfterEach
    void tearDown() {
        calculatorMock.close();
        computeMock.close();
    }

    @Test
    void crippledEnemyIsNotAttacked() {
        when(enemy.isCrippled()).thenReturn(true);
        honorUtil.checkEnemyBroken(enemy, true);

        PhysicalOption option = PhysicalCalculator.getBestPhysical(attacker, game, behaviorSettings, honorUtil);

        assertNull(option.target, "A crippled enemy must not be struck while its owner keeps their honor");
    }

    @Test
    void intactEnemyIsAttacked() {
        honorUtil.checkEnemyBroken(enemy, true);

        PhysicalOption option = PhysicalCalculator.getBestPhysical(attacker, game, behaviorSettings, honorUtil);

        assertSame(enemy, option.target);
    }

    @Test
    void crippledEnemyOfDishonoredPlayerIsAttacked() {
        when(enemy.isCrippled()).thenReturn(true);
        honorUtil.checkEnemyBroken(enemy, true);
        honorUtil.setEnemyDishonored(ENEMY_PLAYER_ID);

        PhysicalOption option = PhysicalCalculator.getBestPhysical(attacker, game, behaviorSettings, honorUtil);

        assertSame(enemy, option.target, "A player who broke the honor rules loses the protection");
    }
}
