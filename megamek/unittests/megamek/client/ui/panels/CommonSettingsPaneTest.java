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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Optional;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;

class CommonSettingsPaneTest {

    @Test
    void searchIndexesNestedLabelsAndTooltips() throws Exception {
        runOnEdt(() -> {
            JPanel main = new JPanel();
            main.add(new JLabel("Display scale"));
            JPanel audio = new JPanel();
            JCheckBox mute = new JCheckBox("Mute notifications");
            mute.setToolTipText("Silence chat alerts");
            audio.add(mute);
            CommonSettingsPane pane = pane(main, audio);

            pane.setFilterText("chat alerts");

            assertEquals("Audio", selectedTreeLabel(pane));
        });
    }

    @Test
    void searchIndexesListEntries() throws Exception {
        runOnEdt(() -> {
            JPanel main = new JPanel();
            main.add(new JList<>(new String[] { "General option", "Experimental pathfinder" }));
            CommonSettingsPane pane = pane(main, new JPanel());

            pane.setFilterText("pathfinder");

            assertEquals("Main", selectedTreeLabel(pane));
        });
    }

    private static CommonSettingsPane pane(JPanel main, JPanel audio) {
        return new CommonSettingsPane(List.of(
              new CommonSettingsPane.OptionPage("main", "Main", main),
              new CommonSettingsPane.OptionPage("audio", "Audio", audio)));
    }

    private static String selectedTreeLabel(Container root) {
        JTree tree = findComponent(root, "settingsNavigationTree", JTree.class);
        assertEquals(1, tree.getRowCount());
        TreePath path = tree.getPathForRow(0);
        return path.getLastPathComponent().toString();
    }

    private static <T extends Component> T findComponent(Container root, String name, Class<T> type) {
        return findComponentOptional(root, name, type)
              .orElseThrow(() -> new AssertionError("No " + type.getSimpleName() + " named " + name));
    }

    private static <T extends Component> Optional<T> findComponentOptional(Container root, String name,
          Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) {
                return Optional.of(type.cast(child));
            }
            if (child instanceof Container container) {
                Optional<T> result = findComponentOptional(container, name, type);
                if (result.isPresent()) {
                    return result;
                }
            }
        }
        return Optional.empty();
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