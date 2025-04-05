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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import megamek.client.bot.caspar.ClassificationScore;
import megamek.client.bot.caspar.MovementClassification;
import megamek.common.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class BrainTest {

    private static Brain brain;
    private static final Map<MovementClassification, Integer> ERROR_COUNTS =
          new EnumMap<>(MovementClassification.class);
    private static int TOTAL_TESTS = 0;
    private static final float ACCEPTABLE_ERROR_PERCENT = 0.5f;

    @BeforeAll
    public static void setUp() {
        resetTrackers();
        brain = Brain.loadBrain(new BrainRegistry("default", 3));
        Brain.testTensorFlow();
    }

    @ParameterizedTest
    @MethodSource(value = "testValidationInputs")
    void testCalculatingMoves(TestValue testValue) {
        var prediction = brain.predict(testValue.input());
        MovementClassification predictedClassification = ClassificationScore.fromPrediction(prediction).getClassification();
        if (!predictedClassification.equals(testValue.classification())) {
            ERROR_COUNTS.merge(testValue.classification(), 1, Integer::sum);
        }
        TOTAL_TESTS++;
    }

    @AfterAll
    public static void afterAllTests() {
        if (TOTAL_TESTS == 0) {
            return;
        }

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
        resetTrackers();
    }

    private static void resetTrackers() {
        ERROR_COUNTS.clear();
        TOTAL_TESTS = 0;
    }

    private record TestValue(float[] input, MovementClassification classification) {}

    private static Stream<TestValue> testValidationInputs() {
        return loadTestValues().stream();
    }

    private static Collection<TestValue> loadTestValues() {
        List<TestValue> testInputs = new ArrayList<>();
        Path testInputFilePath = Path.of(Configuration.aiBrainFolderPath("default").toString(),
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

    private static MovementClassification getMovementClassificationFromSplitLine(String[] values) {
        // The last value in the line is the classification
        return MovementClassification.values()[Integer.parseInt(values[values.length-1])];
    }

    private static float[] getInputsFromSplitLine(String[] values) {
        // The last value in the line is the classification, so we don't want it in the input value
        float[] entry = new float[values.length-1];
        for (int i = 0; i < values.length-1; i++) {
            entry[i] = Float.parseFloat(values[i]);
        }
        return entry;
    }



}
