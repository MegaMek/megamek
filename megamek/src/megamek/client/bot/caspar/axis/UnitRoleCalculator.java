package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.common.Entity;
import megamek.common.UnitRole;

/**
 * Calculates the unit role
 */
public class UnitRoleCalculator extends BaseAxisCalculator {
    @Override
    public double[] axis() {
        return new double[UnitRole.values().length];
    }

    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the unit role
        double[] unitRole = axis();
        Entity unit = pathing.getEntity();
        unitRole[unit.getRole().ordinal()] = 1d;
        return unitRole;
    }
}
