package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the heat management of the unit
 */
public class HeatManagementCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the heat management of the unit
        double[] heatManagement = axis();

        return heatManagement;
    }
}
