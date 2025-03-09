package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates if the favorite target role type is in range
 */
public class FavoriteTargetInRangeCalculator extends BaseAxisCalculator {

    @Override
    public double[] axis() {
        return new double[3];
    }

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // calculate if the favorite target role type is in range
        double[] favoriteTarget = axis();
        // 0 - SNIPER
        // 1 - MISSILE BOAT
        // 2 - JUGGERNAUT
        return favoriteTarget;
    }
}
