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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import megamek.client.ui.Messages;
import megamek.common.loaders.MekSummary;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * This class provides export for MM's RAT data to an excel-optimized CSV file for exchanging data with the MUL team.
 */
public class RATDataCSVExporter {
    private static final MMLogger logger = MMLogger.create(RATDataCSVExporter.class);

    private static final String DELIMITER = ";";
    private static final String NONE_OPTION = "(none)";
    private static final ArrayList<String> EMPTY = new ArrayList<>();

    /**
     * Exports all RAT data to a selectable file as an excel-optimized CSV.
     */
    public static void exportToCSV() {
        RATGenerator ratGenerator = initializeRatGenerator();
        exportToCSV(ratGenerator);
    }

    /**
     * Exports a generated RAT table, including final weights and salvage, to a selectable CSV file.
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
                    String factionName = resolveFactionName(ratGenerator, eras, ratings, faction);
                    var csvLine = new StringBuilder();
                    writeChassisBaseData(chassisRecord, csvLine, faction, factionName);
                    writeEraData2(ratings, csvLine, faction);
                    csvLine.append("\n");
                    bw.write(csvLine.toString());
                }

                for (ModelRecord modelRecord : chassisRecord.getModels()) {
                    ratings = getRatings(ratGenerator, modelRecord);
                    for (String faction : modelRecord.getIncludedFactions()) {
                        String factionName = resolveFactionName(ratGenerator, eras, ratings, faction);
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
     * Prompts for a parameter set and exports the generated RAT table for that selection.
     */
    public static void exportCalculatedToCSV(RATGenerator ratGenerator) {
        RATGenerator initializedRatGenerator = initializeRatGenerator(ratGenerator);
        Parameters params = promptForCalculatedExportParameters(initializedRatGenerator);
        if (params == null) {
            return;
        }

        exportCalculatedToCSV(initializedRatGenerator, params);
    }

    /**
     * Exports a generated RAT table for the provided parameters.
     */
    public static void exportCalculatedToCSV(Parameters params) {
        exportCalculatedToCSV(initializeRatGenerator(), params);
    }

    /**
     * Exports a generated RAT table for the provided parameters using the supplied RAT generator state.
     */
    public static void exportCalculatedToCSV(RATGenerator ratGenerator, Parameters params) {
        File saveFile = getSaveTargetFile();
        if (saveFile == null) {
            return;
        }

        try (PrintWriter pw = new PrintWriter(saveFile); BufferedWriter bw = new BufferedWriter(pw)) {
            bw.write(buildCalculatedCsv(UnitTable.findTable(params), params));
        } catch (Exception ex) {
            logger.error(ex, "exportCalculatedToCSV");
        }
    }

    static String buildCalculatedCsv(UnitTable unitTable, Parameters params) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(DELIMITER,
              "Faction ID",
              "Faction",
              "Deploying Faction ID",
              "Deploying Faction",
              "Year",
              "Rating",
              "Requested Unit Type",
              "Entry Type",
              "Entry",
              "Source Faction ID",
              "Chassis",
              "Model",
              "MUL ID",
              "Tech Base",
              "Role",
              "Weight",
              "Weight %"));
        csv.append("\n");

        int totalWeight = unitTable.getTotalWeight();
        for (UnitTable.TableEntry entry : unitTable.getEntries()) {
            csv.append(buildCalculatedCsvRow(entry, params, totalWeight));
            csv.append("\n");
        }

        return csv.toString();
    }

    static String buildCalculatedCsvRow(UnitTable.TableEntry entry, Parameters params, int totalWeight) {
        StringBuilder csvLine = new StringBuilder();
        appendCsvValue(csvLine, params.getFaction().getKey());
        appendCsvValue(csvLine, params.getFaction().getName(params.getYear()));
        appendCsvValue(csvLine, params.getDeployingFaction().getKey());
        appendCsvValue(csvLine, params.getDeployingFaction().getName(params.getYear()));
        appendCsvValue(csvLine, params.getYear());
        appendCsvValue(csvLine, params.getRating() == null ? "" : params.getRating());
        appendCsvValue(csvLine, UnitType.getTypeName(params.getUnitType()));

        if (entry.isUnit()) {
            MekSummary unitEntry = entry.getUnitEntry();
            appendCsvValue(csvLine, "Unit");
            appendCsvValue(csvLine, unitEntry.getName());
            appendCsvValue(csvLine, "");
            appendCsvValue(csvLine, unitEntry.getChassis());
            appendCsvValue(csvLine, unitEntry.getModel());
            appendCsvValue(csvLine, unitEntry.getMulId());
            appendCsvValue(csvLine, unitEntry.isClan() ? "Clan" : "IS");
            appendCsvValue(csvLine, unitEntry.getRole().toString());
        } else {
            FactionRecord salvageFaction = entry.getSalvageFaction();
            appendCsvValue(csvLine, "Salvage");
            appendCsvValue(csvLine, salvageFaction.getName(params.getYear() - 5));
            appendCsvValue(csvLine, salvageFaction.getKey());
            appendCsvValue(csvLine, "");
            appendCsvValue(csvLine, "");
            appendCsvValue(csvLine, "");
            appendCsvValue(csvLine, salvageFaction.isClan() ? "Clan" : "IS");
            appendCsvValue(csvLine, "");
        }

        appendCsvValue(csvLine, entry.getWeight());
        appendCsvRaw(csvLine, formatWeightPercent(entry.getWeight(), totalWeight));
        return csvLine.toString();
    }

    private static void writeEraData2(HashMap<String, List<String>> ratings, StringBuilder csvLine, String faction) {
        ratings.get(faction).forEach(availabilityCode -> {
            if (availabilityCode != null) {
                csvLine.append("\"=\"\"");
                csvLine.append(availabilityCode);
                csvLine.append("\"\"\"");
            }
            csvLine.append(DELIMITER);
        });
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

    static String resolveFactionName(RATGenerator ratGenerator, Integer[] eras, HashMap<String, List<String>> ratings,
          String factionId) {
        FactionRecord factionRecord = ratGenerator.getFaction(factionId);
        if (factionRecord == null) {
            return factionId;
        }

        List<String> factionRatings = ratings.get(factionId);
        if (factionRatings != null) {
            for (int i = 0; i < Math.min(eras.length, factionRatings.size()); i++) {
                if (factionRatings.get(i) != null) {
                    return factionRecord.getName(eras[i]);
                }
            }
        }

        return factionRecord.getName();
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

    private static Parameters promptForCalculatedExportParameters(RATGenerator ratGenerator) {
        List<FactionRecord> factions = ratGenerator.getFactionList().stream()
              .sorted(Comparator.comparing(FactionRecord::getName, String.CASE_INSENSITIVE_ORDER))
              .toList();
        if (factions.isEmpty() || ratGenerator.getEraSet().isEmpty()) {
            return null;
        }

        JComboBox<FactionRecord> factionChooser = new JComboBox<>(factions.toArray(new FactionRecord[0]));
        JComboBox<FactionRecord> deployingFactionChooser = new JComboBox<>(factions.toArray(new FactionRecord[0]));
        JComboBox<Integer> yearChooser = new JComboBox<>(ratGenerator.getEraSet().toArray(new Integer[0]));
        JComboBox<Integer> unitTypeChooser = new JComboBox<>();
        JComboBox<String> ratingChooser = new JComboBox<>();

        for (int unitType = 0; unitType < UnitType.SIZE; unitType++) {
            unitTypeChooser.addItem(unitType);
        }

        factionChooser.setRenderer((list, value, index, isSelected, cellHasFocus) ->
              new JLabel((value == null) ? "" : value.getName() + " (" + value.getKey() + ")"));
        deployingFactionChooser.setRenderer((list, value, index, isSelected, cellHasFocus) ->
              new JLabel((value == null) ? "" : value.getName() + " (" + value.getKey() + ")"));
        unitTypeChooser.setRenderer((list, value, index, isSelected, cellHasFocus) ->
              new JLabel((value == null) ? "" : UnitType.getTypeName(value)));

        FactionRecord defaultFaction = factions.get(0);
        factionChooser.setSelectedItem(defaultFaction);
        deployingFactionChooser.setSelectedItem(defaultFaction);
        yearChooser.setSelectedItem(ratGenerator.getEraSet().last());
        unitTypeChooser.setSelectedItem(UnitType.MEK);
        updateRatingChooser(ratingChooser, defaultFaction);

        factionChooser.addActionListener(ev -> updateRatingChooser(ratingChooser,
              (FactionRecord) factionChooser.getSelectedItem()));

        JPanel panel = new JPanel(new java.awt.GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Faction"));
        panel.add(factionChooser);
        panel.add(new JLabel("Deploying Faction"));
        panel.add(deployingFactionChooser);
        panel.add(new JLabel("Year"));
        panel.add(yearChooser);
        panel.add(new JLabel("Unit Type"));
        panel.add(unitTypeChooser);
        panel.add(new JLabel("Rating"));
        panel.add(ratingChooser);

        int option = JOptionPane.showConfirmDialog(null,
              panel,
              "Export Calculated RAT CSV",
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE);
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }

        FactionRecord faction = (FactionRecord) factionChooser.getSelectedItem();
        FactionRecord deployingFaction = (FactionRecord) deployingFactionChooser.getSelectedItem();
        Integer year = (Integer) yearChooser.getSelectedItem();
        Integer unitType = (Integer) unitTypeChooser.getSelectedItem();
        if (faction == null || year == null || unitType == null) {
            return null;
        }

        if (deployingFaction == null) {
            deployingFaction = faction;
        }

        String rating = normalizeRatingSelection((String) ratingChooser.getSelectedItem());
        return new Parameters(faction,
              unitType,
              year,
              rating,
              List.of(),
              0,
              List.of(),
              List.of(),
              0,
              deployingFaction);
    }

    private static void updateRatingChooser(JComboBox<String> ratingChooser, FactionRecord faction) {
        String previousSelection = (String) ratingChooser.getSelectedItem();
        ratingChooser.removeAllItems();
        ratingChooser.addItem(NONE_OPTION);
        if (faction != null) {
            faction.getRatingLevelSystem().forEach(ratingChooser::addItem);
        }

        if (previousSelection != null) {
            ratingChooser.setSelectedItem(previousSelection);
        }
        if (ratingChooser.getSelectedItem() == null) {
            ratingChooser.setSelectedIndex(0);
        }
    }

    private static String normalizeRatingSelection(String rating) {
        return (rating == null || NONE_OPTION.equals(rating)) ? null : rating;
    }

    private static String formatWeightPercent(int weight, int totalWeight) {
        if (totalWeight <= 0) {
            return "0";
        }

        return new DecimalFormat("0.####").format(100.0 * weight / totalWeight);
    }

    private static void appendCsvValue(StringBuilder csvLine, Object value) {
        if (csvLine.length() > 0) {
            csvLine.append(DELIMITER);
        }

        if (value instanceof Number) {
            csvLine.append(value);
        } else {
            String text = (value == null) ? "" : value.toString();
            csvLine.append('"').append(text.replace("\"", "\"\"")).append('"');
        }
    }

    private static void appendCsvRaw(StringBuilder csvLine, String value) {
        if (csvLine.length() > 0) {
            csvLine.append(DELIMITER);
        }
        csvLine.append(value);
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
