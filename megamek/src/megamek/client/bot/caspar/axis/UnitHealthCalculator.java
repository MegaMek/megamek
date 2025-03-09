package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the unit health (5 values, average, front, left, right, rear).
 */
public class UnitHealthCalculator extends BaseAxisCalculator {

    @Override
    public double[] axis() {
        return new double[5];
    }

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
