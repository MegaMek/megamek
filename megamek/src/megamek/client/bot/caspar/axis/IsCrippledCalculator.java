package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates if the unit is crippled
 */
public class IsCrippledCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates if the unit is crippled
        double[] isCrippled = axis();

        return isCrippled;
    }
}
