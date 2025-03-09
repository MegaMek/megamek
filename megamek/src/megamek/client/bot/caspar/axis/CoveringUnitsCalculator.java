package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the number of units that the unit is covering
 */
public class CoveringUnitsCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the number of units that the unit is covering
        double[] coveringUnits = axis();

        return coveringUnits;
    }
}
