/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.preference;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/**
 *
 * @author nderwin
 */
public class PreferenceManagerTest {

    private static final String DAMAGE_LEVEL_KEY = "ShowDamageLevel";

    private static final String GUI_PREFERENCES_STORE = "megamek.client.ui.swing.GUIPreferences";
    
    private static final String LOCALE_KEY = "Locale";

    private PreferenceManager testMe;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        testMe = new PreferenceManager();
    }

    @Test
    public void testSaveAndLoad() throws IOException {
        File f = tmpFolder.newFile("test-client-settings.xml");

        testMe.getPreferenceStore(PreferenceManager.CLIENT_SETTINGS_STORE_NAME).setValue(LOCALE_KEY, Locale.GERMAN.getLanguage());
        testMe.getPreferenceStore(GUI_PREFERENCES_STORE).setValue(DAMAGE_LEVEL_KEY, true);

        testMe.save(f);

        assertTrue(f.exists());
        assertTrue(f.length() > 0);

        testMe.load(f.toString());

        assertEquals(Locale.GERMAN.getLanguage(), testMe.getPreferenceStore(PreferenceManager.CLIENT_SETTINGS_STORE_NAME).getString(LOCALE_KEY));
        assertTrue(testMe.getPreferenceStore(GUI_PREFERENCES_STORE).getBoolean(DAMAGE_LEVEL_KEY));
    }

}
