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

import megamek.logging.MMLogger;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.proto.SignatureDef;
import org.tensorflow.proto.TensorInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static megamek.codeUtilities.MathUtility.clamp01;

/**
 * NeuralNetwork class for loading and making predictions with TensorFlow models.
 * @author Luana Coppio
 */
public class NeuralNetwork {
    private static final MMLogger logger = MMLogger.create(NeuralNetwork.class);

    private SavedModelBundle model;
    private Session session;
    private String inputOperationName;
    private String outputOperationName;
    private float[] inputNormalizationMinValues;
    private float[] inputNormalizationMaxValues;
    private int outputAxisLength = 1;

    /**
     * Loads a TensorFlow model from the specified path.
     *
     * @param brainRegistry The BrainRegistry containing model information
     * @return this NeuralNetwork instance
     * @throws RuntimeException If the model cannot be loaded
     */
    public NeuralNetwork loadModel(BrainRegistry brainRegistry) {
        try {
            this.outputAxisLength = brainRegistry.outputAxisLength();
            Path path = Path.of("data", "ai","brains", brainRegistry.modelName());
            model = SavedModelBundle.load(path.toString(), "serve");
            session = model.session();

            SignatureDef sigDef = model.metaGraphDef().getSignatureDefMap().get("serving_default");

            // Extract input operation name
            Map.Entry<String, TensorInfo> inputEntry = sigDef.getInputsMap().entrySet().iterator().next();
            inputOperationName = parseOperationName(inputEntry.getValue().getName());

            // Extract output operation name
            Map.Entry<String, TensorInfo> outputEntry = sigDef.getOutputsMap().entrySet().iterator().next();
            outputOperationName = parseOperationName(outputEntry.getValue().getName());

            loadInputNormalizationParameters(brainRegistry);

            logger.info("Model loaded successfully from: {}", path);
            logger.info("Input operation: {}", inputOperationName);
            logger.info("Output operation: {}", outputOperationName);

        } catch (Exception e) {
            logger.error("Failed to load model", e);
            throw new RuntimeException("Failed to load TensorFlow model: " + e.getMessage(), e);
        }
        return this;
    }

    private void loadInputNormalizationParameters(BrainRegistry brainRegistry) {
        inputNormalizationMinValues = new float[brainRegistry.inputAxisLength()];
        inputNormalizationMaxValues = new float[brainRegistry.inputAxisLength()];

        // Initialize normalization values
        // the normalization values are on a file named min_max_feature_normalization.csv inside the modelPath
        Path normalizationFilePath = Path.of("data", "ai","brains",
              brainRegistry.modelName(), "min_max_feature_normalization.csv");
        try (var reader = new BufferedReader(new FileReader(normalizationFilePath.toFile()))) {
            String line;
            int index;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("feature,")) {
                    continue;
                }
                String[] values = line.split(",");
                index = Integer.parseInt(values[0]);
                inputNormalizationMinValues[index] = Float.parseFloat(values[1]);
                inputNormalizationMaxValues[index] = Float.parseFloat(values[2]);
            }
        } catch (IOException e) {
            logger.warn("Normalization file not found, using default values", e);
        }
    }

    /**
     * Parse the operation name from TensorFlow's tensor name format
     * (e.g., "dense_input:0" -> "dense_input")
     */
    private String parseOperationName(String tensorName) {
        if (tensorName.contains(":")) {
            return tensorName.substring(0, tensorName.lastIndexOf(":"));
        }
        return tensorName;
    }

    /**
     * Makes a prediction using the loaded model.
     *
     * @param inputVector Input features as a double array
     * @return Prediction result as a double
     * @throws IllegalStateException If the model is not loaded
     */
    public float[] predict(float[] inputVector) {
        if (session == null) {
            throw new IllegalStateException("Model not loaded. Call loadModel() first.");
        }

        // normalize the input vector with the min and max values
        for (int i = 0; i < inputVector.length; i++) {
            inputVector[i] = clamp01(
                  (inputVector[i] - inputNormalizationMinValues[i]) /
                        (inputNormalizationMaxValues[i] - inputNormalizationMinValues[i])
            );
        }

        FloatNdArray ndArray = NdArrays.ofFloats(Shape.of(1, inputVector.length));

        // Copy the float values into the correct shape
        for (int i = 0; i < inputVector.length; i++) {
            // Assuming a 2D input shape [1][features]
            ndArray.setFloat(inputVector[i], 0, i);
        }

        try (var inputTensor = Tensor.of(org.tensorflow.types.TFloat32.class, ndArray.shape(), ndArray::copyTo)) {
            try(var outputTensors = session.runner().feed(inputOperationName, inputTensor)
                                          .fetch(outputOperationName)
                                          .run()) {
                // Extract the result
                try (var rOutputTensor = outputTensors.get(0).asRawTensor()) {
                    float[] output = new float[outputAxisLength];
                    rOutputTensor.data().asFloats().read(output);
                    return output;
                }
            }
        } catch (Exception e) {
            logger.error("Prediction failed", e);
            throw new RuntimeException("Failed to make prediction: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the model and session to release resources.
     */
    public void close() {
        if (model != null) {
            try {
                model.close();
                model = null;
                session = null;
                logger.info("Neural network model resources released");
            } catch (Exception e) {
                logger.warn("Error closing model", e);
            } finally {
                model = null;
                session = null;
            }
        }
    }

    /**
     * Simple test method to verify TensorFlow is working.
     */
    public static void testTensorFlow() {
        try {
            String version = TensorFlow.version();
            logger.info("TensorFlow version: {}", version);
        } catch (Exception e) {
            logger.error("Error loading TensorFlow", e);
        }
    }
}
