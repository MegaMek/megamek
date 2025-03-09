package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the target within optimal range
 */
public class TargetWithinOptimalRangeCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the target within optimal range
        double[] targetWithinOptimalRange = axis();

        return targetWithinOptimalRange;
    }
}
