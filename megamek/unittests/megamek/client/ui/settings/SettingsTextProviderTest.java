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

import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

class SettingsTextProviderTest {
    private static final ResourceBundle TEST_BUNDLE = new ListResourceBundle() {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                  { "plain", "Plain text" },
                { "formatted", "Hello, {0}" },
                { "literalFormatCharacters", "Pilot's {notes}" }
            };
        }
    };

    @Test
    void resourceBundleProviderResolvesKnownText() {
        SettingsTextProvider provider = SettingsTextProvider.fromResourceBundle(TEST_BUNDLE);

        assertTrue(provider.containsKey("plain"));
        assertEquals("Plain text", provider.getText("plain"));
    }

    @Test
    void resourceBundleProviderMarksMissingText() {
        SettingsTextProvider provider = SettingsTextProvider.fromResourceBundle(TEST_BUNDLE);

        assertFalse(provider.containsKey("missing"));
        assertEquals("!missing!", provider.getText("missing"));
    }

    @Test
    void providerFormatsResolvedText() {
        SettingsTextProvider provider = SettingsTextProvider.fromResourceBundle(TEST_BUNDLE);

        assertEquals("Hello, Dana", provider.getFormattedText("formatted", "Dana"));
    }

    @Test
    void providerDoesNotFormatTextWithoutArguments() {
        SettingsTextProvider provider = SettingsTextProvider.fromResourceBundle(TEST_BUNDLE);

        assertEquals("Pilot's {notes}", provider.getFormattedText("literalFormatCharacters"));
    }

    @Test
    void megaMekProviderUsesClientMessageFormatting() {
        SettingsTextProvider provider = SettingsTextProvider.megaMek();

        assertTrue(provider.containsKey("about.version"));
        assertFalse(provider.containsKey("missing.settings.text"));
        assertEquals("Version: TEST", provider.getFormattedText("about.version", "TEST"));
    }
}
