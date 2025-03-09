package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates how crowded is the area of 31 hexes around the final position coordinate.
 */
public class PositionCrowdingCalculator extends BaseAxisCalculator {

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This would calculate the health of the unit as a percentage for
        // 0 - average
        // 1 - front
        // 2 - left
        // 3 - right
        // 4 - rear

        double[] health = axis();

        // Implementation goes here

        return health;
    }
}
