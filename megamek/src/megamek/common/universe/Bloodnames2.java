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
package megamek.common.universe;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.MMConstants;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

/**
 * Loads and holds the Bloodname data: {@code data/universe/bloodnames}, one folder per Clan and one
 * file per Bloodname within it.
 *
 * <p>A Bloodname is filed under the Clan that founded it, which is not the same as the set of Clans
 * whose warriors may hold it - a name that is not exclusive can be granted elsewhere, and the Wars of
 * Reaving moved many legacies between Clans. Those relationships live on the House itself.</p>
 *
 * <p>Held separately from {@link Factions2} rather than on the faction, because the data is
 * substantial - some eight hundred names - and only ever concerns a handful of factions.</p>
 */
public class Bloodnames2 {
    private static final MMLogger LOGGER = MMLogger.create(Bloodnames2.class);

    private static Bloodnames2 instance;

    /** Bloodname, lowercased, to every House recorded under it across all Clans. */
    private final Map<String, List<BloodnameHouse>> housesByName = new HashMap<>();

    /** Bloodname, lowercased, to the Bloodname record itself. */
    private final Map<String, Bloodname2> bloodnames = new HashMap<>();

    /** Faction key to the Bloodnames that Clan founded. */
    private final Map<String, List<Bloodname2>> byFoundingClan = new HashMap<>();

    private Bloodnames2() {
        loadFromDefaultDirectories();
    }

    /**
     * This constructor is intended for unit testing and loads only from the given path.
     *
     * @param bloodnamesDataPath the directory to load Bloodname data from
     */
    public Bloodnames2(String bloodnamesDataPath) {
        loadFromDirectory(bloodnamesDataPath, new ObjectMapper(new YAMLFactory()));
    }

    public static synchronized Bloodnames2 getInstance() {
        if (instance == null) {
            instance = new Bloodnames2();
        }
        return instance;
    }

    public static synchronized void setInstance(@Nullable Bloodnames2 newInstance) {
        instance = newInstance;
    }

    /**
     * Every House recorded under a Bloodname, across all Clans.
     *
     * <p>Usually one. Sixteen Bloodnames descend from more than one founder, and those return a House
     * each - the caller has to decide which is meant, ordinarily by matching the founding Clan or the
     * warrior's phenotype.</p>
     *
     * @param name the Bloodname to look up, matched without regard to case
     *
     * @return the Houses under that name, or an empty list if the name is unknown
     */
    public List<BloodnameHouse> getHouses(@Nullable String name) {
        if ((name == null) || name.isBlank()) {
            return Collections.emptyList();
        }
        return housesByName.getOrDefault(name.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    /**
     * @param name the Bloodname to look up, matched without regard to case
     *
     * @return the Bloodname record, or {@code null} if no such Bloodname is recorded
     */
    public @Nullable Bloodname2 getBloodname(@Nullable String name) {
        if ((name == null) || name.isBlank()) {
            return null;
        }
        return bloodnames.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * @param factionKey the Clan's faction key, such as {@code CW}
     *
     * @return the Bloodnames that Clan founded, or an empty list for a faction that founded none
     */
    public List<Bloodname2> getBloodnamesFoundedBy(@Nullable String factionKey) {
        if (factionKey == null) {
            return Collections.emptyList();
        }
        return byFoundingClan.getOrDefault(factionKey, Collections.emptyList());
    }

    /**
     * @return every Bloodname on record
     */
    public Collection<Bloodname2> getAllBloodnames() {
        return Collections.unmodifiableCollection(bloodnames.values());
    }

    /**
     * @return {@code true} when no Bloodname data could be loaded, which usually means the data
     *       directory is missing rather than empty
     */
    public boolean isEmpty() {
        return bloodnames.isEmpty();
    }

    private void loadFromDefaultDirectories() {
        LOGGER.info("Loading Bloodname data...");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        loadFromDirectory(MMConstants.BLOODNAMES_DIR, mapper);
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if ((userDir != null) && !userDir.isBlank()) {
            loadFromDirectory(new File(userDir, MMConstants.BLOODNAMES_DIR).toString(), mapper);
        }
        LOGGER.info("Loaded {} Bloodnames across {} Clans",
              bloodnames.size(), byFoundingClan.size());
    }

    /**
     * Reads every {@code .yml} beneath the given directory. A file that will not parse is logged and
     * skipped rather than aborting the load, so one bad Bloodname does not cost the rest.
     */
    private void loadFromDirectory(String path, ObjectMapper mapper) {
        File directory = new File(path);
        if (!directory.isDirectory()) {
            LOGGER.info("No Bloodname data at {} (directory not present); skipping", path);
            return;
        }
        for (String file : CommonSettingsDialog.filteredFilesWithSubDirs(directory, ".yml")) {
            try (InputStream source = new FileInputStream(file)) {
                register(mapper.readValue(source, Bloodname2.class));
            } catch (Exception exception) {
                LOGGER.error(exception, "Exception trying to parse {} - ignoring.", file);
            }
        }
    }

    private void register(Bloodname2 bloodname) {
        if ((bloodname == null) || (bloodname.getName() == null) || bloodname.getName().isBlank()) {
            LOGGER.warn("Ignoring a Bloodname file with no name");
            return;
        }
        String key = bloodname.getName().toLowerCase(Locale.ROOT);

        Bloodname2 existing = bloodnames.get(key);
        if (existing == null) {
            bloodnames.put(key, bloodname);
        } else {
            // The same Bloodname filed under two Clans; keep the first and merge the Houses in, so a
            // shared legacy is not silently reduced to whichever file happened to load last.
            existing.addHouses(bloodname.getHouses());
        }

        housesByName.computeIfAbsent(key, ignored -> new ArrayList<>())
              .addAll(bloodname.getHouses());

        if (bloodname.getClan() != null) {
            byFoundingClan.computeIfAbsent(bloodname.getClan(), ignored -> new ArrayList<>())
                  .add(bloodname);
        }
    }
}
