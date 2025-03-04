package megamek.client.bot.caspar;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.MovePath;

import java.util.List;

/**
 * Interface for path generation.
 */
public interface PathfindingEngine {
    /**
     * Generate paths for a unit.
     *
     * @param unit The unit to generate paths for
     * @param game The current game
     * @param depth Maximum path generation depth
     * @return List of generated paths
     */
    List<MovePath> generatePaths(Entity unit, Game game, int depth);
}
