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

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

import megamek.client.bot.caspar.ClassificationScore;
import megamek.client.bot.caspar.MovementClassification;
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
    void testCalculatingMoves(Brain.TestValue testValue) {
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

    private static Stream<Brain.TestValue> testValidationInputs() {
        return Brain.testValidationInputs(DEFAULT_BRAIN);
    }
}
