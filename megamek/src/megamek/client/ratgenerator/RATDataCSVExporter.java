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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
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

    /**
     * Exports all RAT data to a selectable file as an excel-optimized CSV.
     */
    public static void exportToCSV() {
        RATGenerator ratGenerator = initializeRatGenerator();
        exportToCSV(ratGenerator);
    }

    /**
     * Exports all RAT data from the given RATGenerator to a selectable file as an excel-optimized CSV.
     */
    public static void exportToCSV(RATGenerator ratGenerator) {
        File saveFile = getSaveTargetFile();
        if (saveFile == null) {
            return;
        }

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
                    var csvLine = new StringBuilder();
                    writeChassisBaseData(chassisRecord, csvLine, faction);
                    writeEraData2(ratings, csvLine, faction);
                    csvLine.append("\n");
                    bw.write(csvLine.toString());
                }

                for (ModelRecord modelRecord : chassisRecord.getModels()) {
                    ratings = getRatings(ratGenerator, modelRecord);
                    for (String faction : modelRecord.getIncludedFactions()) {
                        var csvLine = new StringBuilder();
                        writeModelBaseData(modelRecord, csvLine, faction);
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

    private static void writeModelBaseData(ModelRecord record, StringBuilder csvLine, String faction) {
        csvLine.append("\"=\"\"").append(record.getChassis()).append("\"\"\"").append(DELIMITER);
        csvLine.append("\"=\"\"").append(record.getModel()).append("\"\"\"").append(DELIMITER);
        csvLine.append("Model Data").append(DELIMITER);
        csvLine.append(record.getMekSummary().getMulId()).append(DELIMITER);
        csvLine.append(UnitType.getTypeName(record.getUnitType())).append(DELIMITER);
        csvLine.append(record.getMekSummary().getYear()).append(DELIMITER);
        csvLine.append("TBD").append(DELIMITER);
        csvLine.append(faction).append(DELIMITER);
    }

    private static void writeChassisBaseData(ChassisRecord record, StringBuilder csvLine, String faction) {
        csvLine.append("\"=\"\"").append(record.getChassis()).append("\"\"\"").append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append("Chassis Data").append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append(UnitType.getTypeName(record.getUnitType())).append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append("TBD").append(DELIMITER);
        csvLine.append(faction).append(DELIMITER);
    }

    private static RATGenerator initializeRatGenerator() {
        RATGenerator ratGenerator = RATGenerator.getInstance();
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
