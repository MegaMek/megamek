package megamek.client.bot.neuralnetwork;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import megamek.client.bot.caspar.DefaultInputAxisCalculator;
import megamek.client.bot.common.DifficultyLevel;

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Neural network implementation for CASPAR decision-making.
 * Supports loading multiple network architectures with different weights.
 * Uses Jackson YAML for serialization and deserialization.
 * @author Luana Coppio
 */
public class NeuralNetwork implements Serializable {
    private static final Logger logger = Logger.getLogger(NeuralNetwork.class.getName());

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    private static final Map<ModelCacheKey, NeuralNetwork> MODELS_CACHE = new ConcurrentHashMap<>();

    @JsonIgnore
    private static final ObjectMapper YAML_MAPPER = createYamlMapper();

    private int[] layerSizes;
    private double[][][] weights;
    private double[][] biases;
    private ActivationFunction[] activationFunctions;
    private double learningRate;

    /**
     * Create the YAML mapper with appropriate configuration.
     *
     * @return Configured ObjectMapper
     */
    private static ObjectMapper createYamlMapper() {
        YAMLFactory yamlFactory = new YAMLFactory()
              .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
              .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

        ObjectMapper mapper = new ObjectMapper(yamlFactory);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
              ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

        return mapper;
    }

    private record ModelCacheKey(String modelName, DifficultyLevel difficultyLevel) {}

    /**
     * No-args constructor for Jackson
     */
    public NeuralNetwork() {
    }

    /**
     * Creates a neural network with the specified layer sizes and activation functions.
     *
     * @param layerSizes The size of each layer in the network
     * @param activationFunctions The activation function for each layer (except input)
     * @param weights The weights for each connection between layers
     * @param biases The biases for each layer (except input)
     * @param learningRate The learning rate for training
     */
    public NeuralNetwork(int[] layerSizes, ActivationFunction[] activationFunctions,
                         double[][][] weights, double[][] biases, double learningRate) {
        this.layerSizes = layerSizes;
        this.activationFunctions = activationFunctions;
        this.weights = weights;
        this.biases = biases;
        this.learningRate = learningRate;
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
     * Trains the network on a single sample using backpropagation.
     *
     * @param input The input vector
     * @param target The target output
     * @return The error
     */
    public double train(double[] input, double target) {
        if (input.length != layerSizes[0]) {
            throw new IllegalArgumentException(
                  "Input size " + input.length + " does not match expected size " + layerSizes[0]);
        }

        // Forward pass
        double[][] layerOutputs = new double[layerSizes.length][];
        double[][] layerInputs = new double[layerSizes.length][];

        // Input layer
        layerOutputs[0] = input;

        // Hidden and output layers
        for (int layer = 1; layer < layerSizes.length; layer++) {
            layerInputs[layer] = new double[layerSizes[layer]];
            layerOutputs[layer] = new double[layerSizes[layer]];

            // For each neuron in this layer
            for (int neuron = 0; neuron < layerSizes[layer]; neuron++) {
                double sum = biases[layer - 1][neuron];

                // Add weighted inputs
                for (int prevNeuron = 0; prevNeuron < layerSizes[layer - 1]; prevNeuron++) {
                    sum += layerOutputs[layer - 1][prevNeuron] * weights[layer - 1][prevNeuron][neuron];
                }

                // Store pre-activation input
                layerInputs[layer][neuron] = sum;

                // Apply activation function
                layerOutputs[layer][neuron] = activationFunctions[layer - 1].activate(sum);
            }
        }

        // Calculate output layer error
        double output = layerOutputs[layerSizes.length - 1][0];
        double error = output - target;

        // Backpropagation
        double[][] deltas = new double[layerSizes.length][];

        // Output layer delta
        deltas[layerSizes.length - 1] = new double[layerSizes[layerSizes.length - 1]];
        int outputLayer = layerSizes.length - 1;
        deltas[outputLayer][0] = error * activationFunctions[outputLayer - 1].derivative(layerInputs[outputLayer][0]);

        // Hidden layers deltas
        for (int layer = layerSizes.length - 2; layer > 0; layer--) {
            deltas[layer] = new double[layerSizes[layer]];

            for (int neuron = 0; neuron < layerSizes[layer]; neuron++) {
                double delta = 0;

                // Sum weighted deltas from next layer
                for (int nextNeuron = 0; nextNeuron < layerSizes[layer + 1]; nextNeuron++) {
                    delta += weights[layer][neuron][nextNeuron] * deltas[layer + 1][nextNeuron];
                }

                // Multiply by derivative of activation function
                deltas[layer][neuron] = delta * activationFunctions[layer - 1].derivative(layerInputs[layer][neuron]);
            }
        }

        // Update weights and biases
        for (int layer = 0; layer < layerSizes.length - 1; layer++) {
            for (int neuron = 0; neuron < layerSizes[layer + 1]; neuron++) {
                // Update bias
                biases[layer][neuron] -= learningRate * deltas[layer + 1][neuron];

                // Update weights
                for (int prevNeuron = 0; prevNeuron < layerSizes[layer]; prevNeuron++) {
                    weights[layer][prevNeuron][neuron] -= learningRate * deltas[layer + 1][neuron] * layerOutputs[layer][prevNeuron];
                }
            }
        }

        return error * error; // Return squared error
    }

    /**
     * Trains the network on a batch of samples.
     *
     * @param inputs Batch of input vectors
     * @param targets Batch of target outputs
     * @return Average error
     */
    public double trainBatch(double[][] inputs, double[] targets) {
        if (inputs.length != targets.length) {
            throw new IllegalArgumentException("Number of inputs and targets must match");
        }

        if (inputs.length == 0) {
            return 0;
        }

        double totalError = 0;

        for (int i = 0; i < inputs.length; i++) {
            totalError += train(inputs[i], targets[i]);
        }

        return totalError / inputs.length;
    }

    /**
     * Factory method to load a neural network from a saved model file.
     *
     * @param modelName The name of the model to load
     * @param difficultyLevel The difficulty level to load weights for
     * @return A configured neural network
     */
    public static NeuralNetwork loadModel(String modelName, DifficultyLevel difficultyLevel) {
        ModelCacheKey cacheKey = new ModelCacheKey(modelName, difficultyLevel);

        return MODELS_CACHE.computeIfAbsent(cacheKey, key -> {
            try {
                return loadModelFromFile(modelName, difficultyLevel);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to load model: " + e.getMessage(), e);
                int inputSize = new DefaultInputAxisCalculator().getInputSize();
                return createDefaultModel(new Architecture(new int[] {inputSize, inputSize, 1},
                      new ActivationFunction[] {new ELU(difficultyLevel.getAlphaValue()),
                            new ELU(difficultyLevel.getAlphaValue()),
                            new Linear()}),
                      difficultyLevel);
            }
        });
    }

    /**
     * Loads a neural network model from file.
     *
     * @param modelName The model to load
     * @param difficultyLevel The difficulty level to load weights for
     * @return A configured neural network
     * @throws IOException If the model file cannot be read
     */
    private static NeuralNetwork loadModelFromFile(String modelName, DifficultyLevel difficultyLevel)
          throws IOException {

        Path modelPath = getModelPath(modelName, difficultyLevel);
        File modelFile = modelPath.toFile();

        if (!modelFile.exists()) {
            throw new IOException("Model file does not exist: " + modelPath);
        }

        try {
            return YAML_MAPPER.readValue(modelFile, NeuralNetwork.class);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize model from: " + modelPath, e);
        }
    }

    /**
     * Creates a default model with the given architecture and random weights.
     *
     * @param difficultyLevel The difficulty level (affects learning rate and initialization)
     * @return A newly created neural network
     */
    private static NeuralNetwork createDefaultModel(Architecture architecture, DifficultyLevel difficultyLevel) {
        int[] layerSizes = Arrays.copyOf(architecture.getLayerSizes(),
              architecture.getLayerCount());
        ActivationFunction[] activationFunctions = Arrays.copyOf(architecture.getActivationFunctions(),
              architecture.getActivationFunctionCount());

        /*
        // Use different activation functions based on the layer's position
        for (int i = 0; i < layerSizes.length - 1; i++) {
            // For hidden layers: use ELU with tuned alpha for each difficulty level
            double alpha = getAlphaForDifficulty(difficultyLevel);
            activationFunctions[i] = new ELU(alpha);
        }

        // For output layer: use sigmoid to constrain output between 0 and 1
        activationFunctions[activationFunctions.length - 1] = new Sigmoid();
        */

        // Initialize weights and biases with small random values
        double[][][] weights = new double[layerSizes.length - 1][][];
        double[][] biases = new double[layerSizes.length - 1][];

        // The largest 4-digit prime number such that its sum of digits is a square, i.e., 9+9+0+7 = 25 = 5^2.
        Random random = new Random(difficultyLevel.ordinal() * 9907L * architecture.hashCode());

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

        double learningRate = getLearningRateForDifficulty(difficultyLevel);

        return new NeuralNetwork(layerSizes, activationFunctions, weights, biases, learningRate);
    }

    /**
     * Gets the appropriate learning rate for a difficulty level.
     *
     * @param difficultyLevel The difficulty level
     * @return The learning rate
     */
    private static double getLearningRateForDifficulty(DifficultyLevel difficultyLevel) {
        return switch (difficultyLevel) {
            case BEGINNER -> 0.01;
            case EASY -> 0.005;
            case MEDIUM -> 0.001;
            case HARD -> 0.0005;
            case HARDCORE -> 0.0001;  // Experts learn more carefully
        };
    }

    /**
     * Gets the file path for a model.
     *
     * @param modelName The model name
     * @param difficultyLevel The difficulty level
     * @return The path to the model file
     */
    private static Path getModelPath(String modelName, DifficultyLevel difficultyLevel) {
        String filename = difficultyLevel.toString().toLowerCase() + "_" + modelName + ".yaml";
        return Paths.get("data", "caspar", "models", filename);
    }

    /**
     * Saves the neural network to a file.
     *
     * @param modelName The model name
     * @param difficultyLevel The difficulty level
     * @throws IOException If the model cannot be saved
     */
    public void saveModel(String modelName, DifficultyLevel difficultyLevel) throws IOException {
        Path modelPath = getModelPath(modelName, difficultyLevel);
        File modelFile = modelPath.toFile();

        // Ensure the directory exists
        Files.createDirectories(modelPath.getParent());

        try {
            YAML_MAPPER.writeValue(modelFile, this);
            logger.info("Model saved to: " + modelPath);

            // Update the cache
            MODELS_CACHE.put(new ModelCacheKey(modelName, difficultyLevel), this);
        } catch (Exception e) {
            throw new IOException("Failed to serialize model to: " + modelPath, e);
        }
    }

    /**
     * Clears the model cache.
     */
    public static void clearCache() {
        MODELS_CACHE.clear();
    }

    /**
     * Removes a specific model from the cache.
     *
     * @param modelName The model name
     * @param difficultyLevel The difficulty level
     */
    public static void invalidateCache(String modelName, DifficultyLevel difficultyLevel) {
        MODELS_CACHE.remove(new ModelCacheKey(modelName, difficultyLevel));
    }

    /**
     * Creates a new model with specified architecture and saves it to a file.
     *
     * @param modelName The model name
     * @param difficultyLevel The difficulty level
     * @return The created neural network
     * @throws IOException If the model cannot be saved
     */
    public static NeuralNetwork createAndSaveModel(Architecture architecture, String modelName, DifficultyLevel difficultyLevel) throws IOException {
        NeuralNetwork network = createDefaultModel(architecture, difficultyLevel);
        network.saveModel(modelName, difficultyLevel);
        return network;
    }

    // Getters and setters for Jackson serialization

    public int[] getLayerSizes() {
        return layerSizes;
    }

    public void setLayerSizes(int[] layerSizes) {
        this.layerSizes = layerSizes;
    }

    public double[][][] getWeights() {
        return weights;
    }

    public void setWeights(double[][][] weights) {
        this.weights = weights;
    }

    public double[][] getBiases() {
        return biases;
    }

    public void setBiases(double[][] biases) {
        this.biases = biases;
    }

    public ActivationFunction[] getActivationFunctions() {
        return activationFunctions;
    }

    public void setActivationFunctions(ActivationFunction[] activationFunctions) {
        this.activationFunctions = activationFunctions;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }
}
