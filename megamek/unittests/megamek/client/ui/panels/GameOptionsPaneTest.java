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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Test;

class GameOptionsPaneTest {

    @Test
    void searchFiltersRowsByOptionNameAndDescription() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel searchlights = component(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            DialogOptionComponentYPanel pushOffBoard = component(
                  options.getOption(OptionsConstants.BASE_PUSH_OFF_BOARD));
            GameOptionsPane pane = pane(List.of(searchlights, pushOffBoard), option -> true);

            pane.setFilterText(searchlights.getOption().getDisplayableName());

            assertTrue(searchlights.isVisible());
            assertFalse(pushOffBoard.isVisible());
        });
    }

    @Test
    void groupNameSearchKeepsGroupRowsVisible() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel searchlights = component(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            GameOptionsPane pane = pane(List.of(searchlights), option -> true);

            pane.setFilterText("basic");

            assertTrue(searchlights.isVisible());
        });
    }

    @Test
    void refreshingVisibilityHidesExcludedOption() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel searchlights = component(options.getOption(OptionsConstants.SEARCHLIGHTS_ON));
            AtomicBoolean showSearchlights = new AtomicBoolean(true);
            GameOptionsPane pane = pane(List.of(searchlights),
                  option -> !option.getName().equals(OptionsConstants.SEARCHLIGHTS_ON) || showSearchlights.get());

            showSearchlights.set(false);
            pane.refreshVisibility();

            assertFalse(searchlights.isVisible());
        });
    }

    private static GameOptionsPane pane(List<DialogOptionComponentYPanel> components,
          java.util.function.Predicate<IOption> visibility) {
        return new GameOptionsPane(List.of(new GameOptionsPane.OptionGroup("basic", "Basic", components)),
              visibility);
    }

    private static DialogOptionComponentYPanel component(IOption option) {
        return new DialogOptionComponentYPanel(new DialogOptionListener() {
            @Override
            public void optionClicked(DialogOptionComponentYPanel component, IOption changedOption, boolean state) {
            }

            @Override
            public void optionSwitched(DialogOptionComponentYPanel component, IOption changedOption, int index) {
            }
        }, option, true, true);
    }

    private static void runOnEdt(Runnable test) throws Exception {
        try {
            SwingUtilities.invokeAndWait(test);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof Error error) {
                throw error;
            }
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }
}