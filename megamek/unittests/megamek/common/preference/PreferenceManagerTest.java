/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author nderwin
 */
class PreferenceManagerTest {

    private static final String DAMAGE_LEVEL_KEY = "ShowDamageLevel";

    private static final String GUI_PREFERENCES_STORE = "megamek.client.ui.swing.GUIPreferences";

    private static final String LOCALE_KEY = "Locale";

    private PreferenceManager testMe;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    void beforeEach() {
        testMe = new PreferenceManager();
    }

    @Test
    void testSaveAndLoad() throws IOException {
        assertTrue(Files.isDirectory(tempDirectory));
        final Path createdFilePath = Files.createFile(tempDirectory.resolve("test-client-settings.xml"));
        final File file = createdFilePath.toFile();

        testMe.getPreferenceStore(PreferenceManager.CLIENT_SETTINGS_STORE_NAME).setValue(LOCALE_KEY,
              Locale.GERMAN.getLanguage());
        testMe.getPreferenceStore(GUI_PREFERENCES_STORE).setValue(DAMAGE_LEVEL_KEY, true);

        testMe.save(file);

        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        testMe.load(file.toString());

        assertEquals(Locale.GERMAN.getLanguage(),
              testMe.getPreferenceStore(PreferenceManager.CLIENT_SETTINGS_STORE_NAME).getString(LOCALE_KEY));
        assertTrue(testMe.getPreferenceStore(GUI_PREFERENCES_STORE).getBoolean(DAMAGE_LEVEL_KEY));
    }
}
