package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the formation cohesion of the unit
 */
public class FormationCohesionCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the formation cohesion of the unit
        double[] formationCohesion = axis();

        return formationCohesion;
    }
}
