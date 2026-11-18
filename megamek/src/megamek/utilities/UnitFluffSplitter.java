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

package megamek.utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.EntitySavingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MtfFile;
import megamek.common.loaders.UnitFluffLoader;
import megamek.common.units.Entity;
import megamek.common.units.EntityFluff;
import megamek.common.units.Mek;
import megamek.common.units.System;

/**
 * Moves the fluff texts (overview, capabilities, deployment, history, manufacturer, primary factory, fluff date and
 * the system manufacturers and models) out of unit files into separate unit fluff files that {@link UnitFluffLoader} reads. Notes
 * and all other content stay in the unit files. The unit files are changed by removing just the fluff lines or blocks,
 * so everything else in them, including comments and formatting, stays as it is.
 *
 * <p>Every changed unit is checked: the changed unit file must load to the same unit as before, minus the moved
 * fluff, and the fluff file must read back to exactly the moved fluff. Units that fail a check, can't be loaded, have
 * no UUID of their own or share their UUID with another unit file are left unchanged and listed in the report.</p>
 *
 * <p>Usage: {@code UnitFluffSplitter <unit files folder> <fluff output folder> [--dry-run]}, or
 * {@code ./gradlew :megamek:splitUnitFluff} for the mm-data units next to this repository.</p>
 */
public final class UnitFluffSplitter {

    private static final List<String> MTF_FLUFF_KEYS = List.of(MtfFile.OVERVIEW, MtfFile.CAPABILITIES,
          MtfFile.DEPLOYMENT, MtfFile.HISTORY, MtfFile.MANUFACTURER, MtfFile.PRIMARY_FACTORY, MtfFile.FLUFF_DATE,
          MtfFile.SYSTEM_MANUFACTURER, MtfFile.SYSTEM_MODEL);

    /** The old way of writing the fluff date in MTF files, as a comment; matched as exactly as MtfFile reads it. */
    private static final String MTF_LEGACY_FLUFF_DATE = "# Fluff Date: ";

    private static final List<String> BLK_FLUFF_BLOCKS = List.of("overview", "capabilities", "deployment", "history",
          "manufacturer", "primaryFactory", "fluffDate", "systemManufacturers", "systemModels");

    private static final List<String> BLK_TEXT_BLOCKS = List.of("overview", "capabilities", "deployment", "history",
          "manufacturer", "primaryFactory", "fluffDate");

    private final Path unitDirectory;
    private final Path fluffDirectory;
    private final boolean dryRun;
    private final PrintStream out;

    private final List<String> loadFailures = new ArrayList<>();
    private final List<String> missingUUIDs = new ArrayList<>();
    private final List<String> duplicateUUIDs = new ArrayList<>();
    private final List<String> verificationFailures = new ArrayList<>();
    private final List<String> ignoredLines = new ArrayList<>();
    private int unitFileCount;
    private int withoutFluffCount;
    private int splitCount;

    public static void main(String... args) throws IOException {
        if ((args.length < 2) || (args.length > 3) || ((args.length == 3) && !args[2].equals("--dry-run"))) {
            java.lang.System.out.println("Usage: UnitFluffSplitter <unit files folder> <fluff output folder> [--dry-run]");
            java.lang.System.exit(2);
        }
        EquipmentType.initializeTypes();
        var splitter = new UnitFluffSplitter(Path.of(args[0]), Path.of(args[1]), args.length == 3,
              java.lang.System.out);
        boolean success = splitter.run();
        java.lang.System.exit(success ? 0 : 1);
    }

    UnitFluffSplitter(Path unitDirectory, Path fluffDirectory, boolean dryRun, PrintStream out) {
        this.unitDirectory = unitDirectory;
        this.fluffDirectory = fluffDirectory;
        this.dryRun = dryRun;
        this.out = out;
    }

    /**
     * Splits the fluff from all unit files.
     *
     * @return {@code false} if any unit file failed its verification; the report lists them
     */
    boolean run() throws IOException {
        List<SplitPlan> plans = new ArrayList<>();
        Map<String, List<String>> filesByUUID = new HashMap<>();
        for (Path unitFile : unitFiles()) {
            unitFileCount++;
            SplitPlan plan = plan(unitFile);
            if (plan != null) {
                plans.add(plan);
                filesByUUID.computeIfAbsent(plan.uuid(), uuid -> new ArrayList<>()).add(plan.relativeName());
            }
        }

        if (!dryRun) {
            Files.createDirectories(fluffDirectory);
        }
        for (SplitPlan plan : plans) {
            List<String> sameUUID = filesByUUID.get(plan.uuid());
            if (sameUUID.size() > 1) {
                duplicateUUIDs.add(plan.relativeName() + " (" + plan.uuid() + ", also in "
                      + String.join(", ", sameUUID.stream().filter(name -> !name.equals(plan.relativeName())).toList())
                      + ")");
                continue;
            }
            Path fluffFile = fluffDirectory.resolve(plan.uuid() + UnitFluffLoader.FILE_EXTENSION);
            if (Files.exists(fluffFile)) {
                verificationFailures.add(plan.relativeName() + ": the fluff file " + fluffFile.getFileName()
                      + " already exists");
                continue;
            }
            if (!dryRun) {
                Files.writeString(fluffFile, plan.yaml(), StandardCharsets.UTF_8);
                Files.write(plan.unitFile(), plan.newContent().getBytes(StandardCharsets.UTF_8));
            }
            splitCount++;
        }
        report();
        return verificationFailures.isEmpty() && loadFailures.isEmpty();
    }

    private List<Path> unitFiles() throws IOException {
        try (Stream<Path> files = Files.walk(unitDirectory)) {
            return files.filter(Files::isRegularFile)
                  .filter(file -> isUnitFile(file.getFileName().toString()))
                  .sorted()
                  .toList();
        }
    }

    private static boolean isUnitFile(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".mtf") || lowerName.endsWith(".blk");
    }

    /**
     * @return The changes to make for the given unit file, or {@code null} if there's nothing to do or it can't be
     *       done
     */
    private SplitPlan plan(Path unitFile) {
        String relativeName = unitDirectory.relativize(unitFile).toString().replace('\\', '/');
        String content;
        Entity entity;
        try {
            content = decodeUtf8(Files.readAllBytes(unitFile));
            entity = load(content, unitFile.getFileName().toString());
        } catch (IOException | EntityLoadingException | RuntimeException exception) {
            loadFailures.add(relativeName + ": " + exception.getMessage());
            return null;
        }

        EntityFluff fluff = entity.getFluff();
        if (!UnitFluffLoader.hasFileFluff(fluff)) {
            withoutFluffCount++;
            return null;
        }
        String uuid = entity.getUnitFileUUID();
        if ((uuid == null) || !content.toLowerCase(Locale.ROOT).contains(uuid.toLowerCase(Locale.ROOT))) {
            missingUUIDs.add(relativeName);
            return null;
        }

        String newContent = isMtf(unitFile) ? removeMtfFluff(content, relativeName)
              : removeBlkFluff(content, relativeName);
        String yaml = UnitFluffLoader.toYaml(entity.getChassis(), entity.getModel(), fluff);
        String failure = verify(entity, newContent, yaml, unitFile.getFileName().toString());
        if (failure != null) {
            verificationFailures.add(relativeName + ": " + failure);
            return null;
        }
        return new SplitPlan(unitFile, relativeName, uuid.toLowerCase(Locale.ROOT), newContent, yaml);
    }

    private static boolean isMtf(Path unitFile) {
        return unitFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mtf");
    }

    private static String decodeUtf8(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(bytes))
              .toString();
    }

    private static Entity load(String content, String fileName) throws EntityLoadingException {
        var parser = new MekFileParser(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), fileName);
        return parser.getEntity();
    }

    /** @return A description of what's wrong, or {@code null} if the split is correct */
    private String verify(Entity original, String newContent, String yaml, String fileName) {
        try {
            Entity changed = load(newContent, fileName);
            if (UnitFluffLoader.hasFileFluff(changed.getFluff())) {
                return "the unit file still has fluff after removing it";
            }
            EntityFluff remainingFluff = original.getFluff();
            EntityFluff movedFluff = copyFileFluff(remainingFluff);
            clearFileFluff(remainingFluff);
            try {
                if (!Objects.equals(save(original), save(changed))) {
                    return "the unit changed by more than its fluff";
                }
            } finally {
                restoreFileFluff(remainingFluff, movedFluff);
            }

            Path yamlFile = Files.createTempFile("unitfluff", UnitFluffLoader.FILE_EXTENSION);
            try {
                Files.writeString(yamlFile, yaml, StandardCharsets.UTF_8);
                EntityFluff readBack = UnitFluffLoader.readFluffFile(yamlFile.toFile());
                String difference = fileFluffDifference(movedFluff, readBack);
                if (difference != null) {
                    return "the fluff file doesn't read back the same: " + difference;
                }
            } finally {
                Files.deleteIfExists(yamlFile);
            }
            return null;
        } catch (IOException | EntityLoadingException | EntitySavingException | RuntimeException exception) {
            return "verification error: " + exception;
        }
    }

    private static String save(Entity entity) throws EntitySavingException {
        if (entity instanceof Mek mek) {
            return mek.getMtf();
        }
        return String.join("\n", BLKFile.getBlock(entity).getAllDataAsString());
    }

    private static EntityFluff copyFileFluff(EntityFluff fluff) {
        EntityFluff copy = new EntityFluff();
        restoreFileFluff(copy, fluff);
        return copy;
    }

    private static void restoreFileFluff(EntityFluff target, EntityFluff source) {
        target.setOverview(source.getOverview());
        target.setCapabilities(source.getCapabilities());
        target.setDeployment(source.getDeployment());
        target.setHistory(source.getHistory());
        target.setManufacturer(source.getManufacturer());
        target.setPrimaryFactory(source.getPrimaryFactory());
        target.setFluffDate(source.getFluffDate());
        for (System system : System.values()) {
            target.setSystemManufacturer(system, source.getSystemManufacturer(system));
            target.setSystemModel(system, source.getSystemModel(system));
        }
    }

    private static void clearFileFluff(EntityFluff fluff) {
        restoreFileFluff(fluff, new EntityFluff());
    }

    private static String fileFluffDifference(EntityFluff expected, EntityFluff actual) {
        Map<String, String[]> fields = new HashMap<>();
        fields.put(UnitFluffLoader.OVERVIEW, new String[] { expected.getOverview(), actual.getOverview() });
        fields.put(UnitFluffLoader.CAPABILITIES, new String[] { expected.getCapabilities(), actual.getCapabilities() });
        fields.put(UnitFluffLoader.DEPLOYMENT, new String[] { expected.getDeployment(), actual.getDeployment() });
        fields.put(UnitFluffLoader.HISTORY, new String[] { expected.getHistory(), actual.getHistory() });
        fields.put(UnitFluffLoader.MANUFACTURER, new String[] { expected.getManufacturer(), actual.getManufacturer() });
        fields.put(UnitFluffLoader.PRIMARY_FACTORY,
              new String[] { expected.getPrimaryFactory(), actual.getPrimaryFactory() });
        fields.put(UnitFluffLoader.FLUFF_DATE, new String[] { expected.getFluffDate(), actual.getFluffDate() });
        for (System system : System.values()) {
            fields.put("manufacturer of " + system,
                  new String[] { expected.getSystemManufacturer(system), actual.getSystemManufacturer(system) });
            fields.put("model of " + system,
                  new String[] { expected.getSystemModel(system), actual.getSystemModel(system) });
        }
        for (Map.Entry<String, String[]> field : fields.entrySet()) {
            if (!field.getValue()[0].equals(field.getValue()[1])) {
                return field.getKey();
            }
        }
        return null;
    }

    /** Removes the fluff lines of an MTF file, along with the blank line that separates each from the next entry. */
    private String removeMtfFluff(String content, String relativeName) {
        List<String> lines = splitLines(content);
        boolean[] remove = new boolean[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            if (lineText(lines.get(i)).trim().startsWith(MTF_LEGACY_FLUFF_DATE)) {
                remove[i] = true;
                continue;
            }
            String line = lineText(lines.get(i)).trim().toLowerCase(Locale.ROOT);
            for (String key : MTF_FLUFF_KEYS) {
                if (line.startsWith(key)) {
                    remove[i] = true;
                    if ((key.equals(MtfFile.SYSTEM_MANUFACTURER) || key.equals(MtfFile.SYSTEM_MODEL))
                          && (line.split(":").length <= 2 || System.parse(line.split(":")[1]) == null)) {
                        ignoredLines.add(relativeName + ": " + lineText(lines.get(i)).trim());
                    }
                    break;
                }
            }
        }
        return joinWithoutRemoved(lines, remove, false);
    }

    /** Removes the fluff blocks of a BLK file, along with the blank line that separates each from the next block. */
    private String removeBlkFluff(String content, String relativeName) {
        List<String> lines = splitLines(content);
        boolean[] remove = new boolean[lines.size()];
        for (String block : BLK_FLUFF_BLOCKS) {
            String startTag = "<" + block + ">";
            String endTag = "</" + block + ">";
            int i = 0;
            while (i < lines.size()) {
                if (!lineText(lines.get(i)).trim().equalsIgnoreCase(startTag)) {
                    i++;
                    continue;
                }
                int end = i + 1;
                while ((end < lines.size()) && !lineText(lines.get(end)).trim().equalsIgnoreCase(endTag)) {
                    end++;
                }
                if (end == lines.size()) {
                    // No end tag; leave it alone so the verification reports the remaining fluff
                    break;
                }
                int dataLines = 0;
                for (int line = i; line <= end; line++) {
                    remove[line] = true;
                    String text = lineText(lines.get(line)).trim();
                    if ((line > i) && (line < end) && !text.isBlank() && !text.startsWith("#")) {
                        dataLines++;
                        if (!BLK_TEXT_BLOCKS.contains(block) && (text.split(":").length < 2
                              || System.parse(text.split(":")[0]) == null)) {
                            ignoredLines.add(relativeName + ": " + text);
                        }
                    }
                }
                if (BLK_TEXT_BLOCKS.contains(block) && (dataLines > 1)) {
                    ignoredLines.add(relativeName + ": <" + block + "> has " + dataLines
                          + " lines; only the first was used and moved");
                }
                i = end + 1;
            }
        }
        return joinWithoutRemoved(lines, remove, true);
    }

    /**
     * Joins the lines that aren't removed. Where removed lines leave two blank lines next to each other (or a blank
     * line at the start), the second is dropped, as the unit file writers separate fluff entries with blank lines.
     * Where they leave blank lines at the end, the file ends with a single line break; BLK files keep a final blank
     * line if they had one, as the BLK writer adds one.
     */
    private static String joinWithoutRemoved(List<String> lines, boolean[] remove, boolean keepFinalBlankLine) {
        StringBuilder result = new StringBuilder();
        boolean afterRemoved = false;
        boolean lastKeptBlank = true;
        for (int i = 0; i < lines.size(); i++) {
            if (remove[i]) {
                afterRemoved = true;
                continue;
            }
            boolean blank = lineText(lines.get(i)).isBlank();
            if (afterRemoved && blank && lastKeptBlank) {
                continue;
            }
            afterRemoved = false;
            result.append(lines.get(i));
            lastKeptBlank = blank;
        }
        if (afterRemoved) {
            // The fluff was at the end; drop the blank lines that separated it from the rest
            String text = result.toString();
            String lineBreak = text.endsWith("\r\n") ? "\r\n" : "\n";
            boolean endedWithBlankLine = keepFinalBlankLine && !lines.isEmpty() && lineText(lines.get(lines.size() - 1)).isBlank();
            return text.stripTrailing() + lineBreak + (endedWithBlankLine ? lineBreak : "");
        }
        return result.toString();
    }

    /** @return The lines of the text, each with its line break */
    private static List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines.add(content.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < content.length()) {
            lines.add(content.substring(start));
        }
        return lines;
    }

    private static String lineText(String line) {
        return line.replace("\r", "").replace("\n", "");
    }

    private void report() {
        out.println(dryRun ? "Dry run: no files were changed." : "Unit fluff split done.");
        out.println("Unit files: " + unitFileCount);
        out.println("Without fluff to move: " + withoutFluffCount);
        out.println((dryRun ? "Would split: " : "Split: ") + splitCount);
        reportList("Could not be loaded (left unchanged)", loadFailures);
        reportList("Fluff but no UUID of their own (left unchanged)", missingUUIDs);
        reportList("Same UUID as another unit file (left unchanged)", duplicateUUIDs);
        reportList("Failed verification (left unchanged)", verificationFailures);
        reportList("Fluff lines that MegaMek ignored and that were removed", ignoredLines);
    }

    private void reportList(String title, List<String> entries) {
        out.println(title + ": " + entries.size());
        for (String entry : entries) {
            out.println("  " + entry);
        }
    }

    private record SplitPlan(Path unitFile, String relativeName, String uuid, String newContent, String yaml) {}
}
