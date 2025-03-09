package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the turns to encounter
 */
public class TurnsToEncounterCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the turns to encounter
        double[] turnsToEncounter = axis();

        return turnsToEncounter;
    }
}
