package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.common.UnitRole;

/**
 * Calculates the threat by role
 */
public class ThreatByRoleCalculator extends BaseAxisCalculator {
    @Override
    public double[] axis() {
        return new double[UnitRole.values().length];
    }

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the threat by role
        double[] threatByRole = axis();

        return threatByRole;
    }
}
