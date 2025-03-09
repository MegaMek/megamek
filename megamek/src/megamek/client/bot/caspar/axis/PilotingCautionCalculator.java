package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the piloting caution
 */
public class PilotingCautionCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the piloting caution
        double[] pilotingCaution = axis();

        return pilotingCaution;
    }
}
