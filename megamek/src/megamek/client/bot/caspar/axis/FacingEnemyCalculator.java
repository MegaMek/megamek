package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the facing of the unit against the 5 closest enemy units in the final position
 */
public class FacingEnemyCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates if the unit is facing the enemy
        double[] facing = axis();

        return facing;
    }
}
