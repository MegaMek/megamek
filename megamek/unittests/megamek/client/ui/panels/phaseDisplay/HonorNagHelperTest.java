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
package megamek.client.ui.panels.phaseDisplay;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.Player;
import megamek.common.actions.*;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link HonorNagHelper}, which mirrors, on the human client, the honor rules a Princess bot applies in
 * {@code Princess.checkForDishonoredEnemies} so the player can be warned before an action that would newly dishonor
 * them.
 */
class HonorNagHelperTest {

    private static final int ATTACKER_OWNER_ID = 1;
    private static final int BOT_OWNER_ID = 2;

    private Game game;
    private Player attackerOwner;
    private Player botOwner;
    private Entity attacker;
    private Entity target;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);

        attackerOwner = mock(Player.class);
        when(attackerOwner.getId()).thenReturn(ATTACKER_OWNER_ID);

        botOwner = mock(Player.class);
        when(botOwner.getId()).thenReturn(BOT_OWNER_ID);
        when(botOwner.isBot()).thenReturn(true);
        when(botOwner.isEnemyOf(attackerOwner)).thenReturn(true);

        // Default scenario: a healthy military unit attacks a healthy military enemy bot unit. On its own this does not
        // dishonor anyone; individual tests flip one condition at a time.
        attacker = mock(Entity.class);
        when(attacker.getOwner()).thenReturn(attackerOwner);
        when(attacker.isCrippled()).thenReturn(false);
        when(attacker.isMilitary()).thenReturn(true);

        target = mock(Entity.class);
        when(target.getOwner()).thenReturn(botOwner);
        when(target.isCrippled()).thenReturn(false);
        when(target.isMilitary()).thenReturn(true);

        // No bot has flagged the attacker as dishonored yet.
        when(game.isPlayerDishonoredBy(BOT_OWNER_ID, ATTACKER_OWNER_ID)).thenReturn(false);
    }

    @Test
    void healthyMilitaryAttackerVersusHealthyEnemyDoesNotDishonor() {
        assertFalse(HonorNagHelper.wouldBeDishonored(game, attacker, target));
    }

    @Test
    void crippledAttackerFightingOnDishonors() {
        when(attacker.isCrippled()).thenReturn(true);
        assertTrue(HonorNagHelper.wouldBeDishonored(game, attacker, target));
    }

    @Test
    void civilianAttackerDishonors() {
        when(attacker.isMilitary()).thenReturn(false);
        assertTrue(HonorNagHelper.wouldBeDishonored(game, attacker, target));
    }

    @Test
    void attackingBrokenEnemyDishonors() {
        when(target.isCrippled()).thenReturn(true);
        assertTrue(HonorNagHelper.wouldBeDishonored(game, attacker, target));
    }

    @Test
    void attackingNonBotEnemyNeverDishonors() {
        when(botOwner.isBot()).thenReturn(false);
        // Even with a dishonoring condition present, a non-bot opponent tracks no honor.
        when(attacker.isCrippled()).thenReturn(true);
        assertFalse(HonorNagHelper.wouldBeDishonored(game, attacker, target));
    }

    @Test
    void attackingFriendlyBotNeverDishonors() {
        when(botOwner.isEnemyOf(attackerOwner)).thenReturn(false);
        when(attacker.isCrippled()).thenReturn(true);
        assertFalse(HonorNagHelper.wouldBeDishonored(game, attacker, target));
    }

    @Test
    void alreadyDishonoredIsNotWarnedAgain() {
        when(attacker.isCrippled()).thenReturn(true);
        // The bot already holds a grudge, so committing this attack changes nothing.
        when(game.isPlayerDishonoredBy(BOT_OWNER_ID, ATTACKER_OWNER_ID)).thenReturn(true);
        assertFalse(HonorNagHelper.wouldBeDishonored(game, attacker, target));
    }

    @Test
    void recordDishonorMarksTargetBotAsHoldingGrudge() {
        when(attacker.isCrippled()).thenReturn(true);
        HonorNagHelper.recordDishonor(game, attacker, target);
        // Optimistically flag the player so this turn's later attacks are not re-warned.
        verify(game).addDishonoredPlayer(BOT_OWNER_ID, ATTACKER_OWNER_ID);
    }

    @Test
    void recordDishonorDoesNothingForLegitimateAttack() {
        // Healthy military attacker versus a healthy enemy: nothing to record.
        HonorNagHelper.recordDishonor(game, attacker, target);
        verify(game, never()).addDishonoredPlayer(anyInt(), anyInt());
    }

    @Test
    void recordDishonorFromListMarksTargetBot() {
        when(attacker.isCrippled()).thenReturn(true);
        WeaponAttackAction weaponAttack = mock(WeaponAttackAction.class);
        when(weaponAttack.getEntity(game)).thenReturn(attacker);
        when(weaponAttack.getTarget(game)).thenReturn(target);

        HonorNagHelper.recordDishonor(game, List.of((EntityAction) weaponAttack));
        verify(game).addDishonoredPlayer(BOT_OWNER_ID, ATTACKER_OWNER_ID);
    }

    @Test
    void nullAttackerDoesNotDishonor() {
        assertFalse(HonorNagHelper.wouldBeDishonored(game, null, target));
    }

    @Test
    void nonEntityTargetDoesNotDishonor() {
        Targetable nonEntityTarget = mock(Targetable.class);
        when(attacker.isCrippled()).thenReturn(true);
        assertFalse(HonorNagHelper.wouldBeDishonored(game, attacker, nonEntityTarget));
    }

    @Test
    void weaponAttackInListIsEvaluated() {
        when(attacker.isCrippled()).thenReturn(true);
        WeaponAttackAction weaponAttack = mock(WeaponAttackAction.class);
        when(weaponAttack.getEntity(game)).thenReturn(attacker);
        when(weaponAttack.getTarget(game)).thenReturn(target);

        assertTrue(HonorNagHelper.wouldBeDishonored(game, List.of((EntityAction) weaponAttack)));
    }

    @Test
    void physicalAttackInListIsEvaluated() {
        when(target.isCrippled()).thenReturn(true);
        PhysicalAttackAction physicalAttack = mock(PhysicalAttackAction.class);
        when(physicalAttack.getEntity(game)).thenReturn(attacker);
        when(physicalAttack.getTarget(game)).thenReturn(target);

        assertTrue(HonorNagHelper.wouldBeDishonored(game, List.of((EntityAction) physicalAttack)));
    }

    @ParameterizedTest
    @ValueSource(classes = { PushAttackAction.class, ThrashAttackAction.class, ProtoMekPhysicalAttackAction.class,
                             BAVibroClawAttackAction.class })
    void offensivePhysicalAttacksExtendingAbstractAttackActionAreEvaluated(
          Class<? extends AbstractAttackAction> attackType) {
        // These offensive physical attacks extend AbstractAttackAction directly, not PhysicalAttackAction, and must
        // still be nagged - Thrash / ProtoMek-physical / BA vibroclaw are exactly the light-unit attacks the feature
        // targets.
        when(attacker.isCrippled()).thenReturn(true);
        AbstractAttackAction attack = mock(attackType);
        when(attack.getEntity(game)).thenReturn(attacker);
        when(attack.getTarget(game)).thenReturn(target);

        assertTrue(HonorNagHelper.wouldBeDishonored(game, List.of((EntityAction) attack)));
    }

    @Test
    void nonDamagingSearchlightIsIgnored() {
        // Searchlight targets an entity but deals no damage; the bot never treats it as an attack, so it must not nag
        // even while the attacker is crippled.
        when(attacker.isCrippled()).thenReturn(true);
        SearchlightAttackAction searchlight = mock(SearchlightAttackAction.class);

        assertFalse(HonorNagHelper.wouldBeDishonored(game, List.of((EntityAction) searchlight)));
    }

    @Test
    void nonAttackActionsInListAreIgnored() {
        // A torso twist is not an attack and must never trigger the warning, even mid-crisis.
        when(attacker.isCrippled()).thenReturn(true);
        TorsoTwistAction torsoTwist = mock(TorsoTwistAction.class);

        assertFalse(HonorNagHelper.wouldBeDishonored(game, List.of((EntityAction) torsoTwist)));
    }

    @Test
    void listWithoutDishonoringAttackIsClean() {
        WeaponAttackAction weaponAttack = mock(WeaponAttackAction.class);
        when(weaponAttack.getEntity(game)).thenReturn(attacker);
        when(weaponAttack.getTarget(game)).thenReturn(target);

        // Healthy military attacker versus healthy enemy: a legitimate shot, no warning.
        assertFalse(HonorNagHelper.wouldBeDishonored(game, List.of((EntityAction) weaponAttack)));
    }
}
