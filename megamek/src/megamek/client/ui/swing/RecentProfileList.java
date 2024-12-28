/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import megamek.common.Configuration;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.logging.MMLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class keeps a list of recently opened profile files and makes it available statically. It automatically
 * writes the list to a file in the MM's mmconf directory.
 */
public final class RecentProfileList {

    public static final String RECENT_PROFILE_UPDATED = "recent_profile_event";

    private static final MMLogger LOGGER = MMLogger.create(RecentProfileList.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .disable(YAMLGenerator.Feature.SPLIT_LINES)
    );
    private static final int MAX_RECENT_PROFILES = 10;
    private static final String RECENT_PROFILES_FILENAME = "recent_profiles.yml";
    private static final File RECENT_PROFILE_FILE = new File(Configuration.configDir(), RECENT_PROFILES_FILENAME);

    private static final RecentProfileList INSTANCE = new RecentProfileList();
    private static final List<IPreferenceChangeListener> LISTENERS = new CopyOnWriteArrayList<>();

    private List<String> recentProfiles = null;

    /**
     * @return A list of the most recently opened board files. Can be empty.
     */
    public static List<String> getRecentProfiles() {
        INSTANCE.initialize();
        return INSTANCE.recentProfiles;
    }

    /**
     * Adds a new board to the recent board files, replacing the oldest if the list is full. Also
     * saves the list to file.
     *
     * @param board The board filename (full path)
     */
    public static void addProfile(String board) {
        INSTANCE.addBProfile(board);
    }

    /**
     * Adds a new board to the recent board files, replacing the oldest if the list is full. Also
     * saves the list to file.
     *
     * @param profile The profile file
     */
    public static void addProfile(File profile) {
        addProfile(profile.toString());
    }

    /**
     * Adds a listener for recent board changes. The event will have the name {@link #RECENT_PROFILE_UPDATED}.
     */
    public static void addListener(IPreferenceChangeListener listener) {
        if (!LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(IPreferenceChangeListener listener) {
        LISTENERS.remove(listener);
    }

    private void addBProfile(String board) {
        initialize();
        // remove and add so there is only one copy of each and the new board is at the end of the list
        recentProfiles.remove(board);
        recentProfiles.add(board);
        while (recentProfiles.size() > MAX_RECENT_PROFILES) {
            recentProfiles.remove(0);
        }
        saveRecentBoards();
        LISTENERS.forEach(l -> l.preferenceChange(
                new PreferenceChangeEvent(board, RECENT_PROFILE_UPDATED, null, null)));
    }

    private void saveRecentBoards() {
        try {
            YAML_MAPPER.writeValue(RECENT_PROFILE_FILE, INSTANCE.recentProfiles);
        } catch (IOException e) {
            LOGGER.error("Could not save recent board list", e);
        }
    }

    private void initialize() {
        if (INSTANCE.recentProfiles == null) {
            try {
                TypeReference<List<String>> typeRef = new TypeReference<>() { };
                INSTANCE.recentProfiles = YAML_MAPPER.readValue(RECENT_PROFILE_FILE, typeRef);
            } catch (FileNotFoundException e) {
                // ignore, this happens when no list has been saved yet
            } catch (IOException e) {
                LOGGER.error("Could not load recent board list", e);
            }
            if (INSTANCE.recentProfiles == null) {
                INSTANCE.recentProfiles = new ArrayList<>();
            }
        }
    }

    private RecentProfileList() { }
}
