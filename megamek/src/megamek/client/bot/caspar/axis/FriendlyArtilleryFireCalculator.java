package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the friendly artillery fire potential
 */
public class FriendlyArtilleryFireCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the friendly artillery fire potential
        double[] artilleryFire = axis();

        return artilleryFire;
    }
}
