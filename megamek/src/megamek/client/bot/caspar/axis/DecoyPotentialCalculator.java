package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the potential of the unit to act as a decoy
 */
public class DecoyPotentialCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the potential of the unit to act as a decoy
        double[] decoyPotential = axis();

        // Implementation goes here

        return decoyPotential;
    }
}
