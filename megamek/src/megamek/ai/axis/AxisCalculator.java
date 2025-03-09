package megamek.ai.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Interface for calculating specific input axes.
 */
public interface AxisCalculator {
    /**
     * Returns the number of values this axis calculator will produce.
     *
     * @return The number of values
     */
    default double[] axis() {
        return new double[1];
    }

    /**
     * Calculates one or more input axes for a movement path.
     *
     * @param pathing The movement path to evaluate
     * @return An array of normalized values (0-1)
     */
    double[] calculateAxis(Pathing pathing, GameState gameState);
}
