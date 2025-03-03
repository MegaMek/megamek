package megamek.client.bot.caspar;

import java.io.Serial;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.io.Serializable;

/**
 * Neural network implementation for CASPAR decision-making.
 * Supports loading multiple network architectures with different weights.
 * @author Luana Coppio
 */
public class NeuralNetwork implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int[] layerSizes;
    private final double[][][] weights;
    private final double[][] biases;
    private final ActivationFunction[] activationFunctions;

    private static final Map<ModelType, NeuralNetwork> MODELS_CACHE = new ConcurrentHashMap<>();

    /**
     * Enum to represent different neural network architectures
     */
    public enum ModelType {
        FIRST_MODEL(new int[] {333, 333, 333, 1}, "first_model.weights"),
        SECOND_MODEL(new int[] {333, 333, 1}, "second_model.weights"),
        THIRD_MODEL(new int[] {333, 166, 83, 1}, "third_model.weights"),
        FOURTH_MODEL(new int[] {333, 166, 1}, "fourth_model.weights");

        private final int[] architecture;
        private final String filename;

        ModelType(int[] architecture, String filename) {
            this.architecture = architecture;
            this.filename = filename;
        }

        public int[] getArchitecture() {
            return architecture;
        }

        public String getFilename() {
            return filename;
        }
    }

    /**
     * Interface for activation functions
     */
    public interface ActivationFunction extends Serializable {
        double activate(double x);
    }

    /**
     * ELU (Exponential Linear Unit) activation function
     */
    public static class ELU implements ActivationFunction {
        private static final long serialVersionUID = 1L;
        private final double alpha;

        public ELU(double alpha) {
            this.alpha = alpha;
        }

        @Override
        public double activate(double x) {
            return x > 0 ? x : alpha * (Math.exp(x) - 1);
        }
    }

    /**
     * Linear activation function (for output layer)
     */
    public static class Linear implements ActivationFunction {
        private static final long serialVersionUID = 1L;

        @Override
        public double activate(double x) {
            return x;
        }
    }

    /**
     * Creates a neural network with the specified layer sizes and activation functions.
     *
     * @param layerSizes The size of each layer in the network
     * @param activationFunctions The activation function for each layer (except input)
     * @param weights The weights for each connection between layers
     * @param biases The biases for each layer (except input)
     */
    private NeuralNetwork(int[] layerSizes, ActivationFunction[] activationFunctions,
                          double[][][] weights, double[][] biases) {
        this.layerSizes = layerSizes;
        this.activationFunctions = activationFunctions;
        this.weights = weights;
        this.biases = biases;
    }

    /**
     * Forward pass through the neural network.
     *
     * @param input The input vector
     * @return The output prediction
     */
    public double predict(double[] input) {
        if (input.length != layerSizes[0]) {
            throw new IllegalArgumentException(
                "Input size " + input.length + " does not match expected size " + layerSizes[0]);
        }

        double[] currentLayer = input;
        double[] nextLayer;

        for (int layer = 0; layer < layerSizes.length - 1; layer++) {
            nextLayer = new double[layerSizes[layer + 1]];

            // For each neuron in the next layer
            for (int neuron = 0; neuron < layerSizes[layer + 1]; neuron++) {
                double sum = biases[layer][neuron];

                // Add weighted inputs
                for (int prevNeuron = 0; prevNeuron < layerSizes[layer]; prevNeuron++) {
                    sum += currentLayer[prevNeuron] * weights[layer][prevNeuron][neuron];
                }

                // Apply activation function
                nextLayer[neuron] = activationFunctions[layer].activate(sum);
            }

            currentLayer = nextLayer;
        }

        // Ensure output is between 0 and 1
        return Math.max(0, Math.min(1, currentLayer[0]));
    }

    /**
     * Factory method to load a neural network from a saved model file.
     *
     * @param modelType The type of model architecture to load
     * @param difficultyLevel The difficulty level to load weights for
     * @return A configured neural network
     */
    public static NeuralNetwork loadModel(ModelType modelType, DifficultyLevel difficultyLevel) {
        String cacheKey = modelType.toString() + "_" + difficultyLevel.toString();

        return MODELS_CACHE.computeIfAbsent(modelType, type -> {
            try {
                return loadModelFromFile(type, difficultyLevel);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load model: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Loads a neural network model from file.
     *
     * @param modelType The model architecture to load
     * @param difficultyLevel The difficulty level to load weights for
     * @return A configured neural network
     * @throws IOException If the model file cannot be read
     */
    private static NeuralNetwork loadModelFromFile(ModelType modelType, DifficultyLevel difficultyLevel)
        throws IOException {
        // Construct filename with difficulty level
        String filename = difficultyLevel.toString().toLowerCase() + "_" + modelType.getFilename();
        Path modelPath = Paths.get("data", "caspar", "models", filename);

        // In a real implementation, this would deserialize the model
        // For now, we'll create a dummy model with the right architecture
        int[] layerSizes = modelType.getArchitecture();

        // Create activation functions (ELU for hidden layers, Linear for output)
        ActivationFunction[] activationFunctions = new ActivationFunction[layerSizes.length - 1];
        Arrays.fill(activationFunctions, new ELU(1.0));
        activationFunctions[activationFunctions.length - 1] = new Linear();

        // Initialize weights and biases (in a real implementation, these would be loaded from file)
        double[][][] weights = new double[layerSizes.length - 1][][];
        double[][] biases = new double[layerSizes.length - 1][];

        for (int i = 0; i < layerSizes.length - 1; i++) {
            weights[i] = new double[layerSizes[i]][layerSizes[i + 1]];
            biases[i] = new double[layerSizes[i + 1]];
        }

        // In a real implementation, we would load the weights and biases from the file
        // For now, we'll just initialize them to small random values

        return new NeuralNetwork(layerSizes, activationFunctions, weights, biases);
    }
}
