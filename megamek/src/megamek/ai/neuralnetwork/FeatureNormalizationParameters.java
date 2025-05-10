/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.ai.neuralnetwork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import megamek.common.internationalization.I18n;

/**
 * FeatureNormalizationParameters is a record that holds the minimum and maximum values for input normalization for
 * the Neural Network model being run.
 * @param minValues  the minimum values array of floats
 * @param maxValues  the maximum values array of floats
 * @author Luana Coppio
 */
public record FeatureNormalizationParameters(float[] minValues, float[] maxValues) {

    public static final String FAILED_TO_PARSE_FEATURE_NORMALIZATION_LINE_NUMBER_OF_COMMA_SEPARATED_VALUES =
          "FailedToParseFeatureNormalizationLine.numberOfCommaSeparatedValues";
    public static final String FAILED_TO_PARSE_FEATURE_NORMALIZATION_VALUE =
          "FailedToParseFeatureNormalizationLine.failedToParseFeatureNormalizationValue";
    public static final String HEADER = "feature,";
    public static final String SEPARATOR = ",";
    public static final int EXPECTED_NUMBER_OF_VALUES = 3;
    public static final int FEATURE_MIN_VALUE_POSITION = 1;
    public static final int FEATURE_MAX_VALUE_POSITION = 2;

    /**
     * Creates a new FeatureNormalizationParameters object from the given file with the min and max values.
     * @param featureNormalizationCsvFile the csv file containing the min and max values for feature normalization
     * @return a new FeatureNormalizationParameters object
     */
    public static FeatureNormalizationParameters loadFile(File featureNormalizationCsvFile) {
        List<String> lines = getFeaturesLines(featureNormalizationCsvFile);
        return parseFeatureMinMaxValues(lines);
    }

    private static FeatureNormalizationParameters parseFeatureMinMaxValues(List<String> lines) {
        int size = lines.size();
        float[] minValuesTemp = new float[size];
        float[] maxValuesTemp = new float[size];

        // Process collected lines
        for (int i = 0; i < size; i++) {
            String[] values = getFeaturesMinMaxValues(lines.get(i));
            try {
                minValuesTemp[i] = Float.parseFloat(values[FEATURE_MIN_VALUE_POSITION]);
                maxValuesTemp[i] = Float.parseFloat(values[FEATURE_MAX_VALUE_POSITION]);
            } catch (NumberFormatException e) {
                throw new FailedToParseFeatureNormalizationLine(I18n.getFormattedTextAt(FailedToParseFeatureNormalizationLine.BUNDLE_NAME,
                      FAILED_TO_PARSE_FEATURE_NORMALIZATION_VALUE, i, lines.get(i)), e);
            }
        }
        return new FeatureNormalizationParameters(minValuesTemp, maxValuesTemp);
    }

    private static List<String> getFeaturesLines(File featureNormalizationCsvFile) {
        List<String> lines = new ArrayList<>();
        // Initialize normalization values
        // the normalization values are on a file named min_max_feature_normalization.csv inside the model folder

        try (var reader = new BufferedReader(new FileReader(featureNormalizationCsvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(HEADER)) {
                    lines.add(line);
                }
            }
        } catch (IOException ioException) {
            throw new FailedToLoadFeatureNormalizationFile(featureNormalizationCsvFile, ioException);
        }
        return lines;
    }

    private static String[] getFeaturesMinMaxValues(String line) {
        String[] values = line.split(SEPARATOR);
        if (values.length != EXPECTED_NUMBER_OF_VALUES) {
            throw new FailedToParseFeatureNormalizationLine(I18n.getFormattedTextAt(FailedToParseFeatureNormalizationLine.BUNDLE_NAME,
                  FAILED_TO_PARSE_FEATURE_NORMALIZATION_LINE_NUMBER_OF_COMMA_SEPARATED_VALUES, line));
        }
        return values;
    }
}
