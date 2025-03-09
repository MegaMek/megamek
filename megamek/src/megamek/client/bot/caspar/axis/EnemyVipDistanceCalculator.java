package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the distance to the closest enemy VIP
 */
public class EnemyVipDistanceCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the distance to the closest enemy VIP
        double[] vipDistance = axis();

        return vipDistance;
    }
}
