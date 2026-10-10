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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import megamek.client.ui.util.PlayerColour;
import megamek.common.preference.PreferenceManager;
import megamek.common.preference.PreferenceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

class GUIPreferencesTest {

    private static final String LEGACY_BROWN_RGB = "152 129 107";
    private static final String CORRECTED_BROWN_RGB = "120 100 80";
    private static final String DEFAULT_BROWN_RGB = "153 130 108";

    @Test
    void optionSectionsDefaultToExpandedWhenPreferenceIsMissing() {
        PreferenceStore store = new PreferenceStore();
        withPreferenceStore(store, () -> {
            GUIPreferences preferences = new GUIPreferences();

            assertTrue(preferences.getExpandOptionSections());
            assertTrue(store.getDefaultBoolean(GUIPreferences.EXPAND_OPTION_SECTIONS));
            preferences.setExpandOptionSections(false);
            assertFalse(store.getBoolean(GUIPreferences.EXPAND_OPTION_SECTIONS));
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void preservesSavedOptionSectionPreference(boolean expanded) {
        PreferenceStore store = new PreferenceStore();
        store.putValue(GUIPreferences.EXPAND_OPTION_SECTIONS, Boolean.toString(expanded));

        withPreferenceStore(store, () ->
              assertEquals(expanded, new GUIPreferences().getExpandOptionSections()));
    }

    private static void withPreferenceStore(PreferenceStore store, Runnable assertions) {
        GUIPreferences.getInstance();
        PreferenceManager manager = mock(PreferenceManager.class);
        when(manager.getPreferenceStore("GUIPreferences", GUIPreferences.class.getName(),
              "megamek.client.ui.swing.GUIPreferences")).thenReturn(store);
        try (MockedStatic<PreferenceManager> managerStatic = mockStatic(PreferenceManager.class)) {
            managerStatic.when(PreferenceManager::getInstance).thenReturn(manager);
            assertions.run();
        }
    }

    @Test
    void migratesExplicitLegacyBrownValueWhenCorrectedValueIsAbsent() {
        PreferenceStore preferenceStore = new PreferenceStore();
        preferenceStore.setValue(GUIPreferences.LEGACY_PLAYER_COLOUR_BROWN, LEGACY_BROWN_RGB);

        GUIPreferences.migrateLegacyBrownPlayerColour(preferenceStore);

        assertTrue(preferenceStore.hasProperty(PlayerColour.PLAYER_COLOUR_BROWN));
        assertEquals(LEGACY_BROWN_RGB, preferenceStore.getString(PlayerColour.PLAYER_COLOUR_BROWN));
        assertEquals(LEGACY_BROWN_RGB, preferenceStore.getString(GUIPreferences.LEGACY_PLAYER_COLOUR_BROWN));
    }

    @Test
    void preservesExplicitCorrectedBrownValueWhenBothKeysExist() {
        PreferenceStore preferenceStore = new PreferenceStore();
        preferenceStore.setValue(GUIPreferences.LEGACY_PLAYER_COLOUR_BROWN, LEGACY_BROWN_RGB);
        preferenceStore.setValue(PlayerColour.PLAYER_COLOUR_BROWN, CORRECTED_BROWN_RGB);

        GUIPreferences.migrateLegacyBrownPlayerColour(preferenceStore);

        assertEquals(CORRECTED_BROWN_RGB, preferenceStore.getString(PlayerColour.PLAYER_COLOUR_BROWN));
        assertEquals(LEGACY_BROWN_RGB, preferenceStore.getString(GUIPreferences.LEGACY_PLAYER_COLOUR_BROWN));
    }

    @Test
    void leavesCorrectedBrownValueAsDefaultWhenNeitherKeyIsExplicitlySet() {
        PreferenceStore preferenceStore = new PreferenceStore();
        preferenceStore.setDefault(PlayerColour.PLAYER_COLOUR_BROWN, DEFAULT_BROWN_RGB);

        GUIPreferences.migrateLegacyBrownPlayerColour(preferenceStore);

        assertFalse(preferenceStore.hasProperty(GUIPreferences.LEGACY_PLAYER_COLOUR_BROWN));
        assertFalse(preferenceStore.hasProperty(PlayerColour.PLAYER_COLOUR_BROWN));
        assertEquals(DEFAULT_BROWN_RGB, preferenceStore.getString(PlayerColour.PLAYER_COLOUR_BROWN));
    }
}
