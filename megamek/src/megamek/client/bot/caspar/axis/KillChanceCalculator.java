package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the kill chance
 */
public class KillChanceCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the kill chance
        double[] killChance = axis();

        return killChance;
    }
}
