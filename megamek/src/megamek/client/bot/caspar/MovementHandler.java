package megamek.client.bot.caspar;

import megamek.client.bot.princess.PathEnumerator;
import megamek.client.bot.princess.RankedPath;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles movement decisions for CASPAR AI units.
 * Generates, evaluates, and selects movement paths based on tactical objectives and neural network evaluation.
 */
public class MovementHandler {
    private final NeuralNetwork neuralNetwork;
    private final DifficultyManager difficultyManager;
    private final PathfindingEngine pathfinder;
    private final InputAxisCalculator inputCalculator;

    /**
     * Creates a movement handler with the specified components.
     *
     * @param neuralNetwork The neural network for path evaluation
     * @param difficultyManager The difficulty manager to use
     * @param pathfinder The pathfinding engine for path generation
     * @param inputCalculator The calculator for neural network inputs
     */
    public MovementHandler(NeuralNetwork neuralNetwork, DifficultyManager difficultyManager,
                           PathfindingEngine pathfinder, InputAxisCalculator inputCalculator) {
        this.neuralNetwork = neuralNetwork;
        this.difficultyManager = difficultyManager;
        this.pathfinder = pathfinder;
        this.inputCalculator = inputCalculator;
    }

    /**
     * Generates movement options for a unit based on the current game state.
     *
     * @param unit The unit to generate movement options for
     * @param gameState The current game state
     * @return A list of possible movement paths
     */
    public List<MovePath> generateMovementOptions(Entity unit, GameState gameState) {
        // Get the maximum search depth based on difficulty
        int searchDepth = difficultyManager.getMaxSearchDepth();

        // Use the pathfinder to generate possible paths
        List<MovePath> paths = pathfinder.generatePaths(unit, gameState.getGame(), searchDepth);

        // Filter out illegal paths
        return paths.stream()
            .filter(path -> path.isMoveLegal())
            .collect(Collectors.toList());
    }

    /**
     * Evaluates a movement path using the neural network.
     *
     * @param path The movement path to evaluate
     * @param gameState The current game state
     * @return A score between 0 and 1
     */
    public double evaluateMovePath(MovePath path, GameState gameState) {
        // Calculate input features for neural network
        double[] inputVector = inputCalculator.calculateInputVector(path, gameState);

        // Get raw prediction from neural network
        double prediction = neuralNetwork.predict(inputVector);

        // Allow difficulty manager to adjust prediction quality
        return difficultyManager.adjustDecisionQuality(prediction);
    }

    /**
     * Selects the best movement path for a unit from a set of possible paths.
     *
     * @param unit The unit to move
     * @param possiblePaths List of possible movement paths
     * @param gameState The current game state
     * @return The selected movement path
     */
    public MovePath selectBestMovePath(Entity unit, List<MovePath> possiblePaths, GameState gameState) {
        // Handle edge case: no paths available
        if (possiblePaths.isEmpty()) {
            return new MovePath(gameState.getGame(), unit);
        }

        // Score paths using neural network
        TreeSet<RankedPath> scoredPaths = new TreeSet<>();

        for (MovePath path : possiblePaths) {
            double score = evaluateMovePath(path, gameState);
            scoredPaths.add(new RankedPath(score, path, "Neural Network Evaluation"));
        }

        // Select based on difficulty setting
        return difficultyManager.selectFromTopPaths(scoredPaths);
    }

}
