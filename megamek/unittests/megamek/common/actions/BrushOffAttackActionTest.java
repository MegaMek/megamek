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
package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.ToHitData;
import megamek.common.equipment.INarcPod;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Brushing off an attached iNarc pod is a Total Warfare rule. Under Core rules a pod stays until the location it is
 * attached to is destroyed (Core p.188, p.193).
 */
class BrushOffAttackActionTest {
    private static final int ATTACKER_ID = 1;
    private static final int PILOTING = 5;

    private Game game;
    private INarcPod homingPod;

    @BeforeEach
    void setUp() {
        Crew crew = mock(Crew.class);
        PilotOptions pilotOptions = mock(PilotOptions.class);
        when(pilotOptions.stringOption(anyString())).thenReturn(Crew.ENVIRONMENT_SPECIALIST_NONE);
        when(crew.getOptions()).thenReturn(pilotOptions);
        when(crew.getPiloting()).thenReturn(PILOTING);

        Mek attacker = mock(BipedMek.class);
        when(attacker.getId()).thenReturn(ATTACKER_ID);
        when(attacker.getCrew()).thenReturn(crew);
        when(attacker.getSwarmAttackerId()).thenReturn(Mek.NONE);
        when(attacker.hasWorkingSystem(anyInt(), anyInt())).thenReturn(true);
        when(attacker.hasSystem(anyInt(), anyInt())).thenReturn(true);
        when(attacker.getCockpitType()).thenReturn(Mek.COCKPIT_STANDARD);

        game = mock(Game.class);
        when(game.getEntity(ATTACKER_ID)).thenReturn(attacker);

        homingPod = new INarcPod(2, INarcPod.HOMING, Mek.LOC_LEFT_TORSO);
    }

    @AfterEach
    void restoreDefaultRules() {
        new Game().initializeRulesManager(OptionsConstants.RULES_CORE);
    }

    @Test
    void coreRulesForbidBrushingOffINarcPod() {
        new Game().initializeRulesManager(OptionsConstants.RULES_CORE);

        ToHitData leftArm = BrushOffAttackAction.toHit(game, ATTACKER_ID, homingPod, BrushOffAttackAction.LEFT);
        ToHitData rightArm = BrushOffAttackAction.toHit(game, ATTACKER_ID, homingPod, BrushOffAttackAction.RIGHT);

        assertEquals(TargetRoll.IMPOSSIBLE, leftArm.getValue(), leftArm.getDesc());
        assertEquals(TargetRoll.IMPOSSIBLE, rightArm.getValue(), rightArm.getDesc());
    }

    @Test
    void totalWarfareRulesAllowBrushingOffINarcPod() {
        new Game().initializeRulesManager(OptionsConstants.RULES_TW);

        ToHitData leftArm = BrushOffAttackAction.toHit(game, ATTACKER_ID, homingPod, BrushOffAttackAction.LEFT);

        assertNotEquals(TargetRoll.IMPOSSIBLE, leftArm.getValue(), leftArm.getDesc());
        // Piloting 5 plus the +4 brush-off modifier
        assertEquals(PILOTING + 4, leftArm.getValue(), leftArm.getDesc());
    }
}
