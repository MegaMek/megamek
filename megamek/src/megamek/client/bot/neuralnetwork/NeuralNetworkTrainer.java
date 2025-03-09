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
package megamek.client.bot.neuralnetwork;

import megamek.ai.dataset.ActionAndState;
import megamek.ai.dataset.TrainingDataset;
import megamek.ai.dataset.UnitAction;
import megamek.ai.dataset.UnitState;
import megamek.client.bot.caspar.DefaultInputAxisCalculator;
import megamek.client.bot.common.DifficultyLevel;
import megamek.client.bot.caspar.axis.InputAxisCalculator;
import megamek.logging.MMLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for training and evaluating neural networks for CASPAR AI.
 * Handles the training process and manages model persistence.
 */
public class NeuralNetworkTrainer {
    private static final MMLogger logger = MMLogger.create(NeuralNetworkTrainer.class);

    private final InputAxisCalculator inputCalculator;
    private final Random random;

    /**
     * Creates a neural network trainer.
     */
    public NeuralNetworkTrainer() {
        this.inputCalculator = new DefaultInputAxisCalculator();
        this.random = new Random();
    }

    /**
     * Trains a neural network model for each difficulty level.
     *
     * @param modelName The model name
     * @param dataset The training dataset
     * @param epochs Number of training epochs
     * @param batchSize Batch size for training
     * @throws IOException If models cannot be saved
     */
    public void trainModelsForAllDifficulties(String modelName,
                                              TrainingDataset dataset,
                                              Architecture architecture,
                                              int epochs,
                                              int batchSize) throws IOException {
        // Create directory for models if it doesn't exist
        Path modelsDir = Paths.get("data", "caspar", "models");
        Files.createDirectories(modelsDir);

        // Train models for each difficulty level in parallel
        ExecutorService executor = Executors.newFixedThreadPool(
              Math.min(DifficultyLevel.values().length, Runtime.getRuntime().availableProcessors()));

        for (DifficultyLevel level : DifficultyLevel.values()) {
            executor.submit(() -> {
                try {
                    NeuralNetwork model = trainModel(modelName, level, dataset, architecture, epochs, batchSize);
                    model.saveModel(modelName, level);
                } catch (Exception e) {
                    logger.error(e, "Failed to train model for {}",  level);
                }
            });
        }

        executor.shutdown();
        try {
            boolean result = executor.awaitTermination(1, TimeUnit.DAYS);
            if (!result) {
                logger.warn("Training took longer than expected and was aborted");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(e, "Training interrupted");
        }
    }

    /**
     * Trains a single neural network model.
     *
     * @param modelName The model name to use
     * @param level The difficulty level
     * @param dataset The training dataset
     * @param architecture The neural network architecture
     * @param epochs Number of training epochs
     * @param batchSize Batch size for training
     * @return The trained neural network
     */
    public NeuralNetwork trainModel(String modelName,
                                    DifficultyLevel level,
                                    TrainingDataset dataset,
                                    Architecture architecture,
                                    int epochs,
                                    int batchSize) {
        logger.info("Training model: {} for difficulty: {}", modelName, level);

        // Create or load the model
        NeuralNetwork model;
        try {
            model = NeuralNetwork.loadModel(modelName, level);
            logger.info("Loaded existing model for further training");
        } catch (Exception e) {
            logger.info("Creating new model for training");
            try {
                model = NeuralNetwork.createAndSaveModel(architecture, modelName, level);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to create model", ex);
            }
        }

        // Extract training data
        List<double[]> features = new ArrayList<>();
        List<Double> targets = new ArrayList<>();

        // Prepare features and targets
        prepareTrainingData(dataset, features, targets);

        if (features.isEmpty()) {
            noValidTrainingExamplesFound();
            return model;
        }

        // Apply difficulty-specific adjustments to training targets
        List<Double> adjustedTargets = adjustTargetsForDifficulty(targets, level);

        // Convert to arrays for batch training
        double[][] featuresArray = features.toArray(new double[0][]);
        double[] targetsArray = adjustedTargets.stream().mapToDouble(Double::doubleValue).toArray();

        // Train for specified number of epochs
        for (int epoch = 0; epoch < epochs; epoch++) {
            // Shuffle indices for each epoch
            int[] indices = shuffleIndices(featuresArray.length);

            // Track the average error for this epoch
            double epochError = 0.0;
            int batchCount = 0;

            // Process in batches
            for (int batchStart = 0; batchStart < featuresArray.length; batchStart += batchSize) {
                int batchEnd = Math.min(batchStart + batchSize, featuresArray.length);
                int actualBatchSize = batchEnd - batchStart;

                // Create batch
                double[][] batchFeatures = new double[actualBatchSize][];
                double[] batchTargets = new double[actualBatchSize];

                for (int i = 0; i < actualBatchSize; i++) {
                    int idx = indices[batchStart + i];
                    batchFeatures[i] = featuresArray[idx];
                    batchTargets[i] = targetsArray[idx];
                }

                // Train on batch
                double batchError = model.trainBatch(batchFeatures, batchTargets);
                epochError += batchError;
                batchCount++;
            }

            // Calculate average error for this epoch
            double avgError = batchCount > 0 ? epochError / batchCount : 0.0;

            // Log progress
            if ((epoch + 1) % 10 == 0 || epoch == 0 || epoch == epochs - 1) {
                logger.info("Epoch " + (epoch + 1) + "/" + epochs +
                      " for " + modelName + " " + level +
                      " - Avg. Error: " + avgError);

                // Evaluate on a subset of the training data
                double accuracy = evaluateAccuracy(model, featuresArray, targetsArray, 100);
                logger.info("Training accuracy: " + String.format("%.2f%%", accuracy * 100));
            }

            // Save checkpoint every 50 epochs
            if ((epoch + 1) % 50 == 0) {
                try {
                    model.saveModel(modelName, level);
                    logger.info("Saved checkpoint at epoch {}", epoch + 1);
                } catch (IOException e) {
                    logger.warn(e, "Failed to save checkpoint");
                }
            }
        }

        return model;
    }

    private static void noValidTrainingExamplesFound() {
        logger.warn("No valid training examples found");
    }

    /**
     * Prepares training data from a dataset.
     *
     * @param dataset The training dataset
     * @param features Output list to store features
     * @param targets Output list to store target values
     */
    private void prepareTrainingData(TrainingDataset dataset, List<double[]> features, List<Double> targets) {
        // Convert dataset to iterator
        Iterator<ActionAndState> iterator = dataset.iterator();

        while (iterator.hasNext()) {
            // Get current state
            ActionAndState currentState = iterator.next();

            // Get next state (if available)
            if (!iterator.hasNext()) {
                break;
            }
            ActionAndState nextState = iterator.next();

            // Extract features from current state
            UnitAction action = currentState.unitAction();

            // Calculate input vector
            double[] inputVector = inputCalculator.calculateInputVector(action.movePath(currentState.getEntity()), new SyntheticGameState(currentState));

            // Calculate target value (success metric)
            double targetValue = calculateSuccessMetric(currentState, nextState);

            // Add to training data
            features.add(inputVector);
            targets.add(targetValue);
        }
    }

    /**
     * Generates shuffled indices for batch training.
     *
     * @param size The number of examples
     * @return An array of shuffled indices
     */
    private int[] shuffleIndices(int size) {
        int[] indices = new int[size];
        for (int i = 0; i < size; i++) {
            indices[i] = i;
        }

        // Fisher-Yates shuffle
        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = indices[i];
            indices[i] = indices[j];
            indices[j] = temp;
        }

        return indices;
    }

    /**
     * Adjusts target values based on difficulty level.
     *
     * @param targets Original target values
     * @param level Difficulty level
     * @return Adjusted target values
     */
    private List<Double> adjustTargetsForDifficulty(List<Double> targets, DifficultyLevel level) {
        List<Double> adjusted = new ArrayList<>(targets.size());

        // Get difficulty-specific parameters
        double noiseLevel = getDifficultyNoiseLevel(level);
        double strengthFactor = getDifficultyStrengthFactor(level);

        for (double target : targets) {
            // Add noise to simulate different skill levels
            double noise = (random.nextDouble() * 2 - 1) * noiseLevel;

            // Transform the target value based on difficulty
            // For harder difficulties, this enhances the signal (makes good moves better)
            // For easier difficulties, this flattens the signal (makes decisions more random)
            double transformed = Math.signum(target - 0.5) *
                  Math.pow(Math.abs(target - 0.5) * 2, strengthFactor) / 2 + 0.5;

            // Apply noise
            double adjustedTarget = Math.max(0.0, Math.min(1.0, transformed + noise));
            adjusted.add(adjustedTarget);
        }

        return adjusted;
    }

    /**
     * Gets the noise level for a difficulty level.
     *
     * @param level The difficulty level
     * @return The noise level
     */
    private double getDifficultyNoiseLevel(DifficultyLevel level) {
        return switch (level) {
            case BEGINNER -> 0.3;
            case EASY -> 0.2;
            case MEDIUM -> 0.1;
            case HARD -> 0.05;
            case HARDCORE -> 0.01;
        };
    }

    /**
     * Gets the strength factor for a difficulty level.
     *
     * @param level The difficulty level
     * @return The strength factor
     */
    private double getDifficultyStrengthFactor(DifficultyLevel level) {
        return switch (level) {
            case BEGINNER -> 0.5;  // Flattens the curve, making decisions more random
            case EASY -> 0.7;
            case MEDIUM -> 1.0;    // Neutral transformation
            case HARD -> 1.3;
            case HARDCORE -> 1.7;    // Sharpens the curve, making optimal choices more likely
        };
    }

    /**
     * Calculates a success metric for an action by comparing current and next states.
     *
     * @param currentState The current action and state
     * @param nextState The next action and state
     * @return A success metric between 0 and 1
     */
    private double calculateSuccessMetric(ActionAndState currentState, ActionAndState nextState) {
        UnitAction action = currentState.unitAction();

        // Find the unit in the next state
        UnitState nextUnitState = nextState.boardUnitState().stream()
              .filter(state -> state.id() == action.id())
              .findFirst()
              .orElse(null);

        if (nextUnitState == null) {
            // Unit might have been destroyed
            return 0.0;
        }

        // Calculate various metrics of success
        double survivalBonus = nextUnitState.destroyed() ? 0.0 : 0.5;

        // Health retention (armor and internal structure)
        double initialHealth = (action.armorP() + action.internalP()) / 2.0;
        double finalHealth = (nextUnitState.armorP() + nextUnitState.internalP()) / 2.0;
        double healthRetention = initialHealth > 0 ? Math.min(1.0, finalHealth / initialHealth) : 0.0;

        // Position improvement (would depend on game-specific metrics)
        // This is a placeholder - in a real implementation, you'd measure tactical advantage
        double positionScore = 0.5;

        // Combine metrics with weights
        return 0.4 * survivalBonus + 0.4 * healthRetention + 0.2 * positionScore;
    }

    /**
     * Evaluates the model's accuracy on a subset of data.
     *
     * @param model The model to evaluate
     * @param features Feature vectors
     * @param targets Target values
     * @param sampleSize Number of samples to evaluate (0 for all)
     * @return Accuracy between 0 and 1
     */
    private double evaluateAccuracy(NeuralNetwork model, double[][] features, double[] targets, int sampleSize) {
        if (features.length == 0) {
            return 0.0;
        }

        int numSamples = sampleSize > 0 ? Math.min(sampleSize, features.length) : features.length;

        // Sample indices
        int[] indices = new int[numSamples];
        if (numSamples == features.length) {
            for (int i = 0; i < numSamples; i++) {
                indices[i] = i;
            }
        } else {
            // Random sampling without replacement
            Set<Integer> sampledIndices = new HashSet<>(numSamples);
            while (sampledIndices.size() < numSamples) {
                sampledIndices.add(random.nextInt(features.length));
            }
            int idx = 0;
            for (int i : sampledIndices) {
                indices[idx++] = i;
            }
        }

        // Evaluate
        int correct = 0;
        for (int i = 0; i < numSamples; i++) {
            int idx = indices[i];
            double prediction = model.predict(features[idx]);
            boolean predictedPositive = prediction >= 0.5;
            boolean actualPositive = targets[idx] >= 0.5;

            if (predictedPositive == actualPositive) {
                correct++;
            }
        }

        return (double) correct / numSamples;
    }

    /**
     * Evaluates a model on a validation dataset.
     *
     * @param model The model to evaluate
     * @param dataset The validation dataset
     * @return The evaluation metrics
     */
    public Map<String, Double> evaluateModel(NeuralNetwork model, TrainingDataset dataset) {
        Map<String, Double> metrics = new HashMap<>();

        // Extract validation data
        List<double[]> features = new ArrayList<>();
        List<Double> targets = new ArrayList<>();

        // Prepare features and targets
        prepareTrainingData(dataset, features, targets);

        if (features.isEmpty()) {
            logger.warn("No valid validation examples found");
            metrics.put("mse", Double.NaN);
            metrics.put("accuracy", Double.NaN);
            return metrics;
        }

        // Convert to arrays
        double[][] featuresArray = features.toArray(new double[0][]);
        double[] targetsArray = targets.stream().mapToDouble(Double::doubleValue).toArray();

        // Calculate metrics
        double totalLoss = 0.0;
        int correctPredictions = 0;

        for (int i = 0; i < featuresArray.length; i++) {
            double prediction = model.predict(featuresArray[i]);
            double target = targetsArray[i];

            // Calculate squared error
            double error = Math.pow(prediction - target, 2);
            totalLoss += error;

            // Calculate accuracy (treating >0.5 as positive)
            boolean predictedPositive = prediction >= 0.5;
            boolean actualPositive = target >= 0.5;
            if (predictedPositive == actualPositive) {
                correctPredictions++;
            }
        }

        // Compute final metrics
        double meanSquaredError = totalLoss / featuresArray.length;
        double accuracy = (double) correctPredictions / featuresArray.length;

        metrics.put("mse", meanSquaredError);
        metrics.put("accuracy", accuracy);

        return metrics;
    }

    /**
     * Performs hyperparameter tuning to find optimal network configuration.
     *
     * @param modelName The base model name
     * @param level The difficulty level
     * @param architecture The neural network architecture
     * @param trainingData The training dataset
     * @param validationData The validation dataset
     * @throws IOException If models cannot be saved
     */
    public void tuneHyperparameters(String modelName,
                                    DifficultyLevel level,
                                    Architecture architecture,
                                    TrainingDataset trainingData,
                                    TrainingDataset validationData,
                                    int epochs,
                                    int batchSize) throws IOException {
        logger.info("Starting hyperparameter tuning for model: {}, difficulty: {}, epochs: {}, batchSize: {}", modelName, level, epochs, batchSize);

        // Define hyperparameter ranges to test
        double[] learningRates = {0.01, 0.005, 0.001, 0.0005, 0.0001};
        double[] alphaValues = {0.8, 0.9, 1.0, 1.1, 1.2};

        // Track best configuration
        double bestAccuracy = 0.0;
        double bestLearningRate = 0.0;
        double bestAlpha = 0.0;

        // Grid search
        for (double learningRate : learningRates) {
            for (double alpha : alphaValues) {
                logger.info("Testing configuration: learningRate=" + learningRate + ", alpha=" + alpha);

                // Create and train model with current hyperparameters
                NeuralNetwork model = createModelWithHyperparameters(level, architecture, learningRate, alpha);

                // Train with a small number of epochs for tuning
                int tuningEpochs = 50;
                int intermediaryBatchSize = 32;

                // Extract training data
                List<double[]> features = new ArrayList<>();
                List<Double> targets = new ArrayList<>();
                prepareTrainingData(trainingData, features, targets);

                if (features.isEmpty()) {
                    noValidTrainingExamplesFound();
                    continue;
                }

                // Convert to arrays
                double[][] featuresArray = features.toArray(new double[0][]);
                double[] targetsArray = targets.stream().mapToDouble(Double::doubleValue).toArray();

                // Train model
                for (int epoch = 0; epoch < tuningEpochs; epoch++) {
                    int[] indices = shuffleIndices(featuresArray.length);

                    // Process in batches
                    for (int batchStart = 0; batchStart < featuresArray.length; batchStart += intermediaryBatchSize) {
                        int batchEnd = Math.min(batchStart + intermediaryBatchSize, featuresArray.length);
                        int actualBatchSize = batchEnd - batchStart;

                        // Create batch
                        double[][] batchFeatures = new double[actualBatchSize][];
                        double[] batchTargets = new double[actualBatchSize];

                        for (int i = 0; i < actualBatchSize; i++) {
                            int idx = indices[batchStart + i];
                            batchFeatures[i] = featuresArray[idx];
                            batchTargets[i] = targetsArray[idx];
                        }

                        // Train on batch
                        model.trainBatch(batchFeatures, batchTargets);
                    }
                }

                // Evaluate on validation data
                Map<String, Double> metrics = evaluateModel(model, validationData);
                double accuracy = metrics.get("accuracy");

                logger.info("Configuration accuracy: " + String.format("%.2f%%", accuracy * 100));

                // Check if this is the best configuration so far
                if (accuracy > bestAccuracy) {
                    bestAccuracy = accuracy;
                    bestLearningRate = learningRate;
                    bestAlpha = alpha;

                    // Save this model as a checkpoint
                    model.saveModel(modelName, level);
                }
            }
        }

        logger.info("Hyperparameter tuning completed");
        logger.info("Best configuration: learningRate=" + bestLearningRate + ", alpha=" + bestAlpha +
              " with accuracy=" + String.format("%.2f%%", bestAccuracy * 100));

        // Create final model with best hyperparameters and train fully
        NeuralNetwork finalModel = createModelWithHyperparameters(level, architecture, bestLearningRate, bestAlpha);
        trainModel(finalModel, trainingData, epochs, batchSize);
        finalModel.saveModel(modelName, level);
    }

    /**
     * Creates a model with specified hyperparameters.
     *
     * @param level The difficulty level
     * @param architecture The neural network architecture
     * @param learningRate The learning rate
     * @param alpha The alpha value for ELU activation
     * @return The created neural network
     */
    private NeuralNetwork createModelWithHyperparameters(DifficultyLevel level,
                                                         Architecture architecture,
                                                         double learningRate,
                                                         double alpha) {
        // Create activation functions
        ActivationFunction[] activationFunctions = Arrays.copyOf(architecture.getActivationFunctions(),
              architecture.getActivationFunctionCount());

        for (var activationFunction : activationFunctions) {
            if (activationFunction instanceof ELU elu) {
                elu.setAlpha(alpha);
            }
        }
        // Initialize weights and biases
        double[][][] weights = new double[architecture.getLayerCount() - 1][][];
        double[][] biases = new double[architecture.getLayerCount() - 1][];

        Random random = new Random(level.ordinal() * 1000 + architecture.hashCode());
        int[] layerSizes = architecture.getLayerSizes();

        for (int i = 0; i < layerSizes.length - 1; i++) {
            weights[i] = new double[layerSizes[i]][layerSizes[i + 1]];
            biases[i] = new double[layerSizes[i + 1]];

            // Xavier/Glorot initialization for weights
            double scale = Math.sqrt(2.0 / (layerSizes[i] + layerSizes[i + 1]));

            for (int j = 0; j < layerSizes[i]; j++) {
                for (int k = 0; k < layerSizes[i + 1]; k++) {
                    weights[i][j][k] = (random.nextDouble() * 2 - 1) * scale;
                }
            }

            // Initialize biases to small values
            for (int j = 0; j < layerSizes[i + 1]; j++) {
                biases[i][j] = (random.nextDouble() * 2 - 1) * 0.01;
            }
        }

        return new NeuralNetwork(layerSizes, activationFunctions, weights, biases, learningRate);
    }

    /**
     * Trains a specified model on a dataset.
     *
     * @param model The model to train
     * @param dataset The training dataset
     * @param epochs Number of training epochs
     * @param batchSize Batch size for training
     */
    private void trainModel(NeuralNetwork model, TrainingDataset dataset, int epochs, int batchSize) {
        // Extract training data
        List<double[]> features = new ArrayList<>();
        List<Double> targets = new ArrayList<>();
        prepareTrainingData(dataset, features, targets);

        if (features.isEmpty()) {
            noValidTrainingExamplesFound();
            return;
        }

        // Convert to arrays
        double[][] featuresArray = features.toArray(new double[0][]);
        double[] targetsArray = targets.stream().mapToDouble(Double::doubleValue).toArray();

        // Train for specified number of epochs
        for (int epoch = 0; epoch < epochs; epoch++) {
            // Shuffle indices for each epoch
            int[] indices = shuffleIndices(featuresArray.length);

            // Process in batches
            for (int batchStart = 0; batchStart < featuresArray.length; batchStart += batchSize) {
                int batchEnd = Math.min(batchStart + batchSize, featuresArray.length);
                int actualBatchSize = batchEnd - batchStart;

                // Create batch
                double[][] batchFeatures = new double[actualBatchSize][];
                double[] batchTargets = new double[actualBatchSize];

                for (int i = 0; i < actualBatchSize; i++) {
                    int idx = indices[batchStart + i];
                    batchFeatures[i] = featuresArray[idx];
                    batchTargets[i] = targetsArray[idx];
                }

                // Train on batch
                model.trainBatch(batchFeatures, batchTargets);
            }

            // Log progress
            if ((epoch + 1) % 10 == 0 || epoch == 0 || epoch == epochs - 1) {
                double accuracy = evaluateAccuracy(model, featuresArray, targetsArray, 100);
                logger.info("Epoch " + (epoch + 1) + "/" + epochs +
                      " - Training accuracy: " + String.format("%.2f%%", accuracy * 100));
            }
        }
    }
}
