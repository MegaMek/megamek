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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

class SettingsSearchTextTest {
    @Test
    void extractsPlainLabelAndButtonText() {
        assertEquals("Plain Label", SettingsSearchText.from(new JLabel("Plain Label")).text());
        assertEquals("Enable Option", SettingsSearchText.from(new JCheckBox("Enable Option")).text());
    }

    @Test
    void extractsRenderedHtmlWithoutTagsAndDecodesEntities() {
        JLabel label = new JLabel("<html><b>Heat &amp; Fire</b><br>Rules</html>");

        SettingsSearchText.TextSource text = SettingsSearchText.from(label);

        assertTrue(text.isHtml());
        assertTrue(text.text().contains("Heat & Fire"), text.text());
        assertTrue(text.text().contains("Rules"), text.text());
        assertFalse(text.text().contains("<b>"), text.text());
    }

    @Test
    void mapsCaseInsensitiveAccentFreeTokensBackToSourceOffsets() {
        String source = "Über Heat Rules";

        List<SettingsSearchText.TextRange> ranges = SettingsSearchText.ranges(source, List.of("uber", "heat"));

        assertEquals(List.of(
              new SettingsSearchText.TextRange(0, 4),
              new SettingsSearchText.TextRange(5, 9)), ranges);
    }

    @Test
    void preservesNonLatinLettersDuringNormalization() {
        String source = "Йога Настройки";
        String normalized = SettingsRoute.normalizeSearchText(source);

        assertEquals(List.of(
              new SettingsSearchText.TextRange(0, 4),
              new SettingsSearchText.TextRange(5, 14)),
              SettingsSearchText.ranges(source, SettingsSearchText.tokens(normalized)));
    }

    @Test
    void findsRepeatedTokensIndependently() {
        assertEquals(List.of(
              new SettingsSearchText.TextRange(0, 4),
              new SettingsSearchText.TextRange(9, 13)),
              SettingsSearchText.ranges("Heat and heat", List.of("heat")));
    }

    @Test
    void tokenizesNormalizedFilterWithoutDuplicates() {
        assertEquals(List.of("heat", "fire"), SettingsSearchText.tokens("heat fire heat"));
        assertTrue(SettingsSearchText.tokens("").isEmpty());
    }
}
