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
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.####");

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

        Integer[] eras = ratGenerator.getEraSet().toArray(new Integer[0]);
        List<String> eraYears = ratGenerator.getEraSet().stream().map(era -> era + "").collect(Collectors.toList());
        while (EMPTY.size() < ratGenerator.getEraSet().size()) {
            EMPTY.add("");
        }

        try (PrintWriter pw = new PrintWriter(saveFile); BufferedWriter bw = new BufferedWriter(pw)) {
            String columnNames = "Chassis" + DELIMITER + "Model" + DELIMITER + "Model/Chassis Data" + DELIMITER
                  + "MUL ID"
                  + DELIMITER + "Unit Type" + DELIMITER + "Intro Date" + DELIMITER + "Faction ID" + DELIMITER +
                  "Faction" + DELIMITER;
            columnNames += String.join(DELIMITER, eraYears) + "\n";

            bw.write(columnNames);

            for (ChassisRecord chassisRecord : ratGenerator.getChassisList()) {
                HashMap<String, List<String>> ratings = getRatings(ratGenerator, chassisRecord);
                for (String faction : chassisRecord.getIncludedFactions()) {
                    String factionName = resolveFactionName(ratGenerator, eras, ratings.get(faction), faction);
                    var csvLine = new StringBuilder();
                    writeChassisBaseData(chassisRecord, csvLine, faction, factionName);
                    writeEraData2(ratings, csvLine, faction);
                    csvLine.append("\n");
                    bw.write(csvLine.toString());
                }

                for (ModelRecord modelRecord : chassisRecord.getModels()) {
                    ratings = getRatings(ratGenerator, modelRecord);
                    for (String faction : modelRecord.getIncludedFactions()) {
                        String factionName = resolveFactionName(ratGenerator, eras, ratings.get(faction), faction);
                        var csvLine = new StringBuilder();
                        writeModelBaseData(modelRecord, csvLine, faction, factionName);
                        writeEraData2(ratings, csvLine, faction);
                        csvLine.append("\n");
                        bw.write(csvLine.toString());
                    }
                }
            }
        } catch (Exception ex) {
            logger.error(ex, "exportToCSV");
        }
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
        String csv = buildCalculatedCsv(ratGenerator, progress);
        if (progress.isCanceled()) {
            throw new CancellationException();
        }
        try (PrintWriter pw = new PrintWriter(saveFile); BufferedWriter bw = new BufferedWriter(pw)) {
            bw.write(csv);
        }
    }

    static String buildCalculatedCsv(RATGenerator ratGenerator, ExportProgress progress) {
        Integer[] eras = ratGenerator.getEraSet().toArray(new Integer[0]);
        List<ModelRecord> models = ratGenerator.getModelList().stream()
              .sorted(Comparator.comparing(ModelRecord::getKey))
              .toList();
        List<String> eraYears = ratGenerator.getEraSet().stream().map(String::valueOf).collect(Collectors.toList());
        Set<Integer> requestedUnitTypes = models.stream()
              .map(ModelRecord::getUnitType)
              .map(RATDataCSVExporter::normalizeCalculatedUnitType)
              .collect(Collectors.toSet());
          progress.update(1, "Collecting calculated RAT values...");
        Map<String, HashMap<String, List<String>>> ratingsByFaction = buildCalculatedRatings(ratGenerator,
              eras,
              requestedUnitTypes,
              progress);

        StringBuilder csv = new StringBuilder();
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

        int totalRows = 0;
        for (ModelRecord modelRecord : models) {
            for (String factionId : ratingsByFaction.keySet()) {
                List<String> ratings = ratingsByFaction.get(factionId).get(modelRecord.getKey());
                if (ratings != null && ratings.stream().anyMatch(java.util.Objects::nonNull)) {
                    totalRows += 1;
                }
            }
        }

        int writtenRows = 0;
        progress.update(90, "Writing export rows...");

        for (ModelRecord modelRecord : models) {
            for (String factionId : ratingsByFaction.keySet().stream().sorted().toList()) {
                if (progress.isCanceled()) {
                    throw new CancellationException();
                }
                List<String> ratings = ratingsByFaction.get(factionId).get(modelRecord.getKey());
                if (ratings == null || ratings.stream().allMatch(java.util.Objects::isNull)) {
                    continue;
                }

                String factionName = resolveFactionName(ratGenerator, eras, ratings, factionId);
                var csvLine = new StringBuilder();
                writeCalculatedModelBaseData(modelRecord, csvLine, factionId, factionName);
                writeEraData2(ratings, csvLine);
                csvLine.append("\n");
                csv.append(csvLine);
                writtenRows += 1;
                progress.update(90 + (int) Math.round(10.0 * writtenRows / Math.max(1, totalRows)),
                      "Writing export rows... " + writtenRows + "/" + totalRows);
            }
        }

        return csv.toString();
    }

    private static Map<String, HashMap<String, List<String>>> buildCalculatedRatings(RATGenerator ratGenerator,
          Integer[] eras,
          Set<Integer> requestedUnitTypes,
          ExportProgress progress) {
        Map<String, HashMap<String, List<String>>> ratingsByFaction = new HashMap<>();
        Map<Parameters, Map<String, Double>> tableCache = new HashMap<>();
        List<FactionRecord> factions = ratGenerator.getFactionList().stream().toList();
        int totalSteps = 0;
        for (int era : eras) {
            for (FactionRecord factionRecord : factions) {
                if (factionRecord.isActiveInYear(era)) {
                    totalSteps += requestedUnitTypes.size();
                }
            }
        }
        int processedSteps = 0;

        for (int eraIndex = 0; eraIndex < eras.length; eraIndex++) {
            int era = eras[eraIndex];
            for (FactionRecord factionRecord : factions) {
                if (!factionRecord.isActiveInYear(era)) {
                    continue;
                }

                Map<String, Double> tablePercentages = new HashMap<>();
                for (int unitType : requestedUnitTypes) {
                    if (progress.isCanceled()) {
                        throw new CancellationException();
                    }
                    Parameters params = createCalculatedParameters(factionRecord, era, unitType);
                    Map<String, Double> unitTypePercentages = calculateFinalModelPercentages(params,
                          tableCache,
                          new java.util.HashSet<>());
                    for (Map.Entry<String, Double> entry : unitTypePercentages.entrySet()) {
                        tablePercentages.merge(entry.getKey(), entry.getValue(), Double::sum);
                    }
                    processedSteps += 1;
                    progress.update(5 + (int) Math.round(85.0 * processedSteps / Math.max(1, totalSteps)),
                          "Calculating " + factionRecord.getKey() + " " + era + " " + UnitType.getTypeName(unitType)
                                + "... " + processedSteps + "/" + totalSteps);
                }

                for (Map.Entry<String, Double> entry : tablePercentages.entrySet()) {
                    ratingsByFaction
                          .computeIfAbsent(factionRecord.getKey(), key -> new HashMap<>())
                          .computeIfAbsent(entry.getKey(), key -> new ArrayList<>(Collections.nCopies(eras.length, null)))
                          .set(eraIndex, formatCalculatedAvailability(entry.getValue()));
                }
            }
        }

        return ratingsByFaction;
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

    private static void writeEraData2(List<String> ratings, StringBuilder csvLine) {
        ratings.forEach(availabilityCode -> {
            if (availabilityCode != null) {
                csvLine.append("\"=\"\"");
                csvLine.append(availabilityCode);
                csvLine.append("\"\"\"");
            }
            csvLine.append(DELIMITER);
        });
    }

    private static void writeEraData2(HashMap<String, List<String>> ratings, StringBuilder csvLine, String faction) {
        writeEraData2(ratings.get(faction), csvLine);
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
        csvLine.append("\"=\"\"").append(record.getChassis()).append("\"\"\"").append(DELIMITER);
        csvLine.append("\"=\"\"").append(record.getModel()).append("\"\"\"").append(DELIMITER);
        csvLine.append("Model Data").append(DELIMITER);
        csvLine.append(record.getMekSummary().getMulId()).append(DELIMITER);
        csvLine.append(UnitType.getTypeName(record.getUnitType())).append(DELIMITER);
        csvLine.append(record.getMekSummary().getYear()).append(DELIMITER);
        csvLine.append(factionId).append(DELIMITER);
        csvLine.append(factionName).append(DELIMITER);
    }

    private static void writeChassisBaseData(ChassisRecord record, StringBuilder csvLine, String factionId,
          String factionName) {
        csvLine.append("\"=\"\"").append(record.getChassis()).append("\"\"\"").append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append("Chassis Data").append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append(UnitType.getTypeName(record.getUnitType())).append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append(factionId).append(DELIMITER);
        csvLine.append(factionName).append(DELIMITER);
    }

    private static void writeCalculatedModelBaseData(ModelRecord record, StringBuilder csvLine, String factionId,
          String factionName) {
        csvLine.append("\"=\"\"").append(record.getChassis()).append("\"\"\"").append(DELIMITER);
        csvLine.append("\"=\"\"").append(record.getModel()).append("\"\"\"").append(DELIMITER);
        csvLine.append(record.getMekSummary().getMulId()).append(DELIMITER);
        csvLine.append(UnitType.getTypeName(record.getUnitType())).append(DELIMITER);
        csvLine.append(record.getMekSummary().getYear()).append(DELIMITER);
        csvLine.append(factionId).append(DELIMITER);
        csvLine.append(factionName).append(DELIMITER);
    }

    static String resolveFactionName(RATGenerator ratGenerator, Integer[] eras, List<String> ratings,
          String factionId) {
        FactionRecord factionRecord = ratGenerator.getFaction(factionId);
        if (factionRecord == null) {
            return factionId;
        }

        if (ratings != null) {
            for (int i = 0; i < Math.min(eras.length, ratings.size()); i++) {
                if (ratings.get(i) != null) {
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
        return PERCENT_FORMAT.format(value) + "%";
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
