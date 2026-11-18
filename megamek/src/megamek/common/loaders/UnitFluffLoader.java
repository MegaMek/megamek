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

package megamek.common.loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.Nullable;
import megamek.common.DataFolders;
import megamek.common.DataFolders.DataFolder;
import megamek.common.units.Entity;
import megamek.common.units.EntityFluff;
import megamek.common.units.System;
import megamek.logging.MMLogger;

/**
 * Loads unit fluff that is kept outside the unit files. Such fluff isn't shipped with the games; players who want it
 * place it in a {@value #DIRECTORY_NAME} folder in a data folder: userdata/data/{@value #DIRECTORY_NAME},
 * [User Dir]/data/{@value #DIRECTORY_NAME} or data/{@value #DIRECTORY_NAME}, searched in that order (see
 * {@link DataFolders}). Each unit has its own YAML file named after the unit file's UUID, e.g.
 * {@code 0192f1e6-3f48-7c2b-9d4e-5a6b7c8d9e0f.yml}.
 *
 * <p>Fluff in the unit file always takes precedence. The external fluff only fills what the unit file leaves blank
 * (see {@link EntityFluff#withFallback(EntityFluff)}) and is only applied for display, through
 * {@link #displayFluff(Entity)}, so it never becomes part of the unit itself: it is not written back into unit files
 * when saving, nor sent to other players or stored in saved games.</p>
 */
public final class UnitFluffLoader {

    private static final MMLogger LOGGER = MMLogger.create(UnitFluffLoader.class);

    /** The name of the folder holding the unit fluff files, in any data folder. */
    public static final String DIRECTORY_NAME = "unitfluff";
    public static final String FILE_EXTENSION = ".yml";

    public static final String CHASSIS = "chassis";
    public static final String MODEL = "model";
    public static final String OVERVIEW = "overview";
    public static final String CAPABILITIES = "capabilities";
    public static final String DEPLOYMENT = "deployment";
    public static final String HISTORY = "history";
    public static final String MANUFACTURER = "manufacturer";
    public static final String PRIMARY_FACTORY = "primaryFactory";
    public static final String FLUFF_DATE = "fluffDate";
    public static final String SYSTEM_MANUFACTURERS = "systemManufacturers";
    public static final String SYSTEM_MODELS = "systemModels";

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

    /**
     * How long a lookup result, including "no fluff file", is reused. Fluff is looked up every time a unit is shown;
     * caching keeps that fast on slow (e.g. cloud-synced) user dirs, while fluff added during a session still appears.
     */
    private static final long LOOKUP_LIFETIME_NANOS = TimeUnit.SECONDS.toNanos(30);
    private static final int LOOKUP_CACHE_PRUNE_SIZE = 2000;

    private static final Map<String, Lookup> LOOKUPS = new ConcurrentHashMap<>();
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    private UnitFluffLoader() {}

    /**
     * Returns the fluff to show for the given unit: its own fluff, with anything it leaves blank filled from its
     * external fluff file, if there is one. Use this wherever unit fluff is displayed or printed. When editing or saving
     * a unit, use {@link Entity#getFluff()} instead so that the external fluff doesn't become part of the unit.
     *
     * @param entity The unit
     *
     * @return The fluff to display; this is the unit's own fluff object when there is no external fluff
     */
    public static EntityFluff displayFluff(Entity entity) {
        return entity.getFluff().withFallback(externalFluff(entity.getUnitFileUUID()));
    }

    /**
     * @param unitFileUUID The UUID of a unit file
     *
     * @return The external fluff for the unit with the given UUID, or null if there is none
     */
    public static @Nullable EntityFluff externalFluff(@Nullable String unitFileUUID) {
        if ((unitFileUUID == null) || !UUID_PATTERN.matcher(unitFileUUID).matches()) {
            return null;
        }
        String key = unitFileUUID.toLowerCase();
        long now = java.lang.System.nanoTime();
        Lookup cached = LOOKUPS.get(key);
        if ((cached != null) && !cached.isExpired(now)) {
            return cached.fluff();
        }
        Lookup lookup = new Lookup(findAndReadFluff(key), now);
        if (LOOKUPS.size() > LOOKUP_CACHE_PRUNE_SIZE) {
            LOOKUPS.values().removeIf(entry -> entry.isExpired(now));
        }
        LOOKUPS.put(key, lookup);
        return lookup.fluff();
    }

    /** Forgets all cached lookups so that the next lookups read the fluff folders again. */
    public static void clearCache() {
        LOOKUPS.clear();
    }

    /**
     * @return The folders that may hold unit fluff files, in the order they are searched: the {@value #DIRECTORY_NAME}
     *       folder of each data folder, from the highest layer to the lowest (see {@link DataFolders})
     */
    public static List<File> fluffDirectories() {
        List<DataFolder> dataFolders = DataFolders.lookupOrder();
        List<File> result = new ArrayList<>(dataFolders.size());
        for (DataFolder dataFolder : dataFolders) {
            result.add(dataFolder.resolve(DIRECTORY_NAME));
        }
        return result;
    }

    private static @Nullable EntityFluff findAndReadFluff(String unitFileUUID) {
        for (File directory : fluffDirectories()) {
            File file = new File(directory, unitFileUUID + FILE_EXTENSION);
            if (file.isFile()) {
                try {
                    return readFluffFile(file);
                } catch (IOException | RuntimeException exception) {
                    LOGGER.warn("Could not read the unit fluff file {}", file, exception);
                }
            }
        }
        return null;
    }

    /**
     * Reads a unit fluff file.
     *
     * @param file The unit fluff file
     *
     * @return The fluff in the file; only the fields that unit fluff files can hold are set
     *
     * @throws IOException When the file can't be read or isn't valid YAML
     */
    public static EntityFluff readFluffFile(File file) throws IOException {
        JsonNode root = YAML_MAPPER.readTree(file);
        EntityFluff fluff = new EntityFluff();
        if ((root == null) || !root.isObject()) {
            return fluff;
        }
        fluff.setOverview(text(root, OVERVIEW));
        fluff.setCapabilities(text(root, CAPABILITIES));
        fluff.setDeployment(text(root, DEPLOYMENT));
        fluff.setHistory(text(root, HISTORY));
        fluff.setManufacturer(text(root, MANUFACTURER));
        fluff.setPrimaryFactory(text(root, PRIMARY_FACTORY));
        fluff.setFluffDate(text(root, FLUFF_DATE));
        for (Map.Entry<String, JsonNode> entry : root.path(SYSTEM_MANUFACTURERS).properties()) {
            System system = System.parse(entry.getKey());
            if (system != null) {
                fluff.setSystemManufacturer(system, entry.getValue().asText(""));
            }
        }
        for (Map.Entry<String, JsonNode> entry : root.path(SYSTEM_MODELS).properties()) {
            System system = System.parse(entry.getKey());
            if (system != null) {
                fluff.setSystemModel(system, entry.getValue().asText(""));
            }
        }
        return fluff;
    }

    private static String text(JsonNode root, String key) {
        JsonNode node = root.get(key);
        return ((node == null) || node.isNull() || node.isContainerNode()) ? "" : node.asText("");
    }

    /**
     * @param fluff A unit's fluff
     *
     * @return True if the given fluff has any of the fields that unit fluff files hold
     */
    public static boolean hasFileFluff(EntityFluff fluff) {
        if (!fluff.getOverview().isBlank() || !fluff.getCapabilities().isBlank() || !fluff.getDeployment().isBlank()
              || !fluff.getHistory().isBlank() || !fluff.getManufacturer().isBlank()
              || !fluff.getPrimaryFactory().isBlank() || !fluff.getFluffDate().isBlank()) {
            return true;
        }
        for (System system : System.values()) {
            if (!fluff.getSystemManufacturer(system).isBlank() || !fluff.getSystemModel(system).isBlank()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the content of a unit fluff file. Long texts are written as YAML literal blocks so they stay readable and
     * editable; texts that a literal block can't hold exactly are written as quoted strings. The chassis and model are
     * only written to help people find their way around the files; they aren't read back.
     *
     * @param chassis The unit's chassis
     * @param model   The unit's model
     * @param fluff   The fluff to write; only the fields that unit fluff files hold are written
     *
     * @return The YAML text
     */
    public static String toYaml(String chassis, String model, EntityFluff fluff) {
        StringBuilder yaml = new StringBuilder();
        yaml.append(CHASSIS).append(": ").append(quoted(chassis)).append('\n');
        if (!model.isBlank()) {
            yaml.append(MODEL).append(": ").append(quoted(model)).append('\n');
        }
        appendText(yaml, OVERVIEW, fluff.getOverview());
        appendText(yaml, CAPABILITIES, fluff.getCapabilities());
        appendText(yaml, DEPLOYMENT, fluff.getDeployment());
        appendText(yaml, HISTORY, fluff.getHistory());
        appendLine(yaml, MANUFACTURER, fluff.getManufacturer());
        appendLine(yaml, PRIMARY_FACTORY, fluff.getPrimaryFactory());
        appendLine(yaml, FLUFF_DATE, fluff.getFluffDate());
        appendSystems(yaml, SYSTEM_MANUFACTURERS, fluff, true);
        appendSystems(yaml, SYSTEM_MODELS, fluff, false);
        return yaml.toString();
    }

    private static void appendText(StringBuilder yaml, String key, String value) {
        if (!value.isBlank()) {
            yaml.append(key).append(": ");
            if (fitsLiteralBlock(value)) {
                yaml.append("|-\n  ").append(value).append('\n');
            } else {
                yaml.append(quoted(value)).append('\n');
            }
        }
    }

    private static void appendLine(StringBuilder yaml, String key, String value) {
        if (!value.isBlank()) {
            yaml.append(key).append(": ").append(quoted(value)).append('\n');
        }
    }

    private static void appendSystems(StringBuilder yaml, String key, EntityFluff fluff, boolean manufacturers) {
        StringBuilder entries = new StringBuilder();
        for (System system : System.values()) {
            String value = manufacturers ? fluff.getSystemManufacturer(system) : fluff.getSystemModel(system);
            if (!value.isBlank()) {
                entries.append("  ").append(system.name()).append(": ").append(quoted(value)).append('\n');
            }
        }
        if (!entries.isEmpty()) {
            yaml.append(key).append(":\n").append(entries);
        }
    }

    /**
     * @return True if the value can be written as a single-line literal block and read back unchanged: no surrounding
     *       whitespace and no line breaks or other characters that YAML treats specially or doesn't allow unescaped
     */
    private static boolean fitsLiteralBlock(String value) {
        if (!value.equals(value.strip())) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (needsEscape(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean needsEscape(char character) {
        return (character < 0x20) || ((character >= 0x7F) && (character <= 0x9F)) || (character == '\u2028')
              || (character == '\u2029') || (character == '\uFEFF') || (character == '\uFFFE')
              || (character == '\uFFFF');
    }

    /** @return The value as a YAML double-quoted string */
    private static String quoted(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if ((character == '"') || (character == '\\')) {
                result.append('\\').append(character);
            } else if (needsEscape(character)) {
                result.append(String.format("\\u%04X", (int) character));
            } else {
                result.append(character);
            }
        }
        return result.append('"').toString();
    }

    private record Lookup(@Nullable EntityFluff fluff, long readAt) {
        boolean isExpired(long now) {
            return now - readAt > LOOKUP_LIFETIME_NANOS;
        }
    }
}
