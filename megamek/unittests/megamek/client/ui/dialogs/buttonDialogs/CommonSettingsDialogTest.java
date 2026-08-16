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
package megamek.client.ui.dialogs.buttonDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import megamek.client.ui.buttons.ColourSelectorButton;
import megamek.client.ui.panels.CommonSettingsPane;
import org.junit.jupiter.api.Test;

class CommonSettingsDialogTest {

    @Test
    void rendersCheckBoxesAndColourButtonsInTwoColumnGrids() {
        JCheckBox firstCheckBox = new JCheckBox("First");
        JCheckBox secondCheckBox = new JCheckBox("Second");
        JCheckBox thirdCheckBox = new JCheckBox("Third");
        JLabel label = new JLabel("Label");
        JTextField field = new JTextField();
        ColourSelectorButton shortButton = new ColourSelectorButton("Short");
        ColourSelectorButton longButton = new ColourSelectorButton("A much longer colour label");
        ColourSelectorButton thirdButton = new ColourSelectorButton("Third");

        JPanel content = CommonSettingsDialog.createSettingsPanel(List.of(
              row(firstCheckBox), row(secondCheckBox), row(thirdCheckBox), row(label, field),
              row(shortButton, longButton), row(thirdButton)));
        JPanel form = (JPanel) content.getComponent(0);
        JPanel colourGrid = findNamedPanel(form, "pnlCommonSettingsColourGrid");

        assertCell(form, firstCheckBox, 0, 0);
        assertCell(form, secondCheckBox, 1, 0);
        assertCell(form, thirdCheckBox, 0, 1);
        assertCell(colourGrid, shortButton, 0, 0);
        assertCell(colourGrid, longButton, 1, 0);
        assertCell(colourGrid, thirdButton, 0, 1);
        assertEquals(shortButton.getPreferredSize().width, longButton.getPreferredSize().width);
        assertEquals(shortButton.getPreferredSize().width, thirdButton.getPreferredSize().width);
        assertEquals(SwingConstants.LEFT, shortButton.getHorizontalAlignment());
        assertEquals(SwingConstants.LEFT, longButton.getHorizontalAlignment());
        assertEquals(SwingConstants.LEFT, thirdButton.getHorizontalAlignment());
    }

    @Test
    void keepsCustomRowsFullWidth() {
        JButton customButton = new JButton("Custom");

        JPanel content = CommonSettingsDialog.createSettingsPanel(List.of(row(customButton)));
        JPanel form = (JPanel) content.getComponent(0);
        Component customRow = findDirectParent(form, customButton);
        GridBagConstraints constraints = ((GridBagLayout) form.getLayout()).getConstraints(customRow);

        assertEquals(0, constraints.gridx);
        assertEquals(2, constraints.gridwidth);
        assertEquals(GridBagConstraints.HORIZONTAL, constraints.fill);
    }

    @Test
    void leavesNestedColourButtonWidthsIndependent() {
        ColourSelectorButton nestedButton = new ColourSelectorButton("");
        JPanel nestedPanel = CommonSettingsDialog.createSettingsPanel(List.of(row(nestedButton)));
        int nestedWidth = nestedButton.getPreferredSize().width;

        ColourSelectorButton wideButton = new ColourSelectorButton("Wide");
        Dimension wideSize = wideButton.getPreferredSize();
        wideButton.setPreferredSize(new Dimension(nestedWidth + 100, wideSize.height));

        CommonSettingsDialog.createSettingsPanel(List.of(row(wideButton), row(nestedPanel)));

        assertEquals(nestedWidth, nestedButton.getPreferredSize().width);
    }

    @Test
    void rejectsUnmappedSettingsGroups() {
        JPanel firstGroup = new JPanel();
        JPanel omittedGroup = new JPanel();
        CommonSettingsPane.SectionedContent content = new CommonSettingsPane.SectionedContent(
              List.of(firstGroup, omittedGroup));
        CommonSettingsPane.OptionPage incompletePage = new CommonSettingsPane.OptionPage(
              "test", List.of("Test"), "Test", List.of(new CommonSettingsPane.OptionSection(
                    "first", "First", "First group", firstGroup, false)));

        assertThrows(IllegalArgumentException.class,
              () -> CommonSettingsDialog.addMappedPages(new ArrayList<>(), "test", content, incompletePage));
    }

    private static List<Component> row(Component... components) {
        return List.of(components);
    }

    private static void assertCell(JPanel panel, Component component, int column, int row) {
        GridBagConstraints constraints = ((GridBagLayout) panel.getLayout()).getConstraints(component);
        assertEquals(column, constraints.gridx);
        assertEquals(row, constraints.gridy);
    }

    private static JPanel findNamedPanel(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JPanel panel && name.equals(panel.getName())) {
                return panel;
            }
        }
        throw new AssertionError("No panel named " + name);
    }

    private static Component findDirectParent(Container root, Component target) {
        for (Component child : root.getComponents()) {
            if (child instanceof Container container && contains(container, target)) {
                assertSame(root, child.getParent());
                return child;
            }
        }
        throw new AssertionError("No direct parent for " + target);
    }

    private static boolean contains(Container root, Component target) {
        for (Component child : root.getComponents()) {
            if (child == target || child instanceof Container container && contains(container, target)) {
                return true;
            }
        }
        return false;
    }
}
