package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the enemy threat in nearby hexes (31 values).
 */
public class FriendlyThreatNearbyCalculator extends BaseAxisCalculator {

    @Override
    public double[] axis() {
        return new double[31];
    }

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This would calculate the threat in the final position and all hexes up to 3 away
        double[] threats = axis();

        // Implementation goes here

        return threats;
    }
}
