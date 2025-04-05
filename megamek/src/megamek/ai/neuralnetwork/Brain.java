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

import static megamek.codeUtilities.MathUtility.clamp01;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import megamek.client.bot.caspar.MovementClassification;
import megamek.common.Configuration;
import megamek.logging.MMLogger;
import org.tensorflow.Result;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;
import org.tensorflow.ndarray.FloatNdArray;
import org.tensorflow.ndarray.NdArrays;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.proto.SignatureDef;
import org.tensorflow.proto.TensorInfo;

/**
 * Brain class for loading different tensorflow models and making predictions.
 * @author Luana Coppio
 */
public class Brain {
    private static final MMLogger logger = MMLogger.create(Brain.class);

    @SuppressWarnings("FieldCanBeLocal")
    private final SavedModelBundle model;
    private final Session session;
    private final String inputOperationName;
    private final String outputOperationName;
    private final FeatureNormalizationParameters featureNormalizationParameters;
    private final int outputAxisLength;

    private Brain(
          int outputAxisLength,
          SavedModelBundle model,
          FeatureNormalizationParameters featureNormalizationParameters)
    {
        this.model = model;
        this.outputAxisLength = outputAxisLength;
        this.session = model.session();

        SignatureDef sigDef = model.metaGraphDef().getSignatureDefMap().get("serving_default");

        // Extract input operation name
        Map.Entry<String, TensorInfo> inputEntry = sigDef.getInputsMap().entrySet().iterator().next();
        inputOperationName = parseOperationName(inputEntry.getValue().getName());

        // Extract output operation name
        Map.Entry<String, TensorInfo> outputEntry = sigDef.getOutputsMap().entrySet().iterator().next();
        outputOperationName = parseOperationName(outputEntry.getValue().getName());
        this.featureNormalizationParameters = featureNormalizationParameters;

        logger.info("Input operation: {}", inputOperationName);
        logger.info("Output operation: {}", outputOperationName);
    }

    /**
     * Loads a TensorFlow model from the specified path.
     *
     * @param brainRegistry The BrainRegistry containing model information
     * @return this NeuralNetwork instance
     * @throws RuntimeException If the model cannot be loaded
     */
    public static Brain loadBrain(BrainRegistry brainRegistry) {
        SavedModelBundle model =
              SavedModelBundle.load(Configuration.aiBrainFolderPath(brainRegistry.name()).toString());
        FeatureNormalizationParameters featureNormalizationParameters =
              FeatureNormalizationParameters.loadFile(Configuration.aiBrainNormalizationFile(brainRegistry.name()));
        return new Brain(brainRegistry.outputAxisLength(), model, featureNormalizationParameters);
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
     * Makes a prediction based on the input vector.
     *
     * @param inputVector Input features as a floats array
     * @return Prediction result as a double
     * @throws IllegalStateException If the model is not loaded
     */
    public float[] predict(float[] inputVector) {
        FloatNdArray ndArray = getNormalizedFloatNdArray(inputVector);
        try (var inputTensor = Tensor.of(org.tensorflow.types.TFloat32.class, ndArray.shape(), ndArray::copyTo)) {
            try (var outputTensors = session.runner().feed(inputOperationName, inputTensor)
                                          .fetch(outputOperationName)
                                          .run()) {
                return resultArrayFromTensors(outputTensors);
            }
        } catch (Exception e) {
            logger.error("Prediction failed, there is no recourse from this", e);
            throw new RuntimeException("Failed to make prediction: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the result array from the output tensors.
     * @param outputTensors The output tensors from the model
     * @return The result array containing the prediction values
     */
    private float[] resultArrayFromTensors(Result outputTensors) {
        try (var rOutputTensor = outputTensors.get(0).asRawTensor()) {
            float[] output = new float[outputAxisLength];
            rOutputTensor.data().asFloats().read(output);
            return output;
        }
    }

    /**
     * Creates a FloatNdArray from the input vector.
     * @param inputVector The input vector to convert
     * @return The created FloatNdArray with the normalized values from the input vector
     */
    private FloatNdArray getNormalizedFloatNdArray(float[] inputVector) {
        FloatNdArray ndArray = NdArrays.ofFloats(Shape.of(1, inputVector.length));
        for (int i = 0; i < inputVector.length; i++) {
            ndArray.setFloat(clamp01(
                  (inputVector[i] - featureNormalizationParameters.minValues()[i]) /
                        (featureNormalizationParameters.maxValues()[i] - featureNormalizationParameters.minValues()[i])
            ), 0, i);
        }
        return ndArray;
    }

    /**
     * Simple test method to verify if TensorFlow is working.
     */
    public static boolean testTensorFlow() {
        try {
            String version = TensorFlow.version();
            logger.info("TensorFlow version: {}", version);
            return true;
        } catch (Exception e) {
            logger.error("Error loading TensorFlow", e);
        }
        return false;
    }

    /**
     * Get the input size of the model.
     * @return The input size of the model
     */
    public int getInputSize() {
        return featureNormalizationParameters.minValues().length;
    }

    /**
     * Get the output size of the model.
     * @return The output size of the model
     */
    public int getOutputSize() {
        return outputAxisLength;
    }

    /**
     * TestValue is a record that holds the input values and their corresponding classification.
     * @param input The input values as a float array
     * @param classification The classification of the input values as a {@link MovementClassification}
     */
    public record TestValue(float[] input, MovementClassification classification) {}

    /**
     * Get the test values for the model.
     * @param brainRegistry The BrainRegistry containing model information
     * @return A stream of TestValue objects containing the input values and their corresponding classification
     */
    public static Stream<TestValue> testValidationInputs(BrainRegistry brainRegistry) {
        return loadTestValues(brainRegistry).stream();
    }

    /**
     * Load the test values from the CSV file that accompany the brain model.
     * @param brainRegistry The BrainRegistry containing model information
     * @return A collection of TestValue objects containing the input values and their corresponding classification
     */
    public static Collection<TestValue> loadTestValues(BrainRegistry brainRegistry) {
        List<TestValue> testInputs = new ArrayList<>();
        Path testInputFilePath = Path.of(Configuration.aiBrainFolderPath(brainRegistry.name()).toString(),
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
        return MovementClassification.values()[Integer.parseInt(values[values.length-1])];
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
}
