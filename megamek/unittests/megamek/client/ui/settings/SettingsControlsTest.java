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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import javax.swing.SpinnerNumberModel;

import org.junit.jupiter.api.Test;

class SettingsControlsTest {
    private static final SettingsTextProvider TEXT = SettingsTextProvider.fromResourceBundle(new ListResourceBundle() {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                  { "normal.text", "Normal" },
                  { "normal.tooltip", "Normal tooltip" },
                { "long.text", "Long" },
                { "long.tooltip", "This tooltip is deliberately long enough that wrapping it together with badge "
                    + "markup could insert a break inside the font tag attributes" },
                  { "legacy.text", "Legacy" },
                  { "legacy.toolTipText", "Legacy tooltip" },
                  { "placeholder.text", "Choice {0}" }
            };
        }
    });
    private static final SettingsBadge BADGE = new SettingsBadge(0xE002, null, "Important");

    @Test
    void labelResolvesTextTooltipAndBadgePlaceholder() {
        SettingsLabel normal = new SettingsLabel(TEXT, "normal");
        SettingsLabel placeholder = new SettingsLabel(TEXT, "placeholder", List.of(BADGE));

        assertEquals("<html><nobr>Normal</nobr></html>", normal.getText());
        assertTrue(normal.getToolTipText().contains("Normal tooltip"));
        assertTrue(placeholder.getText().contains("<nobr>"));
        assertTrue(placeholder.getText().contains("Choice "));
        assertTrue(placeholder.getText().contains("Material Symbols Rounded"));
    }

    @Test
    void checkboxAppendsBadgesAndUsesLegacyTooltipFallback() {
        SettingsCheckBox checkBox = new SettingsCheckBox(TEXT, "legacy", List.of(BADGE));

        assertTrue(checkBox.getText().contains("Legacy"));
        assertTrue(checkBox.getText().contains("Material Symbols Rounded"));
        assertTrue(checkBox.getToolTipText().contains("Legacy tooltip"));
        assertEquals("chklegacy", checkBox.getName());
    }

    @Test
    void missingTooltipIsNotInstalled() {
        SettingsTextField textField = new SettingsTextField(TEXT, "placeholder");

        assertNull(textField.getToolTipText());
        assertEquals("txtplaceholder", textField.getName());
    }

    @Test
    void spinnerSelectsNumericModelTypeAndTooltip() {
        SettingsSpinner integerSpinner = new SettingsSpinner(TEXT, "normal", 1, 0, 10, 1);
        SettingsSpinner doubleSpinner = new SettingsSpinner(TEXT, "normal", 1.0, 0.0, 10.0, 0.5);

        SpinnerNumberModel integerModel = assertInstanceOf(SpinnerNumberModel.class, integerSpinner.getModel());
        SpinnerNumberModel doubleModel = assertInstanceOf(SpinnerNumberModel.class, doubleSpinner.getModel());
        assertInstanceOf(Integer.class, integerModel.getNumber());
        assertInstanceOf(Double.class, doubleModel.getNumber());
        assertTrue(integerSpinner.getToolTipText().contains("Normal tooltip"));
        assertEquals("spnnormal", integerSpinner.getName());
    }

    @Test
    void spinnerPreservesNonDefaultModelTypes() {
        SettingsSpinner byteSpinner = new SettingsSpinner(TEXT, "normal", (byte) 1, (byte) 0, (byte) 10, (byte) 1);
        SettingsSpinner shortSpinner = new SettingsSpinner(TEXT, "normal", (short) 1, (short) 0, (short) 10,
              (short) 1);
        SettingsSpinner longSpinner = new SettingsSpinner(TEXT, "normal", 1L, 0L, 10L, 1L);
        SettingsSpinner floatSpinner = new SettingsSpinner(TEXT, "normal", 1.0f, 0.0f, 10.0f, 0.5f);

        SpinnerNumberModel byteModel = assertInstanceOf(SpinnerNumberModel.class, byteSpinner.getModel());
        SpinnerNumberModel shortModel = assertInstanceOf(SpinnerNumberModel.class, shortSpinner.getModel());
        SpinnerNumberModel longModel = assertInstanceOf(SpinnerNumberModel.class, longSpinner.getModel());
        SpinnerNumberModel floatModel = assertInstanceOf(SpinnerNumberModel.class, floatSpinner.getModel());
        assertInstanceOf(Byte.class, byteModel.getNumber());
        assertInstanceOf(Short.class, shortModel.getNumber());
        assertInstanceOf(Long.class, longModel.getNumber());
        assertInstanceOf(Float.class, floatModel.getNumber());
    }

    @Test
    void spinnerRejectsUnsupportedNumberTypesInsteadOfNarrowingThem() {
        BigDecimal value = BigDecimal.ONE;

        assertThrows(IllegalArgumentException.class,
              () -> new SettingsSpinner(TEXT, "normal", value, BigDecimal.ZERO, BigDecimal.TEN, value));
    }

    @Test
    void spinnerStickyHelpIncludesBadges() {
        SettingsSpinner spinner = new SettingsSpinner(TEXT, "long", 1, 0, 10, 1, false, List.of(BADGE));

        assertTrue(spinner.getSettingsHelpText().startsWith("<html>"));
        assertTrue(spinner.getSettingsHelpText().contains("Material Symbols Rounded"));
        assertFalse(spinner.getSettingsHelpText().matches("(?s).*<font[^>]*<br>.*"));
        assertTrue(spinner.getToolTipText().contains("Material Symbols Rounded"));
    }
}
