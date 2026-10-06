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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;
import org.junit.jupiter.api.Test;

class SettingsPairedFieldGridPanelTest {
    @Test
    void constructorRejectsFewerThanOneColumn() {
        assertThrows(IllegalArgumentException.class,
              () -> new SettingsPairedFieldGridPanel("Test", 300, 300, 100, 0));
    }

    @Test
    void addPairsRejectsMismatchedArrayLengths() {
        SettingsPairedFieldGridPanel panel = new SettingsPairedFieldGridPanel("Test", 300, 300, 100, 2);

        assertThrows(IllegalArgumentException.class, () -> panel.addPairs(labels(2), controls(1)));
    }

    @Test
    void addPairsLaysOutPairsRowMajorWithTrailingFiller() {
        SettingsPairedFieldGridPanel panel = new SettingsPairedFieldGridPanel("Test", 300, 300, 100, 2);

        panel.addPairs(labels(3), controls(3));

        assertEquals(4, panel.getComponentCount());
        assertEquals(0, constraintsFor(panel, 0).gridx);
        assertEquals(0, constraintsFor(panel, 0).gridy);
        assertEquals(1, constraintsFor(panel, 1).gridx);
        assertEquals(0, constraintsFor(panel, 1).gridy);
        assertEquals(0, constraintsFor(panel, 2).gridx);
        assertEquals(1, constraintsFor(panel, 2).gridy);
        GridBagConstraints filler = constraintsFor(panel, 3);
        assertEquals(2, filler.gridx);
        assertEquals(1.0, filler.weightx);
        assertEquals(GridBagConstraints.HORIZONTAL, filler.fill);
    }

    @Test
    void singleColumnPlacesEveryPairInOneColumn() {
        SettingsPairedFieldGridPanel panel = new SettingsPairedFieldGridPanel("Test", 300, 300, 100, 1);

        panel.addPairs(labels(3), controls(3));

        assertEquals(0, constraintsFor(panel, 0).gridx);
        assertEquals(0, constraintsFor(panel, 0).gridy);
        assertEquals(0, constraintsFor(panel, 1).gridx);
        assertEquals(1, constraintsFor(panel, 1).gridy);
        assertEquals(0, constraintsFor(panel, 2).gridx);
        assertEquals(2, constraintsFor(panel, 2).gridy);
        assertEquals(1, constraintsFor(panel, 3).gridx);
    }

    @Test
    void addPairsAssociatesSwingLabelsWithControls() {
        SettingsPairedFieldGridPanel panel = new SettingsPairedFieldGridPanel("Test", 300, 300, 100, 1);
        JLabel label = new JLabel("Label");
        JLabel control = new JLabel("Control");

        panel.addPairs(new JComponent[] { label }, new JComponent[] { control });

        assertSame(control, label.getLabelFor());
    }

    @Test
    void addPairsUsesScaledWidthsAndSpacing() {
        SettingsPairedFieldGridPanel panel = new SettingsPairedFieldGridPanel("Test", 300, 280, 100, 2);
        JComponent[] labels = labels(2);
        JComponent[] controls = controls(2);

        panel.addPairs(labels, controls);

        JPanel firstPair = (JPanel) panel.getComponent(0);
        JPanel secondPair = (JPanel) panel.getComponent(1);
        assertEquals(UIUtil.scaleForGUI(300), firstPair.getPreferredSize().width);
        assertEquals(UIUtil.scaleForGUI(280), secondPair.getPreferredSize().width);
        assertEquals(UIUtil.scaleForGUI(100), controls[0].getPreferredSize().width);

        GridBagConstraints pairConstraints = constraintsFor(panel, 0);
        assertEquals(UIUtil.scaleForGUI(5), pairConstraints.insets.top);
        assertEquals(UIUtil.scaleForGUI(5), pairConstraints.insets.bottom);

        GridBagLayout pairLayout = (GridBagLayout) firstPair.getLayout();
        assertEquals(UIUtil.scaleForGUI(8),
              pairLayout.getConstraints(firstPair.getComponent(0)).insets.right);
        assertEquals(UIUtil.scaleForGUI(8),
              pairLayout.getConstraints(firstPair.getComponent(1)).insets.right);
    }

    private static JComponent[] labels(int count) {
        JComponent[] components = new JComponent[count];
        for (int index = 0; index < count; index++) {
            components[index] = new JLabel("label " + index);
        }
        return components;
    }

    private static JComponent[] controls(int count) {
        JComponent[] components = new JComponent[count];
        for (int index = 0; index < count; index++) {
            components[index] = new JLabel("control " + index);
        }
        return components;
    }

    private static GridBagConstraints constraintsFor(SettingsPairedFieldGridPanel panel, int index) {
        GridBagLayout layout = (GridBagLayout) panel.getLayout();
        return layout.getConstraints(panel.getComponent(index));
    }
}
