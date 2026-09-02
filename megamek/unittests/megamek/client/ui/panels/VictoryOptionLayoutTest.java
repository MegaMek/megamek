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
package megamek.client.ui.panels;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;

/**
 * The victory option rules shared by the lobby's Victory Conditions dialog and the game options pane: which
 * numbers are bounded, and which options are greyed until their master switch is on.
 */
class VictoryOptionLayoutTest {

    @Test
    void theVictoryPointThresholdsStartAtZeroBecauseZeroSwitchesThemOff() {
        assertArrayEquals(new int[] { 0, VictoryOptionLayout.MAX_COUNT },
              VictoryOptionLayout.boundsFor(OptionsConstants.VICTORY_VP_WIN_THRESHOLD));
        assertArrayEquals(new int[] { 1, VictoryOptionLayout.MAX_COUNT },
              VictoryOptionLayout.boundsFor(OptionsConstants.VICTORY_GAME_TURN_LIMIT));
        assertNull(VictoryOptionLayout.boundsFor(OptionsConstants.VICTORY_USE_OBJECTIVES), "not a number");
    }

    @Test
    void everyVictoryPointSettingHangsOffUseObjectives() {
        for (String optionName : List.of(OptionsConstants.VICTORY_VP_WIN_THRESHOLD,
              OptionsConstants.VICTORY_VP_LOSS_THRESHOLD, OptionsConstants.VICTORY_VP_SUDDEN_DEATH)) {
            assertTrue(VictoryOptionLayout.isDependent(optionName), optionName);
        }
        assertFalse(VictoryOptionLayout.isDependent(OptionsConstants.VICTORY_USE_OBJECTIVES), "the master itself");
    }

    @Test
    void applyingTheRulesGreysADependentUntilItsMasterIsTicked() {
        GameOptions options = new GameOptions();
        DialogOptionComponentYPanel useObjectives = component(
              options.getOption(OptionsConstants.VICTORY_USE_OBJECTIVES));
        DialogOptionComponentYPanel suddenDeath = component(
              options.getOption(OptionsConstants.VICTORY_VP_SUDDEN_DEATH));
        DialogOptionComponentYPanel winThreshold = component(
              options.getOption(OptionsConstants.VICTORY_VP_WIN_THRESHOLD));

        VictoryOptionLayout.apply(List.of(useObjectives, suddenDeath, winThreshold));

        assertFalse(suddenDeath.getEditable());
        assertFalse(winThreshold.getEditable());
        useObjectives.settingsCheckBox().setSelected(true);
        assertTrue(suddenDeath.getEditable());
        assertTrue(winThreshold.getEditable());
    }

    @Test
    void aDependentWhoseMasterIsNotOnDisplayIsLeftAlone() {
        // a caller showing one option on its own must not have it greyed for a switch nobody can see
        GameOptions options = new GameOptions();
        DialogOptionComponentYPanel suddenDeath = component(
              options.getOption(OptionsConstants.VICTORY_VP_SUDDEN_DEATH));

        VictoryOptionLayout.apply(List.of(suddenDeath));

        assertTrue(suddenDeath.getEditable());
    }

    private static DialogOptionComponentYPanel component(IOption option) {
        return new DialogOptionComponentYPanel(new DialogOptionListener() {
            @Override
            public void optionClicked(DialogOptionComponentYPanel component, IOption changedOption, boolean state) {
            }

            @Override
            public void optionSwitched(DialogOptionComponentYPanel component, IOption changedOption, int index) {
            }
        }, option, true);
    }
}
