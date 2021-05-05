/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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
package megamek.client.generator;

import megamek.MegaMek;
import megamek.common.Configuration;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.util.weightedMaps.WeightedIntMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Save File Formatting:
 * callsign, weight
 * Callsign is a String that does not include a ','
 * Weight is an integer weight that is used during generation
 *
 * Future Ideas:
 * Have it generate based on the role in question, so you could have unique
 * callsigns for MechWarriors, Aerospace Jocks, Administrators, Doctors, etc.
 */
public class RandomCallsignGenerator implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 4721410214327210288L;

    private static final String CALLSIGN_FILE_NAME = "callsigns.csv";

    private static WeightedIntMap<String> callsigns;

    private static RandomCallsignGenerator rcg;

    private static volatile boolean initialized = false; // volatile to ensure readers get the current version
    //endregion Variable Declarations

    //region Constructors
    protected RandomCallsignGenerator() {

    }
    //endregion Constructors

    //region Synchronization
    /**
     * @return the instance of the RandomCallsignGenerator to use
     */
    public static synchronized RandomCallsignGenerator getInstance() {
        // only this code reads and writes 'rcg'
        if (rcg == null) {
            // synchronized ensures this will only be entered exactly once
            rcg = new RandomCallsignGenerator();
            rcg.runThreadLoader();
        }
        // when getInstance returns, rcg will always be non-null
        return rcg;
    }
    //endregion Synchronization

    //region Generation
    public String generate() {
        String callsign = "";

        if (initialized) {
            callsign = callsigns.randomItem();
        } else {
            MegaMek.getLogger().warning("Attempted to generate a callsign before the list was initialized.");
        }

        return callsign;
    }
    //endregion Generation

    //region Initialization
    private void runThreadLoader() {
        Thread loader = new Thread(() -> rcg.populateCallsigns(), "Random Callsign Generator initializer");
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    private void populateCallsigns() {
        int lineNumber = 0;
        callsigns = new WeightedIntMap<>();

        File callsignFile = new MegaMekFile(Configuration.namesDir(), CALLSIGN_FILE_NAME).getFile();

        try (InputStream is = new FileInputStream(callsignFile);
             Scanner input = new Scanner(is, StandardCharsets.UTF_8.name())) {

            // skip the first line, as that's the header
            lineNumber++;
            input.nextLine();

            while (input.hasNextLine()) {
                lineNumber++;
                String[] values = input.nextLine().split(",");
                if (values.length >= 2) {
                    callsigns.add(Integer.parseInt(values[1]), values[0]);
                } else {
                    MegaMek.getLogger().error("Not enough fields in " + callsignFile + " on " + lineNumber);
                }
            }
        } catch (Exception e) {
            MegaMek.getLogger().error("Failed to populate callsigns.", e);
        }

        initialized = true;
    }
    //endregion Initialization
}
