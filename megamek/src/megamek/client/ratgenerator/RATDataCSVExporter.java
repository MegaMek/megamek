/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import megamek.client.ui.Messages;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * This class provides export for MM's RAT data to an excel-optimized CSV file for exchanging data with the MUL team.
 */
public class RATDataCSVExporter {
    private static final MMLogger logger = MMLogger.create(RATDataCSVExporter.class);
    private static final DecimalFormatSymbols ROOT_DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);

    private static final String DELIMITER = ";";
    private static final ArrayList<String> EMPTY = new ArrayList<>();
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.####", ROOT_DECIMAL_SYMBOLS);
    private static final DecimalFormat DISPLAY_PERCENT_FORMAT = new DecimalFormat("0.##", ROOT_DECIMAL_SYMBOLS);
    private static final List<String> NORMALIZED_RATING_LEVELS = List.of("F", "D", "C", "B", "A");

    /**
     * Exports all RAT data to a selectable file as an excel-optimized CSV.
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public static void exportToCSV() {
        RATGenerator ratGenerator = initializeRatGenerator();
        exportToCSV(ratGenerator);
    }

    /**
     * Exports calculated RAT percentages to a selectable CSV file.
     */
    public static void exportCalculatedToCSV() {
        exportCalculatedToCSV(initializeRatGenerator());
    }

    /**
     * Exports all RAT data from the given RATGenerator to a selectable file as an excel-optimized CSV.
     */
    public static void exportToCSV(RATGenerator ratGenerator) {
        File saveFile = getSaveTargetFile();
        if (saveFile == null) {
            return;
        }

        try (PrintWriter pw = new PrintWriter(saveFile); BufferedWriter bw = new BufferedWriter(pw)) {
            bw.write(buildRawCsv(ratGenerator));
        } catch (Exception ex) {
            logger.error(ex, "exportToCSV");
        }
    }

    static String buildRawCsv(RATGenerator ratGenerator) {
        Integer[] eras = ratGenerator.getEraSet().toArray(new Integer[0]);
        List<String> eraYears = ratGenerator.getEraSet().stream().map(String::valueOf).collect(Collectors.toList());
        while (EMPTY.size() < ratGenerator.getEraSet().size()) {
            EMPTY.add("");
        }

        StringBuilder csv = new StringBuilder();
        String columnNames = "Chassis" + DELIMITER + "Model" + DELIMITER + "Model/Chassis Data" + DELIMITER
              + "MUL ID"
              + DELIMITER + "Unit Type" + DELIMITER + "Intro Date" + DELIMITER + "Faction ID" + DELIMITER
              + "Faction" + DELIMITER;
        csv.append(columnNames).append(String.join(DELIMITER, eraYears)).append("\n");

        for (ChassisRecord chassisRecord : ratGenerator.getChassisList()) {
            HashMap<String, List<String>> ratings = getRatings(ratGenerator, chassisRecord);
            for (String faction : ratings.keySet().stream().sorted().toList()) {
                String factionName = resolveFactionName(ratGenerator, eras, ratings.get(faction), faction);
                var csvLine = new StringBuilder();
                writeChassisBaseData(chassisRecord, csvLine, faction, factionName);
                writeRawEraData(ratings, csvLine, faction);
                csvLine.append("\n");
                csv.append(csvLine);
            }
        }

        for (ModelRecord modelRecord : ratGenerator.getModelList().stream()
              .sorted(Comparator.comparing(ModelRecord::getKey))
              .toList()) {
            HashMap<String, List<String>> ratings = getRatings(ratGenerator, modelRecord);
            for (String faction : ratings.keySet().stream().sorted().toList()) {
                String factionName = resolveFactionName(ratGenerator, eras, ratings.get(faction), faction);
                var csvLine = new StringBuilder();
                writeModelBaseData(modelRecord, csvLine, faction, factionName);
                writeRawEraData(ratings, csvLine, faction);
                csvLine.append("\n");
                csv.append(csvLine);
            }
        }

        return csv.toString();
    }

    /**
     * Exports calculated RAT percentages from the given RATGenerator to a CSV file.
     */
    public static void exportCalculatedToCSV(RATGenerator ratGenerator) {
        File saveFile = getSaveTargetFile();
        if (saveFile == null) {
            return;
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                writeCalculatedCsv(saveFile, initializeRatGenerator(ratGenerator), ExportProgress.NO_OP);
            } catch (Exception ex) {
                logger.error(ex, "exportCalculatedToCSV");
            }
            return;
        }

        ProgressMonitor monitor = new ProgressMonitor(null,
              "Exporting calculated RAT CSV",
              "Preparing export...",
              0,
              100);
        monitor.setMillisToDecideToPopup(0);
        monitor.setMillisToPopup(0);
        monitor.setProgress(0);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private String note = "Preparing export...";

            @Override
            protected Void doInBackground() throws Exception {
                updateProgress(0, "Initializing RAT data...");
                RATGenerator initializedRatGenerator = initializeRatGenerator(ratGenerator);
                writeCalculatedCsv(saveFile, initializedRatGenerator, new ExportProgress() {
                    @Override
                    public void update(int percent, String nextNote) {
                        updateProgress(percent, nextNote);
                    }

                    @Override
                    public boolean isCanceled() {
                        return isCancelled() || monitor.isCanceled();
                    }
                });
                updateProgress(100, "Export complete.");
                return null;
            }

            private void updateProgress(int percent, String nextNote) {
                note = nextNote;
                setProgress(Math.max(0, Math.min(100, percent)));
                firePropertyChange("progressNote", null, nextNote);
            }

            @Override
            protected void done() {
                monitor.close();
                try {
                    get();
                } catch (CancellationException ignored) {
                    // User canceled the export.
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    logger.error(ex.getCause() == null ? ex : ex.getCause(), "exportCalculatedToCSV");
                }
            }
        };

        worker.addPropertyChangeListener(event -> {
            if ("progress".equals(event.getPropertyName())) {
                monitor.setProgress((Integer) event.getNewValue());
            } else if ("progressNote".equals(event.getPropertyName())) {
                monitor.setNote(String.valueOf(event.getNewValue()));
            }
            if (monitor.isCanceled() && !worker.isCancelled()) {
                worker.cancel(true);
            }
        });
        worker.execute();
    }

    static String buildCalculatedCsv(RATGenerator ratGenerator) {
        return buildCalculatedCsv(ratGenerator, ExportProgress.NO_OP);
    }

    private static void writeCalculatedCsv(File saveFile, RATGenerator ratGenerator, ExportProgress progress)
          throws IOException {
        try (PrintWriter pw = new PrintWriter(saveFile); BufferedWriter bw = new BufferedWriter(pw)) {
            writeCalculatedCsvContents(bw, ratGenerator, progress);
        }
    }

    static String buildCalculatedCsv(RATGenerator ratGenerator, ExportProgress progress) {
        StringBuilder csv = new StringBuilder();
        try {
            writeCalculatedCsvContents(csv, ratGenerator, progress);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to build calculated CSV", ex);
        }
        return csv.toString();
    }

    public static List<CalculatedModelRow> buildCalculatedRowsForModel(RATGenerator ratGenerator,
          ModelRecord modelRecord) {
                return buildCalculatedRowsForModel(ratGenerator, modelRecord, false, CalculationProgress.NO_OP);
        }

        public static List<CalculatedModelRow> buildCalculatedRowsForModel(RATGenerator ratGenerator,
                    ModelRecord modelRecord,
                    boolean useTargetedPreview) {
                return buildCalculatedRowsForModel(ratGenerator, modelRecord, useTargetedPreview, CalculationProgress.NO_OP);
        }

        public static List<CalculatedModelRow> buildCalculatedRowsForModel(RATGenerator ratGenerator,
                    ModelRecord modelRecord,
                    CalculationProgress progress) {
                return buildCalculatedRowsForModel(ratGenerator, modelRecord, false, progress);
        }

        public static List<CalculatedModelRow> buildCalculatedRowsForModel(RATGenerator ratGenerator,
                    ModelRecord modelRecord,
                    boolean useTargetedPreview,
                    CalculationProgress progress) {
        Integer[] eras = ratGenerator.getEraSet().toArray(new Integer[0]);
        int unitType = normalizeCalculatedUnitType(modelRecord.getUnitType());
        Map<Parameters, Map<String, Double>> tableCache = new HashMap<>();
                Map<Parameters, Map<String, CalculatedAvailability>> separatedTableCache = new HashMap<>();
                Map<Parameters, CalculatedAvailability> targetedSeparatedCache = new HashMap<>();
                Map<Parameters, TargetWeightTotals> targetedFinalCache = new HashMap<>();
        List<FactionRecord> factions = ratGenerator.getFactionList().stream()
              .filter(factionRecord -> java.util.Arrays.stream(eras).anyMatch(factionRecord::isActiveInYear))
              .sorted(Comparator.comparing((FactionRecord factionRecord) -> factionRecord.getName(),
                    String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(factionRecord -> factionRecord.getKey(), String.CASE_INSENSITIVE_ORDER))
              .toList();
        List<CalculatedModelRow> rows = new ArrayList<>();
                progress.update(0, "Preparing faction calculations...");

                for (int factionIndex = 0; factionIndex < factions.size(); factionIndex++) {
            if (progress.isCanceled()) {
                throw new CancellationException();
            }
            FactionRecord factionRecord = factions.get(factionIndex);
            String[] ratings;
            try {
                                ratings = useTargetedPreview
                    ? buildCalculatedTargetedDisplayRatingsForFaction(ratGenerator,
                        modelRecord,
                        factionRecord,
                        eras,
                        unitType,
                        progress,
                        factionIndex,
                        factions.size(),
                        targetedSeparatedCache,
                        targetedFinalCache)
                    : buildCalculatedDisplayRatingsForFaction(ratGenerator,
                        modelRecord,
                        factionRecord,
                        eras,
                        unitType,
                        tableCache,
                        separatedTableCache,
                        progress,
                        factionIndex,
                        factions.size());
            } catch (RuntimeException ex) {
                logger.warn(ex,
                      "Skipping calculated row for " + modelRecord.getKey() + " " + factionRecord.getKey());
                continue;
            }
            if (java.util.Arrays.stream(ratings).allMatch(value -> (value == null) || value.isBlank())) {
                continue;
            }
            String factionName = resolveFactionName(ratGenerator,
                  eras,
                  new ArrayList<>(java.util.Arrays.asList(ratings)),
                  factionRecord.getKey());
            rows.add(new CalculatedModelRow(factionRecord.getKey(), factionName, ratings));
        }

        progress.update(100, "Calculated rows ready.");

        return rows;
    }

    private static void writeCalculatedCsvContents(Appendable csv, RATGenerator ratGenerator, ExportProgress progress)
          throws IOException {
        Integer[] eras = ratGenerator.getEraSet().toArray(new Integer[0]);
        List<ModelRecord> models = ratGenerator.getModelList().stream()
              .sorted(calculatedModelExportOrder())
              .toList();
                Map<String, ModelRecord> modelsByKey = models.stream()
              .collect(Collectors.toMap(ModelRecord::getKey, modelRecord -> modelRecord, (left, right) -> left,
                                        LinkedHashMap::new));
                Map<String, Integer> modelOrder = new HashMap<>();
                for (int index = 0; index < models.size(); index++) {
            modelOrder.put(models.get(index).getKey(), index);
                }
        List<String> eraYears = ratGenerator.getEraSet().stream()
              .map(String::valueOf)
              .flatMap(year -> java.util.stream.Stream.of(year, year + ":S"))
              .collect(Collectors.toList());
        Set<Integer> requestedUnitTypes = models.stream()
              .map(ModelRecord::getUnitType)
              .map(RATDataCSVExporter::normalizeCalculatedUnitType)
              .collect(Collectors.toSet());
        progress.update(1, "Collecting calculated RAT values...");
                Map<Parameters, Map<String, Double>> tableCache = new HashMap<>();
                List<FactionRecord> factions = ratGenerator.getFactionList().stream()
              .filter(factionRecord -> java.util.Arrays.stream(eras).anyMatch(factionRecord::isActiveInYear))
              .sorted(Comparator.comparing((FactionRecord factionRecord) -> factionRecord.getName(),
                                        String.CASE_INSENSITIVE_ORDER)
                  .thenComparing(factionRecord -> factionRecord.getKey(), String.CASE_INSENSITIVE_ORDER))
              .toList();

        csv.append(String.join(DELIMITER,
              "Chassis",
              "Model",
              "MUL ID",
              "Unit Type",
              "Intro Date",
              "Faction ID",
              "Faction",
              String.join(DELIMITER, eraYears)));
        csv.append("\n");

        for (int factionIndex = 0; factionIndex < factions.size(); factionIndex++) {
            if (progress.isCanceled()) {
                throw new CancellationException();
            }
            FactionRecord factionRecord = factions.get(factionIndex);
            Map<String, List<String>> ratingsByModel = buildCalculatedRatingsForFaction(ratGenerator,
                  factionRecord,
                  eras,
                  requestedUnitTypes,
                  tableCache,
                  progress,
                  factionIndex,
                  factions.size());
            List<CalculatedCsvModelRow> rows = collectCalculatedRowsForFaction(
                  ratGenerator,
                  eras,
                  factionRecord,
                  ratingsByModel);
            rows.sort(Comparator.comparingInt((CalculatedCsvModelRow row) -> modelOrder.getOrDefault(row.modelKey(),
                        Integer.MAX_VALUE))
                  .thenComparing(CalculatedCsvModelRow::factionName, String.CASE_INSENSITIVE_ORDER)
                  .thenComparing(CalculatedCsvModelRow::factionId, String.CASE_INSENSITIVE_ORDER));
            int factionProgressBase = 90 + (int) Math.round(9.0 * factionIndex / Math.max(1, factions.size()));
            for (CalculatedCsvModelRow row : rows) {
                if (progress.isCanceled()) {
                    throw new CancellationException();
                }
                ModelRecord modelRecord = modelsByKey.get(row.modelKey());
                if (modelRecord == null) {
                    continue;
                }
                var csvLine = new StringBuilder();
                writeCalculatedModelBaseData(modelRecord, csvLine, row.factionId(), row.factionName());
                writeCalculatedEraData(row.ratings(), csvLine);
                csvLine.append("\n");
                csv.append(csvLine);
            }
            progress.update(Math.min(99, factionProgressBase + 1),
                  "Wrote " + factionRecord.getKey() + " rows (" + rows.size() + ")");
        }
        progress.update(100, "Export complete.");
    }

    private static Map<String, List<String>> buildCalculatedRatingsForFaction(RATGenerator ratGenerator,
          FactionRecord factionRecord,
          Integer[] eras,
          Set<Integer> requestedUnitTypes,
          Map<Parameters, Map<String, Double>> tableCache,
          ExportProgress progress,
          int factionIndex,
          int totalFactions) {
        Map<String, List<String>> ratingsByModel = new HashMap<>();
        int totalSteps = 0;
        for (int era : eras) {
            if (factionRecord.isActiveInYear(era)) {
                totalSteps += requestedUnitTypes.size();
            }
        }
        int processedSteps = 0;
        double factionStart = 5.0 + (85.0 * factionIndex / Math.max(1, totalFactions));
        double factionSpan = 85.0 / Math.max(1, totalFactions);
        double calculateSpan = factionSpan * 0.75;

        for (int eraIndex = 0; eraIndex < eras.length; eraIndex++) {
            int era = eras[eraIndex];
            if (!factionRecord.isActiveInYear(era)) {
                continue;
            }

            Map<String, CalculatedAvailability> tablePercentages = new HashMap<>();
            for (int unitType : requestedUnitTypes) {
                if (progress.isCanceled()) {
                    throw new CancellationException();
                }
                Parameters params = createCalculatedParameters(factionRecord, era, unitType);
                Map<String, CalculatedAvailability> unitTypePercentages = calculateSeparatedModelPercentages(params,
                      tableCache,
                      new java.util.HashSet<>());
                for (Map.Entry<String, CalculatedAvailability> entry : unitTypePercentages.entrySet()) {
                    tablePercentages.merge(entry.getKey(), entry.getValue(), CalculatedAvailability::merge);
                }
                processedSteps += 1;
                progress.update((int) Math.round(factionStart + calculateSpan * processedSteps / Math.max(1, totalSteps)),
                      "Calculating " + factionRecord.getKey() + " " + era + " " + UnitType.getTypeName(unitType)
                            + "... " + processedSteps + "/" + totalSteps);
            }

            for (Map.Entry<String, CalculatedAvailability> entry : tablePercentages.entrySet()) {
                List<String> ratings = ratingsByModel
                      .computeIfAbsent(entry.getKey(), key -> new ArrayList<>(Collections.nCopies(eras.length * 2, null)));
                if (entry.getValue().normal() > 0) {
                    ratings.set(eraIndex * 2, formatCalculatedAvailability(entry.getValue().normal()));
                }
                if (entry.getValue().salvage() > 0) {
                    ratings.set(eraIndex * 2 + 1, formatCalculatedAvailability(entry.getValue().salvage()));
                }
            }
        }

        return ratingsByModel;
    }

    private static List<CalculatedCsvModelRow> collectCalculatedRowsForFaction(
          RATGenerator ratGenerator,
          Integer[] eras,
          FactionRecord factionRecord,
          Map<String, List<String>> ratingsByModel) {
        List<CalculatedCsvModelRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<String>> row : ratingsByModel.entrySet()) {
            if (row.getValue().stream().noneMatch(Objects::nonNull)) {
                continue;
            }
            String factionName = resolveFactionName(ratGenerator, eras, row.getValue(), factionRecord.getKey());
            rows.add(new CalculatedCsvModelRow(row.getKey(), factionRecord.getKey(), factionName, row.getValue()));
        }
        return rows;
    }

    private static String[] buildCalculatedDisplayRatingsForFaction(RATGenerator ratGenerator,
          ModelRecord modelRecord,
          FactionRecord factionRecord,
          Integer[] eras,
          int unitType,
          Map<Parameters, Map<String, Double>> tableCache,
          Map<Parameters, Map<String, CalculatedAvailability>> separatedTableCache,
          CalculationProgress progress,
          int factionIndex,
          int totalFactions) {
        String[] ratings = new String[eras.length * 2];
        int activeEraCount = (int) java.util.Arrays.stream(eras)
              .filter(factionRecord::isActiveInYear)
              .count();
        int processedEras = 0;

        for (int eraIndex = 0; eraIndex < eras.length; eraIndex++) {
            if (progress.isCanceled()) {
                throw new CancellationException();
            }
            int era = eras[eraIndex];
            AvailabilityRating modelAvailability = ratGenerator.findModelAvailabilityRecord(era,
                  modelRecord.getKey(),
                  factionRecord);
            AvailabilityRating chassisAvailability = ratGenerator.findChassisAvailabilityRecord(era,
                  modelRecord.getChassisKey(),
                  factionRecord,
                  era);
            if ((modelAvailability == null) || (chassisAvailability == null)) {
                continue;
            }
            processedEras += 1;
            progress.update(calculateDisplayProgress(factionIndex, totalFactions, processedEras, activeEraCount),
                "Checking " + factionRecord.getKey() + " " + era + " (" + processedEras + "/"
                    + Math.max(1, activeEraCount) + " eras)");

            boolean splitRatings = shouldSplitCalculatedRatings(factionRecord, modelAvailability, chassisAvailability);
            List<String> ratingLevels = splitRatings ? factionRecord.getRatingLevelSystem() : List.of();
            List<String> normalizedLevels = splitRatings ? normalizeRatingLevels(ratingLevels.size()) : List.of();

            if (splitRatings) {
                List<String> normalValues = new ArrayList<>();
                List<String> normalDisplay = new ArrayList<>();
                List<String> salvageValues = new ArrayList<>();
                List<String> salvageDisplay = new ArrayList<>();
                for (int ratingIndex = 0; ratingIndex < ratingLevels.size(); ratingIndex++) {
                    Parameters params = createCalculatedParameters(factionRecord, era, unitType);
                    params.setRating(ratingLevels.get(ratingIndex));
                  CalculatedAvailability availability = getSeparatedModelPercentages(params,
                      tableCache,
                      separatedTableCache).get(modelRecord.getKey());
                    if (availability == null) {
                        continue;
                    }
                    collectDisplayValue(normalizedLevels.get(ratingIndex),
                          availability.normal(),
                          normalValues,
                          normalDisplay);
                    collectDisplayValue(normalizedLevels.get(ratingIndex),
                          availability.salvage(),
                          salvageValues,
                          salvageDisplay);
                }
                ratings[eraIndex * 2] = joinCalculatedDisplayValues(normalValues, normalDisplay);
                ratings[eraIndex * 2 + 1] = joinCalculatedDisplayValues(salvageValues, salvageDisplay);
            } else {
                CalculatedAvailability availability = getSeparatedModelPercentages(
                      createCalculatedParameters(factionRecord, era, unitType),
                      tableCache,
                      separatedTableCache).get(modelRecord.getKey());
                if (availability == null) {
                    continue;
                }
                if (availability.normal() > 0) {
                    ratings[eraIndex * 2] = formatCalculatedDisplayPercent(availability.normal());
                }
                if (availability.salvage() > 0) {
                    ratings[eraIndex * 2 + 1] = formatCalculatedDisplayPercent(availability.salvage());
                }
            }
        }

        return ratings;
    }

    private static int calculateDisplayProgress(int factionIndex,
          int totalFactions,
          int processedEras,
          int activeEraCount) {
        double factionProgress = (double) factionIndex / Math.max(1, totalFactions);
        double eraProgress = (double) processedEras / Math.max(1, activeEraCount);
        return Math.min(99, (int) Math.round((factionProgress + (eraProgress / Math.max(1, totalFactions))) * 100.0));
    }

    private static boolean shouldSplitCalculatedRatings(FactionRecord factionRecord,
          AvailabilityRating modelAvailability,
          AvailabilityRating chassisAvailability) {
        if ((factionRecord.getRatingLevelSystem().size() <= 1) || (factionRecord.getRatingLevels().size() == 1)) {
            return false;
        }
        return modelAvailability.hasMultipleRatings()
              || (modelAvailability.getRatingAdjustment() != 0)
              || chassisAvailability.hasMultipleRatings()
              || (chassisAvailability.getRatingAdjustment() != 0);
    }

    private static String[] buildCalculatedTargetedDisplayRatingsForFaction(RATGenerator ratGenerator,
          ModelRecord modelRecord,
          FactionRecord factionRecord,
          Integer[] eras,
          int unitType,
          CalculationProgress progress,
          int factionIndex,
          int totalFactions,
          Map<Parameters, CalculatedAvailability> targetedSeparatedCache,
          Map<Parameters, TargetWeightTotals> targetedFinalCache) {
        String[] ratings = new String[eras.length * 2];
        int activeEraCount = (int) java.util.Arrays.stream(eras)
              .filter(factionRecord::isActiveInYear)
              .count();
        int processedEras = 0;

        for (int eraIndex = 0; eraIndex < eras.length; eraIndex++) {
            if (progress.isCanceled()) {
                throw new CancellationException();
            }
            int era = eras[eraIndex];
            AvailabilityRating modelAvailability = ratGenerator.findModelAvailabilityRecord(era,
                  modelRecord.getKey(),
                  factionRecord);
            AvailabilityRating chassisAvailability = ratGenerator.findChassisAvailabilityRecord(era,
                  modelRecord.getChassisKey(),
                  factionRecord,
                  era);
            if ((modelAvailability == null) || (chassisAvailability == null)) {
                continue;
            }
            processedEras += 1;
            progress.update(calculateDisplayProgress(factionIndex, totalFactions, processedEras, activeEraCount),
                  "Checking " + factionRecord.getKey() + " " + era + " (" + processedEras + "/"
                        + Math.max(1, activeEraCount) + " eras)");

            boolean splitRatings = shouldSplitCalculatedRatings(factionRecord, modelAvailability, chassisAvailability);
            List<String> ratingLevels = splitRatings ? factionRecord.getRatingLevelSystem() : List.of();
            List<String> normalizedLevels = splitRatings ? normalizeRatingLevels(ratingLevels.size()) : List.of();

            if (splitRatings) {
                List<String> normalValues = new ArrayList<>();
                List<String> normalDisplay = new ArrayList<>();
                List<String> salvageValues = new ArrayList<>();
                List<String> salvageDisplay = new ArrayList<>();
                for (int ratingIndex = 0; ratingIndex < ratingLevels.size(); ratingIndex++) {
                    Parameters params = createCalculatedParameters(factionRecord, era, unitType);
                    params.setRating(ratingLevels.get(ratingIndex));
                    CalculatedAvailability availability = getTargetedSeparatedModelPercentages(params,
                          modelRecord.getKey(),
                          targetedSeparatedCache,
                          targetedFinalCache);
                    collectDisplayValue(normalizedLevels.get(ratingIndex),
                          availability.normal(),
                          normalValues,
                          normalDisplay);
                    collectDisplayValue(normalizedLevels.get(ratingIndex),
                          availability.salvage(),
                          salvageValues,
                          salvageDisplay);
                }
                ratings[eraIndex * 2] = joinCalculatedDisplayValues(normalValues, normalDisplay);
                ratings[eraIndex * 2 + 1] = joinCalculatedDisplayValues(salvageValues, salvageDisplay);
            } else {
                CalculatedAvailability availability = getTargetedSeparatedModelPercentages(
                      createCalculatedParameters(factionRecord, era, unitType),
                      modelRecord.getKey(),
                      targetedSeparatedCache,
                      targetedFinalCache);
                if (availability.normal() > 0) {
                    ratings[eraIndex * 2] = formatCalculatedDisplayPercent(availability.normal());
                }
                if (availability.salvage() > 0) {
                    ratings[eraIndex * 2 + 1] = formatCalculatedDisplayPercent(availability.salvage());
                }
            }
        }

        return ratings;
    }

    private static List<String> normalizeRatingLevels(int ratingCount) {
        List<String> normalizedLevels = new ArrayList<>(ratingCount);
        for (int index = 0; index < ratingCount; index++) {
            int normalizedIndex = (int) Math.round((double) index * (NORMALIZED_RATING_LEVELS.size() - 1)
                  / Math.max(1, ratingCount - 1));
            normalizedLevels.add(NORMALIZED_RATING_LEVELS.get(normalizedIndex));
        }
        return normalizedLevels;
    }

    private static void collectDisplayValue(String ratingLabel,
          double value,
          List<String> rawValues,
          List<String> displayValues) {
        if (value <= 0) {
            return;
        }
        String formattedValue = formatCalculatedDisplayPercent(value);
        rawValues.add(formattedValue);
        displayValues.add(ratingLabel + ": " + formattedValue);
    }

    private static String joinCalculatedDisplayValues(List<String> rawValues, List<String> displayValues) {
        if (rawValues.isEmpty()) {
            return null;
        }
        if (new LinkedHashSet<>(rawValues).size() == 1) {
            return rawValues.get(0);
        }
        return String.join("\n", displayValues);
    }

    private static Map<String, CalculatedAvailability> calculateSeparatedModelPercentages(Parameters params,
          Map<Parameters, Map<String, Double>> tableCache,
          Set<Parameters> visiting) {
        UnitTable unitTable = UnitTable.findTable(params);
        Map<String, Double> normalWeights = new HashMap<>();
        Map<String, Double> salvageWeights = new HashMap<>();
        for (UnitTable.TableEntry entry : unitTable.getEntries()) {
            if (entry.isUnit()) {
                normalWeights.merge(entry.getUnitEntry().getName(), (double) entry.getWeight(), Double::sum);
            } else {
                Parameters salvageParams = new Parameters(entry.getSalvageFaction(),
                      params.getUnitType(),
                      params.getYear() - 5,
                      params.getRating(),
                      params.getWeightClasses(),
                      params.getNetworkMask(),
                      params.getMovementModes(),
                      params.getRoles(),
                      params.getRoleStrictness(),
                      params.getFaction());
                Map<String, Double> salvagePercentages = calculateFinalModelPercentages(salvageParams,
                      tableCache,
                      visiting);
                for (Map.Entry<String, Double> salvageEntry : salvagePercentages.entrySet()) {
                    salvageWeights.merge(salvageEntry.getKey(),
                          entry.getWeight() * salvageEntry.getValue() / 100.0,
                          Double::sum);
                }
            }
        }

        double totalWeight = normalWeights.values().stream().mapToDouble(Double::doubleValue).sum()
              + salvageWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, CalculatedAvailability> percentages = new HashMap<>();
        if (totalWeight > 0) {
            for (Map.Entry<String, Double> entry : normalWeights.entrySet()) {
                percentages.put(entry.getKey(),
                      new CalculatedAvailability(100.0 * entry.getValue() / totalWeight, 0.0));
            }
            for (Map.Entry<String, Double> entry : salvageWeights.entrySet()) {
                CalculatedAvailability existing = percentages.getOrDefault(entry.getKey(), CalculatedAvailability.ZERO);
                percentages.put(entry.getKey(),
                      new CalculatedAvailability(existing.normal(), 100.0 * entry.getValue() / totalWeight));
            }
        }

        return percentages;
    }

    private static Map<String, CalculatedAvailability> getSeparatedModelPercentages(Parameters params,
          Map<Parameters, Map<String, Double>> tableCache,
          Map<Parameters, Map<String, CalculatedAvailability>> separatedTableCache) {
        Parameters cacheKey = params.copy();
        Map<String, CalculatedAvailability> cached = separatedTableCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Map<String, CalculatedAvailability> calculated = calculateSeparatedModelPercentages(params,
              tableCache,
              new HashSet<>());
        separatedTableCache.put(cacheKey, calculated);
        return calculated;
    }

    private static CalculatedAvailability getTargetedSeparatedModelPercentages(Parameters params,
          String modelKey,
          Map<Parameters, CalculatedAvailability> targetedSeparatedCache,
          Map<Parameters, TargetWeightTotals> targetedFinalCache) {
        Parameters cacheKey = params.copy();
        CalculatedAvailability cached = targetedSeparatedCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        CalculatedAvailability calculated = calculateTargetedSeparatedModelPercentages(params,
              modelKey,
              targetedFinalCache,
              new HashSet<>());
        targetedSeparatedCache.put(cacheKey, calculated);
        return calculated;
    }

    private static CalculatedAvailability calculateTargetedSeparatedModelPercentages(Parameters params,
          String modelKey,
          Map<Parameters, TargetWeightTotals> targetedFinalCache,
          Set<Parameters> visiting) {
        UnitTable unitTable = UnitTable.findTable(params);
        double targetNormalWeight = 0.0;
        double targetSalvageWeight = 0.0;
        double totalWeight = 0.0;

        for (UnitTable.TableEntry entry : unitTable.getEntries()) {
            if (entry.isUnit()) {
                totalWeight += entry.getWeight();
                if (modelKey.equals(entry.getUnitEntry().getName())) {
                    targetNormalWeight += entry.getWeight();
                }
            } else {
                Parameters salvageParams = new Parameters(entry.getSalvageFaction(),
                      params.getUnitType(),
                      params.getYear() - 5,
                      params.getRating(),
                      params.getWeightClasses(),
                      params.getNetworkMask(),
                      params.getMovementModes(),
                      params.getRoles(),
                      params.getRoleStrictness(),
                      params.getFaction());
                TargetWeightTotals salvageWeights = calculateTargetedFinalModelWeights(salvageParams,
                      modelKey,
                      targetedFinalCache,
                      visiting);
                if (salvageWeights.totalWeight() > 0) {
                    totalWeight += entry.getWeight();
                    targetSalvageWeight += entry.getWeight() * salvageWeights.targetWeight()
                          / salvageWeights.totalWeight();
                }
            }
        }

        if (totalWeight <= 0) {
            return CalculatedAvailability.ZERO;
        }

        return new CalculatedAvailability(100.0 * targetNormalWeight / totalWeight,
              100.0 * targetSalvageWeight / totalWeight);
    }

    private static Map<String, Double> calculateFinalModelPercentages(Parameters params,
          Map<Parameters, Map<String, Double>> tableCache,
          Set<Parameters> visiting) {
        Parameters cacheKey = params.copy();
        Map<String, Double> cached = tableCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(cacheKey)) {
            return Map.of();
        }

        UnitTable unitTable = UnitTable.findTable(params);
        Map<String, Double> rawWeights = new HashMap<>();
        for (UnitTable.TableEntry entry : unitTable.getEntries()) {
            if (entry.isUnit()) {
                rawWeights.merge(entry.getUnitEntry().getName(), (double) entry.getWeight(), Double::sum);
            } else {
                Parameters salvageParams = new Parameters(entry.getSalvageFaction(),
                      params.getUnitType(),
                      params.getYear() - 5,
                      params.getRating(),
                      params.getWeightClasses(),
                      params.getNetworkMask(),
                      params.getMovementModes(),
                      params.getRoles(),
                      params.getRoleStrictness(),
                      params.getFaction());
                Map<String, Double> salvagePercentages = calculateFinalModelPercentages(salvageParams,
                      tableCache,
                      visiting);
                for (Map.Entry<String, Double> salvageEntry : salvagePercentages.entrySet()) {
                    rawWeights.merge(salvageEntry.getKey(),
                          entry.getWeight() * salvageEntry.getValue() / 100.0,
                          Double::sum);
                }
            }
        }

        double totalWeight = rawWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> percentages = new HashMap<>();
        if (totalWeight > 0) {
            for (Map.Entry<String, Double> entry : rawWeights.entrySet()) {
                percentages.put(entry.getKey(), 100.0 * entry.getValue() / totalWeight);
            }
        }

        visiting.remove(cacheKey);
        tableCache.put(cacheKey, percentages);
        return percentages;
    }

    private static TargetWeightTotals calculateTargetedFinalModelWeights(Parameters params,
          String modelKey,
          Map<Parameters, TargetWeightTotals> targetedFinalCache,
          Set<Parameters> visiting) {
        Parameters cacheKey = params.copy();
        TargetWeightTotals cached = targetedFinalCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(cacheKey)) {
            return TargetWeightTotals.ZERO;
        }

        UnitTable unitTable = UnitTable.findTable(params);
        double targetWeight = 0.0;
        double totalWeight = 0.0;
        for (UnitTable.TableEntry entry : unitTable.getEntries()) {
            if (entry.isUnit()) {
                totalWeight += entry.getWeight();
                if (modelKey.equals(entry.getUnitEntry().getName())) {
                    targetWeight += entry.getWeight();
                }
            } else {
                Parameters salvageParams = new Parameters(entry.getSalvageFaction(),
                      params.getUnitType(),
                      params.getYear() - 5,
                      params.getRating(),
                      params.getWeightClasses(),
                      params.getNetworkMask(),
                      params.getMovementModes(),
                      params.getRoles(),
                      params.getRoleStrictness(),
                      params.getFaction());
                TargetWeightTotals salvageWeights = calculateTargetedFinalModelWeights(salvageParams,
                      modelKey,
                      targetedFinalCache,
                      visiting);
                if (salvageWeights.totalWeight() > 0) {
                    totalWeight += entry.getWeight();
                    targetWeight += entry.getWeight() * salvageWeights.targetWeight() / salvageWeights.totalWeight();
                }
            }
        }

        visiting.remove(cacheKey);
        TargetWeightTotals calculated = new TargetWeightTotals(targetWeight, totalWeight);
        targetedFinalCache.put(cacheKey, calculated);
        return calculated;
    }

    private static void writeRawEraData(List<String> ratings, StringBuilder csvLine) {
        ratings.forEach(availabilityCode -> {
            appendRawAvailabilityField(csvLine, availabilityCode);
            csvLine.append(DELIMITER);
        });
    }

    private static void writeRawEraData(HashMap<String, List<String>> ratings, StringBuilder csvLine, String faction) {
        writeRawEraData(ratings.get(faction), csvLine);
    }

    private static HashMap<String, List<String>> getRatings(RATGenerator ratGenerator, AbstractUnitRecord record) {
        HashMap<String, List<String>> data = new HashMap<>();
        Integer[] ERAS = ratGenerator.getEraSet().toArray(new Integer[0]);
        for (int i = 0; i < ERAS.length; i++) {
            Collection<AvailabilityRating> recs;
            if (record instanceof ModelRecord) {
                recs = ratGenerator.getModelFactionRatings(ERAS[i], record.getKey());
            } else {
                recs = ratGenerator.getChassisFactionRatings(ERAS[i], record.getChassisKey());
            }
            if (recs != null) {
                for (AvailabilityRating rec : recs) {
                    String key = rec.getFactionCode();
                    if (!data.containsKey(key)) {
                        data.put(key, new ArrayList<>(EMPTY));
                    }
                    if (rec.getEra() == rec.getStartYear()) {
                        data.get(key).set(i, rec.getAvailabilityCode());
                    } else {
                        data.get(key).set(i, rec.getAvailabilityCode() + ":" + rec.getStartYear());
                    }
                }
            }
        }
        return data;
    }

    private static void writeModelBaseData(ModelRecord record, StringBuilder csvLine, String factionId,
          String factionName) {
                appendCsvField(csvLine, record.getChassis());
                csvLine.append(DELIMITER);
                appendCsvField(csvLine, record.getModel());
                csvLine.append(DELIMITER);
                appendCsvField(csvLine, "Model Data");
                csvLine.append(DELIMITER);
                appendCsvField(csvLine, Integer.toString(record.getMekSummary().getMulId()));
                csvLine.append(DELIMITER);
                appendCsvField(csvLine, UnitType.getTypeName(record.getUnitType()));
                csvLine.append(DELIMITER);
                appendCsvField(csvLine, Integer.toString(record.getMekSummary().getYear()));
                csvLine.append(DELIMITER);
                appendCsvField(csvLine, factionId);
                csvLine.append(DELIMITER);
                appendCsvField(csvLine, factionName);
                csvLine.append(DELIMITER);
    }

    private static void writeChassisBaseData(ChassisRecord record, StringBuilder csvLine, String factionId,
          String factionName) {
                appendCsvField(csvLine, record.getChassis());
        csvLine.append(DELIMITER);
                appendCsvField(csvLine, "Chassis Data");
                csvLine.append(DELIMITER);
        csvLine.append(DELIMITER);
                appendCsvField(csvLine, UnitType.getTypeName(record.getUnitType()));
                csvLine.append(DELIMITER);
        csvLine.append(DELIMITER);
                appendCsvField(csvLine, factionId);
                csvLine.append(DELIMITER);
                appendCsvField(csvLine, factionName);
                csvLine.append(DELIMITER);
    }

    private static void writeCalculatedModelBaseData(ModelRecord record, StringBuilder csvLine, String factionId,
          String factionName) {
        appendCsvField(csvLine, record.getChassis());
        csvLine.append(DELIMITER);
        appendCsvField(csvLine, record.getModel());
        csvLine.append(DELIMITER);
        appendCsvField(csvLine, Integer.toString(record.getMekSummary().getMulId()));
        csvLine.append(DELIMITER);
        appendCsvField(csvLine, UnitType.getTypeName(record.getUnitType()));
        csvLine.append(DELIMITER);
        appendCsvField(csvLine, Integer.toString(record.getMekSummary().getYear()));
        csvLine.append(DELIMITER);
        appendCsvField(csvLine, factionId);
        csvLine.append(DELIMITER);
        appendCsvField(csvLine, factionName);
        csvLine.append(DELIMITER);
    }

    private static void writeCalculatedEraData(List<String> ratings, StringBuilder csvLine) {
        ratings.forEach(availabilityCode -> {
            appendCsvField(csvLine, availabilityCode);
            csvLine.append(DELIMITER);
        });
    }

    private static void appendRawAvailabilityField(StringBuilder csvLine, String value) {
        appendCsvField(csvLine, value, shouldQuoteRawAvailability(value));
    }

    private static boolean shouldQuoteRawAvailability(String value) {
        return (value != null) && !value.isBlank() && !value.matches("\\d+(?:\\.\\d+)?");
    }

    private static void appendCsvField(StringBuilder csvLine, String value) {
        appendCsvField(csvLine, value, false);
    }

    private static void appendCsvField(StringBuilder csvLine, String value, boolean forceQuotes) {
        if (value == null) {
            return;
        }
        boolean needsQuotes = forceQuotes
              || value.contains(DELIMITER)
              || value.contains("\"")
              || value.contains("\n")
              || value.contains("\r");
        if (needsQuotes) {
            csvLine.append('"');
            csvLine.append(value.replace("\"", "\"\""));
            csvLine.append('"');
        } else {
            csvLine.append(value);
        }
    }

    static String resolveFactionName(RATGenerator ratGenerator, Integer[] eras, List<String> ratings,
          String factionId) {
        FactionRecord factionRecord = ratGenerator.getFaction(factionId);
        if (factionRecord == null) {
            return factionId;
        }

        if (ratings != null) {
            boolean hasSalvageColumns = ratings.size() == eras.length * 2;
            for (int i = 0; i < eras.length; i++) {
                int baseIndex = hasSalvageColumns ? i * 2 : i;
                if ((baseIndex < ratings.size()) && (ratings.get(baseIndex) != null)) {
                    return factionRecord.getName(eras[i]);
                }
                if (hasSalvageColumns && ((baseIndex + 1) < ratings.size()) && (ratings.get(baseIndex + 1) != null)) {
                    return factionRecord.getName(eras[i]);
                }
            }
        }

        return factionRecord.getName();
    }

    private static Parameters createCalculatedParameters(FactionRecord factionRecord, int year, int unitType) {
        return new Parameters(factionRecord,
              unitType,
              year,
              null,
              List.of(),
              0,
              List.of(),
              List.of(),
              0,
              factionRecord);
    }

    private static int normalizeCalculatedUnitType(int unitType) {
        return (unitType == UnitType.AERO) ? UnitType.AEROSPACE_FIGHTER : unitType;
    }

    static Comparator<ModelRecord> calculatedModelExportOrder() {
        return Comparator.comparing(ModelRecord::getChassis, String.CASE_INSENSITIVE_ORDER)
              .thenComparing(ModelRecord::getModel, String.CASE_INSENSITIVE_ORDER)
              .thenComparing(ModelRecord::getKey, String.CASE_INSENSITIVE_ORDER);
    }

    private static RATGenerator initializeRatGenerator() {
        return initializeRatGenerator(RATGenerator.getInstance());
    }

    private static RATGenerator initializeRatGenerator(RATGenerator ratGenerator) {
        while (!ratGenerator.isInitialized()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                // Do nothing
            }
        }
        ratGenerator.getEraSet().forEach(ratGenerator::loadYear);
        ratGenerator.initRemainingUnits();
        return ratGenerator;
    }

    static String formatCalculatedAvailability(double value) {
        return PERCENT_FORMAT.format(value / 100.0);
    }

    static String formatCalculatedDisplayPercent(double value) {
        return DISPLAY_PERCENT_FORMAT.format(value) + "%";
    }

    private record CalculatedAvailability(double normal, double salvage) {
        private static final CalculatedAvailability ZERO = new CalculatedAvailability(0.0, 0.0);

        private static CalculatedAvailability merge(CalculatedAvailability left, CalculatedAvailability right) {
            return new CalculatedAvailability(left.normal + right.normal, left.salvage + right.salvage);
        }
    }

    private record TargetWeightTotals(double targetWeight, double totalWeight) {
        private static final TargetWeightTotals ZERO = new TargetWeightTotals(0.0, 0.0);
    }
    
    private record CalculatedCsvModelRow(String modelKey, String factionId, String factionName, List<String> ratings) {
    }

    public record CalculatedModelRow(String factionId, String factionName, String[] ratings) {
    }

    public interface CalculationProgress {
        CalculationProgress NO_OP = new CalculationProgress() {
            @Override
            public void update(int percent, String note) {
            }

            @Override
            public boolean isCanceled() {
                return false;
            }
        };

        void update(int percent, String note);

        boolean isCanceled();
    }

    private interface ExportProgress {
        ExportProgress NO_OP = new ExportProgress() {
            @Override
            public void update(int percent, String note) {
            }

            @Override
            public boolean isCanceled() {
                return false;
            }
        };

        void update(int percent, String note);

        boolean isCanceled();
    }

    private static File getSaveTargetFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(Messages.getString("BoardEditor.saveBoardAs"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return (dir.getName().endsWith(".csv") || dir.isDirectory());
            }

            @Override
            public String getDescription() {
                return "*.csv";
            }
        });
        int returnVal = fc.showSaveDialog(null);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return null;
        }
        File choice = fc.getSelectedFile();
        if (!choice.getName().toLowerCase().endsWith(".csv")) {
            try {
                choice = new File(choice.getCanonicalPath() + ".csv");
            } catch (IOException ignored) {
                return null;
            }
        }
        return choice;
    }

    private RATDataCSVExporter() {
    }
}