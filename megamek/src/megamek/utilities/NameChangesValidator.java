/*
 * Copyright (c) 2023, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Configuration;
import megamek.common.MechSummaryCache;
import megamek.logging.MMLogger;

/**
 * This tool goes through the name_changes.txt file and performs various tests:
 * - it finds all lines where the left and right side are equal (i.e. are
 * useless and should be deleted)
 * - it finds all lines where the left side (the out-of-date unit name that is
 * no longer an active cache entry) is, in fact, an existing cache unit name and
 * the line is unnecessary (it should be turned around or deleted)
 * - it finds all lines where the right-side entry (the real and existing unit
 * name) does not actually exist in the cache (those lines should probably be
 * kept and the right side entry corrected)
 *
 * To perform the second test, the name-changes.txt file is renamed (to
 * deactivate it - otherwise the left sides would always be found because of the
 * name-changes function itself). After the test, the rename is reversed.
 */
public class NameChangesValidator {
    private static final MMLogger logger = MMLogger.create(NameChangesValidator.class);

    private static final String STRING_FINISHED = "Finished.";

    private MechSummaryCache mechSummaryCache = null;
    private int errors;
    private final File lookupNames = new File(Configuration.unitsDir(), MechSummaryCache.FILENAME_LOOKUP);
    private final File lookupNamesHidden = new File(Configuration.unitsDir(),
            MechSummaryCache.FILENAME_LOOKUP + ".xxx");

    public static void main(String... args) {
        NameChangesValidator validator = new NameChangesValidator();
        validator.testEqualSides();
        validator.testLeftSide();
        validator.testRightSide();
    }

    private List<String> loadFile(File fileName) {
        String message = String.format("Collecting lines from file %s", fileName);
        logger.info(message);

        List<String> lines = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(fileName);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);

            String line = "";

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                lines.add(line);
            }

            br.close();
        } catch (FileNotFoundException exception) {
            logger.error(exception, "File was not found");
            System.exit(64);
        } catch (IOException exception) {
            logger.error(exception, "IO Exception");
            System.exit(64);
        }

        return lines;
    }

    private void testEqualSides() {
        // Find equal left and right sides
        logger.info("Looking for equal left and right sides...");
        List<String> lines = loadFile(lookupNames);
        for (String line : lines) {
            int index = line.indexOf('|');

            if (index > 0) {
                String lookupName = line.substring(0, index);
                String entryName = line.substring(index + 1);
                if (lookupName.equals(entryName)) {
                    String message = String.format("Equal lookup name and cache entry in line: %s", line);
                    logger.info(message);
                    errors++;
                }
            }
        }

        logger.info(STRING_FINISHED);
    }

    private void testLeftSide() {
        String message = String.format("Trying to rename %s to %s", lookupNames, lookupNamesHidden);
        logger.info(message);

        // Find left side entries that are present in the cache
        if (lookupNames.renameTo(lookupNamesHidden) && lookupNamesHidden.exists()) {
            logger.info("Loading Unit Cache...");

            mechSummaryCache = MechSummaryCache.getInstance(true);
            mechSummaryCache.getAllMechs();
            logger.info("Rename successful. Testing lookup names...");

            List<String> lines = loadFile(lookupNamesHidden);
            for (String line : lines) {
                int index = line.indexOf('|');
                if (index > 0) {
                    String lookupName = line.substring(0, index);
                    if (mechSummaryCache.getMech(lookupName) != null) {
                        message = String.format("Lookup name (left side) is an existing unit in line: %s", line);
                        logger.info(message);
                        errors++;
                    }
                }
            }
        }

        logger.info(STRING_FINISHED);
        message = String.format("Trying to rename %s back to %s", lookupNamesHidden, lookupNames);
        logger.info(message);

        if (!lookupNamesHidden.renameTo(lookupNames)) {
            logger.error("ERROR: Could not rename! Check the files!");
            System.exit(64);
        }

        logger.info("Rename successful.");
    }

    private void testRightSide() {
        if (lookupNames.exists()) {
            logger.info("Testing actual names...");
            logger.info("Reloading Unit Cache...");
            mechSummaryCache.loadMechData(true);
            mechSummaryCache.getAllMechs();
            List<String> lines = loadFile(lookupNames);
            for (String line : lines) {
                int index = line.indexOf('|');
                if (index > 0) {
                    String entryName = line.substring(index + 1);
                    if (mechSummaryCache.getMech(entryName) == null) {
                        String message = String.format("Actual name (right side) not found in line: %s", line);
                        logger.error(message);
                        errors++;
                    }
                }

            }
        } else {
            String message = String.format("Cannot find the name-changes file %s", MechSummaryCache.FILENAME_LOOKUP);
            logger.error(message);
            System.exit(64);
        }

        logger.info(STRING_FINISHED);
        System.exit(errors > 0 ? 1 : 0);
    }
}
