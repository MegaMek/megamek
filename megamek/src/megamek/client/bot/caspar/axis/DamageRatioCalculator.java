package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the best expected damage ratio for the current attack
 */
public class DamageRatioCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the best expected damage ratio for the current attack
        double[] damageRatio = axis();

        // Implementation goes here

        return damageRatio;
    }
}
