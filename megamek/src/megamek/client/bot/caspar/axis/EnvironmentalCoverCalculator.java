package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the environmental cover of the final position against 5 enemy units that have you in sight
 */
public class EnvironmentalCoverCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the potential of the unit to act as a decoy
        double[] cover = axis();

        return cover;
    }
}
