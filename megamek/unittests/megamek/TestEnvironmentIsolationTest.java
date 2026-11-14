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
package megamek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.prefs.Preferences;

import megamek.common.Configuration;
import megamek.common.preference.PreferenceManager;
import megamek.testUtilities.InMemoryPreferencesFactory;
import megamek.testUtilities.TestEnvironmentIsolation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guards the isolation of the MegaMek unit tests from the developer's own settings, set up by
 * {@link TestEnvironmentIsolation} and {@link InMemoryPreferencesFactory}.
 *
 * <p>
 * If that isolation is lost, tests start passing or failing depending on whose machine they run on (a different
 * language is enough to fail hundreds of them), which is hard to diagnose. These tests make that failure loud and point
 * at the cause. They check the effect, not just the configuration.
 * </p>
 */
class TestEnvironmentIsolationTest {
    private static final String LISTENER_REGISTRATION = "testresources/META-INF/services/"
                                                              + "org.junit.platform.launcher.LauncherSessionListener";

    @Test
    void userPreferencesAreNotBackedByTheRealUserStore() {
        Preferences userRoot = Preferences.userRoot();

        assertTrue(userRoot.getClass().getName().startsWith(InMemoryPreferencesFactory.class.getName()),
              "Preferences.userRoot() is " + userRoot.getClass().getName() + ", not the in-memory test store. Check "
                    + "testresources/META-INF/services/java.util.prefs.PreferencesFactory.");
    }

    @Test
    void clearAllEmptiesTheInMemoryStore() {
        Preferences node = Preferences.userRoot().node("isolationProbe");
        node.put("isolationProbe", "set");

        InMemoryPreferencesFactory.clearAll();

        assertNull(Preferences.userRoot().node("isolationProbe").get("isolationProbe", null));
    }

    @Test
    void defaultLocaleIsFixed() {
        assertEquals(Locale.US, Locale.getDefault(),
              "The JVM default locale follows the operating system. Check " + LISTENER_REGISTRATION + ".");
        assertEquals(Locale.US, Locale.getDefault(Locale.Category.FORMAT));
    }

    @Test
    void clientSettingsAreNotTheDevelopersFile() {
        String clientSettingsPath = System.getProperty(PreferenceManager.CFG_FILE_OPTION_NAME);

        assertNotNull(clientSettingsPath,
              "The '" + PreferenceManager.CFG_FILE_OPTION_NAME + "' system property is not set, so the language and "
                    + "other client settings come from mmconf/clientsettings.xml. Check " + LISTENER_REGISTRATION
                    + ".");
        assertNotEquals(new File(Configuration.configDir(), PreferenceManager.DEFAULT_CFG_FILE_NAME).getAbsoluteFile(),
              new File(clientSettingsPath).getAbsoluteFile());
    }

    @Test
    void clientSettingsLoadedAsDefaults() {
        assertEquals("", PreferenceManager.getClientPreferences().getLocaleString(),
              "A language was loaded from a settings file, so text will not match the default resource bundles.");
    }

    /**
     * The config directory is pointed at a temporary one while saving, so that if {@code save()} ignores the test
     * settings file, this test overwrites a scratch copy rather than the developer's own settings.
     */
    @Test
    void savingClientSettingsWritesToTheTestFile(@TempDir Path scratchConfigDirectory) {
        File clientSettings = new File(System.getProperty(PreferenceManager.CFG_FILE_OPTION_NAME));
        File originalConfigDirectory = Configuration.configDir();

        Configuration.setConfigDir(scratchConfigDirectory.toFile());
        try {
            PreferenceManager.getInstance().save();
        } finally {
            Configuration.setConfigDir(originalConfigDirectory);
        }

        assertTrue(clientSettings.isFile(),
              "PreferenceManager.save() did not write to " + clientSettings + ", so outside the tests it writes "
                    + "over the developer's own mmconf/clientsettings.xml.");
        assertFalse(scratchConfigDirectory.resolve(PreferenceManager.DEFAULT_CFG_FILE_NAME).toFile().exists(),
              "PreferenceManager.save() wrote to the config directory rather than the file it loaded from.");
    }
}
