/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * without ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */
package megamek.ai.utility;

import java.util.Random;

/**
 * Simple feedforward neural network implementation.
 * Supports regression problems with a single output node.
 * @author Luana Coppio
 */
public class NeuralNetwork {
    private double[][][] weights;
    private double[][][] biases;
    private int[] layerSizes;
    private final Random random = new Random();

    public NeuralNetwork(int... layerSizes) {
        this.layerSizes = layerSizes;
        initializeParameters();
    }

    private void initializeParameters() {
        weights = new double[layerSizes.length-1][][];
        biases = new double[layerSizes.length-1][][];

        for(int i = 0; i < layerSizes.length-1; i++) {
            int inSize = layerSizes[i];
            int outSize = layerSizes[i+1];

            double std = Math.sqrt(2.0 / (inSize + outSize));
            weights[i] = new double[outSize][inSize];
            for(int j = 0; j < outSize; j++) {
                for(int k = 0; k < inSize; k++) {
                    weights[i][j][k] = random.nextGaussian() * std;
                }
            }

            biases[i] = new double[outSize][1];
        }
    }

    private double[][] tanh(double[][] matrix) {
        double[][] result = new double[matrix.length][matrix[0].length];
        for(int i = 0; i < matrix.length; i++) {
            for(int j = 0; j < matrix[0].length; j++) {
                result[i][j] = Math.tanh(matrix[i][j]);
            }
        }
        return result;
    }

    private double[][] forward(double[][] input) {
        double[][] activation = input;
        for(int i = 0; i < weights.length-1; i++) { // Hidden layers
            activation = tanh(matrixAdd(matrixMultiply(weights[i], activation), biases[i]));
        }
        // Output layer (linear activation)
        return matrixAdd(matrixMultiply(weights[weights.length-1], activation),
            biases[biases.length-1]);
    }

    public double[] predict(double[] input) {
        double[][] inputMatrix = new double[input.length][1];
        for(int i = 0; i < input.length; i++) {
            inputMatrix[i][0] = input[i];
        }
        double[][] output = forward(inputMatrix);
        double[] result = new double[output.length];
        for(int i = 0; i < output.length; i++) {
            result[i] = output[i][0];
        }
        return result;
    }

    public double predictSingleOutput(double[] input) {
        return predict(input)[0];
    }


    public void train(double[][] X, double[][] Y, int epochs, double learningRate, int batchSize) {
        for(int epoch = 0; epoch < epochs; epoch++) {
            // Mini-batch training
            for(int batch = 0; batch < X.length; batch += batchSize) {
                int end = Math.min(batch + batchSize, X.length);

                // Forward pass
                double[][][] activations = new double[layerSizes.length][][];
                activations[0] = transpose(new double[][]{X[batch]});

                for(int i = 0; i < weights.length; i++) {
                    double[][] z = matrixAdd(matrixMultiply(weights[i], activations[i]), biases[i]);
                    activations[i+1] = (i == weights.length-1) ? z : tanh(z);
                }

                // Backward pass
                double[][] error = matrixSubtract(activations[activations.length-1],
                    transpose(new double[][]{Y[batch]}));

                for(int i = weights.length-1; i >= 0; i--) {
                    double[][] delta;
                    if(i == weights.length-1) {
                        delta = error; // Output layer (linear activation derivative is 1)
                    } else {
                        delta = matrixMultiplyElementwise(error, derivativeTanh(activations[i+1]));
                    }

                    double[][] gradWeights = matrixMultiply(delta, transpose(activations[i]));
                    double[][] gradBiases = delta;

                    // Update parameters
                    weights[i] = matrixSubtract(weights[i], matrixMultiplyScalar(gradWeights, learningRate));
                    biases[i] = matrixSubtract(biases[i], matrixMultiplyScalar(gradBiases, learningRate));

                    // Propagate error backward
                    error = matrixMultiply(transpose(weights[i]), delta);
                }
            }

            if(epoch % 100 == 0) {
                double loss = mseLoss(X, Y);
                System.out.println("Epoch " + epoch + " - Loss: " + loss);
            }
        }
    }

    private double mseLoss(double[][] X, double[][] Y) {
        double total = 0;
        for(int i = 0; i < X.length; i++) {
            double[] prediction = predict(X[i]);
            double error = prediction[0] - Y[i][0];
            total += error * error;
        }
        return total / X.length;
    }

    // Matrix operations
    private double[][] matrixMultiply(double[][] a, double[][] b) {
        double[][] result = new double[a.length][b[0].length];
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < b[0].length; j++) {
                for(int k = 0; k < a[0].length; k++) {
                    result[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return result;
    }

    private double[][] matrixMultiplyElementwise(double[][] a, double[][] b) {
        if (a.length != b.length || a[0].length != b[0].length) {
            throw new IllegalArgumentException("Matrices must have same dimensions for element-wise multiplication");
        }

        double[][] result = new double[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[0].length; j++) {
                result[i][j] = a[i][j] * b[i][j];
            }
        }
        return result;
    }


    private double[][] matrixAdd(double[][] a, double[][] b) {
        double[][] result = new double[a.length][a[0].length];
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[0].length; j++) {
                result[i][j] = a[i][j] + b[i][j];
            }
        }
        return result;
    }

    private double[][] matrixSubtract(double[][] a, double[][] b) {
        double[][] result = new double[a.length][a[0].length];
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[0].length; j++) {
                result[i][j] = a[i][j] - b[i][j];
            }
        }
        return result;
    }

    private double[][] derivativeTanh(double[][] matrix) {
        double[][] result = new double[matrix.length][matrix[0].length];
        for(int i = 0; i < matrix.length; i++) {
            for(int j = 0; j < matrix[0].length; j++) {
                result[i][j] = 1 - matrix[i][j] * matrix[i][j];
            }
        }
        return result;
    }

    private double[][] transpose(double[][] matrix) {
        double[][] result = new double[matrix[0].length][matrix.length];
        for(int i = 0; i < matrix.length; i++) {
            for(int j = 0; j < matrix[0].length; j++) {
                result[j][i] = matrix[i][j];
            }
        }
        return result;
    }

    private double[][] matrixMultiplyScalar(double[][] matrix, double scalar) {
        double[][] result = new double[matrix.length][matrix[0].length];
        for(int i = 0; i < matrix.length; i++) {
            for(int j = 0; j < matrix[0].length; j++) {
                result[i][j] = matrix[i][j] * scalar;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        // Example: Simple regression problem (y = x^2)
        double[][] X = {{0}, {0.2}, {0.4}, {0.6}, {0.8}, {1.0}};
        double[][] Y = {{0}, {0.04}, {0.16}, {0.36}, {0.64}, {1.0}};

        NeuralNetwork nn = new NeuralNetwork(1, 4, 4, 1); // 1 input, 2 hidden layers (4 nodes each), 1 output

        nn.train(X, Y, 1000, 0.01, 2);

        // Test prediction
        double[] testInput = {0.5};
        double[] prediction = nn.predict(testInput);
        System.out.println("Prediction for 0.5: " + prediction[0]);
    }

    public int getInputLayerSize() {
        return layerSizes[0];
    }

    public void assertNumberOfInputs(int length) {
        if (layerSizes[0] < length) {
            throw new IllegalArgumentException("Number of inputs must be less than or equal to the number of input nodes");
        }
    }
}
