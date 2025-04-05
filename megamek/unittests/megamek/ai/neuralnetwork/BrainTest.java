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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import megamek.client.bot.caspar.ClassificationScore;
import megamek.client.bot.caspar.MovementClassification;
import megamek.common.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BrainTest {

    private static Brain BRAIN;
    private static final Map<MovementClassification, Integer> ERROR_COUNTS =
          new EnumMap<>(MovementClassification.class);
    private static int TOTAL_TESTS = 0;
    private static final float ACCEPTABLE_ERROR_PERCENT = 0.5f;
    private static final BrainRegistry DEFAULT_BRAIN = new BrainRegistry("default", 3);

    @BeforeAll
    public static void setUp() {
        resetTrackers();
        BRAIN = Brain.loadBrain(DEFAULT_BRAIN);

    }

    @Test
    void testLoadBrain() {
        assertNotNull(BRAIN, "Brain should not be null");
        assertTrue(Brain.testTensorFlow());
        assertTrue(BRAIN.getInputSize() > 0, "Brain input size should be greater than 0");
        assertTrue(BRAIN.getOutputSize() > 0, "Brain output size should be greater than 0");
    }

    @ParameterizedTest
    @MethodSource(value = "testValidationInputs")
    void testCalculatingMoves(TestValue testValue) {
        var prediction = BRAIN.predict(testValue.input());
        MovementClassification predictedClassification = ClassificationScore.fromPrediction(prediction).getClassification();
        if (!predictedClassification.equals(testValue.classification())) {
            ERROR_COUNTS.merge(testValue.classification(), 1, Integer::sum);
        }
        TOTAL_TESTS++;
    }

    @AfterAll
    public static void afterAllTests() {
        if (TOTAL_TESTS > 0) {
            assertClassifications();
        }
        BRAIN = null;
        resetTrackers();
    }

    private static void assertClassifications() {
        int threshold = (int) (TOTAL_TESTS * ACCEPTABLE_ERROR_PERCENT);

        assertAll("Distribution check",
              () -> assertTrue(ERROR_COUNTS.getOrDefault(MovementClassification.OFFENSIVE, 0) < threshold,
                    "Classification (" + MovementClassification.OFFENSIVE + ") ,"
                          + " error count: " + ERROR_COUNTS.getOrDefault(MovementClassification.OFFENSIVE, 0) + ","
                          + " total tests: " + TOTAL_TESTS + " threshold: " + threshold),
              () -> assertTrue(ERROR_COUNTS.getOrDefault(MovementClassification.DEFENSIVE, 0) < threshold,
                    "Classification (" + MovementClassification.DEFENSIVE + ") ,"
                          + " error count: " + ERROR_COUNTS.getOrDefault(MovementClassification.DEFENSIVE, 0) + ","
                          + " total tests: " + TOTAL_TESTS + " threshold: " + threshold),
              () -> assertTrue(ERROR_COUNTS.getOrDefault(MovementClassification.HOLD_POSITION, 0) < threshold,
                    "Classification (" + MovementClassification.HOLD_POSITION + ") ,"
                          + " error count: " + ERROR_COUNTS.getOrDefault(MovementClassification.HOLD_POSITION, 0) + ","
                          + " total tests: " + TOTAL_TESTS + " threshold: " + threshold)
        );
    }

    private static void resetTrackers() {
        ERROR_COUNTS.clear();
        TOTAL_TESTS = 0;
    }

    /**
     * TestValue is a record that holds the input values and their corresponding classification.
     * @param input The input values as a float array
     * @param classification The classification of the input values as a {@link MovementClassification}
     */
    private record TestValue(float[] input, MovementClassification classification) {}

    /**
     * Get the test values for the model.
     * @return A stream of TestValue objects containing the input values and their corresponding classification
     */
    static Stream<TestValue> testValidationInputs() {
        return loadTestValues().stream();
    }

    /**
     * Load the test values from the CSV file that accompany the brain model.
     * @return A collection of TestValue objects containing the input values and their corresponding classification
     */
    private static Collection<TestValue> loadTestValues() {
        List<TestValue> testInputs = new ArrayList<>();
        Path testInputFilePath = Path.of(Configuration.aiBrainFolderPath(DEFAULT_BRAIN.name()).toString(),
              "test_inputs.csv");
        try (var reader = new BufferedReader(new FileReader(testInputFilePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                float[] entry = getInputsFromSplitLine(values);
                MovementClassification res = getMovementClassificationFromSplitLine(values);
                testInputs.add(new TestValue(entry, res));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load TensorFlow model: " + e.getMessage(), e);
        }
        return testInputs;
    }

    /**
     * Parse the classification from the split line.
     * @param values The split line as an array of strings, the last value in the array is the classification
     * @return The classification as a {@link MovementClassification}
     */
    private static MovementClassification getMovementClassificationFromSplitLine(String[] values) {
        // The last value in the line is the classification
        return MovementClassification.fromValue(Integer.parseInt(values[values.length-1]));
    }

    /**
     * Parse the input values from the split line.
     * @param values The split line as an array of strings
     * @return The input values as an array of floats
     */
    private static float[] getInputsFromSplitLine(String[] values) {
        // The last value in the line is the classification, so we don't want it in the input value
        float[] entry = new float[values.length-1];
        for (int i = 0; i < values.length-1; i++) {
            entry[i] = Float.parseFloat(values[i]);
        }
        return entry;
    }
    // endregion TestSetup
}
