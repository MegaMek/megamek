package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the ecm coverage of the unit ECM in the map
 */
public class EcmCoverageCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the potential of the unit to act as a decoy
        double[] ecmCoverage = axis();

        // Implementation goes here

        return ecmCoverage;
    }
}
