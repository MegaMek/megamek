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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final String DELIMITER = ";";
    private static final ArrayList<String> EMPTY = new ArrayList<>();
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.000");

    /**
     * Exports all RAT data to a selectable file as an excel-optimized CSV.
     */
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

    private static void writeCalculatedCsvContents(Appendable csv, RATGenerator ratGenerator, ExportProgress progress)
          throws IOException {
        ExportProgress reportingProgress = new ThrottledExportProgress(progress);
        Integer[] eras = ratGenerator.getEraSet().toArray(new Integer[0]);
        List<ModelRecord> models = ratGenerator.getModelList().stream()
              .sorted(calculatedModelExportOrder())
              .toList();
        List<String> eraYears = ratGenerator.getEraSet().stream()
              .map(String::valueOf)
              .flatMap(year -> java.util.stream.Stream.of(year, year + ":S"))
              .collect(Collectors.toList());
        Set<Integer> requestedUnitTypes = models.stream()
              .map(ModelRecord::getUnitType)
              .map(RATDataCSVExporter::normalizeCalculatedUnitType)
              .collect(Collectors.toSet());
                reportingProgress.update(1, "Collecting calculated RAT values...");
                Map<Parameters, UnitTable> unitTableCache = new HashMap<>();
                Map<Parameters, Map<String, Double>> tableCache = new HashMap<>();
                Map<Parameters, Map<String, CalculatedAvailability>> separatedTableCache = new HashMap<>();
                List<FactionRecord> factions = ratGenerator.getFactionList().stream()
              .filter(factionRecord -> java.util.Arrays.stream(eras).anyMatch(factionRecord::isActiveInYear))
              .sorted(Comparator.comparing((FactionRecord factionRecord) -> factionRecord.getName(),
                  String.CASE_INSENSITIVE_ORDER)
                  .thenComparing(factionRecord -> factionRecord.getKey(), String.CASE_INSENSITIVE_ORDER))
              .toList();
        Map<String, List<CalculatedCsvRow>> rowsByModel = new HashMap<>();

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
            if (reportingProgress.isCanceled()) {
                throw new CancellationException();
            }
            FactionRecord factionRecord = factions.get(factionIndex);
            Map<String, String[]> ratingsByModel = buildCalculatedRatingsForFaction(ratGenerator,
                  factionRecord,
                  eras,
                  requestedUnitTypes,
                unitTableCache,
                separatedTableCache,
                  tableCache,
                                    reportingProgress,
                  factionIndex,
                  factions.size());
            collectCalculatedRowsForFaction(rowsByModel,
                  ratGenerator,
                  eras,
                  factionRecord,
                  ratingsByModel);
        }

        int totalRows = rowsByModel.values().stream().mapToInt(List::size).sum();
        int writtenRows = 0;
        reportingProgress.update(90, "Writing export rows...");
        for (ModelRecord modelRecord : models) {
            List<CalculatedCsvRow> rows = rowsByModel.get(modelRecord.getKey());
            if (rows == null) {
                continue;
            }
            rows.sort(Comparator.comparing(CalculatedCsvRow::factionName, String.CASE_INSENSITIVE_ORDER)
                  .thenComparing(CalculatedCsvRow::factionId, String.CASE_INSENSITIVE_ORDER));
            for (CalculatedCsvRow row : rows) {
                if (reportingProgress.isCanceled()) {
                    throw new CancellationException();
                }
                var csvLine = new StringBuilder();
                writeCalculatedModelBaseData(modelRecord, csvLine, row.factionId(), row.factionName());
                writeCalculatedEraData(row.ratings(), csvLine);
                csvLine.append("\n");
                csv.append(csvLine);
                writtenRows += 1;
                reportingProgress.update(90 + (int) Math.round(9.0 * writtenRows / Math.max(1, totalRows)),
                      "Writing export rows... " + writtenRows + "/" + totalRows);
            }
        }
    }

    private static Map<String, String[]> buildCalculatedRatingsForFaction(RATGenerator ratGenerator,
          FactionRecord factionRecord,
          Integer[] eras,
          Set<Integer> requestedUnitTypes,
            Map<Parameters, UnitTable> unitTableCache,
            Map<Parameters, Map<String, CalculatedAvailability>> separatedTableCache,
          Map<Parameters, Map<String, Double>> tableCache,
          ExportProgress progress,
          int factionIndex,
          int totalFactions) {
        Map<String, String[]> ratingsByModel = new HashMap<>();
        int totalSteps = 0;
        int eraColumnCount = eras.length * 2;
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
                        unitTableCache,
                        separatedTableCache,
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
                String[] ratings = ratingsByModel.computeIfAbsent(entry.getKey(), key -> new String[eraColumnCount]);
                if (entry.getValue().normal() > 0) {
                    ratings[eraIndex * 2] = formatCalculatedAvailability(entry.getValue().normal());
                }
                if (entry.getValue().salvage() > 0) {
                    ratings[eraIndex * 2 + 1] = formatCalculatedAvailability(entry.getValue().salvage());
                }
            }
        }

        return ratingsByModel;
    }

    private static void collectCalculatedRowsForFaction(Map<String, List<CalculatedCsvRow>> rowsByModel,
          RATGenerator ratGenerator,
          Integer[] eras,
          FactionRecord factionRecord,
          Map<String, String[]> ratingsByModel) {
        for (Map.Entry<String, String[]> row : ratingsByModel.entrySet()) {
            if (!hasAnyValue(row.getValue())) {
                continue;
            }
            String factionName = resolveFactionName(ratGenerator, eras, row.getValue(), factionRecord.getKey());
            rowsByModel.computeIfAbsent(row.getKey(), key -> new ArrayList<>())
                  .add(new CalculatedCsvRow(factionRecord.getKey(), factionName, row.getValue()));
        }
    }

    private static Map<String, CalculatedAvailability> calculateSeparatedModelPercentages(Parameters params,
          Map<Parameters, UnitTable> unitTableCache,
          Map<Parameters, Map<String, CalculatedAvailability>> separatedTableCache,
          Map<Parameters, Map<String, Double>> tableCache,
          Set<Parameters> visiting) {
        Map<String, CalculatedAvailability> cached = separatedTableCache.get(params);
        if (cached != null) {
            return cached;
        }

        UnitTable unitTable = findCalculatedUnitTable(params, unitTableCache);
        Map<String, Double> normalWeights = new HashMap<>();
        Map<String, Double> salvageWeights = new HashMap<>();
        double totalWeight = 0.0;
        for (UnitTable.TableEntry entry : unitTable.getUnitEntries()) {
            normalWeights.merge(entry.getUnitEntry().getName(), (double) entry.getWeight(), Double::sum);
            totalWeight += entry.getWeight();
        }
        for (UnitTable.TableEntry entry : unitTable.getSalvageEntries()) {
            Parameters salvageParams = createSalvageParameters(params, entry.getSalvageFaction());
            Map<String, Double> salvagePercentages = calculateFinalModelPercentages(salvageParams,
                  unitTableCache,
                  tableCache,
                  visiting);
            for (Map.Entry<String, Double> salvageEntry : salvagePercentages.entrySet()) {
                double adjustedWeight = entry.getWeight() * salvageEntry.getValue() / 100.0;
                salvageWeights.merge(salvageEntry.getKey(), adjustedWeight, Double::sum);
                totalWeight += adjustedWeight;
            }
        }

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

        separatedTableCache.put(params, percentages);
        return percentages;
    }

    private static Map<String, Double> calculateFinalModelPercentages(Parameters params,
          Map<Parameters, UnitTable> unitTableCache,
          Map<Parameters, Map<String, Double>> tableCache,
          Set<Parameters> visiting) {
        Map<String, Double> cached = tableCache.get(params);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(params)) {
            return Map.of();
        }

        UnitTable unitTable = findCalculatedUnitTable(params, unitTableCache);
        Map<String, Double> rawWeights = new HashMap<>();
        double totalWeight = 0.0;
        for (UnitTable.TableEntry entry : unitTable.getUnitEntries()) {
            rawWeights.merge(entry.getUnitEntry().getName(), (double) entry.getWeight(), Double::sum);
            totalWeight += entry.getWeight();
        }
        for (UnitTable.TableEntry entry : unitTable.getSalvageEntries()) {
            Parameters salvageParams = createSalvageParameters(params, entry.getSalvageFaction());
            Map<String, Double> salvagePercentages = calculateFinalModelPercentages(salvageParams,
                  unitTableCache,
                  tableCache,
                  visiting);
            for (Map.Entry<String, Double> salvageEntry : salvagePercentages.entrySet()) {
                double adjustedWeight = entry.getWeight() * salvageEntry.getValue() / 100.0;
                rawWeights.merge(salvageEntry.getKey(), adjustedWeight, Double::sum);
                totalWeight += adjustedWeight;
            }
        }

        Map<String, Double> percentages = new HashMap<>();
        if (totalWeight > 0) {
            for (Map.Entry<String, Double> entry : rawWeights.entrySet()) {
                percentages.put(entry.getKey(), 100.0 * entry.getValue() / totalWeight);
            }
        }

        visiting.remove(params);
        tableCache.put(params, percentages);
        return percentages;
    }

    private static UnitTable findCalculatedUnitTable(Parameters params, Map<Parameters, UnitTable> unitTableCache) {
        UnitTable cached = unitTableCache.get(params);
        if (cached != null) {
            return cached;
        }

        UnitTable unitTable = UnitTable.findTable(params);
        unitTableCache.put(params, unitTable);
        return unitTable;
    }

    private static Parameters createSalvageParameters(Parameters params, FactionRecord salvageFaction) {
        Parameters salvageParams = params.clone();
        if (salvageParams == null) {
            salvageParams = params.copy();
        }
        salvageParams.setDeployingFaction(params.getFaction());
        salvageParams.setFaction(salvageFaction);
        salvageParams.setYear(params.getYear() - 5);
        return salvageParams;
    }

    private static void writeRawEraData(List<String> ratings, StringBuilder csvLine) {
        ratings.forEach(availabilityCode -> {
            appendCsvField(csvLine, availabilityCode);
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

    private static void writeCalculatedEraData(String[] ratings, StringBuilder csvLine) {
        for (String availabilityCode : ratings) {
            appendCsvField(csvLine, availabilityCode);
            csvLine.append(DELIMITER);
        }
    }

    private static boolean hasAnyValue(String[] ratings) {
        for (String rating : ratings) {
            if (rating != null) {
                return true;
            }
        }
        return false;
    }

    private static void appendCsvField(StringBuilder csvLine, String value) {
        if (value == null) {
            return;
        }
        boolean needsQuotes = value.contains(DELIMITER)
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

    static String resolveFactionName(RATGenerator ratGenerator, Integer[] eras, String[] ratings,
          String factionId) {
        FactionRecord factionRecord = ratGenerator.getFaction(factionId);
        if (factionRecord == null) {
            return factionId;
        }

        if (ratings != null) {
            boolean hasSalvageColumns = ratings.length == eras.length * 2;
            for (int i = 0; i < eras.length; i++) {
                int baseIndex = hasSalvageColumns ? i * 2 : i;
                if ((baseIndex < ratings.length) && (ratings[baseIndex] != null)) {
                    return factionRecord.getName(eras[i]);
                }
                if (hasSalvageColumns && ((baseIndex + 1) < ratings.length) && (ratings[baseIndex + 1] != null)) {
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

    private record CalculatedAvailability(double normal, double salvage) {
        private static final CalculatedAvailability ZERO = new CalculatedAvailability(0.0, 0.0);

        private static CalculatedAvailability merge(CalculatedAvailability left, CalculatedAvailability right) {
            return new CalculatedAvailability(left.normal + right.normal, left.salvage + right.salvage);
        }
    }

    private record CalculatedCsvRow(String factionId, String factionName, String[] ratings) {
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

    private static final class ThrottledExportProgress implements ExportProgress {
        private static final long MIN_UPDATE_NANOS = 200_000_000L;

        private final ExportProgress delegate;
        private int lastPercent = Integer.MIN_VALUE;
        private long lastUpdateNanos;

        private ThrottledExportProgress(ExportProgress delegate) {
            this.delegate = delegate;
        }

        @Override
        public void update(int percent, String note) {
            if (delegate == ExportProgress.NO_OP) {
                return;
            }
            long now = System.nanoTime();
            if ((percent != lastPercent) || ((now - lastUpdateNanos) >= MIN_UPDATE_NANOS) || (percent >= 100)) {
                lastPercent = percent;
                lastUpdateNanos = now;
                delegate.update(percent, note);
            }
        }

        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }
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
