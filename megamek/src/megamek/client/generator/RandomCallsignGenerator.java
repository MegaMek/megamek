/*
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import megamek.MMConstants;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;

/**
 * Save File Formatting:
 * callsign, weight
 * Callsign is a String that does not include a ','
 * Weight is an integer weight that is used during generation
 */
public enum RandomCallsignGenerator {
    INSTANCE;

    private final static MMLogger logger = MMLogger.create(RandomCallsignGenerator.class);

    private final WeightedIntMap<String> weightedCallsigns = new WeightedIntMap<>();

    RandomCallsignGenerator() {
        final Map<String, Integer> callsigns = new HashMap<>();
        loadCallsignsFromFile(new File(MMConstants.CALLSIGN_FILE_PATH), callsigns);
        loadCallsignsFromFile(new File(MMConstants.USER_CALLSIGN_FILE_PATH), callsigns);

        for (final Map.Entry<String, Integer> entry : callsigns.entrySet()) {
            getWeightedCallsigns().add(entry.getValue(), entry.getKey());
        }
    }

    public static RandomCallsignGenerator getInstance() {
        return INSTANCE;
    }

    // region Getters/Setters
    public WeightedIntMap<String> getWeightedCallsigns() {
        return weightedCallsigns;
    }

    // region Generation
    public String generate() {
        return getWeightedCallsigns().randomItem();
    }
    // endregion Generation

    private void loadCallsignsFromFile(File file, Map<String, Integer> callsigns) {
        if (!file.exists()) {
            return;
        }

        int lineNumber = 0;

        try (InputStream is = new FileInputStream(file);
                Scanner input = new Scanner(is, StandardCharsets.UTF_8)) {
            // skip the first line, as that's the header
            lineNumber++;
            input.nextLine();

            while (input.hasNextLine()) {
                lineNumber++;
                String[] values = input.nextLine().split(",");
                if (values.length == 2) {
                    callsigns.put(values[0], Integer.parseInt(values[1].trim()));
                } else if (values.length < 2) {
                    logger.warn("Not enough fields in {} on {}",file, lineNumber);
                } else {
                    logger.warn("Too many fields in {} on {}", file, lineNumber);
                }
            }
        } catch (Exception e) {
            logger.error(e, "Failed to populate callsigns from " + file);
        }
    }
    // endregion Initialization
}
