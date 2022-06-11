/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2013 - Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.preference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author nderwin
 */
public class PreferenceManagerTest {

    private static final String DAMAGE_LEVEL_KEY = "ShowDamageLevel";

    private static final String GUI_PREFERENCES_STORE = "megamek.client.ui.swing.GUIPreferences";
    
    private static final String LOCALE_KEY = "Locale";

    private PreferenceManager testMe;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    public void beforeEach() {
        testMe = new PreferenceManager();
    }

    @Test
    public void testSaveAndLoad() throws IOException {
        assertTrue(Files.isDirectory(tempDirectory));
        final Path createdFilePath = Files.createFile(tempDirectory.resolve("test-client-settings.xml"));
        final File file = createdFilePath.toFile();

        testMe.getPreferenceStore(PreferenceManager.CLIENT_SETTINGS_STORE_NAME).setValue(LOCALE_KEY, Locale.GERMAN.getLanguage());
        testMe.getPreferenceStore(GUI_PREFERENCES_STORE).setValue(DAMAGE_LEVEL_KEY, true);

        testMe.save(file);

        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        testMe.load(file.toString());

        assertEquals(Locale.GERMAN.getLanguage(), testMe.getPreferenceStore(PreferenceManager.CLIENT_SETTINGS_STORE_NAME).getString(LOCALE_KEY));
        assertTrue(testMe.getPreferenceStore(GUI_PREFERENCES_STORE).getBoolean(DAMAGE_LEVEL_KEY));
    }
}
