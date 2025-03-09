package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the friendly threat heatmap (100 values).
 */
public class FriendlyThreatHeatmapCalculator extends BaseAxisCalculator {

    @Override
    public double[] axis() {
        return new double[100];
    }

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This would calculate the 10x10 heatmap for friendly threats
        double[] heatmap = axis();

        // Implementation goes here

        return heatmap;
    }
}
