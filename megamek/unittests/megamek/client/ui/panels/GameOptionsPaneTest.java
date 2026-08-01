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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.settings.CollapsibleSectionPanel;
import megamek.client.ui.settings.SettingsNavigationPanel;
import megamek.client.ui.settings.SettingsPagePanel;
import megamek.client.ui.util.UIUtil;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
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

    @Test
    void basicOptionsUseClassifiedCollapsedSectionsAndStandardSize() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel playtest = component(options.getOption(OptionsConstants.PLAYTEST_1));
            DialogOptionComponentYPanel lobby = component(options.getOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP));
            GameOptionsPane pane = pane(List.of(playtest, lobby), option -> true);

            List<CollapsibleSectionPanel> sections = findSections(pane);
            assertEquals(2, sections.size());
            assertFalse(sections.get(0).isExpanded());
            assertFalse(sections.get(1).isExpanded());
            assertEquals(3, GameOptionsPane.legendEntries().size());
            assertTrue(pane.getPreferredSize().width >= UIUtil.scaleForGUI(
                  SettingsNavigationPanel.DEFAULT_NAVIGATION_WIDTH + SettingsPagePanel.DEFAULT_MAXIMUM_PAGE_WIDTH));
            assertTrue(pane.getPreferredSize().height >= UIUtil.scaleForGUI(800));
        });
    }

    @Test
    void searchExpandsOnlySectionContainingMatchingOption() throws Exception {
        runOnEdt(() -> {
            GameOptions options = new GameOptions();
            DialogOptionComponentYPanel playtest = component(options.getOption(OptionsConstants.PLAYTEST_1));
            DialogOptionComponentYPanel lobby = component(options.getOption(OptionsConstants.BASE_LOBBY_AMMO_DUMP));
            GameOptionsPane pane = pane(List.of(playtest, lobby), option -> true);

            pane.setFilterText(playtest.getOption().getDisplayableName());

            List<CollapsibleSectionPanel> sections = findSections(pane);
            assertTrue(sections.get(0).isExpanded());
            assertFalse(sections.get(1).isExpanded());
            assertTrue(playtest.isVisible());
            assertFalse(lobby.isVisible());
        });
    }

    @Test
    void everyGameOptionResolvesToLocalizedSectionText() {
        GameOptions options = new GameOptions();
        int optionCount = 0;
        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                String sectionId = GameOptionsPane.sectionId(group.getName(), option.getName());
                assertTrue(Messages.keyExists("GameOptionsDialog.section." + sectionId + ".title"), sectionId);
                assertTrue(Messages.keyExists("GameOptionsDialog.section." + sectionId + ".summary"), sectionId);
                optionCount++;
            }
        }
        assertTrue(optionCount > 200, "Expected all game options to be classified");
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

    private static List<CollapsibleSectionPanel> findSections(Container root) {
        List<CollapsibleSectionPanel> sections = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof CollapsibleSectionPanel section) {
                sections.add(section);
            }
            if (child instanceof Container container) {
                sections.addAll(findSections(container));
            }
        }
        return sections;
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