package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the strategic goal
 */
public class StrategicGoalCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the strategic goal
        double[] strategicGoal = axis();

        return strategicGoal;
    }
}
