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
package megamek.client.ui.clientGUI;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Warning/Caution/Precaution/Ok colours swap to a deeper variant on a light theme, but only while the player has
 * not picked their own colour (issue #9450).
 */
class GUIPreferencesThemedColorTest {

    private static final Color STOCK = new Color(225, 51, 51);
    private static final Color LIGHT_VARIANT = new Color(0xB71C1C);
    private static final Color PLAYER_CHOICE = new Color(0x3366FF);
    private static final Color LIGHT_BACKGROUND = new Color(0xFFFFFF);
    private static final Color DARK_BACKGROUND = new Color(0x3C3F41);

    private Object savedTableBackground;
    private Object savedMenuBackground;

    @BeforeEach
    void saveLookAndFeelColours() {
        savedTableBackground = UIManager.get("Table.background");
        savedMenuBackground = UIManager.get("Menu.background");
    }

    @AfterEach
    void restoreLookAndFeelColours() {
        UIManager.put("Table.background", savedTableBackground);
        UIManager.put("Menu.background", savedMenuBackground);
    }

    @Test
    void stockColourSwapsToLightVariantOnLightTheme() {
        UIManager.put("Table.background", LIGHT_BACKGROUND);
        assertEquals(LIGHT_VARIANT, GUIPreferences.getThemedColor(STOCK, STOCK, LIGHT_VARIANT));
    }

    @Test
    void stockColourStaysOnDarkTheme() {
        UIManager.put("Table.background", DARK_BACKGROUND);
        assertEquals(STOCK, GUIPreferences.getThemedColor(STOCK, STOCK, LIGHT_VARIANT));
    }

    @Test
    void playerColourIsKeptOnLightTheme() {
        UIManager.put("Table.background", LIGHT_BACKGROUND);
        assertEquals(PLAYER_CHOICE, GUIPreferences.getThemedColor(PLAYER_CHOICE, STOCK, LIGHT_VARIANT));
    }

    @Test
    void stockColourWithAlphaStillCountsAsStock() {
        UIManager.put("Table.background", LIGHT_BACKGROUND);
        Color storedWithAlpha = new Color(STOCK.getRed(), STOCK.getGreen(), STOCK.getBlue(), 128);
        assertEquals(LIGHT_VARIANT, GUIPreferences.getThemedColor(storedWithAlpha, STOCK, LIGHT_VARIANT));
    }

    @Test
    void realWarningGetterUsesLightVariantWithStockPreference() {
        UIManager.put("Table.background", LIGHT_BACKGROUND);
        GUIPreferences preferences = GUIPreferences.getInstance();
        Color savedWarning = preferences.getWarningColor();
        try {
            preferences.setWarningColor(STOCK);
            assertEquals(LIGHT_VARIANT, preferences.getWarningColorForTheme());
            assertEquals(STOCK, preferences.getWarningColor());
        } finally {
            preferences.setWarningColor(savedWarning);
        }
    }
}
