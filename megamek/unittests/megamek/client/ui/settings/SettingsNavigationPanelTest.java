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
package megamek.client.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLabel;
import javax.swing.JTree;

import org.junit.jupiter.api.Test;

class SettingsNavigationPanelTest {
    private static final SettingsNavigationText TEXT = new SettingsNavigationText(
          "Filter", "Filter settings", "No matches", "%d matches", "Expand", "Collapse");

    @Test
    void filterShowsOnlyMatchingRouteAndStatus() {
        SettingsRoute display = new SettingsRoute("display", List.of("Display"));
        SettingsRoute pools = new SettingsRoute("newDay.pools", List.of("New Day", "Personnel Pools"));
        SettingsNavigationPanel panel = new SettingsNavigationPanel(List.of(display, pools), route -> { }, TEXT);

        panel.setFilterText("pool");

        JTree tree = findComponent(panel, "settingsNavigationTree", JTree.class);
        JLabel status = findComponent(panel, "lblSettingsFilterStatus", JLabel.class);
        assertEquals(2, tree.getRowCount());
        assertEquals("1 matches", status.getText());
        assertTrue(status.isVisible());
    }

    @Test
    void nonEmptyFilterInvokesIndexInitializer() {
        AtomicInteger calls = new AtomicInteger();
        SettingsNavigationPanel panel = new SettingsNavigationPanel(
              List.of(new SettingsRoute("display", List.of("Display"))), route -> { }, TEXT);
        panel.setSearchIndexInitializer(calls::incrementAndGet);

        panel.setFilterText("display");

        assertEquals(1, calls.get());
        assertEquals("display", panel.getActiveFilter());
    }

    @Test
    void blankFilterHidesStatus() {
        SettingsNavigationPanel panel = new SettingsNavigationPanel(
              List.of(new SettingsRoute("display", List.of("Display"))), route -> { }, TEXT);
        panel.setFilterText("missing");
        panel.setFilterText("");

        JLabel status = findComponent(panel, "lblSettingsFilterStatus", JLabel.class);
        assertFalse(status.isVisible());
        assertEquals("", status.getText());
    }

    @Test
    void stablePathIdsKeepDuplicateSiblingLabelsReachable() {
        SettingsRoute first = new SettingsRoute("root.first", List.of("Root", "Same"),
              List.of("root", "root.first"), List.of(), true);
        SettingsRoute second = new SettingsRoute("root.second", List.of("Root", "Same"),
              List.of("root", "root.second"), List.of(), true);
        SettingsNavigationPanel panel = new SettingsNavigationPanel(List.of(first, second), route -> { }, TEXT);
        SettingsRoute equivalentSecond = new SettingsRoute("root.second", List.of("Root", "Same"),
              List.of("root", "root.second"), List.of(), true);

        panel.selectRoute(equivalentSecond);

        JTree tree = findComponent(panel, "settingsNavigationTree", JTree.class);
        assertEquals(3, tree.getRowCount());
        assertNotNull(tree.getSelectionPath());
        assertEquals(2, tree.getLeadSelectionRow());
    }

    private static <T extends Component> T findComponent(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new AssertionError("No " + type.getSimpleName() + " named " + name);
    }

    private static <T extends Component> T findComponentOrNull(Container root, String name, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = findComponentOrNull(container, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
