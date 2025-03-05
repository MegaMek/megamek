package megamek.client.bot.caspar;

import megamek.client.bot.Agent;
import megamek.common.MovePath;

/**
 * Interface for calculating input vectors for the neural network.
 * @author Luana Coppio
 */
public interface InputAxisCalculator {
    /**
     * Calculates the input vector for a movement path.
     *
     * @param movePath The movement path to evaluate
     * @return A normalized input vector
     */
    double[] calculateInputVector(MovePath movePath, Agent agent, GameState gameState);
}
