package megamek.client.ratgenerator;

import megamek.client.ui.Messages;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

public class RATDataCSVExporter {

    private static final String DELIMITER = ";";


    public static void exportToCSV() {
        RATGenerator ratGenerator = initializeRatGenerator();
        exportToCSV(ratGenerator);
    }

    public static void exportToCSV(RATGenerator ratGenerator) {
        File saveFile = getSaveFile();
        if (saveFile == null) {
            return;
        }

        Integer[] eras = ratGenerator.getEraSet().toArray(new Integer[0]);
        List<String> eraYears = ratGenerator.getEraSet().stream().map(era -> era + "").collect(Collectors.toList());

        try (PrintWriter pw = new PrintWriter(saveFile); BufferedWriter bw = new BufferedWriter(pw)) {
            String columnNames = "Chassis" + DELIMITER + "Model" + DELIMITER + "MUL ID" + DELIMITER;
            columnNames += "Intro Date" + DELIMITER + "Faction ID" + DELIMITER + "Faction" + DELIMITER;
            columnNames += String.join(DELIMITER, eraYears) + "\n";

            bw.write(columnNames);

            for (ModelRecord record : ratGenerator.getModelList()) {
                for (String faction : record.getIncludedFactions()) {
                    var csvLine = new StringBuilder();
                    csvLine.append(record.getChassis()).append(DELIMITER);
                    csvLine.append(record.getModel()).append(DELIMITER);
                    csvLine.append(record.getMechSummary().getMulId()).append(DELIMITER);
                    csvLine.append(record.getMechSummary().getYear()).append(DELIMITER);
                    csvLine.append("TBD").append(DELIMITER);
                    csvLine.append(faction).append(DELIMITER);
                    for (int era : eras) {
                        csvLine.append("\"=\"\"");
                        AvailabilityRating ar = ratGenerator.findModelAvailabilityRecord(era, record.getKey(), faction);
                        if (ar != null) {
                            csvLine.append(ar.getAvailabilityCode());
                            if (ar.getEra() != ar.getStartYear()) {
                                csvLine.append(":").append(ar.getStartYear());
                            }
                        } else {
                            csvLine.append("--");
                        }
                        csvLine.append("\"\"\"");
                        csvLine.append(DELIMITER);
                    }
                    csvLine.append("\n");
                    bw.write(csvLine.toString());
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
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

    /**
     * Shows a dialog for choosing a .board file to save to.
     * Sets curBoardFile and returns true when a valid file was chosen.
     * Returns false otherwise.
     */
    private static File getSaveFile() {
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
//        saveDialogSize(fc);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return null;
        }
        File choice = fc.getSelectedFile();
        // make sure the file ends in board
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
