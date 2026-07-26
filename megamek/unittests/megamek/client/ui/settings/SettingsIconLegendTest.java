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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;

import megamek.client.ui.util.FontHandler;
import org.junit.jupiter.api.Test;

class SettingsIconLegendTest {
    @Test
    void legendRendersOneIconLabelPerBadge() {
        List<SettingsBadge> badges = List.of(
              new SettingsBadge(0xE002, null, "Important"),
              new SettingsBadge(0xE838, Color.GREEN, "Recent"));
        SettingsIconLegend legend = new SettingsIconLegend(badges);

        assertEquals(2, legend.getComponentCount());
        for (int index = 0; index < legend.getComponentCount(); index++) {
            Component component = legend.getComponent(index);
            JLabel label = (JLabel) component;
            SettingsBadge badge = badges.get(index);
            assertTrue(label.getText().contains(badge.description()));
            assertEquals(FontHandler.symbolFont().canDisplay(badge.codePoint()), label.getIcon() != null);
        }
    }

    @Test
    void legendEscapesPlainTextDescriptionsBeforeRenderingHtml() {
        SettingsIconLegend legend = new SettingsIconLegend(
              List.of(new SettingsBadge(0xE002, null, "A & B < C > D")));

        JLabel label = (JLabel) legend.getComponent(0);
        assertEquals("<html>A &amp; B &lt; C &gt; D</html>", label.getText());
    }

    @Test
    void legendButtonDoesNotRetainMutableSourceList() {
        List<SettingsBadge> badges = new ArrayList<>();
        badges.add(new SettingsBadge(0xE002, null, "Important"));
        JButton button = SettingsIconLegend.createLegendButton("Legend", "Show legend", badges);
        badges.set(0, null);

        RuntimeException exception = assertThrows(RuntimeException.class,
              () -> button.getActionListeners()[0].actionPerformed(
                    new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "legend")));

        assertFalse(exception instanceof NullPointerException, exception::toString);
    }
}
