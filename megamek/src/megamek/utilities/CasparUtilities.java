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
package megamek.utilities;

import static megamek.ai.neuralnetwork.Brain.testValidationInputs;

import java.io.IOException;

import megamek.ai.neuralnetwork.Brain;
import megamek.ai.neuralnetwork.BrainRegistry;
import megamek.client.bot.caspar.ClassificationScore;
import megamek.client.bot.caspar.MovementClassification;

/**
 * CasparUtilities is a utility class for testing the Caspar neural network model.
 * It loads the model and runs predictions on test inputs, printing the results to the console.
 * Very useful when you just want to run it using the CLI and see the results.
 * @author Luana Coppio
 */
public class CasparUtilities {

    private static final BrainRegistry DEFAULT_BRAIN = new BrainRegistry("default", 3);

    public static void main(String[] args) throws IOException {
        Brain brain = Brain.loadBrain(DEFAULT_BRAIN);
        for (var testValue : testValidationInputs(DEFAULT_BRAIN).toList()) {
            var prediction = brain.predict(testValue.input());
            MovementClassification predictedClassification = ClassificationScore.fromPrediction(prediction).getClassification();
            System.out.println((predictedClassification.equals(testValue.classification()) ? "[V]" : "[X]") +
                                     " Predicted classification: " + predictedClassification +
                                     " expected: " + testValue.classification());
        }
    }
}
