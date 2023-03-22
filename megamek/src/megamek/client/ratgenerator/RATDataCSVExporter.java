/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ratgenerator;

import megamek.client.ui.Messages;
import megamek.common.UnitType;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class provides export for MM's RAT data to an excel-optimized CSV file for exchanging data with the MUL team.
 */
public class RATDataCSVExporter {

    private static final String DELIMITER = ";";

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

        Integer[] eras = ratGenerator.getEraSet().toArray(new Integer[0]);
        List<String> eraYears = ratGenerator.getEraSet().stream().map(era -> era + "").collect(Collectors.toList());

        try (PrintWriter pw = new PrintWriter(saveFile); BufferedWriter bw = new BufferedWriter(pw)) {
            String columnNames = "Chassis" + DELIMITER + "Model" + DELIMITER + "MUL ID" + DELIMITER + "Unit Type" + DELIMITER;
            columnNames += "Intro Date" + DELIMITER + "Faction ID" + DELIMITER + "Faction" + DELIMITER;
            columnNames += String.join(DELIMITER, eraYears) + "\n";

            bw.write(columnNames);

            for (ChassisRecord chassisRecord : ratGenerator.getChassisList()) {
                for (String faction : chassisRecord.getIncludedFactions()) {
                    var csvLine = new StringBuilder();
                    writeChassisBaseData(chassisRecord, csvLine, faction);
                    writeEraData(chassisRecord, eras, ratGenerator, csvLine, faction);
                    csvLine.append("\n");
                    bw.write(csvLine.toString());
                }

                for (ModelRecord modelRecord : chassisRecord.getModels()) {
                    for (String faction : modelRecord.getIncludedFactions()) {
                        var csvLine = new StringBuilder();
                        writeModelBaseData(modelRecord, csvLine, faction);
                        writeEraData(modelRecord, eras, ratGenerator, csvLine, faction);
                        csvLine.append("\n");
                        bw.write(csvLine.toString());
                    }
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    private static void writeModelBaseData(ModelRecord record, StringBuilder csvLine, String faction) {
        csvLine.append("\"=\"\"").append(record.getChassis()).append("\"\"\"").append(DELIMITER);
        csvLine.append("\"=\"\"").append(record.getModel()).append("\"\"\"").append(DELIMITER);
        csvLine.append(record.getMechSummary().getMulId()).append(DELIMITER);
        csvLine.append(UnitType.getTypeName(record.getUnitType())).append(DELIMITER);
        csvLine.append(record.getMechSummary().getYear()).append(DELIMITER);
        csvLine.append("TBD").append(DELIMITER);
        csvLine.append(faction).append(DELIMITER);
    }

    private static void writeChassisBaseData(ChassisRecord record, StringBuilder csvLine, String faction) {
        csvLine.append("\"=\"\"").append(record.getChassis()).append("\"\"\"").append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append(UnitType.getTypeName(record.getUnitType())).append(DELIMITER);
        csvLine.append(DELIMITER);
        csvLine.append("TBD").append(DELIMITER);
        csvLine.append(faction).append(DELIMITER);
    }

    private static void writeEraData(AbstractUnitRecord record, Integer[] eras,
                                     RATGenerator ratGenerator, StringBuilder csvLine, String faction) {
        for (int era : eras) {
            AvailabilityRating ar;
            if (record instanceof ModelRecord) {
                ar = ratGenerator.findModelAvailabilityRecord(era, record.getKey(), faction);
            } else {
                ar = ratGenerator.findChassisAvailabilityRecord(era, record.getChassisKey(), faction, era);
            }
            if (ar != null) {
                csvLine.append("\"=\"\"");
                csvLine.append(ar.getAvailabilityCode());
                if (ar.getEra() != ar.getStartYear()) {
                    csvLine.append(":").append(ar.getStartYear());
                }
                csvLine.append("\"\"\"");
            }
            csvLine.append(DELIMITER);
        }
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

    private RATDataCSVExporter() { }
}
