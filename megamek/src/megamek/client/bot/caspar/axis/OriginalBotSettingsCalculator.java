package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates the original bot settings
 */
public class OriginalBotSettingsCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the original bot settings
        double[] botSettings = axis();

        return botSettings;
    }
}
