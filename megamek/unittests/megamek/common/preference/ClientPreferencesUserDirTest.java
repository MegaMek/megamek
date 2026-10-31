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
package megamek.common.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientPreferencesUserDirTest {

    private String previousOverride;
    private PreferenceStore store;
    private ClientPreferences preferences;

    @BeforeEach
    void beforeEach() {
        previousOverride = System.getProperty(ClientPreferences.USER_DIR_PROPERTY);
        System.clearProperty(ClientPreferences.USER_DIR_PROPERTY);
        store = new PreferenceStore();
        preferences = new ClientPreferences(store);
    }

    @AfterEach
    void afterEach() {
        if (previousOverride == null) {
            System.clearProperty(ClientPreferences.USER_DIR_PROPERTY);
        } else {
            System.setProperty(ClientPreferences.USER_DIR_PROPERTY, previousOverride);
        }
    }

    @Test
    void savedPreferenceIsUsedWithoutOverride() {
        preferences.setUserDir("C:\\Users\\player\\MegaMekUser\\");

        assertFalse(preferences.isUserDirOverridden());
        assertEquals("C:\\Users\\player\\MegaMekUser", preferences.getUserDir());
    }

    @Test
    void overrideTakesPrecedenceWithoutChangingSavedPreference() {
        preferences.setUserDir("/home/player/saved");
        System.setProperty(ClientPreferences.USER_DIR_PROPERTY, " /home/player/shared/ ");

        assertTrue(preferences.isUserDirOverridden());
        assertEquals("/home/player/shared", preferences.getUserDir());
        assertEquals("/home/player/saved", store.getString(ClientPreferences.USER_DIR));
    }

    @Test
    void blankOverrideIsIgnored() {
        preferences.setUserDir("/home/player/saved");
        System.setProperty(ClientPreferences.USER_DIR_PROPERTY, "   ");

        assertFalse(preferences.isUserDirOverridden());
        assertEquals("/home/player/saved", preferences.getUserDir());
    }

    @Test
    void savingTheOverrideValueDoesNotPersistIt() {
        preferences.setUserDir("/home/player/saved");
        System.setProperty(ClientPreferences.USER_DIR_PROPERTY, "/home/player/shared");

        preferences.setUserDir(preferences.getUserDir());

        assertEquals("/home/player/saved", store.getString(ClientPreferences.USER_DIR));
    }

    @Test
    void filesystemRootsKeepTheirSeparator() {
        assertEquals("/", ClientPreferences.stripTrailingSeparators("/"));
        assertEquals("/", ClientPreferences.stripTrailingSeparators("//"));
        assertEquals("C:\\", ClientPreferences.stripTrailingSeparators("C:\\"));
        assertEquals("C:\\", ClientPreferences.stripTrailingSeparators("C:\\\\"));
        assertEquals("D:/", ClientPreferences.stripTrailingSeparators("D:/"));
        assertEquals("C:\\Games", ClientPreferences.stripTrailingSeparators("C:\\Games\\"));
    }

    @Test
    void rootOverrideIsUsedAsIs() {
        System.setProperty(ClientPreferences.USER_DIR_PROPERTY, "/");
        assertEquals("/", preferences.getUserDir());

        System.setProperty(ClientPreferences.USER_DIR_PROPERTY, "C:\\");
        assertEquals("C:\\", preferences.getUserDir());
    }
}
