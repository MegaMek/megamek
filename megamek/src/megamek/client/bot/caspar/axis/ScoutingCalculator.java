package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the scouting
 */
public class ScoutingCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the scouting
        double[] scouting = axis();

        return scouting;
    }
}
