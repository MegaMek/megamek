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
package megamek.utilities.ratdata;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import megamek.SuiteConstants;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.UnitTable;
import megamek.codeUtilities.MathUtility;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Writes the RAT Generator data for the RAT Probability Explorer on the MegaMek website.
 * <p>
 * By default it writes {@code inputs.json.gz} ({@link RatInputExporter}): the codes, unit stats and faction settings
 * the website's copy of the table-building steps works from. With {@code --tables} it also writes every table
 * {@link UnitTable#findTable} produces, the same call MekHQ makes, for every year, faction, equipment rating and unit
 * type. Those tables are the reference the website's results are checked against.
 * </p>
 * <p>
 * Table output, under the folder given by {@code --out}:
 * </p>
 * <ul>
 *     <li>{@code manifest.json}: the factions, units, unit types, roles and years the shards index into.</li>
 *     <li>{@code years/<year>.bin.gz}: the general tables for one year ({@link RatDataShard}).</li>
 *     <li>{@code roles/<year>.bin.gz}: the single-role tables for one era year, with the role strictness MekHQ
 *     uses.</li>
 * </ul>
 * <p>
 * Arguments: {@code --out <folder> [--tables] [--from <year>] [--to <year>] [--no-roles] [--no-manifest]
 * [--build-id <text>]}. A year range lets a build split the table work across several runs; every run writes the
 * same faction and unit lists, so their shards fit together. Run from the megamek folder so {@code data/} resolves.
 * </p>
 */
public final class RatDataExporter {

    private static final MMLogger LOGGER = MMLogger.create(RatDataExporter.class);

    /** The role strictness MekHQ passes (RATGeneratorConnector). */
    static final int ROLE_STRICTNESS = 2;

    private static final long INITIALIZE_TIMEOUT_MILLIS = 10 * 60 * 1000L;

    private final RATGenerator ratGenerator;
    private final List<FactionRecord> factions = new ArrayList<>();
    private final Map<String, Integer> factionIndexByKey = new HashMap<>();
    private final List<ModelRecord> units = new ArrayList<>();
    private final Map<String, Integer> unitIndexByName = new HashMap<>();
    private final Map<String, Integer> missingUnitCounts = new TreeMap<>();

    private RatDataExporter(RATGenerator ratGenerator) {
        this.ratGenerator = ratGenerator;
        buildFactionList();
        buildUnitList();
    }

    /**
     * Entry point.
     *
     * @param args see the class description
     */
    public static void main(String[] args) {
        ExportOptions options;
        try {
            options = ExportOptions.parse(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.err.println("Usage: RatDataExporter --out <folder> [--tables] [--from <year>] [--to <year>] [--no-roles]"
                  + " [--no-manifest] [--build-id <text>]");
            System.exit(2);
            return;
        }

        try {
            RATGenerator ratGenerator = waitForRatGenerator();
            new RatDataExporter(ratGenerator).export(options);
            System.exit(0);
        } catch (IOException | InterruptedException exception) {
            LOGGER.error(exception, "[RatData] Export failed");
            System.exit(1);
        }
    }

    /**
     * Loads the unit cache and the RAT Generator, and waits until every era is read.
     *
     * @return the loaded RAT Generator
     *
     * @throws InterruptedException if interrupted while waiting
     */
    static RATGenerator waitForRatGenerator() throws InterruptedException {
        MekSummaryCache.getInstance();
        RATGenerator ratGenerator = RATGenerator.getInstance();
        long deadline = System.currentTimeMillis() + INITIALIZE_TIMEOUT_MILLIS;
        while (!ratGenerator.isInitialized()) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException("The RAT Generator did not finish loading in time");
            }
            Thread.sleep(100);
        }
        return ratGenerator;
    }

    /**
     * Factions are sorted by key so every run, whatever its year range, numbers them the same way.
     */
    private void buildFactionList() {
        TreeMap<String, FactionRecord> sortedFactions = new TreeMap<>();
        for (FactionRecord faction : ratGenerator.getFactionList()) {
            sortedFactions.put(faction.getKey(), faction);
        }
        for (FactionRecord faction : sortedFactions.values()) {
            factionIndexByKey.put(faction.getKey(), factions.size());
            factions.add(faction);
        }
    }

    /**
     * Units are sorted by name so every run numbers them the same way. A unit appears once even when the RAT
     * Generator keeps it under more than one key (renamed units are stored under both names).
     */
    private void buildUnitList() {
        TreeMap<String, ModelRecord> sortedUnits = new TreeMap<>();
        for (ModelRecord modelRecord : ratGenerator.getModelList()) {
            MekSummary mekSummary = modelRecord.getMekSummary();
            if (mekSummary != null) {
                sortedUnits.put(mekSummary.getName(), modelRecord);
            }
        }
        for (Map.Entry<String, ModelRecord> entry : sortedUnits.entrySet()) {
            unitIndexByName.put(entry.getKey(), units.size());
            units.add(entry.getValue());
        }
    }

    private void export(ExportOptions options) throws IOException {
        new RatInputExporter(ratGenerator).write(options.outputFolder(), options.buildId());
        System.out.println("[RatData] Wrote inputs.json.gz");
        if (!options.includeTables()) {
            return;
        }
        TreeSet<Integer> eras = ratGenerator.getEraSet();
        int fromYear = (options.fromYear() != null) ? options.fromYear() : eras.first();
        int toYear = (options.toYear() != null) ? options.toYear() : eras.last();

        Files.createDirectories(options.outputFolder().resolve("years"));
        if (options.includeRoles()) {
            Files.createDirectories(options.outputFolder().resolve("roles"));
        }
        if (options.includeManifest()) {
            writeManifest(options, eras, fromYear, toYear);
        }

        long started = System.currentTimeMillis();
        long totalTables = 0;
        long totalReferences = 0;
        for (int year = fromYear; year <= toYear; year++) {
            RatDataShard generalShard = buildShard(year, RatDataShard.Kind.GENERAL);
            writeShard(options.outputFolder().resolve("years").resolve(year + ".bin.gz"), generalShard);
            totalTables += generalShard.getTableCount();
            totalReferences += generalShard.getReferenceCount();

            if (options.includeRoles() && eras.contains(year)) {
                RatDataShard roleShard = buildShard(year, RatDataShard.Kind.ROLES);
                writeShard(options.outputFolder().resolve("roles").resolve(year + ".bin.gz"), roleShard);
                totalTables += roleShard.getTableCount();
                totalReferences += roleShard.getReferenceCount();
            }
            System.out.printf("[RatData] %d: %d distinct tables, %d references (%.0f s so far)%n",
                  year, generalShard.getTableCount(), generalShard.getReferenceCount(),
                  (System.currentTimeMillis() - started) / 1000.0);
        }

        System.out.printf("[RatData] Done: years %d-%d, %d distinct tables, %d references%n",
              fromYear, toYear, totalTables, totalReferences);
        if (!missingUnitCounts.isEmpty()) {
            LOGGER.warn("[RatData] {} table entries named units missing from the unit list and were left out: {}",
                  missingUnitCounts.size(), missingUnitCounts);
        }
    }

    private RatDataShard buildShard(int year, RatDataShard.Kind kind) {
        RatDataShard shard = new RatDataShard(year, kind);
        for (int factionIndex = 0; factionIndex < factions.size(); factionIndex++) {
            FactionRecord faction = factions.get(factionIndex);
            if (!faction.isActiveInYear(year)) {
                continue;
            }
            List<String> ratings = ratingsFor(faction);
            for (int ratingIndex = 0; ratingIndex < ratings.size(); ratingIndex++) {
                String rating = tableRating(faction, ratings.get(ratingIndex));
                for (int unitType = 0; unitType < UnitType.SIZE; unitType++) {
                    if (kind == RatDataShard.Kind.GENERAL) {
                        addTable(shard, faction, factionIndex, ratingIndex, unitType, year, rating, null);
                    } else {
                        addRoleTables(shard, faction, factionIndex, ratingIndex, unitType, year, rating);
                    }
                }
            }
        }
        return shard;
    }

    private void addRoleTables(RatDataShard shard, FactionRecord faction, int factionIndex, int ratingIndex,
          int unitType, int year, @Nullable String rating) {
        for (MissionRole role : MissionRole.values()) {
            if (role.fitsUnitType(unitType)) {
                addTable(shard, faction, factionIndex, ratingIndex, unitType, year, rating, role);
            }
        }
    }

    private void addTable(RatDataShard shard, FactionRecord faction, int factionIndex, int ratingIndex,
          int unitType, int year, @Nullable String rating, @Nullable MissionRole role) {
        List<MissionRole> roles = new ArrayList<>();
        if (role != null) {
            roles.add(role);
        }
        UnitTable table = UnitTable.findTable(faction, unitType, year, rating, new ArrayList<>(),
              ModelRecord.NETWORK_NONE, new ArrayList<>(), roles, ROLE_STRICTNESS, faction);
        if (!table.hasUnits()) {
            return;
        }
        RatTableContent content = toContent(table);
        if (content.units().isEmpty()) {
            return;
        }
        int roleIndex = (role == null) ? RatDataShard.NO_ROLE : role.ordinal();
        shard.addTable(factionIndex, ratingIndex, unitType, roleIndex, content);
    }

    private RatTableContent toContent(UnitTable table) {
        List<RatTableContent.WeightedEntry> unitEntries = new ArrayList<>();
        List<RatTableContent.WeightedEntry> salvageEntries = new ArrayList<>();
        for (int entryIndex = 0; entryIndex < table.getNumEntries(); entryIndex++) {
            int weight = table.getEntryWeight(entryIndex);
            MekSummary mekSummary = table.getMekSummary(entryIndex);
            if (mekSummary != null) {
                Integer unitIndex = unitIndexByName.get(mekSummary.getName());
                if (unitIndex == null) {
                    missingUnitCounts.merge(mekSummary.getName(), 1, Integer::sum);
                } else {
                    unitEntries.add(new RatTableContent.WeightedEntry(unitIndex, weight));
                }
                continue;
            }
            FactionRecord salvageFaction = table.getSalvageFaction(entryIndex);
            if (salvageFaction != null) {
                Integer salvageIndex = factionIndexByKey.get(salvageFaction.getKey());
                if (salvageIndex != null) {
                    salvageEntries.add(new RatTableContent.WeightedEntry(salvageIndex, weight));
                }
            }
        }
        int salvagePct = salvageEntries.isEmpty() ? 0 : table.getSalvagePct();
        return new RatTableContent(salvagePct, unitEntries, salvageEntries);
    }

    /**
     * The ratings a faction's tables are written for, matching how MekHQ picks one: a faction with exactly one rating
     * level always uses it; any other faction uses each level of its rating system.
     */
    static List<String> ratingsFor(FactionRecord faction) {
        if (faction.getRatingLevels().size() == 1) {
            return List.of(faction.getRatingLevels().getFirst());
        }
        return List.copyOf(faction.getRatingLevelSystem());
    }

    /**
     * The rating passed to the table call. A faction with one rating level gets {@code null}, as MekHQ passes it, and
     * the RAT Generator then applies that level itself.
     */
    private static @Nullable String tableRating(FactionRecord faction, String rating) {
        return (faction.getRatingLevels().size() == 1) ? null : rating;
    }

    private static void writeShard(Path path, RatDataShard shard) throws IOException {
        try (OutputStream output = new GZIPOutputStream(Files.newOutputStream(path))) {
            output.write(shard.toBytes());
        }
    }

    private void writeManifest(ExportOptions options, TreeSet<Integer> eras, int fromYear, int toYear)
          throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("formatVersion", RatDataShard.FORMAT_VERSION);
        manifest.put("generatedAt", Instant.now().toString());
        manifest.put("megamekVersion", SuiteConstants.VERSION.toString());
        manifest.put("buildId", options.buildId());
        manifest.put("fromYear", fromYear);
        manifest.put("toYear", toYear);
        manifest.put("eras", new ArrayList<>(eras));
        manifest.put("roleStrictness", ROLE_STRICTNESS);
        manifest.put("noRole", RatDataShard.NO_ROLE);

        List<String> unitTypeNames = new ArrayList<>();
        for (int unitType = 0; unitType < UnitType.SIZE; unitType++) {
            unitTypeNames.add(UnitType.getTypeName(unitType));
        }
        manifest.put("unitTypes", unitTypeNames);

        List<String> roleNames = new ArrayList<>();
        for (MissionRole role : MissionRole.values()) {
            roleNames.add(role.toString());
        }
        manifest.put("roles", roleNames);

        List<Map<String, Object>> factionRows = new ArrayList<>();
        for (FactionRecord faction : factions) {
            factionRows.add(factionRow(faction, fromYear, toYear));
        }
        manifest.put("factions", factionRows);

        List<Map<String, Object>> unitRows = new ArrayList<>();
        for (ModelRecord unit : units) {
            unitRows.add(unitRow(unit));
        }
        manifest.put("units", unitRows);

        new ObjectMapper().writeValue(options.outputFolder().resolve("manifest.json").toFile(), manifest);
    }

    private Map<String, Object> factionRow(FactionRecord faction, int fromYear, int toYear) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", faction.getKey());
        row.put("name", faction.getName());
        row.put("namesByYear", faction.getAltNames());
        row.put("ratings", ratingsFor(faction));
        row.put("parents", faction.getParentFactions());
        row.put("clan", faction.isClan());
        row.put("periphery", faction.isPeriphery());
        row.put("minor", faction.isMinor());
        row.put("activeYears", activeRanges(faction, fromYear, toYear));
        return row;
    }

    /**
     * @return the year ranges within the export in which the faction is active, as [first, last] pairs
     */
    private static List<int[]> activeRanges(FactionRecord faction, int fromYear, int toYear) {
        List<int[]> ranges = new ArrayList<>();
        int rangeStart = -1;
        for (int year = fromYear; year <= toYear; year++) {
            boolean active = faction.isActiveInYear(year);
            if (active && (rangeStart < 0)) {
                rangeStart = year;
            } else if (!active && (rangeStart >= 0)) {
                ranges.add(new int[] { rangeStart, year - 1 });
                rangeStart = -1;
            }
        }
        if (rangeStart >= 0) {
            ranges.add(new int[] { rangeStart, toYear });
        }
        return ranges;
    }

    private static Map<String, Object> unitRow(ModelRecord unit) {
        MekSummary mekSummary = unit.getMekSummary();
        List<String> roles = new ArrayList<>();
        for (MissionRole role : unit.getRoles()) {
            roles.add(role.toString());
        }
        roles.sort(null);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("name", mekSummary.getName());
        row.put("chassis", mekSummary.getFullChassis());
        row.put("model", mekSummary.getModel());
        row.put("unitType", unit.getUnitType());
        row.put("weightClass", unit.getWeightClass());
        row.put("introYear", unit.getIntroYear());
        row.put("mulId", mekSummary.getMulId());
        row.put("omni", unit.isOmni());
        row.put("clan", unit.isClan());
        row.put("roles", roles);
        return row;
    }

    /**
     * Parsed command-line options.
     *
     * @param outputFolder    where to write the files
     * @param fromYear        first year to export, or {@code null} for the first era
     * @param toYear          last year to export, or {@code null} for the last era
     * @param includeTables   whether to write every table as well as the inputs
     * @param includeRoles    whether to write role tables for the era years
     * @param includeManifest whether to write {@code manifest.json}
     * @param buildId         free text recorded in the manifest, such as the data commit
     */
    record ExportOptions(Path outputFolder, @Nullable Integer fromYear, @Nullable Integer toYear,
          boolean includeTables, boolean includeRoles, boolean includeManifest, String buildId) {

        static ExportOptions parse(String[] args) {
            Path outputFolder = null;
            Integer fromYear = null;
            Integer toYear = null;
            boolean includeTables = false;
            boolean includeRoles = true;
            boolean includeManifest = true;
            String buildId = "";
            for (int index = 0; index < args.length; index++) {
                String argument = args[index];
                switch (argument) {
                    case "--out" -> outputFolder = Path.of(valueAfter(args, index++, argument));
                    case "--from" -> fromYear = parseYear(valueAfter(args, index++, argument));
                    case "--to" -> toYear = parseYear(valueAfter(args, index++, argument));
                    case "--tables" -> includeTables = true;
                    case "--no-roles" -> includeRoles = false;
                    case "--no-manifest" -> includeManifest = false;
                    case "--build-id" -> buildId = valueAfter(args, index++, argument);
                    default -> throw new IllegalArgumentException("Unknown argument " + argument);
                }
            }
            if (outputFolder == null) {
                throw new IllegalArgumentException("--out is required");
            }
            if ((fromYear != null) && (toYear != null) && (toYear < fromYear)) {
                throw new IllegalArgumentException("--to " + toYear + " is before --from " + fromYear);
            }
            return new ExportOptions(outputFolder, fromYear, toYear, includeTables, includeRoles, includeManifest,
                  buildId);
        }

        private static String valueAfter(String[] args, int index, String argument) {
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException(argument + " needs a value");
            }
            return args[index + 1];
        }

        private static int parseYear(String value) {
            int year = MathUtility.parseInt(value, -1);
            if (year < 0) {
                throw new IllegalArgumentException("Not a year: " + value);
            }
            return year;
        }
    }
}
