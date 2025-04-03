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

import java.io.IOException;
import java.util.Arrays;

import megamek.ai.neuralnetwork.BrainRegistry;
import megamek.ai.neuralnetwork.NeuralNetwork;
import megamek.client.bot.caspar.ClassificationScore;

public class CasparUtilities {

    private static final double[] entry = new double[]{0.3333333222222226,0.0,0.9999999500000026,0.9999998076923448,0.0,0.0764160718595137,0.0781249998779297,0.33333330000000333,0.9999999000000099,0.1666666652777778,0.23529411626297578,0.9999999000000099,0.0,0.0,0.0,0.99999950000025,0.0,0.9999998500000226,0.0,0.0,0.4999999950000001,0.42372763609406866,0.4848349085715538,0.4153284385971791,0.9999999000000099,0.9999997500000625,0.0,0.9999999000000099,0.0304600708927842,0.0,0.9999999000000099,0.32416916459119854,0.0,0.0,0.3894020364548058,0.6666666611111112,0.10044641444615156,0.0,0.25963204223019976,0.0,0.3679270433087095,0.5294117645905421,0.0,0.0034075476462798082,0.4999999950000001,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};
    private static final float[] y_test = {1f, 0f, 0f};

    public static void main(String[] args) throws IOException {
        NeuralNetwork.testTensorFlow();
        NeuralNetwork neuralNetwork = new NeuralNetwork();

        neuralNetwork.loadModel(new BrainRegistry("default", 55, 3));
        float[] x_test = new float[entry.length];
        for (int i = 0; i < entry.length; i++) {
            x_test[i] = (float) entry[i];
        }
        ClassificationScore result = ClassificationScore.fromPrediction(neuralNetwork.predict(x_test));
        System.out.println("Prediction result: " + result);
        System.out.println("Expected result: " + Arrays.toString(y_test));
    }
}
