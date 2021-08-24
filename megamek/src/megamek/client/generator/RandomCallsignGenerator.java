/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved
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
import megamek.MegaMekConstants;
import megamek.common.util.weightedMaps.WeightedIntMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Save File Formatting:
 * callsign, weight
 * Callsign is a String that does not include a ','
 * Weight is an integer weight that is used during generation
 */
public class RandomCallsignGenerator implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 4721410214327210288L;

    private static WeightedIntMap<String> weightedCallsigns;

    private static RandomCallsignGenerator rcg;

    private static volatile boolean initialized = false; // volatile to ensure readers get the current version
    //endregion Variable Declarations

    //region Constructors
    protected RandomCallsignGenerator() {

    }
    //endregion Constructors

    //region Getters/Setters
    public static WeightedIntMap<String> getWeightedCallsigns() {
        return weightedCallsigns;
    }

    public static void setWeightedCallsigns(final WeightedIntMap<String> weightedCallsigns) {
        RandomCallsignGenerator.weightedCallsigns = weightedCallsigns;
    }
    //endregion Getters/Setters

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
            callsign = getWeightedCallsigns().randomItem();
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
        setWeightedCallsigns(new WeightedIntMap<>());
        final Map<String, Integer> callsigns = new HashMap<>();
        loadCallsignsFromFile(new File(MegaMekConstants.CALLSIGN_FILE_PATH), callsigns);
        loadCallsignsFromFile(new File(MegaMekConstants.USER_CALLSIGN_FILE_PATH), callsigns);

        for (final Map.Entry<String, Integer> entry : callsigns.entrySet()) {
            getWeightedCallsigns().add(entry.getValue(), entry.getKey());
        }

        initialized = true;
    }

    private void loadCallsignsFromFile(final File file, final Map<String, Integer> callsigns) {
        if (!file.exists()) {
            return;
        }

        int lineNumber = 0;

        try (InputStream is = new FileInputStream(file);
             Scanner input = new Scanner(is, StandardCharsets.UTF_8.name())) {

            // skip the first line, as that's the header
            lineNumber++;
            input.nextLine();

            while (input.hasNextLine()) {
                lineNumber++;
                String[] values = input.nextLine().split(",");
                if (values.length == 2) {
                    callsigns.put(values[0], Integer.parseInt(values[1]));
                } else if (values.length < 2){
                    MegaMek.getLogger().error("Not enough fields in " + file + " on " + lineNumber);
                } else {
                    MegaMek.getLogger().error("Too many fields in " + file + " on " + lineNumber);
                }
            }
        } catch (Exception e) {
            MegaMek.getLogger().error("Failed to populate callsigns from " + file, e);
        }
    }
    //endregion Initialization
}
