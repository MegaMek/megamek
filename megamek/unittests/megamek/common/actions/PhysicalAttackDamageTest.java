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
import static org.mockito.Mockito.when;

import megamek.common.equipment.MiscType;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    private Mek createAttacker() {
        Mek attacker = mock(Mek.class);
        when(attacker.getWeight()).thenReturn(50.0);
        when(attacker.hasWorkingSystem(anyInt(), anyInt())).thenReturn(true);
        when(attacker.hasActiveTSM()).thenReturn(false);
        return attacker;
    }
}
