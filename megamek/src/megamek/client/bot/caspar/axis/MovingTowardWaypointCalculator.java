package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;

/**
 * Calculates if the unit is moving toward the waypoint
 */
public class MovingTowardWaypointCalculator extends BaseAxisCalculator {
    @Override
    public double[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates if the unit is moving toward the waypoint
        double[] movingTowardWaypoint = axis();

        return movingTowardWaypoint;
    }
}
