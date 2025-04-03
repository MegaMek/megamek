package megamek.ai.neuralnetwork;

/**
 * BrainRegistry is a record that holds the model name, input axis length, and output axis length.
 * @param modelName The name of the model
 * @param inputAxisLength The length of the input axis
 * @param outputAxisLength The length of the output axis
 * @author Luana Coppio
 */
public record BrainRegistry(String modelName, int inputAxisLength, int outputAxisLength) {
}
