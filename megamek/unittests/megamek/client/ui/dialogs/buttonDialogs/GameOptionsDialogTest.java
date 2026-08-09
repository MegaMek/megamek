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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import javax.swing.JPanel;

import megamek.client.ui.panels.GameOptionsPane;
import org.junit.jupiter.api.Test;

class GameOptionsDialogTest {
    @Test
    void ruleToggleLabelsUseBadgeIconsAndOnOffText() {
        String unofficialOn = GameOptionsDialog.ruleToggleText(
              GameOptionsPane.unofficialBadge(), "Unofficial Rules", "On");
        String legacyOff = GameOptionsDialog.ruleToggleText(
              GameOptionsPane.legacyBadge(), "Legacy Rules", "Off");

        assertTrue(unofficialOn.contains(Character.toString(0xEA4B)), unofficialOn);
        assertTrue(unofficialOn.contains("color=\"#e69f00\""), unofficialOn);
        assertTrue(unofficialOn.contains("Unofficial Rules: <b>On</b>"), unofficialOn);
        assertTrue(legacyOff.contains(Character.toString(0xE889)), legacyOff);
        assertTrue(legacyOff.contains("Legacy Rules: <b>Off</b>"), legacyOff);
        assertFalse(unofficialOn.contains("\u2713"), unofficialOn);
        assertFalse(legacyOff.contains("\u2717"), legacyOff);
    }

    @Test
    void responsiveFooterKeepsGroupsIntactAndWrapsWhenNeeded() {
        JPanel ruleControls = fixedSizePanel(760, 40);
        JPanel actionButtons = fixedSizePanel(720, 40);
        JPanel footer = GameOptionsDialog.responsiveFooter(ruleControls, actionButtons, 8);

        footer.setSize(1_600, 100);
        footer.doLayout();

        assertEquals(8, ruleControls.getX());
        assertEquals(ruleControls.getY(), actionButtons.getY());
        assertTrue(actionButtons.getX() + actionButtons.getWidth() <= footer.getWidth());

        footer.setSize(1_200, footer.getPreferredSize().height);
        footer.doLayout();

        assertEquals(8, ruleControls.getX());
        assertTrue(actionButtons.getY() > ruleControls.getY());
        assertEquals((footer.getWidth() - actionButtons.getWidth()) / 2, actionButtons.getX());
        assertTrue(ruleControls.getX() + ruleControls.getWidth() <= footer.getWidth());
        assertTrue(actionButtons.getX() + actionButtons.getWidth() <= footer.getWidth());
    }

    private static JPanel fixedSizePanel(int width, int height) {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(width, height));
        return panel;
    }
}
