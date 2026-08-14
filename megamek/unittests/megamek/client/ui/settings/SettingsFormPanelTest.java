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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;

class SettingsFormPanelTest {
    @Test
    void addRowPlacesLabelLeftAndStretchedControlRight() {
        SettingsFormPanel panel = new SettingsFormPanel("Test", 300, 220);

        panel.addRow(new JLabel("Label"), new JLabel("Control"));

        assertEquals(2, panel.getComponentCount());
        assertEquals(0, constraintsFor(panel, 0).gridx);
        GridBagConstraints control = constraintsFor(panel, 1);
        assertEquals(1, control.gridx);
        assertEquals(GridBagConstraints.REMAINDER, control.gridwidth);
        assertEquals(1.0, control.weightx);
        assertEquals(GridBagConstraints.HORIZONTAL, control.fill);
    }

    @Test
    void addRowAppliesConfiguredMinimumControlWidth() {
        SettingsFormPanel panel = new SettingsFormPanel("Test", 0, 200);
        JComponent control = new JLabel();

        panel.addRow(new JLabel("Label"), control);

        assertEquals(UIUtil.scaleForGUI(200), control.getMinimumSize().width);
    }

    @Test
    void addRowAssociatesSwingLabelWithControl() {
        SettingsFormPanel panel = new SettingsFormPanel("Test");
        JLabel label = new JLabel("Label");
        JComponent control = new JLabel("Control");

        panel.addRow(label, control);

        assertSame(control, label.getLabelFor());
    }

    @Test
    void addRowGridAssociatesEverySwingLabelWithItsControl() {
        SettingsFormPanel panel = new SettingsFormPanel("Test");
        JLabel firstLabel = new JLabel("First");
        JLabel firstControl = new JLabel("First control");
        JLabel secondLabel = new JLabel("Second");
        JLabel secondControl = new JLabel("Second control");

        panel.addRowGrid(2, firstLabel, firstControl, secondLabel, secondControl);

        assertSame(firstControl, firstLabel.getLabelFor());
        assertSame(secondControl, secondLabel.getLabelFor());
    }

    @Test
    void addRowGridUsesScaledRowAndColumnSpacing() {
        SettingsFormPanel panel = new SettingsFormPanel("Test");

        panel.addRowGrid(2, new JLabel("First"), new JLabel("First control"),
              new JLabel("Second"), new JLabel("Second control"));

        GridBagConstraints firstLabel = constraintsFor(panel, 0);
        assertEquals(UIUtil.scaleForGUI(5), firstLabel.insets.top);
        assertEquals(UIUtil.scaleForGUI(5), firstLabel.insets.bottom);
        assertEquals(UIUtil.scaleForGUI(12), firstLabel.insets.right);
        assertEquals(UIUtil.scaleForGUI(SettingsFormPanel.GRID_COLUMN_GAP),
              constraintsFor(panel, 1).insets.right);
    }

    @Test
    void addCheckBoxSpansBothColumnsAndAddsTrailingFiller() {
        SettingsFormPanel panel = new SettingsFormPanel("Test", 300, 220);

        panel.addCheckBox(new JCheckBox("Toggle"));

        assertEquals(2, panel.getComponentCount());
        assertEquals(2, constraintsFor(panel, 0).gridwidth);
        assertEquals(2, constraintsFor(panel, 1).gridx);
    }

    @Test
    void mixedRowsShareOneRowCounter() {
        SettingsFormPanel panel = new SettingsFormPanel("Test", 300, 220);
        panel.addCheckBoxGrid(2, new JCheckBox("a"), new JCheckBox("b"), new JCheckBox("c"),
              new JCheckBox("d"));
        JLabel below = new JLabel("Below");

        panel.addRow(below, new JLabel("Control"));

        assertEquals(2, constraintsFor(panel, 6).gridy);
    }

    @Test
    void singleColumnGridFallsBackToFullWidthRows() {
        SettingsFormPanel panel = new SettingsFormPanel("Test");

        panel.addComponentGrid(1, new JLabel("first"), new JLabel("second"));

        assertEquals(2, panel.getComponentCount());
        assertEquals(0, constraintsFor(panel, 0).gridy);
        assertEquals(2, constraintsFor(panel, 0).gridwidth);
        assertEquals(1, constraintsFor(panel, 1).gridy);
        assertEquals(2, constraintsFor(panel, 1).gridwidth);
    }

    @Test
    void equalWidthComponentGridUsesConfiguredCellWidth() {
        SettingsFormPanel panel = new SettingsFormPanel("Test", 300);
        JLabel shortComponent = new JLabel("Short");
        JLabel longComponent = new JLabel("A much longer component");

        panel.addEqualWidthComponentGrid(2, shortComponent, longComponent);
        panel.setSize(panel.getPreferredSize());
        panel.doLayout();

        assertEquals(longComponent.getPreferredSize().width, shortComponent.getPreferredSize().width);
        assertEquals(UIUtil.scaleForGUI(300), shortComponent.getPreferredSize().width);
        assertEquals(longComponent.getWidth(), shortComponent.getWidth());
    }

    @Test
    void equalWidthComponentGridsAlignAcrossPanelsWithDifferentContent() {
        SettingsFormPanel longContentPanel = new SettingsFormPanel("Long", 300);
        JLabel longFirst = new JLabel("A very long option that determines this section's preferred width");
        JLabel longSecond = new JLabel("Second");
        longContentPanel.addEqualWidthComponentGrid(2, longFirst, longSecond);

        SettingsFormPanel shortContentPanel = new SettingsFormPanel("Short", 300);
        JLabel shortFirst = new JLabel("First");
        JLabel shortSecond = new JLabel("Second");
        JLabel finalRow = new JLabel("Final row");
        shortContentPanel.addEqualWidthComponentGrid(2, shortFirst, shortSecond, finalRow);

        int sharedWidth = UIUtil.scaleForGUI(900);
        layoutAtWidth(longContentPanel, sharedWidth);
        layoutAtWidth(shortContentPanel, sharedWidth);

        assertEquals(UIUtil.scaleForGUI(300), longFirst.getPreferredSize().width);
        assertEquals(longContentPanel.getPreferredSize().width, shortContentPanel.getPreferredSize().width);
        assertEquals(longFirst.getWidth(), longSecond.getWidth());
        assertEquals(shortFirst.getWidth(), shortSecond.getWidth());
        assertEquals(longSecond.getX(), shortSecond.getX());
        assertEquals(shortFirst.getWidth(), finalRow.getWidth());
    }

    private static void layoutAtWidth(SettingsFormPanel panel, int width) {
        panel.setSize(width, panel.getPreferredSize().height);
        panel.doLayout();
    }

    private static GridBagConstraints constraintsFor(SettingsFormPanel panel, int index) {
        GridBagLayout layout = (GridBagLayout) panel.getLayout();
        return layout.getConstraints(panel.getComponent(index));
    }
}
