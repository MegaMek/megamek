package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates if the unit is in a flanking position
 */
public class FlankingPositionCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates if the unit is in a flanking position
        double[] flanking = axis();

        return flanking;
    }
}
