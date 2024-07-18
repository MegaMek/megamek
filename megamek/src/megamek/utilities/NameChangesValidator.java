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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import megamek.common.Configuration;
import megamek.common.MechSummaryCache;

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

    private static MechSummaryCache mechSummaryCache = null;
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

    private void testEqualSides() {
        // Find equal left and right sides
        System.out.println("Looking for equal left and right sides...");
        try (FileInputStream fis = new FileInputStream(lookupNames);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr)) {
            String line;
            while (null != (line = br.readLine())) {
                if (line.startsWith("#")) {
                    continue;
                }
                int index = line.indexOf('|');
                if (index > 0) {
                    String lookupName = line.substring(0, index);
                    String entryName = line.substring(index + 1);
                    if (lookupName.equals(entryName)) {
                        System.out.println("Equal lookup name and cache entry in line: " + line);
                        errors++;
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception " + ex.getMessage());
            System.exit(64);
        }
        System.out.println("Finished.");
    }

    private void testLeftSide() {
        // Find left side entries that are present in the cache
        System.out.println("Trying to rename " + lookupNames + " to " + lookupNamesHidden);
        if (lookupNames.renameTo(lookupNamesHidden) && lookupNamesHidden.exists()) {
            System.out.println("Loading Unit Cache...");
            mechSummaryCache = MechSummaryCache.getInstance(true);
            mechSummaryCache.getAllMechs();
            System.out.println("Rename successful. Testing lookup names...");
            try (FileInputStream fis = new FileInputStream(lookupNamesHidden);
                    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr)) {
                String line;
                while (null != (line = br.readLine())) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    int index = line.indexOf('|');
                    if (index > 0) {
                        String lookupName = line.substring(0, index);
                        if (mechSummaryCache.getMech(lookupName) != null) {
                            System.out.println("Lookup name (left side) is an existing unit in line: " + line);
                            errors++;
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("Exception " + ex.getMessage());
                System.exit(64);
            }
        }

        System.out.println("Finished.");
        System.out.println("Trying to rename " + lookupNamesHidden + " back to " + lookupNames);
        if (!lookupNamesHidden.renameTo(lookupNames)) {
            System.out.println("ERROR: Could not rename! Check the files!");
            System.exit(64);
        }
        System.out.println("Rename successful.");
    }

    private void testRightSide() {
        if (lookupNames.exists()) {
            System.out.println("Testing actual names...");
            System.out.println("Reloading Unit Cache...");
            mechSummaryCache.loadMechData(true);
            mechSummaryCache.getAllMechs();
            try (FileInputStream fis = new FileInputStream(lookupNames);
                    InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr)) {
                String line;
                while (null != (line = br.readLine())) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    int index = line.indexOf('|');
                    if (index > 0) {
                        String entryName = line.substring(index + 1);
                        if (mechSummaryCache.getMech(entryName) == null) {
                            System.out.println("Actual name (right side) not found in line: " + line);
                            errors++;
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("Exception " + ex.getMessage());
                System.exit(64);
            }
        } else {
            System.out.println("Cannot find the name-changes file " + MechSummaryCache.FILENAME_LOOKUP);
            System.exit(64);
        }
        System.out.println("Finished.");
        System.exit(errors > 0 ? 1 : 0);
    }
}
