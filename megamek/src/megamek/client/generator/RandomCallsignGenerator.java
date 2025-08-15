/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import megamek.MMConstants;
import megamek.common.util.weightedMaps.WeightedIntMap;
import megamek.logging.MMLogger;

/**
 * Save File Formatting: callsign, weight Callsign is a String that does not include a ',' Weight is an integer weight
 * that is used during generation
 */
public class RandomCallsignGenerator {
    private final static RandomCallsignGenerator INSTANCE = new RandomCallsignGenerator();
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
                String line = input.nextLine();
                int lastCommaIndex = line.lastIndexOf(",");
                if (lastCommaIndex == -1 || line.length() == lastCommaIndex + 1) {
                    logger.debug("Not enough fields in {} on {}", file, lineNumber);
                    continue;
                }

                String[] values = { line.substring(0, lastCommaIndex), line.substring(lastCommaIndex + 1) };

                try {
                    callsigns.put(values[0], Integer.parseInt(values[1].trim()));
                } catch (NumberFormatException e) {
                    logger.warn("Invalid weight in {} on {}", file, lineNumber);
                }
            }
        } catch (Exception e) {
            logger.error(e, "Failed to populate callsigns from {}", file);
        }
    }
    // endregion Initialization
}
