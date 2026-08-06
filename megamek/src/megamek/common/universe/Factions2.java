/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.MMConstants;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.common.annotations.Nullable;
import megamek.common.jacksonAdapters.ColorDeserializer;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;

/**
 * This class manages the unified Faction2 class that combines MHQ's Faction and the RATGenerator's FactionRecord and
 * makes it available to all project parts.
 * <p>
 * Faction data is loaded from data/universe/factions and .../commands, as well as from the same folders in the user
 * directory, if available.
 */
public final class Factions2 {

    /*
    Temporary information about this migration:

    + move together MHQ and RAT Faction information
    + separate to factions, commands, technical factions?
    + unify rat fallback ("parent" and "alternate")
    X single fallback for rating system? auto-fallback to IS/CLAN? (another time)
    X better era mods yaml, like: AOW: 1; (another time)
    + create new MM Faction class, read in yaml
    + use in RATGen
    + use in MHQ
    + use in MML
    + altNames (without year) is not used anywhere; retire
    + save factions from RATGenEd: problem factionrecords are not factions, how to save back to faction
    + add camos folder -> problem: varying with year
    X add specific camos for commands (another time)
    + standard naming for changes with year
    + change startingplanet to capital
    X exact dates? (seems unnecessary)
    + add lance/star sizes formation sizes
    - add aero lance sizes (cant find)
    + test MM force generation
    + test MML faction chooser
    + test MHQ planet and system info
    X clean up Tamar Pact: TamP, TamPact, NTamP (data, not the task here)

    RAT factions not found in MHQ factions: PP BH SE TamPact SL3 IS Periphery VesMar MalCon Blessed Order BAN
    and all commands

    MHQ factions not found in RAT factions: DoL, SCW, SCon, GDL, NONE, IE, CCon, CCom, AC, OMA, AE, AG, MalC,
    VSM, ARC, ARD, MRep, FWLR, ARL, JF, RP, BC, TGU, RU, CRep, SIMA, NDC, ABN, TiC, RTR, FCo, KE, SP, THW, CH,
    ChP, THa, NTamP, REB, DIS, Alf, TU, LR, FoO, FoS, DT, FFR, MA, ME, NOC, RON, ACPS, SIS, IND, Mara, RPG, WA,
    UND, IoS, SSUP, PD, TamP, SKP, AXP, CTL

    */

    private static final MMLogger LOGGER = MMLogger.create(Factions2.class);
    private static Factions2 instance;

    public static final String FACTIONS2_TEST_DIRECTORY = "testresources/data/universe/factions";

    private final Map<String, Faction2> factions = new HashMap<>();

    /** Maps a retired/aliased faction code to the surviving canonical faction key. See {@link Faction2#getAliases()}. */
    private final Map<String, String> aliasToCanonical = new HashMap<>();

    private Factions2() {
        loadFactionsFromFile();
    }

    public static synchronized Factions2 getInstance() {
        return getInstance(false);
    }

    public static synchronized Factions2 getInstance(boolean useTestDirectory) {
        if (instance == null) {
            instance = useTestDirectory ? new Factions2(FACTIONS2_TEST_DIRECTORY) : new Factions2();
        }

        return instance;
    }

    public static synchronized void setInstance(@Nullable Factions2 instance) {
        Factions2.instance = instance;
    }

    /**
     * This constructor is intended for unit testing only and will load factions *only* from the provided path. The path
     * is used as it is.
     *
     * @param factionsDataPath The path to load factions data from
     */
    public Factions2(String factionsDataPath) {
        ObjectMapper mapper = getLoadMapper();
        loadFactionsFromDirectory(factionsDataPath, mapper);
        registerAliases();
    }

    /**
     * @return All available factions. The returned Collection is a view of the internal faction list and must not be
     *       modified.
     */
    public Collection<Faction2> getFactions() {
        return factions.values();
    }

    /**
     * @param factionCode The faction's key such as LA, HL, CGS or LA.DG
     *
     * @return The faction for the given faction code, if any.
     */
    public Optional<Faction2> getFaction(@Nullable String factionCode) {
        if (factionCode == null) {
            return Optional.empty();
        }
        Faction2 faction = factions.get(factionCode);
        if (faction == null) {
            String canonicalKey = aliasToCanonical.get(factionCode);
            if (canonicalKey != null) {
                faction = factions.get(canonicalKey);
            }
        }
        return Optional.ofNullable(faction);
    }

    /**
     * Loads the Factions data from the internal universe factions and commands folders as well as the respective user
     * dir folders, if the user dir is set.
     */
    private void loadFactionsFromFile() {
        LOGGER.info("Loading Faction and Command data...");
        ObjectMapper mapper = getLoadMapper();
        loadFactionsFromDirectory(MMConstants.FACTIONS_DIR, mapper);
        loadFactionsFromDirectory(MMConstants.COMMANDS_DIR, mapper);
        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if ((userDir != null) && !userDir.isBlank()) {
            loadFactionsFromDirectory(new File(userDir, MMConstants.FACTIONS_DIR).toString(), mapper);
            loadFactionsFromDirectory(new File(userDir, MMConstants.COMMANDS_DIR).toString(), mapper);
        }
        registerAliases();
        LOGGER.info("Loaded a total of {} factions and commands", factions.size());
    }

    /**
     * @return The Jackson ObjectMapper that should be used to load factions from the yaml files
     */
    private ObjectMapper getLoadMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Color.class, new ColorDeserializer());
        mapper.registerModule(module);
        return mapper;
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
            LOGGER.warn("Cannot load factions from {} (directory not present or not a directory)", factionsPath);
            return;
        }

        for (String factionFile : CommonSettingsDialog.filteredFilesWithSubDirs(dir, ".yml")) {
            try (FileInputStream fis = new FileInputStream(factionFile)) {
                loadFaction(fis, mapper);
            } catch (Exception ex) {
                // Ignore this file then
                LOGGER.error(ex, "Exception trying to parse {} - ignoring.", factionFile);
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
                            LOGGER.error(ex, "Exception trying to parse zip entry {} - ignoring.", entry.getName());
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "Exception trying to read the zip file {} - ignoring.", factionZipFile);
            }
        }
    }

    private void loadFaction(InputStream source, ObjectMapper mapper) throws IOException {
        Faction2 faction = mapper.readValue(source, Faction2.class);
        factions.put(faction.getKey(), faction);
    }

    /**
     * Registers each faction's historical code aliases (see {@link Faction2#getAliases()}) so that a lookup by a
     * retired faction code resolves to the surviving faction. Runs once after all faction files are loaded, so that a
     * real faction file always wins over an alias claiming the same code - this guards against a merger of two
     * distinct factions being mis-declared as a rename alias.
     */
    private void registerAliases() {
        for (Faction2 faction : factions.values()) {
            for (String aliasCode : faction.getAliases().values()) {
                if (factions.containsKey(aliasCode)) {
                    if (factions.get(aliasCode) != faction) {
                        LOGGER.warn("[FactionAlias] Alias {} for faction {} collides with an existing faction; " +
                                    "keeping the existing faction. Remove the {}.yml faction file to complete the merge.",
                              aliasCode, faction.getKey(), aliasCode);
                    }
                    continue;
                }
                String previousKey = aliasToCanonical.putIfAbsent(aliasCode, faction.getKey());
                if (previousKey != null && !previousKey.equals(faction.getKey())) {
                    LOGGER.warn("[FactionAlias] Alias {} is claimed by both {} and {}; keeping {}.",
                          aliasCode, previousKey, faction.getKey(), previousKey);
                } else if (previousKey == null) {
                    LOGGER.debug("[FactionAlias] Registered alias {} -> {}", aliasCode, faction.getKey());
                }
            }
        }
    }
}
