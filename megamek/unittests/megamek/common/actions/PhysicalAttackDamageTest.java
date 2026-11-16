/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 */
package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.Hex;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class PhysicalAttackDamageTest {

    private RulesManager originalRulesManager;

    @BeforeEach
    void setUpRulesManager() {
        originalRulesManager = Game.rulesManager;
        Game.rulesManager = new CoreRulesManager();
    }

    @AfterEach
    void restoreRulesManager() {
        Game.rulesManager = originalRulesManager;
    }

    @Test
    void punchDamageUsesTsmAndIsReducedByArmActuators() {
        Mek attacker = createAttacker();

        assertEquals(5, PunchAttackAction.getDamageFor(attacker, PunchAttackAction.LEFT, false, false));

        when(attacker.hasActiveTSM()).thenReturn(true);
        assertEquals(10, PunchAttackAction.getDamageFor(attacker, PunchAttackAction.LEFT, false, false));

        when(attacker.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM)).thenReturn(false);
        assertEquals(5, PunchAttackAction.getDamageFor(attacker, PunchAttackAction.LEFT, false, false));
        when(attacker.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM)).thenReturn(true);

        when(attacker.hasActiveTSM()).thenReturn(false);
        when(attacker.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM)).thenReturn(false);
        assertEquals(2, PunchAttackAction.getDamageFor(attacker, PunchAttackAction.LEFT, false, false));

        when(attacker.hasWorkingSystem(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM)).thenReturn(true);
        when(attacker.hasWorkingSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LEFT_ARM)).thenReturn(false);
        assertEquals(2, PunchAttackAction.getDamageFor(attacker, PunchAttackAction.LEFT, false, false));
    }

    @Test
    void kickDamageIncludesTalonsAndTsm() {
        Mek attacker = createAttacker();

        assertEquals(10, KickAttackAction.getDamageFor(attacker, KickAttackAction.LEFT, false));

        when(attacker.hasWorkingMisc(MiscType.F_TALON, null, Mek.LOC_LEFT_LEG)).thenReturn(true);
        assertEquals(15, KickAttackAction.getDamageFor(attacker, KickAttackAction.LEFT, false));

        when(attacker.hasActiveTSM()).thenReturn(true);
        assertEquals(30, KickAttackAction.getDamageFor(attacker, KickAttackAction.LEFT, false));

        when(attacker.hasWorkingSystem(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_LEFT_LEG)).thenReturn(false);
        assertEquals(15, KickAttackAction.getDamageFor(attacker, KickAttackAction.LEFT, false));
    }

    @Test
    void kickDamageIsReducedByLegActuators() {
        Mek attacker = createAttacker();

        when(attacker.hasWorkingSystem(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_LEFT_LEG)).thenReturn(false);
        assertEquals(5, KickAttackAction.getDamageFor(attacker, KickAttackAction.LEFT, false));

        when(attacker.hasWorkingSystem(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_LEFT_LEG)).thenReturn(true);
        when(attacker.hasWorkingSystem(Mek.ACTUATOR_LOWER_LEG, Mek.LOC_LEFT_LEG)).thenReturn(false);
        assertEquals(5, KickAttackAction.getDamageFor(attacker, KickAttackAction.LEFT, false));
    }

    @Test
    void quadMuleKickIsImpossibleWhenEitherRearLegIsMissingOrHasADamagedHip() {
        for (int leg : List.of(KickAttackAction.LEFT_MULE, KickAttackAction.RIGHT_MULE)) {
            for (int rearLeg : List.of(Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG)) {
                assertImpossibleKick(true, false, leg, rearLeg, -1);
                assertImpossibleKick(true, false, leg, -1, rearLeg);
            }
        }
    }

    @Test
    void quadMuleKickIsImpossibleWhenEitherFrontLegIsMissing() {
        for (int frontLeg : List.of(Mek.LOC_LEFT_ARM, Mek.LOC_RIGHT_ARM)) {
            assertImpossibleKick(true, false, KickAttackAction.LEFT_MULE, frontLeg, -1);
            assertImpossibleKick(true, false, KickAttackAction.RIGHT_MULE, frontLeg, -1);
        }
    }

    @Test
    void tripodKickIsImpossibleWhenCenterLegIsMissing() {
        assertImpossibleKick(false, true, KickAttackAction.LEFT, Mek.LOC_CENTER_LEG, -1);
        assertImpossibleKick(false, true, KickAttackAction.RIGHT, Mek.LOC_CENTER_LEG, -1);
    }

    @Test
    void regularMekKickIsImpossibleWhenEitherLegIsMissing() {
        for (int leg : List.of(KickAttackAction.LEFT, KickAttackAction.RIGHT)) {
            for (int missingLeg : List.of(Mek.LOC_LEFT_LEG, Mek.LOC_RIGHT_LEG)) {
                assertImpossibleKick(false, false, leg, missingLeg, -1);
            }
        }
    }

    private Mek createAttacker() {
        Mek attacker = mock(Mek.class);
        when(attacker.getWeight()).thenReturn(50.0);
        when(attacker.hasWorkingSystem(anyInt(), anyInt())).thenReturn(true);
        when(attacker.hasActiveTSM()).thenReturn(false);
        return attacker;
    }

    private void assertImpossibleKick(boolean quad, boolean tripod, int kick, int missingLocation,
          int damagedHipLocation) {
        Mek attacker = mock(Mek.class);
        when(attacker.getId()).thenReturn(1);
        when(attacker.entityIsQuad()).thenReturn(quad);
        when(attacker.isTripodMek()).thenReturn(tripod);
        when(attacker.isStuck()).thenReturn(false);
        when(attacker.isEvading()).thenReturn(false);
        when(attacker.isClimbing()).thenReturn(false);
        when(attacker.endOfTurnCargoInteraction()).thenReturn(false);
        when(attacker.getPosition()).thenReturn(new Coords(0, 0));
        when(attacker.isLocationBad(anyInt())).thenReturn(false);
        if (missingLocation >= 0) {
            when(attacker.isLocationBad(missingLocation)).thenReturn(true);
        }
        when(attacker.hasWorkingSystem(anyInt(), anyInt())).thenReturn(true);
        if (damagedHipLocation >= 0) {
            when(attacker.hasWorkingSystem(Mek.ACTUATOR_HIP, damagedHipLocation)).thenReturn(false);
        }

        Targetable target = target();
        Game game = mock(Game.class);
        GameOptions options = mock(GameOptions.class);
        Hex attackerHex = hex();
        Hex targetHex = hex();
        when(game.getOptions()).thenReturn(options);
        when(game.onTheSameBoard(attacker, target)).thenReturn(true);
        when(game.getEntity(1)).thenReturn(attacker);
        when(game.getHexOf(attacker)).thenReturn(attackerHex);
        when(game.getHexOf(target)).thenReturn(targetHex);

        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.effectiveDistance(game, attacker, target)).thenReturn(0);
            ToHitData toHit = KickAttackAction.toHit(game, 1, target, kick);

            assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue());
        }
    }

    private Targetable target() {
        Targetable target = mock(Targetable.class);
        when(target.getTargetType()).thenReturn(Targetable.TYPE_BUILDING);
        when(target.getPosition()).thenReturn(new Coords(0, 0));
        when(target.getElevation()).thenReturn(0);
        when(target.getHeight()).thenReturn(1);
        return target;
    }

    private Hex hex() {
        Hex hex = mock(Hex.class);
        when(hex.getLevel()).thenReturn(0);
        return hex;
    }
}
