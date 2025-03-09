package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the number of nearby enemy units
 */
public class NearbyEnemyCountCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the number of nearby enemy units
        double[] nearbyEnemies = axis();

        return nearbyEnemies;
    }
}
