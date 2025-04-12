/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.ai.neuralnetwork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FeatureNormalizationParameters is a record that holds the minimum and maximum values for input normalization for
 * the Neural Network model being run.
 * @param minValues  the minimum values array of floats
 * @param maxValues  the maximum values array of floats
 * @author Luana Coppio
 */
public record FeatureNormalizationParameters(float[] minValues, float[] maxValues) {
    /**
     * Creates a new FeatureNormalizationParameters object from the given file with the min and max values.
     * @param featureNormalizationCsvFile the csv file containing the min and max values for feature normalization
     * @return a new FeatureNormalizationParameters object
     */
    public static FeatureNormalizationParameters loadFile(File featureNormalizationCsvFile) {
        List<Float> minValuesList = new ArrayList<>();
        List<Float> maxValuesList = new ArrayList<>();
        float[] minValuesTemp;
        float[] maxValuesTemp;

        int inputSize = 0;
        // Initialize normalization values
        // the normalization values are on a file named min_max_feature_normalization.csv inside the model folder
        try (var reader = new BufferedReader(new FileReader(featureNormalizationCsvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("feature,")) {
                    continue;
                }
                String[] values = line.split(",");
                if (values.length != 3) {
                    // This probably means that it reached the end of the file, but we need to throw an exception here
                    // to avoid using invalid values because otherwise it is impossible to run.
                    throw new IllegalArgumentException("Invalid line in normalization file: " + line);
                }
                minValuesList.add(Float.parseFloat(values[1]));
                maxValuesList.add(Float.parseFloat(values[2]));
                inputSize++;
            }

            minValuesTemp = new float[minValuesList.size()];
            maxValuesTemp = new float[maxValuesList.size()];

            for (int i = 0; i < inputSize; i++) {
                minValuesTemp[i] = minValuesList.get(i);
                maxValuesTemp[i] = maxValuesList.get(i);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load TensorFlow model: " + e.getMessage(), e);
        }

        return new FeatureNormalizationParameters(minValuesTemp, maxValuesTemp);
    }
}
