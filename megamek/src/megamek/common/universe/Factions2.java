/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
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
 */
package megamek.common.universe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.MMConstants;
import megamek.client.ui.swing.CommonSettingsDialog;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class manages the unified Faction2 class that combines MHQ's Faction and the RATGenerator's FactionRecord and
 * makes it available to all project parts.
 * <p>
 * Facion data is loaded from data/universe/factions and .../commands, as well as from the same folders in the user
 * directory, if available.
 */
public final class Factions2 {

    private static final MMLogger LOGGER = MMLogger.create(Factions2.class);
    private static Factions2 instance;

    private final Map<String, Faction2> factions = new HashMap<>();

    private Factions2() {
        loadFactionsFromFile();
    }

    public static synchronized Factions2 getInstance() {
        if (instance == null) {
            instance = new Factions2();
        }

        return instance;
    }

    /**
     * @return All available factions. The returned Collection is a view of the internal faction list and must not be
     * modified.
     */
    public Collection<Faction2> getFactions() {
        return factions.values();
    }

    /**
     * @param factionCode The faction's key such as LA, HL, CGS or LA.DG
     * @return The faction for the given faction code, if any.
     */
    public Optional<Faction2> getFaction(@Nullable String factionCode) {
        return Optional.ofNullable(factionCode == null ? null : factions.get(factionCode));
    }

    /**
     * Loads the Factions data from the internal universe factions and commands folders as well as the respective user
     * dir folders, if the user dir is set.
     */
    private void loadFactionsFromFile() {
        LOGGER.info("Loading Faction and Command data...");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        loadFactionsFromDirectory(MMConstants.FACTIONS_DIR, mapper);
        loadFactionsFromDirectory(MMConstants.COMMANDS_DIR, mapper);
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if ((userDir != null) && !userDir.isBlank()) {
            loadFactionsFromDirectory(new File(userDir, MMConstants.FACTIONS_DIR).toString(), mapper);
            loadFactionsFromDirectory(new File(userDir, MMConstants.COMMANDS_DIR).toString(), mapper);
        }
        LOGGER.info(String.format("Loaded a total of %d factions and commands", factions.size()));
    }

    /**
     * Loads Factions data from the given folder and subfolders.
     *
     * @param factionsPath A folder to load faction data from
     * @param mapper       A Jackson ObjectMapper to parse the YAMl faction files
     */
    private void loadFactionsFromDirectory(String factionsPath, ObjectMapper mapper) {
        Objects.requireNonNull(factionsPath);
        File dir = new File(factionsPath);
        if (!dir.isDirectory()) {
            LOGGER.warn(
                  "Cannot load factions from %s (directory not present or not a directory)".formatted(factionsPath));
            return;
        }

        for (String factionFile : CommonSettingsDialog.filteredFilesWithSubDirs(dir, ".yml")) {
            try (FileInputStream fis = new FileInputStream(factionFile)) {
                loadFaction(fis, mapper);
            } catch (Exception ex) {
                // Ignore this file then
                LOGGER.error("Exception trying to parse %s - ignoring.".formatted(factionFile), ex);
            }
        }

        for (String factionZipFile : CommonSettingsDialog.filteredFilesWithSubDirs(dir, ".zip")) {
            try (ZipFile zip = new ZipFile(factionZipFile)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    // Check if entry is a directory
                    if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                        try (InputStream inputStream = zip.getInputStream(entry)) {
                            loadFaction(inputStream, mapper);
                        } catch (Exception ex) {
                            // Ignore this file then
                            LOGGER.error(
                                  "Exception trying to parse zip entry %s - ignoring.".formatted(entry.getName()), ex);
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error(String.format("Exception trying to read the zip file %s - ignoring.", factionZipFile), ex);
            }
        }
    }

    private void loadFaction(InputStream source, ObjectMapper mapper) throws IOException {
        Faction2 faction = mapper.readValue(source, Faction2.class);
        factions.put(faction.getKey(), faction);
    }
}
